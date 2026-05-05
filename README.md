# MiNotes

这是一个基于 MiCode/Notes 的 fork 优化项目，目标是让旧版 MiCode 便签可以直接在现代 Android Studio 或 IntelliJ IDEA 中继续开发。

原始项目作者与来源：
- MIUI Team 提供最初代码与项目起点
- MiCode Open Source Community 维护原始开源项目
- 原始仓库：MiCode/Notes

许可信息：
- 本项目沿用原项目许可证，详见 NOTICE

## 开发环境

- Android Studio 最新稳定版，或 IntelliJ IDEA + Android 插件
- JDK 17
- Android SDK Platform 35
- Gradle 会通过 Wrapper 自动使用 8.10.2

## 如何使用

1. clone 仓库
2. 用 Android Studio 或 IntelliJ IDEA 直接打开项目根目录
3. 等待 Gradle Sync 完成
4. 如果 IDE 提示安装缺失的 Android SDK 组件，直接按提示安装
5. 运行 app 模块即可开始开发

首次导入说明：
- local.properties 不随仓库提交，IDE 会根据本机 Android SDK 自动生成或提示配置
- 项目已使用 Gradle Wrapper、Kotlin DSL 和 AndroidX，打开后不需要再做旧版 Eclipse/ADT 迁移

## 命令行

构建 Debug 包：

```bash
./gradlew :app:assembleDebug
```

## 项目状态

- 已整理为标准 Android Gradle 工程
- 可直接在 Android Studio / IntelliJ IDEA 中导入
- 已适配 Java 17、AndroidX、Material 和现代 Gradle 工作流
- app 模块面向 JDK 17、compileSdk/targetSdk 35
- 界面使用 Google Material Design 组件与 Material 3 DayNight 主题，可跟随系统明暗模式
- 保留本地便签、搜索、提醒、桌面组件、导出和文件夹功能；文件夹支持创建、重命名、删除，并可将多条便签批量移动到同一文件夹
