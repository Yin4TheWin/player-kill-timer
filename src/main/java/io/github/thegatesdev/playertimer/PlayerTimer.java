package io.github.thegatesdev.playertimer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerTimer extends JavaPlugin {
    private final PlayerTimerHandler playerTimerHandler = new PlayerTimerHandler(this,
            new PlayerTimerHandler.Settings()
                    .maxMinutesOnline(1)
    );

    @Override
    public void onEnable() {
        var s = playerTimerHandler.settings();
        Bukkit.getLogger().info("Time limit: " + s.maxTicksOnline / 20 / 60 + " minutes");
        Bukkit.getLogger().info("Resets at: " + s.resetTime.toString());
        Bukkit.getLogger().info("Timezone: " + s.zoneId.getId());
        playerTimerHandler.init();
    }
}