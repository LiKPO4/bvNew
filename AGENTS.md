# AGENTS.md

## 项目概述

BV 是哔哩哔哩第三方 Android 应用，同时支持 Mobile 和 TV（Jetpack Compose）。

## 模块结构

- `app/` - 主应用（包含 `mobile/`、`tv/`、`shared/` 子模块）
- `bili-api/` - API 层（包含 `grpc/` 子模块用于 protobuf 生成代码）
- `bili-subtitle/` - 字幕解析库（有单元测试）
- `player/` - 播放器（包含 `core/`、`mobile/`、`tv/`、`shared/` 子模块）
- `utils/` - 通用工具
- `symbols/` - 符号相关
- `libs/` - 预编译的解码器 AAR（av1Decoder、ffmpegDecoder、libVLC）
- `buildSrc/` - Gradle 构建配置

## 构建命令

```sh
# Debug 构建（生成 universal apk）
./gradlew assembleDefaultDebug

# Release 构建（需要 signing.properties）
./gradlew assembleRelease

# Alpha 构建
./gradlew assembleDefaultAlpha

# 运行单元测试
./gradlew testDefaultDebugUnitTest

# 运行单个模块测试
./gradlew :bili-subtitle:test

# 运行 lint
./gradlew lintDefaultDebug
```

## 构建前提

1. **JDK 21** - 必须使用 JDK 21
2. **Android SDK** - `local.properties` 中配置 `sdk.dir`
3. **Release 构建** - 根目录需要 `signing.properties` 文件

## 版本号

- `versionCode` = git commit 数量 (`git rev-list --count HEAD`)
- `versionName` = `major.minor.patch` + git short hash
- preBuild 任务会自动下载 blacklist.bin 到 `app/shared/src/main/res/raw/`

## Protobuf

- proto 文件位于 `bili-api/grpc/proto/`
- `ProtobufConfiguration.kt` 定义了使用的 proto 文件集合，未使用的会被排除
- 生成代码任务自动运行，通常不需要手动触发

## 框架和库

- **DI**: Koin（使用 KSP 注解处理器）
- **数据库**: Room（使用 KSP）
- **网络**: Ktor Client + OkHttp
- **序列化**: Kotlinx Serialization
- **播放器**: Media3 + libVLC

## CI 工作流

- `develop` 分支 → Alpha Build
- `feature/*` 分支 → Feature Build
- `release` 标签 → Release Build

## 开发注意事项

- Kotlin 代码风格使用 `official`
- AndroidX 包结构启用
- Compose Compiler Reports 输出到 `build/compose_build_reports`
- 编译类型 `r8Test` 用于测试 R8 混淆
