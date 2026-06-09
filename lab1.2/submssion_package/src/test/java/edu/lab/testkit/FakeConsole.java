package edu.lab.testkit;

import edu.lab.core.console.Console;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 测试用假控制台。
 * <p>
 * - inputs：按顺序返回给 readLine
 * - outputs：收集 print/println 的输出，便于断言
 */
public final class FakeConsole implements Console {
    private final Deque<String> inputs = new ArrayDeque<>();
    private final List<String> outputs = new ArrayList<>();

    /**
     * 追加多行输入（按顺序依次被 readLine 消费）。
     */
    public FakeConsole addInputs(String... lines) {
        for (String l : lines) {
            inputs.addLast(l);
        }
        return this;
    }

    public List<String> outputs() {
        return List.copyOf(outputs);
    }

    public String joinedOutput() {
        // 方便做 contains 断言
        return String.join("\n", outputs);
    }

    @Override
    public void print(String s) {
        // 这里不区分 print/println，统一收集
        outputs.add(s);
    }

    @Override
    public void println(String s) {
        outputs.add(s);
    }

    @Override
    public String readLine() {
        // 若 inputs 用尽，则默认返回 "n"（用于各种 y/n 提示的默认选择）
        return inputs.isEmpty() ? "n" : inputs.removeFirst();
    }
}
