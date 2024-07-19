package com.lon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lon.image.ImageProcessor;
import com.lon.packfile.InvalidPackFileException;
import com.lon.packfile.PackInfo;
import me.tongfei.progressbar.ProgressBar;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@CommandLine.Command(name="ddtool", mixinStandardHelpOptions = true, version = "ddtool v1.0",
        description = "Generates thumbnails for dungeondraft packs")
public class GenThumbs implements Callable<Integer> {

    @Parameters(index = "0", description = "The source package directory or dungeondraft pack")
    private String source = "";

    @Parameters(index = "1", description = "The destination directory for the thumbnails")
    private String destination = "";

    @Option(names = {"-o", "--over-write"}, description = "Overwrite existing thumbnails")
    private boolean overwrite = false;

    @Option(names = {"-l", "--limit"}, description = "The number of files to process concurrently")
    private int fileProcessLimit = 1;

    private final ObjectMapper jackson = new ObjectMapper();

    @Override
    public Integer call()  {

        var packsToProcess = new LinkedList<String>();

        var src = new File(source);

        if(!src.exists()) {
            System.err.println("Source directory or file does not exist");
            return 1;
        }

        if(fileProcessLimit < 1 || fileProcessLimit > 50) {
            System.err.println("File process limit must be between 1 and 50");
            return 1;
        }

        if(src.isDirectory()) {
            var files = src.listFiles();

            if(files != null) {
                for (var file : files) {
                    if (file.getName().endsWith(".dungeondraft_pack")) {
                        packsToProcess.add(file.getAbsolutePath());
                    }
                }
            }
        } else {
            if(src.getName().endsWith(".dungeondraft_pack")) {
                packsToProcess.add(src.getAbsolutePath());
            }
        }

        System.out.println("Generating thumbnails for " + packsToProcess.size() + " pack" + (packsToProcess.size() == 1 ? "" : "s"));

        var dest = new File(destination);
        if(!dest.exists()) {
            dest.mkdirs();
        }

        var semaphore = new Semaphore(fileProcessLimit);

        var callables = packsToProcess.stream().map(filePath -> (Callable<Void>) () -> {

            try {
                semaphore.acquire();
                var packInfo = new PackInfo(filePath, jackson);
                packInfo.scan();
                var buffer = packInfo.getPackageBuffer();
                var imageProcessor = new ImageProcessor();
                imageProcessor.setOverwrite(overwrite);

                var totalImageFiles = packInfo.getFiles().stream()
                        .filter(f -> ImageProcessor.supportedImageFormats.stream().anyMatch(f.getPath().toLowerCase()::endsWith))
                        .count();

                var fileName = new File(filePath).getName();
                try(var pb = new ProgressBar("Processing " + fileName, totalImageFiles)) {
                    imageProcessor.onProgressCallback(pb::stepTo);
                    imageProcessor.makeThumbnailsForPack(packInfo, buffer, destination);
                }
            } catch (InvalidPackFileException e) {
                throw new RuntimeException(e);
            } finally {
                semaphore.release();
            }

            return null;
        }).toList();

        try(var executor = Executors.newFixedThreadPool(1)) {
            executor.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }
}
