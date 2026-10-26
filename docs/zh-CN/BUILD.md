# 构建与开发（中文辅助）

> 对应英文文档：[../BUILD.md](../BUILD.md)。两者冲突时以英文为准。

## 环境要求

| 工具 | 本项目使用的版本 |
| --- | --- |
| JDK | 25 |
| Android SDK | compile SDK 37，构建工具由 AGP 9.2.1 决定 |
| Gradle | 9.5.1（由 wrapper 提供，无需手动安装） |
| Android Gradle Plugin | 9.2.1 |
| Kotlin | 2.3.21 |

支持 Windows、macOS、Linux，不强制要求 Android Studio。

## 目录关系

```text
PipeMusicClient/       # Android 应用
PipeMusicExtractor/    # 解析引擎（复合构建）
```

`PipeMusicClient/settings.gradle` 中有 `includeBuild('../PipeMusicExtractor')`，两个目录必须同级。
外层检出目录名可以是 `PipeMusic` 或 `PipePipe`，不影响构建。

## 配置 SDK

若 SDK 不在默认位置，新建 `PipeMusicClient/local.properties`：

```properties
sdk.dir=/绝对路径/到/Android/Sdk
```

该文件已被 Git 忽略，切勿提交。缺少 platform 或 build-tools 时用 sdkmanager 补齐，不要降低
`compileSdk`：

```bash
sdkmanager "platforms;android-37" "build-tools;37.0.0"
```

## 构建

```bash
cd PipeMusicClient
./gradlew :app:assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
```

产物位于 `PipeMusicClient/app/build/outputs/apk/debug/`：

```text
PipeMusic_1.0.0-universal-debug.apk
PipeMusic_1.0.0-arm64-v8a-debug.apk
PipeMusic_1.0.0-armeabi-v7a-debug.apk
PipeMusic_1.0.0-x86-debug.apk
PipeMusic_1.0.0-x86_64-debug.apk
```

debug 包使用 Android 标准调试密钥签名，可直接安装，applicationId 为
`InfinityLoop1309.NewPipeEnhanced.debug`。

## release 构建

```bash
./gradlew :app:assembleRelease
```

只有同时提供四个签名环境变量才会签名：

```bash
export KEY_PATH=/path/to/keystore.jks
export KEY_STORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
```

否则产出的 APK **未签名，无法安装**。

## 测试与静态检查

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

## 常见错误

- **`SDK location not found`**：创建 `local.properties` 并填写 `sdk.dir`。
- **`Unsupported class file major version`**：JVM 低于 Java 25，把 `JAVA_HOME` 指向 JDK 25。
- **解析不到 `PipeMusicExtractor`**：确认 `../PipeMusicExtractor` 与客户端同级存在。
- **资源合并失败**：多为字符串资源格式错误，加 `--stacktrace` 查看具体文件。
- 首次构建较慢，需要下载 wrapper、工具链与各依赖。
