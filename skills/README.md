# Android 课程项目 Agent Skills

这里公开的是 ImageHistogramAnalyzer 在真实课程开发中使用并持续修订的 Agent Skills。它们不是第三方仓库原文的复制品，而是围绕“需求可追踪、代码可验证、性能有证据、文档不失真、Git 可回退”重新编写的项目规则。

## Skill 清单

| Skill | 适用任务 | 入口 |
|---|---|---|
| `image-histogram-android` | Android 图像直方图、Bitmap、Compose、NDK、性能与课程边界 | [SKILL.md](../.github/skills/image-histogram-android/SKILL.md) |
| `android-ui-material3` | Compose/Material 3 页面、状态、可访问性和 UI 性能 | [SKILL.md](android-ui-material3/SKILL.md) |
| `android-test-gate` | 按代码切片选择单元、Compose、仪器与性能测试 | [SKILL.md](../.github/skills/android-test-gate/SKILL.md) |
| `project-doc-sync` | 根据实际代码与测试同步 TODO、设计、使用和报告 | [SKILL.md](../.github/skills/project-doc-sync/SKILL.md) |
| `project-git-checkpoint` | 检查、显式暂存、验证、提交和安全推送 | [SKILL.md](../.github/skills/project-git-checkpoint/SKILL.md) |
| `course-presentation` | 从源码、测试、截图和真机数据生成课程答辩 PPT | [SKILL.md](../.github/skills/course-presentation/SKILL.md) |

## 推荐组合

一次普通 Android 功能迭代按以下顺序触发：

```text
image-histogram-android / android-ui-material3
                    ↓
            android-test-gate
                    ↓
            project-doc-sync
                    ↓
        project-git-checkpoint
```

准备答辩时，再单独使用 `course-presentation`。这种拆分能让 Agent 只加载当前任务需要的规则，避免一个超长 Skill 同时承担开发、测试、文档和 PPT。

## 安装到其他仓库

支持项目级 Skills 的 Agent 可以直接读取这些目录。迁移时只复制需要的完整 Skill 文件夹，不要只复制 `SKILL.md` 而遗漏 `references/` 或 `agents/`。

示例：

```bash
mkdir -p your-project/.github/skills
cp -R .github/skills/android-test-gate your-project/.github/skills/
cp -R .github/skills/project-doc-sync your-project/.github/skills/
cp -R .github/skills/project-git-checkpoint your-project/.github/skills/
```

`image-histogram-android` 和 `android-ui-material3` 包含本项目的包名、算法、配色和课程指标。迁移到其他项目时必须先修改：

1. Skill 的 `name`、`description` 和 `agents/openai.yaml`。
2. 模块名、包路径、UI 技术栈与构建命令。
3. 项目固定公式、输入输出、性能口径和禁止范围。
4. TODO、测试报告、设计文档和交付物的真实路径。
5. 真机型号、样本、构建类型和验收阈值；不得照抄本项目 30.661ms 的结果。

## 设计原则

- Skill 只规定可复用工作流，不替代课程需求和人工决策。
- 先验证正确性，再讨论速度；模拟器数据不能冒充真机结论。
- 不执行来源不明的第三方脚本，不复制外部 Skill 原文。
- 不把 token、签名文件、账号、聊天记录或本机绝对路径写入 Skill。
- 不因为模板推荐就强行加入 Hilt、Room、Retrofit、多模块或云服务。
- 未运行的测试必须明确写“未运行”，不能写成“已通过”。

## 示例触发语句

```text
Use $image-histogram-android to implement the next histogram TODO.
Use $android-ui-material3 to audit the analyzer result page.
Use $android-test-gate to verify the latest Bitmap change.
Use $project-doc-sync to update documents from verified code.
Use $project-git-checkpoint to create a safe release checkpoint.
Use $course-presentation to update the classroom acceptance deck.
```

## 开源许可

本目录及上述项目 Skills 采用 [MIT License](LICENSE)。项目源码、课程材料和引用的第三方内容仍以各自声明与来源许可为准。
