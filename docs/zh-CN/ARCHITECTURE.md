# 架构说明（中文辅助）

> 对应英文文档：[../ARCHITECTURE.md](../ARCHITECTURE.md)。两者冲突时以英文为准。

## 模块

```text
PipeMusicClient          Android 应用：Compose 界面、播放服务、Room 数据库
PipeMusicExtractor       多平台解析引擎（Gradle 复合构建）
ffmpeg                   内置 FFmpeg，仅在只有混合流时用于提取音轨
```

音乐实现独立成包：

```text
PipeMusicClient/app/src/main/java/org/schabi/newpipe/music/
├── data/        # Room 数据库、DAO、Repository、偏好、语言、账号
├── local/       # 文件夹扫描、元数据读取、导入 Worker
├── playback/    # 队列、播放服务、控制器、UI 状态
├── stream/      # 搜索、音频流选择、下载 Worker
└── ui/          # Compose 主题、页面、组件、ViewModel
```

## 数据流

```text
Compose 界面 -> MusicRepository -> Room / DataStore / WorkManager
Compose 界面 -> MusicPlayerController -> MusicPlayerService -> ExoPlayer
下载与播放 -> PipeMusicExtractor -> 各平台服务
```

## 数据层

`MusicDatabase`（`music.db`）是独立于旧 NewPipe 数据库的 Room 库，避免与上游实体产生复杂迁移。

| 实体 | 用途 |
| --- | --- |
| `Track` | 歌曲：来源信息、本地路径、下载状态、收藏、播放次数 |
| `Playlist` | 歌单 |
| `PlaylistTrack` | 歌单与歌曲的有序关联表 |
| `PlayHistory` | 最近播放 |
| `SearchHistory` | 搜索历史 |

`MusicRepository` 是曲库、歌单、收藏、历史与下载状态的统一入口；`MusicPreferences` 用 DataStore
保存主题、音质、循环模式、随机与上次队列快照。

## 播放

- `MusicPlayerService` 持有唯一 ExoPlayer 与 `MediaSessionCompat`，负责音频焦点、耳机拔出暂停、
  前台通知、锁屏控制、媒体按键与睡眠定时。
- `PlaybackQueue` 为纯 Kotlin 逻辑，维护列表、当前索引、随机顺序与循环模式，并有单元测试覆盖。
- `MusicPlayerController` 绑定服务并以 `StateFlow` 暴露状态；播放位置节流约 500ms，避免整页重组。
- 队列会被持久化，服务重启后恢复队列但保持暂停。

## 搜索与取流

`MusicSearchRepository` 只把流条目映射为歌曲，支持分页、输入防抖、切换平台时取消旧请求；粘贴
链接先按 URL 识别服务再取流。`AudioStreamResolver` 优先选择最高码率的纯音频流，没有纯音频流时回退
混合流，下载阶段用 FFmpeg 提取音轨；链接过期时重新解析一次并恢复播放位置。

## 下载与导入

- `AudioDownloadWorker`：在 Worker 内重新解析链接，先写临时文件，成功后原子重命名，状态与进度写入
  `Track`，界面直接观察 Room。
- `LocalImportWorker`：通过存储访问框架授权目录，递归扫描并流式复制到私有 `files/music/`，用
  `MediaMetadataRetriever` 提取元数据与内嵌封面，按 sourceUri 去重，支持进度、取消与失败汇总。

所有文件限制在私有 `music` 与 `artwork` 目录，删除操作校验 canonical path。

## 界面

单 `MusicMainActivity` 承载 Compose 树，用密封路由与返回栈导航（未使用 Navigation Compose）。页面包括
发现、搜索、曲库、歌单、歌单详情、我的，以及常驻迷你播放器与全屏播放页。`MusicTheme` 与
`ColorSchemeFactory` 提供 8 套预设、自定义取色、明暗/纯黑、动态取色与封面取色，色板按种子色缓存。

## 入口隔离

- launcher 指向 `MusicMainActivity`；旧 `MainActivity` 不导出且无启动入口。
- `RouterActivity` 拦截 `VIEW`／`SEND`，提取 URL 后交给音乐界面。
- 旧视频播放服务不主动启动，避免争抢媒体会话。

## 日志与隐私

统一使用 `MusicPlayer`、`MusicSearch`、`MusicImport`、`MusicDownload` 四个 Tag，只记录服务 ID、轨道 ID
与错误类型，不记录 Cookie、带签名的流地址或用户文件路径。
