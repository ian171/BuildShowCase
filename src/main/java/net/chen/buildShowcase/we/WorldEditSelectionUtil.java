package net.chen.buildShowcase.we;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.entity.Player;

public class WorldEditSelectionUtil {

    public static Selection getSelection(Player player) throws Exception {

        BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance()
                .getSessionManager()
                .get(wePlayer);

        if (session == null) {
            throw new Exception("WorldEdit session not found");
        }

        Region region = session.getSelection(wePlayer.getWorld());
        if (region == null) {
            throw new Exception("你还没有选择区域");
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        World weWorld = region.getWorld();

        return new Selection(weWorld, min, max);
    }

    public record Selection(
            World world,
            BlockVector3 min,
            BlockVector3 max
    ) {}
}
