package com.microyu.pixiv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pixiv Daily 主入口
 * <p>
 * 从 Pixiv 排行榜 API 获取 JSON 数据，解析图片信息，渲染到 README.md
 */
public class Pixiv {

    private static final Logger log = LoggerFactory.getLogger(Pixiv.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        log.info("Pixiv Daily started");
        log.info("API URL: {}", Config.PIXIV_API_URL);
        log.info("Image count: {}, CDN: {}, Max retries: {}", Config.IMAGE_COUNT, Config.CDN_DOMAIN, Config.MAX_RETRIES);

        try {
            // 1. 请求 API
            String httpContent = HttpUtls.getHttpContent(Config.PIXIV_API_URL);

            // 2. 解析 JSON
            JsonNode root = MAPPER.readTree(httpContent);
            JsonNode contents = root.get("contents");

            if (contents == null || !contents.isArray()) {
                log.error("API response does not contain 'contents' array");
                return;
            }

            int available = contents.size();
            int count = Math.min(Config.IMAGE_COUNT, available);
            log.info("Found {} items, will process {}", available, count);

            // 3. 解析图片列表
            List<Image> imagesList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                JsonNode node = contents.get(i);
                try {
                    Image image = MAPPER.treeToValue(node, Image.class);
                    image.setRank(i + 1);
                    image.resolveUrls();
                    imagesList.add(image);
                    log.debug("Parsed #{}: {} by {}", image.getRank(), image.getTitle(), image.getUserName());
                } catch (Exception e) {
                    log.warn("Failed to parse item #{}: {}", i + 1, e.getMessage());
                }
            }

            log.info("Successfully parsed {} images", imagesList.size());

            if (imagesList.isEmpty()) {
                log.error("No images were parsed, skipping README update");
                return;
            }

            // 4. 写入 README.md
            FileUtils.writeReadme(imagesList);
            log.info("README.md updated successfully");

        } catch (IOException e) {
            log.error("Pixiv Daily failed: {}", e.getMessage(), e);
        }
    }
}
