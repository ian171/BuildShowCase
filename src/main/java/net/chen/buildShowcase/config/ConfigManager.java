package net.chen.buildShowcase.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;

public class ConfigManager {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false); // 初次复制默认配置
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载默认配置，用于合并新字段
        try (InputStream defStream = plugin.getResource("config.yml")) {
            if (defStream != null) {
                FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defStream));
                mergeDefaults(config, defConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        save();
    }

    /**
     * 自动合并新字段
     */
    private void mergeDefaults(FileConfiguration target, FileConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
            }
        }
    }

    /**
     * 获取配置实例
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * 保存配置
     */
    public void save() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 手动重载配置（可热更新）
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);

        try (InputStream defStream = plugin.getResource("config.yml")) {
            if (defStream != null) {
                FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defStream));
                mergeDefaults(config, defConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        save();
    }
}
