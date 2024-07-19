package com.lon.image;

import com.lon.packfile.FileInfo;
import com.lon.packfile.PackInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Slf4j
public class ImageProcessor {

    int DEFAULT_PX_HEIGHT = 64;

    int TERRAIN_HEIGHT_PX = 160;
    int TERRAIN_WIDTH_PX = 160;

    int PATH_THUMBNAIL_HEIGHT_PX = 48;
    int WALL_THUMBNAIL_HEIGHT_PX = 32;
    int WALL_THUMBNAIL_MAX_WIDTH_PX = 228;

    public static final List<String> supportedImageFormats = List.of("webp", "png", "jpg", "jpeg");

    @Getter
    @Setter
    private boolean overwrite = false;

    AtomicInteger progressCounter = new AtomicInteger(0);
    Optional<Consumer<Integer>> progressCallback = Optional.empty();

    public void onProgressCallback(Consumer<Integer> callback) {
        progressCallback = Optional.of(callback);
    }

    private void doProgress() {
        progressCounter.incrementAndGet();
        progressCallback.ifPresent(c -> c.accept(progressCounter.get()));
    }

    final Semaphore fileWriteLimiter = new Semaphore(50);

    public void makeThumbnailsForPack(PackInfo packInfo, byte[] packBuffer, String thumbnailDir) {
        var callables = new LinkedList<Callable<Void>>();
        for (FileInfo fileInfo : packInfo.getFiles()) {
            Callable<Void> callable = () -> {
                try {


                    if(supportedImageFormats.stream().anyMatch(fileInfo.getPath().toLowerCase()::endsWith)) {

                        var name = new StringBuilder();
                        for (byte b : generateMd5Hash(fileInfo.getPath())) {
                            name.append(String.format("%02x", b & 0xff));
                        }
                        var thumbnailPath = thumbnailDir + "/" + name + ".png";

                        if (!overwrite && new File(thumbnailPath).exists()) {
                            doProgress();
                            return null;
                        }

                        var imageBytes = new byte[(int) fileInfo.getSize()];
                        System.arraycopy(packBuffer, (int) fileInfo.getOffset(), imageBytes, 0, (int) fileInfo.getSize());
                        var thumbnail = makeThumbnailImage(fileInfo, imageBytes);

                        ImageIO.write(thumbnail, "png", new File(thumbnailPath));
                        doProgress();
                    }
                } catch (IOException e) {
                    log.error("Failed to make thumbnail for resource {} in package {}\r\tFileInfo {}", fileInfo.getPath(), packInfo.getPackagePath(), fileInfo, e);
                } finally {
                    fileWriteLimiter.release();
                }
                return null;
            };
            callables.add(callable);
        }

        try(var executor = Executors.newFixedThreadPool(5)) {
            executor.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public byte[] generateMd5Hash(String fileInfoPath) {
        // return the MD5 hash of the fileInfoPath
        try {
            return MessageDigest.getInstance("MD5").digest(fileInfoPath.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }


    public BufferedImage makeThumbnailImage(FileInfo fileInfo, byte[] imageBytes) throws IOException {

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (fileInfo.getPath().toLowerCase().contains("terrain")) {
                return makeTerrainThumbnail(image);
            } else if (fileInfo.getPath().toLowerCase().contains("wall")) {
                return makeWallThumbnail(image);
            } else if (fileInfo.getPath().toLowerCase().contains("path")) {
                return makePathThumbnail(image);
            } else {
                return makeDefaultThumbnail(image);
            }

        } catch (IOException e) {
            log.error("Failed to read image from byte array", e);
            throw e;
        }
    }

    private BufferedImage makeTerrainThumbnail(BufferedImage image) {

        var thumbImage = image.getScaledInstance(TERRAIN_WIDTH_PX, TERRAIN_HEIGHT_PX, BufferedImage.SCALE_SMOOTH);
        var thumbnail = new BufferedImage(TERRAIN_WIDTH_PX, TERRAIN_HEIGHT_PX, BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(thumbImage, 0, 0, TERRAIN_WIDTH_PX, TERRAIN_HEIGHT_PX, null);
        return thumbnail;

    }

    private BufferedImage makeDefaultThumbnail(BufferedImage image) {
        // Find the ratio of the image
        double ratio = (double) image.getWidth() / image.getHeight();

        // now scale the image to height 64 and width according to the ratio
        int scaledWidth = (int) (DEFAULT_PX_HEIGHT * ratio);
        var thumbImage = image.getScaledInstance(scaledWidth, DEFAULT_PX_HEIGHT, BufferedImage.SCALE_SMOOTH);
        var thumbnail = new BufferedImage(scaledWidth, DEFAULT_PX_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(thumbImage, 0, 0, scaledWidth, DEFAULT_PX_HEIGHT, null);
        return thumbnail;
    }

    private BufferedImage makeWallThumbnail(BufferedImage image) {
        return makeLongThumbnail(WALL_THUMBNAIL_HEIGHT_PX, image);
    }

    private BufferedImage makePathThumbnail(BufferedImage image) {
        return makeLongThumbnail(PATH_THUMBNAIL_HEIGHT_PX, image);
    }

    private BufferedImage makeLongThumbnail(int height, BufferedImage image) {
        // Find the ratio of the image
        double ratio = (double) image.getWidth() / image.getHeight();

        // now scale the image to height 32 and width according to the ratio
        int scaledWidth = (int) (height * ratio);
        if (scaledWidth > WALL_THUMBNAIL_MAX_WIDTH_PX) {
            scaledWidth = WALL_THUMBNAIL_MAX_WIDTH_PX;
        }
        var thumbImage = image.getScaledInstance(scaledWidth, height, BufferedImage.SCALE_SMOOTH);
        var thumbnail = new BufferedImage(scaledWidth, height, BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(thumbImage, 0, 0, scaledWidth, height, null);
        return thumbnail;
    }


}
