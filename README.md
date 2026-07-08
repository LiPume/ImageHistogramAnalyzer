# ImageHistogramAnalyzer

基于 Android 的移动端图像质量分析与直方图计算优化系统，对应课程课题“图像直方图计算及性能优化”。项目计划使用 Kotlin 与 Jetpack Compose，自主实现图片读取、灰度统计、256×100 黑白直方图、性能统计和基础图像质量分析。

## 当前状态

- 阶段：MVP、基础功能和 UI 体验阶段已完成；v3.0 Native 引擎已在 Xiaomi 14 单次测试中达到 300ms 目标，当前进入最终真机多轮验收与交付文档整理。
- App 已完成 Compose 启动页、系统选图、图片预览、分辨率/像素信息、两种灰度统计方案、256×100 Canvas 直方图和耗时展示。
- 已增加平均灰度、暗部/亮部占比、灰度标准差和偏暗/偏亮/低对比度/正常质量卡片；指标只读取 256 个直方图频次。
- Xiaomi 真机的系统图片选择器已验证支持现场拍照并返回分析，无需额外相机或相册权限。
- 页面支持完整纵向滚动；选图后不会自动计算，需要选择“优先灰度化”或“统计时灰度化”，再点击“计算并绘制直方图”。
- v3.0 已加入 NDK/C++ Native 主引擎：直接锁定 Bitmap，取消 Java 整图彩色像素复制；两方案按行分块、线程私有 256-bin 后归并，并保留 Kotlin v2 回退。性能卡片会显示执行引擎和线程数。
- JVM 测试、四 ABI Native 构建、Lint、常规仪器测试和 API 36 模拟器多轮基准已通过。12.58MP 真机单次结果为 56.957ms / 22.244ms，均显著低于 300ms；最终报告前仍需固定原图补充多轮中位数/P90，但暂不继续 NEON 或第三方库优化。

本地五张真实测试图片放在 `test_pic/`（含人物，不上传公开仓库）。安装到指定模拟器：

```bash
ANDROID_SERIAL=emulator-5554 ./scripts/install_test_images.sh
```

详细计划见：

- `docs/final/README.md`（最终交付文档总入口）
- `docs/Android图像直方图系统开发计划.md`
- `docs/迭代计划与TODO.md`
- `docs/协作规范.md`
- `docs/开源项目技术借鉴记录.md`
- `docs/性能优化数据表.md`

## 项目专用 Agent Skill

项目级技能位于：

```text
.github/skills/image-histogram-android/
```

它约束 AI 助手遵循本项目的单模块架构、课程范围、灰度公式、256×100 输出、性能证据和文档同步要求。Codex 可使用 `$image-histogram-android` 显式调用；其他支持 Agent Skills 的工具可直接读取该目录中的 `SKILL.md`。

配套工作流技能：

- `$android-test-gate`：每完成一段代码，选择并执行匹配的分层测试。
- `$project-doc-sync`：根据真实实现与测试证据同步 TODO、设计、测试和使用文档。
- `$project-git-checkpoint`：在任务、阶段和交接节点建立安全、可回退的 Git 检查点。

## 获取外部参考仓库

外部项目仅供本地技术调研，不属于本项目源码，也不会被 Git 提交。首次使用或需要更新时执行：

```bash
./scripts/fetch_references.sh
```

脚本会将图像直方图参考项目和 Android Agent Skills 素材库放在 `references/open_source/`。禁止把其中代码直接复制到 `app/src/main`，也不要把外部技能整套复制进本项目。

## 开发与文档并行原则

1. 每完成一个功能模块，必须同步更新对应文档。
2. 每次重要代码变更后，需要在 TODO 文档中更新状态。
3. 提交信息应清楚描述变更类型，例如：
   - `feat: add image picker`
   - `feat: implement baseline histogram`
   - `docs: update project plan`
   - `test: add histogram unit test`
   - `perf: optimize bitmap pixel reading`
4. `main` 分支必须保持可运行。
5. 不允许多人同时大幅修改 `MainActivity.kt`、`build.gradle.kts`、`AndroidManifest.xml`。
6. 算法、UI、文档、测试按模块协作，避免职责混杂。
7. 每个阶段结束时提交一次阶段性 commit。
8. 不加入登录、注册、数据库、后端、云服务等无关功能。
9. 先保证基本功能能在手机上运行，再进行性能优化。

## 提交前检查

```bash
git status --short
./gradlew test
./gradlew assembleDebug
```

提交前确认没有纳入 `build/`、`.gradle/`、`local.properties` 或 `references/open_source/`。

显式运行模拟器性能基准：

```bash
ANDROID_SERIAL=emulator-5554 ./scripts/run_emulator_benchmark.sh
```
