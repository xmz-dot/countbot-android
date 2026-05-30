# CountBot Android App

CountBot 的手机客户端，基于 WebView 封装，加载 CountBot 的 Web 界面。

## 功能特性

- **WebView 封装**：全屏加载 CountBot Web 界面
- **下拉刷新**：SwipeRefreshLayout 支持下拉刷新页面
- **加载进度条**：顶部显示页面加载进度
- **双指缩放**：支持手势缩放网页内容
- **深色模式**：自动跟随系统深色/浅色主题
- **网络错误处理**：无网络时显示友好提示和重试按钮
- **返回键导航**：支持 WebView 历史后退
- **状态保存**：屏幕旋转等配置变更时保存 WebView 状态

## 构建方法

### 环境要求

- JDK 11+
- Android SDK (compileSdk 34)
- Gradle 8.2

### 本地构建

```bash
# 克隆项目
git clone <repository-url>
cd CountBot-Android

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/` 目录下。

### GitHub Actions 自动构建

项目配置了 GitHub Actions，推送到 `main` 分支或手动触发即可自动构建 Debug 和 Release APK，并上传为构建产物。

## 配置说明

### 修改 CountBot 服务器地址

默认加载地址为 `http://127.0.0.1:8000`，修改以下文件中的 `countBotUrl` 变量：

```kotlin
// app/src/main/java/com/countbot/app/MainActivity.kt
private val countBotUrl = "http://127.0.0.1:8000"
```

例如改为远程服务器：

```kotlin
private val countBotUrl = "https://your-countbot-server.com"
```

### 权限说明

| 权限 | 用途 |
|------|------|
| `INTERNET` | 访问网络资源 |
| `ACCESS_NETWORK_STATE` | 检测网络连接状态 |

## 项目结构

```
android-project/
├── app/
│   ├── build.gradle.kts          # 模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── java/com/countbot/app/
│       │   └── MainActivity.kt   # 主 Activity
│       └── res/
│           ├── layout/           # 布局文件
│           │   ├── activity_main.xml
│           │   ├── view_error.xml
│           │   └── view_nav_bar.xml
│           ├── values/           # 字符串、颜色、主题
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           ├── values-night/     # 深色主题
│           │   └── themes.xml
│           ├── drawable/         # 矢量图标
│           │   ├── ic_launcher_foreground.xml
│           │   └── ic_launcher_background.xml
│           └── mipmap-*/         # 自适应图标
├── build.gradle.kts              # 项目级构建配置
├── settings.gradle.kts           # 模块管理
├── gradle.properties             # Gradle 属性
├── gradle/wrapper/               # Gradle Wrapper
├── .github/workflows/build.yml   # CI/CD 配置
└── README.md
```

## 技术栈

- **Kotlin** - 开发语言
- **AndroidX** - 支持库
- **Material Components** - UI 组件
- **WebView** - 网页渲染
- **ViewBinding** - 视图绑定
