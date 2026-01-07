package net.chen.buildShowcase.display;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.FileOutputStream;

public class StructureExporter {

    public static void exportStructureAsync(
            World world,
            BlockVector3 min,
            BlockVector3 max,
            File output
    ) {

        // FAWE 提供的异步调度
        FaweAPI.getTaskManager().async(() -> {
            try {
                CuboidRegion region = new CuboidRegion(world, min, max);
                BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
                clipboard.setOrigin(min);

                try (EditSession editSession =
                             WorldEdit.getInstance()
                                     .newEditSessionBuilder()
                                     .world(world)
                                     .fastMode(true)
                                     .build()) {

                    ForwardExtentCopy copy = new ForwardExtentCopy(
                            editSession,
                            region,
                            clipboard,
                            region.getMinimumPoint()
                    );

                    Operations.complete(copy);
                }

                output.getParentFile().mkdirs();

                // ✅ 关键：使用 BuiltInClipboardFormat
                BuiltInClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

                try (ClipboardWriter writer =
                             format.getWriter(new FileOutputStream(output))) {
                    writer.write(clipboard);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
