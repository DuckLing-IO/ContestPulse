# 发布检查表

## 自动化

- [ ] `testDebugUnitTest` 通过。
- [ ] `lintDebug` 通过且检查报告无未解释错误。
- [ ] `assembleDebugAndroidTest` 通过。
- [ ] `assembleRelease` 与 R8 shrink 通过。
- [ ] Room schema JSON 已提交，数据库版本与迁移策略一致。
- [ ] Release 包内无演示数据源、调试日志、密钥或本机路径。

## 真机矩阵

- [ ] Android 8/10：基础同步、收藏、日历和通知。
- [ ] Android 12：精确闹钟允许/拒绝两种路径。
- [ ] Android 13/14：通知权限允许、拒绝、设置中重新开放。
- [ ] 重启、应用覆盖升级、手动改时间和切换时区后提醒恢复。
- [ ] Wi-Fi-only、移动网络、无网络有缓存、无网络无缓存。
- [ ] 四个平台分别单独失败、部分失败与全部失败，均验证旧缓存和其他来源不受影响。
- [ ] 自定义 JSON、ICS、HTML 映射各验证一次；错误 CSS、改版页面和单源失败均保留其他来源。
- [ ] 自定义来源拒绝 HTTP、localhost、局域网地址、私网重定向和超过 1 MB 的响应。
- [ ] 浅色、深色、大字体、TalkBack 与系统减少动画。

## 合规与商店

- [ ] 按发布时要求升级 target SDK 并完成行为回归。
- [ ] 审核 `SCHEDULE_EXACT_ALARM` 的商店政策适用性；不满足时移除并仅保留非精确提醒。
- [ ] 隐私说明与数据安全表准确描述“无账号、无上传、本地保存”。
- [ ] 平台名称只用于来源识别，不使用暗示官方合作的文案或资产。
- [ ] 使用真实设备采集首页、详情、筛选、提醒与设置的浅色/深色截图。
- [ ] 使用正式签名生成 AAB；签名文件与口令不得进入仓库。

## GitHub Release APK 更新

- [ ] `app/build.gradle.kts` 中的 `versionName` 已改为本次发布版本，`versionCode` 比上一版严格递增。
- [ ] 创建正式（非 Draft、非 Pre-release）GitHub Release，标签使用 `vX.Y.Z`，并与 `versionName` 一致，例如 `v1.1.0`。
- [ ] 在 Release 中上传正式签名的 APK，文件名以 `.apk` 结尾，且不包含 `debug`；推荐命名为 `ContestPulse-vX.Y.Z.apk`。
- [ ] 可选：上传同名校验文件 `ContestPulse-vX.Y.Z.apk.sha256`，内容为该 APK 的 SHA-256；客户端会在存在该文件时验证下载结果。
- [ ] 在保留旧版数据的真实设备上，通过“设置 → 应用更新 → 检查更新”下载并走完系统安装确认；确认收藏、设置和提醒均仍存在。
