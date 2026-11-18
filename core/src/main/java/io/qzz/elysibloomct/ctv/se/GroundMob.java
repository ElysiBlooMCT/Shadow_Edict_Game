package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.physics.box2d.Body;

public class GroundMob {
    /** Định danh ổn định theo thứ tự sinh - dùng để đồng bộ sát thương CO-OP (thay cho chỉ số mảng). */
    public int id = -1;
    public Body body;
    public int hp, maxHp;
    public boolean isBoss;
    public int facing = -1;
    public float animTime = 0f;
    public float atkCooldown = 0f;
    public String state = "PATROL";
    public boolean hitThisAtk = false;
    public float scale = 1.0f;
    public float patrolLeft, patrolRight;
    public float patrolWaitTimer = 0f;
    public float bossSmashCooldown = 7.0f;
    public boolean isDead = false;
    public float deathTimer = 0f;
    public float hitTimer = 0f;

    // AI nâng cao & hiệu ứng khống chế/độc
    public float alertTimer = 0f;
    public float jumpCooldown = 0f;
    public float stuckTimer = 0f;
    public float lastX = 0f;
    public float turnDelayTimer = 0f;
    public float flankOffset = 0f;
    public float poisonTimer = 0f;
    public float poisonTickAccum = 0f;
    public float slowTimer = 0f;

    // Berserk / Cuồng Huyết Mechanics (Ngưỡng 50% HP)
    public boolean isBerserk = false;           // Đang trong trạng thái cuồng huyết
    public boolean isChargingBerserk = false;   // Đang gồng 2s kích hoạt cuồng huyết
    public float berserkChargeTimer = 0f;       // Đếm ngược 2s gồng
    public float berserkAttackTimer = 0f;       // Đếm 10s chu kỳ tấn công
    public float berserkRestTimer = 0f;         // Đếm 1.2s nghỉ ngơi kiệt sức
    public boolean isResting = false;           // Đang nghỉ 1.2s sau 10s tấn công
    public boolean berserkTriggered = false;    // Đã kích hoạt ngưỡng 50% HP
    public float berserkAuraTime = 0f;          // Hiệu ứng hào quang đỏ bừng bừng
}
