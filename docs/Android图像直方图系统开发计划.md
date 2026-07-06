# 基于 Android 的移动端图像质量分析与直方图计算优化系统——开发计划

> 制定日期：2026-07-06  
> 依据文档：《选题1》《基于Android的移动端图像质量分析与直方图计算优化系统_项目选题与计划报告》  
> 当前阶段约束：本文件仅制定计划，不实施 Android 源码修改。

## 一、当前项目检查结果

- 技术栈：
  - 当前是单 `app` 模块的 Android Application 工程，使用 Kotlin DSL、Gradle 9.3.1、Android Gradle Plugin 9.1.1 和 Version Catalog。
  - AGP 已提供 Kotlin 编译任务，项目可作为 Kotlin 工程继续开发，但当前没有 Kotlin 业务源码。
  - 当前**不是已经配置完成的 Jetpack Compose 项目**：没有 Compose 构建开关、Compose BOM、`activity-compose`、Material 3、Compose UI/Tooling/Test 等依赖，也没有任何 `@Composable` 代码。
  - 当前 UI 主题仍为传统 View 体系的 `Theme.MaterialComponents.DayNight.DarkActionBar`。
  - SDK 配置为 `minSdk = 23`、`targetSdk = 36`、`compileSdk = 36.1`；Java 源/目标兼容级别为 11，Gradle Daemon Toolchain 为 JDK 21。
  - 上述 SDK、Gradle 和 JDK 组合在当前机器上能够完成构建，因此现有配置本身可用。后续接入 Compose 时需要统一 Compose、Lifecycle、Activity 与 Kotlin 编译配置。

- 项目结构：

```text
ImageHistogramAnalyzer/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/lzx/imagehistogramanalyzer/    # 当前为空
│       │   └── res/                                    # 启动图标、传统主题、备份配置
│       ├── test/.../ExampleUnitTest.kt                 # 模板测试
│       └── androidTest/.../ExampleInstrumentedTest.kt  # 模板测试
├── docs/
│   ├── 选题1.docx
│   └── 基于Android的移动端图像质量分析与直方图计算优化系统_项目选题与计划报告.docx
├── gradle/
│   ├── libs.versions.toml
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

- 是否可运行：
  - 已实际执行 `./gradlew :app:assembleDebug`，构建成功并生成 `app-debug.apk`。
  - 但当前 Manifest 中没有 Activity、Launcher Intent Filter，源码目录也没有 `MainActivity.kt`。所以 APK 只是可安装的空壳，Android Studio 会找不到默认启动页面，**不能作为有界面的应用直接运行**。
  - 结论：当前状态为“Gradle 可同步、APK 可编译；产品不可启动、功能尚未开始实现”。

- 可能存在的问题：
  - 项目选题报告声明使用 Kotlin + Jetpack Compose，但工程实际尚未接入 Compose，文档与实现状态不一致。
  - 缺少入口 Activity、应用页面、状态管理、图片读取、算法、绘制和业务测试。
  - 当前 Material Components/AppCompat 配置属于传统 View 技术栈；迁移 Compose 后应使用 Material 3 Compose 主题，避免两套 UI 体系混杂。
  - `compileSdk = 36.1` 能在当前机器构建，但组员机器必须安装相同 SDK；否则需要统一 SDK 安装清单，或经团队确认改用所有成员均已安装的稳定 compileSdk。
  - 当前只有示例测试，没有验证灰度取整、频次统计、归一化、算法一致性和 300ms 指标的测试。
  - 当前目录没有 Git 仓库信息，不利于多人协作、回退和验收版本固化；建议开发前初始化版本管理，但这不属于应用功能。
  - 课程评分要求重要代码有清晰注释。后续应对核心公式、归一化、计时边界、优化策略添加适量中文注释，同时避免对显而易见的 UI 代码堆砌注释。

## 二、功能目标拆分

### 图片选择模块

- 使用 Activity Result API 的系统图片选择器，优先 `PickVisualMedia`，不申请广泛存储权限。
- 接收 `content://` URI，校验 MIME、解码结果、宽高与像素规模；处理取消、无权限、文件损坏和超大图。
- 原图分析与预览图分开：预览允许采样，统计默认保持原始像素语义，不允许静默缩小后宣称为原图统计。
- 验收：Android 6.0 及以上可选择 JPEG/PNG/WebP；取消选择不崩溃；错误有明确中文提示。

### 图片预览模块

- 展示选中图片、文件名或 URI 摘要、分辨率、总像素数。
- 使用受控尺寸的预览 Bitmap，避免把全尺寸 Bitmap 长时间绑定到 Compose 状态导致内存压力。
- 验收：横图、竖图、透明 PNG 均能等比预览；切换图片时旧任务被取消，UI 不显示过期结果。

