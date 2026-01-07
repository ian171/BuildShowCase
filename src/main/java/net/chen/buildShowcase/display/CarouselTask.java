package net.chen.buildShowcase.display;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CarouselTask {

    private final JavaPlugin plugin;
    private BukkitRunnable task;
    private int index = 0;

    public CarouselTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                // TODO: 切换展示建筑
                Bukkit.getLogger().info("[BuildShowcase] Carousel tick: " + index++);
            }
        };
        task.runTaskTimer(plugin, 20L, 20L * 10); // 每10秒轮播
    }

    public void stop() {
        if (task != null) task.cancel();
    }
}
