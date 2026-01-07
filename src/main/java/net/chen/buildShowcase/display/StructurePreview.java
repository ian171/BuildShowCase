package net.chen.buildShowcase.display;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.FileInputStream;

public class StructurePreview {

    public static void pastePreview(
            File schemFile,
            org.bukkit.World bukkitWorld,
            BlockVector3 pastePos
    ) {
        FaweAPI.getTaskManager().async(() -> {
            try {
                Clipboard clipboard = null;

                // 1️⃣ 读取 schem
                try (FileInputStream fis = new FileInputStream(schemFile);
                     ClipboardReader reader =
                             ClipboardFormats.findByFile(schemFile).getReader(fis)) {
                    clipboard = reader.read();
                }catch (Exception e){
                    e.printStackTrace();
                }

                // 2️⃣ 粘贴到世界
                World world = BukkitAdapter.adapt(bukkitWorld);

                try (EditSession editSession = WorldEdit.getInstance()
                        .newEditSessionBuilder()
                        .world(world)
                        .fastMode(true)
                        .build()) {

                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(pastePos)
                            .ignoreAirBlocks(true)
                            .build();

                    Operations.complete(operation);
                }catch (Exception e){
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

