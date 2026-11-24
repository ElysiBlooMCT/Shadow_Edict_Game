package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;

public class Fighter5v5 {
    public Body body;
    public String name;
    public int team;       // 1: Team Blue (Phe Ánh Sáng), 2: Team Red (Phe Bóng Tối)
    public int charId;     // 1: Sát Thủ Trắng, 2: Sát Thủ Đen

    public int hp = 1000;
    public int maxHp = 1000;
    public int shield = 0;

    public float damageMultiplier = 1.0f;
    public float speedMultiplier = 1.0f;
    public float runeTimer = 0f;
    public PowerUpRune.RuneType activeRune = null;

    public boolean isDead = false;
    public float respawnTimer = 0f;
    public float deathX = 0f, deathY = 0f;
    public float deathBlinkTimer = 0f;

    public int facing = 1; // 1 = Right, -1 = Left
    public float animTime = 0f;

    public boolean isAttacking = false;
    public float attackTimer = 0f;
    public float attackCooldown = 0f;
    public int attackCombo = 1;
    public boolean hitThisAttack = false;
    public String animState = "IDLE";

    public boolean hasBerserk() {
        return activeRune == PowerUpRune.RuneType.BERSERK_DAMAGE;
    }

    public float hitTimer = 0f;
    public float jumpCooldown = 0f;
    public float jumpPadCooldown = 0f;
    public float aiDecisionTimer = 0f;

    public float ultCharge = 60f; // 0..100
    public String ultType = "P1"; // "P1" (Trăn Trối) or "P2" (Sát Chiêu Hoàn Hảo)
    public boolean isUltActive = false;
    public float ultTimer = 0f;
    public boolean isStealth = false;
    public float stealthTimer = 0f;

    public int kills = 0;
    public int deaths = 0;

    public enum BotRole {
        VANGUARD,   // Do don tien phong: 1250 HP, tank, can quet
        ASSASSIN,   // Sat thu luon lach: 1000 HP, an nap bui ram, san moi it mau
        ROAMER      // Dao duong tranh bua: 1000 HP, uu tien bua & trum Than Thu
    }
    public BotRole role = BotRole.VANGUARD;

    public boolean isInBush = false;
    public boolean hasAmbushBuff = false;
    public boolean hasTitanBuff = false;
    public float titanBuffTimer = 0f;
    public boolean isRetreating = false;

    /** Nhãn tên hiển thị trên đầu (tạo 1 lần, tránh nối chuỗi mỗi frame). */
    public final String displayTag;

    // Bộ đếm phần lẻ khi hồi máu theo thời gian: (int)(55 * delta) luôn = 0 ở 60 FPS
    // nên trước đây bot về hồ máu KHÔNG BAO GIỜ hồi được và kẹt mãi ở nhà.
    private float healAccumulator = 0f;
    // Phát hiện bị kẹt dựa trên quãng đường thực tế đã đi
    private float stuckTimer = 0f;
    private float lastX = 0f;
    private final Vector2 tmpTarget = new Vector2();
    private float dropTimer = 0f;
    /** Loại mục tiêu hiện tại: quyết định khoảng cách dừng (chém địch 1.35m, nhặt bùa/nấp bụi 0.4m). */
    private enum TargetKind { ENEMY, RUNE, BUSH, POINT }
    private TargetKind targetKind = TargetKind.POINT;
    private int dropDir = 1;
    /** Bùa bot vừa nhặt trong frame này (GameScreen dùng để hiện hiệu ứng), null nếu không có. */
    public PowerUpRune lastPickedRune = null;

    public Fighter5v5(String name, int team, int charId) {
        this(name, team, charId, BotRole.VANGUARD);
    }

    public Fighter5v5(String name, int team, int charId, BotRole role) {
        this.name = name;
        this.team = team;
        this.charId = charId;
        this.role = role;
        if (role == BotRole.VANGUARD) {
            this.maxHp = 1250;
            this.hp = 1250;
        } else {
            this.maxHp = 1000;
            this.hp = 1000;
        }
        this.ultType = (charId == 2) ? "P2" : "P1";
        this.facing = (team == 1) ? 1 : -1;
        this.displayTag = ((team == 1) ? "[XANH] " : "[ĐỎ] ") + name;
    }

    private float p2LungeTimer = 0f;

