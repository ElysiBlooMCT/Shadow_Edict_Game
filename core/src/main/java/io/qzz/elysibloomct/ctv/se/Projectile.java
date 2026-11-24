package io.qzz.elysibloomct.ctv.se;

public class Projectile {
    public float x, y, vx, vy;
    public int damage;
    public boolean isArrow;
    public boolean isCannonBall;
    public boolean isSlingshotPellet;
    public boolean isShotgun;
    public float lifeTime = 3.5f;

    // Khóa mục tiêu cung tên & đạn kỹ năng
    public boolean homingArrow = false;
    public GroundMob targetMob = null;
    public Fighter5v5 targetFighter = null;
    public OutpostTurret targetTurret = null;
    public String targetPeerId = null; // ID của người chơi trong extraPeers5v5 (5v5 đông người)
    public int targetSpecial = 0; // 1=Không tặc, 2=Triều đình, 3=Đối thủ PVP/5v5 chính, 4=Cổ Thần, 5=extraPeer5v5
    public int skillVisualType = 0; // 0=thường, 1=Phong Nhẫn, 2=Thiên Kiếm, 3=Huyết Tiêu
    public boolean piercing = false;
    public boolean applyPoison = false;
    public int lifeSteal = 0;
    public boolean fromRemotePeer = false;
    public int ownerTeam = 0; // 1 hoặc 2
    public final com.badlogic.gdx.utils.IntSet hitIds = new com.badlogic.gdx.utils.IntSet();

    public Projectile(float x, float y, float vx, float vy, int damage, boolean isArrow, boolean isCannonBall, boolean isSlingshotPellet, boolean isShotgun) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.isArrow = isArrow;
        this.isCannonBall = isCannonBall;
        this.isSlingshotPellet = isSlingshotPellet;
        this.isShotgun = isShotgun;
    }
}
