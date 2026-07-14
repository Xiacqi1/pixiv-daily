package com.microyu.pixiv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Objects;
import java.util.regex.Pattern;

// JSON 反序列化时忽略未知字段
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
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

    private int rank;

    private String smallUrl;

    private String bigUrl;

    private String pageUrl;

    public Image() {
        // Jackson 反序列化需要无参构造
    }

    private static final Pattern SIZE_PATTERN = Pattern.compile("/c/\\d+x\\d+/img-master/");

    public void resolveUrls() {
        if (originalUrl != null) {
            // 缩略图：替换域名为 CDN
            this.smallUrl = originalUrl.replace("i.pximg.net", Config.CDN_DOMAIN);
            // 原图：将 /c/{宽}x{高}/img-master/ 替换为 /img-original/，并去掉 _master{数字} 后缀
            this.bigUrl = SIZE_PATTERN.matcher(smallUrl).replaceFirst("/img-original/")
                    .replaceAll("_master\\d+", "");
        }
        if (illustId > 0) {
            this.pageUrl = "https://www.pixiv.net/artworks/" + illustId;
        }
    }

    @Override
    public String toString() {
        boolean isSuffix = bigUrl.substring(bigUrl.lastIndexOf('.')+1).equals("jpg");
        if (isSuffix) {
            return String.format(
                    "![](%s) **#%s** [%s](%s) [[download](%s)]",
                    smallUrl, rank, title, pageUrl, bigUrl
            );
        }
        return String.format(
                "![](%s) **#%s** [%s](%s) [[download](%s)]",
                smallUrl, rank, title, pageUrl, bigUrl.replace("jpg", "png")
        );
    }

    // 重写 equals 方法，去重
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // 如果对象为空或类不同，则返回 false
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