    public boolean takeDamage(int dmg) {
        if (isDead || dmg <= 0) return false;
        hitTimer = 0.20f;

        if (shield > 0) {
            if (shield >= dmg) {
                shield -= dmg;
                return false;
            } else {
                dmg -= shield;
                shield = 0;
            }
        }

        hp -= dmg;
        if (hp <= 0) {
            hp = 0;
            isDead = true;
            deaths++;
            respawnTimer = 5.0f;
            isAttacking = false;
            attackTimer = 0f;
            isUltActive = false;
            ultTimer = 0f;
            isStealth = false;
            stealthTimer = 0f;
            p2LungeTimer = 0f;
            shield = 0;
            damageMultiplier = 1.0f;
            speedMultiplier = 1.0f;
            activeRune = null;
            runeTimer = 0f;
            hasTitanBuff = false;
            titanBuffTimer = 0f;
            hasAmbushBuff = false;
            isInBush = false;
            if (body != null) {
                deathX = body.getPosition().x;
                deathY = body.getPosition().y;
                deathBlinkTimer = 0.85f;
                body.setLinearVelocity(0, 0);
                body.setActive(false);
                float fountainX = (team == 1) ? 4.5f : 55.5f;
                body.setTransform(fountainX, 1.2f, 0);
            }
            return true; // vừa bị tiêu diệt
        }
        return false;
    }

    public void applyRune(PowerUpRune rune) {
        if (rune == null) return;
        switch (rune.type) {
            case RANDOM_ULTIMATE:
                // Nhận ngẫu nhiên Trăn Trối hoặc Sát Chiêu Hoàn Hảo
                this.ultType = MathUtils.randomBoolean() ? "P1" : "P2";
                this.ultCharge = 100f;
                break;
            case BERSERK_DAMAGE:
                this.speedMultiplier = 1.0f;
                this.damageMultiplier = 1.50f;
                this.activeRune = rune.type;
                this.runeTimer = 15.0f;
                break;
            case DIVINE_SHIELD:
                this.shield = Math.min(600, this.shield + 350);
                if (this.activeRune == null) {
                    this.activeRune = rune.type;
                    this.runeTimer = 18.0f;
                }
                break;
            case WIND_STRIDE:
                this.damageMultiplier = 1.0f;
                this.speedMultiplier = 1.60f;
                this.activeRune = rune.type;
                this.runeTimer = 15.0f;
                break;
            case MEGA_HEAL:
                this.hp = Math.min(this.maxHp, this.hp + 400);
                break;
        }
    }

    public void update(float delta, Array<Fighter5v5> allFighters, Vector2 playerPos, int playerTeam, boolean playerDead,
                       Array<PowerUpRune> runes, Array<JumpPad> jumpPads) {
        update(delta, allFighters, playerPos, playerTeam, playerDead, runes, jumpPads, null, null, null);
    }

