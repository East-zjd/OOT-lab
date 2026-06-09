package edu.lab.core.stats;

import java.time.Duration;

/**
 * 编辑时长格式化工具。
 */
public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String format(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "秒";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            long remainMinutes = minutes % 60;
            return hours + "小时" + remainMinutes + "分钟";
        }
        long days = hours / 24;
        long remainHours = hours % 24;
        return days + "天" + remainHours + "小时";
    }
}
