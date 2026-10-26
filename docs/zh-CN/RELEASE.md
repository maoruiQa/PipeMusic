# 发布流程（中文辅助）

> 对应英文文档：[../RELEASE.md](../RELEASE.md)。两者冲突时以英文为准。

## 1. 版本号

版本定义在 `PipeMusicClient/app/build.gradle`：

```gradle
def baseVersionCode = 1
def appVersionName = "1.0.0"
```

`baseVersionCode` 单调递增；每个 ABI 变体的 `versionCode` 为 `100 * baseVersionCode + abiCode`，
保证设备总能收到适配自己的包。

## 2. 更新日志

写入 `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`，文件名为最低 ABI 版本号，即
`100 * baseVersionCode + 1`。GitHub Release 说明与之保持一致。

## 3. 构建

```bash
cd PipeMusicClient
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

产物在 `PipeMusicClient/app/build/outputs/apk/debug/`。发布时至少上传 `universal`、`arm64-v8a`
与 `armeabi-v7a` 三个包，基本覆盖所有手机。debug 包使用调试密钥签名，可直接安装。

## 4. 可选：签名 release 包

```bash
export KEY_PATH=/path/to/keystore.jks
export KEY_STORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

缺少变量时 release 包未签名，无法安装。密钥与密码务必妥善保管。

## 5. 打标签并发布

```bash
git tag -a v1.0.0 -m "PipeMusic 1.0.0"
git push origin v1.0.0
```

然后在 GitHub 上为该标签创建 Release 并上传 APK。发布前请在干净设备上确认能启动，且「我的 > 关于」
显示的版本与标签一致。

## 检查清单

- [ ] `baseVersionCode` 与 `appVersionName` 已更新
- [ ] fastlane 更新日志已添加
- [ ] 单元测试与 lint 通过
- [ ] APK 已构建并签名（调试密钥或发布密钥）
- [ ] 标签已推送
- [ ] Release 已发布并附带 APK
- [ ] 发布说明包含功能、修复与已知限制
