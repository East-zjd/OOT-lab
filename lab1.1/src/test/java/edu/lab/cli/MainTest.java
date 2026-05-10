package edu.lab.cli;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * `Main` 入口类的结构性测试。
 * <p>
 * 该测试不执行 main 的业务逻辑，而是校验入口方法签名是否符合 Java 约定。
 */
class MainTest {
    @Test
    void main_class_has_standard_entrypoint_signature() throws Exception {
        // Main 类应为 final（避免被继承）
        assertTrue(Modifier.isFinal(Main.class.getModifiers()));

        // main(String[] args) 必须是 public static 且返回 void
        Method m = Main.class.getDeclaredMethod("main", String[].class);
        int mod = m.getModifiers();
        assertTrue(Modifier.isPublic(mod));
        assertTrue(Modifier.isStatic(mod));
        assertEquals(void.class, m.getReturnType());
    }
}
