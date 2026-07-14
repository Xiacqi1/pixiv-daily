package com.microyu.pixiv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpUtls {

    private static final Logger log = LoggerFactory.getLogger(HttpUtls.class);

    // 创建实例，用于发送请求
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
            .build();

    private HttpUtls() {
    }

    public static String getHttpContent(String url) throws IOException {
        // 创建请求实例
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
//                .header("Accept-Encoding", "gzip")
                .GET()
                .build();

        IOException lastException = null;
        // 重试循环
        for (int attempt = 1; attempt <= Config.MAX_RETRIES; attempt++) {
            try {
                log.info("HTTP GET {} (重试次数 {}/{})", url, attempt, Config.MAX_RETRIES);
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                // 获取状态码
                int statusCode = response.statusCode();
                if (statusCode != 200) {
                    throw new IOException("HTTP " + statusCode + " for URL: " + url);
                }

                String body = response.body();
                log.info("HTTP GET success, response length: {} chars", body.length());
                return body;

            } catch (InterruptedException e) {
                // 中止
                Thread.currentThread().interrupt();
                throw new IOException("HTTP request interrupted for URL: " + url, e);
            } catch (IOException e) {
                // 若状态码不为200，重试
                lastException = e;
                log.warn("HTTP request failed (attempt {}/{}): {}", attempt, Config.MAX_RETRIES, e.getMessage());

                if (attempt < Config.MAX_RETRIES) {
                    long backoffMs = (long) Math.pow(2, attempt - 1) * 1000;
                    log.info("Retrying in {}ms...", backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        // 中止
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry wait interrupted for URL: " + url, ie);
                    }
                }
            }
        }

        throw new IOException("HTTP request failed after " + Config.MAX_RETRIES + " retries for URL: " + url, lastException);
    }
}
