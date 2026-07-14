package com.microyu.pixiv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public final class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    private static final Path IMAGES_PATH = Paths.get("IMAGES.md");
    private static final int COLUMNS = 3;

    private FileUtils() {
    }

    public static void writeImages(List<Image> imgList) throws IOException {
        String content = renderMarkdown(imgList);

        // 原子写入：先写临时文件，再 rename
        Path tempPath = Paths.get("IMAGES.md.tmp");
        Files.writeString(tempPath, content);
        Files.move(tempPath, IMAGES_PATH, StandardCopyOption.REPLACE_EXISTING);

        log.info("IMAGES.md written ({} images, {} bytes)", imgList.size(), content.length());
    }

    // 将图片列表渲染为 Markdown 字符串
    static String renderMarkdown(List<Image> imgList) {
        String today = (LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).replace("T", " ").substring(0,19);

        StringBuilder sb = new StringBuilder();
        sb.append("## Pixiv Daily\n");
        sb.append("Update: ").append(today).append('\n');
        sb.append("\n");
        // 表头
        sb.append("|      |      |      |\n");
        sb.append("| :----: | :----: | :----: |\n");

        int i = 0;
        for (Image image : imgList) {
            if (i % COLUMNS == 0) {
                sb.append('|');
            }
            sb.append(image.toString()).append('|');
            i++;
            if (i % COLUMNS == 0) {
                sb.append('\n');
            }
        }

        if (i  % COLUMNS != 0) {
            while (i % COLUMNS != 0) {
                sb.append("      |");
                i++;
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}
