package net.chen.buildShowcase.storage;

import org.bukkit.command.CommandSender;

import java.sql.*;
import java.util.UUID;

public class BuildRepositorySQL {

    private final Connection conn;

    public BuildRepositorySQL(Connection conn) {
        this.conn = conn;
        createTable();
    }

    private void createTable() {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS build_requests (" +
                            "id VARCHAR(36) PRIMARY KEY," +
                            "player_uuid CHAR(36) NOT NULL," +
                            "schematic_path VARCHAR(255) NOT NULL," +
                            "submit_time BIGINT NOT NULL," +
                            "status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING'" +
                            ");"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 提交建筑
    public void submit(UUID player, String schematicPath) {
        String sql = "INSERT INTO build_requests(id, player_uuid, schematic_path, submit_time) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO build_requests(id, player_uuid, schematic_path, submit_time, status) VALUES (?,?,?,?,?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, player.toString());
            ps.setString(3, schematicPath);
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, "PENDING");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 获取待审核数量
    public int countPending() {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM build_requests WHERE status='PENDING'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 列出待审核
    public void listPending(CommandSender sender) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, player_uuid, submit_time FROM build_requests WHERE status='PENDING'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String player = rs.getString("player_uuid");
                long time = rs.getLong("submit_time");
                sender.sendMessage("§7- §f" + id + " §7by " + player + " §7at " + time);
            }
        } catch (SQLException e) {
            sender.sendMessage("§c读取失败");
            e.printStackTrace();
        }
    }

    // 审核通过
    public String approve(String id) {
        String schematic = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT schematic_path FROM build_requests WHERE id=? AND status='PENDING'")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    schematic = rs.getString("schematic_path");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (schematic != null) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE build_requests SET status='APPROVED' WHERE id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return schematic;
    }

    // 拒绝
    public void reject(String id) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE build_requests SET status='REJECTED' WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
