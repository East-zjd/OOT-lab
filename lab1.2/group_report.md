# 可测试性报告（Lab1.2 → Lab2）

本部分按讨论课要求聚焦“可测试性”，内容简明，每部分提炼 2 点亮点与 2 点改进建议，并结合当前代码迭代。

## 1. 架构设计对单元测试成本的降低（亮点 2 点）
- **依赖注入 + 接口抽象**：`WorkspaceService` 依赖 `FileSystem`、`LogService`、`SpellCheckService`、`StatisticsService` 等接口/服务，测试可替换实现，减少环境成本。
- 代码示例（迭代前 / 迭代后）：
```java
// 迭代前：依赖较少，拼写检查/统计未注入
public WorkspaceService(FileSystem fileSystem,
					   WorkspacePersistence persistence,
					   EventBus eventBus,
					   LogService logService,
					   Console console) {
	this.fileSystem = fileSystem;
	this.persistence = persistence;
	this.logService = logService;
	this.console = console;
}

// 迭代后：注入 SpellCheckService 与 StatisticsService
public WorkspaceService(FileSystem fileSystem,
					   WorkspacePersistence persistence,
					   EventBus eventBus,
					   LogService logService,
					   Console console,
					   SpellCheckService spellCheckService,
					   StatisticsService statistics) {
	this.fileSystem = fileSystem;
	this.persistence = persistence;
	this.logService = logService;
	this.console = console;
	this.eventBus = eventBus;
	this.spellCheckService = spellCheckService;
	this.statistics = statistics;
}
```
- **适配器隔离第三方**：拼写检查通过 `SpellCheckService` 抽象与 `SpellCheckServiceFactory` 注入，切换实现无需改动编辑器逻辑。
- 代码示例（迭代前 / 迭代后）：
```java
// 迭代前：在装配层直接绑定具体实现
SpellCheckService spellCheckService = DictionarySpellCheckAdapter.defaultEnglish();

// 迭代后：通过工厂按配置切换实现
public static SpellCheckService fromSystemProperties() {
	String provider = System.getProperty(PROVIDER_KEY, "dictionary").trim().toLowerCase();
	if ("languagetool-http".equals(provider)) {
		String endpoint = System.getProperty(ENDPOINT_KEY, "https://api.languagetool.org/v2/check");
		String language = System.getProperty(LANGUAGE_KEY, "en-US");
		return new LanguageToolHttpAdapter(URI.create(endpoint), HttpClient.newHttpClient(), language);
	}
	return DictionarySpellCheckAdapter.defaultEnglish();
}
```

## 2. 测试隔离手段与差异（亮点 2 点）
- **可控 I/O 与时间**：`FakeConsole`、`FakeClock` 让交互与时钟可预测，减少不稳定因素。
- 代码示例（迭代前 / 迭代后）：
```java
// 迭代前：测试中难以控制输入与时间
Console console = new SystemConsole();
Clock clock = new SystemClock();

// 迭代后：可控输入与固定时间
@Override
public String readLine() {
	return inputs.isEmpty() ? "n" : inputs.removeFirst();
}

@Override
public LocalDateTime now() {
	return now;
}
```
- **分层回归覆盖**：`CommandChecklistRegressionTest` 覆盖新增 XML 命令链路，保证跨模块回归。
- 代码示例（迭代前 / 迭代后）：
```java
// 迭代前：仅验证文本命令链路
assertOk(reg.execute("append \"Hello\""));
assertOk(reg.execute("insert 1:1 \"X\""));

// 迭代后：加入 XML 命令链路
assertOk(reg.execute("append-child book book1 root \"Hello\""));
assertOk(reg.execute("insert-before book book0 book1 \"\""));
assertOk(reg.execute("edit-text book1 \"Title\""));
assertOk(reg.execute("edit-id book1 book001"));
assertOk(reg.execute("xml-tree"));
assertOk(reg.execute("delete book001"));
```

## 3. 测试文件分析（层次与覆盖）
- **单元层**：`TextEditorTest` 覆盖编辑与撤销重做边界，保障文本编辑核心行为。
- 代码示例（迭代前 / 迭代后）：
```java
// 迭代前：仅测试单行插入
assertEquals("ok", ed.insert(new LineCol(1, 2), "X"));
assertEquals(List.of("aXbc"), ed.lines());

// 迭代后：补充多行插入边界
assertEquals("ok", ed.insert(new LineCol(1, 3), "X\nY"));
assertEquals(List.of("abX", "Ycd"), ed.lines());
```
- **集成层**：`WorkspaceServiceTest` 验证 load/save/restore 等关键流程。
- 代码示例（迭代前 / 迭代后）：
```java
// 迭代前：仅验证 load/save
assertEquals("ok", reg1.execute("load \"" + file + "\"").output());
assertEquals("ok", reg1.execute("save").output());

// 迭代后：补充 exit/restore 流程
assertEquals("ok", reg1.execute("load \"" + file + "\"").output());
assertEquals("ok", reg1.execute("append \"New line\"").output());
ExecutionResult exit = reg1.execute("exit");
assertTrue(exit.shouldExit());
```

## 4. 改进点（2 点）
- **XML 专属异常覆盖不足**：如 ID 冲突、根节点删除、空文档 `xml-tree`，需要补充单元测试。
- **统计/拼写切换缺独立用例**：`StatisticsService` 和拼写切换工厂缺少独立测试，影响回归稳定性。

## 5. 对 Lab2 扩展性的结论
- 通过插件包与工厂注入，新增编辑器/拼写实现成本低，符合开闭原则。
- 当前迭代已完成的代码改动（用代码说明）：
```java
// XML 插件核心入口（编辑器与树结构）
public final class XmlEditor implements Editor { /* ... */ }
public final class XmlElement implements XmlNode { /* ... */ }

// 统计模块：通过事件解耦
public final class StatisticsService {
	public StatisticsService(EventBus bus, Clock clock) {
		bus.subscribe(EditorActivatedEvent.class, ev -> activate(ev.file()));
		bus.subscribe(EditorDeactivatedEvent.class, ev -> deactivate(ev.file()));
		bus.subscribe(EditorClosedEvent.class, ev -> reset(ev.file()));
	}
}

// 拼写检查可切换：工厂注入
SpellCheckService spellCheckService = SpellCheckServiceFactory.fromSystemProperties();
```
