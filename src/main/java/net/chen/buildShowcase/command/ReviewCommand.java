package net.chen.buildShowcase.command;

import net.chen.buildShowcase.storage.BuildRepository;
import net.chen.buildShowcase.storage.BuildRepositorySQLite;
import net.chen.buildShowcase.storage.BuildRequest;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

public class ReviewCommand implements CommandExecutor {

    private final BuildRepositorySQLite repository;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ReviewCommand(BuildRepositorySQLite repository) {
        this.repository = repository;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!sender.hasPermission("buildshow.review")) {
            sender.sendMessage("§c你没有权限。");
            return true;
        }

        // 未来扩展点
        if (args.length == 0) {
            sender.sendMessage("§e待审核数量: §f" + repository.countPending());
            sender.sendMessage("§7使用 /buildreview list 查看详情");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {

            List<BuildRequest> pending = repository.listPending();

            sender.sendMessage("§6====== 待审核建筑 ======");

            if (pending.isEmpty()) {
                sender.sendMessage("§7暂无待审核建筑。");
            }

            for (BuildRequest req : pending) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(req.getPlayerUuid());

                String name = p.getName() != null ? p.getName() : "Unknown";
                String time = sdf.format(new Date(req.getCreatedAt()));
                String statusColor = "§c"; // PENDING 默认红色

                sender.sendMessage(
                        "§e[" + req.getId() + "] " +
                                "§b" + name +
                                " §7| §f" + time +
                                " §7| " + statusColor + req.getStatus()
                );
            }

            sender.sendMessage("§6========================");
            return true;
        }

        sender.sendMessage("§c未知子指令");
        return true;
    }
}
