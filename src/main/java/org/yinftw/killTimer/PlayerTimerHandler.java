package org.yinftw.killTimer;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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

    private final Map<UUID, TimeTracker> trackers = new TreeMap<>();
    private final Queue<ActiveTracker> activeTrackers = new PriorityQueue<>();

    private boolean specialKillTriggered = false;

    private boolean isScheduled;
    private BukkitTask scheduledTask;
    private ActiveTracker scheduledTracker;

    public PlayerTimerHandler(Plugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // --
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Check if the special kill condition was triggered
        if (specialKillTriggered) {
            // Set the custom death message if the special condition is met
            event.setDeathMessage(player.getName() + " is out of time");
            // Reset the trigger to avoid this message on other deaths
            specialKillTriggered = false;
        }
    }
    @EventHandler
    public void handlePlayerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Retrieve the player's tracker
        TimeTracker tracker = trackerOf(playerId);
        long trackedMillis = tracker != null ? tracker.trackedMillis() : 0;
        long maxMillis = settings.maxOnlineTime.toMillis();

        // Calculate remaining time
        long remainingMillis = Math.max(0, maxMillis - trackedMillis);

        // Convert remaining time into hours, minutes, and seconds
        long hours = remainingMillis / 3600000;
        long minutes = (remainingMillis % 3600000) / 60000;
        long seconds = (remainingMillis % 60000) / 1000;

        event.getPlayer().sendMessage(Component.text("Welcome "+event.getPlayer().getName()+"! You will die in %sh %sm %ss."
                .formatted(hours, minutes, seconds)));
        track(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void handlePlayerLogout(PlayerQuitEvent event) {
        noTrack(event.getPlayer().getUniqueId());
    }

    // --

    private void onTimeout() {
        if (!isScheduled || scheduledTracker == null) return;
        isScheduled = false;

        activeTrackers.poll();
        noTrack(scheduledTracker.playerId);

        scheduledTracker.tracker.reset();
        doBan(scheduledTracker.playerId);

        scheduledTracker = null;
        scheduledTask = null;

        updateSchedule();
    }

    private void updateSchedule() {
        var active = activeTrackers.peek();
        if (isScheduled) {
            if (scheduledTracker == active) return;

            scheduledTask.cancel();
            isScheduled = false;
        }

        scheduledTracker = active;
        if (scheduledTracker == null) return;

        long millisLeft = Math.max(0, settings.maxOnlineTime.toMillis() - scheduledTracker.tracker.trackedMillis());
        if (millisLeft == 0) {
            onTimeout();
            return;
        }
        isScheduled = true;
        scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, this::onTimeout, (long) (millisLeft * 0.02));
    }

    private void track(UUID playerId) {
        var tracker = trackerOfOrNew(playerId);
        tracker.moveAfter(settings.lastReset());

        activeTrackers.offer(new ActiveTracker(playerId, tracker));
        tracker.startTracking();

        updateSchedule();
    }

    private void noTrack(UUID playerId) {
        var tracker = trackerOf(playerId);
        if (tracker == null) return;

        if (activeTrackers.removeIf(t -> t.playerId.equals(playerId))) {
            tracker.stopTracking();
            updateSchedule();
        }
    }

    private TimeTracker trackerOfOrNew(UUID playerId) {
        return trackers.computeIfAbsent(playerId, uuid -> new TimeTracker(settings.timeZone));
    }

    private TimeTracker trackerOf(UUID playerId) {
        return trackers.get(playerId);
    }

    // --

    private void doBan(UUID playerId) {
        specialKillTriggered = true;
        Bukkit.getPlayer(playerId).setHealth(0.0);
    }

    // --

    private record ActiveTracker(UUID playerId, TimeTracker tracker) implements Comparable<ActiveTracker> {
        @Override
        public int compareTo(@NotNull PlayerTimerHandler.ActiveTracker o) {
            return Long.compare(o.tracker.trackedMillis(), tracker.trackedMillis());
        }
    }

    public record Settings(ZoneId timeZone, Duration maxOnlineTime) {
        public ZonedDateTime nextReset() {
            return ZonedDateTime.now(timeZone).with(temporal ->
                    (temporal));
        }

        public ZonedDateTime lastReset() {
            return nextReset().minusDays(1);
        }
    }
}
