package org.yinftw.killTimer;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

public class KillTimer extends JavaPlugin {
    @Override
    public void onEnable() {
        new PlayerTimerHandler(this, loadSettings());
    }

    private PlayerTimerHandler.Settings loadSettings() {
        var config = getConfig();

        var zone = config.getString("time-zone");
        var limit = config.getConfigurationSection("time-limit");
        return new PlayerTimerHandler.Settings(
                zone == null ? ZoneId.systemDefault() : ZoneId.of(zone),
                limit == null ? Duration.ofHours(1) : Duration
                        .ofHours(limit.getInt("hours", 0))
                        .plusMinutes(limit.getInt("minutes", 0))
                        .plusSeconds(limit.getInt("seconds", 0))
        );
    }
}