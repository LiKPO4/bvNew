# AGENTS.md

> 本文件为 AI 编码 Agent 提供项目速览。详细功能/修改清单见 [README.md](README.md)。

## 通用长期开发规则

> 通用长期开发规则。目标：小步、可验证、少废话、可交接。

## 沟通

- 始终用中文，每轮第一句必须说“你好霖江路”
- 信息不足时只问会阻塞当前任务的关键问题。
- 没有测试、日志、构建或运行证据，不说“已修复/已完成”。
- 同一思路连续失败 2 次，停下分析根因；反复卡住时把选择权交给用户。

## 开工

- 先读项目规则、README、关键文件和当前状态，再动手。
- 以仓库实际代码为准，优先搜索确认，别凭记忆改。
- 默认每轮只做一个可验证的最小增量。
- 优先复用现有架构、命名和目录，不轻易新建并行体系。
- 发现与当前任务无关的问题，记录为后续事项，不扩大本轮范围。

## 修改

- 只改必要文件，不做无关重构、格式化、清理。
- 新依赖、新脚本、新配置必须说明理由和回滚方式。
- 删除、覆盖、批量迁移、发布、强推、重置、清理历史等不可逆操作前先征得用户确认。
- 实现与用户原话存在数值、范围或语义偏差时，必须说明。

## Git

- **默认流程（无需逐轮询问）**：每轮有实际改动后，直接本地提交一次 git；**不推云端、不打 tag、不发 Release**。版本号在**软件本体确实有更新**时递增**小版本（patch）**（`src/`、`electron/`、构建配置、依赖等会影响产物的改动），只改 `package.json` 一处（`appVersion.ts` 自动跟随）；纯文档、规则、知识库、过程记录类改动（如 `docs/`、`knowledge/`、`AGENTS.md`、`TASK_STATE.md`）**不递增版本**。推送远端、打 tag、发布 Release 必须等用户明确要求。Agent 按此默认执行即可，不要每轮再问“要不要提交”。
- 即使改动很小，也要先提交再继续下一步，避免丢失内容。
- 提交前必须查看 `git status` 和相关 `git diff`，确认只包含本轮改动。
- 只 stage 本轮相关文件，避免 `git add .` 收进无关改动。
- 未通过验收、分支不明确或风险明显时不提交。
- 不提交密钥、缓存、构建产物、临时文件和个人笔记。
- **提交信息必须使用中文**（标题与正文均中文）。

## 子代理协作

- 大型审查/修复任务可拆给多个子代理并行；拆分时按**文件归属**分区，两个并行子代理不得改同一文件（即使不同区域，行尾/内容匹配也会互相干扰）。
- 调用 Agent / AgentSwarm 时，默认不传 model 参数或显式指定 `"model": "secondary"`；除非用户明确要求主模型执行，否则禁止传 `"model": "primary"`。
- 子代理只做定向测试和 typecheck；**禁止 `pnpm build`**（写 `out/` 会互相冲突）、**禁止任何 git 操作**。全量测试、构建、提交由主代理统一收尾。
- 子代理报告中的关键结论（行号、根因、“已验证”声明）必须抽查复核后再采信；其他 agent 的审查报告同样先证实/证伪再动手，不接受未核实的“严重问题”清单。

## 验收

- 有实际改动后，优先运行项目已有的测试、构建、lint、typecheck 或局部验证命令。
- 无法验证时说明原因，并告诉用户需要回传什么。
- 输出结论必须区分：已验证、未验证、部分验证。

## 每轮输出

每轮结束按这个格式，保持简短：

1. 本轮目标（如修复问题则为出现问题原因和修复方式）
2. 变更文件
3. 验收结果
4. 风险与回滚
5. 下一步最小行动

## 长程状态

长期任务维护 `TASK_STATE.md`，只记录会影响后续工作的内容：

- 当前目标
- 已完成
- 未完成
- 阻塞
- 关键文件
- 下一步

## 交接

用户说“checkpoint / 总结一下 / 准备换人 / 交接”时，输出可直接发给下一个 agent 的摘要：

