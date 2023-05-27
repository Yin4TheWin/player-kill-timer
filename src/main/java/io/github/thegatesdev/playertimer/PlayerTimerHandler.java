package io.github.thegatesdev.playertimer;

import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTimerHandler implements Listener {
    private final Server server;
    private final Settings settings;
    private final Map<UUID, TimeTracker> playerTimeTrackers = new HashMap<>();

    public PlayerTimerHandler(Server server, Settings settings) {
        this.server = server;
        this.settings = settings;
    }

    public PlayerTimerHandler(Server server) {
        this(server, new Settings());
    }

    // -- TIME

    private ZonedDateTime resetTime() {
        return ZonedDateTime.of(LocalDate.now(), settings.resetTime, settings.zoneId);
    }

    // -- HANDLING

    @EventHandler
    public void handlePlayerLogin(PlayerLoginEvent event) {
        var player = event.getPlayer();
        var tracker = trackerOfOrNew(player.getUniqueId());

        tracker.resetTo(resetTime());
        tracker.startTracking();
    }

    @EventHandler
    public void handlePlayerLogout(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var tracker = trackerOf(player.getUniqueId());
        if (tracker == null) return;

        tracker.stopTracking();
    }


    // -- TRACKERS

    private TimeTracker trackerOfOrNew(UUID playerId) {
        return playerTimeTrackers.computeIfAbsent(playerId, uuid -> new TimeTracker(settings.zoneId));
    }

    private TimeTracker trackerOf(UUID playerId) {
        return playerTimeTrackers.get(playerId);
    }

    // --

    public static class Settings {
        boolean ipBan = false;
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime resetTime = LocalTime.MIDNIGHT;
        int maxTicksOnline = 72000; // One hour

        public Settings ipBan(boolean ipBan) {
            this.ipBan = ipBan;
            return this;
        }

        public Settings zoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Settings maxMinutesOnline(int maxMinutesOnline) {
            this.maxTicksOnline = maxMinutesOnline * 60 * 20;
            return this;
        }
    }
}