### 直方图计算模块

- 输入为 Bitmap/像素块，输出与 Android UI 无关的数据模型。
- 固定灰度公式：`gray = red * 0.299 + green * 0.587 + blue * 0.114`。
- 项目统一采用“四舍五入并限制到 0..255”的取整语义；基础和优化算法必须完全一致。若教师另有取整要求，只改算法策略和测试，不改 UI。
- 输出 256 个频次数值、最大频次、总像素数和归一化高度 `0..100`。
- 基础算法：逐像素读取，用于正确性参照和算法对比。
- 优化算法：`getPixels()` 分块批量读取、复用缓冲区、单次循环完成通道提取/灰度化/计数，运行在后台线程。
- 验收：黑、白、纯红、纯绿、纯蓝和人工构造小图结果符合预期；两种算法 256 个 bin 完全一致；频次总和等于有效像素数；最大值归一化为 100。

### 直方图绘制模块

- Compose Canvas 接收已经归一化的数据，只负责绘制，不在 `onDraw` 中计算像素统计。
- 逻辑坐标固定为横轴 256 bin、纵轴 0..100；屏幕上可等比缩放。为课程验收准备 256×100 像素离屏结果校验，保证“256×100”不是仅写在文案里。
- 采用白底黑柱（或黑底白柱，但全项目统一）并标注 0、128、255 与最大频次。
- 验收：数组长度异常时安全失败；全黑、全白、均匀分布、单峰分布方向正确；重组 UI 不重复执行核心算法。

### 性能统计模块

- 使用单调时钟 `elapsedRealtimeNanos`，避免系统时间变化影响结果。
- 明确展示计时口径：
  1. 解码耗时；
  2. 核心耗时（灰度化 + 频次统计 + 归一化，作为 300ms 主指标）；
  3. 首次绘制耗时或总处理耗时（辅助指标，不能与核心耗时混写）。
- 对比测试应预热后重复运行，展示中位数、最小值和加速比；正式数据以 Release/接近 Release 的真机测试为准。
- 验收：界面显示毫秒值和“达标/未达标”；主指标 `< 300ms` 时判定达标；测试报告记录设备、图片尺寸、构建类型和重复次数。

### 图像质量分析模块

- 只基于直方图数据计算，避免再次遍历 Bitmap。
- MVP 指标：平均灰度、暗部占比、亮部占比、灰度标准差；扩展指标可加入黑/白端截断比例。
- 阈值集中定义并在文档中说明，例如暗部 `0..63`、亮部 `192..255`；“偏暗/偏亮/低对比度”属于规则提示，不宣称为专业图像诊断。
- 验收：指标范围合法；全黑、全白、均匀灰度图的判断符合规则；质量分析不增加第二次像素扫描。

### 算法对比模块

- 在同一 Bitmap、同一取整规则、同一线程策略和相同运行次数下比较基础算法与优化算法。
- 展示单次/中位耗时、加速比、结果是否一致；结果不一致时禁止只展示“更快”。
- 默认不在每次选图时自动跑基线多轮测试，避免界面卡顿；由用户点击“开始对比”触发。
- 验收：两种算法 bins 和归一化数据完全相同；对比过程可取消；至少在 3 种分辨率图片上形成可复现数据。

## 三、推荐目录结构

采用单 app 模块、按职责分包的轻量分层。课程项目规模不需要数据库、后端或复杂多模块架构。

