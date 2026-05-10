package edu.lab.cli;

import edu.lab.core.app.App;
import edu.lab.core.app.AppFactory;

/**
 * 程序入口。
 * <p>
 * 负责创建默认的应用对象，并启动交互式命令行编辑器。
 */
public final class Main {
    /**
     * Java 标准入口方法。
     *
     * @param args 命令行参数（当前实现不使用）
     */
    public static void main(String[] args) {
        // 组装应用的默认依赖（控制台/工作区/命令注册表等）
        var app = AppFactory.defaultApp();
        // 进入交互循环，直到用户退出
        App.ExitCode exitCode = app.runInteractive();
        // 将应用退出码交还给操作系统
        System.exit(exitCode.code());
    }
}
