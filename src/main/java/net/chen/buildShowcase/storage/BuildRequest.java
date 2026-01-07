package net.chen.buildShowcase.storage;

import java.util.UUID;

public class BuildRequest {

    private final int id;
    private final UUID playerUuid;
    private final String structurePath;
    private final String status;
    private final long createdAt;

    public BuildRequest(int id,
                        UUID playerUuid,
                        String structurePath,
                        String status,
                        long createdAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.structurePath = structurePath;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getStructurePath() {
        return structurePath;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
