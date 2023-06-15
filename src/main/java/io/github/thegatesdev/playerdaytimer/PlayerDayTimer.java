package io.github.thegatesdev.playerdaytimer;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

public class PlayerDayTimer extends JavaPlugin {
    @Override
    public void onEnable() {
        new PlayerTimerHandler(this, loadSettings());
    }

    private PlayerTimerHandler.Settings loadSettings() {
        var config = getConfig();

        var zone = config.getString("time-zone");
        var limit = config.getConfigurationSection("time-limit");
        var reset = config.getConfigurationSection("reset-time");
        return new PlayerTimerHandler.Settings(
                zone == null ? ZoneId.systemDefault() : ZoneId.of(zone),
                reset == null ? LocalTime.MIDNIGHT : LocalTime.of(
                        reset.getInt("hour", 0),
                        reset.getInt("minute", 0),
                        reset.getInt("second", 0)
                ),
                limit == null ? Duration.ofHours(1) : Duration
                        .ofHours(limit.getInt("hours", 0))
                        .plusMinutes(limit.getInt("minutes", 0))
                        .plusSeconds(limit.getInt("seconds", 0))
        );
    }
}