package net.chen.buildShowcase.command;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import net.chen.buildShowcase.display.DisplaySlotCalculator;
import net.chen.buildShowcase.display.StructurePaster;
import net.chen.buildShowcase.display.StructurePreview;
import net.chen.buildShowcase.storage.BuildRepositorySQLite;
import net.chen.buildShowcase.storage.BuildRequest;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

public class ReviewCommandSQLite implements CommandExecutor {

    private final BuildRepositorySQLite repository;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ReviewCommandSQLite(BuildRepositorySQLite repository) {
        this.repository = repository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("buildshow.review")) {
            sender.sendMessage("§c你没有权限。");
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

        if (args.length >= 2) {
            String sub = args[0];
            String id = args[1];

            if (sub.equalsIgnoreCase("approve")) {
                int requestId;
                try {
                    requestId = Integer.parseInt(id);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cID 格式错误");
                    return true;
                }

                BuildRequest req = repository.findById(requestId);
                if (req == null || !req.getStatus().equals("PENDING")) {
                    sender.sendMessage("§c不存在该申请或已审核");
                    return true;
                }

                // 先更新状态

                sender.sendMessage(repository.approve(Integer.parseInt(id)));
                // 只传 id
                sender.sendMessage("§a已通过审核: " + requestId);

                // FAWE 粘贴
                World world = BukkitAdapter.adapt(Bukkit.getWorld("showcase"));
                int index = requestId - 1; // 或数据库顺序
                BlockVector3 origin = BlockVector3.at(0, 100, 0);
                BlockVector3 pos = DisplaySlotCalculator.calculate(origin, index, 200, 5);

                // 用原始 structurePath
                File schematicFile = new File(req.getStructurePath());
                StructurePaster.pasteAsync(world, schematicFile, pos);
                return true;
            }

            if (sub.equalsIgnoreCase("show")){
                Player player = (Player) sender;
                File schem = null;
                try {
                    System.out.println(repository.findById(Integer.parseInt(args[1])).getStructurePath());
                    schem = new File(repository.findById(Integer.parseInt(args[1])).getStructurePath());
                } catch (NumberFormatException e) {
                    player.sendMessage("无效的数字");
                    return false;
                }
                // 粘贴到玩家脚下 + 5 格
                BlockVector3 pastePos = BlockVector3.at(
                        player.getLocation().getBlockX(),
                        player.getLocation().getBlockY() + 5,
                        player.getLocation().getBlockZ()
                );

                StructurePreview.pastePreview(
                        schem,
                        player.getWorld(),
                        pastePos
                );


                player.sendMessage("§a已生成结构用于审核。");
                return true;

            }

            if (sub.equalsIgnoreCase("reject")) {
                repository.reject(id);
                sender.sendMessage("§c已拒绝: " + id);
                return true;
            }
        }

        sender.sendMessage("§c未知子指令");
        return true;
    }
}
