package edu.lab.core.workspace;

import edu.lab.core.fs.FileSystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录树打印器。
 * <p>
 * 生成类似 Unix `tree` 的目录结构文本。排序规则：目录在前、文件在后；同类按名称排序。
 */
final class DirTreePrinter {
    private DirTreePrinter() {
    }

    /**
     * 从 root 开始打印目录树。
     */
    static String print(FileSystem fs, Path root) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<Path> children = fs.list(root);
        List<Path> dirs = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        for (Path p : children) {
            // 先分类：目录与文件分开收集
            if (fs.isDirectory(p)) {
                dirs.add(p);
            } else {
                files.add(p);
            }
        }
        // 先目录后文件
        List<Path> ordered = new ArrayList<>(dirs);
        ordered.addAll(files);

        for (int i = 0; i < ordered.size(); i++) {
            boolean last = i == ordered.size() - 1;
            render(fs, ordered.get(i), "", last, sb);
        }

        return sb.toString().trim();
    }

    private static void render(FileSystem fs, Path node, String prefix, boolean last, StringBuilder sb) throws IOException {
        // prefix 用于画出树形结构的竖线/缩进
        sb.append(prefix);
        sb.append(last ? "└── " : "├── ");
        sb.append(node.getFileName());
        sb.append('\n');

        if (!fs.isDirectory(node)) {
            return;
        }

        List<Path> children = fs.list(node);
        List<Path> dirs = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        for (Path p : children) {
            if (fs.isDirectory(p)) {
                dirs.add(p);
            } else {
                files.add(p);
            }
        }
        List<Path> ordered = new ArrayList<>(dirs);
        ordered.addAll(files);

        // 计算下一层的 prefix：最后一个节点不需要继续画竖线
        String nextPrefix = prefix + (last ? "    " : "│   ");
        for (int i = 0; i < ordered.size(); i++) {
            render(fs, ordered.get(i), nextPrefix, i == ordered.size() - 1, sb);
        }
    }
}
