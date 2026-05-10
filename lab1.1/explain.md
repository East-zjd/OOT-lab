# explain.md

## 1. 项目整体做了什么

这是一个**基于命令行的文本编辑器（Lab1）**实现，包含：

- **Workspace 工作区**：支持同时打开多个文件、切换活动文件、显示列表、目录树、退出/关闭时保存提示
- **Editor 编辑器**：以 `List<String>` 行数组保存文本，支持 append/insert/delete/replace/show，并提供每文件独立 undo/redo
- **Logging 日志**：当文件首行是 `# log` 时自动启用；支持 `log-on/log-off/log-show`；写入 `.filename.log`，按 session 记录命令与时间戳
- **状态持久化**：退出保存工作区状态到 `.workspace.state.properties`，下次启动可恢复（不恢复 undo/redo 历史，符合实验要求）

补充：

- `editor-list` 的每一行都有固定前缀：活动文件为 `* `，非活动文件为两个空格 `  `（并保留首行前导空格，不会被 trim 掉）。
- `log-show` 本身也会被记入日志，并且为了符合“所见即所得”，其输出会包含本次 `log-show` 这一行记录。

设计目标：尽可能使用**接口抽象**与分层，让后续 Lab2 加 XML 编辑器时不需要推翻整体架构。

---

## 2. 文件与代码逐项说明（每个文件做了什么）

> 说明范围包含：源码、测试、文档、构建配置。`target/` 为编译产物不逐个解释。

### 2.1 顶层文件（配置/文档）

- `pom.xml`
  - Maven 配置：目标 Java 17、JUnit5、打包 jar 时指定主类入口。
  - 作用：标准化依赖管理、测试、打包。

- `README.md`
  - 操作指南：环境准备、运行/测试/打包、命令示例、参数与转义规则、日志与持久化说明。
  - 作用：方便助教直接运行与验收。

- `design.md`
  - 架构设计文档：模块划分图（Mermaid）、模块职责与依赖、设计模式说明、运行说明、测试说明。

- `lab1.md`
  - 实验原始需求说明（任务书）。

- `explain.md`
  - 本文件：更细粒度地解释每个代码文件的职责与做了什么工作。

---

### 2.2 程序入口层（CLI / App）

- `src/main/java/edu/lab/cli/Main.java`
  - 程序入口：创建默认 App 并启动交互式命令循环。

- `src/main/java/edu/lab/core/app/App.java`
  - 应用抽象接口：定义 `runInteractive()`，并用 `ExitCode` 表示退出码。

- `src/main/java/edu/lab/core/app/InteractiveApp.java`
  - 交互循环实现：显示提示符 `> `，读取用户输入，交给命令注册表执行并打印输出。
  - 启动时调用 `workspace.restore()`，实现“重启恢复”。

- `src/main/java/edu/lab/core/app/AppFactory.java`
  - 依赖装配（手写 DI）：创建 Console/Clock/EventBus/FileSystem/Persistence/LogService/Workspace/CommandRegistry。
  - 作用：避免业务类互相 `new`，更易测试与替换实现。

---

### 2.3 命令层（解析/分发/执行结果）

- `src/main/java/edu/lab/core/commands/CommandRegistry.java`
  - 命令执行入口接口：`execute(rawLine)` → `ExecutionResult`。

- `src/main/java/edu/lab/core/commands/ExecutionResult.java`
  - 命令返回值：`output`（输出文本） + `shouldExit`（是否退出）。

- `src/main/java/edu/lab/core/commands/ParsedCommand.java`
  - 解析后的命令结构：命令名 + 参数列表。

- `src/main/java/edu/lab/core/commands/CommandHandler.java`
  - 单个命令处理器接口（当前实现使用 lambda 处理）。

