package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.graphics.Color;

public class FriendInfo {
    public String userId = "";
    public String name = "";
    public int level = 1;
    public String status = "OFFLINE"; // "ONLINE", "IN_ROOM", "IN_GAME", "OFFLINE"
    public String roomCode = "";
    public String mode = ""; // "1v1", "5v5", "coop"
    public boolean isFriend = false;
    public boolean isPending = false;

    public FriendInfo() {}

    public FriendInfo(String userId, String name, int level, String status, String roomCode, String mode) {
        this.userId = userId != null ? userId : "";
        this.name = name != null ? name : "";
        this.level = Math.max(1, level);
        this.status = (status != null && !status.isEmpty()) ? status : "OFFLINE";
        this.roomCode = roomCode != null ? roomCode : "";
        this.mode = mode != null ? mode : "";
    }

    public boolean isOnline() {
        return "ONLINE".equalsIgnoreCase(status)
            || "IN_ROOM".equalsIgnoreCase(status)
            || "IN_GAME".equalsIgnoreCase(status);
    }

    public boolean isAvailableForInvite() {
        return "ONLINE".equalsIgnoreCase(status);
    }

    public Color getStatusColor() {
        if ("ONLINE".equalsIgnoreCase(status)) {
            return new Color(0.2f, 0.9f, 0.3f, 1f); // Xanh lá: Rảnh / Sảnh
        } else if ("IN_ROOM".equalsIgnoreCase(status)) {
            return new Color(1.0f, 0.85f, 0.2f, 1f); // Vàng: Trong phòng chờ
        } else if ("IN_GAME".equalsIgnoreCase(status)) {
            return new Color(0.95f, 0.3f, 0.3f, 1f); // Đỏ: Đang trong trận
        } else {
            return new Color(0.6f, 0.6f, 0.6f, 1f); // Xám: Ngoại tuyến
        }
    }

    public String getStatusDisplayText() {
        String safeMode = mode != null ? mode : "";
        String safeRoom = roomCode != null ? roomCode : "";
        if ("ONLINE".equalsIgnoreCase(status)) {
            return "[ONLINE] Sanh Cho";
        } else if ("IN_ROOM".equalsIgnoreCase(status)) {
            String m = safeMode.isEmpty() ? "Phong" : safeMode.toUpperCase();
            return "[PHONG #" + safeRoom + "] (" + m + ")";
        } else if ("IN_GAME".equalsIgnoreCase(status)) {
            String m = safeMode.isEmpty() ? "Tran Dau" : safeMode.toUpperCase();
            return "[DANG TRAN] (" + m + ")";
        } else {
            return "[OFFLINE]";
        }
    }
}
