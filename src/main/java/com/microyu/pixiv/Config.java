package com.microyu.pixiv;

// 配置管理
public final class Config {

    private Config() {
    }

    // rss链接
    public static final String PIXIV_API_URL = getEnvOrDefault(
            "PIXIV_API_URL",
            "https://www.pixiv.net/ranking.php?format=json&mode=daily&p=1"
    );

    // 抓取图片数量
    public static final int IMAGE_COUNT = Integer.parseInt(
            getEnvOrDefault("IMAGE_COUNT", "50")
    );

    // 图片 CDN 反代域名（替换 i.pximg.net）
    public static final String CDN_DOMAIN = getEnvOrDefault(
            "CDN_DOMAIN",
            "pixiv.microyu.workers.dev"
    );

    // 超时时间
    public static final int HTTP_TIMEOUT_MS = Integer.parseInt(
            getEnvOrDefault("HTTP_TIMEOUT_MS", "10000")
    );

    // 最大重试次数
    public static final int MAX_RETRIES = Integer.parseInt(
            getEnvOrDefault("MAX_RETRIES", "3")
    );


    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
