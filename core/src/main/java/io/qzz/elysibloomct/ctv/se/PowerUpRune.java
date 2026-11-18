package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.graphics.Color;

public class PowerUpRune {
    public enum RuneType {
        RANDOM_ULTIMATE, // Nhận ngẫu nhiên Trăn Trối hoặc Sát Chiêu Hoàn Hảo + hồi đầy 100% Ult
        BERSERK_DAMAGE,  // +50% Sát thương đòn đánh và chiêu thức trong 15s
        DIVINE_SHIELD,   // +350 Giáp ảo (Shield) vàng kim hấp thụ sát thương
        WIND_STRIDE,     // +60% Tốc độ di chuyển, tăng độ cao nhảy và tàn ảnh gió
        MEGA_HEAL;       // Hồi phục 400 HP ngay tức khắc

        private static final Color C_ULT = new Color(0.9f, 0.3f, 1.0f, 1f);
        private static final Color C_BERSERK = new Color(1.0f, 0.25f, 0.1f, 1f);
        private static final Color C_SHIELD = new Color(0.15f, 0.65f, 1.0f, 1f);
        private static final Color C_WIND = new Color(0.15f, 0.95f, 0.55f, 1f);
        private static final Color C_HEAL = new Color(0.2f, 0.9f, 0.2f, 1f);

        /** Màu đặc trưng (hằng số dùng chung - không sửa đổi đối tượng trả về). */
        public Color getColor() {
            switch (this) {
                case RANDOM_ULTIMATE: return C_ULT;
                case BERSERK_DAMAGE: return C_BERSERK;
                case DIVINE_SHIELD: return C_SHIELD;
                case WIND_STRIDE: return C_WIND;
                case MEGA_HEAL: return C_HEAL;
                default: return Color.WHITE;
            }
        }
    }

    public float x, y;
    public RuneType type;
    public boolean active = true;
    public float respawnTimer = 0f;
    public float respawnInterval = 25f;
    public float pulseTime = 0f;

    public PowerUpRune(float x, float y, RuneType type, float respawnInterval) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.respawnInterval = respawnInterval;
    }

    public String getName() {
        switch (type) {
            case RANDOM_ULTIMATE: return "NGỌC TUYỆT KỸ";
            case BERSERK_DAMAGE:  return "BÙA CUỒNG NỘ (+50% sát thương)";
            case DIVINE_SHIELD:   return "KHIÊN HỘ MỆNH (+350 khiên)";
            case WIND_STRIDE:     return "BÙA PHONG TỐC (+60% tốc độ)";
            case MEGA_HEAL:       return "THẦN DƯỢC (+400 HP)";
            default:              return "VẬT PHẨM";
        }
    }
}
