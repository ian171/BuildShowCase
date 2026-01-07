package net.chen.buildShowcase.storage;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import net.chen.buildShowcase.display.DisplaySlotCalculator;
import net.chen.buildShowcase.display.StructurePaster;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuildRepositorySQLite extends BuildRepository {

    private final Connection conn;

    public BuildRepositorySQLite(String pluginDataFolder, JavaPlugin javaPlugin) {
        super(Path.of(pluginDataFolder),javaPlugin);
        try {
            String url = "jdbc:sqlite:" + pluginDataFolder + "/data.db";
            conn = DriverManager.getConnection(url);
            createTable();
        } catch (SQLException e) {
            throw new RuntimeException("无法连接 SQLite 数据库", e);
        }
    }

    private void createTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS build_requests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            structure_path TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at INTEGER NOT NULL
        );
    """;

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 提交建筑
    public void submit(UUID playerUuid, String structurePath) {

        String sql = """
        INSERT INTO build_requests
        (player_uuid, structure_path, status, created_at)
        VALUES (?, ?, ?, ?)
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, structurePath);
            ps.setString(3, "PENDING");
            ps.setLong(4, System.currentTimeMillis()); // ⚠️ 一定是 long

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // 获取待审核数量
    @Override
    public int countPending() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM build_requests WHERE status='PENDING'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 列出待审核

    public List<BuildRequest> listPending() {
        List<BuildRequest> list = new ArrayList<>();

        String sql = """
        SELECT * FROM build_requests
        WHERE status = 'PENDING'
        ORDER BY created_at ASC
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new BuildRequest(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("structure_path"),
                        rs.getString("status"),
                        rs.getLong("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }


    // 审核通过

    public @NotNull String approve(int id) {
        String sql = "UPDATE build_requests SET status = 'APPROVED' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return "false";
        }
        return "true";
    }


    // 拒绝
    public void reject(String id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE build_requests SET status='REJECTED' WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * 自动将待审核建筑生成到展示世界
     */
    public void autoGenerateAll(World world, BlockVector3 origin, int spacing, int maxPerRow) {
        int index = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, schematic_path FROM build_requests WHERE status='PENDING'");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String path = rs.getString("schematic_path");

                File schematic = new File(path);
                BlockVector3 pos = DisplaySlotCalculator.calculate(origin, index, spacing, maxPerRow);
                StructurePaster.pasteAsync(world, schematic, pos);

                index++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BuildRequest findById(int id) {
        String sql = "SELECT * FROM build_requests WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new BuildRequest(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("structure_path"),
                        rs.getString("status"),
                        rs.getLong("created_at")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
