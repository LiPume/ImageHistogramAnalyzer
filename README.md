# ImageHistogramAnalyzer

基于 Android 的移动端图像质量分析与直方图计算优化系统，对应课程课题“图像直方图计算及性能优化”。项目计划使用 Kotlin 与 Jetpack Compose，自主实现图片读取、灰度统计、256×100 黑白直方图、性能统计和基础图像质量分析。

## 当前状态

- 阶段：阶段 0——项目治理初始化。
- Android 工程目前可以编译，但还没有 Launcher Activity 和业务页面，暂不能作为完整应用启动。
- 当前只建立版本控制、参考资料、协作规范与迭代计划，尚未实现图片选择、直方图算法或 UI 页面。

详细计划见：

- `docs/Android图像直方图系统开发计划.md`
- `docs/迭代计划与TODO.md`
- `docs/协作规范.md`
- `docs/开源项目技术借鉴记录.md`

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
