package net.chen.buildShowcase.command;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.chen.buildShowcase.storage.BuildRepositorySQLite;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;

public class SubmitCommand implements CommandExecutor {

    private final BuildRepositorySQLite repository;

    public SubmitCommand(BuildRepositorySQLite repository) {
        this.repository = repository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        // 获取玩家 WorldEdit session
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        if (session == null) {
            player.sendMessage("§c无法获取 WorldEdit 会话！");
            return true;
        }

        Region region = session.getSelection((World) BukkitAdapter.adapt(player).getExtent());
        if (region == null) {
            player.sendMessage("§c请先用 WorldEdit 选择区域！");
            return true;
        }

        // 异步导出
        File buildsFolder = new File(player.getServer().getPluginManager()
                .getPlugin("BuildShowcase").getDataFolder(), "structures/builds");
        if (!buildsFolder.exists()) buildsFolder.mkdirs();

        String filename = sender.getName() + "-" + System.currentTimeMillis() + ".schem";
        File file = new File(buildsFolder, filename);

        FaweAPI.getTaskManager().async(() -> {
            try {
                World world = BukkitAdapter.adapt(player.getWorld());

                // 1️⃣ 创建 Clipboard
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
                clipboard.setOrigin(min);

                // 2️⃣ 拷贝选区到 Clipboard
//                try (EditSession editSession = WorldEdit.getInstance()
//                        .newEditSessionBuilder()
//                        .world(world)
//                        .fastMode(true)
//                        .build()) {
//                    clipboard.paste(editSession,BlockVector3.at(player.getX(), player.getY(), player.getZ()),true);
//                }

                // 3️⃣ 写入 .schem 文件
                BuiltInClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

                try (FileOutputStream fos = new FileOutputStream(file);
                     ClipboardWriter writer = format.getWriter(fos)) {

                    writer.write(clipboard);
                }


                // 4️⃣ 保存 SQLite
                repository.submit(player.getUniqueId(), file.getAbsolutePath());

                player.sendMessage("§a建筑已提交，等待管理员审核！");
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage("§c提交失败，请查看控制台！");
            }
        });

        return true;
    }
}
