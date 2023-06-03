package io.github.thegatesdev.playertimer;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeTracker {
    private final ZoneId zoneId;

    private long trackedTime;
    private ZonedDateTime sessionStartTime;
    private boolean activeSession = false;

    public TimeTracker(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    // -- TRACKING

    public void startTracking() {
        if (activeSession) return;
        sessionStartTime = ZonedDateTime.now(zoneId);
        activeSession = true;
    }

    public void stopTracking() {
        if (!activeSession) return;
        trackedTime = trackedMillis();
        activeSession = false;
    }

    public void moveAfter(ZonedDateTime minimumStartTime) {
        if (sessionStartTime != null && sessionStartTime.isBefore(minimumStartTime)) {
            sessionStartTime = minimumStartTime;
            reset();
        }
    }

    public void reset() {
        trackedTime = 0;
    }

    // -- GET / SET

    public Duration sessionTime() {
        if (!activeSession) return Duration.ZERO;
        return Duration.between(sessionStartTime, ZonedDateTime.now(zoneId));
    }

    public long trackedMillis() {
        return trackedTime + sessionTime().toMillis();
    }
}