- 项目背景
- 当前目标
- 已完成
- 未完成
- 阻塞
- 关键文件/命令
- 下一步最小行动

## 项目概述

BV（B 站第三方 Android 客户端），fork 自 [aaa1115910/bv](https://github.com/aaa1115910/bv)，适配 Android Mobile 与 Android TV，使用 **Jetpack Compose**。项目在原 fork 基础上做了大量个人化修改（详情见 README），包括首页标签整合、自定义导航排序、UGC/PGC 详情评论、播放器控制条扩展、合集/分P 跳转、互动视频、画面旋转、弹幕引擎重写等。

## 模块结构

- `app/` - 主应用
  - `app/mobile/` - 移动端入口
  - `app/tv/` - TV 端入口
  - `app/shared/` - 共享业务（共用 ViewModel、Repository、Prefs、`VideoPlayerV3ViewModel`）
- `bili-api/` - HTTP + gRPC API
  - `bili-api/grpc/` - Protobuf 生成代码（含 `proto/` 源文件）
- `bili-subtitle/` - 字幕解析库（含单元测试）
- `player/` - 播放器
  - `player/core/` - 播放器核心（基于 Media3 + ffmpegDecoder 解码器）
  - `player/mobile/` - 移动端播放器实现
  - `player/tv/` - TV 端播放器实现
  - `player/shared/` - 共享播放器逻辑（含**自定义弹幕引擎** `danmaku/`）
- `utils/` - 通用工具
- `symbols/` - 共享 Compose/Symbol 资源
- `libs/` - **预编译 AAR**（非 Maven 依赖）：`av1Decoder`、`ffmpegDecoder`、`libVLC`、`media3Container`（详见 [libs/README.md](libs/README.md)）
  - 实际集成进 player/core 的只有 `ffmpegDecoder`；`av1Decoder`、`libVLC`、`media3Container` 在 `settings.gradle.kts` 登记但当前没有任何模块依赖，**不要**默认它们会被使用
  - libVLC 仅有 TV 端的可选运行时下载器（`app/tv/.../LibVLCDownloaderDialog.kt`），并未集成到播放器内核
- `buildSrc/` - Gradle build logic（`AppConfiguration.kt`、`ProtobufConfiguration.kt`）

## 包命名空间约定

所有模块 namespace 以 `AppConfiguration.appId`（`dev.aaa1115910.bv`）为根，按模块添加子包：

| 模块 | namespace |
|---|---|
| `app` | `dev.aaa1115910.bv` |
| `app/mobile` | `dev.aaa1115910.bv.mobile` |
| `app/tv` | `dev.aaa1115910.bv.tv` |
| `app/shared` | `dev.aaa1115910.bv`（与 `app` 同包） |
| `player` | `dev.aaa1115910.bv.player` |
| `player/core` | `dev.aaa1115910.bv.player.core` |
| `player/mobile` | `dev.aaa1115910.bv.player.mobile` |
| `player/tv` | `dev.aaa1115910.bv.player.tv` |
| `player/shared` | `dev.aaa1115910.bv.player.shared` |
| `utils` | `dev.aaa1115910.bv.utils` |

最终 `applicationId` 为 `AppConfiguration.applicationId`（`dev.aaa1115910.bv2`），debug 构建加 `.debug` 后缀，`r8Test` 加 `.r8test` 后缀。

## 构建命令

```sh
# Debug 构建（universal apk，applicationId 会变为 .dev.aaa1115910.bv2.debug）
./gradlew assembleDefaultDebug

# Release 构建（需要根目录 signing.properties）
./gradlew assembleRelease

# Alpha 构建（带 R8 混淆 + 签名）
./gradlew assembleDefaultAlpha

# R8 混淆测试构建
./gradlew assembleDefaultR8Test

# 单元测试
./gradlew testDefaultDebugUnitTest

# 单模块测试
./gradlew :bili-subtitle:test

# Lint
./gradlew lintDefaultDebug
```

## 构建前置

1. **JDK 21**（强制，`AppConfiguration.jdk`）
2. **Android SDK**：`local.properties` 中设置 `sdk.dir`
3. **compileSdk = 36**、**minSdk = 23**、**targetSdk = 36**（见 `AppConfiguration`）
4. **Release/Alpha/R8Test 构建**：根目录需 `signing.properties`（含 `keystore.path`、`keystore.pwd`、`keystore.alias`、`keystore.alias_pwd`）
5. **blacklist.bin**：preBuild 任务从 `blacklistUrl` 自动下载到 `app/shared/src/main/res/raw/`

## 版本号

在 `buildSrc/.../AppConfiguration.kt` 定义：

- `versionCode` = `git rev-list --count HEAD`（int）
- `versionName` = `major.minor.patch[.hotFix].r{versionCode}.{shortGitHash}`，例：`0.3.0.r1234.abcdef0`
- 修改版本号：改 `AppConfiguration` 的 `major`/`minor`/`patch`/`hotFix`

## 技术栈

- **DI**：Koin（KSP 注解处理器）
- **数据库**：Room（KSP）
- **网络**：Ktor Client + OkHttp
- **序列化**：Kotlinx Serialization
- **播放器**：Media3（已集成 ffmpegDecoder；libVLC/media3Container 库在 `libs/` 但未集成）
- **UI**：Jetpack Compose（Kotlin official 代码风格）
- **K2 Compiler** 已启用（`android.lint.useK2Uast=true`）

## Protobuf

- 源文件在 `bili-api/grpc/proto/`
- `buildSrc/.../ProtobufConfiguration.kt` 的 `usedProtoFiles` 控制**编译时包含**的 proto（未列出自动排除）
- 新增/移除 proto 后需要同步更新 `usedProtoFiles`
- 生成代码随构建自动执行，通常无需手动触发

## 弹幕（Danmaku）

**当前使用自研引擎**（`player/shared/src/main/kotlin/dev/aaa1115910/bv/player/danmaku/`），3 线程架构（Main/ActionThread/CacheThread），Choreographer 帧驱动。

- 历史背景与重构需求：[doc/弹幕/弹幕重构需求.md](doc/弹幕/弹幕重构需求.md)
- 性能优化记录：[doc/弹幕/弹幕库优化.md](doc/弹幕/弹幕库优化.md)
- Code review 报告：[doc/弹幕/弹幕code%20review%20报告.md](doc/弹幕/弹幕code%20review%20报告.md)
- **不要**引用 `akdanmaku` 模块或文件（已从 `settings.gradle.kts` 和各 `build.gradle.kts` 移除，部分注释残留是历史遗迹）
- 集成入口：`app/shared/.../viewmodel/VideoPlayerV3ViewModel.kt`
- 直播弹幕通过 WebSocket 接收（`LiveDataWebSocket`）
- 点播弹幕分片加载（6 分钟/段），15 秒轮询一次
- 当前**不支持发送弹幕**和弹幕评论

## CI / Git 工作流

- `develop` → Alpha Build
- `feature/*` → Feature Build
- `release` tag → Release Build

## 开发注意事项

- Kotlin 代码风格 `official`（已在 `gradle.properties` 配置）
- `nonTransitiveRClass=true` 已启用
- Compose Compiler Reports 输出到 `build/compose_build_reports`
- ProGuard 规则集中在各模块 `proguard-rules.pro` / `consumer-rules.pro`
- 偏好设置统一在 [app/shared/.../util/Prefs.kt](app/shared/src/main/kotlin/dev/aaa1115910/bv/util/Prefs.kt)

## 常见坑

- 修改 `AppConfiguration` 的版本号常量会立即影响所有变体，无需同步其他文件
- 调整 protobuf 后必须确认 `ProtobufConfiguration.usedProtoFiles` 同步更新，否则会被静默排除
- 弹幕引擎改动需同时关注 `player/mobile` 和 `player/tv` 两端的 `BvPlayer.kt`（分别集成 `DanmakuView` 和 `DanmakuLayer`）
- 依赖的 AAR 在 `libs/`，不是从 Maven 拉取的；版本变动需重新打包并替换
