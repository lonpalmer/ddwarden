package com.lon.packfile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CountingInputStream;
import com.google.common.io.LittleEndianDataInputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

@Getter
@Setter
@Slf4j
public class PackInfo {
    public static final int PACK_MAGIC_NO = 0x43504447;
    private String godotVer;
    private int fileCount;
    private final List<FileInfo> files = new LinkedList<>();
    private String packagePath;
    private final ObjectMapper jackson;
    private PackMetadata packMetaData;
    private final Semaphore fileWriteLimiter = new Semaphore(5);

    public PackInfo(String packagePath, ObjectMapper jackson) throws InvalidPackFileException {
        this.packagePath = packagePath;
        this.jackson = jackson;
    }

    public int getHeaderSize() {
        var totalPathLength = 0;
        for (FileInfo f : files) {
            totalPathLength += f.getPath().length();
        }
        var filesCount = files.size();

        // [4 magic][4*4 version][64 reserved][4 file count][totalPathLength]([4 path length][8 offset][8 size][16 md5 hash] * filesCount)
        return 4 + (4*4) + 64 + 4 + totalPathLength + ((4 + 8 + 8 + 16) * filesCount);
    }

    public void indexAndHashFiles() {
        try {
            files.sort(Comparator.comparing(FileInfo::getPath));
            var offset = (long) getHeaderSize();
            var hash = MessageDigest.getInstance("MD5");
            for (var f : files) {
                f.setOffset(offset);
                offset += f.getSize();
                f.setMd5Hash(hash.digest(f.getPath().getBytes(StandardCharsets.UTF_8)));
            }
        } catch (NoSuchAlgorithmException ignored) {
        }
    }


    public void scan() throws InvalidPackFileException {
        long packFileOffset = 0;
        long packFileSize = 0;

        try(var in = new LittleEndianDataInputStream(new CountingInputStream(new FileInputStream(packagePath)))) {

            int magic = in.readInt();
            if(magic != PACK_MAGIC_NO) {
                throw new InvalidPackFileException(packagePath);
            }

            int[] ver = new int[4];
            for(int i = 0; i < 4; i++) {
                ver[i] = in.readInt();
            }
            godotVer = String.format("%d.%d.%d.%d", ver[0], ver[1], ver[2], ver[3]);

            in.skip(64);

            fileCount = in.readInt();
            for(int i = 0; i < fileCount; i++) {
                var pathLen = in.readInt();
                var pathBytes = new byte[pathLen];
                in.read(pathBytes);
                var path = new String(pathBytes, StandardCharsets.UTF_8);
                var offset = in.readLong();
                var size = in.readLong();
                var md5Hash = new byte[16];
                in.read(md5Hash);
                var fileInfo = new FileInfo(path, size, offset, md5Hash);
                files.add(fileInfo);

                if(fileInfo.getPath().endsWith("pack.json")) {
                    packFileOffset = offset;
                    packFileSize = size;
                }
            }

        } catch (IOException e) {
            throw new InvalidPackFileException(packagePath);
        }

        // read and parse the pack.json file
        if(packFileOffset != 0 && packFileSize != 0) {
            try(InputStream in = new FileInputStream(packagePath)) {
                in.skip(packFileOffset);
                var packJsonBytes = new byte[(int)packFileSize];
                in.read(packJsonBytes);
                packMetaData = jackson.readValue(packJsonBytes , PackMetadata.class);
            } catch (IOException e) {
                log.error("Error while reading pack.json", e);
            }
        } else {
            log.warn("No pack.json file found in the pack");
        }
    }

    public byte[] getPackageBuffer() throws IOException{
        var buffStream = new ByteArrayOutputStream();
        try(var inPack = new BufferedInputStream(new FileInputStream(packagePath))) {
            var buf = new byte[4096];
            var read = 0;
            while((read = inPack.read(buf)) != -1) {
                buffStream.write(buf, 0, read);
            }
        } catch (IOException e) {
            log.error("Error trying to buffer file at {}", packagePath, e);
            throw e;
        }

        return buffStream.toByteArray();
    }

    public Callable<Integer> makeFileWriteTask(final FileInfo fileInfo, final byte[] packBuffer) {
        return () -> {
            var filePath = packagePath + "/" + fileInfo.getPath().replace("res://packs/", "");

            try {

                Files.createDirectories(Path.of(filePath).getParent());
                try (var in = new ByteArrayInputStream(packBuffer)) {
                    in.skip(fileInfo.getOffset());
                    try (var out = new FileOutputStream(filePath)) {

                        var buf = new byte[4096];
                        var read = 0;
                        var toRead = fileInfo.getSize();

                        while (toRead > 0 && (read = in.read(buf, 0, (int) Math.min(packBuffer.length, toRead))) != -1) {
                            out.write(buf, 0, read);
                            toRead -= read;
                        }
                    }
                }

                return 1;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                fileWriteLimiter.release();
            }


        };
    }



    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PackMetadata {
        private String name;
        private String id;
        private String version;
        private String author;
        private CustomColorOverrides custom_color_overrides;

        @Getter
        @Setter
        private static class CustomColorOverrides {
            private boolean enabled;
            private double min_redness;
            private double min_saturation;
            private double red_tolerance;
        }

    }


}
