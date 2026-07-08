# ImageHistogramAnalyzer 轻量 UI 设计系统

## App 视觉定位

本应用是“课程实验仪表盘”，视觉上应专业、克制、可信。首页负责解释能力和入口，分析页负责让图片、算法、直方图、质量结论和性能证据形成清晰阅读顺序。美化服务于任务理解，不添加装饰性图表或冗余动画。

## 颜色系统

用户于 2026-07-08 选定 B“晴空柑橘”。天空蓝用于主要动作和分析主题，薄荷绿用于质量/成功信息，柑橘黄用于性能模块与中间调强调；红色只用于错误或超出性能目标。

| Role | Light | 用途 |
|---|---|---|
| primary | `#3577C8` | 主按钮、性能预算、关键强调 |
| primaryContainer | `#E5F1FF` | 首页 hero、算法选中态、直方图功能模块、达标性能卡 |
| secondary | `#4F9E82` | 亮部/质量/成功提示 |
| secondaryContainer | `#DDF5EC` | 质量功能模块、成功容器 |
| tertiary | `#F2B84B` | 中间调和柑橘强调 |
| tertiaryContainer | `#FFF4D6` | 性能功能模块 |
| background / surface | `#F8FBFF` | 页面基础背景 |
| surface containers | `#FFFFFF`–`#DFE8F1` 蓝灰阶 | 普通卡片、方案卡片和分组层级，禁止回退为紫灰默认色 |
| error | `#BA1A1A` | 失败、超标 |
| outline | `#717985` | 边框和分隔 |

本项目不启用 dynamic color 或 dark theme，原因是用户选择固定 B light 方案，且课程报告与跨设备截图需要一致。即使设备系统处于深色模式，App 仍使用 light scheme 和深色系统栏图标。所有文字必须搭配对应 `on*` 角色；页面代码禁止硬编码主题色。

## Typography 层级

- `headlineMedium`：页面 hero 标题。
- `headlineSmall`：核心结果/重点指标。
- `titleLarge`：页面 section 标题。
- `titleMedium`：卡片标题。
- `bodyLarge`：主要说明和关键数据。
- `bodyMedium`：功能说明、质量解释。
- `bodySmall`：辅助口径、测试提示。
- `labelLarge`：按钮和主要标签。
- `labelMedium`：状态 badge、图例。

不得在页面内随意指定 `fontSize`。数字可使用 `FontWeight.SemiBold`，但仍从 Typography role 复制演进。

## Spacing 规则

基础网格为 8dp：`4 / 8 / 12 / 16 / 24 / 32`。页面水平边距 16dp；卡片内容 16–20dp；section 间距 16–24dp；同组文字间距 4–8dp。宽屏时内容居中并限制最大宽度，避免段落横向铺满。

## Shape / Corner radius

- `extraSmall` 4dp：细小状态标记。
- `small` 8dp：小图例、轻容器。
- `medium` 12dp：普通组件。
- `large` 20dp：主要卡片。
- `extraLarge` 28dp：hero 和强调容器。
- 按钮采用 Material 3 默认 full/pill 形态；页面不得重复写 `RoundedCornerShape(n.dp)`。

## 组件规范

- **Card**：默认 tonal `surfaceContainerLow`；首页直方图/质量/性能模块分别使用 primary/secondary/tertiary container。阴影只用于浮层，本应用普通内容卡不加自定义阴影。
- **Button**：一个页面同一视觉区只保留一个主要按钮。文案描述立即发生的动作，如“选择图片”“计算并绘制直方图”。禁用态要解释前置条件。
- **Input / 方案选择**：算法方案是离散选择，使用可点击卡片/单选语义，不使用文本输入。外层卡片使用蓝灰白 `surfaceContainerLow`，选中项使用浅蓝 `primaryContainer`；薄荷绿不用于算法选中态。
- **Top bar**：首页显示产品名；分析页显示“图片分析”并提供至少 48dp 的“返回首页”动作。
- **Bottom bar**：当前没有并列一级目的地，不使用 bottom bar。
- **Image preview**：固定合理高度并使用 `ContentScale.Fit`，保留文件名、分辨率、像素数、类型。
- **Result card**：先结论，后可视摘要，再精确数字，最后口径说明。

## 页面状态规范

- **Empty / 初始**：首页解释用途并提供“选择图片”；分析页无图时提示先选择图片。
- **Loading**：显示进度指示与“正在读取并分析图片”，期间防止重复提交。
- **Error**：使用 error container，标题说明失败，正文给出可恢复动作；不得只显示异常字符串。
- **Success**：展示图片、算法方案、直方图、质量结论与性能结果；达到 300ms 使用 primary/secondary tone，未达到使用 error tone。

## 图表规范

- 图表只用于回答明确问题，并必须同时保留精确数字。
- **灰度直方图**：严格为 256 个 bin、纵轴归一化 0–100、白底黑柱，保持课程要求。
- **质量分布条**：暗部/中间调/亮部使用蓝/柑橘黄/薄荷绿三段，直接展示占比与图例，用于解释“偏暗/偏亮/正常”。
- **性能预算条**：核心计算耗时相对 300ms 的使用比例，达标时使用天空蓝；超过目标时改用错误色并允许截断视觉长度，但文本显示真实毫秒和百分比。
- 自定义可视化必须提供整体 semantics/content description，不能只依赖颜色表达信息。

## 图标规范

优先 Material Symbols/现有依赖中的图标；若项目未引入图标扩展，不为单个图标增加依赖。装饰图标 `contentDescription = null`；可操作图标描述动作，不描述图形。图标视觉尺寸常用 20–24dp，点击容器至少 48dp。

## 页面信息层级

1. 当前页面与返回路径。
2. 主要任务或结果结论。
3. 选择图片/算法等操作。
4. 图片与直方图证据。
5. 质量和性能摘要。
6. 精确指标、测试口径与辅助说明。

首页顺序：价值说明 → 选择图片 → 核心能力 → 三步流程 → 300ms 目标。分析页顺序：选图 → 预览 → 方案 → 直方图 → 质量 → 性能。

## Accessibility 检查项

- 所有点击区域至少 48×48dp，按钮文案能独立说明动作。
- 有信息的图片提供 content description；装饰图像明确为 `null`。
- section 标题添加 heading 语义；自绘图表提供可读的整体描述。
- 正文对比度目标 4.5:1，大字和 UI 元素目标 3:1；固定 light 主题逐项核对。
- 不只用红/绿区分状态，同时显示文字、图例或数值。
- 1.5× 字体下允许换行，不让指标标签和值互相覆盖；重要操作不可被截断。
- TalkBack 顺序按页面视觉顺序；复合信息按需要 `mergeDescendants`，避免逐像素或逐柱朗读。
