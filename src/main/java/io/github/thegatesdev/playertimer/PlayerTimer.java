package io.github.thegatesdev.playertimer;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

public class PlayerTimer extends JavaPlugin {
    private PlayerTimerHandler playerTimerHandler;

    @Override
    public void onLoad() {
        var config = getConfig();

        var settings = new PlayerTimerHandler.Settings();
        if (config.isString("time-zone"))
            settings.zoneId(ZoneId.of(Objects.requireNonNull(config.getString("time-zone"))));
        if (config.isConfigurationSection("time-limit")) {
            var section = Objects.requireNonNull(config.getConfigurationSection("time-limit"));
            settings.setMaxOnlineTime(
                    Duration.ofHours(section.getInt("hours", 0))
                            .plusMinutes(section.getInt("minutes", 0))
                            .plusSeconds(section.getInt("seconds", 0))
            );
        }
        if (config.isConfigurationSection("reset-time")) {
            var section = Objects.requireNonNull(config.getConfigurationSection("reset-time"));
            settings.resetTime(LocalTime.of(
                    section.getInt("hours", 0),
                    section.getInt("minutes", 0),
                    section.getInt("seconds", 0)
            ));
        }

        playerTimerHandler = new PlayerTimerHandler(this, settings);

        getLogger().info("SETTINGS: Timezone='%s' Timelimit='%s' ResetTime='%s'"
                .formatted(
                        settings.zoneId.getId(),
                        settings.maxOnlineTime.toHoursPart() + "h " +
                                settings.maxOnlineTime.toMinutesPart() + "m " +
                                settings.maxOnlineTime.toSecondsPart() + "s",
                        settings.resetTime
                ));
    }

    @Override
    public void onEnable() {
        playerTimerHandler.init();
    }
}