package io.github.thegatesdev.playertimer;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class PlayerTimerHandler implements Listener {
    private final Plugin plugin;
    private final Settings settings;

    private final Map<UUID, TimeTracker> playerTimeTrackers = new TreeMap<>();
    private final PriorityQueue<ActiveTracker> activeTrackers = new PriorityQueue<>();
    private final TrackerTicker trackerTicker = new TrackerTicker();

    public PlayerTimerHandler(Plugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public PlayerTimerHandler(Plugin plugin) {
        this(plugin, new Settings());
    }


    // -- HANDLING


    @EventHandler
    public void handlePlayerLogin(PlayerLoginEvent event) {
        var player = event.getPlayer();
        var tracker = trackerOfOrNew(player.getUniqueId());

        tracker.resetTo(lastResetTime());
        tracker.startTracking();
        activeTrackers.offer(new ActiveTracker(player.getUniqueId(), tracker));

        trackerTicker.updateSchedule();
    }

    @EventHandler
    public void handlePlayerLogout(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var tracker = trackerOf(player.getUniqueId());
        if (tracker == null) return;

        tracker.stopTracking();
    }

    private class TrackerTicker extends BukkitRunnable {
        private BukkitTask runningTask;
        private ActiveTracker current;

        @Override
        public void run() {
            if (current != null && timeLeft(current.tracker) == 0) {
                Bukkit.getServer().getOfflinePlayer(current.playerId).banPlayer(
                        "Your online-time of %s minutes has run out!".formatted(settings.maxTicksOnline / 20 / 60),
                        Date.from(nextResetTime().toInstant()),
                        "PlayerTimer plugin"
                );
            }
            updateSchedule();
        }

        public void updateSchedule() {
            // Since activeTrackers is ordered, this is always the tracker with the least time left
            var nextTracker = activeTrackers.peek();
            if (nextTracker == null || nextTracker == current)
                return; // Already tracking this tracker
            if (runningTask != null) {
                runningTask.cancel();
                runningTask = null;
            }
            current = nextTracker;
            this.runTaskLater(plugin, timeLeft(nextTracker.tracker));
        }
    }


    // -- TRACKERS


    private TimeTracker trackerOfOrNew(UUID playerId) {
        return playerTimeTrackers.computeIfAbsent(playerId, uuid -> new TimeTracker(settings.zoneId));
    }

    private TimeTracker trackerOf(UUID playerId) {
        return playerTimeTrackers.get(playerId);
    }


    // -- TIME


    private ZonedDateTime lastResetTime() {
        return ZonedDateTime.of(LocalDate.now(), settings.resetTime, settings.zoneId);
    }

    private ZonedDateTime nextResetTime() {
        return lastResetTime().plusDays(1);
    }

    public long timeLeft(TimeTracker tracker) {
        return Math.max(0, settings.maxTicksOnline - tracker.trackedTime());
    }

    // --

    public static class Settings {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime resetTime = LocalTime.MIDNIGHT;
        int maxTicksOnline = 72000; // One hour

        public Settings zoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Settings maxMinutesOnline(int maxMinutesOnline) {
            this.maxTicksOnline = maxMinutesOnline * 60 * 20;
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
            return Long.compare(o.tracker.trackedTime(), tracker.trackedTime());
        }
    }
}