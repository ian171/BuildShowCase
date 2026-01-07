package net.chen.buildShowcase.display;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.FileInputStream;

public class FaweRenderer {

    public static void pasteStructureAsync(
            World world,
            File structure,
            BlockVector3 pastePos,
            boolean pasteAir
    ) {

        FaweAPI.getTaskManager().async(() -> {
            try {
                ClipboardFormat format = ClipboardFormats.findByFile(structure);
                if (format == null) return;

                Clipboard clipboard;
                try (ClipboardReader reader =
                             format.getReader(new FileInputStream(structure))) {
                    clipboard = reader.read();
                }

                try (EditSession editSession =
                             WorldEdit.getInstance()
                                     .newEditSessionBuilder()
                                     .world(world)
                                     .build()) {

                    ClipboardHolder holder = new ClipboardHolder(clipboard);

                    Operation operation = holder
                            .createPaste(editSession)
                            .to(pastePos)
                            .ignoreAirBlocks(!pasteAir)
                            .build();

                    Operations.complete(operation);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


}
