package edu.lab.core.commands;

import java.util.List;

/**
 * 已解析的命令。
 *
 * @param name 命令名（tokens[0]）
 * @param args 参数列表（tokens[1..]）
 */
public record ParsedCommand(String name, List<String> args) {
}
