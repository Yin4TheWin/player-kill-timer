package io.github.thegatesdev.playertimer;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.*;
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
    public void handlePlayerLogin(PlayerLoginEvent event) {
        trackerTicker.startTracking(event.getPlayer().getUniqueId());
        trackerTicker.updateSchedule();
    }

    @EventHandler
    public void handlePlayerLogout(PlayerQuitEvent event) {
        trackerTicker.stopTracking(event.getPlayer().getUniqueId());
        trackerTicker.updateSchedule();
    }


    private class TrackerTicker extends BukkitRunnable {
        private final Map<UUID, TimeTracker> playerTimeTrackers = new TreeMap<>();
        private final PriorityQueue<ActiveTracker> activeTrackers = new PriorityQueue<>();

        private boolean isScheduled = false;
        private ActiveTracker scheduledTracker;

        @Override
        public void run() {
            if (scheduledTracker == null || !isScheduled) return;

            var tracker = scheduledTracker.tracker;
            if (timeLeft(tracker) != 0) return;

            stopTracking(scheduledTracker.playerId);
            tracker.reset();
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
            if (scheduledTracker == current) return;

            if (isScheduled && !isCancelled()) cancel();

            scheduledTracker = current;
            if (scheduledTracker == null) return;

            isScheduled = true;
            runTaskLater(plugin, timeLeft(scheduledTracker.tracker));
        }

        public void startTracking(UUID playerId) {
            var tracker = trackerOfOrNew(playerId);
            tracker.resetTo(lastResetTime());

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
        return ZonedDateTime.of(LocalDate.now(), settings.resetTime, settings.zoneId);
    }

    private ZonedDateTime nextResetTime() {
        return lastResetTime().plusDays(1);
    }

    public long timeLeft(TimeTracker tracker) {
        return Math.max(0, settings.maxTicksOnline() - tracker.trackedTime());
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

        private long maxTicksOnline() {
            return maxOnlineTime.toSeconds() * 20;
        }
    }

    private record ActiveTracker(UUID playerId, TimeTracker tracker) implements Comparable<ActiveTracker> {

        @Override
        public int compareTo(@NotNull PlayerTimerHandler.ActiveTracker o) {
            return Long.compare(o.tracker.trackedTime(), tracker.trackedTime());
        }
    }
}