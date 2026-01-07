package net.chen.buildShowcase.storage;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class BuildRepository {

    private final JavaPlugin plugin;
    private final Path pendingDir;
    private final Set<UUID> pending = Collections.synchronizedSet(new HashSet<>());


    public BuildRepository(Path dataFolder, JavaPlugin plugin) {
        this.pendingDir = dataFolder.resolve("pending");
        this.plugin = plugin;
        try {
            Files.createDirectories(pendingDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        plugin.getLogger().info("BuildRepository initialized");
    }

    public void submit(Player player) {
        pending.add(player.getUniqueId());
    }

    public int countPending() {
        try (Stream<Path> stream = Files.list(pendingDir)) {
            return (int) stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    public void listPending(CommandSender sender) {
        try (Stream<Path> stream = Files.list(pendingDir)) {
            stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p ->
                            sender.sendMessage("§7- §f" + p.getFileName().toString())
                    );
        } catch (IOException e) {
            sender.sendMessage("§c读取失败");
        }
    }

    public void shutdown() {
        pending.clear();
    }
}
