# PipeMusic（中文说明）

> 本文件是辅助说明，仅用于帮助中文读者快速了解项目。**正式文档以英文为准**，请以
> [README.md](README.md) 与 [docs/](docs/README.md) 下的英文文档为权威来源。

## 项目简介

PipeMusic 是一款以“听歌”为核心的 Android 应用，基于 PipePipe 代码库改造而来。它保留了
PipePipe 的多平台解析与下载内核，但去掉了面向视频的界面，改为专注的音乐体验：搜索音乐、
在线播放、下载离线收听、导入本地文件夹、管理歌单，并提供主题个性化。

```text
NewPipe  ->  PipePipe  ->  PipeMusic
```

- **NewPipe**：最初的自由开源流媒体客户端。
- **PipePipe**：NewPipe 的功能增强分支，提供多平台解析器、下载基础设施与旧版 Android 客户端。
- **PipeMusic**：独立的音乐分支，复用 PipePipe 的解析与下载能力，客户端则是全新的 Compose
  音乐播放器；视频播放、订阅、评论与频道页不在产品界面中暴露。

PipeMusic 与 PipePipe、NewPipe 项目没有隶属或背书关系，上游版权声明与 GPL-3.0 许可证均予以保留。

## 主要功能

- 跨平台搜索：YouTube、SoundCloud、Bandcamp、BiliBili、Niconico、PeerTube、media.ccc.de 等解析器
  支持的平台；支持分页、切换平台、粘贴链接解析。
- 在线播放与队列：顺序播放、列表循环、单曲循环、随机播放，可编辑播放队列。
- 音频下载：下载到应用私有音乐库，显示进度、支持重试；仅有混合流时用 FFmpeg 提取音轨。
- 本地导入：通过系统文件夹选择器递归导入音频，复制到私有目录，读取标题、艺术家、专辑、时长
  与内嵌封面，支持进度、取消与去重。
- 歌单与收藏：创建、重命名、删除歌单，添加、移除、排序歌曲；收藏与最近播放。
- 播放控制：后台播放、媒体通知、锁屏控制、耳机按键、睡眠定时。
- 外观：8 套预设主题、自定义主色、浅色/深色/跟随系统/纯黑、Android 12+ 动态取色、封面取色。

## 下载与构建

- 预编译 APK 见 [Releases](https://github.com/maoruiQa/PipeMusic/releases)：不确定设备架构时选
  `universal`，多数现代手机选 `arm64-v8a`。
- 从源码构建：需要 JDK 25、Android SDK（compileSdk 37），在 `PipeMusicClient` 目录执行
  `./gradlew :app:assembleDebug`（Windows 用 `.\gradlew.bat`）。详见
  [docs/BUILD.md](docs/BUILD.md)。

## 目录结构

```text
PipeMusicClient/       # Android 应用：Compose 界面、播放服务、Room 数据库
PipeMusicExtractor/    # 多平台解析引擎（Gradle 复合构建）
fastlane/              # 商店元数据
docs/                  # 英文文档（docs/zh-CN 为中文辅助）
```

音乐实现位于 `PipeMusicClient/app/src/main/java/org/schabi/newpipe/music/`，分为
`data`、`local`、`playback`、`stream`、`ui` 五个子包。

## 已知限制

- 部分平台需要登录、Cookie、住宅 IP 或人机验证，播放与下载可用性随网络与地区变化。
- 下载与导入的文件保存在应用私有目录，卸载应用会一并删除，重要文件请自行备份。
- 发布包为基于源码构建的 debug 签名 APK，不是 PipePipe 或 NewPipe 的官方发布。

## 中文文档索引

| 文档 | 内容 |
| --- | --- |
| [docs/zh-CN/README.md](docs/zh-CN/README.md) | 中文文档索引 |
| [docs/zh-CN/BUILD.md](docs/zh-CN/BUILD.md) | 环境要求、SDK 配置、Gradle 任务与排错 |
| [docs/zh-CN/ARCHITECTURE.md](docs/zh-CN/ARCHITECTURE.md) | 模块、数据层、播放、后台任务与界面结构 |
| [docs/zh-CN/FEATURES.md](docs/zh-CN/FEATURES.md) | 页面、播放、下载、导入、歌单与主题 |
| [docs/zh-CN/FAQ.md](docs/zh-CN/FAQ.md) | 常见问题与隐私说明 |
| [docs/zh-CN/RELEASE.md](docs/zh-CN/RELEASE.md) | 版本号、APK 变体与发布流程 |

## 许可证

GPL-3.0，详见 [LICENSE](LICENSE)。