    public void update(float delta, Array<Fighter5v5> allFighters, Vector2 playerPos, int playerTeam, boolean playerDead,
                       Array<PowerUpRune> runes, Array<JumpPad> jumpPads, Array<TacticalBush> bushes,
                       AncientTitan titan, Array<OutpostTurret> turrets) {
        if (body == null) return;
        animTime += delta;
        lastPickedRune = null;

        // Xử lý hồi sinh
        if (isDead) {
            animState = "DEAD";
            if (deathBlinkTimer > 0) deathBlinkTimer -= delta;
            respawnTimer -= delta;
            if (respawnTimer <= 0) {
                respawn();
            }
            return;
        }

        // Đếm lùi các buff bùa
        if (runeTimer > 0) {
            runeTimer -= delta;
            if (runeTimer <= 0) {
                damageMultiplier = 1.0f;
                speedMultiplier = 1.0f;
                activeRune = null;
            }
        }

        // Đếm lùi bùa Thần Thú Bạo Chúa (Dragon Slayer Buff)
        if (titanBuffTimer > 0) {
            titanBuffTimer -= delta;
            if (titanBuffTimer <= 0) {
                hasTitanBuff = false;
            }
        }

        if (hitTimer > 0) hitTimer -= delta;
        if (attackCooldown > 0) attackCooldown -= delta;
        if (jumpCooldown > 0) jumpCooldown -= delta;
        if (jumpPadCooldown > 0) jumpPadCooldown -= delta;

        Vector2 myPos = body.getPosition();

        // Kiểm tra rơi xuống vực (Pit death check for bot)
        if (myPos.y < -2.0f && !isDead) {
            takeDamage(9999);
            return;
        }

        // Kiểm tra ẩn nấp trong bụi rậm (Tactical Bush Camouflage & Ambush)
        isInBush = false;
        if (bushes != null) {
            for (int bi = 0; bi < bushes.size; bi++) {
                TacticalBush b = bushes.get(bi);
                if (b != null && b.contains(myPos)) {
                    isInBush = true;
                    hasAmbushBuff = true; // Sẵn sàng đòn đánh bạo kích phục kích (+40% DMG)
                    break;
                }
            }
        }

        // Hồi máu tại hồ máu nhà mình (Fountain Healing)
        boolean inHomeFountain = (team == 1 && myPos.x <= 5.8f) || (team == 2 && myPos.x >= 54.2f);
        if (inHomeFountain && hp < maxHp) {
            healAccumulator += 55f * delta;
            int heal = (int) healAccumulator;
            if (heal > 0) {
                healAccumulator -= heal;
                hp = Math.min(maxHp, hp + heal);
            }
        }
        if (isRetreating && inHomeFountain && hp >= (int)(maxHp * 0.70f)) {
            isRetreating = false; // Đã hồi đủ máu, quay lại chiến trường
        }

        // Nhặt bùa khi chạm vào (trước đây bot chạy tới bùa rồi đứng chém không khí mãi vì không có code nhặt)
        if (runes != null) {
            for (int ri = 0; ri < runes.size; ri++) {
                PowerUpRune rune = runes.get(ri);
                if (rune != null && rune.active && myPos.dst(rune.x, rune.y) <= 1.0f) {
                    rune.active = false;
                    rune.respawnTimer = rune.respawnInterval;
                    applyRune(rune);
                    lastPickedRune = rune;
                    break;
                }
            }
        }

        // Kích hoạt cơ chế rút lui khi còn dưới 22% máu
        if (hp < maxHp * 0.22f && !inHomeFountain) {
            isRetreating = true;
        }

        // Nạp nộ khí theo thời gian
        if (ultCharge < 100f) {
            ultCharge = Math.min(100f, ultCharge + delta * 2.5f);
        }

        // Cập nhật trạng thái tấn công
        if (isAttacking) {
            attackTimer -= delta;
            if (attackTimer <= 0) {
                isAttacking = false;
                attackCooldown = MathUtils.random(0.4f, 0.8f);
            }
        }

        // Cập nhật Ultimate P1: Trăn Trối (Lốc xoáy bay zic-zac cắm đất)
        if (isUltActive && ultType.equals("P1")) {
            ultTimer += delta;
            if (ultTimer < 0.20f) {
                body.setLinearVelocity(facing * 9.5f, 6.5f);
            } else if (ultTimer < 0.38f) {
                body.setLinearVelocity(-facing * 8.5f, 5.0f);
            } else if (ultTimer < 0.55f) {
                body.setLinearVelocity(facing * 9.0f, 4.5f);
            } else if (ultTimer < 0.70f) {
                body.setLinearVelocity(0, 0.2f);
            } else {
                float prevVy = body.getLinearVelocity().y;
                body.setLinearVelocity(0, -18.0f);
                if (body.getPosition().y <= 1.35f || (ultTimer > 0.85f && Math.abs(prevVy) < 0.5f) || ultTimer > 2.0f) {
                    isUltActive = false;
                    isAttacking = true;
                    hitThisAttack = false;
                    attackTimer = 0.35f;
                    attackCombo = 3;
                }
            }
            return;
        }

        // Cập nhật Ultimate P2: Sát Chiêu Hoàn Hảo (Tàng hình tăng tốc rồi chém lướt)
        if (isStealth) {
            stealthTimer -= delta;
            if (stealthTimer <= 0) {
                isStealth = false;
            }
        }
        if (p2LungeTimer > 0f) {
            p2LungeTimer -= delta;
            body.setLinearVelocity(facing * 14.0f, body.getLinearVelocity().y);
            lastX = myPos.x;
            animState = "HIT3";
            return;
        }

        // 1. Kiểm tra dẫm bệ phóng nảy cao (Jump Pads)
        if (jumpPadCooldown <= 0 && jumpPads != null) {
            for (int ji = 0; ji < jumpPads.size; ji++) {
                JumpPad pad = jumpPads.get(ji);
                if (pad != null && Math.abs(myPos.x - pad.x) < (pad.width / 2f + 0.3f) && Math.abs(myPos.y - (pad.y + pad.height / 2f)) < 0.6f) {
                    body.setLinearVelocity(facing * 3.5f, pad.bounceForce);
                    jumpPadCooldown = 1.2f;
                    break;
                }
            }
        }

        // 2. AI Tìm mục tiêu kẻ địch, bùa lợi, trùm hoặc căn cứ
        aiDecisionTimer -= delta;
        Vector2 targetPos = null;
        float closestDist = 999f;
        targetKind = TargetKind.POINT;

        // Nếu đang rút lui (Low HP retreat) -> Chạy về hồ máu nhà
        if (isRetreating) {
            targetPos = tmpTarget.set((team == 1) ? 4.5f : 55.5f, 1.2f);
            // Nếu có ngọc Mega Heal gần trên đường rút lui, nhặt luôn
            if (runes != null) {
                for (int ri = 0; ri < runes.size; ri++) {
                    PowerUpRune rune = runes.get(ri);
                    if (rune != null && rune.active && rune.type == PowerUpRune.RuneType.MEGA_HEAL && rune.y < myPos.y + 1.0f) {
                        float dst = myPos.dst(rune.x, rune.y);
                        if (dst < 7.5f) {
                            targetPos = tmpTarget.set(rune.x, rune.y);
                            targetKind = TargetKind.RUNE;
                            break;
                        }
                    }
                }
            }
        } else {
            // Chiến thuật theo vai trò Bot (BotRole Tactical Decision)
            // ROAMER: Chỉ nhắm Thần Thú Titan nếu ở cùng độ cao hoặc gần tầm với (tránh bot dưới đất đứng chém không khí)
            if (role == BotRole.ROAMER && titan != null && titan.isAlive) {
                float dstTitan = myPos.dst(titan.getPosition());
                if (dstTitan < 22.0f && Math.abs(myPos.y - titan.getPosition().y) < 2.5f && hp > maxHp * 0.45f) {
                    targetPos = titan.getPosition();
                    closestDist = dstTitan;
                    targetKind = TargetKind.ENEMY;
                }
            }

            // ASSASSIN: rình trong bụi rậm gần đó, chỉ lao ra khi kẻ địch tới đủ gần (<4.5m).
            // Bản cũ: vào bụi -> nhắm địch -> ra khỏi bụi -> nhắm bụi... => giật qua lại ở mép bụi.
            if (targetPos == null && role == BotRole.ASSASSIN && bushes != null
                && nearestEnemyDistance(allFighters, playerPos, playerTeam, playerDead) > 4.5f) {
                for (int bi = 0; bi < bushes.size; bi++) {
                    TacticalBush bush = bushes.get(bi);
                    if (bush != null) {
                        float bx = bush.x + bush.width / 2f;
                        float by = bush.y + 0.45f; // độ cao đứng trong bụi (mặt đất), không phải độ cao hiện tại của bot
                        float dstB = myPos.dst(bx, by);
                        if (dstB < 8.0f) {
                            targetPos = tmpTarget.set(bx, by);
                            closestDist = 999f;
                            targetKind = TargetKind.BUSH;
                            break;
                        }
                    }
                }
            }

            // Ưu tiên nhặt bùa nếu ở gần (< 6.0m)
            if (targetPos == null && runes != null) {
                for (int ri = 0; ri < runes.size; ri++) {
                    PowerUpRune rune = runes.get(ri);
                    // Chỉ nhắm bùa cùng tầng hoặc thấp hơn: bot không leo thang được, bùa ở tầng trên
                    // khiến bot đứng dưới sàn nhảy đập đầu mãi.
                    if (rune != null && rune.active && rune.y < myPos.y + 1.0f) {
                        float dst = myPos.dst(rune.x, rune.y);
                        if (dst < 6.0f && dst < closestDist) {
                            closestDist = dst;
                            targetPos = tmpTarget.set(rune.x, rune.y);
                            targetKind = TargetKind.RUNE;
                        }
                    }
                }
            }

            // Nếu không có bùa gần, tìm kẻ địch gần nhất
            if (targetPos == null) {
                // Kiểm tra người chơi thật (nếu khác phe)
                if (playerPos != null && playerTeam != this.team && !playerDead) {
                    float pDst = myPos.dst(playerPos);
                    if (pDst < 18.0f) {
                        closestDist = pDst;
                        targetPos = playerPos;
                        targetKind = TargetKind.ENEMY;
                    }
                }

                // Kiểm tra các bot khác (nếu khác phe, giới hạn tầm nhìn 18m & tôn trọng tàng hình/bụi rậm)
                if (allFighters != null) {
                    for (int fi = 0; fi < allFighters.size; fi++) {
                        Fighter5v5 f = allFighters.get(fi);
                        if (f != null && f != this && f.team != this.team && !f.isDead && f.body != null) {
                            if (f.isStealth) continue;
                            float fDst = myPos.dst(f.body.getPosition());
                            if (f.isInBush && (!this.isInBush || fDst > 2.5f)) continue;
                            if (fDst < 18.0f && fDst < closestDist) {
                                closestDist = fDst;
                                targetPos = f.body.getPosition();
                                targetKind = TargetKind.ENEMY;
                            }
                        }
                    }
                }

                // VANGUARD: Nếu không có kẻ địch ở gần, tập trung tấn công Tháp Tiền Đồn của đối phương
                if (targetPos == null && turrets != null) {
                    for (int ti = 0; ti < turrets.size; ti++) {
                        OutpostTurret t = turrets.get(ti);
                        if (t != null && t.team != this.team && !t.isDestroyed) {
                            float tDst = myPos.dst(t.getPosition());
                            if (tDst < 15.0f) {
                                closestDist = tDst;
                                targetPos = t.getPosition();
                                targetKind = TargetKind.ENEMY;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Nếu không có mục tiêu nào trong tầm nhìn, di chuyển về phía trung tâm giao tranh (x = 30m)
        if (targetPos == null) {
            // Tiến về trung tâm (x=30) - giữ điểm đến ổn định thay vì random mỗi frame gây rung lắc
            float centerX = 30.0f + ((name.hashCode() & 7) - 3.5f);
            targetPos = tmpTarget.set(centerX, 1.2f);
        }

        // 3. Di chuyển & Hành động
        float dx = targetPos.x - myPos.x;
        facing = (dx >= 0) ? 1 : -1;
        float finalSpeedMult = speedMultiplier * (hasTitanBuff ? 1.20f : 1.0f);
        float moveSpeed = (isStealth ? 7.0f : 4.2f) * finalSpeedMult;

        boolean isEnemyTarget = (targetKind == TargetKind.ENEMY);
        float stopDist = isEnemyTarget ? 1.35f : 0.4f;

        // Mục tiêu nằm thấp hơn hẳn ngay bên dưới (VD: bot trên tiền đồn, bùa dưới đất):
        // đi tiếp ra mép bục để rơi xuống thay vì đứng chém không khí mãi.
        boolean targetBelow = targetPos.y < myPos.y - 1.0f && Math.abs(dx) <= Math.max(stopDist, 1.35f);
        if (targetBelow) {
            if (dropTimer <= 0f) {
                dropTimer = 1.6f;
                dropDir = (myPos.x < 30f) ? 1 : -1; // tiền đồn 2 bên: đi về phía mép gần trung tâm
            }
        }
        if (dropTimer > 0f) {
            dropTimer -= delta;
            facing = dropDir;
            body.setLinearVelocity(dropDir * moveSpeed, body.getLinearVelocity().y);
            lastX = myPos.x;
            animState = "RUN";
            return;
        }

        if (Math.abs(dx) > stopDist) {
            // Lại gần mục tiêu không phải kẻ địch thì đi chậm lại để không trượt qua
            float spd = (!isEnemyTarget && Math.abs(dx) < 1.2f) ? Math.max(1.5f, moveSpeed * 0.5f) : moveSpeed;
            body.setLinearVelocity(facing * spd, body.getLinearVelocity().y);
            // Gỡ kẹt: đo quãng đường thực tế (bản cũ đọc lại vận tốc vừa gán nên không bao giờ phát hiện kẹt)
            if (Math.abs(myPos.x - lastX) < 0.01f) stuckTimer += delta; else stuckTimer = 0f;
            if (stuckTimer > 0.35f && jumpCooldown <= 0) {
                body.setLinearVelocity(facing * moveSpeed, 7.5f);
                jumpCooldown = 1.2f;
                stuckTimer = 0f;
            }
        } else if (!isEnemyTarget) {
            body.setLinearVelocity(0, body.getLinearVelocity().y); // tới bùa/bụi/điểm tập kết: đứng chờ, không chém không khí
        } else {
            body.setLinearVelocity(0, body.getLinearVelocity().y);
            // Vào tầm cận chiến: chỉ tấn công nếu mục tiêu ở gần cả về chiều cao y
            if (!isAttacking && attackCooldown <= 0 && Math.abs(targetPos.y - myPos.y) < 2.0f) {
                isAttacking = true;
                hitThisAttack = false;
                attackTimer = 0.32f;
                attackCombo = (attackCombo % 3) + 1;
            }
        }

        // Nhảy vượt chướng ngại vật hoặc leo tầng cao
        if (targetPos.y > myPos.y + 1.2f && jumpCooldown <= 0 && Math.abs(dx) < 4.0f) {
            body.setLinearVelocity(facing * moveSpeed, 7.8f);
            jumpCooldown = 1.4f;
        }

        // Kích hoạt Ultimate khi đủ 100% nộ và mục tiêu ở gần
        if (ultCharge >= 100f && isEnemyTarget && closestDist < 4.5f && !isUltActive && !isStealth) {
            ultCharge = 0f;
            if (ultType.equals("P1")) {
                isUltActive = true;
                ultTimer = 0f;
                body.setLinearVelocity(facing * 8f, 7f);
            } else {
                isStealth = true;
                stealthTimer = 3.5f;
                p2LungeTimer = 0.26f;
                isAttacking = true;
                hitThisAttack = false;
                attackTimer = 0.30f;
                attackCombo = 3;
                // Lao chém tức thì
                body.setLinearVelocity(facing * 14.0f, 1.0f);
            }
        }

        lastX = myPos.x;

        if (isAttacking) {
            animState = (attackCombo == 1) ? "HIT1" : (attackCombo == 2 ? "HIT2" : "HIT3");
        } else if (body != null && Math.abs(body.getLinearVelocity().x) > 0.3f) {
            animState = "RUN";
        } else {
            animState = "IDLE";
        }
    }

    private float nearestEnemyDistance(Array<Fighter5v5> all, Vector2 playerPos, int playerTeam, boolean playerDead) {
        Vector2 myPos = body.getPosition();
        float best = 999f;
        if (playerTeam != team && !playerDead && playerPos != null) best = myPos.dst(playerPos);
        if (all != null) {
            for (int i = 0; i < all.size; i++) {
                Fighter5v5 f = all.get(i);
                if (f != null && f != this && f.team != team && !f.isDead && f.body != null && !f.isStealth) {
                    best = Math.min(best, myPos.dst(f.body.getPosition()));
                }
            }
        }
        return best;
    }

    public void respawn() {
        isDead = false;
        hp = maxHp;
        shield = 200; // Khiên bảo vệ khi mới hồi sinh
        isAttacking = false;
        attackTimer = 0f;
        attackCooldown = 0f;
        hitTimer = 0f;
        jumpCooldown = 0f;
        jumpPadCooldown = 0f;
        isUltActive = false;
        ultTimer = 0f;
        isStealth = false;
        stealthTimer = 0f;
        p2LungeTimer = 0f;
        damageMultiplier = 1.0f;
        speedMultiplier = 1.0f;
        activeRune = null;
        runeTimer = 0f;
        hasTitanBuff = false;
        titanBuffTimer = 0f;
        hasAmbushBuff = false;
        isInBush = false;
        isRetreating = false;
        healAccumulator = 0f;
        dropTimer = 0f;
        stuckTimer = 0f;

        // Vị trí hồ máu (Fountain)
        if (body != null) {
            body.setActive(true);
            if (team == 1) {
                body.setTransform(4.5f + MathUtils.random(-1.0f, 1.0f), 1.2f, 0);
                facing = 1;
            } else {
                body.setTransform(55.5f + MathUtils.random(-1.0f, 1.0f), 1.2f, 0);
                facing = -1;
            }
            body.setLinearVelocity(0, 0);
        }
    }
}
