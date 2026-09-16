# SkipSkip

学习向的 Android 开屏「跳过」助手：通过系统无障碍服务识别界面上的跳过按钮并模拟点击。

> 仅用于本人设备上的调试与学习，不是商业广告拦截产品，也不提供李跳跳式自定义规则导入。

## 功能

- 精确匹配「跳过 / 跳过广告 / 关闭广告」（去掉倒计时噪音后整词比较）
- 应用切到前台后约 5 秒内响应内容变化，减少日常刷列表时的扫描
- 排除列表：指定应用不自动跳过
- 建议：忽略电池优化、最近任务加锁

## 环境

- Android Studio / JDK 17+
- minSdk 26，targetSdk 36
- Kotlin + Jetpack Compose

## 本地运行

```bash
./gradlew :app:assembleDebug
```

用 Android Studio 打开工程后，连真机安装即可。首次使用需在系统设置里开启 SkipSkip 无障碍服务。

本地 `assembleDebug` 使用本机 debug 签名，与 GitHub Release 的 release 签名不同，不能互相覆盖安装。

## 发版（GitHub Actions）

推送形如 `v0.0.1` 的 tag 后，Release workflow 会：

1. 运行测试并打包 **release 签名** APK  
2. 上传到 GitHub Release（`v0.*` 会标为 prerelease）

仓库需配置 Actions Secrets（密钥本身不要提交到本仓库）：

| Secret | 说明 |
|--------|------|
| `SIGNING_KEYSTORE_BASE64` | keystore 文件的 base64 |
| `SIGNING_STORE_PASSWORD` | 仓库密码 |
| `SIGNING_KEY_PASSWORD` | 密钥密码 |
| `SIGNING_KEY_ALIAS` | 密钥别名 |

本地若要用同一把钥匙打包，可复制 `keystore.properties.example` 为 `keystore.properties` 并填入路径与密码（该文件已在 `.gitignore` 中）。

## 说明

- 无障碍权限敏感，请只在自己的设备上开启，并知晓其能力边界  
- 纯图片、无障碍树里没有文案的跳过按钮可能点不到——这是当前产品边界  
- 欢迎提 Issue / PR；请保持学习向、小而清晰的范围
