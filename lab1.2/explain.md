# Lab2 操作指南

## 1. 启动与退出

### 1.1 启动

```bash
mvn package
java -jar target/lab1-cli-editor-1.0.0.jar
```

**预期结果**：进入命令行交互界面，显示欢迎信息。

### 1.2 退出

```bash
exit
```

**预期结果**：
- 若有未保存修改，逐个提示是否保存。
- 返回 `bye` 并退出。

## 2. 工作区命令

### 2.1 创建缓冲区

```bash
init <file> [with-log]
```

**预期结果**：
- `.txt` 文件：新建空文本缓冲区，若带 `with-log` 则首行写入 `# log`。
- `.xml` 文件：新建 XML 基础结构。
- 当前文件标记为已修改。

示例：
```bash
init test.txt with-log
```
输出：
```
ok
```

```bash
init test.xml
```
输出：
```
ok
```

### 2.2 加载文件

```bash
load <file>
```

**预期结果**：
- 文件存在：加载内容并成为活动文件。
- 文件不存在：创建空文件并加载。

### 2.3 保存

```bash
save
save all
save <file>
```

**预期结果**：
- `save` 保存当前活动文件。
- `save all` 保存全部打开文件。
- `save <file>` 保存指定文件。

### 2.4 关闭

```bash
close [file]
```

**预期结果**：
- 有未保存修改时提示是否保存。
- 文件从打开列表中移除。

### 2.5 切换活动文件

```bash
edit <file>
```

**预期结果**：
- 切换活动文件并开始计时。

### 2.6 查看打开文件列表

```bash
editor-list [tree]
```

**预期结果**：
- 默认列表输出，包含活动标记、修改标记与编辑时长。

示例：
```
* file1.txt [modified] (2小时15分钟)
  file2.xml (45秒)
```

- `tree` 参数按目录层级展示。

示例：
```
F:\project\
├── src\
│   └── * file1.txt [modified] (2分钟)
└──   file2.xml (45秒)
```

### 2.7 目录树

```bash
dir-tree [path]
```

**预期结果**：打印指定目录的树形结构。

## 3. 通用编辑与历史命令

### 3.1 撤销/重做

```bash
undo
redo
```

**预期结果**：
- `undo` / `redo` 输出 `(undo) ok` 或 `(redo) ok`。
- 若无可操作项，输出 `(undo) nothing` 或 `(redo) nothing`。

## 4. 文本编辑命令（仅 .txt 支持）

### 4.1 追加

```bash
append <text>
```

**预期结果**：追加文本到末尾，输出 `ok`。

### 4.2 插入

```bash
insert <line:col> <text>
```

**预期结果**：在指定位置插入文本，输出 `ok`。

### 4.3 删除

```bash
delete <line:col> <len>
```

**预期结果**：删除指定长度字符，输出 `ok`。

### 4.4 替换

```bash
replace <line:col> <len> <text>
```

**预期结果**：删除并插入，输出 `ok`。

### 4.5 显示内容

```bash
show [start:end]
```

**预期结果**：
- 输出带行号的内容。
- 空文件输出 `(empty)`。

## 5. XML 编辑命令（仅 .xml 支持）

### 5.1 插入同级元素

```bash
insert-before <tagName> <newId> <targetId> ["text"]
```

**预期结果**：
- 成功输出 `ok`。
- `newId` 重复输出 `元素ID已存在: <newId>`。
- `targetId` 不存在输出 `目标元素不存在: <targetId>`。
- 根元素前插入输出 `不能在根元素前插入元素`。

### 5.2 追加子元素

```bash
append-child <tagName> <newId> <parentId> ["text"]
```

**预期结果**：
- 成功输出 `ok`。
- 父元素不存在输出 `父元素不存在: <parentId>`。
- `newId` 重复输出 `元素ID已存在: <newId>`。

### 5.3 修改元素 ID

```bash
edit-id <oldId> <newId>
```

**预期结果**：
- 成功输出 `ok`。
- 元素不存在输出 `元素不存在: <oldId>`。
- `newId` 已存在输出 `目标ID已存在: <newId>`。
- 根元素 ID 修改输出 `不建议修改根元素ID`。

### 5.4 修改元素文本

```bash
edit-text <elementId> ["text"]
```

**预期结果**：
- 成功输出 `ok`。
- 元素不存在输出 `元素不存在: <elementId>`。

### 5.5 删除元素

```bash
delete <elementId>
```

**预期结果**：
- 成功输出 `ok`。
- 元素不存在输出 `元素不存在: <elementId>`。
- 删除根元素输出 `不能删除根元素`。

### 5.6 XML 树形展示

```bash
xml-tree [file]
```

**预期结果**：输出树形结构，含属性与文本。

示例：
```
bookstore [id="root"]
├── book [id="book1", category="COOKING"]
│   └── title [id="title1", lang="en"]
│       └── "Everyday Italian"
```

## 6. 拼写检查

```bash
spell-check [file]
```

**预期结果**：
- 文本文件：按行输出错误与建议。
- XML 文件：按元素输出错误与建议。

示例（文本）：
```
第1行，第5列: "recieve" -> 建议: receive
```

示例（XML）：
```
元素 title1: "Itallian" -> 建议: Italian
```

## 7. 日志命令

```bash
log-on [file]
log-off [file]
log-show [file]
```

**预期结果**：
- `log-on` / `log-off` 返回 `ok`。
- `log-show` 输出该文件的日志内容。

## 8. 不支持提示

当在错误的文件类型上执行命令时，输出：
```
不支持
```
