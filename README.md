# Pixiv Daily

> 基于 [gxywy/pixiv-daily](https://github.com/gxywy/pixiv-daily) 重构，新增图片下载、ZIP 打包和自动发布 Release 功能。

自动获取 Pixiv 每日排行榜，生成 `IMAGES.md` 图片展示页。开启下载模式后，会下载排行榜图片并打包为 ZIP，再通过 GitHub Actions 发布到 Release。

## 功能

- 自动抓取 Pixiv 日榜 Top 50
- 生成 `IMAGES.md` 图片预览
- 支持下载图片到本地
- 支持将每日图片打包为 ZIP
- GitHub Actions 每日定时运行
- 自动发布 Release，保留近 7 天的图片包
- 支持代理、请求超时和重试配置

## 每日图片

[查看今日图片](./IMAGES.md)

## 下载图片包

[Releases 页面](https://github.com/Xiacqi1/pixiv-daily/releases) 可以下载近 7 天的每日图片 ZIP 包。

## 本地运行

### 1. 克隆项目

```bash
git clone https://github.com/Xiacqi1/pixiv-daily.git
cd pixiv-daily
```

### 2. 编译

项目使用 Java 17 和 Maven。

```bash
mvn clean package
```

编译后会生成可执行 JAR：

```text
target/pixiv-daily-jar-with-dependencies.jar
```

### 3. 只更新 IMAGES.md

```bash
java -jar target/pixiv-daily-jar-with-dependencies.jar
```

### 4. 更新 IMAGES.md 并下载、打包图片

```bash
java -jar target/pixiv-daily-jar-with-dependencies.jar --download
```

下载模式会将图片临时保存到 `daily-images/`，再打包到 `releases/` 目录下。

> 注意：当前程序只会在 API 返回的排行榜日期为昨日时执行下载和打包，避免重复打包非最新榜单。

### 5. 使用代理运行

国内环境如果无法直连 Pixiv，可以为 JVM 配置 HTTPS 代理：

```bash
java -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=10808 -jar target/pixiv-daily-jar-with-dependencies.jar --download
```

## 环境变量

| 变量名 | 默认值     | 说明 |
| --- |---------| --- |
| `PIXIV_API_URL` | --      | Pixiv 排行榜 API |
| `IMAGE_COUNT` | `50`    | 抓取图片数量 |
| `CDN_DOMAIN` | --      | 图片 CDN 反代域名，用于替换 `i.pximg.net` |
| `HTTP_TIMEOUT_MS` | `10000` | HTTP 请求超时时间，单位毫秒 |
| `MAX_RETRIES` | `3`     | HTTP 请求最大重试次数 |

### Windows CMD 示例

```cmd
set PIXIV_API_URL=https://你的镜像地址/ranking.php
set IMAGE_COUNT=30
java -jar target/pixiv-daily-jar-with-dependencies.jar --download
```

### Windows PowerShell 示例

```powershell
$env:PIXIV_API_URL="https://你的镜像地址/ranking.php"
$env:IMAGE_COUNT="30"
java -jar target/pixiv-daily-jar-with-dependencies.jar --download
```

## GitHub Actions

仓库内置 GitHub Actions 工作流，默认每天运行一次：

1. 使用 Maven 编译项目
2. 运行程序并更新 `IMAGES.md`
3. 下载图片并生成 ZIP 包
4. 自动提交 `IMAGES.md`
5. 创建 Release 并上传 ZIP
6. 清理 7 天前的旧 Release

自动发布 Release 需要仓库配置 `MY_GIT_TOKEN` Secret，并授予发布 Release 所需权限。

## 主要改动

相对原项目，本项目主要改动如下：

- 重构代码结构，拆分配置、HTTP 请求、Markdown 生成、文件下载和打包逻辑
- 新增 `--download` 参数，支持下载图片到本地
- 新增每日图片 ZIP 打包功能
- 新增 GitHub Actions 自动发布 Release
- 新增旧 Release 自动清理逻辑，只保留近 7 天
- 优化并发下载性能
- 优化日志输出和 HTTP 重试配置

## 原项目

本项目 Fork 自 [gxywy/pixiv-daily](https://github.com/gxywy/pixiv-daily)，感谢原作者。