```text
ImageHistogramAnalyzer/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/lzx/imagehistogramanalyzer/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ImageHistogramApplication.kt       # 仅在确有初始化需求时创建
│       │   │   ├── data/
│       │   │   │   └── image/
│       │   │   │       ├── BitmapDecoder.kt
│       │   │   │       └── ImageRepository.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   ├── HistogramResult.kt
│       │   │   │   │   ├── ImageQualityResult.kt
│       │   │   │   │   └── AlgorithmComparisonResult.kt
│       │   │   │   ├── histogram/
│       │   │   │   │   ├── HistogramCalculator.kt
│       │   │   │   │   ├── BaselineHistogramCalculator.kt
│       │   │   │   │   ├── OptimizedHistogramCalculator.kt
│       │   │   │   │   └── HistogramNormalizer.kt
│       │   │   │   ├── quality/
│       │   │   │   │   └── ImageQualityAnalyzer.kt
│       │   │   │   └── benchmark/
│       │   │   │       └── AlgorithmComparator.kt
│       │   │   └── ui/
│       │   │       ├── ImageHistogramApp.kt
│       │   │       ├── analyzer/
│       │   │       │   ├── AnalyzerScreen.kt
│       │   │       │   ├── AnalyzerUiState.kt
│       │   │       │   └── AnalyzerViewModel.kt
│       │   │       ├── component/
│       │   │       │   ├── ImagePickerCard.kt
│       │   │       │   ├── ImagePreviewCard.kt
│       │   │       │   ├── HistogramCanvas.kt
│       │   │       │   ├── PerformanceCard.kt
│       │   │       │   ├── QualityAnalysisCard.kt
│       │   │       │   └── AlgorithmComparisonCard.kt
│       │   │       └── theme/
│       │   │           ├── Color.kt
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   └── res/
│       │       ├── values/strings.xml
│       │       └── values/themes.xml
│       ├── test/java/com/lzx/imagehistogramanalyzer/
│       │   ├── domain/histogram/HistogramCalculatorTest.kt
│       │   ├── domain/histogram/HistogramNormalizerTest.kt
│       │   └── domain/quality/ImageQualityAnalyzerTest.kt
│       └── androidTest/java/com/lzx/imagehistogramanalyzer/
│           ├── HistogramAlgorithmConsistencyTest.kt
│           └── AnalyzerScreenTest.kt
├── benchmark/                                      # 优化阶段再创建，可选但推荐
│   ├── build.gradle.kts
│   └── src/androidTest/java/.../HistogramBenchmark.kt
├── docs/
│   ├── Android图像直方图系统开发计划.md
│   ├── 需求规格说明书.md
│   ├── 概要设计.md
│   ├── 测试计划与测试报告.md
│   └── 使用说明.md
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

依赖方向固定为：`UI -> ViewModel -> domain 接口/模型 -> data 图片读取`。`domain` 不引用 Compose；Canvas 只消费 `HistogramResult`；ViewModel 负责协程、取消和状态组合，避免所有逻辑集中在 `MainActivity.kt`。

## 四、开发里程碑

### 阶段一：MVP 版本

- 开发任务：
  - 接入 Jetpack Compose、Material 3、Lifecycle ViewModel 和 Compose 测试基础依赖。
  - 创建 Launcher Activity、应用主题和单页骨架。
  - 完成系统选图、受控 Bitmap 解码、图片预览。
  - 实现独立的基础直方图算法、归一化和结果数据类。
  - 使用 Compose Canvas 展示 256 bin、0..100 高度的黑白直方图。
  - 后台线程执行解码和计算，界面展示加载、成功、失败状态。
- 预期产物：可启动 Debug APK；图片选择和预览页面；基础算法；核心单元测试；MVP 演示截图。
- 验收标准：
  - 可安装、可启动、可选图、可看到原图和直方图。
  - 主线程不执行整图像素循环，选大图期间 UI 不假死。
  - 典型人工样例的灰度 bin 与归一化结果正确。
  - `MainActivity.kt` 只承担 Activity/Compose 入口职责。

### 阶段二：基础功能版本

- 开发任务：
  - 完成核心耗时统计、`<300ms` 达标提示和明确的计时口径。
  - 完成平均灰度、暗亮占比、标准差和规则化质量结论。
  - 完成错误处理、重新选图、进度状态、图片元数据展示。
  - 补充字符串资源、无障碍描述、横竖屏/不同宽度布局。
  - 完成需求规格、概要设计和使用说明初稿。
- 预期产物：功能完整 APK；质量分析卡片；性能卡片；需求/设计/使用文档；基础 UI 测试。
- 验收标准：
  - 所有课程必需信息均在同一主流程中可见：原图、256×100 直方图、耗时、300ms 判断。
  - 质量指标可由直方图复算验证，不二次扫描图片。
  - 取消选图、损坏图片、超限图片不会导致崩溃或永久加载。

### 阶段三：优化版本

- 开发任务：
  - 实现分块 `getPixels()` 优化算法，复用 `IntArray`，减少 Bitmap JNI 调用和峰值内存。
  - 保留基础算法作为正确性参照，实现手动触发的算法对比。
  - 建立预热、多轮、中位数测试流程；必要时建立 AndroidX Benchmark 模块。
  - 使用 Profiler/Memory 工具检查线程、分配、Bitmap 生命周期和峰值内存。
  - 根据数据决定是否继续采用行分块、整图数组、多线程分区；只有有实测收益且结果一致的方案才保留。
- 预期产物：优化算法；算法对比界面；真机性能数据；性能测试记录；可选 Benchmark 模块。
- 验收标准：
  - 优化算法与基础算法的 256 个频次和归一化结果完全一致。
  - 指定测试设备和课程测试图片上，核心耗时稳定低于 300ms；报告同时给出图片分辨率、构建类型、中位数和最差值。
  - 连续切换多张图无明显内存持续增长，无 OOM、ANR 和过期结果覆盖。

### 阶段四：验收版本

- 开发任务：
  - 执行功能、边界、兼容、准确性、性能和内存测试。
  - 使用至少 5 类图片：全黑/全白、暗图、亮图、低对比度图、普通照片；覆盖小/中/大分辨率。
  - 修复缺陷，整理中文注释，删除模板测试和调试残留。
  - 生成 Release APK，整理需求、设计、测试、使用说明、答辩演示流程与关键技术说明。
- 预期产物：最终源码；Release APK；测试报告；使用说明；答辩材料与可复现测试图片集。
- 验收标准：
  - 冷启动、选图、分析、重新选图、算法对比流程稳定。
  - 准确性测试全部通过，300ms 指标有真实设备数据支撑。
  - 文档、界面术语、公式、取整规则和代码实现一致。
  - 核心类有解释“为什么这样做”的中文注释，团队成员可说明模块边界、算法与优化依据。

## 五、文件修改计划

### 需要新建的 Kotlin 文件

- 应用入口：
  - `app/src/main/java/com/lzx/imagehistogramanalyzer/MainActivity.kt`
  - `app/src/main/java/com/lzx/imagehistogramanalyzer/ui/ImageHistogramApp.kt`
- 图片数据层：
  - `data/image/BitmapDecoder.kt`
  - `data/image/ImageRepository.kt`
- 领域模型：
  - `domain/model/HistogramResult.kt`
  - `domain/model/ImageQualityResult.kt`
  - `domain/model/AlgorithmComparisonResult.kt`
- 核心算法：
  - `domain/histogram/HistogramCalculator.kt`
  - `domain/histogram/BaselineHistogramCalculator.kt`
  - `domain/histogram/OptimizedHistogramCalculator.kt`
  - `domain/histogram/HistogramNormalizer.kt`
  - `domain/quality/ImageQualityAnalyzer.kt`
  - `domain/benchmark/AlgorithmComparator.kt`
- UI 与状态：
  - `ui/analyzer/AnalyzerScreen.kt`
  - `ui/analyzer/AnalyzerUiState.kt`
  - `ui/analyzer/AnalyzerViewModel.kt`
  - `ui/component/ImagePickerCard.kt`
  - `ui/component/ImagePreviewCard.kt`
  - `ui/component/HistogramCanvas.kt`
  - `ui/component/PerformanceCard.kt`
  - `ui/component/QualityAnalysisCard.kt`
  - `ui/component/AlgorithmComparisonCard.kt`
  - `ui/theme/Color.kt`
  - `ui/theme/Theme.kt`
  - `ui/theme/Type.kt`
- 测试：
  - `HistogramCalculatorTest.kt`
  - `HistogramNormalizerTest.kt`
  - `ImageQualityAnalyzerTest.kt`
  - `HistogramAlgorithmConsistencyTest.kt`
  - `AnalyzerScreenTest.kt`
  - 优化阶段可新增 `benchmark/src/androidTest/.../HistogramBenchmark.kt`

`ImageHistogramApplication.kt` 暂不创建；只有出现全局初始化需求时才增加，避免空架构文件。

### 需要修改的现有文件

- `app/build.gradle.kts`：启用 Compose；配置 Kotlin/JVM 编译选项；加入 Activity Compose、Compose BOM、Material 3、Lifecycle ViewModel、Compose Tooling/Test、协程等依赖；按需要增加 Release 配置。
- `gradle/libs.versions.toml`：集中声明 Compose、Lifecycle、Activity、Coroutines、Benchmark 等版本和别名；移除后续不再使用的传统 AppCompat/Material 依赖。
- `app/src/main/AndroidManifest.xml`：注册导出的 Launcher `MainActivity`；不添加不必要的存储权限。
- `app/src/main/res/values/themes.xml` 和 `values-night/themes.xml`：切换为适配 Compose 的无 ActionBar 容器主题。
- `app/src/main/res/values/strings.xml`：加入中文界面、错误提示、性能状态和无障碍文本。
- `settings.gradle.kts`：仅当增加 `benchmark` 模块时加入 `include(":benchmark")`。
- `build.gradle.kts`：仅当 benchmark 或其他插件需要在根项目声明时修改。
- 模板 `ExampleUnitTest.kt`、`ExampleInstrumentedTest.kt`：在真实测试建立后删除或替换。

### 需要新建的 Gradle 文件

- MVP 和基础版本保持单模块，**不需要新建 Gradle 文件**，只修改现有 `app/build.gradle.kts` 与 `libs.versions.toml`。
- 若优化阶段采用独立 AndroidX Benchmark 模块，再新建 `benchmark/build.gradle.kts`，并修改根 `settings.gradle.kts`/`build.gradle.kts`。不为形式上的“模块化”提前增加构建复杂度。

### 需要新建的项目文档

- `docs/需求规格说明书.md`
- `docs/概要设计.md`
- `docs/测试计划与测试报告.md`
- `docs/使用说明.md`
- 可选：`docs/性能测试原始数据.csv`、`docs/答辩问题清单.md`

## 六、风险点和解决方案

- Android 图片读取：
  - 风险：不同系统版本返回的 URI、云端媒体、权限有效期和文件格式不同，直接依赖真实文件路径容易失败。
  - 方案：只通过 `ContentResolver` 读取；使用 Activity Result API；不解析绝对路径；对空流、异常、取消和不支持格式建立明确状态。

- Bitmap 解码与超大图：
  - 风险：一张 48MP ARGB_8888 Bitmap 约占 192MB，再复制整图 `IntArray` 会显著增加 OOM 风险。
  - 方案：先读尺寸；预览图采样；分析时使用行/块级 `getPixels()` 缓冲区并复用；设置可解释的像素上限。不得静默降采样后仍声称是全图准确统计。后续若必须支持超大图，评估分块解码。

- Bitmap 像素处理：
  - 风险：逐像素 `getPixel()` JNI 调用慢；浮点公式、取整方式和 Alpha 处理会造成算法结果差异。
  - 方案：基础算法只用于参照；生产算法批量取像素、一次遍历统计；固定四舍五入规则；透明像素按其解码后的 RGB 参与统计，或在需求变更时统一定义背景合成规则；所有策略由测试锁定。

- Compose Canvas 绘制：
  - 风险：在 Canvas 中做计算会因重绘重复执行；物理屏幕尺寸不等于 256×100 像素；频繁分配绘制对象会抖动。
  - 方案：Canvas 只消费不可变归一化数组；使用 256×100 逻辑坐标映射到实际画布；缓存 Path/绘制参数；增加精确 256×100 离屏输出或像素测试作为验收依据。

- 运行性能与 300ms 指标：
  - 风险：Debug、首次 JIT、图片解码、算法、绘制混在一个计时值中会让结果不可比较；后台线程数量过多也可能更慢。
  - 方案：分别计时并清晰标注；主指标固定为灰度统计+归一化；预热、多轮取中位数；以 Release 真机为准；先测单线程批处理，再以数据决定是否分区并行。

- 内存占用与生命周期：
  - 风险：Compose State/ViewModel 长期持有多个大 Bitmap、快速选图产生并发任务，可能导致内存上涨或旧结果覆盖。
  - 方案：只保留当前图；新选择到来时取消旧 Job；避免不必要 `copy()`；退出或替换时释放引用；用序号/URI 校验结果归属；通过 Profiler 连续选图验证。

- 主线程和 ANR：
  - 风险：解码、像素循环或多轮对比运行在主线程会卡住界面。
  - 方案：解码放 `Dispatchers.IO`，计算放 `Dispatchers.Default`；ViewModel 管理协程和错误；UI 仅收集状态；算法对比支持取消并限制运行次数。

- 结果准确性：
  - 风险：灰度取整、最大值为零、归一化整数除法、柱高方向、透明像素规则都可能产生边界错误。
  - 方案：为纯色、已知小矩阵、空/异常输入、单峰和均匀分布建立确定性测试；基础与优化算法逐 bin 比对；归一化先乘后除并防止除零。

- 性能优化的可解释性：
  - 风险：为了展示“优化”引入复杂多线程，却没有可复现收益，答辩时难以说明。
  - 方案：保留基线；每项优化记录假设、实现、测试环境和前后数据；未带来稳定收益的优化不合并；对比结果同时展示正确性。

- 课程文档与答辩：
  - 风险：界面显示、报告、代码公式和计时口径不一致；源码注释不足或充斥无意义注释。
  - 方案：以本计划中的公式、取整和计时定义为唯一口径；核心算法、边界与性能决策写中文注释；验收前做代码—界面—文档一致性检查。

## 实施顺序建议

1. 先让 Compose 空页面可启动，再提交一个可回退基线。
2. 先用纯 Kotlin/Android Bitmap 单元测试确定算法语义，再接图片选择和 UI。
3. 先完成正确的基础链路，再实现优化算法；每次优化都跑一致性测试。
4. 最后做真机性能、内存和文档验收，不用模拟器数据替代最终 300ms 结论。

