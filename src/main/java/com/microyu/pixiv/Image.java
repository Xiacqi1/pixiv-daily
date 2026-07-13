package com.microyu.pixiv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Pixiv 排行榜图片数据模型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    @JsonProperty("title")
    private String title;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("date")
    private String date;

    @JsonProperty("illust_id")
    private long illustId;

    @JsonProperty("url")
    private String originalUrl;

    /** 排名序号（非 JSON 字段，解析后赋值） */
    private int rank;

    /** 缩略图 URL（CDN 反代） */
    private String smallUrl;

    /** 原图 URL（CDN 反代） */
    private String bigUrl;

    /** 页面地址 */
    private String pageUrl;

    public Image() {
        // Jackson 反序列化需要无参构造
    }

    /**
     * 根据原始 JSON 数据补全衍生字段
     * <p>
     * 在 Jackson 反序列化完成后调用，用于生成 CDN URL、页面地址等
     */
    public void resolveUrls() {
        if (originalUrl != null) {
            // 缩略图：替换域名为 CDN
            this.smallUrl = originalUrl.replace("i.pximg.net", Config.CDN_DOMAIN);
            // 原图：路径替换为 original，去掉 _master1200 后缀
            this.bigUrl = smallUrl
                    .replace("/c/240x480/img-master/", "/img-original/")
                    .replace("_master1200", "");
        }
        if (illustId > 0) {
            this.pageUrl = "https://www.pixiv.net/artworks/" + illustId;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "![](%s) **#%s** [%s](%s) download: [JPG](%s) [PNG](%s)",
                smallUrl, rank, title, pageUrl, bigUrl, bigUrl != null ? bigUrl.replace("jpg", "png") : ""
        );
    }

    // --- Getters & Setters ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getIllustId() {
        return illustId;
    }

    public void setIllustId(long illustId) {
        this.illustId = illustId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getSmallUrl() {
        return smallUrl;
    }

    public String getBigUrl() {
        return bigUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return illustId == image.illustId && rank == image.rank
                && Objects.equals(title, image.title)
                && Objects.equals(userName, image.userName)
                && Objects.equals(date, image.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, userName, date, illustId, rank);
    }
}
