package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Trùm trung tâm võ đài: THẦN THÚ BẠO CHÚA (Ancient Titan Guardian)
 * Xuất hiện trên đỉnh tháp tối cao trung tâm (x=30m, y=5.8m).
 * Phe tiêu diệt được Thần Thú sẽ nhận BÙA THẦN THÚ (Dragon Slayer Buff)
 * tăng 35% sát thương và 250 giáp hộ thể cho toàn bộ thành viên còn sống!
 * Có cơ chế CUỒNG HUYẾT khi tụt xuống dưới 50% HP!
 */
public class AncientTitan {
    public float x = 30.0f;
    public float y = 5.8f;
    public int hp = 1500;
    public int maxHp = 1500;
    public boolean isAlive = true;
    public float respawnTimer = 0f;
    public float attackCooldown = 0f;
    public boolean isAttacking = false;
    public float attackTimer = 0f;
    public int facing = 1;
    public float animTime = 0f;
    public float hitTimer = 0f;
    private final Vector2 position = new Vector2();

    // Berserk / Cuồng Huyết Mechanics (Ngưỡng 50% HP)
    public boolean isBerserk = false;           // Đang trong trạng thái cuồng huyết
    public boolean isChargingBerserk = false;   // Đang gồng 2s kích hoạt cuồng huyết
    public float berserkChargeTimer = 0f;       // Đếm ngược 2s gồng
    public float berserkAttackTimer = 0f;       // Đếm 10s chu kỳ tấn công
    public float berserkRestTimer = 0f;         // Đếm 1.2s nghỉ ngơi kiệt sức
    public boolean isResting = false;           // Đang nghỉ 1.2s sau 10s tấn công
    public boolean berserkTriggered = false;    // Đã kích hoạt ngưỡng 50% HP
    public float berserkAuraTime = 0f;          // Hiệu ứng hào quang đỏ bừng bừng

    public AncientTitan() {
        this.isAlive = true;
        this.hp = this.maxHp;
    }

    public boolean takeDamage(int dmg) {
        if (!isAlive || dmg <= 0) return false;

        // Trong thời gian gồng 2s: giảm 85% sát thương
        if (isChargingBerserk) {
            dmg = Math.max(1, (int)(dmg * 0.15f));
        }

        hitTimer = 0.20f;
        hp -= dmg;

        // Kích hoạt trạng thái gồng 2s Cuồng Huyết khi xuống <= 50% HP
        if (!berserkTriggered && hp <= maxHp * 0.50f && hp > 0) {
            berserkTriggered = true;
            isChargingBerserk = true;
            berserkChargeTimer = 2.0f;
            isAttacking = false;
            attackTimer = 0f;
            attackCooldown = 2.2f;
        }

        if (hp <= 0) {
            hp = 0;
            isAlive = false;
            isAttacking = false;
            attackTimer = 0f;
            hitTimer = 0f;
            isBerserk = false;
            isChargingBerserk = false;
            isResting = false;
            berserkTriggered = false;
            berserkChargeTimer = 0f;
            berserkAttackTimer = 0f;
            berserkRestTimer = 0f;
            respawnTimer = 65.0f; // Hồi sinh sau 65 giây
            return true; // Bị hạ gục
        }
        return false;
    }

    public void update(float delta) {
        animTime += delta;
        berserkAuraTime += delta;
        if (hitTimer > 0) hitTimer -= delta;

        if (!isAlive) {
            respawnTimer -= delta;
            if (respawnTimer <= 0) {
                isAlive = true;
                hp = maxHp;
                attackCooldown = 2.0f;
                isAttacking = false;
                attackTimer = 0f;
                hitTimer = 0f;
                isBerserk = false;
                isChargingBerserk = false;
                isResting = false;
                berserkTriggered = false;
                berserkChargeTimer = 0f;
                berserkAttackTimer = 0f;
                berserkRestTimer = 0f;
            }
            return;
        }

        // Giai đoạn gồng 2s
        if (isChargingBerserk) {
            berserkChargeTimer -= delta;
            if (berserkChargeTimer <= 0) {
                isChargingBerserk = false;
                isBerserk = true;
                berserkAttackTimer = 10.0f; // 10s tấn công dồn dập
                isResting = false;
                attackCooldown = 0.5f;
            }
            return;
        }

        // Trạng thái Cuồng Huyết: chu kỳ 10s tấn công / 1.2s nghỉ ngơi
        if (isBerserk) {
            if (isResting) {
                berserkRestTimer -= delta;
                if (berserkRestTimer <= 0) {
                    isResting = false;
                    berserkAttackTimer = 10.0f;
                    attackCooldown = 0.6f;
                }
                return; // Trong lúc nghỉ ngơi không tấn công
            } else {
                berserkAttackTimer -= delta;
                if (berserkAttackTimer <= 0) {
                    isResting = true;
                    berserkRestTimer = 1.2f; // Nghỉ ngơi 1.2s
                    isAttacking = false;
                    return;
                }
            }
        }

        if (attackCooldown > 0) attackCooldown -= delta;

        if (isAttacking) {
            attackTimer -= delta;
            if (attackTimer <= 0) {
                isAttacking = false;
            }
        }
    }

    public boolean canAttack() {
        return isAlive && !isAttacking && attackCooldown <= 0 && !isChargingBerserk && !isResting;
    }

    public boolean triggerAttack(int targetFacing) {
        if (!canAttack()) return false;
        isAttacking = true;
        attackTimer = isBerserk ? 0.28f : 0.45f; // Tốc độ đánh nhanh hơn khi cuồng huyết
        float minCd = isBerserk ? 0.9f : 1.8f;
        float maxCd = isBerserk ? 1.4f : 2.8f;
        attackCooldown = attackTimer + MathUtils.random(minCd, maxCd);
        this.facing = targetFacing;
        return true;
    }

    public Vector2 getPosition() {
        return position.set(x, y);
    }
}
