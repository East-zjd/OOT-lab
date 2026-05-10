# Lab1 CLI 文本编辑器（Java）

## 操作指南

### 1) 环境准备

- Java：17+（推荐 17 或 21）
- Maven：3.8+（需要系统已安装并可在终端直接使用 `mvn`）

Windows PowerShell 验证：

- `java -version`
- `mvn -version`

### 2) 构建、测试、运行

在项目根目录执行：

- 运行测试：`mvn -q test`
- 打包：`mvn -q package`
- 启动程序：`java -jar target/lab1-cli-editor-1.0.0.jar`

启动后会进入交互式命令行：

- 提示符：`> `
- 输入 `exit` 退出

### 3) 常用命令示例

工作区命令：

- `load lab.txt`
- `editor-list`
- `edit lab.txt`
- `save` / `save all`
- `close`（若已修改会提示是否保存：`y/n`）

文本编辑命令（默认作用于当前活动文件）：

- `append "New line"`
- `insert 1:1 "Hello"`
- `delete 1:7 5`
- `replace 1:1 4 "slow"`
- `show` 或 `show 1:3`
- `undo` / `redo`
- `spell-check`

参数约定：

- 行列从 1 开始：`line:col`
- 带空格的文本参数使用双引号包裹
- 文本参数支持转义：`\n`（换行）、`\t`（制表符）、`\\`、`\"`

例如插入多行：

- `insert 1:1 "a\nb"`

### 4) 日志（Logging）

- 自动启用：打开文件后，如果第一行是 `# log`，将自动开启日志
- 手动开关：`log-on` / `log-off`（可选指定文件名）
- 查看日志：`log-show`
- 日志文件：在同目录生成 `.filename.log`（例如 `lab.txt` 对应 `.lab.txt.log`）
- 日志写入失败只会提示 warning，不会中断程序

### 5) 工作区状态持久化

- 程序退出会在当前工作目录生成/更新：`.workspace.state.properties`
- 下次启动自动恢复：打开文件列表、活动文件、modified 标记、日志开关
- 不恢复：每个文件的 undo/redo 历史（符合实验要求）

### 6) 常见问题

- `mvn` 找不到：请安装 Maven，并确保将 Maven 的 `bin` 目录加入 PATH（重开终端生效）
- `mvn` 使用的是 Java 8 导致 "无效的目标发行版: 17"：运行 `mvn -v` 查看 `Java version`，需要把 `JAVA_HOME` 指向 JDK 17+ 并将 `%JAVA_HOME%\\bin` 放到 PATH 前面，然后重开终端再执行 `mvn test`
- PowerShell 下查看 `java/javac` 路径：请用 `where.exe java` / `where.exe javac`（注意不是 `where`，因为 PowerShell 的 `where` 是别名）
- 中文/编码问题：项目读写文件统一使用 UTF-8

## 运行

- Java: 17+
- 构建并运行：
  - `mvn -q test`
  - `mvn -q package`
  - `java -jar target/lab1-cli-editor-1.0.0.jar`

## 命令

命令规范与功能要求见 lab1.md。

项目会在当前工作目录生成工作区状态文件：`.workspace.state.properties`。
