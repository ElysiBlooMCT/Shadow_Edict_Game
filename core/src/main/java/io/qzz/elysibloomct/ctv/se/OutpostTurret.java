package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.math.Vector2;

/**
 * Tháp trụ tiền đồn phòng ngự (Tactical Defense Outpost Turret)
 * Bảo vệ căn cứ và cứ điểm hai bên phe, tự động bắn tên năng lượng vào kẻ địch xâm lăng.
 * Có thể bị tấn công và phá hủy để giành tài nguyên và quyền kiểm soát bản đồ.
 */
public class OutpostTurret {
    public float x;
    public float y;
    public int team;       // 1 = Blue (Anh Sang), 2 = Red (Bong Toi)
    public int hp = 850;
    public int maxHp = 850;
    public boolean isDestroyed = false;
    public float shootCooldown = 0f;
    public float hitTimer = 0f;
    public float animTime = 0f;
    private final Vector2 position = new Vector2();

    public OutpostTurret(float x, float y, int team) {
        this.x = x;
        this.y = y;
        this.team = team;
    }

    public boolean takeDamage(int dmg) {
        if (isDestroyed) return false;
        hitTimer = 0.22f;
        hp -= dmg;
        if (hp <= 0) {
            hp = 0;
            isDestroyed = true;
            return true; // Tháp vừa bị phá hủy
        }
        return false;
    }

    public void update(float delta) {
        animTime += delta;
        if (hitTimer > 0) hitTimer -= delta;
        if (shootCooldown > 0) shootCooldown -= delta;
    }

    public boolean canShoot() {
        return !isDestroyed && shootCooldown <= 0;
    }

    public void resetShootCooldown() {
        shootCooldown = 2.4f;
    }

    public Vector2 getPosition() {
        return position.set(x, y);
    }
}
