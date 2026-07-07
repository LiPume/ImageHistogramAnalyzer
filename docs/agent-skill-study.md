# Agent Skill Study

本记录用于说明本项目实际阅读了哪些公开 Android Agent Skills，以及哪些规则经过筛选后进入本项目。第三方仓库均位于被 `.gitignore` 忽略的 `references/open_source/`，本次只读 README、`SKILL.md` 与相关 references，没有执行第三方脚本、安装第三方依赖或复制其源码。

## Sources

| Source | Main Use | Adopted? | Reason |
|---|---|---|---|
| [hamen/material-3-skill](https://github.com/hamen/material-3-skill)；`skills/material-3/SKILL.md`、`references/color-system.md`、`theming-and-dynamic-color.md`、`typography-and-shape.md`、`navigation-patterns.md`、`layout-and-responsive.md` | Material 3 token、主题、组件、布局和审计 | 部分采用 | 采用色彩角色、排版/形状、8dp 间距、tonal surface、light/dark 与无障碍；不引入生成器或额外 CLI。 |
| [aldefy/compose-skill](https://github.com/aldefy/compose-skill)；`skills/compose-expert/SKILL.md` | Compose 状态、组件边界、Modifier、预览与性能 | 采用 | 与现有 Compose + UDF 技术栈一致，强调最小正确方案和按需抽象。 |
| [anhvt52/jetpack-compose-skills](https://github.com/anhvt52/jetpack-compose-skills)；`modern-jetpack-compose/SKILL.md` 及 design/state/accessibility/performance references | 现代 Compose 写作与审查顺序 | 部分采用 | 采用生命周期感知状态、Material 3、状态下沉与无障碍；其建议的按 feature 组织按本项目规模轻量应用。 |
| [skydoves/compose-performance-skills](https://github.com/skydoves/compose-performance-skills)；lazy layout、`derivedStateOf`、effects、stability、modifier skills | 防止 UI 美化引入重组和滚动开销 | 部分采用 | 采用“先测量再优化”、稳定参数、正确状态读取和 Lazy 布局原则；不为静态页面机械添加 key、缓存或注解。 |
| [skydoves/android-testing-skills](https://github.com/skydoves/android-testing-skills)；choosing-what-to-test、Compose test structure、lazy list、accessibility、preview skills | 最小测试契约、语义测试、预览与设备验收 | 部分采用 | 采用语义优先、Arrange/Act/Assert、状态组件预览；不新增截图测试框架或仅为一次改版增加大型测试依赖。 |
| [rcosteira79/android-skills](https://github.com/rcosteira79/android-skills)；`compose`、`android-ux`、`android-testing` | 综合 Compose、Android UX、测试审查 | 部分采用 | 采用 M3 十项审计、48dp 目标、heading 语义和最小测试形态；折叠屏/KMP/复杂 DI 不属于课程 MVP。 |
| [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills)；`Agent.md`、compose-ui、android-accessibility、compose-performance-audit、android-testing | Skill 分类与通用工程规则 | 部分采用 | 采用 UDF、语义、性能审计与测试分层；拒绝其默认 Hilt/Room/Retrofit/多层数据架构。 |

## Adopted Rules

### Material Design 3

- 现有靛蓝 `#3F51B5` 作为品牌 seed，青绿色作为次级强调；所有页面从 `MaterialTheme.colorScheme` 取语义角色。
- 补齐 light/dark 的 container、surface、outline、error 角色，不在页面内散落 ARGB 颜色。
- 使用 8dp 间距基线、统一 Shapes 与 Typography；卡片以 tonal surface 建立层级，避免堆阴影。
- 两层页面使用顶部返回动作，不引入 bottom bar、rail 或 drawer。
- 按钮、空态、加载、错误、成功状态必须语义明确；主要点击目标至少 48dp。

### Jetpack Compose

- ViewModel 只负责业务状态，页面内部目的地属于 `rememberSaveable` 的轻量 UI 状态。
- 状态下传、事件上传；可预览和可测试组件只接收普通值与回调。
- 业务计算不进入 composable/Canvas；`MainActivity` 保持薄。
- `remember`、`derivedStateOf`、effect 只在生命周期或更新频率确有需求时使用，避免“为了性能”增加无效复杂度。
- Modifier 顺序表达点击区、背景与间距；新组件的 caller modifier 应作用于根节点。

### Performance

- UI 优化不得改变 Native v3 核心计时边界，也不得把 Compose 绘制时间混入核心算法耗时。
- Lazy 列表只有真实、可变的领域数据项才需要稳定 key；当前固定页面区块不制造虚假 ID。
- 图片预览限制显示尺寸；耗时处理留在 ViewModel/协程，不在重组路径分配大数组或格式化整套结果。
- 先以构建、测试和真机观察验证；只有有重组/掉帧证据时才增加更深性能机制。

### Testing

- 纯算法继续使用 JVM 测试；页面文字、状态、回调和语义使用 stateless Compose 测试。
- 首页验证“选择图片”的真实语义、功能说明与进入回调；分析页验证返回首页动作。
- 自绘/比例条必须有可测试的整体语义描述，精确数字仍以文本展示。
- UI 修改后至少运行 debug 单测、APK 构建、androidTest APK 编译和 Lint；有设备时运行 connected tests。

### Android Architecture

- 保持单 Activity、单 `:app` 模块、ViewModel + StateFlow + UDF。
- 仅两个层级页面时使用本地目的地状态与系统返回处理；当出现三个以上独立目的地、深链或可恢复回退栈时再评估 Navigation Compose。
- 不引入仓库模式、UseCase、DI 框架或多模块来包装本地图片分析流程。

## Rules Not Adopted

- **动态取色**：课程截图和性能对比需要跨设备一致视觉，本项目采用固定 light/dark scheme，不随壁纸变化。
- **Hilt/Koin、Room、Retrofit、离线优先和多模块 Clean Architecture**：项目没有远端或持久化需求，引入只会扩大构建与答辩复杂度。
- **Navigation Compose**：目前只有“首页 → 分析页”两层，没有深链与复杂回退栈，轻量 UI 状态更合适。
- **第三方图表、截图测试和新图片加载库**：两条比例可视化可由 Compose 原生布局完成，暂不为美化增加依赖。
- **Material Expressive 实验组件、复杂 motion、折叠屏专用布局和 KMP 规则**：不属于当前手机端课程验收范围。
- **对所有列表强制 key、对所有表达式强制 remember/derivedStateOf、随意添加 `@Stable`**：这些是需要证据的工具，不是机械检查项。
