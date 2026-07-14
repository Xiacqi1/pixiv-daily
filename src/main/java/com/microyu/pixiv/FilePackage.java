package com.microyu.pixiv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FilePackage {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    private static final String filePath = "releases/";
    private static final String imagePath = "daily-images/";
    private static final Path tempDir = Paths.get(imagePath);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    // 并发线程池
    private static final int THREAD_POOL_SIZE = 10;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            4,
            THREAD_POOL_SIZE,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(60),
            new ThreadFactory() {
                @Override
                // 自定义线程名称
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "image-download-" + Thread.currentThread().getId());
                    t.setDaemon(true);  // 守护线程，主程序退出自动结束
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private FilePackage() {

    }

    public static void imagesDownload(List<Image> imagesList) throws IOException {
        counter.set(0);
        log.info("begin to download {} images", imagesList.size());

        Files.createDirectories(tempDir);
        List<CompletableFuture<Void>> futures = imagesList.stream()
                .map(image -> CompletableFuture.runAsync(() -> downloadOne(image), executor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("download {} images success, save in {}", imagesList.size(), imagePath);
    }
    // 判断下载是否成功：状态码 200 且 Content-Type 为图片
    private static final long MIN_IMAGE_SIZE = 5 * 1024;

    public static void downloadOne(Image image) {
        int index = counter.incrementAndGet();
        log.info("begin to download image: {}. {}", index, image.getTitle());
        String safeTitle = image.getTitle()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_");

        if (safeTitle.length() > 50) {
            safeTitle = safeTitle.substring(0, 50);
        }
        if (safeTitle.isEmpty()) { safeTitle = index + "-untitled"; }
        else { safeTitle = index + "-" + safeTitle; }

        // 先尝试原图，失败则回退缩略图
        String bigExt = image.getBigUrl().contains(".")
                ? image.getBigUrl().substring(image.getBigUrl().lastIndexOf(".")) : ".jpg";
        String smallExt = image.getSmallUrl().contains(".")
                ? image.getSmallUrl().substring(image.getSmallUrl().lastIndexOf(".")) : ".jpg";

        boolean success = tryDownload(image.getBigUrl(), safeTitle + bigExt);
        if (!success) {
            log.warn("big image failed, fallback to small image for: {}", image.getTitle());
            success = tryDownload(image.getSmallUrl(), safeTitle + smallExt);
        }
        if (!success) {
            log.error("all urls failed for image: {}", image.getTitle());
        }
    }

    private static boolean tryDownload(String url, String imageName) {
        log.info("try download: {}, url: {}", imageName, url);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URL(url).toURI())
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            int statusCode = response.statusCode();
            String contentType = response.headers().firstValue("Content-Type").orElse("unknown");

            if (statusCode != 200) {
                log.warn("download failed, HTTP {} for: {}", statusCode, url);
                return false;
            }
            if (!contentType.startsWith("image/")) {
                log.warn("download failed, not an image: Content-Type={} for: {}", contentType, url);
                return false;
            }

            try (InputStream in = response.body()) {
                long bytes = Files.copy(in, tempDir.resolve(imageName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                if (bytes < MIN_IMAGE_SIZE) {
                    log.warn("download failed, file too small: {} bytes for: {}", bytes, url);
                    Files.deleteIfExists(tempDir.resolve(imageName));
                    return false;
                }
                log.info("download success: {}, size: {} bytes", imageName, bytes);
                return true;
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            log.error("download error: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
    public static void zipDownload(String date) {
        // 创建压缩包
        Path sourceDir = Paths.get(imagePath);
        Path zipDir = Paths.get(filePath);

        if (!Files.exists(sourceDir)) {
            log.error("source directory does not exist: {}", sourceDir);
            return;
        }
        try{
            Files.createDirectories(zipDir);
            String zipFileName = "pixiv-daily-" + date + ".zip";
            Path zipFile = zipDir.resolve(zipFileName);
            try(ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                File[] files = sourceDir.toFile().listFiles();
                if ( files == null || files.length == 0 ) {
                    log.error("source directory does not exist: {}", sourceDir);
                    return;
                }
                for (File file : files) {
                    if (file.isFile()) {
                        zos.putNextEntry(new ZipEntry(file.getName()));
                        Files.copy(file.toPath(), zos);
                        zos.closeEntry();
                    }
                }
            }catch (IOException e){
                log.error("zip error: {}", e.getMessage());
            }
            log.info("zip file success: {}", zipFile.toAbsolutePath());

            // 清除图片
            File[] files = sourceDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    Files.deleteIfExists(file.toPath());
                }
            }
            Files.deleteIfExists(sourceDir);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
