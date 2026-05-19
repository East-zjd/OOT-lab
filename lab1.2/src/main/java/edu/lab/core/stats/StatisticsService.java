package edu.lab.core.stats;

import edu.lab.core.events.EditorActivatedEvent;
import edu.lab.core.events.EditorClosedEvent;
import edu.lab.core.events.EditorDeactivatedEvent;
import edu.lab.core.events.EventBus;
import edu.lab.core.time.Clock;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 会话内编辑时长统计服务。
 */
public final class StatisticsService {
    private final Clock clock;
    private final Map<Path, Duration> totals = new HashMap<>();
    private final Map<Path, LocalDateTime> activeSince = new HashMap<>();

    public StatisticsService(EventBus bus, Clock clock) {
        this.clock = clock;
        bus.subscribe(EditorActivatedEvent.class, ev -> activate(ev.file()));
        bus.subscribe(EditorDeactivatedEvent.class, ev -> deactivate(ev.file()));
        bus.subscribe(EditorClosedEvent.class, ev -> reset(ev.file()));
    }

    public Duration durationFor(Path file) {
        Duration total = totals.getOrDefault(file, Duration.ZERO);
        LocalDateTime start = activeSince.get(file);
        if (start == null) {
            return total;
        }
        return total.plus(Duration.between(start, clock.now()));
    }

    public String formatDuration(Path file) {
        return DurationFormatter.format(durationFor(file));
    }

    private void activate(Path file) {
        if (file == null || activeSince.containsKey(file)) {
            return;
        }
        activeSince.put(file, clock.now());
    }

    private void deactivate(Path file) {
        if (file == null) {
            return;
        }
        LocalDateTime start = activeSince.remove(file);
        if (start == null) {
            return;
        }
        Duration delta = Duration.between(start, clock.now());
        totals.put(file, totals.getOrDefault(file, Duration.ZERO).plus(delta));
    }

    private void reset(Path file) {
        if (file == null) {
            return;
        }
        activeSince.remove(file);
        totals.remove(file);
    }
}
