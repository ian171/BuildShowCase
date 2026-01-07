package net.chen.buildShowcase;

import jdk.jfr.Description;
import net.chen.buildShowcase.command.ReviewCommandSQLite;
import net.chen.buildShowcase.command.SubmitCommand;
import net.chen.buildShowcase.config.ConfigManager;
import net.chen.buildShowcase.download.HttpsFileServer;
import net.chen.buildShowcase.storage.BuildRepositorySQLite;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class BuildShowcasePlugin extends JavaPlugin {

    private static BuildShowcasePlugin instance;
    private BuildRepositorySQLite repository;
    private ConfigManager configManager;
    @Description("Download")
    private Thread nettyThread;

    @Override
    public void onEnable() {
        long ms1 = System.currentTimeMillis();
        instance = this;
        // 初始化配置
        configManager = new ConfigManager(this);
        int port = configManager.getConfig().getInt("download-port", 8080);
        File structureDir = new File(getDataFolder(), "structures/builds");
        structureDir.mkdirs();
        File root = new File(getDataFolder(), "downloads");
        root.mkdirs();

        File cert = new File(getDataFolder(), "cert/fullchain.pem");
        File key  = new File(getDataFolder(), "cert/privkey.pem");

        nettyThread =  new Thread(() -> {
            try {
                new HttpsFileServer(port, root, cert, key).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "BuildShowcase-DownloadServer");
        nettyThread.start();
        //File structureFolder = new File(Objects.requireNonNull(configManager.getConfig().getString("structure.save-path")));
        //repository = new BuildRepository(structureFolder.toPath(),this);
        repository = new BuildRepositorySQLite(getDataFolder().getAbsolutePath(),this);
        repository.init();
        //getCommand("buildsubmit").setExecutor(new SubmitCommand(repository));
        // 注册命令
        getCommand("buildsubmit")
                .setExecutor(new SubmitCommand(repository));
        getCommand("buildreview").setExecutor(new ReviewCommandSQLite(repository));

        long ms2 = System.currentTimeMillis();
        getLogger().info("BuildShowcase enabled in"+(ms2-ms1)+"ms");
    }

    @Override
    public void onDisable() {
        repository.shutdown();
        nettyThread.interrupt();
        nettyThread = null;
    }

    public static BuildShowcasePlugin getInstance() {
        return instance;
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