- `src/main/java/edu/lab/core/commands/CommandLineTokenizer.java`
  - 命令行 tokenizer：支持双引号将带空格文本作为一个参数；支持 `\"`、`\\`。
  - **注意**：为兼容 Windows 路径（如 `C:\temp\a.txt`），这里不做 `\t`/`\n` 的通用转义。

- `src/main/java/edu/lab/core/commands/TextEscapes.java`
  - 仅对“文本参数”做转义：`\n`（换行）、`\t`（制表）、`\\`、`\"`。
  - 被 `append/insert/replace` 使用，以满足“插入文本可包含换行”的要求。

- `src/main/java/edu/lab/core/commands/DefaultCommandRegistry.java`
  - 将 18 个命令全部注册并分发到 `Workspace`：
    - 工作区：load/save/init/close/edit/editor-list/dir-tree/undo/redo/exit
    - 编辑：append/insert/delete/replace/show/spell-check
    - 日志：log-on/log-off/log-show
  - 每次命令执行后发布 `CommandExecutedEvent`（观察者模式），供日志模块订阅。
  - 细节：`log-show` 会先发布事件写入日志，再读取日志内容返回，保证输出中能看到本次 `log-show`。

---

### 2.4 工作区层（Workspace：多文件会话、持久化、协调 editor）

- `src/main/java/edu/lab/core/workspace/Workspace.java`
  - 工作区接口：命令层只依赖此接口。

- `src/main/java/edu/lab/core/workspace/WorkspaceService.java`
  - 核心业务实现：
    - 多文件打开与 active editor 管理
    - `load/save/saveAll/init/close/edit/editor-list/dir-tree/undo/redo/exit`
    - 退出/关闭时对 modified 文件提示是否保存（通过 `Console` 读取 y/n）
    - `restore/exit` 与持久化交互
    - 订阅 `CommandExecutedEvent`，对启用日志的文件写入日志
  - `editor-list` 输出格式约定：活动文件行以 `* ` 开头，非活动文件行以 `  ` 开头。

- `src/main/java/edu/lab/core/workspace/WorkspaceSnapshot.java`
  - 工作区备忘录（Memento）：打开文件列表、活动文件、modified、日志开关等。
  - 不保存 undo/redo（符合要求）。

- `src/main/java/edu/lab/core/workspace/LineCol.java`
  - `line:col` 值对象与解析逻辑（从 1 开始）。

- `src/main/java/edu/lab/core/workspace/DirTreePrinter.java`
  - 目录树渲染：输出 `├──`、`└──`、`│` 结构。

---

### 2.5 编辑器层（Editor：行数组 + 文本编辑 + undo/redo）

- `src/main/java/edu/lab/core/editor/Editor.java`
  - 编辑器接口：append/insert/delete/replace/show/spellCheck/undo/redo 等。
  - 为 Lab2 增加 XML editor 预留扩展点。

- `src/main/java/edu/lab/core/editor/TextEditor.java`
  - 文本编辑器实现：
    - 使用 `List<String>` 作为行数组存储
    - `append/insert/delete/replace` 按要求实现，delete 不跨行；insert 支持 `\n` 拆分多行
    - 每个 editor 独立 undo/redo 栈
    - modified 标记：通过“保存基线是否初始化 + 当前内容与基线对比”判断，确保新建缓冲区在首次保存前视为 modified
    - `spell-check`：最小可用实现（内置简单英文词典 + 抽词正则），并暴露 `SpellChecker` 接口便于替换

---

### 2.6 基础设施层（可替换实现：IO/时间/事件/日志/持久化）

- Console
  - `src/main/java/edu/lab/core/console/Console.java`：抽象 print/println/readLine
  - `src/main/java/edu/lab/core/console/SystemConsole.java`：System.in/out 实现

- Clock
  - `src/main/java/edu/lab/core/time/Clock.java`：时间抽象
  - `src/main/java/edu/lab/core/time/SystemClock.java`：系统时间实现

