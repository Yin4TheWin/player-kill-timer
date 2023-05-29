package io.github.thegatesdev.playertimer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public class PlayerTimer extends JavaPlugin {
    private final PlayerTimerHandler playerTimerHandler = new PlayerTimerHandler(this,
            new PlayerTimerHandler.Settings()
                    .setMaxOnlineTime(Duration.ofSeconds(20))
    );

    @Override
    public void onEnable() {
        var s = playerTimerHandler.settings();
        Bukkit.getLogger().info("Time limit: " + s.maxOnlineTime.toSeconds() + " seconds or " + s.maxOnlineTime.toMinutes() + " minutes");
        Bukkit.getLogger().info("Resets at: " + s.resetTime.toString());
        Bukkit.getLogger().info("Timezone: " + s.zoneId.getId());
        playerTimerHandler.init();
    }
}