package io.github.thegatesdev.playertimer;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeTracker {
    private final ZoneId zoneId;

    private long trackedTime;
    private ZonedDateTime lastStartTime;
    private boolean activeSession = false;

    public TimeTracker(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    // -- TRACKING

    public void startTracking() {
        if (activeSession) throw new RuntimeException("Already tracking time");
        lastStartTime = ZonedDateTime.now(zoneId);
        activeSession = true;
    }

    public void stopTracking() {
        if (!activeSession) throw new RuntimeException("Already not tracking time");
        trackedTime += sessionTime().toMillis();
        activeSession = false;
    }

    public void resetTo(ZonedDateTime minimumStartTime) {
        if (activeSession && lastStartTime.isBefore(minimumStartTime)) {
            lastStartTime = minimumStartTime;
            reset();
        }
    }

    public void reset() {
        trackedTime = 0;
    }

    // -- GET / SET

    public Duration sessionTime() {
        if (!activeSession) return Duration.ZERO;
        return Duration.between(lastStartTime, ZonedDateTime.now(zoneId));
    }

    public long trackedTime() {
        return trackedTime + sessionTime().toMillis();
    }
}
