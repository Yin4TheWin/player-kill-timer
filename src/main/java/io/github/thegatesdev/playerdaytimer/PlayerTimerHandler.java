package io.github.thegatesdev.playerdaytimer;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class PlayerTimerHandler implements Listener {
    private final Plugin plugin;
    private final Settings settings;

    private TrackerTicker trackerTicker;

    public PlayerTimerHandler(Plugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public PlayerTimerHandler(Plugin plugin) {
        this(plugin, new Settings());
    }


    public void init() {
        trackerTicker = new TrackerTicker();
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // -- HANDLING

    @EventHandler
    public void handlePlayerLogin(PlayerJoinEvent event) {
        trackerTicker.startTracking(event.getPlayer().getUniqueId());
        trackerTicker.updateSchedule();
    }

    @EventHandler
    public void handlePlayerLogout(PlayerQuitEvent event) {
        trackerTicker.stopTracking(event.getPlayer().getUniqueId());
        trackerTicker.updateSchedule();
    }


    private class TrackerTicker {
        private final Map<UUID, TimeTracker> playerTimeTrackers = new TreeMap<>();
        private final PriorityQueue<ActiveTracker> activeTrackers = new PriorityQueue<>();

        private boolean isScheduled = false;
        private BukkitTask activeTask;
        private ActiveTracker scheduledTracker;

        public void nextTimeout() {
            if (scheduledTracker == null || !isScheduled) return;

            stopTracking(scheduledTracker.playerId);
            scheduledTracker.tracker.reset();
            doBan(scheduledTracker.playerId);

            isScheduled = false;
            updateSchedule();
        }

        private void doBan(UUID playerId) {
            Bukkit.getServer().getOfflinePlayer(playerId).banPlayer(
                    "Your maximum online-time of %sh %sm %ss has run out!"
                            .formatted(settings.maxOnlineTime.toHours(),
                                    settings.maxOnlineTime.toMinutes(),
                                    settings.maxOnlineTime.getSeconds()),
                    Date.from(nextResetTime().toInstant()),
                    "PlayerTimer plugin"
            );
        }

        // -- TICKER

        private void updateSchedule() {
            var current = activeTrackers.peek();

            if (isScheduled) {
                if (scheduledTracker == current) return;

                isScheduled = false;
                activeTask.cancel();
            }

            scheduledTracker = current;
            if (scheduledTracker == null) return;

            isScheduled = true;

            long millisLeft = millisLeft(scheduledTracker.tracker);
            if (millisLeft == 0) nextTimeout();
            else activeTask = Bukkit.getScheduler().runTaskLater(plugin, this::nextTimeout, millisLeft * 20 / 1000);
        }

        public void startTracking(UUID playerId) {
            var tracker = trackerOfOrNew(playerId);
            tracker.moveAfter(lastResetTime());

            tracker.startTracking();
            activeTrackers.offer(new ActiveTracker(playerId, tracker));
        }

        public boolean stopTracking(UUID playerId) {
            var tracker = trackerOf(playerId);
            if (tracker == null) return false;

            tracker.stopTracking();
            return activeTrackers.removeIf(activeTracker -> activeTracker.playerId.equals(playerId));
        }

        // -- TRACKERS

        private TimeTracker trackerOfOrNew(UUID playerId) {
            return playerTimeTrackers.computeIfAbsent(playerId, uuid -> new TimeTracker(settings.zoneId));
        }

        private TimeTracker trackerOf(UUID playerId) {
            return playerTimeTrackers.get(playerId);
        }
    }

    // -- TIME

    private ZonedDateTime lastResetTime() {
        return nextResetTime().minusDays(1);
    }

    private ZonedDateTime nextResetTime() {
        return ZonedDateTime.now(settings.zoneId).with(temporal ->
                LocalTime.from(temporal).isBefore(settings.resetTime)
                        ? temporal.with(settings.resetTime)
                        : temporal.plus(Duration.ofDays(1)).with(settings.resetTime));
    }

    public long millisLeft(TimeTracker tracker) {
        return Math.max(0, settings.maxOnlineTime.toMillis() - tracker.trackedMillis());
    }

    // -- GET / SET

    public Settings settings() {
        return settings;
    }

    // --

    public static class Settings {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime resetTime = LocalTime.MIDNIGHT;
        Duration maxOnlineTime = Duration.ofHours(1);

        public Settings zoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Settings setMaxOnlineTime(Duration maxOnlineTime) {
            this.maxOnlineTime = maxOnlineTime;
            return this;
        }

        public Settings resetTime(LocalTime resetTime) {
            this.resetTime = resetTime;
            return this;
        }
    }

    private record ActiveTracker(UUID playerId, TimeTracker tracker) implements Comparable<ActiveTracker> {

        @Override
        public int compareTo(@NotNull PlayerTimerHandler.ActiveTracker o) {
            return Long.compare(o.tracker.trackedMillis(), tracker.trackedMillis());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ActiveTracker other)) return false;
            return playerId.equals(other.playerId);
        }
    }
}