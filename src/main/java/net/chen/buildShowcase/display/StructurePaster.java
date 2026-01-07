package net.chen.buildShowcase.display;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.FileInputStream;

public class StructurePaster {

    /**
     * 异步粘贴结构到世界
     */
    public static void pasteAsync(World world, File schematicFile, BlockVector3 pos) {
        FaweAPI.getTaskManager().async(() -> {
            try (FileInputStream fis = new FileInputStream(schematicFile)) {
                BuiltInClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
                try (ClipboardReader reader = format.getReader(fis);
                     EditSession editSession = WorldEdit.getInstance()
                             .newEditSessionBuilder()
                             .world(world)
                             .fastMode(true)
                             .build()) {

                    var clipboard = reader.read();
                    ClipboardHolder holder = new ClipboardHolder(clipboard);

                    Operation op = holder.createPaste(editSession)
                            .to(pos)
                            .ignoreAirBlocks(true)
                            .build();

                    Operations.complete(op);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