- EventBus
  - `src/main/java/edu/lab/core/events/EventBus.java`：事件总线接口
  - `src/main/java/edu/lab/core/events/SimpleEventBus.java`：最小实现
  - `src/main/java/edu/lab/core/events/CommandExecutedEvent.java`：命令执行事件（用于日志）

- FileSystem
  - `src/main/java/edu/lab/core/fs/FileSystem.java`：文件系统抽象
  - `src/main/java/edu/lab/core/fs/LocalFileSystem.java`：基于 `java.nio.file.Files` 的实现

- Logging
  - `src/main/java/edu/lab/core/logging/LogService.java`：日志服务接口（best-effort）
  - `src/main/java/edu/lab/core/logging/WorkspaceLogService.java`：写 `.filename.log`，session start 行，失败仅 warning

- Persistence
  - `src/main/java/edu/lab/core/persistence/WorkspacePersistence.java`：持久化接口
  - `src/main/java/edu/lab/core/persistence/PropertiesWorkspacePersistence.java`：以 `.properties` 文件保存/恢复 `WorkspaceSnapshot`

---

### 2.7 测试代码（TDD 与可测试性支撑）

- `src/test/java/edu/lab/testkit/FakeConsole.java`
  - 测试用 console：预置输入、收集输出，便于验证 close/exit 的 y/n 提示行为。

- `src/test/java/edu/lab/testkit/FakeClock.java`
  - 测试用 clock：固定时间戳，便于断言日志内容。

- `src/test/java/edu/lab/core/editor/TextEditorTest.java`
  - 覆盖编辑器核心语义：append/insert/delete/replace、多行插入、越界异常、undo/redo。

- `src/test/java/edu/lab/core/workspace/WorkspaceServiceTest.java`
  - 覆盖工作区流程：load→append→show→save→exit→restore；验证 `# log` 自动启用、`.filename.log` 落盘；验证 close modified 提示可跳过保存。
  - 回归测试：活动文件不是第一个时，`editor-list` 第一行仍应保留两个空格前缀。

---

## 3. 关于 target/ 目录

`target/` 是编译产物（`.class` 等），一般**不建议提交**到实验压缩包中。提交时保留源码、测试、文档即可。

---

## 4. 解决问题：PowerShell 提示 “mvn 无法识别” 怎么办？

你看到的报错：

```powershell
mvn : 无法将“mvn”项识别为 cmdlet...
```

含义：系统找不到 `mvn` 可执行文件（通常是 Maven 未安装，或 PATH 未配置）。

### 方案 A（推荐）：安装 Maven 并配置 PATH

1) 安装 Maven（任选其一）：

- **方式 1：用 winget（推荐）**
  - PowerShell（管理员或普通均可）：
    - `winget install -e --id Apache.Maven`

- **方式 2：手动安装**
  - 下载 Apache Maven 二进制包（zip）并解压到例如：`C:\Program Files\Apache\maven`（或任意目录）

2) 配置环境变量（系统属性 → 高级 → 环境变量）：

- 新建（或确认）`MAVEN_HOME` 指向 Maven 目录，例如：
  - `MAVEN_HOME=C:\Program Files\Apache\maven`
- 在 `Path` 里追加：
  - `%MAVEN_HOME%\bin`

3) 关闭并重新打开 PowerShell，验证：

- `mvn -version`

### 方案 B：不装 Maven，用 IDE 自带 Maven

如果你使用 IntelliJ IDEA：

- IDEA 自带 Maven，可直接右侧 Maven 工具窗口执行 `test/package`。

### 方案 C：只验证能编译（没有 Maven 时）

如果只是想先验证能编译：

- 项目当前已可用 `javac` 编译主代码（但运行 JUnit 测试仍建议用 Maven）。

---

## 5. 建议：提交前如何验证

- 确认可运行：`java -jar ...`
- 确认测试可过：`mvn test`
- 确认文档齐全：`design.md`、`README.md`、`explain.md`
