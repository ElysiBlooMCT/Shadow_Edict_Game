package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import org.json.JSONObject;

public class GameScreen implements Screen {

    // -- Constants --
    public static final float PPM = 100f;
    private static final float PLAYER_SPEED = 2.2f;
    private static final float AIR_SPEED = 2.5f;
    private static final float DASH_SPEED = 6.5f;
    private static final float DASH_DURATION = 0.28f;
    private static final float JUMP_FORCE = 4.8f;
    private static final float DOUBLE_JUMP_FORCE = 4.2f;
    private static final float EAGLE_STRIKE_SPEED = 7.5f;
    private static final float MAX_CLIMB_STAMINA = 4.0f;
    private static final float ROPE_LENGTH = 2.6f;
    private static final int SWORD_DMG_1 = 35, SWORD_DMG_2 = 55, SWORD_DMG_3 = 90;
    private static final int DASH_DMG = 50;
    private static final int EAGLE_DMG = 280;
    private static final int FALL_PENALTY = 50;

    // Lọc va chạm Box2D: bot 5v5 chỉ va chạm với địa hình (không đẩy người chơi / không chồng lên nhau)
    private static final short CAT_WORLD = 0x0001;
    private static final short CAT_PLAYER = 0x0002;
    private static final short CAT_FIGHTER = 0x0004;

    // Lực bật bệ nhảy: v = sqrt(2*g*h), g = 8.5. Bản cũ dùng 15-16.5 m/s => đỉnh nhảy ~14m,
    // vượt trần bản đồ (8.8m) nên người chơi đập trần, rơi lại đúng bệ và bị bật mãi.
    private static final float PAD_LOW = 7.0f;   // ~2.9m: lên tiền đồn
    private static final float PAD_MID = 8.0f;   // ~3.8m: lên tầng 1 tháp giữa
    private static final float PAD_HIGH = 8.6f;  // ~4.3m: nhảy vượt vực / lên đỉnh

    // -- Game Mode --
    public enum GameMode { SOLO, PVP, COOP, BATTLE_5V5 }
    private final GameMode currentGameMode;
    private final int currentLevel;
    /** SOLO: dữ liệu ải cốt truyện (null ở các chế độ khác). currentLevel = chỉ số ải 1..30. */
    private final StoryChapterConfig.StageData story;

    // -- Hộp thoại cốt truyện (letterbox + chân dung + chữ có dấu) --
    private boolean storyDialogueActive = false;
    private java.util.List<StoryChapterConfig.StoryLine> storyLines = java.util.Collections.emptyList();
    private int storyLineIndex = 0;
    private float storyLineTime = 0f; // hiệu ứng chữ chạy
    private Runnable storyDialogueCallback = null;
    private float storyFocusX = 0f, storyFocusY = 0f;
    private boolean storyVictoryPending = false;
    private boolean storyIntroStarted = false, storyOutroDone = false;
    private float dmgFontK = 1f; // hệ số đổi cỡ chữ cũ -> font có dấu
    private float frozenTime = 0f; // thời gian hội thoại / tiệm đồ (không tính vào thưởng tốc độ)
    private long storyInputFrame = -1;       // khung hình đã dùng phím cho hội thoại (tránh nhảy/tạm dừng theo)
    private Label lblStageBanner;             // tên chương/ải hiện đầu màn
    private final java.util.HashMap<String, TextureRegion> portraitCache = new java.util.HashMap<>();
    private boolean boyRewardGiven = false;
    private int boyChatIndex = 0;
    private TextButton btnTalkNPC;
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout storyLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
    private final Main game;

    // -- 5v5 ARAM Arena System --
    private final Array<Fighter5v5> fighters5v5 = new Array<>();
    private final Array<PowerUpRune> powerUpRunes = new Array<>();
    private final Array<JumpPad> jumpPads = new Array<>();
    private int blueKills = 0;
    private int redKills = 0;
    private static final int WIN_KILLS_5V5 = 30;
    private float playerRespawnTimer5v5 = 0f;
    private Label lblScore5v5;
    private Label lblKillStreak;
    private float killStreakTimer = 0f;
    private int playerMultiKill = 0;
    private float multiKillWindow = 0f;
    private Texture jumpPadTexture;
    private Texture runeBerserkTex, runeShieldTex, runeWindTex, runeHealTex, runeUltTex;
    private int playerShield = 0;
    private float playerDamageBuff = 1.0f;
    private float playerSpeedBuff = 1.0f;
    private float playerRuneTimer = 0f;
    private PowerUpRune.RuneType playerActiveRune = null;
    private String playerCurrentUltType = "P1";

    // -- 5v5 Tactical War System (Bushes, Turrets, Titan Boss, Fountain) --
    private final Array<TacticalBush> tacticalBushes = new Array<>();
    private final Array<OutpostTurret> outpostTurrets = new Array<>();
    private AncientTitan ancientTitan;
    private Texture bushTexture;
    private Texture turretTexture;
    private Texture titanBossTexture;
    private Texture fountainAuraTexture;
    private Texture vignetteTexture;
    private boolean playerInBush = false;
    private boolean playerAmbushBuff = false;
    private float playerTitanBuffTimer = 0f;
    private boolean playerHasTitanBuff = false;

    // -- Boss Intro Cinematic Zoom & Alert --
    private boolean bossIntroActive = false;
    private boolean bossIntroTriggered = false;
    private float bossIntroTimer = 0f;
    private static final float BOSS_INTRO_DURATION = 3.0f;
    private float bossIntroTargetX = 0f;
    private float bossIntroTargetY = 0f;
    private float currentCameraZoom = 1.0f;
    private float targetCameraZoom = 1.0f;

    // -- Checkpoints & Dynamic Hazards --
    public static class MapCheckpoint {
        public float x, y;
        public boolean activated = false;
        public MapCheckpoint(float x, float y) { this.x = x; this.y = y; }
    }
    private final Array<MapCheckpoint> checkpoints = new Array<>();
    private float activeCheckpointX = 10.0f;
    private float activeCheckpointY = 1.2f;

    // -- Collapsing Platforms (Sàn gỗ sập chậm sau khi dẫm) --
    public static class CollapsingPlatform {
        public float x, y, width, height;
        public Body body;
        public boolean steppedOn = false;
        public float timer = 0f;
        public boolean collapsed = false;
        public float respawnTimer = 0f;
        public float shakeOffset = 0f;
        public CollapsingPlatform(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }
    private final Array<CollapsingPlatform> collapsingPlatforms = new Array<>();

    // -- Movement Physics: Coyote Time & Jump Buffer --
    private float coyoteTimer = 0f;
    private float jumpBufferTimer = 0f;
    private static final float COYOTE_TIME_MAX = 0.15f;
    private static final float JUMP_BUFFER_MAX = 0.15f;

    // -- Rendering --
    private OrthographicCamera camera;
    private boolean isFirstCameraFrame = true;
    private boolean isAttackButtonPressed = false;
    private Viewport viewport;
    private World world;
    public final float mapWidth, mapHeight, finishLineX;
    /** Chơi tự do (Chọn màn chơi): không ghi checkpoint / tiến độ vào file lưu của chiến dịch. */
    private final boolean freePlay;

    private SpriteBatch batch;
    private Texture arenaBackground;
    private Texture platformWoodTexture, groundBrickTexture, ladderTexture, wallTexture;
    private Texture platformConsoleTex;
    private int currentMapThemeIndex = 1;
    // -- Kỹ năng & Thần khí thủ tục (Procedural Skill Visuals) --
    private Texture p1CrescentAuraTex;      // Đại Kiếm Khí Bán Nguyệt (Phong Nhẫn)
    private Texture p1CelestialSwordTex;    // Cổ Thần Binh Thần Kiếm (Vạn Kiếm Quy Tông & Kiếm Thiền)
    private TextureRegion p1CelestialSwordRegion;
    private Texture p1MagicCircleTex;       // Bát Quái Kiếm Trận (Lôi Phong Hộ Thể / Thức Tỉnh)
    private Texture p2KunaiTex;             // Phi Tiêu Ám Khí Hắc Ám (Huyết Tiêu Liên Hoàn)
    private TextureRegion p2KunaiRegion;
    private Texture hpOrbTexture, weaponBoxTexture, arrowTexture, bulletTexture, ropeTexture;
    private TextureRegion weaponBoxRegion, arrowRegion;
    private Texture cannonTexture, cannonBallTexture, slingshotPelletTexture, finishFlagTexture;

    // -- Visible Obstacle Wall & Climbing System --
    public static class MapWall {
        public float x, y, width, height;
        public Body body;
        public MapWall(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }
    private final Array<MapWall> wallsList = new Array<>();
    private boolean isWallClinging = false;
    private MapWall clingingWall = null;
    private int clingingSide = 0; // -1: bám mép trái của tường, +1: bám mép phải của tường

    // -- Player sprites --
    private Texture playerRunSheet, playerIdleSheet, playerDashTexture, playerCrouchTexture;
    private TextureRegion dashFrame, crouchFrame;
    private Texture playerJumpTexture, playerDownTexture;
    private TextureRegion jumpUpFrame, jumpDownFrame;

    private Texture hit1Texture, hit2Texture, hit3Texture;
    private TextureRegion hit1Frame, hit2Frame, hit3Frame;
    private Animation<TextureRegion> attack1Anim, attack2Anim, attack3Anim;

    private int attackCombo = 0;
    private float attackTime = 0f;
    private int queuedAttacks = 0;
    private boolean hitThisAttack = false;

    private Animation<TextureRegion> playerRunAnim;
    private TextureRegion staticIdleFrame;
    private float stateTime;

    // 3-Frame Idle Animations for White (P1) & Dark (P2) Assassins
    private Texture texTrangDung1, texTrangDung2, texTrangDung3;
    private Texture texDenDung1, texDenDung2, texDenDung3;
    private TextureRegion trangDung1Region, denDung1Region;
    private Animation<TextureRegion> whiteIdleAnim, darkIdleAnim;
    private boolean hasMultiFrameWhiteIdle = false;
    private String selectedChar = "P1";
    private Animation<TextureRegion> activeIdleAnim;

    // White Assassin (P1) Run, Jump & Weapon Animations
    private Texture texTrangChay1, texTrangChay2, texTrangChay3;
    private Animation<TextureRegion> whiteRunAnim;
    private Texture texTrangNhay, texTrangRoi;
    private TextureRegion whiteJumpUpFrame, whiteJumpDownFrame;
    private Texture texTrangChem1, texTrangChem2, texTrangChem3;
    private TextureRegion trangChem1Frame, trangChem2Frame, trangChem3Frame;
    private Texture texTrangSungDung, texTrangSungBan1, texTrangSungBan2;
    private TextureRegion trangSungDungFrame, trangSungBan1Frame, trangSungBan2Frame;
    private Animation<TextureRegion> whiteShotgunAtkAnim;
    private Texture texTrangCungDung, texTrangCungBan1, texTrangCungBan2;
    private TextureRegion trangCungDungFrame, trangCungBan1Frame, trangCungBan2Frame;
    private Animation<TextureRegion> whiteBowAtkAnim;
    private Texture texTrangRiuDung, texTrangRiuBo1, texTrangRiuBo2;
    private TextureRegion trangRiuDungFrame, trangRiuBo1Frame, trangRiuBo2Frame;
    private Animation<TextureRegion> whiteAxeAtkAnim;
    private Texture texSongDaoDung, texSongDaoChem1, texSongDaoChem2;
    private TextureRegion songDaoDungFrame, songDaoChem1Frame, songDaoChem2Frame;
    private Animation<TextureRegion> whiteDaggerAtk1Anim, whiteDaggerAtk2Anim;
    private int daggerComboStep = 0;
    private int nextSwordCombo = 1;
    private float swordComboWindowTimer = 0f;

    // Dark Assassin (P2) Run, Weapon Stances & Animations
    private Texture texDenChay1, texDenChay2, texDenNhay, texDenRoi;
    private TextureRegion denDashFrame, denJumpUpFrame, denJumpDownFrame, denRollFrame;
    private Animation<TextureRegion> darkRunAnim;
    private Texture texDenCungDung, texDenCungDuong, texDenBanCung;
    private TextureRegion denCungDungFrame, denCungDuongFrame, denBanCungFrame;
    private Animation<TextureRegion> darkBowAtkAnim;
    private Texture texDenSongDaoDung, texDenSongDaoChem1, texDenSongDaoChem2;
    private TextureRegion denSongDaoDungFrame, denSongDaoChem1Frame, denSongDaoChem2Frame;
    private Texture texDenSungDung, texDenSungBan1, texDenSungBan2;
    private TextureRegion denSungDungFrame, denSungBan1Frame, denSungBan2Frame;
    private Animation<TextureRegion> darkShotgunAtkAnim;
    private Texture texDenRiuDung, texDenRiuBo1, texDenRiuBo2;
    private TextureRegion denRiuDungFrame, denRiuBo1Frame, denRiuBo2Frame;
    private Animation<TextureRegion> darkAxeAtkAnim;

    // 13-Frame Ultra-Smooth Attack Combo for Dark Assassin (P2)
    private Animation<TextureRegion> darkAttackAnim;
    private Animation<TextureRegion> darkAttackAnim1, darkAttackAnim2, darkAttackAnim3;
    private Animation<TextureRegion> darkDaggerAtk1Anim, darkDaggerAtk2Anim;
    private Array<Texture> texDenDanhList = new Array<>(Texture.class);
    private Array<TextureRegion> darkAtkRegionsList = new Array<>(TextureRegion.class);

    // Virtual Joystick Touchpad
    private Touchpad touchpad;

    // -- Monster sprites --
    private TextureRegion monsterIdleFrame;
    private Animation<TextureRegion> monsterRunAnim;
    private TextureRegion monsterHitFrame;

    // Hit Sparks Particle System
    private static class HitSpark {
        float x, y, vx, vy, size, lifeTime, maxLife;
        Color color = new Color();
    }
    private final Array<HitSpark> hitSparks = new Array<>();
    private final Array<HitSpark> sparkPool = new Array<>();
    private Texture sparkTexture;
    private final Vector3 screenPos = new Vector3();

    // Reusable Color constants for zero-GC render performance
    private static final Color COLOR_HIT_FLASH = new Color(1.0f, 0.35f, 0.35f, 1.0f);
    private static final Color COLOR_STUN = new Color(1.0f, 0.9f, 0.3f, 1.0f);
    private static final Color COLOR_STEALTH = new Color(0.40f, 0.55f, 0.75f, 0.65f);
    private static final Color COLOR_DASH_GLOW = new Color(0.65f, 0.90f, 1.0f, 0.85f);
    private static final Color COLOR_DASH_TRAIL = new Color(0.3f, 0.6f, 1.0f, 0.40f);
    private static final Color COLOR_PVP_REMOTE = new Color(1.0f, 0.65f, 0.65f, 1.0f);
    private static final Color COLOR_COOP_REMOTE = new Color(0.55f, 0.85f, 1.0f, 1.0f);
    private static final Color COLOR_SAGE = new Color(1.0f, 0.5f, 1.0f, 1f);
    private static final Color COLOR_BOSS_SMASH_TINT = new Color(1f, 0.4f, 0f, 1f);
    private static final Color COLOR_BOSS_TINT = new Color(1f, 0.85f, 0.85f, 1f);

    private static final Color COLOR_P2_STEALTH = new Color(0.35f, 0.25f, 0.45f, 0.35f);
    private static final Color COLOR_P2_DASH_GLOW = new Color(0.85f, 0.45f, 1.0f, 0.88f);
    private static final Color COLOR_P2_DASH_TRAIL = new Color(0.60f, 0.20f, 0.85f, 0.45f);
    private static final Color COLOR_P1_ULT = new Color(0.85f, 0.95f, 1.0f, 0.95f);
    private static final Color COLOR_P1_ULT_TRAIL = new Color(0.3f, 0.8f, 1.0f, 0.45f);
    private static final Color COLOR_P2_RING_NORMAL = new Color(0.8f, 0.3f, 1.0f, 0.7f);
    private static final Color COLOR_TURRET_BLUE = new Color(0.35f, 0.85f, 1.0f, 1.0f);
    private static final Color COLOR_TURRET_RED = new Color(1.0f, 0.40f, 0.40f, 1.0f);
    private static final Color COLOR_TURRET_BAR_BLUE = new Color(0.20f, 0.70f, 1.0f, 1.0f);
    private static final Color COLOR_TURRET_BAR_RED = new Color(0.95f, 0.25f, 0.25f, 1.0f);
    private static final Color COLOR_BOSS_CHARGING = new Color(1.0f, 0.20f, 0.20f, 1.0f);
    private static final Color COLOR_BOSS_RESTING = new Color(0.85f, 0.85f, 0.95f, 0.90f);
    private static final Color COLOR_BOSS_BERSERK = new Color(1.0f, 0.35f, 0.30f, 1.0f);
    private static final Color COLOR_MINIMAP_BG = new Color(0.05f, 0.08f, 0.14f, 0.88f);
    private static final Color COLOR_MINIMAP_BORDER = new Color(0.85f, 0.75f, 0.45f, 0.95f);
    private static final Color COLOR_MINIMAP_GROUND = new Color(0.55f, 0.45f, 0.35f, 0.9f);
    private static final Color COLOR_MINIMAP_PLAT = new Color(0.80f, 0.65f, 0.40f, 0.9f);
    private static final Color COLOR_MINIMAP_LADDER = new Color(0.6f, 0.45f, 0.3f, 0.7f);
    private static final Color COLOR_MINIMAP_ROPE = new Color(0.7f, 0.5f, 0.3f, 0.7f);
    private static final Color COLOR_MINIMAP_BUSH = new Color(0.12f, 0.50f, 0.16f, 0.85f);
    private static final Color COLOR_MINIMAP_VIEW_RECT = new Color(1f, 1f, 0.4f, 0.65f);
    private static final Color COLOR_FIGHTER_TEAM1 = new Color(0.85f, 0.95f, 1.0f, 1.0f);
    private static final Color COLOR_FIGHTER_TEAM2 = new Color(1.0f, 0.85f, 0.85f, 1.0f);
    private static final Color COLOR_FIGHTER_BLUE = new Color(0.2f, 0.7f, 1f, 1f);
    private static final Color COLOR_FIGHTER_RED = new Color(1f, 0.3f, 0.3f, 1f);
    private static final Color COLOR_FIGHTER_BERSERK = new Color(1.0f, 0.5f, 0.5f, 1.0f);
    private static final Color COLOR_FIGHTER_BUSH = new Color(0.65f, 0.95f, 0.70f, 0.75f);
    private final Color tempColor = new Color();
    private static final Color COLOR_TAG_BLUE = new Color(0.30f, 0.80f, 1.0f, 1f);
    private static final Color COLOR_TAG_RED = new Color(1.0f, 0.40f, 0.40f, 1f);
    private static final Color COLOR_TAG_SELF = new Color(1.0f, 0.90f, 0.20f, 1f);

    // -- Hiệu năng: biến dùng lại thay vì cấp phát mỗi frame --
    private static final float PHYSICS_STEP = 1f / 60f;
    private float physicsAccumulator = 0f;           // Bước vật lý cố định, độc lập FPS
    private final Vector2 tmpVec = new Vector2();
    private final Vector3 tmpVec3 = new Vector3();
    private final TextureRegion idleLegRegion = new TextureRegion();
    private final TextureRegion idleTorsoRegion = new TextureRegion();
    private float playerFountainAccum = 0f, playerZapAccum = 0f; // phần lẻ HP hồi/mất theo thời gian
    private int lastDashTenths = Integer.MIN_VALUE;
    private int lastStunTenths = Integer.MIN_VALUE;
    private int lastUltButtonKey = Integer.MIN_VALUE;
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout glyphLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
    private GroundMob eagleTargetMob = null;          // mục tiêu khóa lúc bắt đầu ứng kích
    private Fighter5v5 eagleTargetFighter = null;
    private boolean partnerLeft = false;
    private boolean coopWipeShown = false;
    private String playerTag5v5 = "";
    private int nextMobId = 0;
    private boolean rewardsGranted = false;
    private Fighter5v5 lockOnFighter = null;
    private Texture idleFallbackTex, padBgTex, padKnobTex;

    // New Guard & Boss sprites
    private Texture texLinhDung, texLinhBuoc, texLinhDon, texLinhBiDanh, texLinhChet, texLinhDanh1, texLinhDanh2, texLinhNga;
    private TextureRegion linhDungFrame, linhDonFrame, linhBiDanhFrame, linhChetFrame, linhDanh1Frame, linhDanh2Frame, linhNgaFrame;
    private Animation<TextureRegion> linhRunAnim;

    // Boss (Thu Linh) Textures & Frames
    private Texture texBossBuoc1, texBossBuoc2, texBossDanh1, texBossDanh2, texBossGam, texBossUlti, texBossNga, texBossChet;
    private TextureRegion bossDanh1Frame, bossDanh2Frame, bossGamFrame, bossUltiFrame, bossNgaFrame, bossChetFrame;
    private Animation<TextureRegion> bossWalkAnim;

    // Imperial Soldier (Quan Trieu Dinh) Textures & Frames
    private String imperialState = "FLY"; // FLY, ATTACK_PREP, ATTACK_FIRE, FALLING, DEAD
    private float imperialDeathTimer = 0f;
    private Texture texTrieuDinhBay1, texTrieuDinhBay2, texTrieuDinhBan1, texTrieuDinhBan2, texTrieuDinhRoi, texTrieuDinhChet;
    private TextureRegion trieuDinhBan1Frame, trieuDinhBan2Frame, trieuDinhRoiFrame, trieuDinhChetFrame;
    private Animation<TextureRegion> trieuDinhFlyAnim;

    // Assassination button & Cinematic Camera
    private TextButton btnAssassinate;
    private GroundMob targetToAssassinate = null;
    private Fighter5v5 targetFighterToAssassinate = null;
    private boolean targetRemoteToAssassinate = false;
    private String targetAssassinatePeerId = null;
    private float cinematicAssassinateTimer = 0f;
    private float assassinateFocusX = 0f, assassinateFocusY = 0f;
    private boolean isAssassinating = false;
    private boolean assassinateExecuted = false;
    private GroundMob pendingAssassinateMob = null;
    private Fighter5v5 pendingAssassinateFighter = null;
    private boolean pendingAssassinateRemote = false;
    private String pendingAssassinatePeerId = null;
    private float assassinateVictimX = 0f, assassinateVictimY = 0f;

    // -- UI --
    private Stage uiStage;
    private Skin skin;
    private BitmapFont damageFont;
    private boolean moveLeft, moveRight, isAimingUp, isAimingUpHold, isCrouching, isClimbingHold;
    private boolean needsHitboxUpdate = false;
    private Label actionLog, lblPlayerHp, lblEnemyInfo, lblCurrentWeapon, lblPartnerHp, lblScoreGold;
    private HealthBar playerHpBar, enemyHpBar, partnerHpBar;
    private TextButton btnEagleStrike, btnShootCannon;

    // -- Pause Menu & Game Over dialog & Touch Controls --
    private boolean isPaused = false;
    private boolean isSettingsModalOpen = false;
    private int pauseMenuIndex = 0;
    private TextButton btnPauseResume, btnPauseSettings, btnPauseExitBtn;
    private boolean virtualTouchHidden = false;
    private TextButton btnJumpRef, btnAtkRef, btnCrouchRef, btnClimbRef;
    private Table pauseDialog;
    private Table leftTable, rightTable;
    private Image modalOverlay;
    private Table gameOverDialog, dialogButtons;
    private Label lblEndGameTitle, lblEndStats;
    private TextButton btnPlayAgain, btnWaitRevive, btnQuitGame;
    private boolean isMatchEnded = false;
    private boolean isDisposed = false;

    // -- Player physics --
    private Body playerBody;
    private Fixture playerPhysicsFixture;
    private boolean isGrounded = false;
    private int footContacts = 0;
    private int jumpCount = 0;
    private int playerFacing = 1;
    private int playerHp = 100;
    private int playerMaxHp = 100;
    private int swordDamageBonus = 0;
    private int healthPotions = 1;
    private boolean isShopOpen = false;
    private Table shopDialogTable;
    private Label lblPotions;
    private boolean playerDead = false;
    private float playerDeathX = 0f, playerDeathY = 0f;
    private float playerDeathBlinkTimer = 0f;
    private float remoteDeathX = 0f, remoteDeathY = 0f;
    private float remoteDeathBlinkTimer = 0f;
    private float remoteRespawnTimer = 0f;
    private float playerStunTimer = 0f;

    // -- Dash & Cooldowns --
    private boolean isDashing = false;
    private float dashTime = 0f;
    private boolean hitThisDash = false;
    private float dashCooldownTimer = 0f;
    private float jumpCooldownTimer = 0f;
    private TextButton btnDashRef;

    // -- Ultimate System --
    private static final int ULTIMATE_REQ_KILLS = 4;
    private int ultimateKills = ULTIMATE_REQ_KILLS; // Sẵn sàng dùng ngay từ đầu trận!
    private TextButton btnUltimate;
    private TextButton.TextButtonStyle ultBtnNormalStyle, ultBtnReadyStyle, ultBtnP2LockStyle;

    // P1 Yasuo "Trấn Trời"
    private boolean isP1UltActive = false;
    private float p1UltTimer = 0f;
    private int p1UltBeat = 0;

    // P2 "Sát Chiêu Hoàn Hảo"
    private boolean isP2StealthActive = false;
    private float p2StealthTimer = 0f;
    private static final float P2_STEALTH_DURATION = 8.0f;
    private static final float P2_STEALTH_RANGE = 2.8f;
    private boolean p2TargetInRange = false;
    private Vector2 p2TargetPos = new Vector2();
    private int p2TargetFacing = 1;
    private Vector2 p2BehindTargetPos = new Vector2();
    private boolean isP2DashingAssassinate = false;
    private float p2DashAssassinateTimer = 0f;

    // Dư ảnh bóng ma sát thủ (Shadow Afterimages)
    public static class P2Afterimage {
        public float x, y;
        public int facing;
        public TextureRegion frame;
        public float alpha = 0.85f;
        public float lifeTime = 0.55f;
        public Color color = new Color(0.70f, 0.15f, 0.95f, 0.85f);
    }
    private final Array<P2Afterimage> p2Afterimages = new Array<P2Afterimage>();

    // Parkour Wall Slide
    private boolean isWallSliding = false;

    // PVP Ultimate Orbs
    public static class UltOrb {
        public float x, y;
        public boolean active = true;
        public float respawnTimer = 0f;
        public float pulseTime = 0f;
        public UltOrb(float x, float y) { this.x = x; this.y = y; }
    }
    private final Array<UltOrb> pvpUltOrbs = new Array<UltOrb>();
    private Texture ultimateOrbTexture;

    // -- Climbing --
    private float climbStamina = MAX_CLIMB_STAMINA;
    private boolean isClimbing = false;

    // -- Rope swing (Physics Pendulum System) --
    private final Array<Vector2> ropeAnchors = new Array<>();
    private final Array<SwingRope> swingRopes = new Array<>();
    private SwingRope currentSwingingRope = null;
    private TextureRegion ropeRegion;
    private boolean isSwinging = false;
    private float swingCooldown = 0f;

    // -- Eagle Strike & Lock-On --
    private boolean isEagleStriking = false;
    private int eagleStrikePhase = 0; // 0: inactive, 1: athletic launch arc, 2: dive
    private float eagleStrikeLaunchTimer = 0f;
    private float eagleStrikeTimeout = 0f;
    private float cameraShakeTime = 0f;
    private float cameraShakeIntensity = 0f;
    private float hitStopTimer = 0f;
    private float playerHitFlashTimer = 0f;
    private GroundMob lockOnEnemy = null;
    private Texture lockOnTexture;
    /** Pixel trắng dùng vẽ thanh máu/minimap (texture gỗ tô màu trước đây làm thanh bị sọc). */
    private final TextureRegion pixel = UiKit.whiteRegion();
    private TextureRegion lockOnRegion;

    // -- Score --
    private int score = 0;
    private int goldEarned = 0;
    private int stageBankedGold = 0;
    private int mobsKilledCount = 0;

    // -- Weapons --
    public enum WeaponType { SWORD, BOW, SHOTGUN, AXE, DUAL_DAGGERS }
    private WeaponType currentWeapon = WeaponType.SWORD;
    private float weaponAttackCooldown = 0f;
    private float totalMatchTime = 0f;

    // -- Floating damage text --
    private Array<DamageText> damageTexts = new Array<>();

    // -- Projectiles --
    private Array<Projectile> projectiles = new Array<>();

    // -- Pickups --
    private Array<Vector2> hpPickupPositions = new Array<>();
    private Array<Boolean> hpPickupActives = new Array<>();
    private Array<Vector2> weaponPickupPositions = new Array<>();
    private Array<Boolean> weaponPickupActives = new Array<>();
    private float itemRespawnTimer = 0f;

    // -- Map geometry --
    private Array<MapPlatform> platformsList = new Array<>();

    private Array<Ladder> laddersList = new Array<>();

    // -- Network --
    private static class RemotePeer5v5 {
        String id;
        String socketId = "";
        String name = "Đối thủ";
        int team = 2;
        int charId = 1;
        WeaponType weapon = WeaponType.SWORD;
        float x = -999f, y = -999f;
        float targetX = -999f, targetY = -999f;
        int facing = 1;
        String animState = "IDLE";
        float attackAnimTime = 0f;
        int hp = 1000, maxHp = 1000, shield = 0;
        boolean dead = false;
        boolean inBush = false;
        boolean stealth = false;
        float hitFlashTimer = 0f;
        float deathX = -999f, deathY = -999f;
        float deathBlinkTimer = 0f;
        float respawnTimer = 0f;
    }

    private GameSocket socket;
    private final String myNetId = DeviceIdentity.randomUuid().substring(0, 8);
    private String primaryRemoteId = "";
    private String primaryRemoteSocketId = "";
    private float remoteX = -999f, remoteY = -999f;
    private float targetRemoteX = -999f, targetRemoteY = -999f;
    private int remoteFacing = 1;
    private String remoteAnimState = "IDLE";
    private int remoteCharId = 1; // 1: Sát thủ trắng (Vân Tiêu), 2: Sát thủ đen (Vân U)
    private WeaponType remoteWeapon = WeaponType.SWORD;
    private int remoteTeam = 2;
    private String remoteName = "Đối thủ";
    private int remoteHp = 100;
    private int remoteShield = 0;
    private boolean remoteDead = false;
    private boolean remoteInBush = false;
    private boolean remoteStealth = false;
    private float remoteAttackAnimTime = 0f;
    private float remoteHitFlashTimer = 0f;
    private final Array<RemotePeer5v5> extraPeers5v5 = new Array<>();
    private final Array<Pixmap> ownedPixmaps = new Array<>();
    private boolean isHidden = false;
    private boolean networkListenersRegistered = false;
    private float playerPoisonSlowTimer = 0f;
    private float reviveCooldownTimer = 0f;
    private boolean pendingEagleGroundAbort = false;
    private String bowLockPeerId = null;
    private String lockOnPeerId = null;
    private String eagleTargetPeerId = null;
    private String p2TargetPeerId = null;
    private boolean lockOnIsPrimaryRemote = false;
    private boolean eagleTargetIsPrimaryRemote = false;
    private boolean p2TargetIsPrimaryRemote = false;

    /** Tạo Texture managed từ Pixmap (tự khôi phục khi Android mất GL context lúc chuyển ứng dụng). */
    private Texture managedTex(Pixmap pm) {
        ownedPixmaps.add(pm);
        return new Texture(new com.badlogic.gdx.graphics.glutils.PixmapTextureData(pm, null, false, false, true));
    }
    private float networkTimer = 0f;
    private float lastSentX = -999f, lastSentY = -999f;
    private String lastSentAnim = "";
    private int lastSentFacing = 0;
    private WeaponType lastSentWeapon = WeaponType.SWORD;
    private boolean lastSentDead = false;
    private float forceSyncTimer = 0f;

    // -- Ground mobs --
    private Array<GroundMob> activeMonsters = new Array<>();
    private int bossKills = 0;
    private float mobSpawnTimer = 0f;

    // -- Sky Pirate --
    private boolean hasSkyPirate = false;
    private float pirateX = 25.0f, pirateY = 6.2f;
    private int pirateHp = 140;
    private boolean pirateAlive = false;
    private int pirateFacing = -1;
    private enum PirateState { PATROL, ALERT, DIVE, CARRY, FALLING, DEAD }
    private PirateState pirateState = PirateState.PATROL;
    private float pirateAlertTimer = 3.0f;
    private float pirateCarryTimer = 1.0f;
    private float pirateDeathTimer = 0f;
    private boolean isGrabbedByPirate = false;
    private boolean isFallingFromSky = false;
    private float pirateRespawnTimer = 0f;

    // Sky Pirate (Khong Tac) Textures & Frames
    private Texture texKhongTac1, texKhongTac2, texKhongTacLao, texKhongTacRoi, texKhongTacChet;
    private TextureRegion khongTacLaoFrame, khongTacRoiFrame, khongTacChetFrame;
    private Animation<TextureRegion> khongTacFlyAnim;

    // -- Imperial soldier + cannon --
    private boolean hasImperial = false;
    private float imperialX = 45.0f, imperialY = 6.0f;
    private int imperialHp = 250;
    private boolean imperialAlive = false;
    private int imperialFacing = -1;
    private float imperialShootCooldown = 2.8f;
    private int imperialSkillCounter = 0;
    private float imperialRespawnTimer = 0f;
    private final Vector2 cannonPos = new Vector2(40.0f, 4.30f);

    // -- Sage healer --
    private boolean hasSage = false;
    private float sageX = 0f, sageY = 0f;
    private TextButton btnRevivePartner;

    // -- Orphan boy --
    public enum BoyState { INACTIVE, FLEEING, RESCUED, FALLING, DEAD }
    private BoyState boyState = BoyState.INACTIVE;
    private Body boyBody;
    private int boyHp = 350;
    private final int boyMaxHp = 350;
    private int boyFacing = 1;
    private GroundMob boyPursuer = null;
    private GroundMob boyGuard1 = null, boyGuard2 = null;
    private float boyAttackCooldown = 0f;
    private float boyHitTimer = 0f;
    private float boyDeathTimer = 0f;
    private boolean boyIsClimbing = false;
    private float boyJumpCooldown = 0f;
    private float boyDropThroughTimer = 0f;
    private float boyStuckTimer = 0f;
    private float boyLastX = 0f;

    // -- Khóa mục tiêu Cung Tên --
    private static final float BOW_LOCK_RANGE = 11.5f;
    private GroundMob bowLockMob = null;
    private Fighter5v5 bowLockFighter = null;
    private OutpostTurret bowLockTurret = null;
    private int bowLockSpecial = 0; // 0=không, 1=Không tặc, 2=Triều đình, 3=Đối thủ online, 4=Cổ Thần
    private final Vector2 bowLockWorldPos = new Vector2();

    // -- Kỹ năng chủ động 1, 2, 3 (Z, X, C) --
    private final float[] skillCooldowns = new float[3];
    private final int[] lastSkillBtnState = new int[]{-99, -99, -99};
    private TextButton btnSkill1, btnSkill2, btnSkill3;
    private TextButton.TextButtonStyle skillReadyStyleP1, skillReadyStyleP2, skillCdStyle;
    // === NỘI TẠI & KỸ NĂNG VÂN TIÊU (P1) ===
    private int p1PassiveStacks = 0;       // 0..4 tầng Kiếm Ý
    private float p1AwakenedTimer = 0f;    // Thời gian Kiếm Thần Thức Tỉnh (5s)
    private float p1WindShieldTimer = 0f;  // Khiên Lôi Phong hộ thể
    private int p1SwordRainRemaining = 0;  // 8 Thần Kiếm Vạn Kiếm Quy Tông
    private float p1SwordRainTick = 0f;

    // === NỘI TẠI & KỸ NĂNG VÂN U (P2) ===
    private float p2SwapAvailableTimer = 0f; // Cửa sổ Hoán Đổi với Phân Thân (2.5s)
    private boolean p2CloneActive = false;
    private float p2CloneX = 0f, p2CloneY = 0f, p2CloneTimer = 0f;
    private int p2CloneFacing = 1;
    private float p2AbyssTimer = 0f, p2AbyssTick = 0f, p2AbyssX = 0f, p2AbyssY = 0f;
    private float p2AbyssShieldAccum = 0f;

    // === ĐỒNG BỘ HIỆU ỨNG KỸ NĂNG NGƯỜI CHƠI KHÁC QUA MẠNG (REMOTE SKILLS) ===
    private float remoteWindShieldTimer = 0f;
    private boolean remoteCloneActive = false;
    private float remoteCloneX = 0f, remoteCloneY = 0f, remoteCloneTimer = 0f;
    private int remoteCloneFacing = 1, remoteCloneTeam = 2;
    private float remoteAbyssTimer = 0f, remoteAbyssTick = 0f, remoteAbyssX = 0f, remoteAbyssY = 0f;
    private int remoteAbyssTeam = 2;
    private int remoteSwordRainRemaining = 0;
    private float remoteSwordRainTick = 0f, remoteSwordRainX = 0f, remoteSwordRainY = 0f;
    private int remoteSwordRainFacing = 1, remoteSwordRainTeam = 2;

    // -- Chat & Voice Chat trực tuyến --
    private OnlineCommsManager commsManager;
    private Table chatPanel;
    private Label lblChatHistory, lblVoiceStatus;
    private ScrollPane chatScroll;
    private TextField chatInputField;
    private TextButton btnChatToggle, btnMicToggle;
    private boolean chatPanelOpen = false;
    private int lastUnreadBadge = -1;
    private int lastMicStateKey = -1;

    // Boy Textures & Frames
    private Texture texBoyDung, texBoyDi1, texBoyDi2, texBoyDanh, texBoyBiDanh, texBoyNga, texBoyChet;
    private TextureRegion boyIdleFrame, boyAtkFrame, boyHitFrame, boyNgaFrame, boyChetFrame;
    private Animation<TextureRegion> boyWalkAnim;

    // -- Helpers --
    public void addDamageText(float worldX, float worldY, int amount, Color color) {
        damageTexts.add(new DamageText(
            worldX + MathUtils.random(-0.25f, 0.25f), worldY + 0.5f,
            String.valueOf(amount), color));
    }

    public void addCustomText(float worldX, float worldY, String text, Color color) {
        damageTexts.add(new DamageText(worldX, worldY + 0.6f, text, color));
    }

    public void applySaveData(int savedHp, int savedGold, int savedScore, int savedPotions, float checkpointX) {
        if (savedHp > 0) this.playerHp = Math.min(savedHp, this.playerMaxHp);
        this.goldEarned = Math.max(0, savedGold);
        this.score = Math.max(0, savedScore);
        if (savedPotions >= 0) this.healthPotions = savedPotions;
        if (Float.isFinite(checkpointX) && checkpointX > 1.0f) {
            this.activeCheckpointX = checkpointX;
            // Khớp checkpoint gần nhất để lấy đúng độ cao Y (tránh bị đặt lên bục pháo phía trên ở ải 2-3)
            // và đánh dấu các checkpoint đã đi qua để không kích hoạt lại ngay khung hình 1.
            float targetY = -1f;
            for (int i = 0; i < checkpoints.size; i++) {
                MapCheckpoint cp = checkpoints.get(i);
                if (cp.x <= checkpointX + 1.2f) {
                    cp.activated = true;
                }
                if (Math.abs(cp.x - checkpointX) <= 1.2f) {
                    targetY = cp.y + 0.25f;
                }
            }
            if (targetY < 0f) {
                MapPlatform g = groundAt(checkpointX);
                targetY = (g != null) ? (g.y + g.height + 0.5f) : 1.2f;
            }
            this.activeCheckpointY = targetY;
            if (playerBody != null) {
                playerBody.setTransform(checkpointX, targetY, 0f);
                playerBody.setLinearVelocity(0, 0);
                float halfInitW = viewport.getWorldWidth() / 2f;
                float halfInitH = viewport.getWorldHeight() / 2f;
                float initCamX = MathUtils.clamp(checkpointX, halfInitW, mapWidth - halfInitW);
                float initCamY = MathUtils.clamp(targetY + 0.6f, halfInitH, mapHeight - halfInitH);
                camera.position.set(initCamX, initCamY, 0);
                camera.update();
            }
        }
        if (playerHpBar != null) playerHpBar.resetHp(playerHp, playerMaxHp);
        if (lblPlayerHp != null) lblPlayerHp.setText("Máu: " + playerHp + " HP");
        updateScoreGoldUI();
    }

    // -- Constructors --
    public GameScreen(Main game) { this(game, GameMode.SOLO, 1); }
    public GameScreen(Main game, GameMode mode) { this(game, mode, 1); }

    public GameScreen(Main game, GameMode mode, int level) {
        this.game = game;
        this.currentGameMode = mode;
        this.currentLevel = (mode == GameMode.SOLO) ? StoryChapterConfig.clampIndex(level) : level;
        this.story = (mode == GameMode.SOLO) ? StoryChapterConfig.get(this.currentLevel) : null;
        this.freePlay = (mode == GameMode.SOLO) && game != null && game.freePlayMode;

        try {
            if (NetworkManager.getInstance() != null) {
                this.socket = NetworkManager.getInstance().socket;
            }
        } catch (Exception ignored) {}

        if (currentGameMode == GameMode.PVP) {
            this.mapWidth = 25.6f;
            this.mapHeight = 8.0f;
            this.finishLineX = 999f;
            this.playerMaxHp = 1000;
            this.playerHp = 1000;
            this.remoteHp = 1000;
            this.healthPotions = 0; // Đấu 1v1 không dùng bình máu
            pvpUltOrbs.clear();
            pvpUltOrbs.add(new UltOrb(12.8f, 3.8f)); // Trung tâm võ đài
            pvpUltOrbs.add(new UltOrb(6.5f, 4.2f));  // Bục cao bên trái
            pvpUltOrbs.add(new UltOrb(19.1f, 4.2f)); // Bục cao bên phải
        } else if (currentGameMode == GameMode.BATTLE_5V5) {
            this.mapWidth = 60.0f;
            this.mapHeight = 8.8f;
            this.finishLineX = 999f;
            this.playerMaxHp = 1000;
            this.playerHp = 1000;
            this.remoteHp = 1000;
        } else if (story != null) {
            this.mapWidth = story.mapWidth;
            this.mapHeight = 9.6f + story.chapter * 0.3f;
            this.finishLineX = this.mapWidth - 3.5f;
        } else {
            this.mapWidth = 85.0f + (level * 25.0f);
            this.mapHeight = 9.2f + (level * 0.5f);
            this.finishLineX = this.mapWidth - 3.5f;
        }

        this.ultimateKills = ULTIMATE_REQ_KILLS; // Sẵn sàng dùng ngay khi vừa vào trận!

        // Xác định chính xác nhân vật và phe: nếu đang ở phòng online, lấy cấu hình server đã đồng bộ trong NetworkManager
        if (currentGameMode != GameMode.BATTLE_5V5 && game != null) {
            game.selectedTeam = 1; // Tránh sót phe Đỏ từ trận 5v5 trước gây đánh nhầm đồng đội ở CO-OP
        }
        if (currentGameMode != GameMode.SOLO && NetworkManager.getInstance() != null) {
            NetworkManager nm = NetworkManager.getInstance();
            boolean inOnlineRoom = nm.currentRoomCode != null && !nm.currentRoomCode.isEmpty();
            int resolvedChar;
            if (inOnlineRoom) {
                resolvedChar = (nm.myCharacter == 2) ? 2 : 1;
            } else {
                resolvedChar = "P2".equals(game != null ? game.selectedCharacter : null) ? 2 : 1;
            }
            this.selectedChar = (resolvedChar == 2) ? "P2" : "P1";
            nm.myCharacter = resolvedChar;
            if (game != null) game.selectedCharacter = this.selectedChar;

            if (currentGameMode == GameMode.BATTLE_5V5 && inOnlineRoom && (nm.myTeam == 1 || nm.myTeam == 2)) {
                if (game != null) game.selectedTeam = nm.myTeam;
            }
            this.remoteCharId = (nm.partnerCharacter == 2) ? 2 : 1;
            int myT = (game != null && game.selectedTeam == 2) ? 2 : 1;
            this.remoteTeam = (currentGameMode == GameMode.COOP) ? myT
                : ((nm.partnerTeam == 1 || nm.partnerTeam == 2) ? nm.partnerTeam : (myT == 1 ? 2 : 1));
            if (nm.partnerName != null && !nm.partnerName.isEmpty()) {
                this.remoteName = nm.partnerName;
            }
        } else if (game != null && game.selectedCharacter != null && !game.selectedCharacter.isEmpty()) {
            this.selectedChar = "P2".equals(game.selectedCharacter) ? "P2" : "P1";
        } else {
            this.selectedChar = "P1";
        }
        this.playerCurrentUltType = this.selectedChar;

        camera = new OrthographicCamera();
        viewport = new FitViewport(11.0f, 6.18f, camera);
        world = new World(new Vector2(0, -8.5f), true);
        batch = new SpriteBatch();

        int mapIdx = 1;
        if (story != null) {
            mapIdx = story.chapter;
        } else if (game != null && game.selectedOnlineMap >= 1 && game.selectedOnlineMap <= 3) {
            mapIdx = game.selectedOnlineMap;
        } else if (level >= 1 && level <= 3) {
            mapIdx = level;
        }
        currentMapThemeIndex = mapIdx;
        String bgName = (mapIdx == 2) ? "map2_bencang.png" : ((mapIdx == 3) ? "map3_phaodai.png" : "map1_thanhco.png");
        arenaBackground = GameAssets.background(bgName);
        if (arenaBackground == null) arenaBackground = GameAssets.background("anhnen.png");
        if (arenaBackground == null) arenaBackground = GameAssets.background("background.jpg");

        createProceduralTextures();

        // Chữ bay trong thế giới game (sát thương, tên, thông báo) – font CÓ DẤU.
        // Các chỗ gọi setScale(x) vẫn ghi theo cỡ font cũ (Arial 15px) nên nhân thêm DMG_FONT_K.
        if (Gdx.files.internal("ui/font_body_bold.fnt").exists()) {
            damageFont = new BitmapFont(Gdx.files.internal("ui/font_body_bold.fnt"));
            for (TextureRegion r : damageFont.getRegions()) r.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            damageFont.setUseIntegerPositions(false);
            dmgFontK = 0.56f;
        } else {
            damageFont = new BitmapFont();
            dmgFontK = 1f;
        }
        damageFont.getData().setScale(1.6f * dmgFontK);

        loadCharacterSprites();
        loadAttackTextures();
        stateTime = 0f;
        logAnimationStatus();

        // CO-OP: 2 máy phải sinh CÙNG một bản đồ ngẫu nhiên -> seed theo mã phòng + ải.
        // (Trước đây mỗi máy tự random nên 2 người chơi ở 2 map khác nhau.)
        boolean seeded = false;
        if (currentGameMode == GameMode.COOP) {
            String room = (NetworkManager.getInstance() != null) ? NetworkManager.getInstance().currentRoomCode : "";
            MathUtils.random.setSeed(((long) room.hashCode() * 31L) + currentLevel * 7919L + 1L);
            seeded = true;
        } else if (story != null) {
            // Mỗi ải cốt truyện có một bản đồ CỐ ĐỊNH (chơi lại vẫn là map đó, dễ cân bằng độ khó)
            MathUtils.random.setSeed(currentLevel * 104729L + 17L);
            seeded = true;
        }

        createWorldObjects();
        setupItemPositions();
        createPlayer();

        if (currentGameMode == GameMode.BATTLE_5V5) {
            init5v5Fighters();
        } else if (currentGameMode != GameMode.PVP) {
            initMapMonsters();
        }
        if (seeded) MathUtils.random.setSeed(System.nanoTime());

        if (game != null) {
            String pName = (game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần";
            int pTeam = game.selectedTeam;
            playerTag5v5 = "[BẠN] " + (pTeam == 1 ? "[XANH] " : "[ĐỎ] ") + pName;
        }

        float halfInitW = viewport.getWorldWidth() / 2f;
        float halfInitH = viewport.getWorldHeight() / 2f;
        float initCamX = MathUtils.clamp(playerBody.getPosition().x, halfInitW, mapWidth - halfInitW);
        float initCamY = MathUtils.clamp(playerBody.getPosition().y + 0.6f, halfInitH, mapHeight - halfInitH);
        camera.position.set(initCamX, initCamY, 0);
        camera.update();

        if (currentGameMode != GameMode.SOLO) {
            commsManager = new OnlineCommsManager(game);
        }

        createMobileUI();
        setupContactListener();
    }

    private void loadCharacterSprites() {
        if (Gdx.files.internal("player_run.png").exists()) {
            playerRunSheet = GameAssets.sprite("player_run.png");
            int runCols = 5;
            TextureRegion[][] tmpRun = TextureRegion.split(playerRunSheet,
                playerRunSheet.getWidth() / runCols, playerRunSheet.getHeight());

            TextureRegion[] runFrames = new TextureRegion[runCols];
            TextureRegion[] monsterRunFrames = new TextureRegion[runCols];
            for (int i = 0; i < runCols; i++) {
                runFrames[i] = tmpRun[0][i];
                monsterRunFrames[i] = new TextureRegion(tmpRun[0][i]);
            }
            playerRunAnim = new Animation<>(0.15f, runFrames);
            playerRunAnim.setPlayMode(Animation.PlayMode.LOOP);
            monsterRunAnim = new Animation<>(0.18f, monsterRunFrames);
            monsterRunAnim.setPlayMode(Animation.PlayMode.LOOP);
        }

        try {
            texLinhDung = GameAssets.sprite("linhdung.png");
            texLinhBuoc = GameAssets.sprite("linhbuoc.png");
            texLinhDon = GameAssets.sprite("linhdon.png");
            texLinhBiDanh = GameAssets.sprite("linhbidanh.png");
            texLinhChet = GameAssets.sprite("linhchet.png");
            if (Gdx.files.internal("linhdanh1.png").exists()) {
                texLinhDanh1 = GameAssets.sprite("linhdanh1.png");
                linhDanh1Frame = new TextureRegion(texLinhDanh1);
            }
            if (Gdx.files.internal("linhdanh2.png").exists()) {
                texLinhDanh2 = GameAssets.sprite("linhdanh2.png");
                linhDanh2Frame = new TextureRegion(texLinhDanh2);
            }
            if (Gdx.files.internal("linhnga.png").exists()) {
                texLinhNga = GameAssets.sprite("linhnga.png");
                linhNgaFrame = new TextureRegion(texLinhNga);
            }

            linhDungFrame = new TextureRegion(texLinhDung);
            linhDonFrame = new TextureRegion(texLinhDon);
            linhBiDanhFrame = new TextureRegion(texLinhBiDanh);
            linhChetFrame = new TextureRegion(texLinhChet);

            linhRunAnim = new Animation<>(0.25f, new TextureRegion[]{ linhDungFrame, new TextureRegion(texLinhBuoc) });
            linhRunAnim.setPlayMode(Animation.PlayMode.LOOP);

            // Load Boy Textures & Frames
            if (Gdx.files.internal("caubedung.png").exists()) {
                texBoyDung = GameAssets.sprite("caubedung.png");
                boyIdleFrame = new TextureRegion(texBoyDung);
            }
            if (Gdx.files.internal("caubedi1.png").exists() && Gdx.files.internal("caubedi2.png").exists()) {
                texBoyDi1 = GameAssets.sprite("caubedi1.png");
                texBoyDi2 = GameAssets.sprite("caubedi2.png");
                boyWalkAnim = new Animation<>(0.20f, new TextureRegion[]{ new TextureRegion(texBoyDi1), new TextureRegion(texBoyDi2) });
                boyWalkAnim.setPlayMode(Animation.PlayMode.LOOP);
            }
            if (Gdx.files.internal("caubedanh.png").exists()) {
                texBoyDanh = GameAssets.sprite("caubedanh.png");
                boyAtkFrame = new TextureRegion(texBoyDanh);
            }
            if (Gdx.files.internal("caubebidanh.png").exists()) {
                texBoyBiDanh = GameAssets.sprite("caubebidanh.png");
                boyHitFrame = new TextureRegion(texBoyBiDanh);
            }
            if (Gdx.files.internal("caubenga.png").exists()) {
                texBoyNga = GameAssets.sprite("caubenga.png");
                boyNgaFrame = new TextureRegion(texBoyNga);
            }
            if (Gdx.files.internal("caubechet.png").exists()) {
                texBoyChet = GameAssets.sprite("caubechet.png");
                boyChetFrame = new TextureRegion(texBoyChet);
            }

            // Load Boss (Thu Linh) Textures & Frames
            if (Gdx.files.internal("thulinhbuoc1.png").exists() && Gdx.files.internal("thulinhbuoc2.png").exists()) {
                texBossBuoc1 = GameAssets.sprite("thulinhbuoc1.png");
                texBossBuoc2 = GameAssets.sprite("thulinhbuoc2.png");
                bossWalkAnim = new Animation<>(0.25f, new TextureRegion[]{ new TextureRegion(texBossBuoc1), new TextureRegion(texBossBuoc2) });
                bossWalkAnim.setPlayMode(Animation.PlayMode.LOOP);
            }
            if (Gdx.files.internal("thulinhdanh1.png").exists()) {
                texBossDanh1 = GameAssets.sprite("thulinhdanh1.png");
                bossDanh1Frame = new TextureRegion(texBossDanh1);
            }
            if (Gdx.files.internal("thulinhdanh2.png").exists()) {
                texBossDanh2 = GameAssets.sprite("thulinhdanh2.png");
                bossDanh2Frame = new TextureRegion(texBossDanh2);
            }
            if (Gdx.files.internal("thulinhgam.png").exists()) {
                texBossGam = GameAssets.sprite("thulinhgam.png");
                bossGamFrame = new TextureRegion(texBossGam);
            }
            if (Gdx.files.internal("thulinhulti.png").exists()) {
                texBossUlti = GameAssets.sprite("thulinhulti.png");
                bossUltiFrame = new TextureRegion(texBossUlti);
            }
            if (Gdx.files.internal("thulinhnga.png").exists()) {
                texBossNga = GameAssets.sprite("thulinhnga.png");
                bossNgaFrame = new TextureRegion(texBossNga);
            }
            if (Gdx.files.internal("thulinhchet.png").exists()) {
                texBossChet = GameAssets.sprite("thulinhchet.png");
                bossChetFrame = new TextureRegion(texBossChet);
            }

            // Load Imperial Soldier (Quan Trieu Dinh) Textures & Frames
            if (Gdx.files.internal("trieudinhbay1.png").exists() && Gdx.files.internal("trieudinhbay2.png").exists()) {
                texTrieuDinhBay1 = GameAssets.sprite("trieudinhbay1.png");
                texTrieuDinhBay2 = GameAssets.sprite("trieudinhbay2.png");
                trieuDinhFlyAnim = new Animation<>(0.20f, new TextureRegion[]{ new TextureRegion(texTrieuDinhBay1), new TextureRegion(texTrieuDinhBay2) });
                trieuDinhFlyAnim.setPlayMode(Animation.PlayMode.LOOP);
            }
            if (Gdx.files.internal("trieudinhban1.png").exists()) {
                texTrieuDinhBan1 = GameAssets.sprite("trieudinhban1.png");
                trieuDinhBan1Frame = new TextureRegion(texTrieuDinhBan1);
            }
            if (Gdx.files.internal("trieudinhban2.png").exists()) {
                texTrieuDinhBan2 = GameAssets.sprite("trieudinhban2.png");
                trieuDinhBan2Frame = new TextureRegion(texTrieuDinhBan2);
            }
            if (Gdx.files.internal("trieudinhroi.png").exists()) {
                texTrieuDinhRoi = GameAssets.sprite("trieudinhroi.png");
                trieuDinhRoiFrame = new TextureRegion(texTrieuDinhRoi);
            }
            if (Gdx.files.internal("trieudinhchet.png").exists()) {
                texTrieuDinhChet = GameAssets.sprite("trieudinhchet.png");
                trieuDinhChetFrame = new TextureRegion(texTrieuDinhChet);
            }

            // Load Sky Pirate (Khong Tac) Textures & Frames
            if (Gdx.files.internal("khongtac1.png").exists() && Gdx.files.internal("khongtac2.png").exists()) {
                texKhongTac1 = GameAssets.sprite("khongtac1.png");
                texKhongTac2 = GameAssets.sprite("khongtac2.png");
                khongTacFlyAnim = new Animation<>(0.22f, new TextureRegion[]{ new TextureRegion(texKhongTac1), new TextureRegion(texKhongTac2) });
                khongTacFlyAnim.setPlayMode(Animation.PlayMode.LOOP);
            }
            if (Gdx.files.internal("khongtaclao.png").exists()) {
                texKhongTacLao = GameAssets.sprite("khongtaclao.png");
                khongTacLaoFrame = new TextureRegion(texKhongTacLao);
            }
            if (Gdx.files.internal("khongtacroi.png").exists()) {
                texKhongTacRoi = GameAssets.sprite("khongtacroi.png");
                khongTacRoiFrame = new TextureRegion(texKhongTacRoi);
            }
            if (Gdx.files.internal("khongtacchet.png").exists()) {
                texKhongTacChet = GameAssets.sprite("khongtacchet.png");
                khongTacChetFrame = new TextureRegion(texKhongTacChet);
            }
        } catch (Exception e) {
            System.out.println("Loi tai anh linh/cau be/thu linh/trieu dinh/khong tac: " + e.getMessage());
        }

        // Load White Assassin (P1) Idle (trangdung1.png, trangdung2.png), Run, Jump & Weapon Stances
        try {
            if (Gdx.files.internal("trangdung1.png").exists()) {
                texTrangDung1 = GameAssets.sprite("trangdung1.png");
                if (texTrangDung1 != null) {
                    trangDung1Region = new TextureRegion(texTrangDung1);
                    staticIdleFrame = trangDung1Region;
                }
            }
            if (Gdx.files.internal("trangdung2.png").exists()) {
                texTrangDung2 = GameAssets.sprite("trangdung2.png");
            }

            Array<TextureRegion> whiteFrames = new Array<>(TextureRegion.class);
            if (trangDung1Region != null) whiteFrames.add(trangDung1Region);
            if (texTrangDung2 != null) whiteFrames.add(new TextureRegion(texTrangDung2));

            if (whiteFrames.size > 0) {
                hasMultiFrameWhiteIdle = (whiteFrames.size >= 2);
                whiteIdleAnim = new Animation<>(0.35f, whiteFrames.toArray(TextureRegion.class));
                whiteIdleAnim.setPlayMode(Animation.PlayMode.LOOP);
            }

            // White Assassin Run (trangchay1, trangchay2, trangchay3)
            Array<TextureRegion> whiteRunFrames = new Array<>(TextureRegion.class);
            if (Gdx.files.internal("trangchay1.png").exists()) {
                texTrangChay1 = GameAssets.sprite("trangchay1.png");
                if (texTrangChay1 != null) whiteRunFrames.add(new TextureRegion(texTrangChay1));
            }
            if (Gdx.files.internal("trangchay2.png").exists()) {
                texTrangChay2 = GameAssets.sprite("trangchay2.png");
                if (texTrangChay2 != null) whiteRunFrames.add(new TextureRegion(texTrangChay2));
            }
            if (Gdx.files.internal("trangchay3.png").exists()) {
                texTrangChay3 = GameAssets.sprite("trangchay3.png");
                if (texTrangChay3 != null) whiteRunFrames.add(new TextureRegion(texTrangChay3));
            }
            if (whiteRunFrames.size > 0) {
                whiteRunAnim = new Animation<>(0.12f, whiteRunFrames.toArray(TextureRegion.class));
                whiteRunAnim.setPlayMode(Animation.PlayMode.LOOP);
                if (playerRunAnim == null) playerRunAnim = whiteRunAnim;
            }

            // White Assassin Jump & Fall (trangnhay.png, trangroi.png)
            if (Gdx.files.internal("trangnhay.png").exists()) {
                texTrangNhay = GameAssets.sprite("trangnhay.png");
                if (texTrangNhay != null) whiteJumpUpFrame = new TextureRegion(texTrangNhay);
            }
            if (Gdx.files.internal("trangroi.png").exists()) {
                texTrangRoi = GameAssets.sprite("trangroi.png");
                if (texTrangRoi != null) whiteJumpDownFrame = new TextureRegion(texTrangRoi);
            }

        } catch (Exception e) {
            System.out.println("Loi tai anh trangdung/trangchay/trangnhay: " + e.getMessage());
        }

        // Load Dark Assassin (P2) 3-Frame Sequential Breathing Idle
        try {
            texDenDung1 = GameAssets.sprite("dendung1.png");
            texDenDung2 = GameAssets.sprite("dendung2.png");
            texDenDung3 = GameAssets.sprite("dendung3.png");
            denDung1Region = new TextureRegion(texDenDung1);
            darkIdleAnim = new Animation<>(0.25f, new TextureRegion[]{
                denDung1Region,
                new TextureRegion(texDenDung2),
                new TextureRegion(texDenDung3)
            });
            darkIdleAnim.setPlayMode(Animation.PlayMode.LOOP);
        } catch (Exception ignored) {}

        // Load Dark Assassin (P2) Run (denchay1.png, denchay2.png)
        try {
            Array<TextureRegion> darkRunFrames = new Array<>(TextureRegion.class);
            if (Gdx.files.internal("denchay1.png").exists()) {
                texDenChay1 = GameAssets.sprite("denchay1.png");
                if (texDenChay1 != null) darkRunFrames.add(new TextureRegion(texDenChay1));
            }
            if (Gdx.files.internal("denchay2.png").exists()) {
                texDenChay2 = GameAssets.sprite("denchay2.png");
                if (texDenChay2 != null) darkRunFrames.add(new TextureRegion(texDenChay2));
            }
            if (darkRunFrames.size > 0) {
                darkRunAnim = new Animation<>(0.14f, darkRunFrames.toArray(TextureRegion.class));
                darkRunAnim.setPlayMode(Animation.PlayMode.LOOP);
            }
            if (Gdx.files.internal("dennhay.png").exists()) {
                texDenNhay = GameAssets.sprite("dennhay.png");
                if (texDenNhay != null) denJumpUpFrame = new TextureRegion(texDenNhay);
            }
            if (Gdx.files.internal("denroi.png").exists()) {
                texDenRoi = GameAssets.sprite("denroi.png");
                if (texDenRoi != null) denJumpDownFrame = new TextureRegion(texDenRoi);
            }
            if (texDenChay2 != null) {
                denDashFrame = new TextureRegion(texDenChay2);
                denRollFrame = new TextureRegion(texDenChay2);
                if (denJumpUpFrame == null) denJumpUpFrame = new TextureRegion(texDenChay2);
            } else if (texDenChay1 != null) {
                denDashFrame = new TextureRegion(texDenChay1);
                denRollFrame = new TextureRegion(texDenChay1);
                if (denJumpUpFrame == null) denJumpUpFrame = new TextureRegion(texDenChay1);
            }
            if (denJumpDownFrame == null) {
                if (texDenChay1 != null) denJumpDownFrame = new TextureRegion(texDenChay1);
                else if (texDenChay2 != null) denJumpDownFrame = new TextureRegion(texDenChay2);
            }
        } catch (Exception ignored) {}

        if (Gdx.files.internal("player_idle.png").exists()) {
            playerIdleSheet = GameAssets.sprite("player_idle.png");
            TextureRegion[][] tmpIdle = TextureRegion.split(playerIdleSheet,
                playerIdleSheet.getWidth() / 4, playerIdleSheet.getHeight());
            if (staticIdleFrame == null) staticIdleFrame = tmpIdle[0][0];
            monsterIdleFrame = new TextureRegion(tmpIdle[0][0]);
        }

        if (Gdx.files.internal("player_dash.png").exists()) {
            playerDashTexture = GameAssets.sprite("player_dash.png");
            dashFrame = new TextureRegion(playerDashTexture);
        }
        if (Gdx.files.internal("ngoi.png").exists()) {
            playerCrouchTexture = GameAssets.sprite("ngoi.png");
            crouchFrame = new TextureRegion(playerCrouchTexture);
        }

        if (Gdx.files.internal("player_jump.png").exists()) {
            playerJumpTexture = GameAssets.sprite("player_jump.png");
            jumpUpFrame = new TextureRegion(playerJumpTexture);
        }
        if (Gdx.files.internal("player_down.png").exists()) {
            playerDownTexture = GameAssets.sprite("player_down.png");
            jumpDownFrame = new TextureRegion(playerDownTexture);
        }

        // Ensure staticIdleFrame & monsterIdleFrame are NEVER null
        if (staticIdleFrame == null) {
            Pixmap idleFallbackPix = new Pixmap(32, 48, Pixmap.Format.RGBA8888);
            idleFallbackPix.setColor(new Color(0.9f, 0.9f, 0.95f, 1.0f));
            idleFallbackPix.fillRectangle(8, 8, 16, 32);
            idleFallbackTex = managedTex(idleFallbackPix);
            staticIdleFrame = new TextureRegion(idleFallbackTex);
        }

        if (monsterIdleFrame == null) {
            monsterIdleFrame = staticIdleFrame;
        }

        // Frame fallbacks
        if (jumpUpFrame == null) jumpUpFrame = (whiteJumpUpFrame != null) ? whiteJumpUpFrame : staticIdleFrame;
        if (jumpDownFrame == null) jumpDownFrame = (whiteJumpDownFrame != null) ? whiteJumpDownFrame : staticIdleFrame;
        if (dashFrame == null) dashFrame = (texTrangChay3 != null) ? new TextureRegion(texTrangChay3) : ((whiteJumpUpFrame != null) ? whiteJumpUpFrame : staticIdleFrame);
        if (crouchFrame == null) crouchFrame = staticIdleFrame;
        if (denDashFrame == null) denDashFrame = (denDung1Region != null) ? denDung1Region : staticIdleFrame;
        if (denJumpUpFrame == null) denJumpUpFrame = (denDung1Region != null) ? denDung1Region : staticIdleFrame;
        if (denJumpDownFrame == null) denJumpDownFrame = (denDung1Region != null) ? denDung1Region : staticIdleFrame;
        if (denRollFrame == null) denRollFrame = (denDung1Region != null) ? denDung1Region : staticIdleFrame;
    }

    /** Ghi Logcat (tag "GameScreen") những animation nào bị thiếu -> nhân vật sẽ đứng yên một khung hình. */
    private void logAnimationStatus() {
        StringBuilder miss = new StringBuilder();
        if (whiteIdleAnim == null) miss.append(" whiteIdle(trangdung)");
        if (darkIdleAnim == null) miss.append(" darkIdle(dendung)");
        if (darkAttackAnim1 == null) miss.append(" darkAttack(dendanh)");
        if (linhRunAnim == null) miss.append(" linhRun(linhdung/linhbuoc)");
        if (bossWalkAnim == null) miss.append(" bossWalk(thulinhbuoc)");
        if (boyWalkAnim == null) miss.append(" boyWalk(caubedi)");
        if (trieuDinhFlyAnim == null) miss.append(" trieuDinhFly(trieudinhbay)");
        if (khongTacFlyAnim == null) miss.append(" khongTacFly(khongtac)");
        if (miss.length() == 0) Gdx.app.log("GameScreen", "Animation day du.");
        else Gdx.app.error("GameScreen", "THIEU animation:" + miss);
    }

    private Vector2 snapPickupPosition(float desiredX, float preferredY) {
        float x = MathUtils.clamp(desiredX, 2.0f, Math.max(2.0f, mapWidth - 2.0f));
        // Nếu có sàn/bục tại x gần preferredY (hoặc mặt đất), đặt vật phẩm ngay phía trên mặt sàn đó
        MapPlatform best = null;
        float bestScore = Float.MAX_VALUE;
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (x >= p.x + 0.35f && x <= p.x + p.width - 0.35f) {
                float topY = p.y + p.height;
                // Tránh đặt vật phẩm bên dưới trần hầm chui thấp
                if (hasLowCeilingAt(x, topY)) continue;
                float diff = Math.abs((topY + 0.45f) - preferredY);
                if (diff < bestScore) {
                    bestScore = diff;
                    best = p;
                }
            }
        }
        if (best != null) {
            return new Vector2(x, best.y + best.height + 0.45f);
        }
        float sx = snapToGround(x, 0.8f);
        MapPlatform g = groundAt(sx);
        float y = (g != null) ? (g.y + g.height + 0.45f) : Math.max(1.05f, preferredY);
        return new Vector2(sx, y);
    }

    private void setupItemPositions() {
        hpPickupPositions.clear();
        hpPickupActives.clear();
        weaponPickupPositions.clear();
        weaponPickupActives.clear();

        if (currentGameMode == GameMode.PVP) {
            hpPickupPositions.add(new Vector2(12.8f, 1.05f));
            hpPickupActives.add(true);
            weaponPickupPositions.add(new Vector2(12.8f, 3.2f));
            weaponPickupActives.add(true);
        } else if (currentGameMode == GameMode.BATTLE_5V5) {
            // Đấu trường 5v5 dài 60m và đã có hệ thống Bùa (PowerUpRune) đối xứng; đặt vật phẩm đối xứng trong [0, 60]
            hpPickupPositions.add(snapPickupPosition(15.0f, 1.05f));
            hpPickupPositions.add(snapPickupPosition(45.0f, 1.05f));
            for (int i = 0; i < hpPickupPositions.size; i++) hpPickupActives.add(true);
            weaponPickupPositions.add(snapPickupPosition(30.0f, 1.05f));
            for (int i = 0; i < weaponPickupPositions.size; i++) weaponPickupActives.add(true);
        } else {
            hpPickupPositions.add(snapPickupPosition(12.0f, 1.05f));
            hpPickupPositions.add(snapPickupPosition(28.0f, 1.05f));
            hpPickupPositions.add(snapPickupPosition(46.0f, 2.5f));
            hpPickupPositions.add(snapPickupPosition(65.0f, 1.05f));
            for (int i = 0; i < hpPickupPositions.size; i++) hpPickupActives.add(true);

            weaponPickupPositions.add(snapPickupPosition(8.5f, 2.3f));
            weaponPickupPositions.add(snapPickupPosition(34.0f, 3.2f));
            weaponPickupPositions.add(snapPickupPosition(54.0f, 4.5f));
            for (int i = 0; i < weaponPickupPositions.size; i++) weaponPickupActives.add(true);
        }
    }

    private void setupMapTerrainTextures(int mapId) {
        // 1. Platform Console (Chân dầm console gỗ đinh sắt đỡ bục - Triangular Architectural Bracket)
        Pixmap cpm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        cpm.setColor(new Color(0f, 0f, 0f, 0f));
        cpm.fill();
        Color timberDark = (mapId == 2) ? new Color(0.16f, 0.18f, 0.24f, 1f) : ((mapId == 3) ? new Color(0.20f, 0.20f, 0.26f, 1f) : new Color(0.28f, 0.16f, 0.10f, 1f));
        Color timberMid  = (mapId == 2) ? new Color(0.26f, 0.32f, 0.40f, 1f) : ((mapId == 3) ? new Color(0.32f, 0.34f, 0.42f, 1f) : new Color(0.48f, 0.30f, 0.18f, 1f));
        Color ironColor  = (mapId == 2) ? new Color(0.35f, 0.60f, 0.55f, 1f) : ((mapId == 3) ? new Color(0.58f, 0.64f, 0.72f, 1f) : new Color(0.55f, 0.58f, 0.64f, 1f));
        // Khối dầm tam giác xiên chịu lực
        cpm.setColor(timberMid);
        cpm.fillTriangle(0, 0, 31, 0, 0, 31);
        cpm.setColor(timberDark);
        cpm.fillTriangle(2, 6, 25, 6, 2, 29);
        // Thanh giằng nẹp kim loại gia cố
        cpm.setColor(ironColor);
        cpm.fillRectangle(0, 0, 32, 4);
        cpm.fillRectangle(0, 0, 4, 32);
        cpm.drawLine(4, 28, 28, 4);
        cpm.drawLine(5, 28, 28, 5);
        // Đinh tán vuông gia cố thời trung cổ
        cpm.setColor(Color.BLACK);
        cpm.fillRectangle(2, 2, 3, 3);
        cpm.fillRectangle(2, 22, 3, 3);
        cpm.fillRectangle(22, 2, 3, 3);
        cpm.setColor(Color.WHITE);
        cpm.drawPixel(2, 2); cpm.drawPixel(2, 22); cpm.drawPixel(22, 2);
        platformConsoleTex = managedTex(cpm);

        // 2. Map-Specific Terrain Textures (High-Contrast Medieval Pixel Art):
        if (mapId == 2) {
            // === MAP 2: BẾN CẢNG CỔ TRONG ĐÊM (Wet Harbor Cobblestone, Dark Dock Timbers, Sea Wall) ===
            // Sàn đá bến cảng ướt sương đêm (64x32) - Độ tương phản cao, nổi bật trên nền
            Pixmap gm = new Pixmap(64, 32, Pixmap.Format.RGBA8888);
            gm.setColor(new Color(0.18f, 0.22f, 0.28f, 1f));
            gm.fill();
            for (int r = 0; r < 4; r++) {
                int y0 = r * 8;
                int shift = (r % 2 == 0) ? 0 : 16;
                for (int b = 0; b < 4; b++) {
                    int x0 = (b * 16 + shift) % 64;
                    gm.setColor(new Color(0.30f, 0.38f, 0.48f, 1f));
                    gm.fillRectangle(x0 + 1, y0 + 1, 14, 6);
                    gm.setColor(new Color(0.48f, 0.62f, 0.75f, 1f));
                    gm.fillRectangle(x0 + 3, y0 + 2, 8, 2);
                    gm.setColor(new Color(0.10f, 0.14f, 0.18f, 1f));
                    gm.fillRectangle(x0, y0 + 7, 16, 1);
                }
            }
            gm.setColor(new Color(0.60f, 0.90f, 1.0f, 1f));
            gm.fillRectangle(0, 0, 64, 4);
            groundBrickTexture = managedTex(gm);

            // Bục ván gỗ cầu tàu sẫm màu ngâm nước biển (64x16)
            Pixmap pm = new Pixmap(64, 16, Pixmap.Format.RGBA8888);
            pm.setColor(new Color(0.22f, 0.24f, 0.28f, 1f));
            pm.fill();
            for (int plank = 0; plank < 4; plank++) {
                int px = plank * 16;
                pm.setColor(new Color(0.32f, 0.36f, 0.42f, 1f));
                pm.fillRectangle(px + 1, 2, 14, 12);
                pm.setColor(new Color(0.48f, 0.54f, 0.64f, 1f));
                pm.fillRectangle(px + 3, 4, 10, 2);
                pm.fillRectangle(px + 2, 9, 11, 2);
                pm.setColor(new Color(0.30f, 0.65f, 0.55f, 1f));
                pm.fillRectangle(px + 2, 3, 2, 2);
                pm.fillRectangle(px + 12, 3, 2, 2);
                pm.fillRectangle(px + 2, 11, 2, 2);
                pm.fillRectangle(px + 12, 11, 2, 2);
            }
            pm.setColor(new Color(0.65f, 0.92f, 1.0f, 1f));
            pm.fillRectangle(0, 0, 64, 3);
            pm.setColor(new Color(0.08f, 0.10f, 0.14f, 1f));
            pm.drawRectangle(0, 0, 64, 16);
            platformWoodTexture = managedTex(pm);

            // Tường kè đá cảng biển (64x64)
            Pixmap wm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            wm.setColor(new Color(0.16f, 0.20f, 0.26f, 1f));
            wm.fill();
            for (int row = 0; row < 4; row++) {
                int yOff = row * 16;
                int shift = (row % 2 == 0) ? 0 : 16;
                wm.setColor(new Color(0.28f, 0.35f, 0.45f, 1f));
                wm.fillRectangle(2, yOff + 2, 60, 12);
                wm.setColor(new Color(0.42f, 0.52f, 0.65f, 1f));
                wm.fillRectangle(4, yOff + 2, 56, 2);
                wm.setColor(new Color(0.08f, 0.11f, 0.15f, 1f));
                wm.fillRectangle((30 + shift) % 64, yOff, 4, 16);
                if (row == 1) {
                    wm.setColor(new Color(0.55f, 0.68f, 0.82f, 1f));
                    wm.drawCircle(32, yOff + 8, 5);
                }
            }
            wallTexture = managedTex(wm);

        } else if (mapId == 3) {
            // === MAP 3: PHÁO ĐÀI TRÊN NÚI TRƯỚC CƠN BÃO (Alpine Slate, Reinforced Military Timber, Bastion Ramparts) ===
            // Sàn đá phiến pháo đài đỉnh núi góc cạnh (64x32)
            Pixmap gm = new Pixmap(64, 32, Pixmap.Format.RGBA8888);
            gm.setColor(new Color(0.22f, 0.24f, 0.30f, 1f));
            gm.fill();
            for (int r = 0; r < 4; r++) {
                int y0 = r * 8;
                int shift = (r % 2 == 0) ? 0 : 16;
                for (int b = 0; b < 4; b++) {
                    int x0 = (b * 16 + shift) % 64;
                    gm.setColor(new Color(0.38f, 0.42f, 0.50f, 1f));
                    gm.fillRectangle(x0 + 1, y0 + 1, 14, 6);
                    gm.setColor(new Color(0.60f, 0.68f, 0.80f, 1f));
                    gm.drawLine(x0 + 2, y0 + 2, x0 + 12, y0 + 3);
                    gm.setColor(new Color(0.85f, 0.90f, 0.98f, 0.9f));
                    gm.fillRectangle(x0 + 4, y0 + 5, 4, 1);
                }
            }
            gm.setColor(new Color(0.95f, 0.98f, 1.0f, 1f));
            gm.fillRectangle(0, 0, 64, 4);
            groundBrickTexture = managedTex(gm);

            // Giàn giáo gỗ thông núi giằng kim loại kiên cố (64x16)
            Pixmap pm = new Pixmap(64, 16, Pixmap.Format.RGBA8888);
            pm.setColor(new Color(0.26f, 0.24f, 0.22f, 1f));
            pm.fill();
            pm.setColor(new Color(0.40f, 0.36f, 0.32f, 1f));
            pm.fillRectangle(2, 2, 60, 12);
            pm.setColor(new Color(0.55f, 0.60f, 0.68f, 1f));
            pm.fillRectangle(0, 2, 6, 12);
            pm.fillRectangle(58, 2, 6, 12);
            pm.drawLine(6, 3, 58, 13);
            pm.drawLine(6, 13, 58, 3);
            pm.setColor(Color.WHITE);
            pm.drawPixel(2, 4); pm.drawPixel(2, 11);
            pm.drawPixel(60, 4); pm.drawPixel(60, 11);
            pm.setColor(new Color(0.92f, 0.96f, 1.0f, 1f));
            pm.fillRectangle(0, 0, 64, 3);
            pm.setColor(new Color(0.10f, 0.10f, 0.12f, 1f));
            pm.drawRectangle(0, 0, 64, 16);
            platformWoodTexture = managedTex(pm);

            // Vách lũy pháo đài đá hộc thô ráp kiên cố (64x64)
            Pixmap wm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            wm.setColor(new Color(0.18f, 0.20f, 0.25f, 1f));
            wm.fill();
            for (int row = 0; row < 4; row++) {
                int yOff = row * 16;
                int shift = (row % 2 == 0) ? 0 : 16;
                wm.setColor(new Color(0.32f, 0.36f, 0.44f, 1f));
                wm.fillRectangle(2, yOff + 2, 60, 12);
                wm.setColor(new Color(0.52f, 0.60f, 0.72f, 1f));
                wm.fillRectangle(4, yOff + 2, 56, 2);
                wm.setColor(new Color(0.10f, 0.11f, 0.14f, 1f));
                wm.fillRectangle((30 + shift) % 64, yOff, 4, 16);
                if (row == 2) {
                    wm.setColor(Color.BLACK);
                    wm.fillRectangle(28, yOff + 4, 8, 8);
                }
            }
            wallTexture = managedTex(wm);

        } else {
            // === MAP 1: THÀNH PHỐ CỔ LÚC HOÀNG HÔN (Weathered Castle Stone, Ancient Oak Beams, Medieval Ashlar Walls) ===
            // Đá thành cổ sa thạch phong hóa nhuốm màu hoàng hôn (64x32)
            Pixmap gm = new Pixmap(64, 32, Pixmap.Format.RGBA8888);
            gm.setColor(new Color(0.28f, 0.23f, 0.19f, 1f));
            gm.fill();
            for (int r = 0; r < 4; r++) {
                int y0 = r * 8;
                int shift = (r % 2 == 0) ? 0 : 16;
                for (int b = 0; b < 4; b++) {
                    int x0 = (b * 16 + shift) % 64;
                    gm.setColor(new Color(0.45f, 0.38f, 0.30f, 1f));
                    gm.fillRectangle(x0 + 1, y0 + 1, 14, 6);
                    gm.setColor(new Color(0.65f, 0.54f, 0.35f, 1f));
                    gm.fillRectangle(x0 + 2, y0 + 4, 6, 2);
                    gm.setColor(new Color(0.16f, 0.12f, 0.09f, 1f));
                    gm.drawLine(x0 + 4, y0 + 1, x0 + 10, y0 + 5);
                }
            }
            gm.setColor(new Color(1.0f, 0.88f, 0.55f, 1f));
            gm.fillRectangle(0, 0, 64, 4);
            groundBrickTexture = managedTex(gm);

            // Xà dầm gỗ sồi cổ nẹp sắt rỉ đinh tán vuông (64x16)
            Pixmap pm = new Pixmap(64, 16, Pixmap.Format.RGBA8888);
            pm.setColor(new Color(0.38f, 0.24f, 0.14f, 1f));
            pm.fill();
            pm.setColor(new Color(0.55f, 0.36f, 0.20f, 1f));
            pm.fillRectangle(2, 2, 60, 12);
            pm.setColor(new Color(0.25f, 0.14f, 0.08f, 1f));
            pm.drawLine(8, 5, 26, 6);
            pm.drawLine(34, 10, 52, 9);
            pm.setColor(new Color(0.36f, 0.38f, 0.44f, 1f));
            pm.fillRectangle(0, 1, 6, 14);
            pm.fillRectangle(58, 1, 6, 14);
            pm.setColor(new Color(0.60f, 0.65f, 0.72f, 1f));
            pm.fillRectangle(2, 3, 2, 2);
            pm.fillRectangle(2, 11, 2, 2);
            pm.fillRectangle(60, 3, 2, 2);
            pm.fillRectangle(60, 11, 2, 2);
            pm.setColor(new Color(1.0f, 0.86f, 0.50f, 1f));
            pm.fillRectangle(0, 0, 64, 3);
            pm.setColor(new Color(0.16f, 0.09f, 0.04f, 1f));
            pm.drawRectangle(0, 0, 64, 16);
            platformWoodTexture = managedTex(pm);

            // Tường thành đá lũy trung cổ phong hóa (64x64)
            Pixmap wm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            wm.setColor(new Color(0.25f, 0.22f, 0.20f, 1f));
            wm.fill();
            for (int row = 0; row < 4; row++) {
                int yOff = row * 16;
                int shift = (row % 2 == 0) ? 0 : 16;
                wm.setColor(new Color(0.42f, 0.37f, 0.32f, 1f));
                wm.fillRectangle(2, yOff + 2, 60, 12);
                wm.setColor(new Color(0.60f, 0.52f, 0.44f, 1f));
                wm.fillRectangle(2, yOff + 2, 58, 2);
                wm.setColor(new Color(0.12f, 0.10f, 0.09f, 1f));
                wm.fillRectangle((30 + shift) % 64, yOff, 4, 16);
                wm.setColor(new Color(0.10f, 0.09f, 0.08f, 1f));
                wm.drawLine(10, yOff + 3, 22, yOff + 12);
            }
            wallTexture = managedTex(wm);
        }

        // Cấu hình Texture lặp vô tận (Repeat) & Nearest pixel art sắc nét chống vỡ hình
        if (groundBrickTexture != null) {
            groundBrickTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            groundBrickTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        if (platformWoodTexture != null) {
            platformWoodTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            platformWoodTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        if (wallTexture != null) {
            wallTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            wallTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
    }

    private void createSkillVisualTextures() {
        // === 1. ĐẠI KIẾM KHÍ BÁN NGUYỆT (P1 Crescent Aura Texture - 128x128) ===
        Pixmap cap = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        cap.setColor(new Color(0, 0, 0, 0));
        cap.fill();
        float arcCx = 36f, arcCy = 64f;
        float rOut = 62f, rIn = 38f;
        for (int py = 0; py < 128; py++) {
            for (int px = 0; px < 128; px++) {
                float dx = px - arcCx;
                float dy = py - arcCy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist >= rIn && dist <= rOut && dx >= -10f) {
                    float ang = Math.abs((float) Math.atan2(dy, dx));
                    if (ang <= 1.25f) { // Cung bán nguyệt ~140 độ
                        float normRadial = (dist - rIn) / (rOut - rIn);
                        float tipFade = 1.0f - (ang / 1.25f);
                        tipFade = (float) Math.pow(tipFade, 0.5f);

                        float alpha;
                        Color c;
                        if (normRadial >= 0.82f) {
                            float edgeFrac = (normRadial - 0.82f) / 0.18f;
                            alpha = Math.min(1.0f, (0.85f + 0.15f * edgeFrac) * tipFade);
                            c = new Color(0.92f, 0.98f, 1.0f, alpha);
                        } else if (normRadial >= 0.40f) {
                            float midFrac = (normRadial - 0.40f) / 0.42f;
                            alpha = Math.min(1.0f, (0.65f + 0.30f * midFrac) * tipFade);
                            c = new Color(0.20f + 0.65f * midFrac, 0.85f + 0.12f * midFrac, 1.0f, alpha);
                        } else {
                            float tailFrac = normRadial / 0.40f;
                            alpha = Math.min(1.0f, tailFrac * 0.60f * tipFade);
                            c = new Color(0.10f, 0.55f, 0.95f, alpha);
                        }
                        cap.setColor(c);
                        cap.drawPixel(px, py);
                    }
                }
            }
        }
        cap.setColor(Color.WHITE);
        for (float a = -1.0f; a <= 1.0f; a += 0.02f) {
            float rMid = (rIn + rOut) * 0.5f + (float) Math.sin(a * 12f) * 2f;
            int kx = (int) (arcCx + Math.cos(a) * rMid);
            int ky = (int) (arcCy + Math.sin(a) * rMid);
            if (kx >= 0 && kx < 128 && ky >= 0 && ky < 128) {
                float tf = (1.0f - Math.abs(a) / 1.0f);
                cap.setColor(new Color(1f, 1f, 1f, tf));
                cap.drawPixel(kx, ky);
            }
        }
        p1CrescentAuraTex = managedTex(cap);

        // === 2. CỔ THẦN BINH THẦN KIẾM (P1 Celestial Sword - 96x32) ===
        Pixmap swp = new Pixmap(96, 32, Pixmap.Format.RGBA8888);
        swp.setColor(new Color(0, 0, 0, 0));
        swp.fill();
        for (int x = 34; x <= 92; x++) {
            float frac = (x - 34f) / 58f;
            float halfH = (1.0f - frac) * 5.5f;
            int top = (int) Math.floor(16 - halfH);
            int bot = (int) Math.ceil(16 + halfH);
            for (int y = top; y <= bot; y++) {
                if (y == 16) {
                    swp.setColor(new Color(0.40f, 0.95f, 1.0f, 1.0f));
                } else if (y == top || y == bot) {
                    swp.setColor(Color.WHITE);
                } else if (y < 16) {
                    swp.setColor(new Color(0.85f, 0.92f, 1.0f, 0.95f));
                } else {
                    swp.setColor(new Color(0.52f, 0.65f, 0.82f, 0.95f));
                }
                swp.drawPixel(x, y);
            }
        }
        swp.setColor(new Color(0.95f, 0.78f, 0.20f, 1.0f));
        swp.fillRectangle(31, 6, 4, 20);
        swp.fillTriangle(35, 6, 35, 26, 39, 16);
        swp.fillTriangle(27, 4, 31, 8, 31, 4);
        swp.fillTriangle(27, 28, 31, 24, 31, 28);
        swp.setColor(new Color(0.15f, 0.90f, 1.0f, 1.0f));
        swp.fillCircle(33, 16, 2);
        swp.setColor(new Color(0.16f, 0.14f, 0.18f, 1.0f));
        swp.fillRectangle(14, 14, 17, 5);
        swp.setColor(new Color(0.92f, 0.75f, 0.18f, 1.0f));
        for (int gx = 16; gx < 30; gx += 4) {
            swp.drawLine(gx, 14, gx + 2, 18);
        }
        swp.setColor(new Color(0.95f, 0.78f, 0.20f, 1.0f));
        swp.fillCircle(11, 16, 4);
        swp.setColor(new Color(0.12f, 0.85f, 1.0f, 1.0f));
        swp.fillCircle(11, 16, 2);
        p1CelestialSwordTex = managedTex(swp);
        p1CelestialSwordTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p1CelestialSwordRegion = new TextureRegion(p1CelestialSwordTex);

        // === 3. BÁT QUÁI KIẾM TRẬN PHÁT QUANG (P1 Magic Array Circle - 128x128) ===
        Pixmap mgp = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        mgp.setColor(new Color(0, 0, 0, 0));
        mgp.fill();
        int mcX = 64, mcY = 64;
        mgp.setColor(new Color(0.20f, 0.85f, 1.0f, 0.45f));
        mgp.drawCircle(mcX, mcY, 60);
        mgp.drawCircle(mcX, mcY, 59);
        mgp.setColor(new Color(0.95f, 0.85f, 0.25f, 0.75f));
        mgp.drawCircle(mcX, mcY, 52);
        mgp.drawCircle(mcX, mcY, 44);
        mgp.setColor(new Color(0.30f, 0.95f, 1.0f, 0.65f));
        mgp.drawCircle(mcX, mcY, 26);
        mgp.setColor(new Color(0.95f, 0.88f, 0.35f, 0.85f));
        for (int d = 0; d < 8; d++) {
            float a = d * (MathUtils.PI / 4f);
            float cos = MathUtils.cos(a), sin = MathUtils.sin(a);
            mgp.drawLine((int)(mcX + cos * 26), (int)(mcY + sin * 26),
                         (int)(mcX + cos * 52), (int)(mcY + sin * 52));
            float bqX = mcX + cos * 48, bqY = mcY + sin * 48;
            mgp.fillCircle((int)bqX, (int)bqY, 2);
        }
        mgp.setColor(Color.WHITE);
        mgp.fillCircle(mcX, mcY, 6);
        mgp.setColor(new Color(0.15f, 0.90f, 1.0f, 1.0f));
        mgp.drawCircle(mcX, mcY, 12);
        p1MagicCircleTex = managedTex(mgp);
        p1MagicCircleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // === 4. HUYẾT TIÊU KUNAI ÁM KHÍ HẮC ÁM (P2 Kunai Texture - 64x20) ===
        Pixmap knp = new Pixmap(64, 20, Pixmap.Format.RGBA8888);
        knp.setColor(new Color(0, 0, 0, 0));
        knp.fill();
        knp.setColor(new Color(0.14f, 0.12f, 0.16f, 1.0f));
        knp.fillTriangle(24, 4, 24, 16, 60, 10);
        knp.setColor(new Color(0.95f, 0.12f, 0.28f, 1.0f));
        knp.drawLine(24, 4, 60, 10);
        knp.drawLine(24, 16, 60, 10);
        knp.setColor(new Color(0.68f, 0.15f, 0.85f, 1.0f));
        knp.drawLine(24, 10, 56, 10);
        knp.setColor(new Color(0.24f, 0.15f, 0.32f, 1.0f));
        knp.fillRectangle(10, 8, 14, 5);
        knp.setColor(new Color(0.85f, 0.20f, 0.45f, 1.0f));
        knp.drawRectangle(10, 8, 14, 5);
        knp.setColor(new Color(0.42f, 0.44f, 0.48f, 1.0f));
        knp.drawCircle(5, 10, 4);
        knp.drawCircle(5, 10, 3);
        p2KunaiTex = managedTex(knp);
        p2KunaiTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p2KunaiRegion = new TextureRegion(p2KunaiTex);
    }

    private void createProceduralTextures() {
        setupMapTerrainTextures(currentMapThemeIndex);
        createSkillVisualTextures();

        Pixmap lm = new Pixmap(32, 64, Pixmap.Format.RGBA8888);
        lm.setColor(new Color(0, 0, 0, 0));
        lm.fill();
        lm.setColor(new Color(0.42f, 0.25f, 0.14f, 1f));
        lm.fillRectangle(2, 0, 5, 64);
        lm.fillRectangle(25, 0, 5, 64);
        for (int y = 6; y < 64; y += 14) {
            lm.setColor(new Color(0.65f, 0.42f, 0.22f, 1f));
            lm.fillRectangle(6, y, 20, 4);
        }
        ladderTexture = managedTex(lm);

        Pixmap hpPm = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        // Outer pulsing glow aura
        hpPm.setColor(new Color(0.1f, 0.95f, 0.45f, 0.35f));
        hpPm.fillCircle(24, 24, 23);
        // Glass flask body
        hpPm.setColor(new Color(0.1f, 0.3f, 0.15f, 0.85f));
        hpPm.fillCircle(24, 26, 17);
        // Flask liquid (Emerald gradient)
        hpPm.setColor(new Color(0.12f, 0.95f, 0.38f, 0.95f));
        hpPm.fillCircle(24, 26, 14);
        // Golden neck & stopper
        hpPm.setColor(new Color(0.95f, 0.75f, 0.2f, 1f));
        hpPm.fillRectangle(20, 6, 8, 6);
        hpPm.setColor(new Color(0.65f, 0.4f, 0.15f, 1f));
        hpPm.fillRectangle(21, 3, 6, 4);
        // Glowing Holy Cross in the heart of the flask
        hpPm.setColor(Color.WHITE);
        hpPm.fillRectangle(22, 17, 4, 18);
        hpPm.fillRectangle(15, 24, 18, 4);
        // Light reflection highlight
        hpPm.setColor(new Color(1f, 1f, 1f, 0.75f));
        hpPm.fillCircle(19, 21, 3);
        hpOrbTexture = managedTex(hpPm);

        Pixmap wpm = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        // Ambient golden aura
        wpm.setColor(new Color(1f, 0.75f, 0.1f, 0.35f));
        wpm.fillCircle(24, 24, 23);
        // Mahogany wooden chest body
        wpm.setColor(new Color(0.42f, 0.22f, 0.08f, 1f));
        wpm.fillRectangle(6, 12, 36, 26);
        // Reinforced steel / gold brackets
        wpm.setColor(new Color(0.85f, 0.70f, 0.20f, 1f));
        wpm.drawRectangle(6, 12, 36, 26);
        wpm.fillRectangle(6, 12, 4, 26);
        wpm.fillRectangle(38, 12, 4, 26);
        wpm.fillRectangle(6, 23, 36, 4);
        // Center mythical crossed blades symbol with cyan energy
        wpm.setColor(new Color(0.15f, 0.85f, 1.0f, 1f));
        wpm.drawLine(16, 17, 32, 33);
        wpm.drawLine(17, 17, 33, 33);
        wpm.drawLine(32, 17, 16, 33);
        wpm.drawLine(31, 17, 15, 33);
        // Golden lock buckle
        wpm.setColor(Color.GOLD);
        wpm.fillCircle(24, 25, 4);
        wpm.setColor(Color.WHITE);
        wpm.fillCircle(24, 25, 2);
        weaponBoxTexture = managedTex(wpm);
        weaponBoxRegion = new TextureRegion(weaponBoxTexture);

        Pixmap ar = new Pixmap(36, 12, Pixmap.Format.RGBA8888);
        // Azure glowing energy trail & arrowhead
        ar.setColor(new Color(0.2f, 0.6f, 1.0f, 0.5f));
        ar.fillRectangle(0, 3, 26, 6);
        ar.setColor(new Color(0.9f, 0.95f, 1.0f, 1f));
        ar.fillRectangle(2, 4, 24, 4);
        ar.setColor(new Color(0.1f, 0.8f, 1.0f, 1f));
        ar.fillTriangle(26, 0, 36, 6, 26, 12);
        ar.setColor(Color.WHITE);
        ar.fillTriangle(28, 3, 34, 6, 28, 9);
        arrowTexture = managedTex(ar);
        arrowRegion = new TextureRegion(arrowTexture);

        Pixmap bl = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        bl.setColor(new Color(1f, 0.4f, 0.05f, 0.45f));
        bl.fillCircle(8, 8, 7);
        bl.setColor(new Color(1f, 0.65f, 0.1f, 0.85f));
        bl.fillCircle(8, 8, 5);
        bl.setColor(Color.WHITE);
        bl.fillCircle(8, 8, 3);
        bulletTexture = managedTex(bl);

        Pixmap uom = new Pixmap(44, 44, Pixmap.Format.RGBA8888);
        uom.setColor(new Color(0.95f, 0.75f, 0.1f, 0.45f));
        uom.fillCircle(22, 22, 21);
        uom.setColor(new Color(1f, 0.85f, 0.2f, 0.85f));
        uom.fillCircle(22, 22, 16);
        uom.setColor(new Color(1f, 1f, 0.6f, 1f));
        uom.fillCircle(22, 22, 10);
        uom.setColor(Color.WHITE);
        uom.fillCircle(22, 22, 5);
        ultimateOrbTexture = managedTex(uom);

        Pixmap rp = new Pixmap(8, 32, Pixmap.Format.RGBA8888);
        rp.setColor(new Color(0.6f, 0.42f, 0.2f, 1f));
        rp.fill();
        ropeTexture = managedTex(rp);
        ropeRegion = new TextureRegion(ropeTexture);

        Pixmap cPm = new Pixmap(48, 32, Pixmap.Format.RGBA8888);
        cPm.setColor(new Color(0.2f, 0.2f, 0.25f, 1f));
        cPm.fillRectangle(8, 6, 36, 16);
        cPm.setColor(new Color(0.6f, 0.4f, 0.2f, 1f));
        cPm.fillCircle(12, 22, 9);
        cannonTexture = managedTex(cPm);

        Pixmap cbPm = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        cbPm.setColor(Color.BLACK);
        cbPm.fillCircle(12, 12, 11);
        cbPm.setColor(Color.ORANGE);
        cbPm.fillCircle(8, 8, 4);
        cannonBallTexture = managedTex(cbPm);

        Pixmap spPm = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        spPm.setColor(new Color(0.55f, 0.4f, 0.25f, 1f));
        spPm.fillCircle(6, 6, 5);
        spPm.setColor(Color.WHITE);
        spPm.fillCircle(4, 4, 2);
        slingshotPelletTexture = managedTex(spPm);

        Pixmap flagPm = new Pixmap(36, 64, Pixmap.Format.RGBA8888);
        flagPm.setColor(Color.GOLD);
        flagPm.fillTriangle(6, 6, 34, 20, 6, 34);
        flagPm.setColor(Color.DARK_GRAY);
        flagPm.fillRectangle(2, 4, 4, 60);
        finishFlagTexture = managedTex(flagPm);

        Pixmap reticlePm = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        reticlePm.setColor(Color.RED);
        reticlePm.drawCircle(24, 24, 22);
        reticlePm.drawCircle(24, 24, 21);
        reticlePm.setColor(Color.GOLD);
        reticlePm.drawCircle(24, 24, 14);
        reticlePm.fillCircle(24, 24, 4);
        reticlePm.setColor(Color.RED);
        reticlePm.fillRectangle(22, 0, 4, 10);
        reticlePm.fillRectangle(22, 38, 4, 10);
        reticlePm.fillRectangle(0, 22, 10, 4);
        reticlePm.fillRectangle(38, 22, 10, 4);
        lockOnTexture = managedTex(reticlePm);
        lockOnTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        lockOnRegion = new TextureRegion(lockOnTexture);

        // 5v5 Jump Pad Texture (Bệ phóng nảy cao Parkour)
        Pixmap jpm = new Pixmap(48, 16, Pixmap.Format.RGBA8888);
        jpm.setColor(new Color(0.10f, 0.15f, 0.28f, 0.95f));
        jpm.fillRectangle(0, 4, 48, 12);
        jpm.setColor(new Color(0.2f, 0.85f, 1.0f, 1f));
        jpm.drawRectangle(0, 4, 48, 12);
        jpm.fillTriangle(24, 0, 16, 8, 32, 8);
        jpm.fillTriangle(14, 2, 8, 8, 20, 8);
        jpm.fillTriangle(34, 2, 28, 8, 40, 8);
        jumpPadTexture = managedTex(jpm);

        // 5v5 Contested Runes
        // 1. Cuồng Nộ (Berserk Flame Rune)
        Pixmap berPm = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        berPm.setColor(new Color(1.0f, 0.2f, 0.1f, 0.35f));
        berPm.fillCircle(20, 20, 19);
        berPm.setColor(new Color(0.9f, 0.15f, 0.1f, 0.95f));
        berPm.fillCircle(20, 20, 13);
        berPm.setColor(Color.ORANGE);
        berPm.fillTriangle(20, 4, 12, 28, 28, 28);
        berPm.setColor(Color.YELLOW);
        berPm.fillTriangle(20, 10, 15, 26, 25, 26);
        runeBerserkTex = managedTex(berPm);

        // 2. Khiên Hộ Thể (Divine Barrier Shield Rune)
        Pixmap shiPm = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        shiPm.setColor(new Color(0.1f, 0.6f, 1.0f, 0.35f));
        shiPm.fillCircle(20, 20, 19);
        shiPm.setColor(new Color(0.15f, 0.45f, 0.95f, 0.95f));
        shiPm.fillCircle(20, 20, 13);
        shiPm.setColor(Color.CYAN);
        shiPm.fillRectangle(12, 10, 16, 12);
        shiPm.fillTriangle(12, 22, 28, 22, 20, 32);
        shiPm.setColor(Color.WHITE);
        shiPm.fillCircle(20, 18, 4);
        runeShieldTex = managedTex(shiPm);

        // 3. Bùa Phong Tốc (Windstride Rune)
        Pixmap windPm = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        windPm.setColor(new Color(0.2f, 0.95f, 0.7f, 0.35f));
        windPm.fillCircle(20, 20, 19);
        windPm.setColor(new Color(0.1f, 0.85f, 0.5f, 0.95f));
        windPm.fillCircle(20, 20, 13);
        windPm.setColor(Color.WHITE);
        windPm.fillRectangle(8, 14, 20, 3);
        windPm.fillRectangle(12, 20, 20, 3);
        windPm.fillRectangle(10, 26, 16, 3);
        runeWindTex = managedTex(windPm);

        // 4. Thần Dược Hồi Phục (Ancient Mega Heal Relic)
        Pixmap healPm = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        healPm.setColor(new Color(0.1f, 0.9f, 0.2f, 0.35f));
        healPm.fillCircle(20, 20, 19);
        healPm.setColor(new Color(0.15f, 0.75f, 0.2f, 0.95f));
        healPm.fillCircle(20, 20, 13);
        healPm.setColor(Color.WHITE);
        healPm.fillRectangle(17, 8, 6, 24);
        healPm.fillRectangle(8, 17, 24, 6);
        runeHealTex = managedTex(healPm);

        // 5. Ngọc Tối Thượng Thần Bí (Random Ultimate Orb)
        Pixmap ultPm = new Pixmap(44, 44, Pixmap.Format.RGBA8888);
        ultPm.setColor(new Color(1.0f, 0.85f, 0.1f, 0.45f));
        ultPm.fillCircle(22, 22, 21);
        ultPm.setColor(new Color(0.75f, 0.15f, 0.95f, 0.95f));
        ultPm.fillCircle(22, 22, 15);
        ultPm.setColor(Color.GOLD);
        ultPm.fillCircle(22, 22, 9);
        ultPm.setColor(Color.WHITE);
        ultPm.fillCircle(22, 22, 4);
        runeUltTex = managedTex(ultPm);

        Pixmap spkPix = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        spkPix.setColor(Color.WHITE);
        spkPix.fillCircle(6, 6, 5);
        sparkTexture = managedTex(spkPix);

        // 6. Bụi rậm chiến thuật (Tactical Ambush Bush Texture)
        Pixmap bshPm = new Pixmap(64, 48, Pixmap.Format.RGBA8888);
        bshPm.setColor(new Color(0.06f, 0.25f, 0.08f, 0.90f));
        bshPm.fillCircle(16, 26, 15);
        bshPm.fillCircle(48, 26, 15);
        bshPm.fillCircle(32, 20, 18);
        bshPm.setColor(new Color(0.12f, 0.50f, 0.16f, 0.95f));
        bshPm.fillCircle(16, 22, 13);
        bshPm.fillCircle(48, 22, 13);
        bshPm.fillCircle(32, 16, 15);
        bshPm.setColor(new Color(0.25f, 0.75f, 0.28f, 0.95f));
        bshPm.fillCircle(20, 16, 9);
        bshPm.fillCircle(44, 16, 9);
        bshPm.fillCircle(32, 12, 10);
        bshPm.setColor(new Color(0.85f, 0.95f, 0.35f, 0.85f));
        bshPm.fillCircle(15, 14, 2);
        bshPm.fillCircle(48, 12, 2);
        bshPm.fillCircle(32, 8, 2);
        bushTexture = managedTex(bshPm);

        // 7. Tháp trụ tiền đồn phòng ngự (Tactical Defense Outpost Turret Texture)
        Pixmap turPm = new Pixmap(40, 80, Pixmap.Format.RGBA8888);
        // Đế tháp bằng đá hoa cương kiên cố
        turPm.setColor(new Color(0.20f, 0.22f, 0.28f, 1f));
        turPm.fillRectangle(4, 30, 32, 50);
        turPm.setColor(new Color(0.35f, 0.38f, 0.46f, 1f));
        turPm.drawRectangle(4, 30, 32, 50);
        turPm.fillRectangle(8, 40, 24, 6);
        turPm.fillRectangle(8, 55, 24, 6);
        turPm.fillRectangle(8, 70, 24, 6);
        // Vương miện trụ & Viên ngọc lõi pha lê phát quang
        turPm.setColor(new Color(0.15f, 0.15f, 0.20f, 1f));
        turPm.fillTriangle(4, 30, 36, 30, 20, 14);
        turPm.setColor(new Color(0.2f, 0.85f, 1.0f, 0.5f));
        turPm.fillCircle(20, 12, 11);
        turPm.setColor(new Color(0.85f, 0.95f, 1.0f, 1.0f));
        turPm.fillCircle(20, 12, 6);
        turPm.setColor(Color.WHITE);
        turPm.fillCircle(20, 12, 3);
        turretTexture = managedTex(turPm);

        // 8. Trùm Thần Thú Bạo Chúa (Ancient Titan Boss Texture)
        Pixmap titPm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        // Hào quang dung nham cổ đại
        titPm.setColor(new Color(1.0f, 0.35f, 0.05f, 0.35f));
        titPm.fillCircle(32, 32, 31);
        // Thân thể Thần Thú bằng đá hắc diện thạch
        titPm.setColor(new Color(0.18f, 0.14f, 0.16f, 1.0f));
        titPm.fillCircle(32, 36, 24);
        titPm.fillRectangle(14, 32, 36, 28);
        // Giáp sừng & vảy đá
        titPm.setColor(new Color(0.35f, 0.28f, 0.30f, 1.0f));
        titPm.fillTriangle(14, 18, 24, 30, 10, 34);
        titPm.fillTriangle(50, 18, 40, 30, 54, 34);
        titPm.fillTriangle(32, 8, 24, 24, 40, 24);
        // Khe nứt nham thạch rực cháy trên ngực
        titPm.setColor(new Color(1.0f, 0.45f, 0.05f, 0.95f));
        titPm.drawLine(32, 28, 22, 45);
        titPm.drawLine(32, 28, 42, 45);
        titPm.drawLine(22, 45, 32, 54);
        titPm.drawLine(42, 45, 32, 54);
        titPm.fillCircle(32, 40, 6);
        // Mắt quỷ rực sáng vàng kim
        titPm.setColor(Color.GOLD);
        titPm.fillCircle(26, 24, 4);
        titPm.fillCircle(38, 24, 4);
        titPm.setColor(Color.WHITE);
        titPm.fillCircle(26, 24, 2);
        titPm.fillCircle(38, 24, 2);
        titanBossTexture = managedTex(titPm);

        // 9. Vòng tròn thần thánh hồi máu Hồ Máu (Fountain Healing Sanctum Aura)
        Pixmap fntPm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        for (int r = 31; r >= 4; r -= 4) {
            float alpha = (float)r / 31f * 0.45f;
            fntPm.setColor(new Color(1.0f, 1.0f, 1.0f, alpha));
            fntPm.drawCircle(32, 32, r);
        }
        fntPm.setColor(Color.WHITE);
        fntPm.fillCircle(32, 32, 5);
        fountainAuraTexture = managedTex(fntPm);

        // 10. Lớp phủ điện ảnh góc tối (Cinematic Vignette Texture)
        Pixmap vigPm = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                float dx = (x - 64f) / 64f;
                float dy = (y - 64f) / 64f;
                float dist = (float)Math.sqrt(dx * dx + dy * dy);
                if (dist > 0.55f) {
                    float alpha = Math.min(0.68f, (dist - 0.55f) * 1.5f);
                    vigPm.setColor(new Color(0.02f, 0.02f, 0.05f, alpha));
                    vigPm.drawPixel(x, y);
                }
            }
        }
        vignetteTexture = managedTex(vigPm);
    }

    private void spawnHitSparks(float cx, float cy, int count, Color baseColor) {
        if (sparkTexture == null) return;
        int activeCount = hitSparks.size;
        if (activeCount >= 180) return; // Giới hạn số lượng hạt tia lửa cùng lúc để tránh lag
        int spawnCount = Math.min(count, 180 - activeCount);
        for (int i = 0; i < spawnCount; i++) {
            HitSpark s = (sparkPool.size > 0) ? sparkPool.pop() : new HitSpark();
            s.x = cx + MathUtils.random(-0.15f, 0.15f);
            s.y = cy + MathUtils.random(-0.15f, 0.15f);
            float angle = MathUtils.random(0f, 360f);
            float speed = MathUtils.random(2.5f, 5.5f);
            s.vx = MathUtils.cosDeg(angle) * speed;
            s.vy = MathUtils.sinDeg(angle) * speed + 1.2f;
            s.size = MathUtils.random(0.08f, 0.16f);
            s.maxLife = MathUtils.random(0.18f, 0.32f);
            s.lifeTime = s.maxLife;
            s.color.set(baseColor);
            hitSparks.add(s);
        }
    }

    private void loadAttackTextures() {
        // Load Dark Assassin (P2) Attack Animation Frames (dendanh1..13)
        darkAtkRegionsList.clear();
        texDenDanhList.clear();
        for (int i = 1; i <= 13; i++) {
            String pngName = "dendanh" + i + ".png";
            String jpgName = "dendanh" + i + ".jpg";
            Texture tex = null;
            try {
                if (Gdx.files.internal(pngName).exists()) {
                    tex = GameAssets.sprite(pngName);
                } else if (i == 4 && Gdx.files.internal("danhdanh4.png").exists()) {
                    tex = GameAssets.sprite("danhdanh4.png");
                } else if (i == 6 && Gdx.files.internal("denhdanh6.png").exists()) {
                    tex = GameAssets.sprite("denhdanh6.png");
                } else if (Gdx.files.internal(jpgName).exists()) {
                    tex = GameAssets.sprite(jpgName);
                }
            } catch (Exception ignored) {}

            if (tex != null) {
                texDenDanhList.add(tex);
                darkAtkRegionsList.add(new TextureRegion(tex));
            }
        }

        int frameCount = darkAtkRegionsList.size;
        if (frameCount >= 13) {
            // Nhịp 1 (Frames 1-4: Vung kiếm chém quét ngang): 4 frames @ 0.08s
            Array<TextureRegion> r1 = new Array<>(TextureRegion.class);
            for (int k = 0; k <= 3; k++) r1.add(darkAtkRegionsList.get(k));
            darkAttackAnim1 = new Animation<>(0.08f, r1.toArray(TextureRegion.class));
            darkAttackAnim1.setPlayMode(Animation.PlayMode.NORMAL);

            // Nhịp 2 (Frames 5-8: Bật nhảy xoay kiếm trên không): 4 frames @ 0.08s
            Array<TextureRegion> r2 = new Array<>(TextureRegion.class);
            for (int k = 4; k <= 7; k++) r2.add(darkAtkRegionsList.get(k));
            darkAttackAnim2 = new Animation<>(0.08f, r2.toArray(TextureRegion.class));
            darkAttackAnim2.setPlayMode(Animation.PlayMode.NORMAL);

            // Nhịp 3 (Frames 9-13: Trảm kích xuyên thấu dứt điểm): 5 frames @ 0.08s
            Array<TextureRegion> r3 = new Array<>(TextureRegion.class);
            for (int k = 8; k < frameCount; k++) r3.add(darkAtkRegionsList.get(k));
            darkAttackAnim3 = new Animation<>(0.08f, r3.toArray(TextureRegion.class));
            darkAttackAnim3.setPlayMode(Animation.PlayMode.NORMAL);

            darkAttackAnim = new Animation<>(0.08f, darkAtkRegionsList.toArray(TextureRegion.class));
            darkAttackAnim.setPlayMode(Animation.PlayMode.NORMAL);

            darkDaggerAtk1Anim = new Animation<>(0.055f, new TextureRegion[]{ darkAtkRegionsList.get(0), darkAtkRegionsList.get(1) });
            darkDaggerAtk1Anim.setPlayMode(Animation.PlayMode.NORMAL);
            darkDaggerAtk2Anim = new Animation<>(0.055f, new TextureRegion[]{ darkAtkRegionsList.get(4), darkAtkRegionsList.get(5) });
            darkDaggerAtk2Anim.setPlayMode(Animation.PlayMode.NORMAL);
        } else if (frameCount >= 4) {
            // Bộ 4 Frame: Chia 3 nhịp combo võ thuật rõ ràng
            Array<TextureRegion> r1 = new Array<>(TextureRegion.class);
            r1.add(darkAtkRegionsList.get(0));
            r1.add(darkAtkRegionsList.get(1));
            darkAttackAnim1 = new Animation<>(0.11f, r1.toArray(TextureRegion.class));
            darkAttackAnim1.setPlayMode(Animation.PlayMode.NORMAL);

            Array<TextureRegion> r2 = new Array<>(TextureRegion.class);
            r2.add(darkAtkRegionsList.get(1));
            r2.add(darkAtkRegionsList.get(2));
            darkAttackAnim2 = new Animation<>(0.11f, r2.toArray(TextureRegion.class));
            darkAttackAnim2.setPlayMode(Animation.PlayMode.NORMAL);

            Array<TextureRegion> r3 = new Array<>(TextureRegion.class);
            r3.add(darkAtkRegionsList.get(2));
            r3.add(darkAtkRegionsList.get(3));
            darkAttackAnim3 = new Animation<>(0.13f, r3.toArray(TextureRegion.class));
            darkAttackAnim3.setPlayMode(Animation.PlayMode.NORMAL);

            darkAttackAnim = new Animation<>(0.10f, darkAtkRegionsList.toArray(TextureRegion.class));
            darkAttackAnim.setPlayMode(Animation.PlayMode.NORMAL);

            darkDaggerAtk1Anim = new Animation<>(0.055f, new TextureRegion[]{ darkAtkRegionsList.get(0), darkAtkRegionsList.get(1) });
            darkDaggerAtk1Anim.setPlayMode(Animation.PlayMode.NORMAL);
            darkDaggerAtk2Anim = new Animation<>(0.055f, new TextureRegion[]{ darkAtkRegionsList.get(2), darkAtkRegionsList.get(3) });
            darkDaggerAtk2Anim.setPlayMode(Animation.PlayMode.NORMAL);
        } else if (frameCount > 0) {
            darkAttackAnim = new Animation<>(0.10f, darkAtkRegionsList.toArray(TextureRegion.class));
            darkAttackAnim.setPlayMode(Animation.PlayMode.NORMAL);

            darkDaggerAtk1Anim = darkAttackAnim;
            darkDaggerAtk2Anim = darkAttackAnim;
            darkAttackAnim1 = darkAttackAnim;
            darkAttackAnim2 = darkAttackAnim;
            darkAttackAnim3 = darkAttackAnim;
        }

        // Load White Assassin (P1) & Dark Assassin (P2) Weapon Animations
        try {
            if (Gdx.files.internal("trangchem1.png").exists()) {
                texTrangChem1 = GameAssets.sprite("trangchem1.png");
                if (texTrangChem1 != null) {
                    trangChem1Frame = new TextureRegion(texTrangChem1);
                    hit1Frame = trangChem1Frame;
                    attack1Anim = new Animation<>(0.22f, new TextureRegion[]{ trangChem1Frame });
                    attack1Anim.setPlayMode(Animation.PlayMode.NORMAL);
                }
            }
            if (Gdx.files.internal("trangchem2.png").exists()) {
                texTrangChem2 = GameAssets.sprite("trangchem2.png");
                if (texTrangChem2 != null) {
                    trangChem2Frame = new TextureRegion(texTrangChem2);
                    hit2Frame = trangChem2Frame;
                    attack2Anim = new Animation<>(0.22f, new TextureRegion[]{ trangChem2Frame });
                    attack2Anim.setPlayMode(Animation.PlayMode.NORMAL);
                }
            }
            if (Gdx.files.internal("trangchem3.png").exists()) {
                texTrangChem3 = GameAssets.sprite("trangchem3.png");
                if (texTrangChem3 != null) {
                    trangChem3Frame = new TextureRegion(texTrangChem3);
                    hit3Frame = trangChem3Frame;
                    attack3Anim = new Animation<>(0.26f, new TextureRegion[]{ trangChem3Frame });
                    attack3Anim.setPlayMode(Animation.PlayMode.NORMAL);
                }
            }

            // White Assassin Shotgun (trangsungdung.png, trangsungban1.png, trangsungban2.png)
            if (Gdx.files.internal("trangsungdung.png").exists()) {
                texTrangSungDung = GameAssets.sprite("trangsungdung.png");
                if (texTrangSungDung != null) trangSungDungFrame = new TextureRegion(texTrangSungDung);
            }
            if (Gdx.files.internal("trangsungban1.png").exists()) {
                texTrangSungBan1 = GameAssets.sprite("trangsungban1.png");
                if (texTrangSungBan1 != null) trangSungBan1Frame = new TextureRegion(texTrangSungBan1);
            }
            if (Gdx.files.internal("trangsungban2.png").exists()) {
                texTrangSungBan2 = GameAssets.sprite("trangsungban2.png");
                if (texTrangSungBan2 != null) trangSungBan2Frame = new TextureRegion(texTrangSungBan2);
            }
            if (trangSungBan1Frame != null && trangSungBan2Frame != null) {
                whiteShotgunAtkAnim = new Animation<>(0.15f, new TextureRegion[]{ trangSungBan1Frame, trangSungBan2Frame });
                whiteShotgunAtkAnim.setPlayMode(Animation.PlayMode.NORMAL);
            }

            // White Assassin Bow (trangcungdung.png / trangcundung.png, trangcungban1.png, trangcungban2.png)
            if (Gdx.files.internal("trangcungdung.png").exists()) {
                texTrangCungDung = GameAssets.sprite("trangcungdung.png");
            } else if (Gdx.files.internal("trangcundung.png").exists()) {
                texTrangCungDung = GameAssets.sprite("trangcundung.png");
            }
            if (texTrangCungDung != null) trangCungDungFrame = new TextureRegion(texTrangCungDung);

            if (Gdx.files.internal("trangcungban1.png").exists()) {
                texTrangCungBan1 = GameAssets.sprite("trangcungban1.png");
            } else if (Gdx.files.internal("trangcungban1.jpg").exists()) {
                texTrangCungBan1 = GameAssets.sprite("trangcungban1.jpg");
            }
            if (texTrangCungBan1 != null) trangCungBan1Frame = new TextureRegion(texTrangCungBan1);

            if (Gdx.files.internal("trangcungban2.png").exists()) {
                texTrangCungBan2 = GameAssets.sprite("trangcungban2.png");
                if (texTrangCungBan2 != null) trangCungBan2Frame = new TextureRegion(texTrangCungBan2);
            }
            if (trangCungBan1Frame != null && trangCungBan2Frame != null) {
                whiteBowAtkAnim = new Animation<>(0.15f, new TextureRegion[]{ trangCungBan1Frame, trangCungBan2Frame });
                whiteBowAtkAnim.setPlayMode(Animation.PlayMode.NORMAL);
            }

            // White Assassin Axe (trangriudung.png, trangriubo1.png, trangriubo2.png)
            if (Gdx.files.internal("trangriudung.png").exists()) {
                texTrangRiuDung = GameAssets.sprite("trangriudung.png");
                if (texTrangRiuDung != null) trangRiuDungFrame = new TextureRegion(texTrangRiuDung);
            }
            if (Gdx.files.internal("trangriubo1.png").exists()) {
                texTrangRiuBo1 = GameAssets.sprite("trangriubo1.png");
                if (texTrangRiuBo1 != null) trangRiuBo1Frame = new TextureRegion(texTrangRiuBo1);
            }
            if (Gdx.files.internal("trangriubo2.png").exists()) {
                texTrangRiuBo2 = GameAssets.sprite("trangriubo2.png");
                if (texTrangRiuBo2 != null) trangRiuBo2Frame = new TextureRegion(texTrangRiuBo2);
            }
            if (trangRiuBo1Frame != null && trangRiuBo2Frame != null) {
                whiteAxeAtkAnim = new Animation<>(0.18f, new TextureRegion[]{ trangRiuBo1Frame, trangRiuBo2Frame });
                whiteAxeAtkAnim.setPlayMode(Animation.PlayMode.NORMAL);
            }

            // White Assassin Dual Daggers (songdaodung.png, songdaochem1.png, songdaochem2.png) - tốc độ chém cực nhanh
            if (Gdx.files.internal("songdaodung.png").exists()) {
                texSongDaoDung = GameAssets.sprite("songdaodung.png");
                if (texSongDaoDung != null) songDaoDungFrame = new TextureRegion(texSongDaoDung);
            }
            if (Gdx.files.internal("songdaochem1.png").exists()) {
                texSongDaoChem1 = GameAssets.sprite("songdaochem1.png");
                if (texSongDaoChem1 != null) {
                    songDaoChem1Frame = new TextureRegion(texSongDaoChem1);
                    whiteDaggerAtk1Anim = new Animation<>(0.095f, new TextureRegion[]{ songDaoChem1Frame });
                    whiteDaggerAtk1Anim.setPlayMode(Animation.PlayMode.NORMAL);
                }
            }
            if (Gdx.files.internal("songdaochem2.png").exists()) {
                texSongDaoChem2 = GameAssets.sprite("songdaochem2.png");
                if (texSongDaoChem2 != null) {
                    songDaoChem2Frame = new TextureRegion(texSongDaoChem2);
                    whiteDaggerAtk2Anim = new Animation<>(0.095f, new TextureRegion[]{ songDaoChem2Frame });
                    whiteDaggerAtk2Anim.setPlayMode(Animation.PlayMode.NORMAL);
                }
            }

            // Dark Assassin Shotgun (densungdung.png, densungban1.png, densungban2.png)
            if (Gdx.files.internal("densungdung.png").exists()) {
                texDenSungDung = GameAssets.sprite("densungdung.png");
                if (texDenSungDung != null) denSungDungFrame = new TextureRegion(texDenSungDung);
            }
            if (Gdx.files.internal("densungban1.png").exists()) {
                texDenSungBan1 = GameAssets.sprite("densungban1.png");
                if (texDenSungBan1 != null) denSungBan1Frame = new TextureRegion(texDenSungBan1);
            }
            if (Gdx.files.internal("densungban2.png").exists()) {
                texDenSungBan2 = GameAssets.sprite("densungban2.png");
                if (texDenSungBan2 != null) denSungBan2Frame = new TextureRegion(texDenSungBan2);
            }
            if (denSungBan1Frame != null && denSungBan2Frame != null) {
                darkShotgunAtkAnim = new Animation<>(0.15f, new TextureRegion[]{ denSungBan1Frame, denSungBan2Frame });
                darkShotgunAtkAnim.setPlayMode(Animation.PlayMode.NORMAL);
            }

            // Dark Assassin Axe (denriudung.png, denriubo1.png, denriubo2.png)
            if (Gdx.files.internal("denriudung.png").exists()) {
                texDenRiuDung = GameAssets.sprite("denriudung.png");
                if (texDenRiuDung != null) denRiuDungFrame = new TextureRegion(texDenRiuDung);
            }
            if (Gdx.files.internal("denriubo1.png").exists()) {
                texDenRiuBo1 = GameAssets.sprite("denriubo1.png");
                if (texDenRiuBo1 != null) denRiuBo1Frame = new TextureRegion(texDenRiuBo1);
            }
            if (Gdx.files.internal("denriubo2.png").exists()) {
                texDenRiuBo2 = GameAssets.sprite("denriubo2.png");
                if (texDenRiuBo2 != null) denRiuBo2Frame = new TextureRegion(texDenRiuBo2);
            }
            if (denRiuBo1Frame != null && denRiuBo2Frame != null) {
                darkAxeAtkAnim = new Animation<>(0.18f, new TextureRegion[]{ denRiuBo1Frame, denRiuBo2Frame });
                darkAxeAtkAnim.setPlayMode(Animation.PlayMode.NORMAL);
            }

            // Dark Assassin Bow (cungdung.png, cungduong.png, bancung.png)
            if (Gdx.files.internal("cungdung.png").exists()) {
                texDenCungDung = GameAssets.sprite("cungdung.png");
            } else if (Gdx.files.internal("dencungdung.png").exists()) {
                texDenCungDung = GameAssets.sprite("dencungdung.png");
            }
            if (texDenCungDung != null) denCungDungFrame = new TextureRegion(texDenCungDung);

            if (Gdx.files.internal("cungduong.png").exists()) {
                texDenCungDuong = GameAssets.sprite("cungduong.png");
            } else if (Gdx.files.internal("dencungduong.png").exists()) {
                texDenCungDuong = GameAssets.sprite("dencungduong.png");
            }
            if (texDenCungDuong != null) denCungDuongFrame = new TextureRegion(texDenCungDuong);

            if (Gdx.files.internal("bancung.png").exists()) {
                texDenBanCung = GameAssets.sprite("bancung.png");
            } else if (Gdx.files.internal("denbancung.png").exists()) {
                texDenBanCung = GameAssets.sprite("denbancung.png");
            }
            if (texDenBanCung != null) denBanCungFrame = new TextureRegion(texDenBanCung);

            if (denCungDuongFrame != null && denBanCungFrame != null) {
                darkBowAtkAnim = new Animation<>(0.15f, new TextureRegion[]{ denCungDuongFrame, denBanCungFrame });
                darkBowAtkAnim.setPlayMode(Animation.PlayMode.NORMAL);
            }

            // Dark Assassin Dual Daggers (densongdaodung.png, densongdaochem1.png, densongdaochem2.png)
            if (Gdx.files.internal("densongdaodung.png").exists()) {
                texDenSongDaoDung = GameAssets.sprite("densongdaodung.png");
                if (texDenSongDaoDung != null) denSongDaoDungFrame = new TextureRegion(texDenSongDaoDung);
            }
            if (Gdx.files.internal("densongdaochem1.png").exists()) {
                texDenSongDaoChem1 = GameAssets.sprite("densongdaochem1.png");
                if (texDenSongDaoChem1 != null) {
                    denSongDaoChem1Frame = new TextureRegion(texDenSongDaoChem1);
                    darkDaggerAtk1Anim = new Animation<>(0.095f, new TextureRegion[]{ denSongDaoChem1Frame });
                    darkDaggerAtk1Anim.setPlayMode(Animation.PlayMode.NORMAL);
                }
            }
            if (Gdx.files.internal("densongdaochem2.png").exists()) {
                texDenSongDaoChem2 = GameAssets.sprite("densongdaochem2.png");
                if (texDenSongDaoChem2 != null) {
                    denSongDaoChem2Frame = new TextureRegion(texDenSongDaoChem2);
                    darkDaggerAtk2Anim = new Animation<>(0.095f, new TextureRegion[]{ denSongDaoChem2Frame });
                    darkDaggerAtk2Anim.setPlayMode(Animation.PlayMode.NORMAL);
                }
            }
        } catch (Exception e) {
            System.out.println("Loi tai anh vu khi: " + e.getMessage());
        }

        if (hit1Frame == null && Gdx.files.internal("hit1.png").exists()) {
            hit1Texture = GameAssets.sprite("hit1.png");
            hit1Frame = new TextureRegion(hit1Texture);
            monsterHitFrame = new TextureRegion(hit1Texture); // FIX: separate instance, not shared ref
            attack1Anim = new Animation<>(0.28f, new TextureRegion[]{ hit1Frame });
        }
        if (hit2Frame == null && Gdx.files.internal("hit2.png").exists()) {
            hit2Texture = GameAssets.sprite("hit2.png");
            hit2Frame = new TextureRegion(hit2Texture);
            attack2Anim = new Animation<>(0.28f, new TextureRegion[]{ hit2Frame });
        }
        if (hit3Frame == null && Gdx.files.internal("hit3.png").exists()) {
            hit3Texture = GameAssets.sprite("hit3.png");
            hit3Frame = new TextureRegion(hit3Texture);
            attack3Anim = new Animation<>(0.35f, new TextureRegion[]{ hit3Frame });
        }

        // White Assassin (P1) attack animations & frames fallback using trangdung1
        if (trangDung1Region != null) {
            if (hit1Frame == null) hit1Frame = trangDung1Region;
            if (hit2Frame == null) hit2Frame = trangDung1Region;
            if (hit3Frame == null) hit3Frame = trangDung1Region;
            if (attack1Anim == null) {
                attack1Anim = new Animation<>(0.22f, new TextureRegion[]{ trangDung1Region });
                attack1Anim.setPlayMode(Animation.PlayMode.NORMAL);
            }
            if (attack2Anim == null) {
                attack2Anim = new Animation<>(0.22f, new TextureRegion[]{ trangDung1Region });
                attack2Anim.setPlayMode(Animation.PlayMode.NORMAL);
            }
            if (attack3Anim == null) {
                attack3Anim = new Animation<>(0.28f, new TextureRegion[]{ trangDung1Region });
                attack3Anim.setPlayMode(Animation.PlayMode.NORMAL);
            }
        }
    }

    /** Trả về frame đứng yên của nhân vật trắng theo vũ khí đang cầm. */
    private TextureRegion getWhiteWeaponIdleFrame() {
        switch (currentWeapon) {
            case SHOTGUN:
                if (trangSungDungFrame != null) return trangSungDungFrame;
                break;
            case BOW:
                if (trangCungDungFrame != null) return trangCungDungFrame;
                break;
            case AXE:
                if (trangRiuDungFrame != null) return trangRiuDungFrame;
                break;
            case DUAL_DAGGERS:
                if (songDaoDungFrame != null) return songDaoDungFrame;
                break;
            case SWORD:
            default:
                break;
        }
        return (trangDung1Region != null) ? trangDung1Region : staticIdleFrame;
    }

    /** Trả về frame đứng yên của nhân vật hiện tại (P1 hoặc P2) theo vũ khí đang cầm. */
    private TextureRegion getCharacterWeaponIdleFrame() {
        if ("P2".equals(selectedChar)) {
            if (currentWeapon == WeaponType.SHOTGUN && denSungDungFrame != null) return denSungDungFrame;
            if (currentWeapon == WeaponType.AXE && denRiuDungFrame != null) return denRiuDungFrame;
            if (currentWeapon == WeaponType.BOW && denCungDungFrame != null) return denCungDungFrame;
            if (currentWeapon == WeaponType.DUAL_DAGGERS && denSongDaoDungFrame != null) return denSongDaoDungFrame;
            if (denDung1Region != null) return denDung1Region;
        }
        return getWhiteWeaponIdleFrame();
    }

    /** Trả về animation tấn công tương ứng với nhân vật (P1/P2), vũ khí hiện tại và nhịp combo. */
    private Animation<TextureRegion> getCurrentPlayerAttackAnim() {
        if (isAssassinating) {
            // Đòn ám sát điện ảnh: hiển thị animation cầm kiếm chém thường
            if ("P2".equals(selectedChar)) {
                if (darkAttackAnim1 != null) return darkAttackAnim1;
                if (darkAttackAnim != null) return darkAttackAnim;
            }
            if (attack1Anim != null) return attack1Anim;
        }
        if ("P2".equals(selectedChar)) {
            if (currentWeapon == WeaponType.BOW && darkBowAtkAnim != null) return darkBowAtkAnim;
            if (currentWeapon == WeaponType.SHOTGUN && darkShotgunAtkAnim != null) return darkShotgunAtkAnim;
            if (currentWeapon == WeaponType.AXE && darkAxeAtkAnim != null) return darkAxeAtkAnim;
            if (currentWeapon == WeaponType.DUAL_DAGGERS && darkDaggerAtk1Anim != null) {
                return (attackCombo == 2 && darkDaggerAtk2Anim != null) ? darkDaggerAtk2Anim : darkDaggerAtk1Anim;
            }
            if (darkAtkRegionsList.size > 0) {
                return (attackCombo == 1) ? darkAttackAnim1 : (attackCombo == 2 ? darkAttackAnim2 : darkAttackAnim3);
            }
        }
        switch (currentWeapon) {
            case SHOTGUN:
                if (whiteShotgunAtkAnim != null) return whiteShotgunAtkAnim;
                break;
            case BOW:
                if (whiteBowAtkAnim != null) return whiteBowAtkAnim;
                break;
            case AXE:
                if (whiteAxeAtkAnim != null) return whiteAxeAtkAnim;
                break;
            case DUAL_DAGGERS:
                if (attackCombo == 2 && whiteDaggerAtk2Anim != null) return whiteDaggerAtk2Anim;
                if (whiteDaggerAtk1Anim != null) return whiteDaggerAtk1Anim;
                break;
            case SWORD:
            default:
                break;
        }
        return (attackCombo == 1) ? attack1Anim : (attackCombo == 2 ? attack2Anim : attack3Anim);
    }

    private boolean isRemoteVisibleIn5v5(float rx, float ry, int rTeam, boolean rInBush) {
        if (currentGameMode != GameMode.BATTLE_5V5 || !rInBush) return true;
        int myT = (game != null && game.selectedTeam == 2) ? 2 : 1;
        if (rTeam == myT) return true;
        if (!playerInBush || playerBody == null) return false;
        Vector2 plPos = playerBody.getPosition();
        for (int bi = 0; bi < tacticalBushes.size; bi++) {
            TacticalBush bush = tacticalBushes.get(bi);
            if (bush.contains(plPos) && rx >= bush.x && rx <= bush.x + bush.width
                && ry >= bush.y - 0.5f && ry <= bush.y + bush.height + 0.5f) {
                return true;
            }
        }
        return false;
    }

    private void drawRemote5v5OverheadBar(float rx, float ry, int hp, int maxHp, int shield, int team) {
        float barW = 0.85f, barH = 0.08f;
        float hpPercent = MathUtils.clamp((float) hp / Math.max(1, maxHp), 0f, 1f);
        float barY = ry + 0.85f;
        batch.setColor(0.10f, 0.10f, 0.14f, 0.85f);
        batch.draw(pixel, rx - barW / 2f - 0.02f, barY - 0.02f, barW + 0.04f, barH + 0.04f);
        batch.setColor(0.35f, 0.05f, 0.05f, 0.90f);
        batch.draw(pixel, rx - barW / 2f, barY, barW, barH);
        if (team == 1) batch.setColor(0.20f, 0.70f, 1.0f, 1.0f);
        else batch.setColor(0.95f, 0.25f, 0.25f, 1.0f);
        batch.draw(pixel, rx - barW / 2f, barY, barW * hpPercent, barH);
        if (shield > 0) {
            float sPct = MathUtils.clamp(shield / 650f, 0f, 1f);
            batch.setColor(0.35f, 0.95f, 1.0f, 0.92f);
            batch.draw(pixel, rx - barW / 2f, barY + barH + 0.02f, barW * sPct, 0.045f);
        }
        batch.setColor(Color.WHITE);
    }

    private static boolean isAttackAnimState(String s) {
        return "HIT1".equals(s) || "HIT2".equals(s) || "HIT3".equals(s) || "ATTACK".equals(s);
    }

    /**
     * Chọn đúng frame hoạt ảnh cho đối thủ/đồng đội trực tuyến theo nhân vật (1: Sát thủ trắng, 2: Sát thủ đen),
     * vũ khí đang cầm và trạng thái chuyển động/tấn công.
     */
    private TextureRegion resolveRemoteCharacterFrame(boolean isDark, WeaponType rWeapon, String rState, float rAtkTime) {
        if (rWeapon == null) rWeapon = WeaponType.SWORD;
        if (rState == null) rState = "IDLE";

        TextureRegion stanceFrame;
        if (isDark) {
            if (rWeapon == WeaponType.SHOTGUN && denSungDungFrame != null) stanceFrame = denSungDungFrame;
            else if (rWeapon == WeaponType.AXE && denRiuDungFrame != null) stanceFrame = denRiuDungFrame;
            else if (rWeapon == WeaponType.BOW && denCungDungFrame != null) stanceFrame = denCungDungFrame;
            else if (rWeapon == WeaponType.DUAL_DAGGERS && denSongDaoDungFrame != null) stanceFrame = denSongDaoDungFrame;
            else stanceFrame = (darkIdleAnim != null) ? darkIdleAnim.getKeyFrame(stateTime, true)
                : ((denDung1Region != null) ? denDung1Region : staticIdleFrame);
        } else {
            switch (rWeapon) {
                case SHOTGUN:
                    stanceFrame = (trangSungDungFrame != null) ? trangSungDungFrame
                        : ((whiteIdleAnim != null) ? whiteIdleAnim.getKeyFrame(stateTime, true) : trangDung1Region);
                    break;
                case BOW:
                    stanceFrame = (trangCungDungFrame != null) ? trangCungDungFrame
                        : ((whiteIdleAnim != null) ? whiteIdleAnim.getKeyFrame(stateTime, true) : trangDung1Region);
                    break;
                case AXE:
                    stanceFrame = (trangRiuDungFrame != null) ? trangRiuDungFrame
                        : ((whiteIdleAnim != null) ? whiteIdleAnim.getKeyFrame(stateTime, true) : trangDung1Region);
                    break;
                case DUAL_DAGGERS:
                    stanceFrame = (songDaoDungFrame != null) ? songDaoDungFrame
                        : ((whiteIdleAnim != null) ? whiteIdleAnim.getKeyFrame(stateTime, true) : trangDung1Region);
                    break;
                case SWORD:
                default:
                    stanceFrame = (whiteIdleAnim != null) ? whiteIdleAnim.getKeyFrame(stateTime, true)
                        : ((trangDung1Region != null) ? trangDung1Region : staticIdleFrame);
                    break;
            }
        }
        if (stanceFrame == null) stanceFrame = staticIdleFrame;

        if ("RUN".equals(rState)) {
            if (!isDark) {
                Animation<TextureRegion> rRun = (whiteRunAnim != null) ? whiteRunAnim : playerRunAnim;
                return (rRun != null) ? rRun.getKeyFrame(stateTime, true) : stanceFrame;
            }
            if (darkRunAnim != null) {
                return darkRunAnim.getKeyFrame(stateTime, true);
            }
            if (playerRunSheet != null && playerRunAnim != null) {
                return playerRunAnim.getKeyFrame(stateTime, true);
            }
            return (darkIdleAnim != null) ? darkIdleAnim.getKeyFrame(stateTime * 1.8f, true) : stanceFrame;
        }
        if ("JUMP_UP".equals(rState) || "CLIMB".equals(rState)) {
            if (!isDark) {
                return (whiteJumpUpFrame != null) ? whiteJumpUpFrame : stanceFrame;
            }
            return (denJumpUpFrame != null) ? denJumpUpFrame : stanceFrame;
        }
        if ("JUMP_DOWN".equals(rState) || "EAGLE_STRIKE".equals(rState)) {
            if (!isDark) {
                return (whiteJumpDownFrame != null) ? whiteJumpDownFrame : stanceFrame;
            }
            return (denJumpDownFrame != null) ? denJumpDownFrame : stanceFrame;
        }
        if ("DASH".equals(rState)) {
            if (!isDark) {
                if (dashFrame != null && dashFrame != staticIdleFrame) return dashFrame;
                return (whiteRunAnim != null) ? whiteRunAnim.getKeyFrame(stateTime, true) : stanceFrame;
            }
            return (denDashFrame != null) ? denDashFrame : stanceFrame;
        }
        if ("CROUCH".equals(rState) || "ROLL".equals(rState)) {
            if (isDark) {
                return (denRollFrame != null) ? denRollFrame : stanceFrame;
            }
            return (playerCrouchTexture != null && crouchFrame != null) ? crouchFrame : stanceFrame;
        }
        if (isAttackAnimState(rState)) {
            int combo = "HIT2".equals(rState) ? 2 : ("HIT3".equals(rState) ? 3 : 1);
            if (isDark) {
                if (rWeapon == WeaponType.BOW && darkBowAtkAnim != null) {
                    return darkBowAtkAnim.getKeyFrame(rAtkTime, false);
                }
                if (rWeapon == WeaponType.SHOTGUN && darkShotgunAtkAnim != null) {
                    return darkShotgunAtkAnim.getKeyFrame(rAtkTime, false);
                }
                if (rWeapon == WeaponType.AXE && darkAxeAtkAnim != null) {
                    return darkAxeAtkAnim.getKeyFrame(rAtkTime, false);
                }
                if (rWeapon == WeaponType.DUAL_DAGGERS && darkDaggerAtk1Anim != null) {
                    Animation<TextureRegion> dAnim = (combo == 2 && darkDaggerAtk2Anim != null) ? darkDaggerAtk2Anim : darkDaggerAtk1Anim;
                    return dAnim.getKeyFrame(rAtkTime, false);
                }
                if ("ATTACK".equals(rState) && darkAttackAnim != null) {
                    return darkAttackAnim.getKeyFrame(rAtkTime, false);
                }
                Animation<TextureRegion> sAnim = (combo == 1) ? darkAttackAnim1 : (combo == 2 ? darkAttackAnim2 : darkAttackAnim3);
                return (sAnim != null) ? sAnim.getKeyFrame(rAtkTime, false) : stanceFrame;
            } else {
                switch (rWeapon) {
                    case SHOTGUN:
                        if (whiteShotgunAtkAnim != null) return whiteShotgunAtkAnim.getKeyFrame(rAtkTime, false);
                        break;
                    case BOW:
                        if (whiteBowAtkAnim != null) return whiteBowAtkAnim.getKeyFrame(rAtkTime, false);
                        break;
                    case AXE:
                        if (whiteAxeAtkAnim != null) return whiteAxeAtkAnim.getKeyFrame(rAtkTime, false);
                        break;
                    case DUAL_DAGGERS:
                        if (combo == 2 && whiteDaggerAtk2Anim != null) return whiteDaggerAtk2Anim.getKeyFrame(rAtkTime, false);
                        if (whiteDaggerAtk1Anim != null) return whiteDaggerAtk1Anim.getKeyFrame(rAtkTime, false);
                        break;
                    case SWORD:
                    default:
                        break;
                }
                if (combo == 2) return (trangChem2Frame != null) ? trangChem2Frame : (hit2Frame != null ? hit2Frame : stanceFrame);
                if (combo == 3) return (trangChem3Frame != null) ? trangChem3Frame : (hit3Frame != null ? hit3Frame : stanceFrame);
                return (trangChem1Frame != null) ? trangChem1Frame : (hit1Frame != null ? hit1Frame : stanceFrame);
            }
        }
        return stanceFrame;
    }

    private String getWeaponDisplayName(WeaponType weapon) {
        switch (weapon) {
            case DUAL_DAGGERS: return "Song đao";
            case BOW: return "Cung tên";
            case SHOTGUN: return "Súng săn";
            case AXE: return "Rìu chiến";
            case SWORD:
            default: return "Kiếm thường";
        }
    }

    /** Trang bị vũ khí và reset sạch trạng thái combo để chuyển hoạt ảnh tức thì. */
    private void equipWeapon(WeaponType nextWeapon, boolean fromCrate) {
        currentWeapon = nextWeapon;
        attackCombo = 0;
        attackTime = 0f;
        queuedAttacks = 0;
        hitThisAttack = false;
        daggerComboStep = 0;
        nextSwordCombo = 1;
        swordComboWindowTimer = 0f;
        weaponAttackCooldown = 0f;

        String wName = getWeaponDisplayName(nextWeapon);
        if (lblCurrentWeapon != null) lblCurrentWeapon.setText("Vũ khí: " + wName);
        if (actionLog != null) {
            actionLog.setText(fromCrate
                ? ("Nhặt rương vũ khí: " + wName + " (+8 sát thương)!")
                : ("Đổi vũ khí: " + wName));
        }
    }

    /** Đổi luân phiên qua đủ 5 loại vũ khí để người chơi dùng mọi bộ hoạt ảnh. */
    private void cycleWeapon() {
        if (playerDead || isMatchEnded || inCutscene()) return;
        WeaponType next;
        switch (currentWeapon) {
            case SWORD: next = WeaponType.DUAL_DAGGERS; break;
            case DUAL_DAGGERS: next = WeaponType.BOW; break;
            case BOW: next = WeaponType.SHOTGUN; break;
            case SHOTGUN: next = WeaponType.AXE; break;
            case AXE:
            default: next = WeaponType.SWORD; break;
        }
        equipWeapon(next, false);
        SoundManager.getInstance().playOrbPickup();
    }

    private void createPlatform(float centerX, float topY, float width, float thickness, boolean isGround) {
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(centerX, topY - thickness / 2f);
        Body body = world.createBody(bdef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, thickness / 2f);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.friction = 0.5f;
        body.createFixture(fdef).setUserData("ground");
        shape.dispose();

        platformsList.add(new MapPlatform(centerX - width / 2f, topY - thickness, width, thickness, isGround));
    }

    private void createWall(float centerX, float centerY, float halfWidth, float halfHeight) {
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(centerX, centerY);
        Body body = world.createBody(bdef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfWidth, halfHeight);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.friction = 0.2f;
        body.createFixture(fdef).setUserData("wall");
        shape.dispose();
    }

    private void createObstacleWall(float x, float bottomY, float width, float height) {
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(x + width / 2f, bottomY + height / 2f);
        Body body = world.createBody(bdef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.friction = 0.35f;
        body.createFixture(fdef).setUserData("wall");
        shape.dispose();

        MapWall mw = new MapWall(x, bottomY, width, height);
        mw.body = body;
        wallsList.add(mw);

        // Nóc tường thành có bục đứng vững chãi
        createPlatform(x + width / 2f, bottomY + height, width + 0.2f, 0.22f, false);
    }

    private void addRope(float x, float y) {
        ropeAnchors.add(new Vector2(x, y));
        swingRopes.add(new SwingRope(x, y, ROPE_LENGTH));
    }

    /** Sàn sập chậm. centerX = tâm sàn (giống createPlatform), y = mặt trên. */
    private void createCollapsingPlatform(float centerX, float y, float width, float height) {
        // Bản trước coi tham số đầu là mép TRÁI trong khi mọi lời gọi truyền TÂM sàn
        // -> sàn lệch phải 1,75 m, ở ải 2-3 vực đầu tiên thành 5 m (không nhảy qua được).
        float x = centerX - width / 2f;
        CollapsingPlatform cp = new CollapsingPlatform(x, y, width, height);
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(x + width / 2f, y - height / 2f);
        Body b = world.createBody(bdef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.friction = 0.6f;
        fdef.filter.categoryBits = CAT_WORLD;
        fdef.filter.maskBits = -1;
        b.createFixture(fdef).setUserData("ground");
        shape.dispose();
        cp.body = b;
        collapsingPlatforms.add(cp);
    }

    private void destroyCollapsingPlatformBodies() {
        for (int i = 0; i < collapsingPlatforms.size; i++) {
            CollapsingPlatform cp = collapsingPlatforms.get(i);
            if (cp.body != null && world != null) {
                world.destroyBody(cp.body);
                cp.body = null;
            }
        }
        collapsingPlatforms.clear();
    }

    private void updateCollapsingPlatforms(float delta) {
        if (collapsingPlatforms.size == 0 || inCutscene() || isPaused) return;
        Vector2 pos = (playerBody != null) ? playerBody.getPosition() : null;
        for (int i = 0; i < collapsingPlatforms.size; i++) {
            CollapsingPlatform cp = collapsingPlatforms.get(i);
            if (cp.collapsed) {
                cp.respawnTimer -= delta;
                if (cp.respawnTimer <= 0) {
                    cp.collapsed = false;
                    cp.steppedOn = false;
                    cp.timer = 0f;
                    cp.shakeOffset = 0f;
                    if (cp.body != null) cp.body.setActive(true);
                }
            } else {
                if (cp.steppedOn) {
                    cp.timer += delta;
                    cp.shakeOffset = MathUtils.sin(cp.timer * 45f) * 0.04f;
                    if (cp.timer >= 0.80f) {
                        cp.collapsed = true;
                        cp.respawnTimer = 3.5f;
                        if (cp.body != null) cp.body.setActive(false);
                        SoundManager.getInstance().playHit();
                        spawnHitSparks(cp.x + cp.width / 2f, cp.y, 8, Color.BROWN);
                    }
                } else if (pos != null && isGrounded) {
                    if (pos.x >= cp.x - 0.15f && pos.x <= cp.x + cp.width + 0.15f) {
                        if (Math.abs(pos.y - 0.40f - cp.y) < 0.35f) {
                            cp.steppedOn = true;
                            cp.timer = 0f;
                        }
                    }
                }
            }
        }
    }

    private void updateCheckpoints() {
        if (playerBody == null || checkpoints.size == 0 || playerDead || isMatchEnded) return;
        Vector2 pos = playerBody.getPosition();
        for (int i = 0; i < checkpoints.size; i++) {
            MapCheckpoint cp = checkpoints.get(i);
            if (!cp.activated && Math.abs(pos.x - cp.x) < 1.2f && Math.abs(pos.y - cp.y) < 2.0f) {
                cp.activated = true;
                activeCheckpointX = cp.x;
                activeCheckpointY = cp.y;
                SoundManager.getInstance().playOrbPickup();
                actionLog.setText("ĐÃ LƯU CHECKPOINT AN TOÀN!");
                spawnHitSparks(cp.x, cp.y + 0.5f, 15, Color.GOLD);
                if (story != null && !freePlay) {
                    flushUnbankedSoloGold();
                    StorySaveManager.saveProgress(story.chapter, story.stage,
                        selectedChar, playerHp, 0, score, healthPotions, activeCheckpointX);
                }
            }
        }
    }

    private boolean canStandUp() {
        if (playerBody == null) return true;
        Vector2 pos = playerBody.getPosition();
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (pos.x + 0.18f > p.x && pos.x - 0.18f < p.x + p.width) {
                float platBottom = p.y;
                float platTop = p.y + p.height;
                if (platBottom <= pos.y + 0.65f && platTop >= pos.y + 0.15f) {
                    return false;
                }
            }
        }
        for (int i = 0; i < wallsList.size; i++) {
            MapWall w = wallsList.get(i);
            if (pos.x + 0.18f > w.x && pos.x - 0.18f < w.x + w.width) {
                float wallBottom = w.y;
                float wallTop = w.y + w.height;
                if (wallBottom <= pos.y + 0.65f && wallTop >= pos.y + 0.15f) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isLedgeGrabbing = false;
    private float ledgeGrabX = 0f, ledgeGrabY = 0f;
    private int ledgeGrabFacing = 1;
    private float ledgeHoldTowardTimer = 0f;   // giữ hướng về mép bao lâu -> tự trèo lên (tiện cho cần điều khiển)
    private float ledgeRegrabCooldown = 0f;    // vừa buông tay thì không bám lại ngay mép đó

    private void performLedgeMantle() {
        if (!isLedgeGrabbing || playerBody == null) return;
        float mantleX = ledgeGrabX + ledgeGrabFacing * 0.35f;
        // ledgeGrabY = topY - 0.30f; nửa chiều cao nhân vật là 0.40f -> đặt chân ngay trên topY + 0.02f
        float mantleY = ledgeGrabY + 0.72f;
        playerBody.setTransform(mantleX, mantleY, 0);
        playerBody.setLinearVelocity(ledgeGrabFacing * 1.5f, 2.5f);
        isLedgeGrabbing = false;
        ledgeHoldTowardTimer = 0f;
        ledgeRegrabCooldown = 0.30f;
        jumpCount = 0;
        SoundManager.getInstance().playJump();
        actionLog.setText("BÁM MÉP & TRÈO LÊN THÀNH CÔNG!");
    }

    private void checkLedgeGrab(float delta, boolean kUp, boolean padUp, boolean kJump, boolean movingLeft, boolean movingRight) {
        if (ledgeRegrabCooldown > 0f) ledgeRegrabCooldown -= delta;
        if (playerDead || isAssassinating || isDashing || isSwinging || isWallClinging || isEagleStriking
            || playerStunTimer > 0f || isGrabbedByPirate || isP1UltActive || isP2DashingAssassinate
            || inCutscene() || playerBody == null) {
            isLedgeGrabbing = false;
            ledgeHoldTowardTimer = 0f;
            return;
        }
        Vector2 pos = playerBody.getPosition();
        Vector2 vel = playerBody.getLinearVelocity();

        if (isLedgeGrabbing) {
            playerBody.setLinearVelocity(0, 0);
            playerBody.setTransform(ledgeGrabX, ledgeGrabY, 0);
            playerFacing = ledgeGrabFacing;

            boolean towardLedge = (ledgeGrabFacing == 1) ? movingRight : movingLeft;
            boolean awayFromLedge = (ledgeGrabFacing == 1) ? movingLeft : movingRight;
            ledgeHoldTowardTimer = towardLedge ? ledgeHoldTowardTimer + delta : 0f;

            boolean keysAllowed = !isPaused && !isChatTyping();
            boolean wantsMantle = kUp || padUp || kJump || ledgeHoldTowardTimer >= 0.25f;
            float knobY = (touchpad != null && touchpad.isVisible()) ? touchpad.getKnobPercentY() : 0f;
            boolean wantsDrop = (keysAllowed && (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)))
                || knobY < -0.4f || awayFromLedge;

            if (wantsMantle) {
                performLedgeMantle();
            } else if (wantsDrop) {
                isLedgeGrabbing = false;
                ledgeHoldTowardTimer = 0f;
                ledgeRegrabCooldown = 0.45f;
                jumpCooldownTimer = 0.25f;
                if (awayFromLedge) playerBody.setLinearVelocity(-ledgeGrabFacing * 1.6f, 0f);
                actionLog.setText("BUÔNG TAY KHỎI MÉP!");
            }
            return;
        }

        if (isGrounded || vel.y > 1.2f || ledgeRegrabCooldown > 0f) return;

        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            float topY = p.y + p.height;
            if (Math.abs(pos.x - p.x) < 0.35f && Math.abs((pos.y + 0.3f) - topY) < 0.35f && playerFacing == 1 && movingRight) {
                isLedgeGrabbing = true;
                ledgeGrabX = p.x - 0.22f;
                ledgeGrabY = topY - 0.30f;
                ledgeGrabFacing = 1;
                ledgeHoldTowardTimer = 0f;
                playerBody.setLinearVelocity(0, 0);
                SoundManager.getInstance().playWallJump();
                actionLog.setText("BÁM ĐƯỢC MÉP BỤC! Giữ hướng/LÊN/NHẢY để trèo, đẩy ngược lại để buông.");
                return;
            }
            if (Math.abs(pos.x - (p.x + p.width)) < 0.35f && Math.abs((pos.y + 0.3f) - topY) < 0.35f && playerFacing == -1 && movingLeft) {
                isLedgeGrabbing = true;
                ledgeGrabX = p.x + p.width + 0.22f;
                ledgeGrabY = topY - 0.30f;
                ledgeGrabFacing = -1;
                ledgeHoldTowardTimer = 0f;
                playerBody.setLinearVelocity(0, 0);
                SoundManager.getInstance().playWallJump();
                actionLog.setText("BÁM ĐƯỢC MÉP BỤC! Giữ hướng/LÊN/NHẢY để trèo, đẩy ngược lại để buông.");
                return;
            }
        }
    }

    private void buildStoryStageLayout(int chapter, int stage) {
        destroyCollapsingPlatformBodies();
        checkpoints.clear();
        collapsingPlatforms.clear();

        activeCheckpointX = 3.0f;
        activeCheckpointY = 1.2f;
        cannonPos.set(40.0f, 4.30f);

        if (chapter == 1) {
            // === CHƯƠNG 1: THÀNH CỔ THỜI CHIẾN ===
            if (stage == 1) {
                // Ải 1-1 (mapWidth = 95f): Khởi Binh Ngoại Thành
                createPlatform(9.0f, 0.65f, 18.0f, 0.65f, true); // 0 -> 18m
                createPlatform(20.5f, 1.30f, 2.5f, 0.65f, false); // Thùng hàng 19.25 -> 21.75
                createPlatform(28.0f, 0.65f, 10.0f, 0.65f, true); // Nền đá 23 -> 33m
                createPlatform(35.25f, 2.60f, 6.5f, 0.25f, false); // Ban công 32.0 -> 38.5
                laddersList.add(new Ladder(32.5f, 0.65f, 0.5f, 1.95f));
                createPlatform(42.25f, 3.80f, 8.0f, 0.25f, false); // Mái nhà cao 38.25 -> 46.25
                laddersList.add(new Ladder(38.0f, 2.60f, 0.5f, 1.20f));
                createObstacleWall(48.0f, 0.65f, 1.2f, 2.8f);
                createPlatform(55.0f, 0.65f, 12.0f, 0.65f, true); // Sân giữa 49 -> 61m
                checkpoints.add(new MapCheckpoint(51.0f, 1.0f));
                createPlatform(64.5f, 2.20f, 4.5f, 0.25f, false); // Giàn giáo 62.25 -> 66.75
                createPlatform(71.0f, 1.20f, 5.0f, 0.55f, false); // Thềm thấp 68.5 -> 73.5
                // Sàn kết thúc trải dài tới tận mép mapWidth = 95m (KHÔNG BAO GIỜ CÓ VỰC TRỐNG CUỐI MAP!):
                createPlatform(86.0f, 0.65f, 24.0f, 0.65f, true); // 74 -> 98m
            } else if (stage == 2) {
                // Ải 1-2 (mapWidth = 110f): Mái Ngói Mê Cung (Cứu Tiểu Vũ)
                createPlatform(8.0f, 0.65f, 16.0f, 0.65f, true); // 0 -> 16m
                createPlatform(20.0f, 2.60f, 6.0f, 0.25f, false); // 17 -> 23m
                createPlatform(28.0f, 4.00f, 6.5f, 0.25f, false); // 24.75 -> 31.25m
                createPlatform(36.0f, 2.90f, 5.5f, 0.25f, false); // 33.25 -> 38.75m
                addRope(42.0f, 6.5f); // Đu dây vượt hẻm sâu
                createPlatform(48.0f, 2.80f, 6.0f, 0.25f, false); // Bục đón bên kia hẻm 45 -> 51m
                createPlatform(59.0f, 0.65f, 14.0f, 0.65f, true); // Sân dưới 52 -> 66m
                createPlatform(57.0f, 1.65f, 6.0f, 0.40f, false); // Cổng kẹt chui thấp (cúi người chui qua)
                checkpoints.add(new MapCheckpoint(63.0f, 1.0f));
                createPlatform(69.5f, 2.40f, 9.0f, 0.25f, false); // 65 -> 74m
                laddersList.add(new Ladder(65.5f, 0.65f, 0.5f, 1.75f));
                createPlatform(80.0f, 1.50f, 7.0f, 0.45f, false); // 76.5 -> 83.5m
                // Sàn kết thúc trải dài tới tận mép mapWidth = 110m:
                createPlatform(98.0f, 0.65f, 28.0f, 0.65f, true); // 84 -> 112m
            } else {
                // Ải 1-3 (mapWidth = 120f): Huyết Chiến Sân Thành (Boss: Đội Trưởng Tiền Đồn)
                createPlatform(9.0f, 0.65f, 18.0f, 0.65f, true); // 0 -> 18m
                createObstacleWall(18.0f, 0.65f, 1.2f, 3.2f);
                createPlatform(24.0f, 2.80f, 6.0f, 0.25f, false); // 21 -> 27m
                checkpoints.add(new MapCheckpoint(24.0f, 3.2f));
                createCollapsingPlatform(30.0f, 2.80f, 3.5f, 0.22f); // Sàn sập 1: 28.25 -> 31.75
                createCollapsingPlatform(36.0f, 3.20f, 3.5f, 0.22f); // Sàn sập 2: 34.25 -> 37.75
                createPlatform(42.5f, 2.60f, 5.0f, 0.25f, false); // Bục đón 40 -> 45m
                createPlatform(52.0f, 0.65f, 12.0f, 0.65f, true); // Sân trong 46 -> 58m
                checkpoints.add(new MapCheckpoint(50.0f, 1.0f));
                createPlatform(63.0f, 1.80f, 6.0f, 0.45f, false); // Cầu dẫn 60 -> 66m
                // SÂN THÀNH CỔ ĐẠI CHIẾN BOSS (70m -> 122m liền mạch, trùm xuất hiện giữa sân):
                createPlatform(96.0f, 0.65f, 52.0f, 0.65f, true); // 70 -> 122m
                createPlatform(82.0f, 3.20f, 6.5f, 0.25f, false);
                createPlatform(105.0f, 3.20f, 6.5f, 0.25f, false);
                createPlatform(93.5f, 5.00f, 6.0f, 0.25f, false);
                laddersList.add(new Ladder(91.0f, 0.65f, 0.5f, 4.35f));
                addRope(93.5f, 7.5f);
            }
        } else if (chapter == 2) {
            // === CHƯƠNG 2: BẾN CẢNG BAN ĐÊM ===
            if (stage == 1) {
                // Ải 2-1 (mapWidth = 115f): Sóng Gió Bến Cảng
                createPlatform(8.0f, 0.65f, 16.0f, 0.65f, true); // 0 -> 16m
                createPlatform(22.0f, 0.65f, 8.0f, 0.65f, true); // Cầu cảng gãy 1 (vực nước sâu 2m)
                createPlatform(32.5f, 0.65f, 9.0f, 0.65f, true); // Cầu cảng gãy 2 (vực nước sâu 2.5m)
                addRope(27.0f, 5.5f); // Dây đu cứu hộ qua vùng nước sâu
                checkpoints.add(new MapCheckpoint(35.0f, 1.0f));
                createPlatform(42.0f, 1.50f, 3.0f, 0.75f, false); // Kiện hàng 1
                createPlatform(47.0f, 2.50f, 3.5f, 0.85f, false); // Kiện hàng 2
                createPlatform(56.0f, 3.80f, 9.0f, 0.25f, false); // Sàn kho cảng 51.5 -> 60.5m
                createPlatform(68.0f, 1.60f, 10.0f, 0.45f, true); // Cầu tàu dưới 63 -> 73m
                laddersList.add(new Ladder(63.5f, 1.60f, 0.5f, 2.20f));
                createPlatform(80.0f, 1.00f, 9.0f, 0.55f, true); // Boong tàu cập cảng 75.5 -> 84.5m
                // Sàn kết thúc bến cảng kéo dài tới mép mapWidth = 115m:
                createPlatform(101.0f, 0.65f, 30.0f, 0.65f, true); // 86 -> 116m
            } else if (stage == 2) {
                // Ải 2-2 (mapWidth = 125f): Chiến Thuyền Hắc Ám
                createPlatform(8.0f, 0.65f, 16.0f, 0.65f, true); // 0 -> 16m
                createPlatform(19.0f, 3.80f, 7.5f, 0.25f, false); // Cần trục hàng cảng 15.25 -> 22.75
                laddersList.add(new Ladder(15.5f, 0.65f, 0.5f, 3.15f));
                addRope(26.5f, 7.5f); // Đu dây cáp cần trục đáp thẳng boong tàu!
                createPlatform(37.0f, 1.20f, 15.0f, 0.55f, true); // Boong chiến thuyền 1: 29.5 -> 44.5m
                createPlatform(37.0f, 4.20f, 4.5f, 0.22f, false); // Chòi quan sát trên cột buồm
                createPlatform(49.0f, 0.65f, 12.0f, 0.65f, true); // Sàn khoang hàng 43 -> 55m
                createPlatform(49.0f, 1.65f, 7.0f, 0.40f, false); // Khoang thấp chui né làn tên cung thủ
                checkpoints.add(new MapCheckpoint(53.0f, 1.0f));
                createPlatform(63.0f, 1.40f, 12.0f, 0.45f, true); // Boong chiến thuyền 2: 57 -> 69m
                createPlatform(75.0f, 3.20f, 6.0f, 0.25f, false); // Cầu nối mạn thuyền 72 -> 78m
                addRope(70.5f, 6.5f);
                createPlatform(85.0f, 1.20f, 10.0f, 0.45f, true); // Đuôi thuyền 80 -> 90m
                createPlatform(94.0f, 2.00f, 6.0f, 0.45f, false); // Bờ kè quân cảng 91 -> 97m
                // Sàn kết thúc bến quân cảng kéo dài tới mép mapWidth = 125m:
                createPlatform(112.0f, 0.65f, 28.0f, 0.65f, true); // 98 -> 126m
            } else {
                // Ải 2-3 (mapWidth = 135f): Tử Thủ Quân Cảng (Boss: Thống Chế Thủy Quân)
                createPlatform(8.0f, 0.65f, 16.0f, 0.65f, true); // 0 -> 16m
                createCollapsingPlatform(21.0f, 1.20f, 3.5f, 0.22f); // Sàn sập 1: 19.25 -> 22.75
                createCollapsingPlatform(27.0f, 1.60f, 3.5f, 0.22f); // Sàn sập 2: 25.25 -> 28.75
                createCollapsingPlatform(33.0f, 1.20f, 3.5f, 0.22f); // Sàn sập 3: 31.25 -> 34.75
                createPlatform(41.0f, 0.65f, 10.0f, 0.65f, true); // Trụ cầu vững 36 -> 46m
                checkpoints.add(new MapCheckpoint(41.0f, 1.0f));
                createObstacleWall(48.0f, 0.65f, 1.4f, 3.5f); // Cổng sắt quân cảng
                createPlatform(53.5f, 3.20f, 6.0f, 0.25f, false); // Bục vọng vượt cổng 50.5 -> 56.5
                createPlatform(62.0f, 0.65f, 14.0f, 0.65f, true); // Cầu cảng trong 55 -> 69m
                laddersList.add(new Ladder(55.8f, 0.65f, 0.5f, 2.55f));
                createPlatform(74.0f, 1.50f, 6.0f, 0.35f, false); // Bậc chuyển tiếp 71 -> 77m
                checkpoints.add(new MapCheckpoint(74.0f, 1.8f));
                // ĐẠI SÂN QUÂN CẢNG ĐẤU BOSS (79m -> 137m kiên cố liền mạch, không có vực!):
                createPlatform(108.0f, 0.65f, 58.0f, 0.65f, true); // 79 -> 137m
                createPlatform(95.0f, 3.00f, 7.5f, 0.25f, false);
                createPlatform(120.0f, 3.00f, 7.5f, 0.25f, false);
                createPlatform(107.5f, 5.00f, 7.0f, 0.25f, false);
                laddersList.add(new Ladder(105.0f, 0.65f, 0.5f, 4.35f));
                addRope(107.5f, 7.2f);
            }
        } else {
            // === CHƯƠNG 3: PHÁO ĐÀI TRÊN NÚI ===
            if (stage == 1) {
                // Ải 3-1 (mapWidth = 120f): Đường Lên Tử Địa
                createPlatform(6.0f, 0.65f, 12.0f, 0.65f, true); // 0 -> 12m
                createPlatform(16.0f, 1.50f, 5.0f, 0.85f, true); // Bậc đá dốc 1: 13.5 -> 18.5m
                createPlatform(23.5f, 2.50f, 5.0f, 1.00f, true); // Bậc đá dốc 2: 21 -> 26m
                createPlatform(31.0f, 3.60f, 5.5f, 1.10f, true); // Bậc đá dốc 3: 28.25 -> 33.75m
                createObstacleWall(36.0f, 1.00f, 1.2f, 3.5f); // Tường vỡ chắn đường
                createPlatform(39.5f, 2.00f, 3.2f, 0.35f, false); // Thềm đá chân thang
                createPlatform(42.0f, 4.50f, 7.0f, 0.25f, false); // Ban công pháo đài 38.5 -> 45.5m
                laddersList.add(new Ladder(39.5f, 2.00f, 0.5f, 2.50f));
                checkpoints.add(new MapCheckpoint(43.0f, 4.8f));
                createPlatform(52.0f, 3.20f, 6.5f, 0.25f, false); // Thềm hạ 48.75 -> 55.25m
                createPlatform(62.0f, 2.00f, 6.5f, 0.25f, false); // 58.75 -> 65.25m
                createPlatform(73.0f, 1.00f, 8.0f, 0.55f, true); // Thềm pháo 69 -> 77m
                createPlatform(85.0f, 0.65f, 12.0f, 0.65f, true); // Sân tiền tiêu 79 -> 91m
                // Sàn kết thúc đỉnh núi kéo dài tới mép bản đồ (mapWidth = 125m, bản trước dừng ở 122m -> hố cuối map):
                createPlatform((92.0f + mapWidth + 1.0f) / 2f, 0.65f, mapWidth + 1.0f - 92.0f, 0.65f, true); // 92 -> mapWidth+1
            } else if (stage == 2) {
                // Ải 3-2 (mapWidth = 130f): Tử Chiến Trên Vực Thẳm
                createPlatform(8.0f, 0.65f, 16.0f, 0.65f, true); // 0 -> 16m
                createPlatform(19.0f, 3.00f, 7.5f, 0.25f, false); // Vách thành 15.25 -> 22.75m
                laddersList.add(new Ladder(15.5f, 0.65f, 0.5f, 2.35f));
                addRope(26.5f, 7.2f); // Dây cáp treo vượt vực bão tuyết!
                createPlatform(35.0f, 3.50f, 9.0f, 0.25f, false); // Bờ vực đón 30.5 -> 39.5m
                createPlatform(47.0f, 0.65f, 16.0f, 0.65f, true); // Sân trong 39 -> 55m
                laddersList.add(new Ladder(39.2f, 0.65f, 0.5f, 2.85f));
                createPlatform(47.0f, 1.65f, 6.5f, 0.45f, false); // Chui qua lỗ sập tường đá pháo đài
                checkpoints.add(new MapCheckpoint(52.0f, 1.0f));
                addRope(59.0f, 6.8f); // Dây đu vượt vực 2
                createPlatform(67.0f, 2.40f, 7.0f, 0.25f, false); // 63.5 -> 70.5m
                createPlatform(78.0f, 1.40f, 8.0f, 0.55f, true); // Thềm bậc 74 -> 82m
                createPlatform(89.0f, 2.20f, 8.0f, 0.35f, false); // Cầu đá 85 -> 93m
                // Sàn kết thúc pháo đài kéo dài tới mép bản đồ (mapWidth = 135m, bản trước dừng ở 131m -> vạch đích 131,5m nằm trên hố):
                createPlatform((93.0f + mapWidth + 1.0f) / 2f, 0.65f, mapWidth + 1.0f - 93.0f, 0.65f, true); // 93 -> mapWidth+1
            } else {
                // Ải 3-3 (mapWidth = 145f): Quyết Đấu Đỉnh Núi (Final Boss: Nhiếp Chính Vương Triệu Vô Cực)
                cannonPos.set(54.0f, 4.30f); // Đặt vọng pháo trên sân trung tâm 46..61m, tránh đè Tháp canh 40..46m
                createPlatform(8.0f, 0.65f, 16.0f, 0.65f, true); // 0 -> 16m
                createPlatform(18.0f, 2.40f, 5.0f, 0.25f, false); // Bậc đá 15.5 -> 20.5m
                createCollapsingPlatform(24.0f, 3.00f, 3.2f, 0.22f); // Đá sụp 1: 22.4 -> 25.6m
                createCollapsingPlatform(30.0f, 3.50f, 3.2f, 0.22f); // Đá sụp 2: 28.4 -> 31.6m
                addRope(36.0f, 7.5f); // Đu dây cáp chịu lực vượt vực thẳm
                createPlatform(43.0f, 3.80f, 6.5f, 0.25f, false); // Tháp canh 39.75 -> 46.25m
                createPlatform(53.5f, 1.20f, 15.0f, 0.55f, true); // Sân trung tâm 46 -> 61m
                laddersList.add(new Ladder(46.0f, 1.20f, 0.5f, 2.60f));
                checkpoints.add(new MapCheckpoint(43.0f, 4.0f));
                createObstacleWall(62.0f, 1.20f, 1.4f, 3.8f); // Cổng sắt vương thành
                createPlatform(66.0f, 1.20f, 3.2f, 0.35f, false); // Thềm đá chân thang
                createPlatform(68.0f, 4.00f, 6.5f, 0.25f, false); // Ban công vượt cổng 64.75 -> 71.25m
                laddersList.add(new Ladder(66.0f, 1.20f, 0.5f, 2.80f));
                createPlatform(78.0f, 2.00f, 8.0f, 0.45f, true); // Thềm hạ điện 74 -> 82m
                checkpoints.add(new MapCheckpoint(78.0f, 2.2f));
                createPlatform(88.0f, 1.00f, 7.0f, 0.45f, true); // Cầu ngọc 84.5 -> 91.5m
                // ĐẠI ĐIỆN THƯỢNG TẦNG PHÁO ĐÀI - TRIỆU VÔ CỰC (92.5m -> 147.5m kiên cố tuyệt đối!):
                createPlatform(120.0f, 0.65f, 55.0f, 0.65f, true); // 92.5 -> 147.5m
                createPlatform(107.0f, 3.50f, 8.0f, 0.25f, false);
                createPlatform(133.0f, 3.50f, 8.0f, 0.25f, false);
                createPlatform(120.0f, 5.50f, 7.0f, 0.25f, false);
                laddersList.add(new Ladder(117.5f, 0.65f, 0.5f, 4.85f));
                addRope(120.0f, 7.8f);
            }
        }

        // Finishing observation tower (chân tháp luôn cắm trên nền đất vững chắc)
        float towerX = mapWidth - 4.5f;
        float towerTop = towerTopY();
        createPlatform(towerX, 2.30f, 3.0f, 0.18f, false);
        createPlatform(towerX, 4.20f, 2.8f, 0.18f, false);
        createPlatform(towerX, towerTop, 3.2f, 0.24f, false);
        laddersList.add(new Ladder(towerX - 0.8f, 0.65f, 0.55f, towerTop - 0.65f));

        if (story != null && story.hasFlyingMobs) {
            float cx = snapToGround(cannonPos.x, 1.8f);
            MapPlatform cg = groundAt(cx);
            float baseY = (cg != null) ? (cg.y + cg.height) : 0.65f;
            cannonPos.set(cx, Math.max(4.30f, baseY + 3.1f));
            createPlatform(cannonPos.x, cannonPos.y - 0.1f, 3.5f, 0.20f, false);
            laddersList.add(new Ladder(cannonPos.x - 1.0f, baseY, 0.5f, Math.max(1.5f, cannonPos.y - 0.1f - baseY)));
        }
    }

    private void createWorldObjects() {
        destroyCollapsingPlatformBodies();
        checkpoints.clear();
        platformsList.clear();
        laddersList.clear();
        wallsList.clear();
        isWallClinging = false;
        clingingWall = null;
        ropeAnchors.clear();
        swingRopes.clear();
        currentSwingingRope = null;
        isSwinging = false;

        createWall(-0.1f, mapHeight / 2f, 0.1f, mapHeight / 2f);
        createWall(mapWidth + 0.1f, mapHeight / 2f, 0.1f, mapHeight / 2f);
        createWall(mapWidth / 2f, mapHeight + 0.1f, mapWidth / 2f, 0.1f);

        if (currentGameMode == GameMode.PVP) {
            createPlatform(mapWidth / 2f, 0.65f, mapWidth, 0.65f, true);
            createPlatform(3.6f, 2.10f, 3.2f, 0.18f, false);
            createPlatform(3.6f, 3.80f, 3.0f, 0.18f, false);
            createPlatform(22.0f, 2.10f, 3.2f, 0.18f, false);
            createPlatform(22.0f, 3.80f, 3.0f, 0.18f, false);
            laddersList.add(new Ladder(2.5f, 0.65f, 0.5f, 3.2f));
            laddersList.add(new Ladder(21.0f, 0.65f, 0.5f, 3.2f));
            addRope(12.8f, 5.5f);
        } else if (currentGameMode == GameMode.BATTLE_5V5) {
            // === BẢN ĐỒ ĐẠI CHIẾN ARAM 5V5 (60 MÉT LIỀN MẠCH, THÔNG THOÁNG KHÔNG KẸT) ===
            // 1. Cầu chính xuyên suốt 60m liền khối duy nhất — triệt tiêu hoàn toàn lỗi kẹt mép Box2D
            createPlatform(30.0f, 0.65f, 60.0f, 0.65f, true);

            // 2. Tiền đồn cánh gà (Tier 1 Outposts)
            createPlatform(14.0f, 2.7f, 6.0f, 0.22f, false); // Tiền đồn trái
            createPlatform(46.0f, 2.7f, 6.0f, 0.22f, false); // Tiền đồn phải

            // 3. Tháp trung tâm 2 tầng (Center Contested Tower)
            createPlatform(30.0f, 3.5f, 10.0f, 0.25f, false); // Tầng 1 tháp giữa
            createPlatform(30.0f, 6.0f, 6.0f, 0.22f, false);  // Đỉnh tháp tối cao

            // 4. Thang leo (Ladders)
            laddersList.add(new Ladder(11.0f, 0.65f, 0.55f, 2.05f));
            laddersList.add(new Ladder(25.2f, 0.65f, 0.55f, 2.85f));
            laddersList.add(new Ladder(34.3f, 0.65f, 0.55f, 2.85f));
            laddersList.add(new Ladder(48.5f, 0.65f, 0.55f, 2.05f));
            laddersList.add(new Ladder(27.5f, 3.50f, 0.55f, 2.50f));

            // 5. Dây đu Parkour ở khoảng không thoáng đãng (không vướng sàn/tường)
            addRope(21.0f, 6.2f);
            addRope(39.0f, 6.2f);

            // 6. Bệ phóng nhảy cao Parkour (Jump Pads)
            jumpPads.clear();
            jumpPads.add(new JumpPad(8.5f, 0.65f, 1.2f, 0.25f, PAD_LOW));   // Phóng lên tiền đồn trái
            jumpPads.add(new JumpPad(23.5f, 0.65f, 1.2f, 0.25f, PAD_MID));  // Phóng vào tháp giữa
            jumpPads.add(new JumpPad(30.0f, 3.50f, 1.2f, 0.25f, PAD_LOW));  // Phóng lên đỉnh tháp
            jumpPads.add(new JumpPad(36.5f, 0.65f, 1.2f, 0.25f, PAD_MID));  // Phóng vào tháp từ phải
            jumpPads.add(new JumpPad(51.5f, 0.65f, 1.2f, 0.25f, PAD_LOW));  // Phóng lên tiền đồn phải

            // 7. Vật phẩm cường hóa & Tuyệt kỹ ngẫu nhiên (Contested Power-Up Runes)
            powerUpRunes.clear();
            // Random Ult: 1 trên đỉnh tháp, 1 dưới chân tháp
            powerUpRunes.add(new PowerUpRune(30.0f, 6.4f, PowerUpRune.RuneType.RANDOM_ULTIMATE, 25f));
            powerUpRunes.add(new PowerUpRune(30.0f, 1.1f, PowerUpRune.RuneType.RANDOM_ULTIMATE, 25f));

            // Cuồng Nộ (+50% DMG): 2 tiền đồn
            powerUpRunes.add(new PowerUpRune(14.0f, 3.1f, PowerUpRune.RuneType.BERSERK_DAMAGE, 30f));
            powerUpRunes.add(new PowerUpRune(46.0f, 3.1f, PowerUpRune.RuneType.BERSERK_DAMAGE, 30f));

            // Khiên Hộ Thể (+350 Shield): 2 bên sàn tháp tầng 1
            powerUpRunes.add(new PowerUpRune(26.5f, 3.9f, PowerUpRune.RuneType.DIVINE_SHIELD, 30f));
            powerUpRunes.add(new PowerUpRune(33.5f, 3.9f, PowerUpRune.RuneType.DIVINE_SHIELD, 30f));

            // Thần Dược Cổ Đại (+400 HP): 2 bên dưới đất
            powerUpRunes.add(new PowerUpRune(18.5f, 1.1f, PowerUpRune.RuneType.MEGA_HEAL, 20f));
            powerUpRunes.add(new PowerUpRune(41.5f, 1.1f, PowerUpRune.RuneType.MEGA_HEAL, 20f));

            // Bùa Phong Tốc (+60% Speed): giữa sàn tầng 1
            powerUpRunes.add(new PowerUpRune(30.0f, 3.9f, PowerUpRune.RuneType.WIND_STRIDE, 25f));

            // 8. Bụi rậm chiến thuật (Tactical Ambush Bushes)
            tacticalBushes.clear();
            tacticalBushes.add(new TacticalBush(18.5f, 0.65f, 2.8f, 1.4f)); // Bụi rậm dưới cầu trái
            tacticalBushes.add(new TacticalBush(38.7f, 0.65f, 2.8f, 1.4f)); // Bụi rậm dưới cầu phải

            // 9. Không tháp tiền đồn, không boss đẩy trụ (thuần kỹ năng đối kháng)
            outpostTurrets.clear();
            ancientTitan = null;
        } else if (story != null) {
            buildStoryStageLayout(story.chapter, story.stage);
        } else {
            // ===== Thông số: CO-OP giữ nguyên bản cũ; CHƠI ĐƠN lấy theo chủ đề chương (StoryChapterConfig) =====
            StoryChapterConfig.MapTheme th = (story != null) ? story.theme : null;
            float gapMin = th != null ? th.gapMin : 2.4f, gapMax = th != null ? th.gapMax : 4.0f;
            float platMin = th != null ? th.platMin : 11.0f, platMax = th != null ? th.platMax : 18.0f;
            float bChance = th != null ? th.buildingChance : 0.65f;
            float bMinH = th != null ? th.buildingMinH : 5.2f, bMaxH = th != null ? th.buildingMaxH : 7.5f;
            float midChance = th != null ? th.midPlatChance : 0.7f, ladderChance = th != null ? th.ladderChance : 0.5f;
            float bushChance = th != null ? th.bushChance : 0.6f, ropeGap = th != null ? th.ropeGap : 3.2f;
            int padEvery = th != null ? th.padEvery : 2;
            if (story != null) {
                // Độ khó tăng dần trong chương: ải sau vực rộng hơn một chút (không vượt 4.0 m)
                float k = (story.stage - 1) / (float) (StoryChapterConfig.STAGES_PER_CHAPTER - 1);
                gapMin = Math.min(3.6f, gapMin + 0.2f * k);
                gapMax = Math.min(4.0f, gapMax + 0.3f * k);
                if (story.chapter == 3 && story.stage == 3) { // Hố Xương Trắng: vực sâu, nhiều dây & thang
                    gapMin = 3.0f; gapMax = 4.0f; ropeGap = 2.9f; ladderChance = 1f; midChance = 1f;
                }
                if (story.chapter == 5 && story.stage == 3) { // Vọng Lâu: nhà cao, nhiều thang
                    bChance = 0.9f; bMinH = 6.5f; bMaxH = 8.2f; ladderChance = 0.9f;
                }
            }
            boolean bossStage = (story == null) || story.hasBoss;
            float arenaLen = bossStage ? 40.0f : 18.0f;

            createPlatform(10.0f, 0.65f, 20.0f, 0.65f, true);

            float currentX = 20.0f;
            int numGaps = (story != null) ? 40 : 4 + currentLevel * 2; // SOLO: lấp đầy tới quảng trường cuối
            int numRopes = (story != null) ? 99 : 2 + currentLevel;
            float bossArenaStartX = mapWidth - arenaLen;

            for (int i = 0; i < numGaps; i++) {
                float gapSize = MathUtils.random(gapMin, gapMax);
                float platformWidth = MathUtils.random(platMin, platMax);

                if (currentX + gapSize + platformWidth >= bossArenaStartX - 4.0f) break;

                // Dây đu ở độ cao 4.3 m (nhảy lên là bắt được)
                if (numRopes > 0 && gapSize >= ropeGap) {
                    addRope(currentX + gapSize * 0.50f, 4.3f);
                    numRopes--;
                }

                currentX += gapSize + platformWidth / 2f;
                createPlatform(currentX, 0.65f, platformWidth, 0.65f, true);

                if (padEvery > 0 && i % padEvery == padEvery - 1) {
                    jumpPads.add(new JumpPad(currentX - platformWidth / 3f, 0.65f, 1.2f, 0.25f, PAD_HIGH));
                }

                if (MathUtils.randomBoolean(bushChance)) {
                    tacticalBushes.add(new TacticalBush(currentX + platformWidth / 4f, 0.65f, 2.4f, 1.3f));
                }

                // Nhà cao để leo
                if (MathUtils.randomBoolean(bChance)) {
                    float bH = MathUtils.random(bMinH, bMaxH);
                    float bW = MathUtils.random(2.4f, 3.4f);
                    float bX = currentX + MathUtils.random(-2.0f, 2.0f);
                    createPlatform(bX, bH, bW, 0.28f, false);

                    if (MathUtils.randomBoolean(0.5f)) {
                        powerUpRunes.add(new PowerUpRune(bX, bH + 0.45f,
                            MathUtils.randomBoolean() ? PowerUpRune.RuneType.MEGA_HEAL : PowerUpRune.RuneType.BERSERK_DAMAGE, 35f));
                    }
                }

                // Tường thành & vật cản nhìn thấy bằng mắt (Visible Obstacle Walls & Pillars)
                if (MathUtils.randomBoolean(0.70f) && platformWidth >= 8.5f) {
                    float obsW = MathUtils.random(0.9f, 1.4f);
                    float obsH = MathUtils.random(2.5f, 4.6f);
                    float obsX = currentX - platformWidth / 3.2f + MathUtils.random(-0.5f, 0.5f);
                    createObstacleWall(obsX, 0.65f, obsW, obsH);
                }

                // Bục giữa & thang
                if (MathUtils.randomBoolean(midChance)) {
                    float platY = MathUtils.random(2.2f, 4.0f);
                    createPlatform(currentX, platY, 3.8f, 0.18f, false);
                    if (MathUtils.randomBoolean(ladderChance)) {
                        laddersList.add(new Ladder(currentX - 1.2f, 0.65f, 0.5f, platY - 0.65f));
                    }
                }

                currentX += platformWidth / 2f;
            }

            // === Cầu nối an toàn (vực 2.0 m) tới quảng trường cuối (KHÔNG BAO GIỜ BỊ HỔNG) ===
            while (bossArenaStartX - currentX > 2.5f) {
                float gap = 2.0f;
                float remainingDist = bossArenaStartX - (currentX + gap);
                if (remainingDist <= 0.5f) break;
                float segWidth = Math.min(remainingDist - 2.0f, 10.0f);
                if (segWidth < 2.5f) {
                    segWidth = Math.max(1.5f, remainingDist - 2.0f);
                    if (segWidth <= 1.0f) segWidth = remainingDist;
                }
                float segCenterX = currentX + gap + segWidth / 2f;
                createPlatform(segCenterX, 0.65f, segWidth, 0.65f, true);
                if (segWidth >= 4.0f) {
                    jumpPads.add(new JumpPad(segCenterX, 0.65f, 1.2f, 0.25f, PAD_HIGH));
                }
                currentX += gap + segWidth;
            }
            if (bossArenaStartX - currentX > 2.2f) {
                float segW = Math.max(2.0f, bossArenaStartX - currentX - 1.8f);
                createPlatform(currentX + 1.8f + segW / 2f, 0.65f, segW, 0.65f, true);
                currentX += 1.8f + segW;
            }

            jumpPads.add(new JumpPad(Math.max(20.0f, currentX - 1.2f), 0.65f, 1.2f, 0.25f, PAD_HIGH));
            if (bossArenaStartX - currentX >= 1.5f) {
                addRope(currentX + (bossArenaStartX - currentX) * 0.5f, 4.3f);
            }

            if (bossStage) {
                // === ĐẤU TRƯỜNG TRÙM (38 m): sàn chính + bục 2 tầng + bục cao trung tâm + dây đu ===
                createPlatform(mapWidth - 20.0f, 0.65f, 40.0f, 0.65f, true);
                jumpPads.add(new JumpPad(mapWidth - 36.5f, 0.65f, 1.2f, 0.25f, PAD_HIGH));
                tacticalBushes.add(new TacticalBush(mapWidth - 33.0f, 0.65f, 2.8f, 1.35f));
                tacticalBushes.add(new TacticalBush(mapWidth - 12.0f, 0.65f, 2.8f, 1.35f));
                createPlatform(mapWidth - 30.0f, 2.6f, 6.0f, 0.20f, false);
                createPlatform(mapWidth - 15.0f, 2.6f, 6.0f, 0.20f, false);
                laddersList.add(new Ladder(mapWidth - 32.2f, 0.65f, 0.5f, 1.95f));
                laddersList.add(new Ladder(mapWidth - 13.0f, 0.65f, 0.5f, 1.95f));
                createPlatform(mapWidth - 22.5f, 4.6f, 7.5f, 0.22f, false);
                laddersList.add(new Ladder(mapWidth - 25.5f, 2.6f, 0.5f, 2.0f));
                addRope(mapWidth - 22.5f, 6.8f);
                powerUpRunes.add(new PowerUpRune(mapWidth - 22.5f, 5.0f, PowerUpRune.RuneType.DIVINE_SHIELD, 30f));
            } else {
                // === QUẢNG TRƯỜNG CUỐI ải thường: sàn 18 m + 1 bục nghỉ có bình máu ===
                createPlatform(mapWidth - 9.0f, 0.65f, 18.0f, 0.65f, true);
                createPlatform(mapWidth - 11.0f, 2.6f, 4.0f, 0.20f, false);
                laddersList.add(new Ladder(mapWidth - 13.2f, 0.65f, 0.5f, 1.95f));
                powerUpRunes.add(new PowerUpRune(mapWidth - 11.0f, 3.0f, PowerUpRune.RuneType.MEGA_HEAL, 40f));
            }

            // Vọng đài đặt pháo (khi ải có lính bay; CO-OP từ ải 2)
            if ((story != null) ? story.hasFlyingMobs : currentLevel >= 2) {
                float cx = snapToGround(40.0f, 1.8f);
                MapPlatform cg = groundAt(cx);
                float baseY = (cg != null) ? (cg.y + cg.height) : 0.65f;
                cannonPos.set(cx, Math.max(4.30f, baseY + 3.1f));
                createPlatform(cannonPos.x, cannonPos.y - 0.1f, 3.5f, 0.20f, false);
                laddersList.add(new Ladder(cannonPos.x - 1.0f, baseY, 0.5f, Math.max(1.5f, cannonPos.y - 0.1f - baseY)));
            }

            // Tháp quan sát ở điểm kết thúc
            float towerX = mapWidth - 4.5f;
            float towerTop = towerTopY();
            createPlatform(towerX, 2.30f, 3.0f, 0.18f, false);
            createPlatform(towerX, 4.20f, 2.8f, 0.18f, false);
            createPlatform(towerX, 6.00f, 2.6f, 0.18f, false);
            createPlatform(towerX, towerTop, 3.2f, 0.24f, false);
            laddersList.add(new Ladder(towerX - 0.8f, 0.65f, 0.55f, towerTop - 0.65f));

            if (currentGameMode == GameMode.COOP) {
                hasSage = true;
                sageX = towerX;
                sageY = towerTop + 0.30f;
            }
        }
    }

    private void createPlayer() {
        BodyDef bdef = new BodyDef();

        // FIX: null-safe NetworkManager access
        int myIndex = 1;
        try {
            if (NetworkManager.getInstance() != null)
                myIndex = NetworkManager.getInstance().myPlayerIndex;
        } catch (Exception ignored) {}

        if (currentGameMode == GameMode.PVP) {
            if (myIndex == 2) {
                bdef.position.set(21.5f, 1.2f);
                playerFacing = -1;
                remoteX = 3.5f; remoteY = 1.2f; remoteFacing = 1;
            } else {
                bdef.position.set(3.5f, 1.2f);
                playerFacing = 1;
                remoteX = 21.5f; remoteY = 1.2f; remoteFacing = -1;
            }
        } else if (currentGameMode == GameMode.BATTLE_5V5) {
            int pTeam = (game != null) ? game.selectedTeam : 1;
            if (pTeam == 1) {
                bdef.position.set(4.5f, 1.2f);
                playerFacing = 1;
            } else {
                bdef.position.set(55.5f, 1.2f);
                playerFacing = -1;
            }
            remoteX = -999f;
            remoteY = -999f;
            targetRemoteX = -999f;
            targetRemoteY = -999f;
        } else if (currentGameMode == GameMode.COOP) {
            bdef.position.set(myIndex == 2 ? 4.5f : 2.5f, 1.2f);
            playerFacing = 1;
            remoteX = (myIndex == 2) ? 2.5f : 4.5f;
            remoteY = 1.2f;
            remoteFacing = 1;
        } else {
            bdef.position.set(2.5f, 1.2f);
            playerFacing = 1;
            remoteX = -999f;
            remoteY = -999f;
        }

        bdef.type = BodyDef.BodyType.DynamicBody;
        // Chế độ không có checkpoint (1v1, hợp tác, 5v5): rơi xuống nước ở bản đồ Bến Cảng thì về đúng điểm xuất phát của mình
        if (currentGameMode != GameMode.SOLO) {
            activeCheckpointX = bdef.position.x;
            activeCheckpointY = bdef.position.y;
        }
        playerBody = world.createBody(bdef);
        playerBody.setFixedRotation(true);
        updatePlayerHitbox(false);

        // FIX: "playerFoot" instead of "foot" — avoids cross-counting with monsters
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.18f, 0.05f, new Vector2(0, -0.4f), 0);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.isSensor = true;
        fdef.filter.categoryBits = CAT_PLAYER;
        playerBody.createFixture(fdef).setUserData("playerFoot");
        shape.dispose();
    }

    private void updatePlayerHitbox(boolean crouch) {
        if (playerBody == null) return;
        Array<Fixture> fixtures = playerBody.getFixtureList();
        for (int i = 0; i < fixtures.size; i++) {
            Fixture f = fixtures.get(i);
            if ("player".equals(f.getUserData())) {
                playerBody.destroyFixture(f);
                break;
            }
        }
        PolygonShape shape = new PolygonShape();
        if (crouch) shape.setAsBox(0.20f, 0.25f, new Vector2(0, -0.15f), 0);
        else shape.setAsBox(0.20f, 0.40f);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 1.0f;
        fdef.friction = 0.3f;
        fdef.filter.categoryBits = CAT_PLAYER;
        playerBody.createFixture(fdef).setUserData("player");
        shape.dispose();
    }

    private boolean isPlayerCoveredUnderPlank() {
        if (playerBody == null) return false;
        Vector2 pPos = playerBody.getPosition();
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (!p.isGround && pPos.x >= p.x && pPos.x <= p.x + p.width && pPos.y < p.y) {
                if (p.y - pPos.y < 3.0f) return true;
            }
        }
        return false;
    }

    // FIX: dynamic tower coords based on mapWidth/level
    /** Độ cao đỉnh tháp cuối map. (Bản cũ = 7.8 + ải*0.3 -> với 30 ải sẽ vượt khỏi bản đồ.) */
    private float towerTopY() {
        int tier = (story != null) ? Math.min(story.chapter, 5) : Math.min(currentLevel, 5);
        return 7.80f + tier * 0.3f;
    }

    private boolean isPlayerOnTowerSummit() {
        if (playerBody == null) return false;
        Vector2 pos = playerBody.getPosition();
        float towerX = mapWidth - 4.5f;
        float towerTopY = towerTopY();
        return (pos.y >= towerTopY - 0.5f && Math.abs(pos.x - towerX) <= 1.8f && isGrounded);
    }

    private void initMapMonsters() {
        if (story != null) {
            // Quái rải đều trên đường đi (tránh 15 m đầu & quảng trường cuối); trùm đứng giữa đấu trường
            int n = story.groundMobCount;
            float endX = mapWidth - (story.hasBoss ? 40f : 20f);
            if (n > 0) {
                float spacing = (endX - 15f) / n;
                for (int i = 0; i < n; i++) spawnGroundMob(false, 15.0f + spacing * (i + 0.5f));
            }
            if (story.hasBoss) spawnGroundMob(true, mapWidth - 22.5f);
            if (story.startWithShield) {
                playerShield = 350; // Hộ Tâm Kính của Mặc lão
            }
            if (story.boyMode == StoryChapterConfig.BoyMode.ESCORT) spawnEscortBoy();
            return;
        }
        int numMonsters = 2 + currentLevel * 2;
        if (currentGameMode == GameMode.COOP) {
            numMonsters += 3; // Thêm nhiều quái thử thách trong CO-OP
        }
        float spacing = (mapWidth - 45f) / numMonsters;
        for (int i = 0; i < numMonsters; i++) {
            spawnGroundMob(false, 15.0f + (i * spacing));
        }
        // Trùm xuất hiện chính giữa đấu trường đại chiến rộng lớn 38m
        spawnGroundMob(true, mapWidth - 22.5f);
        if (currentGameMode == GameMode.COOP) {
            // Thêm 1 Thủ Lĩnh / Trung Tướng tinh anh bảo vệ cứ điểm
            spawnGroundMob(true, mapWidth - 15.0f);
        }
    }

    /** Sàn mặt đất (isGround) chứa tọa độ x, hoặc null nếu x nằm trên vực. */
    private MapPlatform groundAt(float x) {
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (p.isGround && x >= p.x && x <= p.x + p.width) return p;
        }
        return null;
    }

    /** Dời x về sàn mặt đất gần nhất (tránh sinh quái lơ lửng trên vực rồi rơi chết). */
    private float snapToGround(float x, float margin) {
        MapPlatform best = groundAt(x);
        if (best == null) {
            float bestDist = Float.MAX_VALUE;
            for (int i = 0; i < platformsList.size; i++) {
                MapPlatform p = platformsList.get(i);
                if (!p.isGround) continue;
                float d = Math.min(Math.abs(x - p.x), Math.abs(x - (p.x + p.width)));
                if (d < bestDist) { bestDist = d; best = p; }
            }
        }
        if (best == null) return x;
        float lo = best.x + Math.min(margin, best.width / 2f), hi = best.x + best.width - Math.min(margin, best.width / 2f);
        return MathUtils.clamp(x, lo, hi);
    }

    /** Kiểm tra tại tọa độ x có trần hầm chui thấp (< 1.05m so với mặt sàn groundTopY) hay không. */
    private boolean hasLowCeilingAt(float x, float groundTopY) {
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (p.isGround) continue;
            if (x >= p.x - 0.45f && x <= p.x + p.width + 0.45f) {
                float gap = p.y - groundTopY;
                if (gap >= 0.05f && gap < 1.05f) return true;
            }
        }
        return false;
    }

    /** Dời x về sàn mặt đất gần nhất và tránh nằm kẹt trong hầm chui thấp. */
    private float snapToOpenGround(float x, float margin) {
        float sx = snapToGround(x, margin);
        MapPlatform home = groundAt(sx);
        if (home == null) return sx;
        float topY = home.y + home.height;
        if (!hasLowCeilingAt(sx, topY)) return sx;
        float lo = home.x + Math.min(margin, home.width / 2f);
        float hi = home.x + home.width - Math.min(margin, home.width / 2f);
        for (float d = 0.6f; d <= home.width; d += 0.6f) {
            float rx = sx + d;
            if (rx <= hi && !hasLowCeilingAt(rx, topY)) return rx;
            float lx = sx - d;
            if (lx >= lo && !hasLowCeilingAt(lx, topY)) return lx;
        }
        return sx;
    }

    private void spawnGroundMob(boolean isBoss, float spawnX) {
        spawnX = snapToOpenGround(spawnX, isBoss ? 1.2f : 0.8f);
        GroundMob m = new GroundMob();
        m.id = nextMobId++;
        m.isBoss = isBoss;
        if (currentGameMode == GameMode.COOP) {
            m.maxHp = isBoss ? 1600 : 380; // Quái và Boss CO-OP trâu hơn để 2 người phối hợp
        } else if (story != null) {
            // Chơi đơn: máu quái tăng theo chương (220 -> 370), trùm theo cấu hình từng ải
            m.maxHp = isBoss ? story.bossMaxHp : 190 + story.chapter * 30;
        } else {
            m.maxHp = isBoss ? 1200 : 250;
        }
        m.hp = m.maxHp;
        m.scale = isBoss ? 1.75f : 1.0f;
        m.patrolLeft = Math.max(1.0f, isBoss ? (spawnX - 12.0f) : (spawnX - 4.5f));
        m.patrolRight = Math.min(mapWidth - 1.0f, isBoss ? (spawnX + 12.0f) : (spawnX + 4.5f));
        // Giới hạn vùng tuần tra trong sàn đang đứng & tránh đi vào hầm chui thấp (quái đứng cao 0.8m sẽ kẹt cứng)
        MapPlatform home = groundAt(spawnX);
        float homeTopY = 0.65f;
        if (home != null) {
            homeTopY = home.y + home.height;
            m.patrolLeft = Math.max(m.patrolLeft, home.x + 0.6f);
            m.patrolRight = Math.min(m.patrolRight, home.x + home.width - 0.6f);
            for (int pi = 0; pi < platformsList.size; pi++) {
                MapPlatform p = platformsList.get(pi);
                if (p.isGround) continue;
                float gap = p.y - homeTopY;
                if (gap >= 0.05f && gap < 1.05f) {
                    if (spawnX >= p.x + p.width) {
                        m.patrolLeft = Math.max(m.patrolLeft, p.x + p.width + 0.55f);
                    } else if (spawnX <= p.x) {
                        m.patrolRight = Math.min(m.patrolRight, p.x - 0.55f);
                    }
                }
            }
            if (m.patrolLeft > m.patrolRight) {
                m.patrolLeft = spawnX;
                m.patrolRight = spawnX;
            }
        }

        float bw = isBoss ? 0.38f : 0.2f, bh = isBoss ? 0.70f : 0.4f;
        BodyDef bdef = new BodyDef();
        // Tính độ cao Y theo chính sàn home (bản cũ cứng y=1.2f khiến quái rơi dưới sàn nâng cao ở ải 2-1, 3-1, 3-3)
        bdef.position.set(spawnX, homeTopY + bh + 0.08f);
        bdef.type = BodyDef.BodyType.DynamicBody;
        m.body = world.createBody(bdef);
        m.body.setFixedRotation(true);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(bw, bh);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = isBoss ? 3.0f : 1.0f;
        fdef.friction = 0.3f;
        m.body.createFixture(fdef).setUserData("monster");

        // FIX: "monsterFoot" to avoid affecting player's footContacts
        PolygonShape footShape = new PolygonShape();
        footShape.setAsBox(bw * 0.9f, 0.05f, new Vector2(0, -bh), 0);
        FixtureDef footDef = new FixtureDef();
        footDef.shape = footShape;
        footDef.isSensor = true;
        m.body.createFixture(footDef).setUserData("monsterFoot");

        shape.dispose();
        footShape.dispose();
        activeMonsters.add(m);
    }

    private void cleanSocketListeners() {
        if (socket == null || !networkListenersRegistered) return;
        networkListenersRegistered = false;
        socket.off("enemyMove");
        socket.off("enemyAction");
        socket.off("playerLeft");
        socket.off("rematchReady");
    }

    private RemotePeer5v5 findExtraPeerById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (int i = 0; i < extraPeers5v5.size; i++) {
            RemotePeer5v5 p = extraPeers5v5.get(i);
            if (id.equals(p.id)) return p;
        }
        return null;
    }

    private void handleLocalPlayerDeath5v5(String atkName, int atkTeam) {
        playerDead = true;
        closeShop();
        resetTraversalState();
        if (playerBody != null) {
            playerDeathX = playerBody.getPosition().x;
            playerDeathY = playerBody.getPosition().y;
            playerDeathBlinkTimer = 0.85f;
        }
        sendSyncPacket("IDLE");
        playerRespawnTimer5v5 = 5.0f;
        int pTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;
        String safeAtk = (atkName != null && !atkName.isEmpty()) ? atkName : remoteName;
        int safeTeam = (atkTeam == 1 || atkTeam == 2) ? atkTeam : (pTeam == 1 ? 2 : 1);
        String myName = (game != null && game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần";
        on5v5Kill(safeAtk, safeTeam, myName, pTeam);
        broadcast5v5Kill(safeAtk, safeTeam, myName, pTeam);
    }

    private void setupNetworkListeners() {
        if (socket == null || currentGameMode == GameMode.SOLO) return;
        cleanSocketListeners();
        networkListenersRegistered = true;

        socket.on("enemyMove", args -> Gdx.app.postRunnable(() -> {
            if (isDisposed || isHidden || (game != null && game.getScreen() != this)) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String senderId = data.optString("senderId", "");
                if (!senderId.isEmpty() && senderId.equals(myNetId)) return;
                String senderSockId = data.optString("socketId", "");

                float nx = (float) data.getDouble("x");
                float ny = (float) data.getDouble("y");
                int nFacing = data.getInt("facing");
                String nAnim = data.optString("animState", "IDLE");
                int myTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;

                // Hỗ trợ nhiều người chơi trong phòng 5v5 (2v2, 3v3, 5v5):
                // ưu tiên gắn đối thủ khác phe vào primaryRemote để khóa mục tiêu cung & kỹ năng hoạt động tự động.
                if (currentGameMode == GameMode.BATTLE_5V5 && !senderId.isEmpty()) {
                    int pktTeam = (data.optInt("team", remoteTeam) == 2) ? 2 : 1;
                    if (primaryRemoteId.isEmpty()) {
                        primaryRemoteId = senderId;
                        primaryRemoteSocketId = senderSockId;
                        remoteX = -999f;
                        remoteDead = false;
                        remoteDeathBlinkTimer = 0f;
                        remoteRespawnTimer = 0f;
                        remoteHitFlashTimer = 0f;
                    } else if (!senderId.equals(primaryRemoteId)) {
                        if (remoteTeam == myTeam && pktTeam != myTeam) {
                            RemotePeer5v5 allyCopy = getOrCreateExtraPeer(primaryRemoteId);
                            allyCopy.socketId = primaryRemoteSocketId;
                            allyCopy.name = remoteName;
                            allyCopy.team = remoteTeam;
                            allyCopy.charId = remoteCharId;
                            allyCopy.weapon = remoteWeapon;
                            allyCopy.x = remoteX;
                            allyCopy.y = remoteY;
                            allyCopy.targetX = targetRemoteX;
                            allyCopy.targetY = targetRemoteY;
                            allyCopy.facing = remoteFacing;
                            allyCopy.animState = remoteAnimState;
                            allyCopy.attackAnimTime = remoteAttackAnimTime;
                            allyCopy.hp = remoteHp;
                            allyCopy.shield = remoteShield;
                            allyCopy.dead = remoteDead;
                            allyCopy.inBush = remoteInBush;
                            allyCopy.stealth = remoteStealth;
                            allyCopy.hitFlashTimer = remoteHitFlashTimer;
                            allyCopy.deathX = remoteDeathX;
                            allyCopy.deathY = remoteDeathY;
                            allyCopy.deathBlinkTimer = remoteDeathBlinkTimer;
                            allyCopy.respawnTimer = remoteRespawnTimer;

                            // Khôi phục trạng thái của đối thủ vừa thăng lên primaryRemote (tránh dính vị trí/dead của đồng đội cũ)
                            RemotePeer5v5 existing = findExtraPeerById(senderId);
                            primaryRemoteId = senderId;
                            primaryRemoteSocketId = senderSockId;
                            if (existing != null) {
                                remoteX = existing.x;
                                remoteY = existing.y;
                                targetRemoteX = existing.targetX;
                                targetRemoteY = existing.targetY;
                                remoteDead = existing.dead;
                                remoteHitFlashTimer = existing.hitFlashTimer;
                                remoteDeathX = existing.deathX;
                                remoteDeathY = existing.deathY;
                                remoteDeathBlinkTimer = existing.deathBlinkTimer;
                                remoteRespawnTimer = existing.respawnTimer;
                                removeExtraPeerById(senderId);
                            } else {
                                remoteX = -999f;
                                remoteY = -999f;
                                remoteDead = false;
                                remoteHitFlashTimer = 0f;
                                remoteDeathBlinkTimer = 0f;
                                remoteRespawnTimer = 0f;
                            }
                        } else {
                            RemotePeer5v5 peer = getOrCreateExtraPeer(senderId);
                            if (!senderSockId.isEmpty()) peer.socketId = senderSockId;
                            boolean wasPeerDead = peer.dead;
                            peer.targetX = nx;
                            peer.targetY = ny;
                            if (peer.x == -999f || Vector2.dst(peer.x, peer.y, nx, ny) > 10f) {
                                peer.x = nx;
                                peer.y = ny;
                            }
                            peer.facing = nFacing;
                            if (!nAnim.equals(peer.animState) && isAttackAnimState(nAnim)) {
                                peer.attackAnimTime = 0f;
                            }
                            peer.animState = nAnim;
                            if (data.has("charId")) peer.charId = (data.optInt("charId", 1) == 2) ? 2 : 1;
                            if (data.has("weapon")) {
                                try { peer.weapon = WeaponType.valueOf(data.getString("weapon")); } catch (Exception ignored) {}
                            }
                            peer.team = pktTeam;
                            String pNm = data.optString("name", "");
                            if (!pNm.isEmpty()) peer.name = pNm;
                            if (data.has("hp")) peer.hp = data.optInt("hp", peer.hp);
                            peer.shield = data.optInt("shield", 0);
                            peer.dead = data.optBoolean("dead", peer.hp <= 0);
                            peer.inBush = data.optBoolean("inBush", false);
                            peer.stealth = data.optBoolean("stealth", false);
                            if (!wasPeerDead && peer.dead) {
                                peer.deathX = nx;
                                peer.deathY = ny;
                                peer.deathBlinkTimer = 0.85f;
                                peer.respawnTimer = 5.0f;
                            }
                            if (wasPeerDead && !peer.dead) {
                                peer.x = nx;
                                peer.y = ny;
                                peer.deathBlinkTimer = 0f;
                                peer.respawnTimer = 0f;
                            }
                            return;
                        }
                    }
                }

                if (!senderSockId.isEmpty()) primaryRemoteSocketId = senderSockId;
                boolean wasDead = remoteDead;
                targetRemoteX = nx;
                targetRemoteY = ny;
                if (remoteX == -999f || Vector2.dst(remoteX, remoteY, nx, ny) > 10f) {
                    remoteX = targetRemoteX;
                    remoteY = targetRemoteY;
                }
                remoteFacing = nFacing;
                if (!nAnim.equals(remoteAnimState) && isAttackAnimState(nAnim)) {
                    remoteAttackAnimTime = 0f;
                }
                remoteAnimState = nAnim;

                if (data.has("charId")) {
                    remoteCharId = (data.optInt("charId", 1) == 2) ? 2 : 1;
                    if (NetworkManager.getInstance() != null) {
                        NetworkManager.getInstance().partnerCharacter = remoteCharId;
                    }
                }
                if (data.has("weapon")) {
                    try { remoteWeapon = WeaponType.valueOf(data.getString("weapon")); } catch (Exception ignored) {}
                }
                if (data.has("team")) {
                    remoteTeam = (data.optInt("team", remoteTeam) == 2) ? 2 : 1;
                }
                if (data.has("name")) {
                    String rNm = data.optString("name", "");
                    if (!rNm.isEmpty()) remoteName = rNm;
                }
                remoteShield = data.optInt("shield", 0);
                remoteInBush = data.optBoolean("inBush", false);
                remoteStealth = data.optBoolean("stealth", false);

                if (data.has("hp")) {
                    remoteHp = data.getInt("hp");
                    if (currentGameMode == GameMode.PVP) {
                        enemyHpBar.setHp(remoteHp);
                        lblEnemyInfo.setText("Đối thủ: " + remoteHp + " HP");
                        if (remoteHp <= 0 && !isMatchEnded && !playerDead) {
                            remoteDead = true;
                            actionLog.setText("ĐỐI THỦ ĐÃ BỊ HẠ GỤC! BẠN CHIẾN THẮNG!");
                            showGameOverDialog(true);
                        }
                    } else if (currentGameMode == GameMode.COOP) {
                        partnerHpBar.setHp(remoteHp);
                        lblPartnerHp.setText("Đồng đội: " + remoteHp + " HP");
                        remoteDead = data.optBoolean("dead", remoteHp <= 0);
                    } else if (currentGameMode == GameMode.BATTLE_5V5) {
                        remoteDead = data.optBoolean("dead", remoteHp <= 0);
                        if (!wasDead && remoteDead) {
                            remoteDeathX = targetRemoteX;
                            remoteDeathY = targetRemoteY;
                            remoteDeathBlinkTimer = 0.85f;
                            remoteRespawnTimer = 5.0f;
                        }
                        if (wasDead && !remoteDead) {
                            remoteX = targetRemoteX;
                            remoteY = targetRemoteY;
                            remoteDeathBlinkTimer = 0f;
                            remoteRespawnTimer = 0f;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }));

        socket.on("enemyAction", args -> Gdx.app.postRunnable(() -> {
            if (isDisposed || isHidden || (game != null && game.getScreen() != this)) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String action = data.getString("type");

                if ("chat".equals(action) && commsManager != null) {
                    commsManager.onIncomingChat(
                        data.optString("sender", "Đồng đạo"),
                        data.optInt("team", 0),
                        data.optString("text", ""),
                        chatPanelOpen);
                    SoundManager.getInstance().playOrbPickup();
                    return;
                }
                if ("voice".equals(action) && commsManager != null) {
                    commsManager.onIncomingVoice(
                        data.optString("sender", "Đồng đạo"),
                        data.optString("pcm", ""));
                    return;
                }
                if ("kill5v5".equals(action) && currentGameMode == GameMode.BATTLE_5V5) {
                    on5v5Kill(
                        data.optString("killerName", "Đối thủ"),
                        data.optInt("killerTeam", 1),
                        data.optString("victimName", "Chiến Thần"),
                        data.optInt("victimTeam", 2));
                    return;
                }
                if ("cloneSwap".equals(action)) {
                    if (remoteCloneActive) {
                        remoteCloneX = (float) data.optDouble("x", remoteCloneX);
                        remoteCloneY = (float) data.optDouble("y", remoteCloneY);
                        remoteCloneFacing = data.optInt("facing", remoteCloneFacing);
                        spawnHitSparks(remoteCloneX, remoteCloneY + 0.3f, 14, Color.PURPLE);
                    }
                    return;
                }

                if ("skillCast".equals(action)) {
                    int slot = data.optInt("slot", 0);
                    int charId = data.optInt("charId", 1);
                    float castX = (float) data.optDouble("x", remoteX);
                    float castY = (float) data.optDouble("y", remoteY);
                    int castFacing = data.optInt("facing", remoteFacing);
                    int senderTeam = data.optInt("team", remoteTeam);
                    String senderName = data.optString("senderName", remoteName);
                    String senderId = data.optString("senderId", "");
                    executeRemoteSkill(senderId, senderName, senderTeam, charId, slot, castX, castY, castFacing);
                    return;
                }
                if ("ultCast".equals(action)) {
                    int charId = data.optInt("charId", 1);
                    int stage = data.optInt("stage", 1);
                    float castX = (float) data.optDouble("x", remoteX);
                    float castY = (float) data.optDouble("y", remoteY);
                    int castFacing = data.optInt("facing", remoteFacing);
                    float tX = (float) data.optDouble("targetX", 0);
                    float tY = (float) data.optDouble("targetY", 0);
                    int senderTeam = data.optInt("team", remoteTeam);
                    String senderName = data.optString("senderName", remoteName);
                    String senderId = data.optString("senderId", "");
                    executeRemoteUltimate(senderId, senderName, senderTeam, charId, stage, castX, castY, castFacing, tX, tY);
                    return;
                }

                if (currentGameMode == GameMode.PVP || currentGameMode == GameMode.BATTLE_5V5) {
                    if ("hit".equals(action) && !playerDead) {
                        if (currentGameMode == GameMode.BATTLE_5V5) {
                            String targetId = data.optString("targetId", "");
                            if (!targetId.isEmpty() && !targetId.equals(myNetId)) return;
                            int pTeamCheck = (game != null && game.selectedTeam == 2) ? 2 : 1;
                            int atkTeamCheck = data.optInt("attackerTeam", 0);
                            if (atkTeamCheck == pTeamCheck) return;
                        }
                        int dmg = Math.max(0, data.optInt("damage", 0));
                        boolean noReflect = data.optBoolean("noReflect", false);
                        boolean poisonHit = data.optBoolean("poison", false);
                        if (poisonHit) {
                            playerPoisonSlowTimer = 2.5f;
                            addCustomText(playerBody.getPosition().x, playerBody.getPosition().y + 0.9f, "TRÚNG ĐỘC (-35% TỐC)!", Color.PURPLE);
                        }
                        // Cơ chế Phản Chấn Sấm Sét của Lôi Phong Hộ Thể (P1 Chiêu 2): không phản chấn lại đòn phản chấn!
                        if (!noReflect && p1WindShieldTimer > 0f) {
                            String atkId = data.optString("senderId", primaryRemoteId);
                            float fx = remoteX, fy = remoteY;
                            RemotePeer5v5 atkPeer = findExtraPeerById(atkId);
                            if (atkPeer != null && atkPeer.x != -999f) {
                                fx = atkPeer.x;
                                fy = atkPeer.y;
                            }
                            if (fx != -999f) {
                                int counterDmg = Math.max(15, (int)(dmg * 0.35f));
                                sendActionPacketFull("hit", counterDmg, 0, atkId, true, false);
                                spawnHitSparks(fx, fy + 0.3f, 18, Color.GOLD);
                                addCustomText(fx, fy + 0.8f, "PHẢN CHẤN LÔI ĐIỆN!", Color.GOLD);
                                SoundManager.getInstance().playTrangUltimate();
                            }
                        }
                        if (playerShield > 0) {
                            if (playerShield >= dmg) { playerShield -= dmg; dmg = 0; }
                            else { dmg -= playerShield; playerShield = 0; }
                        }
                        playerHp = Math.max(0, playerHp - dmg);
                        playerHpBar.setHp(playerHp);
                        lblPlayerHp.setText("Máu: " + playerHp + " HP");
                        playerHitFlashTimer = 0.22f;
                        SoundManager.getInstance().playHurt();
                        cameraShakeTime = 0.25f;
                        cameraShakeIntensity = 0.25f;
                        addDamageText(playerBody.getPosition().x, playerBody.getPosition().y + 0.4f, dmg, Color.RED);
                        actionLog.setText("Bạn bị đối thủ chém! -" + dmg + " HP");
                        sendSyncPacket(lastSentAnim.isEmpty() ? "IDLE" : lastSentAnim);
                        if (playerHp <= 0) {
                            if (currentGameMode == GameMode.BATTLE_5V5) {
                                int pTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;
                                String atkName = data.optString("attackerName", remoteName);
                                int atkTeam = data.optInt("attackerTeam", pTeam == 1 ? 2 : 1);
                                handleLocalPlayerDeath5v5(atkName, atkTeam);
                            } else {
                                playerDead = true;
                                closeShop();
                                resetTraversalState();
                                if (playerBody != null) {
                                    playerDeathX = playerBody.getPosition().x;
                                    playerDeathY = playerBody.getPosition().y;
                                    playerDeathBlinkTimer = 0.85f;
                                }
                                sendSyncPacket("IDLE");
                                showGameOverDialog(false);
                            }
                        }
                    }
                } else if (currentGameMode == GameMode.COOP) {
                    if ("revive".equals(action) && playerDead) {
                        playerDead = false;
                        playerHp = playerMaxHp;
                        playerHpBar.resetHp(playerMaxHp, playerMaxHp);
                        lblPlayerHp.setText("Máu: " + playerMaxHp + " HP");
                        actionLog.setText("ĐỒNG ĐỘI ĐÃ CỨU BẠN!");
                        setGameOverDialogVisible(false);
                        isMatchEnded = false;
                        resetTraversalState();
                        playerBody.setTransform(remoteX, remoteY + 1.0f, 0);
                        playerBody.setLinearVelocity(0, 0);
                    } else if ("mobHit".equals(action)) {
                        // Đồng bộ theo ID ổn định (chỉ số mảng thay đổi khi quái chết bị xóa -> trúng nhầm quái)
                        int mobId = data.getInt("index");
                        int dmg = data.getInt("damage");
                        for (int mi = 0; mi < activeMonsters.size; mi++) {
                            GroundMob gm = activeMonsters.get(mi);
                            if (gm.id == mobId) {
                                damageGroundMob(gm, dmg, false);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }));

        socket.on("rematchReady", args -> Gdx.app.postRunnable(() -> {
            if (isDisposed || isHidden || (game != null && game.getScreen() != this)) return;
            restartOrReturnLobby();
        }));

        socket.on("playerLeft", args -> Gdx.app.postRunnable(() -> {
            if (isDisposed || isHidden || (game != null && game.getScreen() != this) || isMatchEnded) return;
            if (currentGameMode == GameMode.PVP) {
                actionLog.setText("Đối thủ đã rời phòng! Bạn chiến thắng.");
                showGameOverDialog(true);
            } else if (currentGameMode == GameMode.COOP) {
                partnerLeft = true;
                remoteX = -999f;
                remoteY = -999f;
                targetRemoteX = -999f;
                targetRemoteY = -999f;
                actionLog.setText("Đồng đội đã thoát trận!");
                partnerHpBar.setVisible(false);
                lblPartnerHp.setVisible(false);
                if (btnRevivePartner != null) btnRevivePartner.setVisible(false);
            } else if (currentGameMode == GameMode.BATTLE_5V5) {
                String leftSockId = "";
                if (args != null && args.length > 0 && args[0] instanceof JSONObject) {
                    leftSockId = ((JSONObject) args[0]).optString("id", "");
                }
                if (!leftSockId.isEmpty()) {
                    for (int i = extraPeers5v5.size - 1; i >= 0; i--) {
                        if (leftSockId.equals(extraPeers5v5.get(i).socketId)) {
                            extraPeers5v5.removeIndex(i);
                        }
                    }
                    if (leftSockId.equals(primaryRemoteSocketId)) {
                        primaryRemoteId = "";
                        primaryRemoteSocketId = "";
                        remoteX = -999f;
                        remoteY = -999f;
                        targetRemoteX = -999f;
                        targetRemoteY = -999f;
                        remoteDead = true;
                        // Thăng 1 người chơi còn lại trong extraPeers5v5 (ưu tiên đối thủ khác phe) lên primaryRemote
                        if (extraPeers5v5.size > 0) {
                            int myTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;
                            int pickIdx = 0;
                            for (int i = 0; i < extraPeers5v5.size; i++) {
                                if (extraPeers5v5.get(i).team != myTeam) { pickIdx = i; break; }
                            }
                            RemotePeer5v5 nextP = extraPeers5v5.removeIndex(pickIdx);
                            primaryRemoteId = nextP.id;
                            primaryRemoteSocketId = nextP.socketId;
                            remoteName = nextP.name;
                            remoteTeam = nextP.team;
                            remoteCharId = nextP.charId;
                            remoteWeapon = nextP.weapon;
                            remoteX = nextP.x;
                            remoteY = nextP.y;
                            targetRemoteX = nextP.targetX;
                            targetRemoteY = nextP.targetY;
                            remoteFacing = nextP.facing;
                            remoteAnimState = nextP.animState;
                            remoteAttackAnimTime = nextP.attackAnimTime;
                            remoteHp = nextP.hp;
                            remoteShield = nextP.shield;
                            remoteDead = nextP.dead;
                            remoteInBush = nextP.inBush;
                            remoteStealth = nextP.stealth;
                            remoteHitFlashTimer = nextP.hitFlashTimer;
                            remoteDeathX = nextP.deathX;
                            remoteDeathY = nextP.deathY;
                            remoteDeathBlinkTimer = nextP.deathBlinkTimer;
                            remoteRespawnTimer = nextP.respawnTimer;
                        }
                    }
                }
                actionLog.setText("Một người chơi đã rời đấu trường 5v5.");
            }
        }));

        // Phát ngay gói đồng bộ đầu tiên khi vào trận để đối phương biết nhân vật (Trắng/Đen) & vũ khí ngay khung hình 1
        sendSyncPacket("IDLE");
    }

    private RemotePeer5v5 findExtraPeer(String id) {
        if (id == null || id.isEmpty()) return null;
        for (int i = 0; i < extraPeers5v5.size; i++) {
            RemotePeer5v5 p = extraPeers5v5.get(i);
            if (id.equals(p.id)) return p;
        }
        return null;
    }

    private RemotePeer5v5 getOrCreateExtraPeer(String id) {
        RemotePeer5v5 existing = findExtraPeer(id);
        if (existing != null) return existing;
        RemotePeer5v5 created = new RemotePeer5v5();
        created.id = id;
        extraPeers5v5.add(created);
        return created;
    }

    private void removeExtraPeerById(String id) {
        for (int i = extraPeers5v5.size - 1; i >= 0; i--) {
            if (id.equals(extraPeers5v5.get(i).id)) {
                extraPeers5v5.removeIndex(i);
            }
        }
    }

    private void sendSyncPacket(String currentAnim) {
        if (socket != null && socket.connected() && playerBody != null) {
            try {
                JSONObject packet = new JSONObject();
                packet.put("senderId", myNetId);
                packet.put("socketId", socket.id() != null ? socket.id() : "");
                packet.put("x", (playerDead && currentGameMode == GameMode.BATTLE_5V5) ? playerDeathX : playerBody.getPosition().x);
                packet.put("y", (playerDead && currentGameMode == GameMode.BATTLE_5V5) ? playerDeathY : playerBody.getPosition().y);
                packet.put("facing", playerFacing);
                packet.put("animState", (currentAnim != null && !currentAnim.isEmpty()) ? currentAnim : "IDLE");
                packet.put("hp", playerHp);
                packet.put("charId", "P2".equals(selectedChar) ? 2 : 1);
                packet.put("weapon", currentWeapon.name());
                packet.put("team", (game != null && game.selectedTeam == 2) ? 2 : 1);
                packet.put("name", (game != null && game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần");
                packet.put("dead", playerDead);
                packet.put("shield", playerShield);
                packet.put("inBush", playerInBush);
                packet.put("stealth", isP2StealthActive);
                socket.emit("playerMove", packet);
            } catch (Exception ignored) {}
        }
    }

    private void sendActionPacket(String type, int damage, int targetIndex) {
        sendActionPacketFull(type, damage, targetIndex, primaryRemoteId, false, false);
    }

    private void sendActionPacketToPeer(String type, int damage, int targetIndex, String targetPeerId) {
        sendActionPacketFull(type, damage, targetIndex, targetPeerId, false, false);
    }

    private void sendActionPacketFull(String type, int damage, int targetIndex, String targetPeerId, boolean noReflect, boolean poison) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject packet = new JSONObject();
                packet.put("type", type);
                packet.put("damage", damage);
                packet.put("index", targetIndex);
                packet.put("senderId", myNetId);
                if (noReflect) packet.put("noReflect", true);
                if (poison) packet.put("poison", true);
                if (targetPeerId != null && !targetPeerId.isEmpty()) {
                    packet.put("targetId", targetPeerId);
                }
                if (currentGameMode == GameMode.BATTLE_5V5) {
                    packet.put("attackerName", (game != null && game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần");
                    packet.put("attackerTeam", (game != null && game.selectedTeam == 2) ? 2 : 1);
                }
                socket.emit("playerAction", packet);
            } catch (Exception ignored) {}
        }
    }

    private void broadcastCloneSwap(float cloneX, float cloneY, int facing) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject pkt = new JSONObject();
                pkt.put("type", "cloneSwap");
                pkt.put("x", cloneX);
                pkt.put("y", cloneY);
                pkt.put("facing", facing);
                pkt.put("senderId", myNetId);
                socket.emit("playerAction", pkt);
            } catch (Exception ignored) {}
        }
    }

    private void broadcast5v5Kill(String killerName, int killerTeam, String victimName, int victimTeam) {
        if (socket != null && socket.connected() && currentGameMode == GameMode.BATTLE_5V5) {
            try {
                JSONObject packet = new JSONObject();
                packet.put("type", "kill5v5");
                packet.put("killerName", killerName);
                packet.put("killerTeam", killerTeam);
                packet.put("victimName", victimName);
                packet.put("victimTeam", victimTeam);
                socket.emit("playerAction", packet);
            } catch (Exception ignored) {}
        }
    }

    private void broadcastSkillCast(int slot, int charId, float x, float y, int facing) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject pkt = new JSONObject();
                pkt.put("type", "skillCast");
                pkt.put("slot", slot);
                pkt.put("charId", charId);
                pkt.put("x", x);
                pkt.put("y", y);
                pkt.put("facing", facing);
                pkt.put("senderId", myNetId);
                pkt.put("senderName", (game != null && game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần");
                pkt.put("team", (game != null && game.selectedTeam == 2) ? 2 : 1);
                socket.emit("playerAction", pkt);
            } catch (Exception ignored) {}
        }
    }

    private void broadcastUltCast(int charId, int stage, float x, float y, int facing, float targetX, float targetY) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject pkt = new JSONObject();
                pkt.put("type", "ultCast");
                pkt.put("charId", charId);
                pkt.put("stage", stage);
                pkt.put("x", x);
                pkt.put("y", y);
                pkt.put("facing", facing);
                pkt.put("targetX", targetX);
                pkt.put("targetY", targetY);
                pkt.put("senderId", myNetId);
                pkt.put("senderName", (game != null && game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần");
                pkt.put("team", (game != null && game.selectedTeam == 2) ? 2 : 1);
                socket.emit("playerAction", pkt);
            } catch (Exception ignored) {}
        }
    }

    private void executeRemoteSkill(String senderId, String senderName, int senderTeam, int charId, int slot, float castX, float castY, int castFacing) {
        if (slot < 0 || slot > 2) return;
        // Hiệu ứng kỹ năng của người chơi khác chỉ hiển thị hình ảnh (sát thương đã do máy tung chiêu gửi qua gói "hit"/"mobHit")

        if (charId == 1) {
            // === VÂN TIÊU (P1 - PHONG NHẪN / LÔI PHONG / VẠN KIẾM) ===
            if (slot == 0) {
                Projectile wave = new Projectile(castX + castFacing * 0.55f, castY + 0.28f,
                    castFacing * 14.5f, 0f, 0, true, false, false, false);
                wave.skillVisualType = 1;
                wave.piercing = true;
                wave.lifeTime = 1.45f;
                wave.fromRemotePeer = true;
                wave.ownerTeam = senderTeam;
                projectiles.add(wave);
                spawnHitSparks(castX + castFacing * 0.65f, castY + 0.28f, 18, Color.CYAN);
                SoundManager.getInstance().playPlayerSlash(true);
                addCustomText(castX, castY + 0.8f, "PHONG NHẪN!", Color.CYAN);
            } else if (slot == 1) {
                remoteWindShieldTimer = 4.5f;
                spawnHitSparks(castX, castY + 0.3f, 26, Color.CYAN);
                spawnHitSparks(castX, castY + 0.3f, 14, Color.GOLD);
                SoundManager.getInstance().playDash();
                addCustomText(castX, castY + 0.85f, "LÔI PHONG HỘ THỂ!", Color.SKY);
            } else {
                remoteSwordRainRemaining = 8;
                remoteSwordRainTick = 0.05f;
                remoteSwordRainX = castX;
                remoteSwordRainY = castY;
                remoteSwordRainFacing = castFacing;
                remoteSwordRainTeam = senderTeam;
                spawnHitSparks(castX, castY + 0.85f, 30, Color.GOLD);
                SoundManager.getInstance().playUltimateP1();
                addCustomText(castX, castY + 1.0f, "VẠN KIẾM QUY TÔNG!", Color.GOLD);
            }
        } else {
            // === VÂN U (P2 - HUYẾT TIÊU / PHÂN THÂN / HUYẾT TRẬN) ===
            if (slot == 0) {
                float[] angles = new float[]{-0.20f, 0f, 0.20f};
                for (float ang : angles) {
                    float vx = castFacing * (float) Math.cos(ang) * 14.0f;
                    float vy = (float) Math.sin(ang) * 14.0f;
                    Projectile blade = new Projectile(castX + castFacing * 0.50f, castY + 0.32f,
                        vx, vy, 0, true, false, false, false);
                    blade.skillVisualType = 3;
                    blade.applyPoison = false;
                    blade.lifeTime = 1.45f;
                    blade.fromRemotePeer = true;
                    blade.ownerTeam = senderTeam;
                    projectiles.add(blade);
                }
                spawnHitSparks(castX + castFacing * 0.60f, castY + 0.32f, 18, Color.PURPLE);
                SoundManager.getInstance().playPlayerSlash(false);
                addCustomText(castX, castY + 0.8f, "HUYẾT TIÊU!", Color.MAGENTA);
            } else if (slot == 1) {
                remoteCloneActive = true;
                remoteCloneX = castX;
                remoteCloneY = castY;
                remoteCloneFacing = castFacing;
                remoteCloneTimer = 2.5f;
                remoteCloneTeam = senderTeam;
                spawnHitSparks(castX, castY + 0.3f, 22, Color.PURPLE);
                SoundManager.getInstance().playDash();
                addCustomText(castX, castY + 0.8f, "ẢNH PHÂN THÂN!", Color.PURPLE);
            } else {
                remoteAbyssTimer = 4.2f;
                remoteAbyssTick = 0.05f;
                remoteAbyssX = castX + castFacing * 2.5f;
                remoteAbyssY = castY;
                remoteAbyssTeam = senderTeam;
                spawnHitSparks(remoteAbyssX, remoteAbyssY + 0.3f, 32, Color.PURPLE);
                SoundManager.getInstance().playHeavyHit();
                addCustomText(remoteAbyssX, remoteAbyssY + 1.0f, "HẮC ÁM HUYẾT TRẬN!", Color.MAGENTA);
            }
        }
    }

    private void executeRemoteUltimate(String senderId, String senderName, int senderTeam, int charId, int stage, float castX, float castY, int castFacing, float targetX, float targetY) {
        if (charId == 1) {
            spawnHitSparks(castX, castY + 0.5f, 35, Color.CYAN);
            spawnHitSparks(castX, castY + 0.5f, 20, Color.GOLD);
            cameraShakeTime = 0.35f; cameraShakeIntensity = 0.30f;
            SoundManager.getInstance().playUltimateP1();
            SoundManager.getInstance().playTrangUltimate();
            addCustomText(castX, castY + 1.2f, "TRẤN TRỜI: CUỒNG LÔI PHONG BẠO!", Color.CYAN);
        } else {
            if (stage == 1) {
                remoteStealth = true;
                spawnHitSparks(castX, castY + 0.4f, 28, Color.PURPLE);
                SoundManager.getInstance().playUltimateP2();
                addCustomText(castX, castY + 1.0f, "ÁM DẠ TÀNG HÌNH!", Color.PURPLE);
            } else {
                remoteStealth = false;
                spawnHitSparks(castX, castY + 0.4f, 25, Color.PURPLE);
                spawnHitSparks(targetX, targetY + 0.4f, 35, Color.MAGENTA);
                cameraShakeTime = 0.32f; cameraShakeIntensity = 0.28f;
                SoundManager.getInstance().playHeavyHit();
                SoundManager.getInstance().playPlayerSlash(false);
                addCustomText(targetX, targetY + 1.2f, "TỬ THẦN TRẢM!", Color.RED);
            }
        }
    }

    private void addP1PassiveStack() {
        if ("P2".equals(selectedChar)) return;
        p1PassiveStacks = Math.min(4, p1PassiveStacks + 1);
        if (p1PassiveStacks >= 4 && p1AwakenedTimer <= 0f) {
            p1AwakenedTimer = 5.0f;
            p1PassiveStacks = 0;
            if (playerBody != null) {
                Vector2 pPos = playerBody.getPosition();
                addCustomText(pPos.x, pPos.y + 0.95f, "⚡ KIẾM THẦN THỨC TỈNH!", Color.GOLD);
                SoundManager.getInstance().playTrangUltimate();
                spawnHitSparks(pPos.x, pPos.y + 0.4f, 30, Color.GOLD);
                spawnHitSparks(pPos.x, pPos.y + 0.4f, 18, Color.CYAN);
                cameraShakeTime = 0.25f; cameraShakeIntensity = 0.22f;
                actionLog.setText("NỘI TẠI KÍCH HOẠT: KIẾM THẦN THỨC TỈNH! +30% Tốc chạy & +35% Tốc đánh!");
                Projectile tempest = new Projectile(pPos.x + playerFacing * 0.6f, pPos.y + 0.3f,
                    playerFacing * 15.0f, 0f, 125, true, false, false, false);
                tempest.skillVisualType = 1;
                tempest.piercing = true;
                tempest.lifeTime = 1.5f;
                tempest.ownerTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;
                projectiles.add(tempest);
                broadcastSkillCast(0, 1, pPos.x, pPos.y, playerFacing);
            }
        }
    }

    private boolean checkP2Backstab(float targetX, int targetFacing) {
        if (!"P2".equals(selectedChar) || playerBody == null) return false;
        Vector2 pPos = playerBody.getPosition();
        boolean isBehind = (targetFacing == 1 && pPos.x < targetX) || (targetFacing == -1 && pPos.x > targetX);
        return isBehind && (playerFacing == targetFacing || isP2StealthActive || isDashing);
    }

    private boolean isOnlineMode() {
        return currentGameMode != GameMode.SOLO;
    }

    private static boolean isDesktopPlatform() {
        return Gdx.app.getType() == Application.ApplicationType.Desktop
            || (Gdx.app.getType() == Application.ApplicationType.WebGL && !TextInputBridge.isTouchPromptActive());
    }

    private void applyTouchHudVisibility(boolean gameplayVisible) {
        if (!gameplayVisible) {
            if (uiStage != null) uiStage.cancelTouchFocus();
            isAttackButtonPressed = false;
            isClimbingHold = false;
        }
        boolean showPad = gameplayVisible && !virtualTouchHidden;
        if (touchpad != null) touchpad.setVisible(showPad);
        if (leftTable != null) leftTable.setVisible(showPad);
        if (rightTable != null) rightTable.setVisible(gameplayVisible);
        if (btnAtkRef != null) btnAtkRef.setVisible(showPad);
        if (btnJumpRef != null) btnJumpRef.setVisible(showPad);
        if (btnDashRef != null) btnDashRef.setVisible(showPad);
        if (btnCrouchRef != null) btnCrouchRef.setVisible(showPad);
        if (btnClimbRef != null) btnClimbRef.setVisible(showPad);
    }

    private void toggleVirtualTouchHud() {
        virtualTouchHidden = !virtualTouchHidden;
        boolean gameplayVisible = !isPaused && !storyDialogueActive
            && (gameOverDialog == null || !gameOverDialog.isVisible());
        applyTouchHudVisibility(gameplayVisible);
        SoundManager.getInstance().playOrbPickup();
        if (actionLog != null) {
            actionLog.setText(virtualTouchHidden
                ? "ĐÃ ẨN NÚT CẢM ỨNG ẢO (Bấm F1 để hiện lại)"
                : "ĐÃ HIỆN NÚT CẢM ỨNG ẢO (Bấm F1 để ẩn)");
        }
    }

    private void updatePauseSelectionVisual() {
        if (btnPauseResume != null) {
            btnPauseResume.setText(pauseMenuIndex == 0 ? "▸ CHƠI TIẾP (Enter) ‹" : "CHƠI TIẾP (1)");
        }
        if (btnPauseSettings != null) {
            btnPauseSettings.setText(pauseMenuIndex == 1 ? "▸ CÀI ĐẶT & PHÍM (Enter) ‹" : "CÀI ĐẶT & PHÍM (2)");
        }
        if (btnPauseExitBtn != null) {
            btnPauseExitBtn.setText(pauseMenuIndex == 2 ? "▸ THOÁT RA MENU (Enter) ‹" : "THOÁT RA MENU (Q)");
        }
    }

    private void activatePauseSelection(int idx) {
        if (!isPaused || isSettingsModalOpen) return;
        if (idx == 0) {
            togglePause();
        } else if (idx == 1) {
            isSettingsModalOpen = true;
            SettingsDialog.show(uiStage, game, () -> isSettingsModalOpen = false);
        } else if (idx == 2) {
            togglePause();
            exitToMenu();
        }
    }

    private boolean isAttackKeyHeld(boolean chatTyping) {
        return !chatTyping && (Gdx.input.isKeyPressed(Input.Keys.J)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
    }

    private boolean isAttackKeyJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.J)
            || Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_RIGHT);
    }

    private void closeChatOverlay() {
        chatPanelOpen = false;
        if (chatPanel != null) chatPanel.setVisible(false);
        if (uiStage != null && chatInputField != null && uiStage.getKeyboardFocus() == chatInputField) {
            uiStage.setKeyboardFocus(null);
        }
    }

    private boolean isChatTyping() {
        return chatPanelOpen && uiStage != null && chatInputField != null && uiStage.getKeyboardFocus() == chatInputField;
    }

    private void togglePause() {
        boolean coopDowned = (currentGameMode == GameMode.COOP && playerDead && !coopWipeShown);
        if (isMatchEnded || (playerDead && !coopDowned) || storyDialogueActive) return;
        if (gameOverDialog != null && gameOverDialog.isVisible()) return;
        isPaused = !isPaused;
        if (isPaused) {
            closeChatOverlay();
            closeShop();
            pauseMenuIndex = 0;
            updatePauseSelectionVisual();
        }
        if (modalOverlay != null) modalOverlay.setVisible(isPaused);
        if (pauseDialog != null) pauseDialog.setVisible(isPaused);
        if (isOnlineMode()) {
            if (btnChatToggle != null) btnChatToggle.setVisible(!isPaused);
            if (btnMicToggle != null) btnMicToggle.setVisible(!isPaused);
        }
        applyTouchHudVisibility(!isPaused);
        if (actionLog != null) {
            if (isPaused) actionLog.setText("ĐANG TẠM DỪNG (W/S chọn • Enter xác nhận • Esc tiếp tục)");
            else actionLog.setText("TIẾP TỤC TRẬN ĐẤU!");
        }
    }

    private void updateDialogButtons() {
        if (dialogButtons == null) return;
        dialogButtons.clearChildren();
        if (btnWaitRevive != null && btnWaitRevive.isVisible()) {
            dialogButtons.add(btnWaitRevive).size(205, 65).padRight(14);
            dialogButtons.add(btnPlayAgain).size(220, 65).padRight(14);
            dialogButtons.add(btnQuitGame).size(190, 65);
        } else {
            dialogButtons.add(btnPlayAgain).size(275, 65).padRight(20);
            dialogButtons.add(btnQuitGame).size(205, 65);
        }
    }

    private void setGameOverDialogVisible(boolean visible) {
        if (visible) {
            closeChatOverlay();
            closeShop();
            isPaused = false;
            if (pauseDialog != null) pauseDialog.setVisible(false);
            if (isDesktopPlatform()) {
                if (btnPlayAgain != null) {
                    String cur = btnPlayAgain.getText().toString();
                    if (!cur.endsWith("(Enter)")) btnPlayAgain.setText(cur + " (Enter)");
                }
                if (btnWaitRevive != null && btnWaitRevive.isVisible()) {
                    btnWaitRevive.setText("ĐỢI HỒI SINH (R)");
                }
                if (btnQuitGame != null) {
                    btnQuitGame.setText("VỀ MENU (Esc)");
                }
            }
            updateDialogButtons();
        }
        if (modalOverlay != null) modalOverlay.setVisible(visible);
        if (gameOverDialog != null) gameOverDialog.setVisible(visible);
        if (isOnlineMode()) {
            if (btnChatToggle != null) btnChatToggle.setVisible(!visible);
            if (btnMicToggle != null) btnMicToggle.setVisible(!visible);
        }

        applyTouchHudVisibility(!visible);

        if (visible) {
            if (btnEagleStrike != null) btnEagleStrike.setVisible(false);
            if (btnShootCannon != null) btnShootCannon.setVisible(false);
            if (btnRevivePartner != null) btnRevivePartner.setVisible(false);
            if (btnAssassinate != null) btnAssassinate.setVisible(false);
        }
    }

    // =====================================================================
    // HỘI THOẠI CỐT TRUYỆN (chơi đơn)
    // =====================================================================
    private boolean isBlackAssassin() { return "P2".equals(selectedChar); }

    /** Đang có cảnh phim (giới thiệu trùm / hội thoại): khóa điều khiển, quái đứng yên. */
    private boolean inCutscene() { return bossIntroActive || storyDialogueActive || shopFreezesWorld(); }

    /** Chơi đơn: mở tiệm đồ thì thế giới đứng yên (online không thể dừng đối phương). */
    private boolean shopFreezesWorld() { return isShopOpen && !isOnlineMode(); }

    private boolean storyInputUsedThisFrame() { return storyInputFrame == Gdx.graphics.getFrameId(); }

    private void startStoryDialogue(java.util.List<StoryChapterConfig.StoryLine> lines, Runnable onDone) {
        if (lines == null || lines.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        closeChatOverlay();
        closeShop();
        storyLines = lines;
        storyLineIndex = 0;
        storyLineTime = 0f;
        storyDialogueCallback = onDone;
        storyDialogueActive = true;
        moveLeft = false; moveRight = false; isDashing = false;
        if (playerBody != null) playerBody.setLinearVelocity(0, playerBody.getLinearVelocity().y);
        applyTouchHudVisibility(false);
        if (btnTalkNPC != null) btnTalkNPC.setVisible(false);
        if (btnEagleStrike != null) btnEagleStrike.setVisible(false);
        if (btnShootCannon != null) btnShootCannon.setVisible(false);
        if (btnAssassinate != null) btnAssassinate.setVisible(false);
        updateStoryFocus();
    }

    private int storyVisibleChars() { return (int) (storyLineTime * 60f); }

    private void advanceStoryDialogue() {
        if (!storyDialogueActive) return;
        StoryChapterConfig.StoryLine line = storyLines.get(storyLineIndex);
        if (storyVisibleChars() < line.text.length()) { // chữ đang chạy -> hiện hết câu trước
            storyLineTime = 999f;
            return;
        }
        storyLineIndex++;
        storyLineTime = 0f;
        if (storyLineIndex >= storyLines.size()) endStoryDialogue();
        else updateStoryFocus();
    }

    private void endStoryDialogue() {
        storyDialogueActive = false;
        storyLines = java.util.Collections.emptyList();
        if (!isMatchEnded) {
            applyTouchHudVisibility(true);
        }
        Runnable cb = storyDialogueCallback;
        storyDialogueCallback = null;
        if (cb != null) cb.run();
    }

    /** Camera hướng về người đang nói (cậu bé / trùm), còn lại là nhân vật đang điều khiển. */
    private void updateStoryFocus() {
        if (playerBody == null || storyLineIndex >= storyLines.size()) return;
        String av = storyLines.get(storyLineIndex).avatar;
        float fx = playerBody.getPosition().x, fy = playerBody.getPosition().y + 0.6f;
        if (StoryChapterConfig.AV_BOY.equals(av) && boyBody != null) {
            fx = boyBody.getPosition().x; fy = boyBody.getPosition().y + 0.6f;
        } else if (StoryChapterConfig.AV_BOSS.equals(av)) {
            for (int i = 0; i < activeMonsters.size; i++) {
                GroundMob m = activeMonsters.get(i);
                if (m.isBoss && !m.isDead && m.body != null) { fx = m.body.getPosition().x; fy = m.body.getPosition().y + 0.8f; break; }
            }
        }
        storyFocusX = fx;
        storyFocusY = fy;
    }

    private void handleStoryDialogueInput() {
        if (!storyDialogueActive) return;
        if (isBackJustPressed()) {
            storyInputFrame = Gdx.graphics.getFrameId();
            // Esc: bỏ qua cả đoạn hội thoại
            while (storyDialogueActive) { storyLineTime = 999f; advanceStoryDialogue(); }
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.E) || isAttackKeyJustPressed()
            || Gdx.input.justTouched()) {
            storyInputFrame = Gdx.graphics.getFrameId();
            SoundManager.getInstance().playSlash();
            advanceStoryDialogue();
        }
    }

    /** Mở đầu ải: lời dẫn (ải 1) + thoại mở đầu, sau đó hiện tên ải & mục tiêu. */
    private void startStoryIntro() {
        storyIntroStarted = true;
        java.util.List<StoryChapterConfig.StoryLine> lines = new java.util.ArrayList<>();
        if (story.index == 1) lines.addAll(StoryChapterConfig.PROLOGUE);
        lines.addAll(story.introDialogues);
        startStoryDialogue(lines, this::showStageBanner);
    }

    private void showStageBanner() {
        if (story == null) return;
        actionLog.setText(story.objectiveText);
        if (lblStageBanner == null) return;
        lblStageBanner.setText(story.shortLabel().toUpperCase() + "  —  " + story.stageTitle.toUpperCase()
            + "\n" + story.objectiveText);
        lblStageBanner.pack();
        lblStageBanner.setSize(lblStageBanner.getPrefWidth() + 60f, lblStageBanner.getPrefHeight() + 24f);
        lblStageBanner.setPosition((1280f - lblStageBanner.getWidth()) / 2f, 400f);
        lblStageBanner.clearActions();
        lblStageBanner.getColor().a = 0f;
        lblStageBanner.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.4f),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(3.6f),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.8f)));
    }

    /** Chọn & cắt ảnh chân dung (vùng đầu–vai) theo người nói. */
    private TextureRegion portraitFor(String avatar) {
        if (portraitCache.containsKey(avatar)) return portraitCache.get(avatar);
        TextureRegion r = null;
        try {
            if (StoryChapterConfig.AV_P1.equals(avatar)) r = crop("p1.jpg", 0.20f, 0.02f, 0.60f, "trangdung1.png", 0.30f, 0.04f, 0.40f);
            else if (StoryChapterConfig.AV_P2.equals(avatar)) r = crop("p2.jpg", 0.20f, 0.02f, 0.60f, "dendung1.png", 0.30f, 0.04f, 0.40f);
            else if (StoryChapterConfig.AV_BOY.equals(avatar)) r = crop("caubedung.png", 0.24f, 0.06f, 0.46f, null, 0, 0, 0);
            else if (StoryChapterConfig.AV_BOSS.equals(avatar)) r = crop("thulinhdanh1.png", 0.24f, 0.03f, 0.52f, null, 0, 0, 0);
        } catch (Exception ignored) {}
        portraitCache.put(avatar, r);
        return r;
    }

    private TextureRegion crop(String file, float fx, float fy, float fs, String alt, float ax, float ay, float as) {
        String f = file; float x = fx, y = fy, sz = fs;
        if (!Gdx.files.internal(f).exists()) {
            if (alt == null || !Gdx.files.internal(alt).exists()) return null;
            f = alt; x = ax; y = ay; sz = as;
        }
        Texture t = GameAssets.sprite(f); // texture dùng chung, GameAssets tự giải phóng
        if (t == null) return null;
        int w = t.getWidth(), h = t.getHeight();
        return new TextureRegion(t, (int) (x * w), (int) (y * h), (int) (sz * w), (int) (sz * h));
    }

    private Color speakerColor(String avatar) {
        switch (avatar) {
            case StoryChapterConfig.AV_P1: return new Color(0.97f, 0.90f, 0.62f, 1f);
            case StoryChapterConfig.AV_P2: return new Color(0.82f, 0.62f, 1.00f, 1f);
            case StoryChapterConfig.AV_BOY: return new Color(0.50f, 0.95f, 0.62f, 1f);
            case StoryChapterConfig.AV_BOSS: return new Color(1.00f, 0.40f, 0.38f, 1f);
            case StoryChapterConfig.AV_SAGE: return new Color(0.55f, 0.85f, 1.00f, 1f);
            case StoryChapterConfig.AV_ELDER: return new Color(0.85f, 0.80f, 0.70f, 1f);
            default: return new Color(0.95f, 0.86f, 0.62f, 1f);
        }
    }

    /** Vẽ hộp thoại: viền điện ảnh trên/dưới, chân dung, tên, chữ chạy, gợi ý phím. */
    private void drawStoryDialogue() {
        if (!storyDialogueActive || storyLineIndex >= storyLines.size()) return;
        StoryChapterConfig.StoryLine line = storyLines.get(storyLineIndex);
        storyLineTime += Gdx.graphics.getDeltaTime();
        boolean narrator = StoryChapterConfig.AV_NARRATOR.equals(line.avatar);

        uiStage.getViewport().apply();
        batch.setProjectionMatrix(uiStage.getCamera().combined);
        batch.begin();
        float barH = 62f;
        batch.setColor(0f, 0f, 0f, 0.92f);
        batch.draw(pixel, 0, 720 - barH, 1280, barH);
        batch.draw(pixel, 0, 0, 1280, barH);
        if (narrator) { // lời dẫn: phủ tối cả cảnh cho dễ đọc
            batch.setColor(0f, 0f, 0f, 0.55f);
            batch.draw(pixel, 0, barH, 1280, 720 - 2 * barH);
        }

        float boxW = 1040f, boxH = 156f, boxX = (1280f - boxW) / 2f, boxY = 22f;
        batch.setColor(0.05f, 0.06f, 0.09f, 0.94f);
        batch.draw(pixel, boxX, boxY, boxW, boxH);
        Color sc = speakerColor(line.avatar);
        batch.setColor(sc.r, sc.g, sc.b, 0.85f);
        batch.draw(pixel, boxX, boxY + boxH - 2, boxW, 2);
        batch.draw(pixel, boxX, boxY, boxW, 2);
        batch.draw(pixel, boxX, boxY, 2, boxH);
        batch.draw(pixel, boxX + boxW - 2, boxY, 2, boxH);

        float textX = boxX + 26f, textW = boxW - 52f;
        if (!narrator) {
            float ps = 128f, px = boxX + 14f, py = boxY + (boxH - ps) / 2f;
            batch.setColor(0.12f, 0.13f, 0.18f, 1f);
            batch.draw(pixel, px, py, ps, ps);
            TextureRegion pr = portraitFor(line.avatar);
            if (pr != null) {
                batch.setColor(Color.WHITE);
                batch.draw(pr, px + 3, py + 3, ps - 6, ps - 6);
            } else { // nhân vật chưa có ảnh: khung màu + chữ cái đầu
                batch.setColor(sc.r * 0.35f, sc.g * 0.35f, sc.b * 0.35f, 1f);
                batch.draw(pixel, px + 3, py + 3, ps - 6, ps - 6);
                com.badlogic.gdx.graphics.g2d.BitmapFont big = UiKit.display();
                big.getData().setScale(0.9f);
                big.setColor(sc);
                big.draw(batch, line.speaker.substring(0, 1), px, py + ps / 2f + big.getCapHeight() / 2f, ps, Align.center, false);
                big.getData().setScale(1f);
            }
            batch.setColor(sc);
            batch.draw(pixel, px, py, ps, 2); batch.draw(pixel, px, py + ps - 2, ps, 2);
            batch.draw(pixel, px, py, 2, ps); batch.draw(pixel, px + ps - 2, py, 2, ps);
            textX = px + ps + 22f;
            textW = boxX + boxW - 24f - textX;

            com.badlogic.gdx.graphics.g2d.BitmapFont nf = UiKit.bodyBold();
            nf.getData().setScale(0.78f);
            nf.setColor(sc);
            storyLayout.setText(nf, line.speaker);
            nf.draw(batch, line.speaker, textX, boxY + boxH - 16f);
            if (line.monologue) {
                nf.getData().setScale(0.56f);
                nf.setColor(0.65f, 0.72f, 0.85f, 0.9f);
                nf.draw(batch, "(nghĩ thầm)", textX + storyLayout.width + 12f, boxY + boxH - 20f);
            }
            nf.getData().setScale(1f);
        }

        com.badlogic.gdx.graphics.g2d.BitmapFont tf = UiKit.body();
        tf.getData().setScale(narrator ? 0.80f : 0.74f);
        int n = Math.min(line.text.length(), storyVisibleChars());
        String shown = line.text.substring(0, n);
        if (narrator) tf.setColor(0.96f, 0.90f, 0.74f, 1f);
        else tf.setColor(line.monologue ? new Color(0.80f, 0.86f, 0.95f, 1f) : new Color(0.95f, 0.95f, 0.97f, 1f));
        float textTop = narrator ? boxY + boxH - 26f : boxY + boxH - 56f;
        tf.draw(batch, shown, textX, textTop, textW, narrator ? Align.center : Align.left, true);
        tf.getData().setScale(1f);

        com.badlogic.gdx.graphics.g2d.BitmapFont hf = UiKit.body();
        hf.getData().setScale(0.5f);
        float pulse = 0.55f + 0.45f * MathUtils.sin(totalMatchTime * 6f);
        hf.setColor(1f, 0.85f, 0.35f, n >= line.text.length() ? pulse : 0.35f);
        hf.draw(batch, (storyLineIndex + 1) + "/" + storyLines.size() + "   Space / Enter / chạm: tiếp   •   Esc: bỏ qua",
            boxX, boxY + 16f, boxW - 18f, Align.right, false);
        hf.getData().setScale(1f);
        hf.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    // ---------------------------------------------------------------- TIỂU VŨ
    private void createBoyBody(float x, float y) {
        BodyDef bdef = new BodyDef();
        bdef.position.set(x, y);
        bdef.type = BodyDef.BodyType.DynamicBody;
        boyBody = world.createBody(bdef);
        boyBody.setFixedRotation(true);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.15f, 0.3f);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 1.0f;
        fdef.friction = 0.2f;
        boyBody.createFixture(fdef).setUserData("boy");
        shape.dispose();
    }

    /** Ải hộ tống: Tiểu Vũ đi cùng ngay từ đầu. */
    private void spawnEscortBoy() {
        boyState = BoyState.RESCUED;
        boyHp = boyMaxHp;
        boyFacing = 1;
        createBoyBody(1.4f, 1.4f);
    }

    private static final String[][] BOY_CHATS = {
        {"Mẹ ta nói người họ Vân không bao giờ bỏ rơi ai.", "Mẹ em nói đúng. Đi sát sau lưng ta."},
        {"Ta có ná cao su! Bọn lính bay cứ để ta bắn yểm trợ!", "Nhớ nấp kỹ, đừng để chúng thấy em."},
        {"Dưới ngục... hai huynh có sợ không?", "Có. Nhưng sợ không phải lý do để quay đầu."},
        {"Ngày xưa mẹ hay dắt ta đi hái sen ở hồ này.", "Rồi em sẽ được về đây lần nữa – lần này không phải trốn."},
        {"Mặc lão có hiền không? Ta hơi sợ người lạ.", "Ông ấy nghiêm lắm. Nhưng ông ấy đã cứu ta."},
        {"Nếu ta làm vua... ta sẽ không để ai bị oan như họ Vân nữa.", "Hãy nhớ lời đó suốt đời, Tiểu Vũ."},
    };

    private void talkToBoy() {
        if (story == null || boyBody == null || storyDialogueActive || isMatchEnded) return;
        String[] chat = BOY_CHATS[Math.min(story.chapter, BOY_CHATS.length) - 1];
        boolean p2 = isBlackAssassin();
        java.util.List<StoryChapterConfig.StoryLine> lines = new java.util.ArrayList<>();
        lines.add(new StoryChapterConfig.StoryLine("TIỂU VŨ", chat[0], StoryChapterConfig.AV_BOY, false));
        lines.add(new StoryChapterConfig.StoryLine(p2 ? "VÂN U" : "VÂN TIÊU", chat[1], p2 ? StoryChapterConfig.AV_P2 : StoryChapterConfig.AV_P1, false));
        boyChatIndex++;
        startStoryDialogue(lines, () -> {
            if (!boyRewardGiven) { // mỗi ải 1 lần: Tiểu Vũ đưa 1 bình thuốc nhặt được
                boyRewardGiven = true;
                healthPotions++;
                if (lblPotions != null) lblPotions.setText("Thuốc: x" + healthPotions);
                SoundManager.getInstance().playOrbPickup();
                actionLog.setText("Tiểu Vũ đưa bạn 1 bình thuốc! (+1)");
            }
        });
    }

    /** Ải có Tiểu Vũ mà cậu bé hy sinh -> nhiệm vụ thất bại, chơi lại ải. */
    private void checkStoryFailure() {
        if (story == null || isMatchEnded || playerDead) return;
        if (story.boyMode != StoryChapterConfig.BoyMode.NONE && boyState == BoyState.DEAD) {
            playerDead = true;
            isMatchEnded = true;
            btnWaitRevive.setVisible(false);
            lblEndGameTitle.setText("NHIỆM VỤ THẤT BẠI");
            lblEndGameTitle.setColor(Color.RED);
            lblEndStats.setText("Tiểu Vũ đã ngã xuống. Không còn ai mang Huyết Thư của Tiên Hoàng.\nHãy giữ cậu bé ở gần và hạ lính trước khi chúng áp sát.");
            btnPlayAgain.setText("CHƠI LẠI ẢI NÀY");
            setGameOverDialogVisible(true);
        }
    }

    /** Dùng khiên (Hộ Tâm Kính / bùa khiên) đỡ sát thương; trả về phần sát thương còn lại. */
    private int absorbShield(int dmg) {
        if (playerShield <= 0) return dmg;
        if (playerShield >= dmg) { playerShield -= dmg; return 0; }
        int rest = dmg - playerShield;
        playerShield = 0;
        return rest;
    }

    private void showFinishVictoryDialog() {
        // Chơi đơn: phát thoại kết thúc ải (và kết cục ở ải cuối) trước khi hiện bảng thắng
        if (story != null && !storyOutroDone) {
            storyOutroDone = true;
            java.util.List<StoryChapterConfig.StoryLine> outro = new java.util.ArrayList<>(story.outroDialogues);
            if (story.index == StoryChapterConfig.TOTAL_STAGES) outro.addAll(StoryChapterConfig.ending(isBlackAssassin()));
            if (!outro.isEmpty()) {
                isMatchEnded = true;
                if (playerBody != null) playerBody.setLinearVelocity(0, playerBody.getLinearVelocity().y);
                startStoryDialogue(outro, this::showFinishVictoryDialog);
                return;
            }
        }
        isMatchEnded = true;
        btnWaitRevive.setVisible(false);
        lblEndGameTitle.setText(story != null ? story.shortLabel().toUpperCase() + "  —  HOÀN THÀNH!" : "VƯỢT QUA ẢI " + currentLevel + " THÀNH CÔNG!");
        lblEndGameTitle.setColor(Color.GOLD);

        int timeBonus = Math.max(0, 1500 - (int) (totalMatchTime - frozenTime) * 5); // không tính lúc đọc thoại / mua đồ
        int boyBonus = (boyState == BoyState.RESCUED) ? 1000 : 0;
        int boyGold = (boyState == BoyState.RESCUED) ? 500 : 0;
        int totalScore = score + mobsKilledCount * 150 + timeBonus + boyBonus;
        int bonusGold = mobsKilledCount * 45 + boyGold + 1500;
        int totalGold = stageBankedGold + goldEarned + bonusGold;
        goldEarned += bonusGold; // thưởng hoàn thành ải được cộng vào vàng mang về
        grantMatchRewards();
        if (currentGameMode == GameMode.SOLO) {
            PlayerProfile.getInstance().onLevelCleared(currentLevel, totalScore);
            if (!freePlay) StorySaveManager.advanceStage(currentLevel, selectedChar, playerHp, 0, totalScore, healthPotions);
        }

        if (story != null) {
            lblEndStats.setText(story.victoryLore + "\n\n"
                + "Hạ gục: " + mobsKilledCount + "   •   Điểm: " + totalScore + "   •   Vàng: +" + totalGold
                + (story.boyMode != StoryChapterConfig.BoyMode.NONE && boyState == BoyState.RESCUED ? "   •   Tiểu Vũ an toàn (+1000)" : ""));
        } else {
            lblEndStats.setText("Hạ gục: " + mobsKilledCount + "\n" +
                "Cậu bé: " + (boyState == BoyState.RESCUED ? "ĐÃ CỨU (+1000 điểm)" : "Chưa cứu được") + "\n" +
                "TỔNG ĐIỂM: " + totalScore + " ĐIỂM\n" +
                "THƯỞNG VÀNG: +" + totalGold + " VÀNG!");
        }

        if (currentGameMode == GameMode.COOP) {
            btnPlayAgain.setText("VỀ SẢNH HỢP TÁC");
        } else if (currentLevel < StoryChapterConfig.TOTAL_STAGES) {
            StoryChapterConfig.StageData next = StoryChapterConfig.get(currentLevel + 1);
            btnPlayAgain.setText("TIẾP: " + next.shortLabel().toUpperCase());
        } else {
            btnPlayAgain.setText("VỀ MENU (ĐÃ PHÁ ĐẢO)");
        }
        setGameOverDialogVisible(true);
        actionLog.setText(story != null ? "Hoàn thành " + story.shortLabel() + "!" : "CHÚC MỪNG! BẠN ĐÃ VỀ ĐÍCH AN TOÀN!");
    }

    private void showGameOverDialog(boolean isWinner) {
        btnWaitRevive.setVisible(false);

        if (!isWinner && currentGameMode == GameMode.COOP) {
            lblEndGameTitle.setText("BẠN ĐÃ NGÃ XUỐNG!");
            lblEndGameTitle.setColor(Color.ORANGE);
            lblEndStats.setText("Đồng đội có thể cứu bạn với 50 vàng.\nBấm ĐỢI HỒI SINH để theo dõi đồng đội.");
            btnPlayAgain.setText("THOÁT RA SẢNH");
            btnWaitRevive.setVisible(true);
            setGameOverDialogVisible(true);
            return;
        }

        isMatchEnded = true;
        try {
            PlayerProfile.getInstance().recordMatchResult(isWinner);
        } catch (Exception ignored) {}

        int matchGoldTotal = stageBankedGold + goldEarned;
        lblEndGameTitle.setText(isWinner ? "CHIẾN THẮNG!" : "BẠN ĐÃ BỊ HẠ GỤC!");
        lblEndGameTitle.setColor(isWinner ? Color.GOLD : Color.RED);
        lblEndStats.setText("Tổng điểm: " + score + "  •  Vàng: " + matchGoldTotal);

        if (currentGameMode == GameMode.BATTLE_5V5) {
            lblEndGameTitle.setText(isWinner ? "CHIẾN THẮNG – PHE BẠN ĐÃ THẮNG!" : "THẤT BẠI – PHE BẠN ĐÃ THUA!");
            lblEndGameTitle.setColor(isWinner ? Color.GOLD : Color.RED);
            lblEndStats.setText("TỈ SỐ: [XANH] " + blueKills + "  -  " + redKills + " [ĐỎ]\nĐÃ CHẠM MỐC 30 MẠNG!");
            btnPlayAgain.setText("VỀ SẢNH 5V5");
        } else if (currentGameMode == GameMode.PVP) {
            btnPlayAgain.setText("VỀ SẢNH 1V1");
        } else if (currentGameMode == GameMode.COOP) {
            btnPlayAgain.setText("VỀ SẢNH HỢP TÁC");
        } else {
            if (!isWinner) {
                lblEndGameTitle.setText("BẠN ĐÃ GỤC NGÃ!");
                lblEndStats.setText((story != null ? story.shortLabel() + " — " + story.stageTitle + "\n" : "")
                    + "Điểm: " + score + "   •   Vàng: " + matchGoldTotal + "\nMẹo: nấp trong bụi rậm để đánh lén, uống thuốc bằng phím H.");
                btnPlayAgain.setText("CHƠI LẠI ẢI NÀY");
            } else if (currentLevel < StoryChapterConfig.TOTAL_STAGES) {
                btnPlayAgain.setText("TIẾP: " + StoryChapterConfig.get(currentLevel + 1).shortLabel().toUpperCase());
            } else {
                btnPlayAgain.setText("VỀ MENU (ĐÃ PHÁ ĐẢO)");
            }
        }
        setGameOverDialogVisible(true);
    }

    /** Chuyển vàng chưa ngân hàng hóa sang hồ sơ (dùng trước khi lưu checkpoint/thoát ải để tránh nhân đôi). */
    private void flushUnbankedSoloGold() {
        if (currentGameMode != GameMode.SOLO || goldEarned <= 0) return;
        try {
            PlayerProfile.getInstance().addGold(goldEarned);
            stageBankedGold += goldEarned;
            goldEarned = 0;
        } catch (Exception ignored) {}
    }

    /** Cộng vàng mang về vào hồ sơ (chỉ 1 lần mỗi trận). */
    private void grantMatchRewards() {
        if (rewardsGranted) return;
        rewardsGranted = true;
        try {
            if (goldEarned > 0) {
                PlayerProfile.getInstance().addGold(goldEarned);
                stageBankedGold += goldEarned;
                goldEarned = 0;
            }
        } catch (Exception ignored) {}
    }

    private void showCoopWipe() {
        coopWipeShown = true;
        isMatchEnded = true;
        try {
            PlayerProfile.getInstance().recordMatchResult(false);
        } catch (Exception ignored) {}
        btnWaitRevive.setVisible(false);
        lblEndGameTitle.setText("CẢ ĐỘI ĐÃ GỤC NGÃ!");
        lblEndGameTitle.setColor(Color.RED);
        lblEndStats.setText("Hạ gục: " + mobsKilledCount + "  •  Điểm: " + score + "  •  Vàng: " + (stageBankedGold + goldEarned));
        btnPlayAgain.setText("VỀ SẢNH HỢP TÁC");
        setGameOverDialogVisible(true);
        actionLog.setText("THẤT BẠI! KHÔNG CÒN AI ĐỂ HỒI SINH.");
    }

    private void restartOrReturnLobby() {
        if (isDisposed) return;
        cleanSocketListeners();
        grantMatchRewards();
        if (currentGameMode == GameMode.BATTLE_5V5) game.setScreen(new Arena5v5LobbyScreen(game));
        else if (currentGameMode == GameMode.PVP) game.setScreen(new LobbyScreen(game));
        else if (currentGameMode == GameMode.COOP) game.setScreen(new CoopLobbyScreen(game));
        else {
            if (playerDead) game.setScreen(new GameScreen(game, GameMode.SOLO, currentLevel));
            else if (currentLevel < StoryChapterConfig.TOTAL_STAGES) {
                game.selectedLevel = currentLevel + 1;
                game.setScreen(new GameScreen(game, GameMode.SOLO, currentLevel + 1));
            }
            else {
                if (NetworkManager.getInstance() != null) {
                    NetworkManager.getInstance().updateMyPresence("ONLINE", "", "");
                }
                game.setScreen(new MainMenuScreen(game));
            }
        }
    }

    private void exitToMenu() {
        if (isDisposed) return;
        grantMatchRewards();
        if (currentGameMode == GameMode.SOLO && story != null && !freePlay && !playerDead && !isMatchEnded) {
            float cpX = (activeCheckpointX > 1.0f) ? activeCheckpointX : (playerBody != null ? playerBody.getPosition().x : 10.0f);
            StorySaveManager.saveProgress(story.chapter, story.stage,
                selectedChar, playerHp, 0, score, healthPotions, cpX);
        }
        cleanSocketListeners();
        NetworkManager nm = NetworkManager.getInstance();
        if (currentGameMode == GameMode.PVP || currentGameMode == GameMode.COOP || currentGameMode == GameMode.BATTLE_5V5) {
            nm.leaveRoom(); // rời phòng thật sự, không tự vào lại phòng cũ ở lần sau
        } else {
            nm.updateMyPresence("ONLINE", "", "");
        }
        game.setScreen(new MainMenuScreen(game));
    }

    private void init5v5Fighters() {
        fighters5v5.clear();
        // Đấu trường thuần kỹ năng: KHÔNG tự động sinh BOT!
        // Chỉ người chơi thực sự tham gia trận đấu (1v1, 2v2, 3v3...).
    }

    private void createFighterBody(Fighter5v5 f, float startX, float startY) {
        BodyDef bdef = new BodyDef();
        bdef.position.set(startX, startY);
        bdef.type = BodyDef.BodyType.DynamicBody;
        Body b = world.createBody(bdef);
        b.setFixedRotation(true);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.20f, 0.40f);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 1.0f;
        fdef.friction = 0.3f;
        fdef.filter.categoryBits = CAT_FIGHTER;
        fdef.filter.maskBits = CAT_WORLD; // chỉ đứng trên sàn/tường, đi xuyên qua người chơi & bot khác như MOBA
        b.createFixture(fdef).setUserData("fighter5v5");
        shape.dispose();

        f.body = b;
    }

    private void on5v5Kill(String killerName, int killerTeam, String victimName, int victimTeam) {
        if (isMatchEnded) return;

        if (killerTeam == 1) blueKills++;
        else if (killerTeam == 2) redKills++;

        if (lblScore5v5 != null) {
            lblScore5v5.setText("[XANH] " + blueKills + "  -  " + redKills + " [ĐỎ]  (MỤC TIÊU: " + WIN_KILLS_5V5 + ")");
        }

        int pTeam = (game != null) ? game.selectedTeam : 1;
        String myName = (game != null && game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần";

        if (killerName.equals(myName)) {
            playerMultiKill++;
            multiKillWindow = 5.0f;
            killStreakTimer = 3.5f;
            String announcement = "HẠ GỤC: " + victimName;
            if (playerMultiKill == 2) announcement = ">> NHỊ SÁT! <<";
            else if (playerMultiKill == 3) announcement = ">>> TAM SÁT! <<<";
            else if (playerMultiKill == 4) announcement = ">>>> TỨ SÁT! <<<<";
            else if (playerMultiKill >= 5) announcement = "===== NGŨ SÁT! QUÉT SẠCH! =====";

            if (lblKillStreak != null) {
                lblKillStreak.setText(announcement);
            }
            actionLog.setText("BẠN ĐÃ HẠ GỤC " + victimName + "!");
            SoundManager.getInstance().playHeavyHit();
        } else {
            actionLog.setText(killerName + " ĐÃ HẠ GỤC " + victimName + "!");
        }

        if (blueKills >= WIN_KILLS_5V5 || redKills >= WIN_KILLS_5V5) {
            boolean isWinner = (pTeam == 1 && blueKills >= WIN_KILLS_5V5) || (pTeam == 2 && redKills >= WIN_KILLS_5V5);
            showGameOverDialog(isWinner);
        }
    }

    private void onTitanSlain(int killerTeam) {
        int pTeam = (game != null) ? game.selectedTeam : 1;
        String teamName = (killerTeam == 1) ? "PHE ÁNH SÁNG (XANH)" : "PHE BÓNG TỐI (ĐỎ)";
        actionLog.setText(teamName + " ĐÃ HẠ THẦN THÚ BẠO CHÚA! CẢ ĐỘI NHẬN BÙA DIỆT THÚ!");
        addCustomText(30.0f, 6.8f, "BÙA DIỆT THÚ!", Color.GOLD);
        cameraShakeTime = 0.50f; cameraShakeIntensity = 0.35f;
        SoundManager.getInstance().playUltimateP1();

        // Ban thuong Bua Than Thu cho toan bo thanh vien phe tieu diet
        for (int i = 0; i < fighters5v5.size; i++) {
            Fighter5v5 f = fighters5v5.get(i);
            if (f.team == killerTeam && !f.isDead) {
                f.hasTitanBuff = true;
                f.titanBuffTimer = 35.0f;
                f.shield = Math.min(650, f.shield + 250);
            }
        }
        if (pTeam == killerTeam && !playerDead && playerBody != null) {
            playerHasTitanBuff = true;
            playerTitanBuffTimer = 35.0f;
            playerShield = Math.min(650, playerShield + 250);
            addCustomText(playerBody.getPosition().x, playerBody.getPosition().y + 0.9f, "+35% SÁT THƯƠNG & +250 KHIÊN!", Color.GOLD);
        }
    }

    private void onTurretDestroyed(OutpostTurret turret, int killerTeam) {
        int pTeam = (game != null) ? game.selectedTeam : 1;
        String tName = (turret.team == 1) ? "TIỀN ĐỒN ÁNH SÁNG" : "TIỀN ĐỒN BÓNG TỐI";
        actionLog.setText(tName + " ĐÃ BỊ CÔNG PHÁ HOÀN TOÀN!");
        addCustomText(turret.x, turret.y + 1.2f, "THÁP BỊ HẠ!", Color.ORANGE);
        cameraShakeTime = 0.35f; cameraShakeIntensity = 0.25f;
        SoundManager.getInstance().playHeavyHit();
        spawnHitSparks(turret.x, turret.y + 0.8f, 30, Color.ORANGE);

        if (pTeam == killerTeam) {
            goldEarned += 100;
            score += 250;
            updateScoreGoldUI();
            addCustomText(turret.x, turret.y + 1.6f, "+100 VÀNG!", Color.GOLD);
        }
    }

    private void revivePartner() {
        if (currentGameMode != GameMode.COOP || partnerLeft || reviveCooldownTimer > 0f) return;
        if (socket == null || !socket.connected()) {
            actionLog.setText("MẤT KẾT NỐI MÁY CHỦ! KHÔNG THỂ HỒI SINH LÚC NÀY.");
            return;
        }
        if (goldEarned >= 50 && remoteHp <= 0) {
            goldEarned -= 50;
            reviveCooldownTimer = 2.0f;
            if (btnRevivePartner != null) btnRevivePartner.setVisible(false);
            updateScoreGoldUI();
            actionLog.setText("ĐÃ CỨU ĐỒNG ĐỘI (-50 VÀNG)!");
            try {
                JSONObject packet = new JSONObject();
                packet.put("type", "revive");
                packet.put("damage", 0);
                packet.put("index", 0);
                socket.emit("playerAction", packet);
            } catch (Exception ignored) {}
        }
    }

    private void triggerAssassinate() {
        if (isAssassinating || playerDead || isMatchEnded || inCutscene() || isPaused
            || playerStunTimer > 0f || isGrabbedByPirate || isEagleStriking) return;

        float targetX = 0f, targetY = 0f;
        boolean found = false;
        pendingAssassinateMob = null;
        pendingAssassinateFighter = null;
        pendingAssassinateRemote = false;
        pendingAssassinatePeerId = null;

        if (targetToAssassinate != null && !targetToAssassinate.isDead && targetToAssassinate.body != null) {
            pendingAssassinateMob = targetToAssassinate;
            targetX = targetToAssassinate.body.getPosition().x;
            targetY = targetToAssassinate.body.getPosition().y;
            found = true;
        } else if (targetFighterToAssassinate != null && !targetFighterToAssassinate.isDead && targetFighterToAssassinate.body != null) {
            pendingAssassinateFighter = targetFighterToAssassinate;
            targetX = targetFighterToAssassinate.body.getPosition().x;
            targetY = targetFighterToAssassinate.body.getPosition().y;
            found = true;
        } else if (targetRemoteToAssassinate && !remoteDead && remoteX != -999f) {
            pendingAssassinateRemote = true;
            targetX = remoteX;
            targetY = remoteY;
            found = true;
        } else if (targetAssassinatePeerId != null) {
            RemotePeer5v5 ep = findExtraPeer(targetAssassinatePeerId);
            if (ep != null && !ep.dead && ep.x != -999f) {
                pendingAssassinatePeerId = ep.id;
                targetX = ep.x;
                targetY = ep.y;
                found = true;
            }
        }

        if (!found) return;

        assassinateVictimX = targetX;
        assassinateVictimY = targetY;

        // Quay mặt về phía nạn nhân và khóa vị trí
        if (playerBody != null) {
            float px = playerBody.getPosition().x;
            playerFacing = (targetX >= px) ? 1 : -1;
            playerBody.setLinearVelocity(0, 0);
        }

        // Bắt đầu chuỗi ám sát điện ảnh
        isAssassinating = true;
        assassinateExecuted = false;

        // Bật animation chém kiếm thường
        attackCombo = 1;
        attackTime = 0f;
        hitThisAttack = true; // Tránh vòng lặp đánh thường gây sát thương sớm

        // Kích hoạt khoảnh khắc điện ảnh ám sát: Zoom cận cảnh + Tua chậm
        cinematicAssassinateTimer = 1.0f;
        assassinateFocusX = targetX;
        assassinateFocusY = targetY + 0.3f;

        // Âm thanh vung kiếm chém
        SoundManager.getInstance().playSlash();
        actionLog.setText("ÁM SÁT ĐIỆN ẢNH! VUNG KIẾM KẾT LIỄU...");

        if (btnAssassinate != null) btnAssassinate.setVisible(false);
        targetToAssassinate = null;
        targetFighterToAssassinate = null;
        targetRemoteToAssassinate = false;
        targetAssassinatePeerId = null;
    }

    private void shootCannonAtImperial() {
        if (!imperialAlive || playerDead || isMatchEnded || inCutscene() || isPaused
            || playerStunTimer > 0f || isGrabbedByPirate || isEagleStriking) return;
        Vector2 dir = new Vector2(imperialX - cannonPos.x, imperialY - cannonPos.y).nor();
        projectiles.add(new Projectile(cannonPos.x + 0.5f, cannonPos.y + 0.3f,
            dir.x * 16f, dir.y * 16f, 999, false, true, false, false));
        actionLog.setText("KHAI HỎA PHÁO THẦN CÔNG!");
        cameraShakeTime = 0.4f;
        cameraShakeIntensity = 0.3f;
    }

    private void triggerBoyRescueEvent() {
        boyState = BoyState.FLEEING;
        boyHp = boyMaxHp;

        float spawnX = 22.0f;
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (p.isGround && p.x > 18.0f && p.x < mapWidth - 20.0f) {
                spawnX = p.x + p.width / 2f;
                break;
            }
        }
        spawnX = snapToOpenGround(spawnX, 1.0f);
        boyFacing = 1;
        createBoyBody(spawnX, 1.2f);

        // Spawn 2 Guard Mobs guarding the Orphan Boy!
        spawnGroundMob(false, spawnX - 2.2f);
        boyGuard1 = activeMonsters.peek();

        spawnGroundMob(false, spawnX + 2.2f);
        boyGuard2 = activeMonsters.peek();

        actionLog.setText(story != null ? "Tiểu Vũ bị 2 tên lính áp giải! Hạ chúng để cứu cậu bé!"
            : "CẬU BÉ BỊ 2 TÊN LÍNH CANH GIỮ! HẠ CẢ HAI ĐỂ CỨU CẬU BÉ!");
    }

    private void updateSkyEntities(float delta) {
        if (currentGameMode == GameMode.PVP || currentGameMode == GameMode.BATTLE_5V5 || isMatchEnded || storyDialogueActive || shopFreezesWorld()) return;

        Vector2 pPos = playerBody.getPosition();

        // Sky pirate activation
        if (pPos.x >= 28.0f && !hasSkyPirate && (story == null || story.hasSkyPirates)) {
            hasSkyPirate = true;
            pirateRespawnTimer = 2.0f;
        }

        if (hasSkyPirate) {
            if (pirateState == PirateState.FALLING) {
                pirateY -= 7.5f * delta; // Falling from sky to ground
                if (pirateY <= 1.25f) {
                    pirateY = 1.25f;
                    pirateState = PirateState.DEAD;
                }
            } else if (pirateState == PirateState.DEAD) {
                pirateDeathTimer -= delta;
                if (pirateDeathTimer <= 0) {
                    pirateState = PirateState.PATROL;
                    pirateRespawnTimer = 20.0f;
                }
            } else if (!pirateAlive) {
                pirateRespawnTimer -= delta;
                if (pirateRespawnTimer <= 0) {
                    pirateAlive = true;
                    pirateHp = 140;
                    pirateX = pPos.x + MathUtils.random(-5f, 5f);
                    pirateY = 6.2f;
                    pirateState = PirateState.PATROL;
                    pirateAlertTimer = 3.0f;
                }
            } else {
                pirateFacing = (pPos.x > pirateX) ? 1 : -1;
                boolean playerCovered = isPlayerCoveredUnderPlank();

                switch (pirateState) {
                    case PATROL:
                        pirateX += MathUtils.sin(stateTime * 1.5f) * 2.2f * delta;
                        pirateY = 6.0f + MathUtils.sin(stateTime * 2.5f) * 0.4f;
                        if (!playerCovered && !playerDead && !isEagleStriking) {
                            pirateState = PirateState.ALERT;
                            pirateAlertTimer = 3.0f;
                        }
                        break;
                    case ALERT:
                        pirateX += MathUtils.sin(stateTime * 1.5f) * 1.5f * delta;
                        if (playerDead || playerCovered) {
                            pirateState = PirateState.PATROL;
                            if (!playerDead) actionLog.setText("Đã nấp dưới mái! Không Tặc bỏ qua.");
                        } else {
                            pirateAlertTimer -= delta;
                            actionLog.setText("KHÔNG TẶC PHÁT HIỆN! CÒN " +
                                Math.max(0, (int)Math.ceil(pirateAlertTimer)) + "S ĐỂ NẤP!");
                            if (pirateAlertTimer <= 0) {
                                pirateState = PirateState.DIVE;
                                actionLog.setText("KHÔNG TẶC LAO XUỐNG TÓM BẠN!");
                            }
                        }
                        break;
                    case DIVE:
                        if (playerDead || playerCovered) {
                            pirateState = PirateState.PATROL;
                            break;
                        }
                        Vector2 toPlayer = new Vector2(pPos.x - pirateX, pPos.y - pirateY);
                        float dist = toPlayer.len();
                        toPlayer.nor();
                        pirateX += toPlayer.x * 6.5f * delta;
                        pirateY += toPlayer.y * 6.5f * delta;
                        if (dist <= 0.85f) {
                            resetTraversalState();
                            pirateState = PirateState.CARRY;
                            isGrabbedByPirate = true;
                            pirateCarryTimer = 1.0f;
                            actionLog.setText("BỊ TÓM! CÓ 1 GIÂY ĐỂ CHÉM TRẢ!");
                        }
                        break;
                    case CARRY:
                        if (playerDead) {
                            isGrabbedByPirate = false;
                            pirateState = PirateState.PATROL;
                            break;
                        }
                        pirateY += 3.2f * delta;
                        playerBody.setTransform(pirateX, pirateY - 0.6f, 0);
                        playerBody.setLinearVelocity(0, 0);
                        pirateCarryTimer -= delta;
                        if (pirateCarryTimer <= 0 || pirateY >= 7.2f) {
                            isGrabbedByPirate = false;
                            isFallingFromSky = true;
                            pirateState = PirateState.PATROL;
                            pirateAlertTimer = 3.0f;
                            actionLog.setText("BỊ THẢ RƠI TỪ TRÊN TRỜI!");
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        // Imperial soldier activation
        if (pPos.x >= 45.0f && !hasImperial && (story == null || story.hasFlyingMobs)) {
            hasImperial = true;
            imperialRespawnTimer = 2.0f;
        }

        if (hasImperial) {
            if ("FALLING".equals(imperialState)) {
                imperialY -= 7.5f * delta; // Falling from sky to ground
                if (imperialY <= 1.25f) {
                    imperialY = 1.25f;
                    imperialState = "DEAD";
                }
            } else if ("DEAD".equals(imperialState)) {
                imperialDeathTimer -= delta;
                if (imperialDeathTimer <= 0) {
                    imperialState = "FLY";
                    imperialRespawnTimer = 22.0f; // Respawn cooldown
                }
            } else if (!imperialAlive) {
                imperialRespawnTimer -= delta;
                if (imperialRespawnTimer <= 0) {
                    imperialAlive = true;
                    imperialState = "FLY";
                    imperialHp = 250;
                    imperialX = 58.0f;
                    imperialY = 6.2f;
                    actionLog.setText("LÍNH BAY TRIỀU ĐÌNH XUẤT HIỆN! Lên vọng đài dùng pháo bắn hạ!");
                }
            } else { // Imperial Alive & Flying
                imperialFacing = (pPos.x > imperialX) ? 1 : -1;
                imperialX += MathUtils.cos(stateTime * 1.0f) * 1.8f * delta;
                imperialShootCooldown -= delta;

                // 2-Beat Ranged Attack Animations:
                if (imperialShootCooldown <= 0.8f && imperialShootCooldown > 0.3f) {
                    imperialState = "ATTACK_PREP"; // Aiming/preparing -> trieudinhban1.png
                } else if (imperialShootCooldown <= 0.3f && imperialShootCooldown > 0f) {
                    imperialState = "ATTACK_FIRE"; // Firing -> trieudinhban2.png
                } else {
                    imperialState = "FLY"; // Flying / Hovering -> trieudinhbay1 & bay2
                }

                if (imperialShootCooldown <= 0 && !playerDead) {
                    imperialShootCooldown = 3.2f;
                    imperialSkillCounter++;
                    Vector2 baseDir = new Vector2(pPos.x - imperialX, pPos.y - imperialY).nor();
                    if (imperialSkillCounter % 2 == 1) {
                        projectiles.add(new Projectile(imperialX, imperialY,
                            baseDir.x * 5.0f, baseDir.y * 5.0f, 18, false, false, false, false));
                        projectiles.add(new Projectile(imperialX, imperialY,
                            baseDir.x * 5.0f - 0.8f, baseDir.y * 5.0f + 0.8f, 18, false, false, false, false));
                        projectiles.add(new Projectile(imperialX, imperialY,
                            baseDir.x * 5.0f + 0.8f, baseDir.y * 5.0f - 0.8f, 18, false, false, false, false));
                    } else {
                        projectiles.add(new Projectile(imperialX, imperialY,
                            baseDir.x * 6.5f, baseDir.y * 6.5f, 35, false, false, false, false));
                    }
                }
            }
        }
    }

    private void alertNearbyMobs(float srcX, float srcY, float radius) {
        for (int i = 0; i < activeMonsters.size; i++) {
            GroundMob ally = activeMonsters.get(i);
            if (ally == null || ally.isDead || ally.body == null) continue;
            if ("PATROL".equals(ally.state)) {
                Vector2 aPos = ally.body.getPosition();
                if (Math.abs(aPos.x - srcX) <= radius && Math.abs(aPos.y - srcY) <= 2.4f) {
                    ally.state = "CHASE";
                    ally.alertTimer = Math.max(ally.alertTimer, 5.0f);
                    ally.patrolWaitTimer = 0f;
                    if (Math.abs(srcX - aPos.x) > 0.1f) {
                        ally.facing = (srcX > aPos.x) ? 1 : -1;
                    }
                }
            }
        }
    }

    private void updateGroundMobs(float delta) {
        if (currentGameMode == GameMode.PVP || currentGameMode == GameMode.BATTLE_5V5 || isMatchEnded) return;

        Vector2 pPos = playerBody.getPosition();

        // Cậu bé: CO-OP giữ như cũ; chơi đơn chỉ ở ải "giải cứu" (ải hộ tống thì cậu bé đi cùng từ đầu)
        if (pPos.x >= 20.0f && boyState == BoyState.INACTIVE
            && (story == null || story.boyMode == StoryChapterConfig.BoyMode.RESCUE)) {
            triggerBoyRescueEvent();
        }

        // FIX: reverse-index loop to safely remove dead monsters
        for (int i = activeMonsters.size - 1; i >= 0; i--) {
            GroundMob m = activeMonsters.get(i);
            if (m.body == null) continue;

            // Quái rơi xuống vực: loại bỏ, KHÔNG cộng điểm/vàng/nộ
            if (m.body.getPosition().y < -1.5f && !m.isDead) {
                m.isDead = true;
                m.hp = 0;
                m.state = "DEAD";
                m.deathTimer = 0f;
                continue;
            }

            if (m.isDead) {
                m.deathTimer -= delta;
                m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                if (m.deathTimer <= 0) {
                    world.destroyBody(m.body);
                    m.body = null;
                    activeMonsters.removeIndex(i);
                }
                continue;
            }

            if (m.jumpCooldown > 0f) m.jumpCooldown -= delta;
            if (m.slowTimer > 0f) m.slowTimer -= delta;

            // Sát thương độc rút máu từ kỹ năng của Vân U
            if (m.poisonTimer > 0f && !inCutscene()) {
                m.poisonTimer -= delta;
                m.poisonTickAccum += delta;
                if (m.poisonTickAccum >= 0.5f) {
                    m.poisonTickAccum -= 0.5f;
                    Vector2 mp = m.body.getPosition();
                    spawnHitSparks(mp.x, mp.y + 0.35f, 5, Color.PURPLE);
                    damageGroundMob(m, 18, true, true);
                    if (m.isDead) continue;
                }
            }

            if (m.hitTimer > 0) {
                m.hitTimer -= delta;
                m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                if (m.hitTimer <= 0 && "HIT".equals(m.state)) {
                    m.state = "CHASE";
                    m.alertTimer = Math.max(m.alertTimer, 5.5f);
                }
                continue;
            }

            if (inCutscene()) {
                m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                continue;
            }

            Vector2 mPos = m.body.getPosition();
            float distToPlayer = pPos.x - mPos.x;
            float absDist = Math.abs(distToPlayer);
            m.animTime += delta;
            if (m.atkCooldown > 0) m.atkCooldown -= delta;

            // Monster attacks boy (chỉ khi Tiểu Vũ đã được giải cứu & quái đang giao chiến — tránh lính gác giết cậu bé đứng yên!)
            if (boyState == BoyState.RESCUED && boyBody != null
                && ("CHASE".equals(m.state) || "ATTACK".equals(m.state))) {
                float distToBoy = Math.abs(boyBody.getPosition().x - mPos.x);
                if (distToBoy < 1.15f && Math.abs(boyBody.getPosition().y - mPos.y) < 1.1f && m.atkCooldown <= 0) {
                    boyHp = Math.max(0, boyHp - 15);
                    boyHitTimer = 0.22f;
                    addDamageText(boyBody.getPosition().x, boyBody.getPosition().y + 0.4f, 15, Color.RED);
                    m.atkCooldown = 1.6f;
                    if (boyHp <= 0) {
                        float deathX = boyBody.getPosition().x;
                        float deathY = boyBody.getPosition().y;
                        boyState = BoyState.DEAD;
                        world.destroyBody(boyBody);
                        boyBody = null;
                        actionLog.setText("CẬU BÉ ĐÃ HY SINH!");
                        addCustomText(deathX, deathY, "CẬU BÉ ĐÃ NGÃ XUỐNG!", Color.RED);
                    }
                }
            }

            // Boss Berserk & Ground Smash mechanics
            if (m.isBoss) {
                m.berserkAuraTime += delta;

                if (m.isChargingBerserk) {
                    m.berserkChargeTimer -= delta;
                    m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                    if (MathUtils.randomBoolean(0.45f)) {
                        spawnHitSparks(mPos.x + MathUtils.random(-0.5f, 0.5f), mPos.y + MathUtils.random(0.1f, 1.2f), 8, Color.RED);
                    }
                    if (m.berserkChargeTimer <= 0) {
                        m.isChargingBerserk = false;
                        m.isBerserk = true;
                        m.berserkAttackTimer = 10.0f;
                        m.isResting = false;
                        m.state = "CHASE";
                        m.alertTimer = 8.0f;
                        m.atkCooldown = 0.2f;
                        SoundManager.getInstance().playBossSmash();
                        cameraShakeTime = 0.7f;
                        cameraShakeIntensity = 0.40f;
                        actionLog.setText("THỦ LĨNH GẦM LÊN – CUỒNG HUYẾT!");
                        addCustomText(mPos.x, mPos.y + 1.3f, "CUỒNG HUYẾT BÙNG PHÁT!", Color.RED);
                    }
                    continue;
                }

                if (m.isBerserk) {
                    if (m.isResting) {
                        m.berserkRestTimer -= delta;
                        m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                        if (m.berserkRestTimer <= 0) {
                            m.isResting = false;
                            m.berserkAttackTimer = 10.0f;
                            m.state = "CHASE";
                            m.alertTimer = 8.0f;
                            m.atkCooldown = 0.4f;
                            actionLog.setText("THỦ LĨNH HỒI SỨC – TIẾP TỤC TẤN CÔNG!");
                            addCustomText(mPos.x, mPos.y + 1.2f, "TIẾP TỤC CUỒNG BẠO!", Color.ORANGE);
                        }
                        continue;
                    } else {
                        m.berserkAttackTimer -= delta;
                        if (m.berserkAttackTimer <= 0) {
                            m.isResting = true;
                            m.berserkRestTimer = 1.2f;
                            m.state = "RESTING";
                            m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                            actionLog.setText("THỦ LĨNH KIỆT SỨC (1,2 GIÂY) – CƠ HỘI TẤN CÔNG!");
                            addCustomText(mPos.x, mPos.y + 1.2f, "THỞ DỐC (1,2 GIÂY)!", Color.CYAN);
                            continue;
                        }
                    }
                }

                float smashRate = m.isBerserk ? 1.55f : 1.0f;
                m.bossSmashCooldown -= delta * smashRate;
                if (m.bossSmashCooldown <= 0 && !"SMASH_WINDUP".equals(m.state) && !m.isChargingBerserk && !m.isResting) {
                    m.state = "SMASH_WINDUP";
                    m.animTime = 0f;
                    actionLog.setText("THỦ LĨNH GIƠ CHÙY ĐẬP ĐẤT – NHẢY LÊN ĐỂ NÉ!");
                }
            }

            if ("SMASH_WINDUP".equals(m.state)) {
                m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                float windupLimit = m.isBerserk ? 0.55f : 0.8f;
                if (m.animTime >= windupLimit) {
                    cameraShakeTime = 0.6f;
                    cameraShakeIntensity = 0.45f;
                    SoundManager.getInstance().playBossSmash();
                    spawnHitSparks(mPos.x, mPos.y - 0.5f, 25, new Color(1f, 0.5f, 0.1f, 1f));
                    if (absDist <= 6.8f && Math.abs(pPos.y - mPos.y) <= 1.8f) {
                        if (isGrounded && !playerDead) {
                            int smashDmg = absorbShield(m.isBerserk ? 65 : 45);
                            playerHp = Math.max(0, playerHp - smashDmg);
                            playerHpBar.setHp(playerHp);
                            lblPlayerHp.setText("Máu: " + playerHp + " HP");
                            playerStunTimer = 1.8f;
                            playerHitFlashTimer = 0.28f;
                            SoundManager.getInstance().playHurt();
                            addDamageText(pPos.x, pPos.y + 0.4f, smashDmg, Color.RED);
                            spawnHitSparks(pPos.x, pPos.y + 0.4f, 12, Color.RED);
                            addCustomText(pPos.x, pPos.y + 0.9f, "CHOÁNG!", Color.YELLOW);
                            if (playerHp <= 0) { playerDead = true; showGameOverDialog(false); }
                        } else {
                            actionLog.setText("NHẢY NÉ CÚ ĐẬP ĐẤT THÀNH CÔNG!");
                            addCustomText(pPos.x, pPos.y + 0.6f, "NÉ ĐƯỢC!", Color.GREEN);
                        }
                    }
                    m.bossSmashCooldown = m.isBerserk ? 4.8f : 7.5f;
                    m.state = "CHASE";
                    m.alertTimer = 7.0f;
                }
                continue;
            }

            // AI phát hiện & giữ cảnh giác thông minh (vẫn cho phép ám sát lén phía sau khi PATROL)
            if (!"ATTACK".equals(m.state)) {
                boolean playerHidden = isP2StealthActive || playerInBush || playerDead;
                boolean isFacingPlayer = (m.facing == 1 && distToPlayer > 0) || (m.facing == -1 && distToPlayer < 0);
                float detectRange = (m.isBoss && m.isBerserk) ? 7.0f : (m.isBoss ? 5.2f : (isCrouching ? 2.2f : 3.8f));

                if ("PATROL".equals(m.state)) {
                    if (!playerHidden && isFacingPlayer && absDist <= detectRange && Math.abs(pPos.y - mPos.y) < 2.5f) {
                        m.state = "CHASE";
                        m.alertTimer = 5.5f;
                        m.turnDelayTimer = 0f;
                        m.facing = (distToPlayer >= 0) ? 1 : -1;
                        alertNearbyMobs(pPos.x, pPos.y, 5.0f);
                    }
                } else if ("CHASE".equals(m.state)) {
                    if (m.alertTimer > 0f) m.alertTimer -= delta;
                    if (playerHidden || (absDist > 9.5f && m.alertTimer <= 0f)) {
                        m.state = "PATROL";
                        m.turnDelayTimer = 0f;
                    } else if (absDist <= 6.5f && Math.abs(pPos.y - mPos.y) < 3.4f) {
                        m.alertTimer = Math.max(m.alertTimer, 2.8f);
                    }
                    if ("CHASE".equals(m.state)) {
                        if (!isFacingPlayer && absDist > 0.22f) {
                            m.turnDelayTimer += delta;
                            if (m.turnDelayTimer >= (m.isBoss ? 0.10f : 0.16f)) {
                                m.facing = (distToPlayer >= 0) ? 1 : -1;
                                m.turnDelayTimer = 0f;
                            }
                        } else {
                            m.turnDelayTimer = 0f;
                        }
                    }
                }
            }

            if ("ATTACK".equals(m.state)) {
                float damp = (float) Math.pow(0.86, delta * 60.0);
                m.body.setLinearVelocity(m.body.getLinearVelocity().x * damp, m.body.getLinearVelocity().y);
                float triggerTime = (m.isBoss && m.isBerserk) ? 0.22f : 0.30f;
                if (!m.hitThisAtk && m.animTime >= triggerTime) {
                    float hitRange = m.isBoss ? (m.isBerserk ? 1.85f : 1.6f) : 1.35f;
                    int baseDmg = m.isBoss ? (currentGameMode == GameMode.COOP ? 50 : 35) : (currentGameMode == GameMode.COOP ? 25 : 15);
                    int dmg = (m.isBoss && m.isBerserk) ? (int)(baseDmg * 1.70f) : baseDmg;
                    boolean mobFacingTarget = (m.facing == 1 && distToPlayer >= -0.2f) || (m.facing == -1 && distToPlayer <= 0.2f);
                    if (mobFacingTarget && absDist <= hitRange && Math.abs(pPos.y - mPos.y) < 1.35f && !playerDead) {
                        boolean isFacingEnemy = (playerFacing == 1 && distToPlayer < 0) || (playerFacing == -1 && distToPlayer > 0);
                        if (isCrouching && isFacingEnemy) {
                            m.state = "HIT";
                            m.hitTimer = (m.isBoss && m.isBerserk) ? 0.5f : 1.0f;
                            m.animTime = 0f;
                            SoundManager.getInstance().playHeavyHit();
                            cameraShakeTime = 0.35f;
                            cameraShakeIntensity = 0.25f;
                            hitStopTimer = 0.08f;
                            addCustomText(pPos.x, pPos.y + 0.8f, "PHẢN ĐÒN!", Color.GREEN);
                            actionLog.setText("PHẢN ĐÒN XUẤT THẦN! ĐỠ ĐÒN VÀ GÂY SÁT THƯƠNG!");
                            spawnHitSparks(pPos.x + playerFacing * 0.5f, pPos.y + 0.5f, 20, Color.CYAN);
                            damageGroundMob(m, 60 + swordDamageBonus, true);
                        } else {
                            int actualDmg = absorbShield(dmg);
                            playerHp = Math.max(0, playerHp - actualDmg);
                            playerHpBar.setHp(playerHp);
                            lblPlayerHp.setText("Máu: " + playerHp + " HP");
                            playerHitFlashTimer = 0.20f;
                            SoundManager.getInstance().playMonsterSlash();
                            SoundManager.getInstance().playHurt();
                            cameraShakeTime = 0.25f;
                            cameraShakeIntensity = 0.20f;
                            addDamageText(pPos.x, pPos.y + 0.4f, actualDmg, Color.RED);
                            spawnHitSparks(pPos.x, pPos.y + 0.4f, 10, Color.RED);
                            if (playerHp <= 0) { playerDead = true; showGameOverDialog(false); }
                        }
                    }
                    m.hitThisAtk = true;
                }
                float attackAnimLimit = (m.isBoss && m.isBerserk) ? 0.38f : 0.52f;
                if (m.animTime >= attackAnimLimit) {
                    m.state = "CHASE";
                    m.atkCooldown = m.isBoss ? (m.isBerserk ? 0.65f : 1.45f) : 1.35f;
                }
            } else if ("CHASE".equals(m.state)) {
                if (Math.abs(mPos.x - m.lastX) < 0.015f) m.stuckTimer += delta;
                else m.stuckTimer = 0f;
                m.lastX = mPos.x;

                float attackDist = m.isBoss ? (m.isBerserk ? 1.65f : 1.45f) : 1.22f;
                if (absDist <= attackDist && Math.abs(pPos.y - mPos.y) < 1.4f) {
                    if (m.atkCooldown <= 0 && !playerDead) {
                        m.facing = (distToPlayer >= 0) ? 1 : -1;
                        m.state = "ATTACK";
                        m.animTime = 0f;
                        m.hitThisAtk = false;
                        m.body.setLinearVelocity(m.facing * (m.isBoss ? 2.4f : 1.9f), m.body.getLinearVelocity().y);
                    } else if (!m.isBoss && absDist < 0.62f) {
                        m.body.setLinearVelocity(-m.facing * 0.85f, m.body.getLinearVelocity().y);
                    } else {
                        m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                    }
                } else {
                    float baseSpeed = m.isBoss ? (m.isBerserk ? 2.85f : 1.65f) : 1.68f;
                    float speed = baseSpeed * (currentGameMode == GameMode.COOP ? 1.20f : 1.0f) * (m.slowTimer > 0f ? 0.55f : 1.0f);
                    boolean onSolidGround = Math.abs(m.body.getLinearVelocity().y) < 0.15f;
                    boolean edgeAhead = mPos.y < 2.0f && groundAt(mPos.x + m.facing * (m.isBoss ? 0.9f : 0.55f)) == null;

                    if (m.isBoss && onSolidGround && m.jumpCooldown <= 0f && absDist >= 3.4f && absDist <= 6.2f
                        && groundAt(mPos.x + m.facing * 2.8f) != null) {
                        m.body.setLinearVelocity(m.facing * 4.2f, 5.9f);
                        m.jumpCooldown = 2.5f;
                        m.stuckTimer = 0f;
                    } else if (onSolidGround && m.jumpCooldown <= 0f) {
                        boolean playerAbove = (pPos.y - mPos.y > 0.90f && pPos.y - mPos.y < 3.2f && absDist < 4.6f);
                        boolean blockedByObstacle = m.stuckTimer > 0.22f;
                        boolean canJumpGap = edgeAhead && groundAt(mPos.x + m.facing * 2.7f) != null;
                        if (playerAbove || blockedByObstacle || canJumpGap) {
                            m.body.setLinearVelocity(m.facing * Math.max(speed, 2.5f), m.isBoss ? 6.6f : 6.2f);
                            m.jumpCooldown = 1.15f;
                            m.stuckTimer = 0f;
                        } else {
                            m.body.setLinearVelocity(edgeAhead ? 0f : m.facing * speed, m.body.getLinearVelocity().y);
                        }
                    } else {
                        m.body.setLinearVelocity(edgeAhead && onSolidGround ? 0f : m.facing * speed, m.body.getLinearVelocity().y);
                    }
                }
            } else { // PATROL
                if (m.patrolWaitTimer > 0) {
                    m.patrolWaitTimer -= delta;
                    m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                } else {
                    if (mPos.x <= m.patrolLeft) {
                        m.facing = 1;
                        m.patrolWaitTimer = MathUtils.random(1.5f, 2.5f);
                        m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                    } else if (mPos.x >= m.patrolRight) {
                        m.facing = -1;
                        m.patrolWaitTimer = MathUtils.random(1.5f, 2.5f);
                        m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                    } else {
                        m.body.setLinearVelocity(m.facing * 0.78f, m.body.getLinearVelocity().y);
                    }
                }
            }
        }

        // Boy AI: leo thang, leo tường, bật nhảy lên bục cao và nhảy xuống đi theo nhân vật chính
        if (storyDialogueActive && boyBody != null) {
            boyIsClimbing = false;
            boyBody.setGravityScale(1f);
            boyBody.setLinearVelocity(0, boyBody.getLinearVelocity().y);
        }
        if (!storyDialogueActive && boyState != BoyState.INACTIVE && boyState != BoyState.DEAD && boyBody != null) {
            if (boyJumpCooldown > 0f) boyJumpCooldown -= delta;
            if (boyDropThroughTimer > 0f) boyDropThroughTimer -= delta;
            Vector2 bPos = boyBody.getPosition();

            if (boyState == BoyState.FLEEING) {
                boyIsClimbing = false;
                boyBody.setGravityScale(1f);
                boyFacing = (pPos.x > bPos.x) ? 1 : -1;
                boyBody.setLinearVelocity(0, boyBody.getLinearVelocity().y);

                boolean g1Dead = (boyGuard1 == null || !activeMonsters.contains(boyGuard1, true) || boyGuard1.isDead);
                boolean g2Dead = (boyGuard2 == null || !activeMonsters.contains(boyGuard2, true) || boyGuard2.isDead);

                if (g1Dead && g2Dead) {
                    boyState = BoyState.RESCUED;
                    actionLog.setText(story != null ? "Đã cứu Tiểu Vũ! Cậu bé đi theo và bắn ná yểm trợ."
                        : "ĐÃ CỨU CẬU BÉ! CẬU BÉ ĐI THEO VÀ BẮN NÁ YỂM TRỢ!");
                }
            } else if (boyState == BoyState.RESCUED) {
                if (bPos.y < -1.0f || Math.abs(pPos.x - bPos.x) > 16.5f || Math.abs(pPos.y - bPos.y) > 7.5f) {
                    boyIsClimbing = false;
                    boyBody.setGravityScale(1f);
                    boyBody.setTransform(pPos.x - boyFacing * 1.3f, Math.max(1.05f, pPos.y + 0.25f), 0);
                    boyBody.setLinearVelocity(0, 0);
                    bPos = boyBody.getPosition();
                }

                float dyP = pPos.y - bPos.y;
                Ladder bestLadder = null;
                float bestLadderDist = 6.5f;
                MapWall bestWall = null;
                float bestWallDist = 5.0f;

                if (dyP > 0.85f || boyIsClimbing) {
                    for (int li = 0; li < laddersList.size; li++) {
                        Ladder l = laddersList.get(li);
                        float lcx = l.x + l.width * 0.5f;
                        float d = Math.abs(lcx - bPos.x);
                        if (d < bestLadderDist && bPos.y >= l.y - 0.5f && bPos.y <= l.y + l.height + 0.35f
                            && l.y + l.height > bPos.y + 0.2f) {
                            bestLadderDist = d;
                            bestLadder = l;
                        }
                    }
                    for (int wi = 0; wi < wallsList.size; wi++) {
                        MapWall w = wallsList.get(wi);
                        float wcx = w.x + w.width * 0.5f;
                        float d = Math.abs(wcx - bPos.x);
                        if (d < bestWallDist && bPos.y >= w.y - 0.4f && bPos.y <= w.y + w.height + 0.35f
                            && w.y + w.height > bPos.y + 0.2f) {
                            bestWallDist = d;
                            bestWall = w;
                        }
                    }
                }

                if (boyIsClimbing) {
                    boyBody.setGravityScale(0f);
                    float climbTargetX = bPos.x;
                    float topY = pPos.y;
                    if (bestLadder != null) {
                        climbTargetX = bestLadder.x + bestLadder.width * 0.5f;
                        topY = Math.min(pPos.y, bestLadder.y + bestLadder.height + 0.35f);
                    } else if (bestWall != null) {
                        climbTargetX = (bPos.x < bestWall.x + bestWall.width * 0.5f) ? (bestWall.x - 0.22f) : (bestWall.x + bestWall.width + 0.22f);
                        topY = bestWall.y + bestWall.height + 0.35f;
                    }
                    if (bPos.y >= topY - 0.08f || (bestLadder == null && bestWall == null) || dyP < -0.3f) {
                        boyIsClimbing = false;
                        boyBody.setGravityScale(1f);
                        boyFacing = (pPos.x >= bPos.x) ? 1 : -1;
                        boyBody.setLinearVelocity(boyFacing * 2.6f, 2.8f);
                        boyJumpCooldown = 0.35f;
                    } else {
                        float vxAlign = MathUtils.clamp((climbTargetX - bPos.x) * 6f, -2.2f, 2.2f);
                        boyBody.setLinearVelocity(vxAlign, 3.3f);
                    }
                } else {
                    boyBody.setGravityScale(1f);

                    float navTargetX = pPos.x;
                    if (dyP > 0.85f) {
                        if (bestLadder != null) navTargetX = bestLadder.x + bestLadder.width * 0.5f;
                        else if (bestWall != null) {
                            navTargetX = (bPos.x < bestWall.x + bestWall.width * 0.5f) ? (bestWall.x - 0.20f) : (bestWall.x + bestWall.width + 0.20f);
                        }
                    }

                    float distToGoal = navTargetX - bPos.x;
                    float absDistGoal = Math.abs(distToGoal);
                    boolean wantsMove = false;

                    if (dyP > 0.85f && bestLadder != null && absDistGoal < 0.42f) {
                        boyIsClimbing = true;
                        boyBody.setGravityScale(0f);
                        boyBody.setLinearVelocity(0f, 3.3f);
                    } else if (dyP > 0.85f && bestWall != null && absDistGoal < 0.45f) {
                        boyIsClimbing = true;
                        boyBody.setGravityScale(0f);
                        boyBody.setLinearVelocity(0f, 3.3f);
                    } else {
                        float stopDist = (navTargetX == pPos.x) ? 1.85f : 0.25f;
                        if (absDistGoal > 3.6f) {
                            boyFacing = (distToGoal >= 0) ? 1 : -1;
                            boyBody.setLinearVelocity(boyFacing * 4.0f, boyBody.getLinearVelocity().y);
                            wantsMove = true;
                        } else if (absDistGoal > stopDist) {
                            boyFacing = (distToGoal >= 0) ? 1 : -1;
                            boyBody.setLinearVelocity(boyFacing * 2.8f, boyBody.getLinearVelocity().y);
                            wantsMove = true;
                        } else {
                            boyBody.setLinearVelocity(0f, boyBody.getLinearVelocity().y);
                        }

                        if (wantsMove && Math.abs(bPos.x - boyLastX) < 0.015f) boyStuckTimer += delta;
                        else boyStuckTimer = 0f;
                        boyLastX = bPos.x;

                        // Nhảy xuống khỏi bục cao khi người chơi đã xuống đất
                        if (dyP < -1.15f && bPos.y > 1.45f) {
                            boyDropThroughTimer = 0.38f;
                            if (boyBody.getLinearVelocity().y > -1.2f) {
                                boyBody.setLinearVelocity(boyBody.getLinearVelocity().x, -1.6f);
                            }
                        }

                        // Nhảy qua vực, vượt chướng ngại hoặc bật nhảy lên bục cao theo nhân vật chính
                        boolean boyOnGround = Math.abs(boyBody.getLinearVelocity().y) < 0.15f;
                        boolean gapAhead = bPos.y < 1.8f && groundAt(bPos.x + boyFacing * 1.15f) == null;
                        boolean jumpToOverheadPlat = dyP > 0.85f && dyP <= 3.0f && Math.abs(pPos.x - bPos.x) < 3.6f;

                        if (boyOnGround && boyJumpCooldown <= 0f && (gapAhead || boyStuckTimer > 0.18f || jumpToOverheadPlat)) {
                            // Nếu vướng ngay tường thành cao thì leo tường luôn
                            MapWall blockingWall = null;
                            for (int wi = 0; wi < wallsList.size; wi++) {
                                MapWall w = wallsList.get(wi);
                                if (bPos.y < w.y + w.height - 0.2f
                                    && (Math.abs(bPos.x - w.x) < 0.55f || Math.abs(bPos.x - (w.x + w.width)) < 0.55f)) {
                                    blockingWall = w;
                                    break;
                                }
                            }
                            if (blockingWall != null && blockingWall.height > 1.8f) {
                                boyIsClimbing = true;
                                boyBody.setGravityScale(0f);
                                boyBody.setLinearVelocity(0f, 3.3f);
                            } else {
                                float jvx = jumpToOverheadPlat ? boyFacing * 2.8f : boyFacing * 3.8f;
                                float jvy = jumpToOverheadPlat ? 6.8f : 6.4f;
                                boyBody.setLinearVelocity(jvx, jvy);
                                boyJumpCooldown = 0.65f;
                            }
                            boyStuckTimer = 0f;
                        }
                    }
                }

                // Smart AI Slingshot Combat
                boyAttackCooldown -= delta;
                if (boyAttackCooldown <= 0) {
                    GroundMob targetMob = null;
                    float minMobDist = 8.0f;
                    for (int mi = 0; mi < activeMonsters.size; mi++) {
                        GroundMob m = activeMonsters.get(mi);
                        if (m.body != null && !m.isDead) {
                            float dx = m.body.getPosition().x - bPos.x;
                            float dy = m.body.getPosition().y - bPos.y;
                            float d = Math.abs(dx);
                            if (d < minMobDist && Math.abs(dy) < 4.0f) {
                                minMobDist = d;
                                targetMob = m;
                            }
                        }
                    }

                    if (targetMob != null) {
                        Vector2 targetDir = new Vector2(
                            targetMob.body.getPosition().x - bPos.x,
                            (targetMob.body.getPosition().y + 0.18f) - (bPos.y + 0.22f)).nor();
                        boyFacing = (targetDir.x > 0) ? 1 : -1;
                        projectiles.add(new Projectile(bPos.x + boyFacing * 0.32f, bPos.y + 0.22f,
                            targetDir.x * 11.0f, targetDir.y * 11.0f, 30, false, false, true, false));
                        SoundManager.getInstance().playSlash();
                        boyAttackCooldown = 1.15f;
                    } else if (pirateAlive && !isGrabbedByPirate) {
                        float dx = pirateX - bPos.x;
                        float dy = pirateY - (bPos.y + 0.22f);
                        if (Math.abs(dx) < 8.5f) {
                            Vector2 targetDir = new Vector2(dx, dy).nor();
                            boyFacing = (targetDir.x > 0) ? 1 : -1;
                            projectiles.add(new Projectile(bPos.x + boyFacing * 0.32f, bPos.y + 0.22f,
                                targetDir.x * 11.0f, targetDir.y * 11.0f, 30, false, false, true, false));
                            SoundManager.getInstance().playSlash();
                            boyAttackCooldown = 1.15f;
                        }
                    }
                }
            }
        }
    }

    private void damageGroundMob(GroundMob m, int damage, boolean shouldSync) {
        damageGroundMob(m, damage, shouldSync, false);
    }

    private void damageGroundMob(GroundMob m, int damage, boolean shouldSync, boolean fromPoison) {
        if (m == null || m.isDead || m.body == null || damage <= 0) return;

        // Trong lúc Boss gồng 2s Cuồng Huyết: giảm mạnh 85% sát thương gánh chịu!
        if (m.isChargingBerserk) {
            damage = Math.max(1, (int)(damage * 0.15f));
            if (!fromPoison) {
                addCustomText(m.body.getPosition().x, m.body.getPosition().y + 0.8f, "GIẢM 85% SÁT THƯƠNG!", Color.CYAN);
            }
        }

        int prevHp = m.hp;
        m.hp = Math.max(0, m.hp - damage);
        int effectiveDmg = Math.min(prevHp, damage);

        // Kích hoạt trạng thái GỒNG CUỒNG HUYẾT trong 2s khi tụt xuống <= 50% HP
        if (m.isBoss && !m.berserkTriggered && m.hp <= (m.maxHp * 0.50f) && m.hp > 0) {
            m.berserkTriggered = true;
            m.isChargingBerserk = true;
            m.berserkChargeTimer = 2.0f;
            m.hitTimer = 0f;
            m.state = "BERSERK_CHARGE";
            m.animTime = 0f;
            m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
            SoundManager.getInstance().playBossSmash();
            cameraShakeTime = 0.8f;
            cameraShakeIntensity = 0.45f;
            actionLog.setText("THỦ LĨNH GỒNG CUỒNG HUYẾT (2 GIÂY) – GIẢM 85% SÁT THƯƠNG!");
            addCustomText(m.body.getPosition().x, m.body.getPosition().y + 1.3f, "GỒNG CUỒNG HUYẾT (2 GIÂY)!", Color.RED);
        }

        enemyHpBar.setMaxHp(m.maxHp);
        enemyHpBar.setHp(m.hp);
        lblEnemyInfo.setText((m.isBoss ? (story != null ? story.bossName + ": " : "THỦ LĨNH: ") : "Lính: ") + m.hp + " HP");
        addDamageText(m.body.getPosition().x, m.body.getPosition().y + 0.4f, damage, fromPoison ? Color.PURPLE : Color.YELLOW);

        if (!fromPoison) {
            spawnHitSparks(m.body.getPosition().x, m.body.getPosition().y + 0.4f, m.isBoss ? 16 : 8, m.isBoss ? Color.ORANGE : Color.YELLOW);
            if (m.isBoss || damage >= 80) {
                SoundManager.getInstance().playHeavyHit();
                hitStopTimer = 0.08f;
                cameraShakeTime = 0.35f;
                cameraShakeIntensity = 0.30f;
            } else {
                SoundManager.getInstance().playHit();
                hitStopTimer = 0.04f;
            }
        }

        if (damage < 9000) {
            score += effectiveDmg * 2;
            goldEarned += (int)(effectiveDmg * 0.4f);
            updateScoreGoldUI();
        }

        if (shouldSync && currentGameMode == GameMode.COOP && m.id >= 0) {
            sendActionPacket("mobHit", damage, m.id);
        }

        if (m.hp <= 0) {
            m.isDead = true;
            m.deathTimer = 0.75f;
            m.state = "DEAD";
            mobsKilledCount++;
            onEnemyKilled();
            goldEarned += 5;
            if (m.isBoss) {
                bossKills++;
                score += 1000;
                goldEarned += 350;
                actionLog.setText("HẠ GỤC THỦ LĨNH! +1000 ĐIỂM & +350 VÀNG!");
            }
            updateScoreGoldUI();
            if (m.body != null) {
                m.body.setLinearVelocity(0, m.body.getLinearVelocity().y);
                Array<Fixture> flist = m.body.getFixtureList();
                for (int fi = 0; fi < flist.size; fi++) {
                    flist.get(fi).setSensor(true);
                }
            }
        } else if (!fromPoison) {
            boolean bossImmuneHitstun = m.isBoss && (m.isChargingBerserk || "SMASH_WINDUP".equals(m.state) || m.isResting || m.isBerserk);
            if (!bossImmuneHitstun) {
                m.hitTimer = 0.24f;
                m.state = "HIT";
            }
            m.alertTimer = 6.5f;
            if (playerBody != null && !m.isChargingBerserk && !m.isResting) {
                float dx = playerBody.getPosition().x - m.body.getPosition().x;
                if (Math.abs(dx) > 0.1f) m.facing = (dx > 0) ? 1 : -1;
            }
            if (damage < 9000) {
                alertNearbyMobs(m.body.getPosition().x, m.body.getPosition().y, 5.2f);
            }
        }
    }

    private void onEnemyKilled() {
        if (ultimateKills < ULTIMATE_REQ_KILLS) {
            ultimateKills++;
            if (ultimateKills >= ULTIMATE_REQ_KILLS) {
                actionLog.setText("TUYỆT KỸ ĐÃ SẴN SÀNG! NHẤN (U) ĐỂ XUẤT CHIÊU!");
                SoundManager.getInstance().playCoin();
            } else {
                actionLog.setText("Tích nộ tuyệt kỹ: " + ultimateKills + "/" + ULTIMATE_REQ_KILLS);
            }
        }
    }

    private void damagePirate(int dmg) {
        if (!pirateAlive) return;
        pirateHp -= dmg;
        addDamageText(pirateX, pirateY + 0.3f, dmg, Color.CYAN);
        SoundManager.getInstance().playHit();
        if (pirateHp <= 0) {
            pirateAlive = false;
            isGrabbedByPirate = false;
            pirateState = PirateState.FALLING;
            pirateDeathTimer = 1.2f;
            score += 300; goldEarned += 80;
            onEnemyKilled();
            updateScoreGoldUI();
            actionLog.setText("ĐÃ BẮN HẠ KHÔNG TẶC!");
        }
    }

    private void damageImperial(int dmg) {
        if (!imperialAlive) return;
        imperialHp -= dmg;
        addDamageText(imperialX, imperialY + 0.4f, dmg, Color.GOLD);
        SoundManager.getInstance().playHeavyHit();
        if (imperialHp <= 0) {
            imperialAlive = false;
            imperialState = "FALLING";
            imperialDeathTimer = 1.2f;
            score += 500; goldEarned += 150;
            onEnemyKilled();
            updateScoreGoldUI();
            actionLog.setText("LÍNH BAY TRIỀU ĐÌNH ĐÃ BỊ BẮN HẠ!");
        }
    }

    private int checkNearWallSide(Vector2 pPos) {
        if (pPos.x <= 0.65f) return -1;
        if (pPos.x >= mapWidth - 0.65f) return 1;

        for (int i = 0; i < wallsList.size; i++) {
            MapWall w = wallsList.get(i);
            if (pPos.y >= w.y - 0.2f && pPos.y <= w.y + w.height + 0.4f) {
                if (Math.abs(pPos.x - w.x) <= 0.65f) return 1; // Wall to the right
                if (Math.abs(pPos.x - (w.x + w.width)) <= 0.65f) return -1; // Wall to the left
            }
        }

        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (p.isGround) continue;
            if (pPos.y >= p.y - 0.35f && pPos.y <= p.y + p.height + 0.45f) {
                if (Math.abs(pPos.x - p.x) <= 0.55f) return 1; // Wall to the right
                if (Math.abs(pPos.x - (p.x + p.width)) <= 0.55f) return -1; // Wall to the left
            }
        }
        return 0;
    }

    private MapWall findNearbyWall(Vector2 pos, float reach) {
        for (int i = 0; i < wallsList.size; i++) {
            MapWall w = wallsList.get(i);
            if (pos.y >= w.y - 0.3f && pos.y <= w.y + w.height + 0.4f) {
                if (Math.abs(pos.x - w.x) <= reach || Math.abs(pos.x - (w.x + w.width)) <= reach) {
                    return w;
                }
            }
        }
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (!p.isGround && p.y >= 2.0f && p.height < 1.0f) {
                if (pos.y >= 0.5f && pos.y <= p.y + 0.5f) {
                    if (Math.abs(pos.x - p.x) <= reach || Math.abs(pos.x - (p.x + p.width)) <= reach) {
                        return new MapWall(p.x, 0.65f, p.width, p.y - 0.65f);
                    }
                }
            }
        }
        return null;
    }

    private void toggleWallClimb() {
        if (playerDead || isMatchEnded || isGrabbedByPirate || playerStunTimer > 0 || inCutscene()) return;
        Vector2 pos = playerBody.getPosition();

        if (isWallClinging) {
            isWallClinging = false;
            clingingWall = null;
            isClimbing = false;
            actionLog.setText("BUÔNG TAY KHỎI TƯỜNG!");
            return;
        }

        MapWall nearW = findNearbyWall(pos, 0.90f);
        if (nearW != null) {
            isWallClinging = true;
            clingingWall = nearW;
            clingingSide = (pos.x < nearW.x + nearW.width / 2f) ? -1 : 1;
            playerFacing = -clingingSide;
            playerBody.setLinearVelocity(0, 0);
            isClimbing = true;
            SoundManager.getInstance().playHit();
            actionLog.setText("ĐÃ BÁM TƯỜNG! Kéo cần lên/xuống để leo • Bấm NHẢY để Khinh Công!");
            spawnHitSparks(pos.x, pos.y, 10, Color.LIGHT_GRAY);
            return;
        }

        for (int i = 0; i < laddersList.size; i++) {
            Ladder l = laddersList.get(i);
            if (Math.abs(pos.x - (l.x + l.width / 2f)) < 0.75f && pos.y >= l.y - 0.2f && pos.y <= l.y + l.height + 0.5f) {
                isClimbing = true;
                playerBody.setLinearVelocity(0, 2.6f);
                actionLog.setText("ĐANG LEO THANG!");
                return;
            }
        }

        actionLog.setText("HÃY TIẾP CẬN TƯỜNG THÀNH HOẶC CÔNG TRÌNH ĐỂ LEO!");
    }

    private void triggerUltimate() {
        if (playerDead || isAssassinating || isMatchEnded || isPaused || inCutscene()
            || playerStunTimer > 0 || isGrabbedByPirate || playerBody == null) return;

        if (isSwinging) {
            if (currentSwingingRope != null) {
                currentSwingingRope.release();
                currentSwingingRope = null;
            }
            isSwinging = false;
        }

        String activeUlt = (currentGameMode == GameMode.BATTLE_5V5 && playerCurrentUltType != null)
            ? playerCurrentUltType : selectedChar;
        boolean isP1 = !"P2".equals(activeUlt);
        Vector2 pPos = playerBody.getPosition();

        if (isP1) {
            // P1: Yasuo "Trấn Trời: Cuồng Lôi Phong Bạo"
            if (ultimateKills < ULTIMATE_REQ_KILLS || isP1UltActive) {
                if (ultimateKills < ULTIMATE_REQ_KILLS) {
                    actionLog.setText("Chưa đủ nộ tuyệt kỹ (" + ultimateKills + "/" + ULTIMATE_REQ_KILLS + ")!");
                }
                return;
            }
            ultimateKills = 0;
            isP1UltActive = true;
            p1UltTimer = 0f;
            p1UltBeat = 1;
            playerBody.setLinearVelocity(playerFacing * 9.5f, 6.5f);
            SoundManager.getInstance().playUltimateP1();
            SoundManager.getInstance().playTrangUltimate();
            cameraShakeTime = 0.40f;
            cameraShakeIntensity = 0.30f;
            actionLog.setText("TRẤN TRỜI: BÃO KIẾM THẦN TỐC!");
            spawnHitSparks(playerBody.getPosition().x, playerBody.getPosition().y, 25, Color.CYAN);
            spawnHitSparks(playerBody.getPosition().x, playerBody.getPosition().y, 15, Color.GOLD);
            broadcastUltCast(1, 1, pPos.x, pPos.y, playerFacing, pPos.x + playerFacing * 4.0f, pPos.y);
        } else {
            // P2: "Ám Dạ Tuyệt Diệt: Tử Thần Trảm"
            if (!isP2StealthActive) {
                if (ultimateKills < ULTIMATE_REQ_KILLS) {
                    actionLog.setText("Chưa đủ nộ tuyệt kỹ (" + ultimateKills + "/" + ULTIMATE_REQ_KILLS + ")!");
                    return;
                }
                ultimateKills = 0;
                isP2StealthActive = true;
                p2StealthTimer = P2_STEALTH_DURATION;
                SoundManager.getInstance().playUltimateP2();
                cameraShakeTime = 0.25f;
                cameraShakeIntensity = 0.20f;
                actionLog.setText("SÁT CHIÊU: TÀNG HÌNH & TĂNG TỐC!");
                spawnHitSparks(playerBody.getPosition().x, playerBody.getPosition().y, 25, Color.PURPLE);
                broadcastUltCast(2, 1, pPos.x, pPos.y, playerFacing, 0, 0);
            } else {
                if (!p2TargetInRange) {
                    actionLog.setText("Chưa có mục tiêu trong tầm để kích hoạt lần 2!");
                    return;
                }
                isP2StealthActive = false;
                isP2DashingAssassinate = true;
                p2DashAssassinateTimer = 0.16f; // Vút nhanh như tia chớp
                Vector2 startPos = playerBody.getPosition();

                // Tính toán chính xác vị trí SAU LƯNG của kẻ địch
                float behindX = MathUtils.clamp(p2TargetPos.x - (p2TargetFacing * 0.85f), 0.8f, mapWidth - 0.8f);
                float behindY = p2TargetPos.y;
                p2BehindTargetPos.set(behindX, behindY);

                // Kích hoạt khoảnh khắc điện ảnh ám sát (Slow-Motion + Zoom cận cảnh)
                cinematicAssassinateTimer = 0.55f;
                assassinateFocusX = behindX;
                assassinateFocusY = behindY + 0.3f;

                // Tạo vệt DƯ ẢNH (Shadow Afterimages) bóng ma sát thủ rải đều từ vị trí cũ tới sau lưng mục tiêu
                TextureRegion p2Frame = (darkAtkRegionsList.size > 0) ? darkAtkRegionsList.get(0) : staticIdleFrame;
                int numShadows = 7;
                for (int k = 0; k <= numShadows; k++) {
                    float progress = (float)k / numShadows;
                    float sX = startPos.x + (behindX - startPos.x) * progress;
                    float sY = startPos.y + (behindY - startPos.y) * progress;
                    P2Afterimage ai = new P2Afterimage();
                    ai.x = sX;
                    ai.y = sY;
                    ai.facing = playerFacing;
                    ai.frame = p2Frame;
                    ai.alpha = 0.90f - progress * 0.15f;
                    ai.lifeTime = 0.55f;
                    ai.color = (k % 2 == 0) ? new Color(0.70f, 0.15f, 0.95f, 0.85f) : new Color(0.25f, 0.05f, 0.40f, 0.85f);
                    p2Afterimages.add(ai);
                }

                // Lao nhanh như chớp với vận tốc cực lớn (34.0 m/s)
                Vector2 dir = new Vector2(behindX - startPos.x, behindY - startPos.y).nor();
                if (Math.abs(dir.x) > 0.05f) playerFacing = (dir.x > 0) ? 1 : -1;
                playerBody.setLinearVelocity(dir.x * 34.0f, dir.y * 34.0f);
                SoundManager.getInstance().playHeavyHit();
                SoundManager.getInstance().playPlayerSlash(false);
                actionLog.setText("SÁT CHIÊU: LAO NHƯ CHỚP, XUẤT HIỆN SAU LƯNG!");
                broadcastUltCast(2, 2, startPos.x, startPos.y, playerFacing, behindX, behindY);
            }
        }
    }

    private void updateUltimate(float delta) {
        // Cooldown timers
        if (dashCooldownTimer > 0) dashCooldownTimer -= delta;
        if (jumpCooldownTimer > 0) jumpCooldownTimer -= delta;

        if (btnDashRef != null) {
            int tenths = (dashCooldownTimer > 0) ? (int) Math.ceil(dashCooldownTimer * 10f) : 0;
            if (tenths != lastDashTenths) { // chỉ đổi chữ khi giá trị hiển thị thay đổi
                lastDashTenths = tenths;
                btnDashRef.setText(tenths > 0 ? ("LƯỚT (" + (tenths / 10) + "." + (tenths % 10) + "s)")
                    : (isDesktopPlatform() ? "LƯỚT (L)" : "LƯỚT"));
            }
        }

        // PVP Ultimate Orbs
        if (currentGameMode == GameMode.PVP && !isMatchEnded && !playerDead) {
            Vector2 pPos = playerBody.getPosition();
            for (int i = 0; i < pvpUltOrbs.size; i++) {
                UltOrb orb = pvpUltOrbs.get(i);
                if (!orb.active) {
                    orb.respawnTimer -= delta;
                    if (orb.respawnTimer <= 0) {
                        orb.active = true;
                        orb.respawnTimer = 0f;
                    }
                } else {
                    orb.pulseTime += delta;
                    if (pPos.dst(orb.x, orb.y) < 0.85f && ultimateKills < ULTIMATE_REQ_KILLS) {
                        orb.active = false;
                        orb.respawnTimer = 22.0f;
                        ultimateKills = ULTIMATE_REQ_KILLS;
                        SoundManager.getInstance().playOrbPickup();
                        cameraShakeTime = 0.25f; cameraShakeIntensity = 0.20f;
                        spawnHitSparks(orb.x, orb.y, 25, Color.GOLD);
                        addCustomText(orb.x, orb.y + 0.6f, "NGỌC TUYỆT KỸ!", Color.GOLD);
                        actionLog.setText("NHẶT ĐƯỢC NGỌC TUYỆT KỸ! NHẤN (U) ĐỂ RA ĐÒN!");
                    }
                }
            }
        }

        // P1 "Trấn Trời" Logic - Lưu ý: trong lúc dùng chiêu vẫn nhận sát thương bình thường!
        if (isP1UltActive && !playerDead) {
            p1UltTimer += delta;
            Vector2 pos = playerBody.getPosition();

            if (p1UltTimer < 0.16f) {
                playerBody.setLinearVelocity(playerFacing * 9.5f, 6.5f);
                spawnHitSparks(pos.x, pos.y, 3, Color.CYAN);
            } else if (p1UltTimer < 0.32f) {
                playerBody.setLinearVelocity(-playerFacing * 8.5f, 5.0f);
                spawnHitSparks(pos.x, pos.y, 3, Color.WHITE);
            } else if (p1UltTimer < 0.48f) {
                playerBody.setLinearVelocity(playerFacing * 9.0f, 4.5f);
                spawnHitSparks(pos.x, pos.y, 3, Color.SKY);
            } else if (p1UltTimer < 0.68f) {
                playerBody.setLinearVelocity(0, 0.2f);
                spawnHitSparks(pos.x, pos.y, 5, Color.GOLD);
            } else {
                float prevVy = playerBody.getLinearVelocity().y;
                playerBody.setLinearVelocity(0, -24.0f);
                spawnHitSparks(pos.x, pos.y, 4, Color.ORANGE);

                if (isGrounded || pos.y <= 1.25f || (p1UltTimer > 0.80f && prevVy >= -0.5f) || p1UltTimer > 2.2f) {
                    isP1UltActive = false;
                    cameraShakeTime = 0.70f;
                    cameraShakeIntensity = 0.55f;
                    SoundManager.getInstance().playBossSmash();
                    spawnHitSparks(pos.x, pos.y - 0.2f, 45, Color.GOLD);
                    spawnHitSparks(pos.x, pos.y - 0.2f, 30, Color.CYAN);

                    int p1UltDmg = (currentGameMode == GameMode.PVP) ? 200 : 320;
                    if (currentGameMode == GameMode.PVP) {
                        float dist = Math.abs(remoteX - pos.x);
                        if (!remoteDead && remoteX != -999f && dist <= 3.5f && Math.abs(remoteY - pos.y) < 2.0f) {
                            sendActionPacket("hit", p1UltDmg, 0);
                        }
                    } else if (currentGameMode == GameMode.BATTLE_5V5) {
                        int pTeam = (game != null) ? game.selectedTeam : 1;
                        int p1UltDmg5v5 = (int)(320 * playerDamageBuff * (playerHasTitanBuff ? 1.35f : 1f));
                        if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && pos.dst(remoteX, remoteY) <= 4.2f) {
                            sendActionPacket("hit", p1UltDmg5v5, 0);
                            remoteHitFlashTimer = 0.25f;
                            addDamageText(remoteX, remoteY + 0.5f, p1UltDmg5v5, Color.GOLD);
                        }
                        for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                            RemotePeer5v5 ep = extraPeers5v5.get(epi);
                            if (ep.team != pTeam && !ep.dead && ep.x != -999f && pos.dst(ep.x, ep.y) <= 4.2f) {
                                sendActionPacketToPeer("hit", p1UltDmg5v5, 0, ep.id);
                                ep.hitFlashTimer = 0.25f;
                                addDamageText(ep.x, ep.y + 0.5f, p1UltDmg5v5, Color.GOLD);
                            }
                        }
                        for (int i = 0; i < fighters5v5.size; i++) {
                            Fighter5v5 f = fighters5v5.get(i);
                            if (f.team != pTeam && !f.isDead && f.body != null && pos.dst(f.body.getPosition()) <= 4.2f) {
                                boolean killed = f.takeDamage(p1UltDmg5v5);
                                spawnHitSparks(f.body.getPosition().x, f.body.getPosition().y, 25, Color.GOLD);
                                addDamageText(f.body.getPosition().x, f.body.getPosition().y + 0.5f, p1UltDmg5v5, Color.GOLD);
                                if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                            }
                        }
                    } else {
                        for (int i = activeMonsters.size - 1; i >= 0; i--) {
                            GroundMob m = activeMonsters.get(i);
                            if (m.body != null && !m.isDead && pos.dst(m.body.getPosition()) <= 3.5f) {
                                damageGroundMob(m, p1UltDmg, true);
                            }
                        }
                        if (hasSkyPirate && pirateAlive && pos.dst(pirateX, pirateY) <= 3.5f) {
                            damagePirate(p1UltDmg);
                        }
                        if (hasImperial && imperialAlive && pos.dst(imperialX, imperialY) <= 3.5f) {
                            damageImperial(p1UltDmg);
                        }
                    }
                    actionLog.setText("TRẤN TRỜI: BÃO KIẾM CẮM ĐẤT! (-" + p1UltDmg + " sát thương lan)");
                    addCustomText(pos.x, pos.y + 0.8f, "TRẤN TRỜI TẬN DIỆT!", Color.GOLD);
                }
            }
        }

        // P2 "Sát Chiêu Hoàn Hảo" Logic - Lưu ý: trong lúc tàng hình vẫn nhận sát thương bình thường!
        if (isP2StealthActive && !playerDead) {
            p2StealthTimer -= delta;
            Vector2 pPos = playerBody.getPosition();
            p2TargetInRange = false;
            p2TargetIsPrimaryRemote = false;
            p2TargetPeerId = null;

            if (currentGameMode == GameMode.PVP) {
                if (remoteX != -999f && !remoteDead && !remoteStealth && pPos.dst(remoteX, remoteY) <= P2_STEALTH_RANGE) {
                    p2TargetInRange = true;
                    p2TargetIsPrimaryRemote = true;
                    p2TargetPos.set(remoteX, remoteY);
                    p2TargetFacing = remoteFacing;
                }
            } else if (currentGameMode == GameMode.BATTLE_5V5) {
                int pTeam = (game != null) ? game.selectedTeam : 1;
                float closestDst = 999f;
                if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && !remoteStealth
                    && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)) {
                    float d = pPos.dst(remoteX, remoteY);
                    if (d <= P2_STEALTH_RANGE && d < closestDst) {
                        closestDst = d;
                        p2TargetInRange = true;
                        p2TargetIsPrimaryRemote = true;
                        p2TargetPeerId = null;
                        p2TargetPos.set(remoteX, remoteY);
                        p2TargetFacing = remoteFacing;
                    }
                }
                for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                    RemotePeer5v5 ep = extraPeers5v5.get(epi);
                    if (ep.team != pTeam && !ep.dead && ep.x != -999f && !ep.stealth
                        && isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) {
                        float d = pPos.dst(ep.x, ep.y);
                        if (d <= P2_STEALTH_RANGE && d < closestDst) {
                            closestDst = d;
                            p2TargetInRange = true;
                            p2TargetIsPrimaryRemote = false;
                            p2TargetPeerId = ep.id;
                            p2TargetPos.set(ep.x, ep.y);
                            p2TargetFacing = ep.facing;
                        }
                    }
                }
                for (int i = 0; i < fighters5v5.size; i++) {
                    Fighter5v5 f = fighters5v5.get(i);
                    if (f.team != pTeam && !f.isDead && f.body != null && !f.isStealth && !(f.isInBush && !playerInBush)) {
                        float d = pPos.dst(f.body.getPosition());
                        if (d <= P2_STEALTH_RANGE && d < closestDst) {
                            closestDst = d;
                            p2TargetInRange = true;
                            p2TargetIsPrimaryRemote = false;
                            p2TargetPeerId = null;
                            p2TargetPos.set(f.body.getPosition().x, f.body.getPosition().y);
                            p2TargetFacing = f.facing;
                        }
                    }
                }
            } else {
                float closestDst = 999f;
                for (int mi = 0; mi < activeMonsters.size; mi++) {
                    GroundMob m = activeMonsters.get(mi);
                    if (m.body != null && !m.isDead) {
                        float d = pPos.dst(m.body.getPosition());
                        if (d <= P2_STEALTH_RANGE && d < closestDst) {
                            closestDst = d;
                            p2TargetInRange = true;
                            p2TargetPos.set(m.body.getPosition().x, m.body.getPosition().y);
                            p2TargetFacing = m.facing;
                        }
                    }
                }
                if (!p2TargetInRange && hasSkyPirate && pirateAlive && pPos.dst(pirateX, pirateY) <= P2_STEALTH_RANGE) {
                    p2TargetInRange = true;
                    p2TargetPos.set(pirateX, pirateY);
                    p2TargetFacing = pirateFacing;
                }
                if (!p2TargetInRange && hasImperial && imperialAlive && pPos.dst(imperialX, imperialY) <= P2_STEALTH_RANGE) {
                    p2TargetInRange = true;
                    p2TargetPos.set(imperialX, imperialY);
                    p2TargetFacing = imperialFacing;
                }
            }

            if (p2StealthTimer <= 0) {
                isP2StealthActive = false;
                actionLog.setText("Hết tàng hình!");
            }
        }

        // Lần 2 của P2: Lao tới đâm xuyên tim từ SAU LƯNG kẻ địch có vệt dư ảnh
        if (isP2DashingAssassinate && !playerDead) {
            p2DashAssassinateTimer -= delta;
            Vector2 pPos = playerBody.getPosition();

            // Sinh thêm dư ảnh theo thời gian thực trong khi đang lao
            P2Afterimage ai = new P2Afterimage();
            ai.x = pPos.x;
            ai.y = pPos.y;
            ai.facing = playerFacing;
            ai.frame = (darkAtkRegionsList.size > 0) ? darkAtkRegionsList.get(0) : staticIdleFrame;
            ai.alpha = 0.85f;
            ai.lifeTime = 0.40f;
            ai.color = new Color(0.85f, 0.20f, 1.0f, 0.85f);
            p2Afterimages.add(ai);

            // Đến vị trí sau lưng mục tiêu hoặc hết thời gian lao chớp nhoáng
            if (pPos.dst(p2BehindTargetPos) <= 0.65f || p2DashAssassinateTimer <= 0) {
                isP2DashingAssassinate = false;
                playerBody.setTransform(p2BehindTargetPos.x, p2BehindTargetPos.y, 0);
                playerBody.setLinearVelocity(0, 0);
                playerFacing = p2TargetFacing; // Quay mặt cùng hướng mục tiêu -> Đâm từ sau lưng!

                int p2UltDmg = (currentGameMode == GameMode.PVP) ? 220 : 350;
                SoundManager.getInstance().playHeavyHit();
                SoundManager.getInstance().playPlayerSlash(false);
                cameraShakeTime = 0.60f;
                cameraShakeIntensity = 0.55f;
                hitStopTimer = 0.12f;
                spawnHitSparks(p2BehindTargetPos.x + playerFacing * 0.4f, p2BehindTargetPos.y, 40, Color.PURPLE);
                spawnHitSparks(p2BehindTargetPos.x + playerFacing * 0.4f, p2BehindTargetPos.y, 30, Color.RED);
                spawnHitSparks(p2BehindTargetPos.x + playerFacing * 0.4f, p2BehindTargetPos.y, 25, Color.GOLD);

                if (currentGameMode == GameMode.PVP) {
                    if (remoteX != -999f && !remoteDead && p2BehindTargetPos.dst(remoteX, remoteY) <= 2.2f) {
                        sendActionPacket("hit", p2UltDmg, 0);
                    }
                } else if (currentGameMode == GameMode.BATTLE_5V5) {
                    int pTeam = (game != null) ? game.selectedTeam : 1;
                    int p2UltDmg5v5 = (int)(380 * playerDamageBuff * (playerHasTitanBuff ? 1.35f : 1f));
                    if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && p2BehindTargetPos.dst(remoteX, remoteY) <= 2.5f) {
                        sendActionPacket("hit", p2UltDmg5v5, 0);
                        remoteHitFlashTimer = 0.25f;
                        addDamageText(remoteX, remoteY + 0.5f, p2UltDmg5v5, Color.PURPLE);
                    }
                    for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                        RemotePeer5v5 ep = extraPeers5v5.get(epi);
                        if (ep.team != pTeam && !ep.dead && ep.x != -999f && p2BehindTargetPos.dst(ep.x, ep.y) <= 2.5f) {
                            sendActionPacketToPeer("hit", p2UltDmg5v5, 0, ep.id);
                            ep.hitFlashTimer = 0.25f;
                            addDamageText(ep.x, ep.y + 0.5f, p2UltDmg5v5, Color.PURPLE);
                        }
                    }
                    for (int i = 0; i < fighters5v5.size; i++) {
                        Fighter5v5 f = fighters5v5.get(i);
                        if (f.team != pTeam && !f.isDead && f.body != null && p2BehindTargetPos.dst(f.body.getPosition()) <= 2.5f) {
                            boolean killed = f.takeDamage(p2UltDmg5v5);
                            spawnHitSparks(f.body.getPosition().x, f.body.getPosition().y, 30, Color.PURPLE);
                            addDamageText(f.body.getPosition().x, f.body.getPosition().y + 0.5f, p2UltDmg5v5, Color.PURPLE);
                            if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                        }
                    }
                } else {
                    for (int i = activeMonsters.size - 1; i >= 0; i--) {
                        GroundMob m = activeMonsters.get(i);
                        if (m.body != null && !m.isDead && p2BehindTargetPos.dst(m.body.getPosition()) <= 2.2f) {
                            damageGroundMob(m, p2UltDmg, true);
                        }
                    }
                    if (hasSkyPirate && pirateAlive && p2BehindTargetPos.dst(pirateX, pirateY) <= 2.5f) {
                        damagePirate(p2UltDmg);
                    }
                    if (hasImperial && imperialAlive && p2BehindTargetPos.dst(imperialX, imperialY) <= 2.5f) {
                        damageImperial(p2UltDmg);
                    }
                }
                actionLog.setText("SÁT CHIÊU: ÁM SÁT SAU LƯNG! ĐÂM XUYÊN TIM! (-" + p2UltDmg + " sát thương)");
                addCustomText(p2BehindTargetPos.x, p2BehindTargetPos.y + 0.8f, "ÁM SÁT SAU LƯNG!", Color.RED);
            }
        }

        // Cập nhật Dư ảnh bóng ma sát thủ (P2 Afterimages)
        for (int i = p2Afterimages.size - 1; i >= 0; i--) {
            P2Afterimage ai = p2Afterimages.get(i);
            ai.lifeTime -= delta;
            ai.alpha = Math.max(0f, ai.lifeTime / 0.55f);
            if (ai.lifeTime <= 0) {
                p2Afterimages.removeIndex(i);
            }
        }

        // Cap nhat trang thai hien thi cua nut btnUltimate (ASCII an toan)
        if (btnUltimate != null) {
            String activeUlt = (currentGameMode == GameMode.BATTLE_5V5 && playerCurrentUltType != null)
                ? playerCurrentUltType : selectedChar;
            boolean isP1 = !"P2".equals(activeUlt);
            int stealthSecs = isP2StealthActive ? Math.max(0, Math.round(p2StealthTimer)) : 0;
            int key = (isP1 ? 1 : 0) | (Math.min(ultimateKills, 15) << 1) | ((isP2StealthActive ? 1 : 0) << 5)
                | ((p2TargetInRange ? 1 : 0) << 6) | (stealthSecs << 7);
            if (key == lastUltButtonKey) {
                // không đổi -> bỏ qua setStyle/setText (setStyle làm layout lại cả HUD mỗi frame)
            } else if (isP1) {
                lastUltButtonKey = key;
                if (ultimateKills < ULTIMATE_REQ_KILLS) {
                    btnUltimate.setStyle(ultBtnNormalStyle);
                    btnUltimate.setText("TUYỆT KỸ (" + ultimateKills + "/4)");
                } else {
                    btnUltimate.setStyle(ultBtnReadyStyle);
                    btnUltimate.setText("TRẤN TRỜI (U)");
                }
            } else {
                lastUltButtonKey = key;
                if (!isP2StealthActive) {
                    if (ultimateKills < ULTIMATE_REQ_KILLS) {
                        btnUltimate.setStyle(ultBtnNormalStyle);
                        btnUltimate.setText("TUYỆT KỸ (" + ultimateKills + "/4)");
                    } else {
                        btnUltimate.setStyle(ultBtnReadyStyle);
                        btnUltimate.setText("TÀNG HÌNH (U)");
                    }
                } else {
                    if (p2TargetInRange) {
                        btnUltimate.setStyle(ultBtnP2LockStyle);
                        btnUltimate.setText("KẾT LIỄU! (U)");
                    } else {
                        btnUltimate.setStyle(ultBtnNormalStyle);
                        btnUltimate.setText("RÌNH RẬP... (" + stealthSecs + "s)");
                    }
                }
            }
        }
    }

    /** Chơi đơn được tiêu cả vàng trong kho (hồ sơ); chế độ online chỉ dùng vàng kiếm trong trận cho công bằng. */
    private int bankGold() {
        return (currentGameMode == GameMode.SOLO) ? PlayerProfile.getInstance().gold : 0;
    }

    /** Trừ vàng: dùng vàng nhặt trong trận trước, thiếu thì lấy từ kho. Trả false nếu không đủ. */
    private boolean spendGold(int cost) {
        if (goldEarned + bankGold() < cost) return false;
        int fromMatch = Math.min(goldEarned, cost);
        goldEarned -= fromMatch;
        int rest = cost - fromMatch;
        if (rest > 0) {
            PlayerProfile p = PlayerProfile.getInstance();
            p.gold -= rest;
            p.save();
        }
        return true;
    }

    private void updateScoreGoldUI() {
        if (lblScoreGold != null)
            lblScoreGold.setText("Điểm: " + score + "  •  Vàng: " + (goldEarned + bankGold()));
        if (lblPotions != null)
            lblPotions.setText("Thuốc: x" + healthPotions);
    }

    private void useHealthPotion() {
        if (currentGameMode == GameMode.PVP || playerDead || isMatchEnded || storyDialogueActive || isPaused) return;
        if (healthPotions > 0) {
            if (playerHp >= playerMaxHp) {
                actionLog.setText("MÁU ĐÃ ĐẦY, CHƯA CẦN UỐNG THUỐC!");
                return;
            }
            healthPotions--;
            int heal = 40;
            playerHp = Math.min(playerMaxHp, playerHp + heal);
            playerHpBar.setHp(playerHp);
            lblPlayerHp.setText("Máu: " + playerHp + " HP");
            addDamageText(playerBody.getPosition().x, playerBody.getPosition().y + 0.5f, heal, Color.GREEN);
            SoundManager.getInstance().playCoin();
            updateScoreGoldUI();
            actionLog.setText("ĐÃ UỐNG BÌNH MÁU! HỒI +" + heal + " HP! Còn " + healthPotions + " bình");
        } else {
            actionLog.setText("HẾT BÌNH MÁU! VÀO TIỆM ĐỒ (B) ĐỂ MUA THÊM!");
            SoundManager.getInstance().playHurt();
        }
    }

    private void closeShop() {
        isShopOpen = false;
        if (shopDialogTable != null) shopDialogTable.setVisible(false);
    }

    private void toggleShop() {
        if (currentGameMode == GameMode.PVP || playerDead || isMatchEnded || storyDialogueActive || isPaused) return;
        isShopOpen = !isShopOpen;
        if (shopDialogTable != null) shopDialogTable.setVisible(isShopOpen);
        if (isShopOpen) {
            SoundManager.getInstance().playCoin();
            actionLog.setText("TIỆM ĐỒ ĐANG MỞ – Bấm [1] Kiếm • [2] Giáp • [3] Bình máu • [B/Esc] Đóng.");
        } else {
            SoundManager.getInstance().playSlash();
            actionLog.setText("ĐÃ ĐÓNG TIỆM ĐỒ. TIẾP TỤC CHIẾN ĐẤU!");
        }
    }

    private void buySwordUpgrade() {
        if (!isShopOpen || playerDead || isMatchEnded || isPaused || storyDialogueActive) return;
        if (spendGold(100)) {
            swordDamageBonus += 15;
            updateScoreGoldUI();
            SoundManager.getInstance().playHeavyHit();
            actionLog.setText("NÂNG CẤP KIẾM! SÁT THƯƠNG +" + swordDamageBonus);
        } else {
            SoundManager.getInstance().playHurt();
            actionLog.setText("KHÔNG ĐỦ VÀNG! CẦN 100 VÀNG!");
        }
    }

    private void buyArmorUpgrade() {
        if (!isShopOpen || playerDead || isMatchEnded || isPaused || storyDialogueActive) return;
        if (spendGold(120)) {
            playerMaxHp += 25;
            playerHp = Math.min(playerMaxHp, playerHp + 25);
            playerHpBar.setMaxHp(playerMaxHp);
            playerHpBar.setHp(playerHp);
            lblPlayerHp.setText("Máu: " + playerHp + " HP");
            updateScoreGoldUI();
            SoundManager.getInstance().playCoin();
            actionLog.setText("NÂNG CẤP GIÁP! MÁU TỐI ĐA: " + playerMaxHp);
        } else {
            SoundManager.getInstance().playHurt();
            actionLog.setText("KHÔNG ĐỦ VÀNG! CẦN 120 VÀNG!");
        }
    }

    private void buyHealthPotion() {
        if (!isShopOpen || playerDead || isMatchEnded || isPaused || storyDialogueActive) return;
        if (spendGold(40)) {
            healthPotions++;
            updateScoreGoldUI();
            SoundManager.getInstance().playCoin();
            actionLog.setText("ĐÃ MUA BÌNH MÁU! ĐANG CÓ: " + healthPotions + " bình (phím H)");
        } else {
            SoundManager.getInstance().playHurt();
            actionLog.setText("KHÔNG ĐỦ VÀNG! CẦN 40 VÀNG!");
        }
    }

    private void createShopUI(BitmapFont font, TextButton.TextButtonStyle baseBtnStyle) {
        shopDialogTable = new Table();
        shopDialogTable.setSize(490, 365);
        shopDialogTable.setPosition((1280 - 490) / 2f, (720 - 365) / 2f);
        shopDialogTable.setBackground(UiKit.panel());

        Label.LabelStyle titleStyle = new Label.LabelStyle(UiKit.large(), Color.GOLD);
        Label lblTitle = new Label("TIỆM ĐỒ SÁT THỦ", titleStyle);
        lblTitle.setAlignment(Align.center);

        TextButton.TextButtonStyle swordBtnStyle = UiKit.buttonStyle(new Color(0.46f, 0.14f, 0.14f, 1f), font);
        TextButton.TextButtonStyle armorBtnStyle = UiKit.buttonStyle(new Color(0.14f, 0.38f, 0.20f, 1f), font);
        TextButton.TextButtonStyle potionBtnStyle = UiKit.buttonStyle(new Color(0.14f, 0.30f, 0.50f, 1f), font);

        TextButton btnUpgradeSword = new TextButton("[1] Cường hóa kiếm (+15 sát thương) – 100 vàng", swordBtnStyle);
        TextButton btnUpgradeArmor = new TextButton("[2] Giáp sát thủ (+25 máu tối đa) – 120 vàng", armorBtnStyle);
        TextButton btnBuyPotion = new TextButton("[3] Bình máu (+40 HP) – 40 vàng", potionBtnStyle);
        TextButton btnCloseShop = new TextButton("ĐÓNG TIỆM ĐỒ (B / Esc)", baseBtnStyle);

        btnUpgradeSword.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { buySwordUpgrade(); }
        });

        btnUpgradeArmor.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { buyArmorUpgrade(); }
        });

        btnBuyPotion.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { buyHealthPotion(); }
        });

        btnCloseShop.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeShop();
                SoundManager.getInstance().playSlash();
                actionLog.setText("ĐÃ ĐÓNG TIỆM ĐỒ. TIẾP TỤC CHIẾN ĐẤU!");
            }
        });

        shopDialogTable.add(lblTitle).padBottom(15).row();
        shopDialogTable.add(btnUpgradeSword).width(440).height(46).padBottom(10).row();
        shopDialogTable.add(btnUpgradeArmor).width(440).height(46).padBottom(10).row();
        shopDialogTable.add(btnBuyPotion).width(440).height(46).padBottom(15).row();
        shopDialogTable.add(btnCloseShop).width(300).height(42);

        shopDialogTable.setVisible(false);
        uiStage.addActor(shopDialogTable);
    }

    private void triggerJump(boolean kLeft, boolean kRight) {
        if (playerDead || isAssassinating || isEagleStriking || isMatchEnded || isGrabbedByPirate || playerStunTimer > 0 || inCutscene()) return;
        if (isLedgeGrabbing) {
            performLedgeMantle();
            return;
        }
        if (isCrouching) {
            if (!canStandUp()) {
                actionLog.setText("Trần quá thấp! Cần bò ra khỏi đường hầm trước.");
                return;
            }
            isCrouching = false;
            needsHitboxUpdate = true;
            return;
        }
        if (jumpCooldownTimer > 0) return;

        boolean movingLeft = moveLeft || kLeft;
        boolean movingRight = moveRight || kRight;

        Vector2 pos = playerBody.getPosition();

        // Khinh Công (Wall Cling Leap): Bám tường đạp mạnh phóng vút
        if (isWallClinging) {
            float pushX = (clingingSide != 0) ? -clingingSide * 6.8f : -playerFacing * 6.8f;
            playerBody.setLinearVelocity(pushX, 9.8f);
            isWallClinging = false;
            clingingWall = null;
            isClimbing = false;
            jumpCount = 1;
            coyoteTimer = 0f;
            playerFacing = (pushX > 0) ? 1 : -1;
            jumpCooldownTimer = 0.22f;
            SoundManager.getInstance().playWallJump();
            spawnHitSparks(pos.x, pos.y, 16, Color.GOLD);
            addCustomText(pos.x, pos.y + 0.4f, "KHINH CÔNG!", Color.GOLD);
            actionLog.setText("KHINH CÔNG: ĐẠP TƯỜNG PHÓNG VÚT!");
            return;
        }

        // Rope swing release → launch off rope with pendulum momentum + jump boost!
        if (isSwinging && currentSwingingRope != null) {
            Vector2 tVel = currentSwingingRope.getTangentialVelocity();
            float launchVx = tVel.x + playerFacing * 3.6f;
            float launchVy = Math.max(3.5f, tVel.y + 4.8f);
            currentSwingingRope.release();
            currentSwingingRope = null;
            isSwinging = false;
            playerBody.setLinearVelocity(launchVx, launchVy);
            jumpCount = 1;
            coyoteTimer = 0f;
            jumpCooldownTimer = 0.20f;
            swingCooldown = 0.6f;
            SoundManager.getInstance().playJump();
            actionLog.setText("PHÓNG NGƯỜI TỪ DÂY ĐU!");
            return;
        }

        // Wall climb release → kick off
        if (isClimbing) {
            isClimbing = false;
            float pushX = (movingLeft ? -3.2f : (movingRight ? 3.2f : -playerFacing * 2.8f));
            playerBody.setLinearVelocity(pushX, JUMP_FORCE);
            jumpCount = 1;
            coyoteTimer = 0f;
            jumpCooldownTimer = 0.18f;
            SoundManager.getInstance().playJump();
            return;
        }

        // Parkour Wall Jump (Mọi bức tường công trình và biên bản đồ)
        int wallSide = checkNearWallSide(pos);
        if (!isGrounded && wallSide != 0) {
            playerBody.setLinearVelocity(-wallSide * 4.4f, 5.4f);
            jumpCount = 1;
            coyoteTimer = 0f;
            playerFacing = -wallSide;
            jumpCooldownTimer = 0.20f;
            SoundManager.getInstance().playWallJump();
            spawnHitSparks(pos.x + wallSide * 0.2f, pos.y, 10, Color.WHITE);
            actionLog.setText("ĐẠP TƯỜNG BẬT NHẢY!");
            return;
        }

        boolean canFirstJump = isGrounded || (coyoteTimer > 0f && jumpCount == 0);
        if ((canFirstJump && jumpCount == 0) || (jumpCount < 2 && attackCombo == 0 && !isDashing)) {
            float hVel = playerBody.getLinearVelocity().x;
            if (movingLeft) hVel = -2.6f;
            else if (movingRight) hVel = 2.6f;

            float jForce = (jumpCount == 0) ? JUMP_FORCE : DOUBLE_JUMP_FORCE;
            playerBody.setLinearVelocity(hVel, jForce);
            jumpCount++;
            coyoteTimer = 0f;
            jumpCooldownTimer = 0.18f;
            SoundManager.getInstance().playJump();
            isGrounded = false;
            footContacts = 0;
            if (jumpCount == 2) actionLog.setText("Nhảy đúp trên không!");
        } else {
            jumpBufferTimer = JUMP_BUFFER_MAX;
        }
    }

    private void triggerAttack() {
        triggerAttack(true);
    }

    private void triggerAttack(boolean allowMouseFacing) {
        if (playerDead || isAssassinating || isDashing || isClimbing || isSwinging || isEagleStriking
            || isMatchEnded || playerStunTimer > 0 || inCutscene()) return;
        if (isCrouching) {
            if (!canStandUp()) return;
            isCrouching = false;
            needsHitboxUpdate = true;
        }
        if (weaponAttackCooldown > 0) return;
        SoundManager.getInstance().playSlash();

        // Turn character facing direction to match attack input / mouse click / movement
        boolean kLeft = !isChatTyping() && (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT));
        boolean kRight = !isChatTyping() && (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT));
        float knobX = (touchpad != null && touchpad.isVisible()) ? touchpad.getKnobPercentX() : 0f;
        if (moveLeft || kLeft || knobX < -0.25f) playerFacing = -1;
        else if (moveRight || kRight || knobX > 0.25f) playerFacing = 1;
        else if (allowMouseFacing && isDesktopPlatform() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 mouseWorldPos = viewport.unproject(tmpVec3.set(Gdx.input.getX(), Gdx.input.getY(), 0));
            float dx = mouseWorldPos.x - playerBody.getPosition().x;
            if (Math.abs(dx) > 0.2f) playerFacing = (dx > 0) ? 1 : -1;
        }

        Vector2 pPos = playerBody.getPosition();

        // Counter-attack while grabbed by pirate
        if (isGrabbedByPirate && pirateAlive) {
            damagePirate(55 + swordDamageBonus);
            attackCombo = 1;
            attackTime = 0f;
            queuedAttacks = 0;
            hitThisAttack = true;
            weaponAttackCooldown = 0.25f;
            return;
        }

        // Anti-air attack against sky pirate
        if (isAimingUp && pirateAlive) {
            float dyP = pirateY - pPos.y;
            if (Math.abs(pPos.x - pirateX) < 2.0f && dyP >= -0.4f && dyP < 2.8f) {
                damagePirate(50 + swordDamageBonus);
                attackCombo = 1;
                attackTime = 0f;
                queuedAttacks = 0;
                hitThisAttack = true;
                weaponAttackCooldown = 0.35f;
                return;
            }
        }

        float timeDmgScale = 1.0f + (totalMatchTime / 60f) * 0.25f;

        switch (currentWeapon) {
            case BOW:
                updateBowLockOn();
                int bowDmg = (int)(68 * timeDmgScale) + swordDamageBonus / 2;
                float arrowSpawnY = pPos.y + 0.44f;
                if (hasBowLock()) {
                    playerFacing = (bowLockWorldPos.x >= pPos.x) ? 1 : -1;
                    float arrowSpawnX = pPos.x + playerFacing * 0.50f;
                    float dxL = bowLockWorldPos.x - arrowSpawnX;
                    float dyL = bowLockWorldPos.y - arrowSpawnY;
                    float lenL = (float) Math.sqrt(dxL * dxL + dyL * dyL);
                    if (lenL < 0.001f) { dxL = playerFacing; dyL = 0f; lenL = 1f; }
                    Projectile arrow = new Projectile(arrowSpawnX, arrowSpawnY,
                        (dxL / lenL) * 14.5f, (dyL / lenL) * 14.5f, bowDmg, true, false, false, false);
                    arrow.lifeTime = 4.5f;
                    arrow.homingArrow = true;
                    arrow.targetMob = bowLockMob;
                    arrow.targetFighter = bowLockFighter;
                    arrow.targetTurret = bowLockTurret;
                    arrow.targetSpecial = bowLockSpecial;
                    arrow.targetPeerId = bowLockPeerId;
                    projectiles.add(arrow);
                    spawnHitSparks(arrowSpawnX + playerFacing * 0.20f, arrowSpawnY, 10, Color.GREEN);
                    actionLog.setText("CUNG TÊN TRUY HỒN: ĐÃ KHÓA MỤC TIÊU! (-" + bowDmg + " HP)");
                } else {
                    boolean kUpAim = isAimingUp || Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
                    float arrowSpawnX = pPos.x + playerFacing * 0.50f;
                    float bowVy = kUpAim ? 5.2f : 0f;
                    Projectile arrow = new Projectile(arrowSpawnX, arrowSpawnY,
                        playerFacing * 12.5f, bowVy, bowDmg, true, false, false, false);
                    arrow.lifeTime = 3.8f;
                    projectiles.add(arrow);
                    spawnHitSparks(arrowSpawnX + playerFacing * 0.20f, arrowSpawnY, 8, Color.CYAN);
                    actionLog.setText("Bắn tên tầm xa! (-" + bowDmg + " sát thương)");
                }
                weaponAttackCooldown = 0.36f;
                attackCombo = 1; attackTime = 0f;
                queuedAttacks = 0; hitThisAttack = true;
                SoundManager.getInstance().playPlayerSlash(!"P2".equals(selectedChar));
                break;

            case SHOTGUN:
                int shotgunDmg = (int)(60 * timeDmgScale) + swordDamageBonus / 2;
                float gunSpawnY = pPos.y + 0.38f;
                for (int i = -1; i <= 1; i++) {
                    projectiles.add(new Projectile(pPos.x + playerFacing * 0.55f, gunSpawnY,
                        playerFacing * 9.5f, i * 0.85f, shotgunDmg, false, false, false, true));
                }
                playerBody.applyLinearImpulse(new Vector2(-playerFacing * 1.6f, 0),
                    playerBody.getWorldCenter(), true);
                weaponAttackCooldown = 0.72f;
                attackCombo = 2; attackTime = 0f;
                queuedAttacks = 0; hitThisAttack = true;
                cameraShakeTime = 0.18f;
                cameraShakeIntensity = 0.22f;
                spawnHitSparks(pPos.x + playerFacing * 0.88f, gunSpawnY, 10, Color.ORANGE);
                spawnHitSparks(pPos.x + playerFacing * 0.88f, gunSpawnY, 6, Color.YELLOW);
                SoundManager.getInstance().playHeavyHit();
                actionLog.setText("Nổ súng hoa cải! (-" + (shotgunDmg * 3) + " sát thương)");
                break;

            case AXE:
                int axeDmg = (int)(160 * timeDmgScale) + swordDamageBonus;
                weaponAttackCooldown = 0.82f;
                attackCombo = 3; attackTime = 0f;
                queuedAttacks = 0; hitThisAttack = true;
                cameraShakeTime = 0.28f;
                cameraShakeIntensity = 0.34f;
                spawnHitSparks(pPos.x + playerFacing * 0.98f, pPos.y + 0.15f, 14, Color.FIREBRICK);
                spawnHitSparks(pPos.x + playerFacing * 0.98f, pPos.y + 0.15f, 10, Color.GOLD);
                SoundManager.getInstance().playHeavyHit();
                addCustomText(pPos.x + playerFacing * 0.85f, pPos.y + 0.6f, "ĐẠI TRẢM RÌU CHIẾN!", Color.ORANGE);
                if (currentGameMode == GameMode.PVP) {
                    float dxR = remoteX - pPos.x;
                    boolean facingR = (playerFacing == 1 && dxR >= -0.25f) || (playerFacing == -1 && dxR <= 0.25f);
                    if (!remoteDead && remoteX != -999f && facingR && Math.abs(dxR) <= 2.0f && Math.abs(remoteY - pPos.y) < 1.5f)
                        sendActionPacket("hit", axeDmg, 0);
                } else if (currentGameMode == GameMode.BATTLE_5V5) {
                    int pTeam = (game != null) ? game.selectedTeam : 1;
                    float titanMult = playerHasTitanBuff ? 1.35f : 1.0f;
                    float ambushMult = playerAmbushBuff ? 1.40f : 1.0f;
                    int finalDmg = (int)(axeDmg * playerDamageBuff * titanMult * ambushMult);
                    boolean hitAny = false;
                    if (remoteX != -999f && !remoteDead && remoteTeam != pTeam) {
                        float dxR = remoteX - pPos.x;
                        boolean facingR = (playerFacing == 1 && dxR >= -0.25f) || (playerFacing == -1 && dxR <= 0.25f);
                        if (facingR && Math.abs(dxR) <= 2.0f && Math.abs(remoteY - pPos.y) < 1.5f) {
                            sendActionPacket("hit", finalDmg, 0);
                            remoteHitFlashTimer = 0.20f;
                            addDamageText(remoteX, remoteY + 0.5f, finalDmg, Color.ORANGE);
                            hitAny = true;
                        }
                    }
                    for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                        RemotePeer5v5 ep = extraPeers5v5.get(epi);
                        if (ep.team != pTeam && !ep.dead && ep.x != -999f) {
                            float dxEp = ep.x - pPos.x;
                            boolean facingEp = (playerFacing == 1 && dxEp >= -0.25f) || (playerFacing == -1 && dxEp <= 0.25f);
                            if (facingEp && Math.abs(dxEp) <= 2.0f && Math.abs(ep.y - pPos.y) < 1.5f) {
                                sendActionPacketToPeer("hit", finalDmg, 0, ep.id);
                                ep.hitFlashTimer = 0.20f;
                                addDamageText(ep.x, ep.y + 0.5f, finalDmg, Color.ORANGE);
                                hitAny = true;
                            }
                        }
                    }
                    for (int i = 0; i < fighters5v5.size; i++) {
                        Fighter5v5 f = fighters5v5.get(i);
                        if (f.team != pTeam && !f.isDead && f.body != null) {
                            float dxF = f.body.getPosition().x - pPos.x;
                            boolean facingF = (playerFacing == 1 && dxF >= -0.25f) || (playerFacing == -1 && dxF <= 0.25f);
                            if (facingF && Math.abs(dxF) <= 2.0f && Math.abs(f.body.getPosition().y - pPos.y) < 1.5f) {
                                boolean killed = f.takeDamage(finalDmg);
                                spawnHitSparks(f.body.getPosition().x, f.body.getPosition().y + 0.3f, 20, Color.ORANGE);
                                addDamageText(f.body.getPosition().x, f.body.getPosition().y + 0.5f, finalDmg, Color.ORANGE);
                                if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                                hitAny = true;
                            }
                        }
                    }
                    if (hitAny && playerAmbushBuff) {
                        playerAmbushBuff = false;
                        addCustomText(pPos.x, pPos.y + 0.8f, "PHỤC KÍCH! +40%", Color.GOLD);
                    }
                } else {
                    for (int i = activeMonsters.size - 1; i >= 0; i--) {
                        GroundMob m = activeMonsters.get(i);
                        if (m.body != null && !m.isDead) {
                            float dxM = m.body.getPosition().x - pPos.x;
                            boolean facingM = (playerFacing == 1 && dxM >= -0.25f) || (playerFacing == -1 && dxM <= 0.25f);
                            if (facingM && Math.abs(dxM) <= 2.0f && Math.abs(m.body.getPosition().y - pPos.y) < 1.5f)
                                damageGroundMob(m, axeDmg, true);
                        }
                    }
                    if (hasSkyPirate && pirateAlive && Math.abs(pirateX - pPos.x) <= 2.2f && Math.abs(pirateY - pPos.y) < 1.6f) {
                        damagePirate(axeDmg);
                    }
                    if (hasImperial && imperialAlive && Math.abs(imperialX - pPos.x) <= 2.2f && Math.abs(imperialY - pPos.y) < 1.6f) {
                        damageImperial(axeDmg);
                    }
                }
                break;

            case DUAL_DAGGERS:
                if (attackCombo == 0 || attackTime >= 0.055f) {
                    daggerComboStep = (daggerComboStep == 1) ? 2 : 1;
                    attackCombo = daggerComboStep;
                    attackTime = 0f;
                    queuedAttacks = 0;
                    hitThisAttack = true;
                    weaponAttackCooldown = 0.02f;
                    applyDaggerSlashDamage(pPos);
                } else {
                    queuedAttacks = Math.min(2, queuedAttacks + 1);
                    weaponAttackCooldown = 0.02f;
                }
                break;

            case SWORD:
            default:
                if ("P2".equals(selectedChar) && darkAtkRegionsList.size > 0) {
                    if (attackCombo == 0) {
                        attackCombo = (swordComboWindowTimer > 0) ? nextSwordCombo : 1;
                        swordComboWindowTimer = 0f;
                        attackTime = 0f;
                        queuedAttacks = 0;
                        hitThisAttack = false;
                        if (attackCombo == 3) {
                            SoundManager.getInstance().playHeavyHit();
                            actionLog.setText("VÂN U: TỬ KÍCH ĐOẠT MỆNH (NHỊP 3)!");
                            cameraShakeTime = 0.20f;
                            cameraShakeIntensity = 0.22f;
                        } else if (attackCombo == 2) {
                            SoundManager.getInstance().playSlash();
                            actionLog.setText("VÂN U: PHI THIÊN BẠO KÍCH (NHỊP 2)!");
                        } else {
                            SoundManager.getInstance().playSlash();
                            actionLog.setText("VÂN U: TRẢM LIÊN HOÀN (NHỊP 1)!");
                        }
                    } else {
                        queuedAttacks = Math.min(2, queuedAttacks + 1);
                    }
                    weaponAttackCooldown = 0.05f;
                    break;
                }

                if (attackCombo == 0) {
                    attackCombo = (swordComboWindowTimer > 0) ? nextSwordCombo : 1;
                    swordComboWindowTimer = 0f;
                    attackTime = 0f;
                    queuedAttacks = 0;
                    hitThisAttack = false;
                    if (attackCombo == 3) {
                        SoundManager.getInstance().playHeavyHit();
                        actionLog.setText("VÂN TIÊU: CUỒNG PHONG TUYỆT DIỆT (NHỊP 3)!");
                        cameraShakeTime = 0.20f;
                        cameraShakeIntensity = 0.22f;
                    } else if (attackCombo == 2) {
                        SoundManager.getInstance().playSlash();
                        actionLog.setText("VÂN TIÊU: PHONG LÔI TRẢM (NHỊP 2)!");
                    } else {
                        SoundManager.getInstance().playSlash();
                        actionLog.setText("VÂN TIÊU: BẠCH PHI KIẾM (NHỊP 1)!");
                    }
                } else {
                    queuedAttacks = Math.min(2, queuedAttacks + 1);
                }
                weaponAttackCooldown = 0.05f;
                break;
        }
    }

    /** Gây sát thương và hiệu ứng cho 1 nhịp chém Song Đao siêu tốc. */
    private void applyDaggerSlashDamage(Vector2 pPos) {
        float timeDmgScale = 1.0f + (totalMatchTime / 60f) * 0.25f;
        int daggerDmg = (int)(34 * timeDmgScale) + swordDamageBonus / 2;
        playerBody.applyLinearImpulse(new Vector2(playerFacing * 0.85f, 0), playerBody.getWorldCenter(), true);
        spawnHitSparks(pPos.x + playerFacing * 0.78f, pPos.y + 0.25f, 8, Color.RED);
        spawnHitSparks(pPos.x + playerFacing * 0.78f, pPos.y + 0.25f, 6, Color.CYAN);
        SoundManager.getInstance().playSlash();
        actionLog.setText(daggerComboStep == 1 ? "SONG ĐAO: TẢ TRẢM!" : "SONG ĐAO: HỮU PHẠT!");

        if (currentGameMode == GameMode.PVP) {
            float dxR = remoteX - pPos.x;
            boolean facingR = (playerFacing == 1 && dxR >= -0.25f) || (playerFacing == -1 && dxR <= 0.25f);
            if (!remoteDead && remoteX != -999f && facingR && Math.abs(dxR) <= 1.75f && Math.abs(remoteY - pPos.y) < 1.35f)
                sendActionPacket("hit", daggerDmg, 0);
        } else if (currentGameMode == GameMode.BATTLE_5V5) {
            int pTeam = (game != null) ? game.selectedTeam : 1;
            float titanMult = playerHasTitanBuff ? 1.35f : 1.0f;
            float ambushMult = playerAmbushBuff ? 1.40f : 1.0f;
            int finalDmg = (int)(daggerDmg * playerDamageBuff * titanMult * ambushMult);
            boolean hitAny = false;
            if (remoteX != -999f && !remoteDead && remoteTeam != pTeam) {
                float dxR = remoteX - pPos.x;
                boolean facingR = (playerFacing == 1 && dxR >= -0.25f) || (playerFacing == -1 && dxR <= 0.25f);
                if (facingR && Math.abs(dxR) <= 1.75f && Math.abs(remoteY - pPos.y) < 1.35f) {
                    sendActionPacket("hit", finalDmg, 0);
                    remoteHitFlashTimer = 0.20f;
                    addDamageText(remoteX, remoteY + 0.5f, finalDmg, Color.RED);
                    hitAny = true;
                }
            }
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.team != pTeam && !ep.dead && ep.x != -999f) {
                    float dxEp = ep.x - pPos.x;
                    boolean facingEp = (playerFacing == 1 && dxEp >= -0.25f) || (playerFacing == -1 && dxEp <= 0.25f);
                    if (facingEp && Math.abs(dxEp) <= 1.75f && Math.abs(ep.y - pPos.y) < 1.35f) {
                        sendActionPacketToPeer("hit", finalDmg, 0, ep.id);
                        ep.hitFlashTimer = 0.20f;
                        addDamageText(ep.x, ep.y + 0.5f, finalDmg, Color.RED);
                        hitAny = true;
                    }
                }
            }
            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.team != pTeam && !f.isDead && f.body != null) {
                    float dxF = f.body.getPosition().x - pPos.x;
                    boolean facingF = (playerFacing == 1 && dxF >= -0.25f) || (playerFacing == -1 && dxF <= 0.25f);
                    if (facingF && Math.abs(dxF) <= 1.75f && Math.abs(f.body.getPosition().y - pPos.y) < 1.35f) {
                        boolean killed = f.takeDamage(finalDmg);
                        spawnHitSparks(f.body.getPosition().x, f.body.getPosition().y + 0.3f, 12, Color.RED);
                        addDamageText(f.body.getPosition().x, f.body.getPosition().y + 0.5f, finalDmg, Color.RED);
                        if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                        hitAny = true;
                    }
                }
            }
            if (hitAny && playerAmbushBuff) {
                playerAmbushBuff = false;
                addCustomText(pPos.x, pPos.y + 0.8f, "PHỤC KÍCH! +40%", Color.GOLD);
            }
        } else {
            for (int i = activeMonsters.size - 1; i >= 0; i--) {
                GroundMob m = activeMonsters.get(i);
                if (m.body != null && !m.isDead) {
                    float dxM = m.body.getPosition().x - pPos.x;
                    boolean facingM = (playerFacing == 1 && dxM >= -0.25f) || (playerFacing == -1 && dxM <= 0.25f);
                    if (facingM && Math.abs(dxM) <= 1.75f && Math.abs(m.body.getPosition().y - pPos.y) < 1.35f)
                        damageGroundMob(m, daggerDmg, true);
                }
            }
            if (hasSkyPirate && pirateAlive && Math.abs(pirateX - pPos.x) <= 1.9f && Math.abs(pirateY - pPos.y) < 1.5f) {
                damagePirate(daggerDmg);
            }
            if (hasImperial && imperialAlive && Math.abs(imperialX - pPos.x) <= 1.9f && Math.abs(imperialY - pPos.y) < 1.5f) {
                damageImperial(daggerDmg);
            }
        }
    }

    private boolean hasBowLock() {
        return bowLockMob != null || bowLockFighter != null || bowLockTurret != null || bowLockSpecial != 0 || bowLockPeerId != null;
    }

    private boolean resolveProjectileTargetPos(Projectile p, Vector2 out) {
        if (p.targetPeerId != null) {
            RemotePeer5v5 ep = findExtraPeer(p.targetPeerId);
            if (ep != null && !ep.dead && ep.x != -999f) {
                out.set(ep.x, ep.y + 0.22f);
                return true;
            }
        }
        if (p.targetMob != null && p.targetMob.body != null && !p.targetMob.isDead) {
            Vector2 mp = p.targetMob.body.getPosition();
            out.set(mp.x, mp.y + (p.targetMob.isBoss ? 0.35f : 0.22f));
            return true;
        }
        if (p.targetFighter != null && p.targetFighter.body != null && !p.targetFighter.isDead) {
            Vector2 fp = p.targetFighter.body.getPosition();
            out.set(fp.x, fp.y + 0.22f);
            return true;
        }
        if (p.targetTurret != null && !p.targetTurret.isDestroyed) {
            out.set(p.targetTurret.x, p.targetTurret.y + 0.85f);
            return true;
        }
        if (p.targetSpecial == 1 && hasSkyPirate && pirateAlive) {
            out.set(pirateX, pirateY + 0.15f);
            return true;
        }
        if (p.targetSpecial == 2 && hasImperial && imperialAlive) {
            out.set(imperialX, imperialY + 0.18f);
            return true;
        }
        if (p.targetSpecial == 3 && remoteX != -999f && !remoteDead) {
            out.set(remoteX, remoteY + 0.22f);
            return true;
        }
        if (p.targetSpecial == 4 && ancientTitan != null && ancientTitan.isAlive) {
            out.set(ancientTitan.x, ancientTitan.y + 0.90f);
            return true;
        }
        if (p.targetSpecial == 5 && playerBody != null && !playerDead) {
            Vector2 pPos = playerBody.getPosition();
            out.set(pPos.x, pPos.y + 0.25f);
            return true;
        }
        return false;
    }

    private void updateBowLockOn() {
        bowLockMob = null;
        bowLockFighter = null;
        bowLockTurret = null;
        bowLockSpecial = 0;
        bowLockPeerId = null;
        if (currentWeapon != WeaponType.BOW || playerDead || isMatchEnded || playerBody == null) return;

        Vector2 pPos = playerBody.getPosition();
        float bestDist = BOW_LOCK_RANGE;

        if (currentGameMode == GameMode.PVP) {
            if (remoteX != -999f && !remoteDead && !remoteStealth) {
                float d = Vector2.dst(pPos.x, pPos.y, remoteX, remoteY);
                if (d <= bestDist) {
                    bowLockSpecial = 3;
                    bowLockWorldPos.set(remoteX, remoteY + 0.22f);
                }
            }
            return;
        }

        if (currentGameMode == GameMode.BATTLE_5V5) {
            int pTeam = (game != null) ? game.selectedTeam : 1;
            if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && !remoteStealth
                && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)) {
                float d = Vector2.dst(pPos.x, pPos.y, remoteX, remoteY);
                if (d < bestDist) {
                    bestDist = d;
                    bowLockSpecial = 3;
                    bowLockPeerId = null;
                    bowLockWorldPos.set(remoteX, remoteY + 0.22f);
                }
            }
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.team != pTeam && !ep.dead && ep.x != -999f && !ep.stealth
                    && isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) {
                    float d = Vector2.dst(pPos.x, pPos.y, ep.x, ep.y);
                    if (d < bestDist) {
                        bestDist = d;
                        bowLockSpecial = 0;
                        bowLockFighter = null;
                        bowLockTurret = null;
                        bowLockPeerId = ep.id;
                        bowLockWorldPos.set(ep.x, ep.y + 0.22f);
                    }
                }
            }
            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.team != pTeam && !f.isDead && f.body != null && !f.isStealth && !(f.isInBush && !playerInBush)) {
                    Vector2 fp = f.body.getPosition();
                    float d = pPos.dst(fp);
                    if (d < bestDist) {
                        bestDist = d;
                        bowLockFighter = f;
                        bowLockSpecial = 0;
                        bowLockPeerId = null;
                        bowLockWorldPos.set(fp.x, fp.y + 0.22f);
                    }
                }
            }
            if (ancientTitan != null && ancientTitan.isAlive) {
                float d = Vector2.dst(pPos.x, pPos.y, ancientTitan.x, ancientTitan.y);
                if (d < bestDist) {
                    bestDist = d;
                    bowLockFighter = null;
                    bowLockPeerId = null;
                    bowLockSpecial = 4;
                    bowLockWorldPos.set(ancientTitan.x, ancientTitan.y + 0.90f);
                }
            }
            for (int i = 0; i < outpostTurrets.size; i++) {
                OutpostTurret t = outpostTurrets.get(i);
                if (t.team != pTeam && !t.isDestroyed) {
                    float d = Vector2.dst(pPos.x, pPos.y, t.x, t.y);
                    if (d < bestDist) {
                        bestDist = d;
                        bowLockFighter = null;
                        bowLockPeerId = null;
                        bowLockSpecial = 0;
                        bowLockTurret = t;
                        bowLockWorldPos.set(t.x, t.y + 0.85f);
                    }
                }
            }
            return;
        }

        for (int i = 0; i < activeMonsters.size; i++) {
            GroundMob m = activeMonsters.get(i);
            if (m.body != null && !m.isDead) {
                Vector2 mp = m.body.getPosition();
                float d = pPos.dst(mp);
                if (d < bestDist) {
                    bestDist = d;
                    bowLockMob = m;
                    bowLockSpecial = 0;
                    bowLockWorldPos.set(mp.x, mp.y + (m.isBoss ? 0.35f : 0.22f));
                }
            }
        }
        if (hasSkyPirate && pirateAlive && !isGrabbedByPirate) {
            float d = Vector2.dst(pPos.x, pPos.y, pirateX, pirateY);
            if (d < bestDist) {
                bestDist = d;
                bowLockMob = null;
                bowLockSpecial = 1;
                bowLockWorldPos.set(pirateX, pirateY + 0.15f);
            }
        }
        if (hasImperial && imperialAlive) {
            float d = Vector2.dst(pPos.x, pPos.y, imperialX, imperialY);
            if (d < bestDist) {
                bestDist = d;
                bowLockMob = null;
                bowLockSpecial = 2;
                bowLockWorldPos.set(imperialX, imperialY + 0.18f);
            }
        }
    }

    private void healPlayer(int amount) {
        if (amount <= 0 || playerDead) return;
        int prev = playerHp;
        playerHp = Math.min(playerMaxHp, playerHp + amount);
        if (playerHp > prev) {
            playerHpBar.setHp(playerHp);
            lblPlayerHp.setText("Máu: " + playerHp + " HP");
        }
    }

    private void dealAreaSkillDamage(float cx, float cy, float radius, int dmg, boolean stunMobs, Color fxColor) {
        if (currentGameMode == GameMode.PVP) {
            if (!remoteDead && remoteX != -999f && Vector2.dst(cx, cy, remoteX, remoteY) <= radius) {
                sendActionPacket("hit", dmg, 0);
                spawnHitSparks(remoteX, remoteY + 0.3f, 12, fxColor);
            }
            return;
        }
        if (currentGameMode == GameMode.BATTLE_5V5) {
            int pTeam = (game != null) ? game.selectedTeam : 1;
            int finalDmg = (int)(dmg * playerDamageBuff * (playerHasTitanBuff ? 1.35f : 1f));
            if (!remoteDead && remoteX != -999f && remoteTeam != pTeam && Vector2.dst(cx, cy, remoteX, remoteY) <= radius) {
                sendActionPacket("hit", finalDmg, 0);
                remoteHitFlashTimer = 0.20f;
                spawnHitSparks(remoteX, remoteY + 0.3f, 12, fxColor);
                addDamageText(remoteX, remoteY + 0.45f, finalDmg, fxColor);
            }
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.team != pTeam && !ep.dead && ep.x != -999f && Vector2.dst(cx, cy, ep.x, ep.y) <= radius) {
                    sendActionPacketToPeer("hit", finalDmg, 0, ep.id);
                    ep.hitFlashTimer = 0.20f;
                    spawnHitSparks(ep.x, ep.y + 0.3f, 12, fxColor);
                    addDamageText(ep.x, ep.y + 0.45f, finalDmg, fxColor);
                }
            }
            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.team != pTeam && !f.isDead && f.body != null && f.body.getPosition().dst(cx, cy) <= radius) {
                    boolean killed = f.takeDamage(finalDmg);
                    spawnHitSparks(f.body.getPosition().x, f.body.getPosition().y + 0.3f, 12, fxColor);
                    addDamageText(f.body.getPosition().x, f.body.getPosition().y + 0.45f, finalDmg, fxColor);
                    if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                }
            }
            return;
        }
        for (int i = activeMonsters.size - 1; i >= 0; i--) {
            GroundMob m = activeMonsters.get(i);
            if (m.body != null && !m.isDead && m.body.getPosition().dst(cx, cy) <= radius) {
                damageGroundMob(m, dmg, true);
                boolean bossImmune = m.isBoss && (m.isChargingBerserk || "SMASH_WINDUP".equals(m.state) || m.isResting || m.isBerserk);
                if (stunMobs && !m.isDead && !bossImmune) {
                    m.hitTimer = Math.max(m.hitTimer, 0.85f);
                    m.state = "HIT";
                }
            }
        }
        if (hasSkyPirate && pirateAlive && Vector2.dst(cx, cy, pirateX, pirateY) <= radius + 0.4f) {
            damagePirate(dmg);
        }
        if (hasImperial && imperialAlive && Vector2.dst(cx, cy, imperialX, imperialY) <= radius + 0.4f) {
            damageImperial(dmg);
        }
    }

    private void triggerSkill(int slot) {
        if (slot < 0 || slot > 2) return;
        if (playerDead || isAssassinating || isMatchEnded || isPaused || inCutscene() || playerStunTimer > 0 || isGrabbedByPirate || playerBody == null) return;

        // Xử lý cơ chế HOÁN ĐỔI đặc biệt của Vân U (Skill 2)
        if ("P2".equals(selectedChar) && slot == 1 && p2SwapAvailableTimer > 0f && p2CloneActive) {
            float oldX = playerBody.getPosition().x;
            float oldY = playerBody.getPosition().y;
            resetTraversalState();
            playerBody.setTransform(p2CloneX, p2CloneY, 0);
            p2CloneX = oldX;
            p2CloneY = oldY;
            p2SwapAvailableTimer = 0f;
            skillCooldowns[1] = 8.0f;
            SoundManager.getInstance().playDash();
            spawnHitSparks(playerBody.getPosition().x, playerBody.getPosition().y + 0.3f, 22, Color.PURPLE);
            addCustomText(playerBody.getPosition().x, playerBody.getPosition().y + 0.8f, "HOÁN ĐỔI!", Color.PURPLE);
            actionLog.setText("HOÁN ĐỔI THẦN TỐC: Đổi vị trí với Phân Thân Hắc Ám!");
            broadcastCloneSwap(p2CloneX, p2CloneY, playerFacing);
            return;
        }

        if (skillCooldowns[slot] > 0f) {
            actionLog.setText("Chiêu " + (slot + 1) + " đang hồi (" + TextFmt.oneDecimal(skillCooldowns[slot]) + "s)!");
            return;
        }

        Vector2 pPos = playerBody.getPosition();
        float timeDmgScale = 1.0f + (totalMatchTime / 60f) * 0.22f;
        boolean isP2 = "P2".equals(selectedChar);

        if (!isP2) {
            // === KỸ NĂNG VÂN TIÊU (P1 - PHONG & KIẾM KHÍ) ===
            if (slot == 0) {
                skillCooldowns[0] = 4.2f;
                int baseDmg = (int)(110 * timeDmgScale) + swordDamageBonus;
                boolean isAwakened = p1AwakenedTimer > 0f;
                int finalDmg = isAwakened ? (int)(baseDmg * 1.35f) : baseDmg;

                Projectile wave = new Projectile(pPos.x + playerFacing * 0.55f, pPos.y + 0.28f,
                    playerFacing * 14.5f, 0f, finalDmg, true, false, false, false);
                wave.skillVisualType = 1;
                wave.piercing = true;
                wave.lifeTime = 1.45f;
                wave.ownerTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;
                projectiles.add(wave);

                addP1PassiveStack();
                attackCombo = 1; attackTime = 0f; hitThisAttack = true;
                spawnHitSparks(pPos.x + playerFacing * 0.65f, pPos.y + 0.28f, 20, isAwakened ? Color.GOLD : Color.CYAN);
                SoundManager.getInstance().playPlayerSlash(true);
                addCustomText(pPos.x, pPos.y + 0.75f, isAwakened ? "CUỒNG PHONG NHẪN!" : "PHONG NHẪN!", isAwakened ? Color.GOLD : Color.CYAN);
                actionLog.setText("VÂN TIÊU – PHONG NHẪN: Kiếm khí chân không xé rách không gian!");
            } else if (slot == 1) {
                skillCooldowns[1] = 8.5f;
                p1WindShieldTimer = 4.5f;
                playerShield = Math.min(600, playerShield + 200);
                int dmg = (int)(90 * timeDmgScale) + swordDamageBonus / 2;

                // Triệt tiêu toàn bộ đạn đạo của kẻ địch trong phạm vi 3.2m (không xóa đạn đồng đội CO-OP / 5v5)
                int pTeam = (game != null) ? game.selectedTeam : 1;
                int cleared = 0;
                for (int pi = projectiles.size - 1; pi >= 0; pi--) {
                    Projectile pr = projectiles.get(pi);
                    boolean hostileRemote = pr.fromRemotePeer
                        && (currentGameMode == GameMode.PVP || (currentGameMode == GameMode.BATTLE_5V5 && pr.ownerTeam != pTeam));
                    boolean isEnemyProj = hostileRemote || (!pr.fromRemotePeer && !pr.isArrow && !pr.isSlingshotPellet && !pr.isShotgun && !pr.isCannonBall && pr.skillVisualType == 0);
                    if (isEnemyProj && Vector2.dst(pPos.x, pPos.y, pr.x, pr.y) <= 3.2f) {
                        spawnHitSparks(pr.x, pr.y, 8, Color.CYAN);
                        projectiles.removeIndex(pi);
                        cleared++;
                    }
                }
                dealAreaSkillDamage(pPos.x, pPos.y, 2.8f, dmg, false, Color.CYAN);
                spawnHitSparks(pPos.x, pPos.y + 0.3f, 28, Color.CYAN);
                spawnHitSparks(pPos.x, pPos.y + 0.3f, 16, Color.GOLD);
                cameraShakeTime = 0.24f; cameraShakeIntensity = 0.22f;
                SoundManager.getInstance().playDash();
                addP1PassiveStack();
                addCustomText(pPos.x, pPos.y + 0.85f, "LÔI PHONG HỘ THỂ! (+200 KHIÊN)", Color.SKY);
                actionLog.setText("LÔI PHONG HỘ THỂ: +200 Khiên, Phản Chấn Sấm Sét & Xóa " + cleared + " đạn địch!");
            } else {
                skillCooldowns[2] = 12.5f;
                p1SwordRainRemaining = 8;
                p1SwordRainTick = 0.05f;
                spawnHitSparks(pPos.x, pPos.y + 0.85f, 32, Color.GOLD);
                spawnHitSparks(pPos.x, pPos.y + 0.85f, 16, Color.CYAN);
                cameraShakeTime = 0.32f; cameraShakeIntensity = 0.26f;
                SoundManager.getInstance().playUltimateP1();
                addP1PassiveStack();
                addCustomText(pPos.x, pPos.y + 1.0f, "VẠN KIẾM QUY TÔNG!", Color.GOLD);
                actionLog.setText("VẠN KIẾM QUY TÔNG: 8 Thần Kiếm giáng trần khóa mục tiêu truy kích!");
            }
        } else {
            // === KỸ NĂNG VÂN U (P2 - HẮC ẢNH & HUYẾT ĐỘC) ===
            if (slot == 0) {
                skillCooldowns[0] = 4.0f;
                int dmg = (int)(72 * timeDmgScale) + swordDamageBonus / 2;
                float[] angles = new float[]{-0.20f, 0f, 0.20f};
                for (float ang : angles) {
                    float vx = playerFacing * (float) Math.cos(ang) * 14.0f;
                    float vy = (float) Math.sin(ang) * 14.0f;
                    Projectile blade = new Projectile(pPos.x + playerFacing * 0.50f, pPos.y + 0.32f,
                        vx, vy, dmg, true, false, false, false);
                    blade.skillVisualType = 3;
                    blade.applyPoison = true;
                    blade.lifeSteal = 15;
                    blade.lifeTime = 1.45f;
                    blade.ownerTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;
                    projectiles.add(blade);
                }
                attackCombo = 1; attackTime = 0f; hitThisAttack = true;
                spawnHitSparks(pPos.x + playerFacing * 0.60f, pPos.y + 0.32f, 18, Color.PURPLE);
                SoundManager.getInstance().playPlayerSlash(false);
                addCustomText(pPos.x, pPos.y + 0.75f, "HUYẾT TIÊU ĐOẠT HỒN!", Color.MAGENTA);
                actionLog.setText("VÂN U – HUYẾT TIÊU: Phóng 3 phi tiêu tẩm độc & hút máu!");
            } else if (slot == 1) {
                skillCooldowns[1] = 0.5f;
                p2SwapAvailableTimer = 2.5f;
                p2CloneActive = true;
                p2CloneX = pPos.x;
                p2CloneY = pPos.y;
                p2CloneFacing = playerFacing;
                p2CloneTimer = 2.5f;

                float destX = pPos.x + playerFacing * 3.8f;
                float destY = pPos.y;
                if (currentGameMode == GameMode.PVP) {
                    if (remoteX != -999f && !remoteDead && !remoteStealth && pPos.dst(remoteX, remoteY) <= 5.5f) {
                        destX = remoteX - remoteFacing * 0.90f;
                        destY = remoteY + 0.1f;
                        playerFacing = remoteFacing;
                    }
                } else if (currentGameMode == GameMode.BATTLE_5V5) {
                    int pTeam = (game != null) ? game.selectedTeam : 1;
                    float bestD = 5.5f;
                    if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && !remoteStealth
                        && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)) {
                        float d = pPos.dst(remoteX, remoteY);
                        if (d <= bestD) { bestD = d; destX = remoteX - remoteFacing * 0.90f; destY = remoteY + 0.1f; playerFacing = remoteFacing; }
                    }
                    for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                        RemotePeer5v5 ep = extraPeers5v5.get(epi);
                        if (ep.team != pTeam && !ep.dead && ep.x != -999f && !ep.stealth
                            && isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) {
                            float d = pPos.dst(ep.x, ep.y);
                            if (d <= bestD) { bestD = d; destX = ep.x - ep.facing * 0.90f; destY = ep.y + 0.1f; playerFacing = ep.facing; }
                        }
                    }
                    for (int fi = 0; fi < fighters5v5.size; fi++) {
                        Fighter5v5 f = fighters5v5.get(fi);
                        if (f.team != pTeam && !f.isDead && f.body != null && !f.isStealth && !(f.isInBush && !playerInBush)) {
                            float d = pPos.dst(f.body.getPosition());
                            if (d <= bestD) { bestD = d; destX = f.body.getPosition().x - f.facing * 0.90f; destY = f.body.getPosition().y + 0.1f; playerFacing = f.facing; }
                        }
                    }
                } else {
                    for (int i = 0; i < activeMonsters.size; i++) {
                        GroundMob m = activeMonsters.get(i);
                        if (m.body != null && !m.isDead && pPos.dst(m.body.getPosition()) <= 5.2f) {
                            destX = m.body.getPosition().x - m.facing * 0.90f;
                            destY = m.body.getPosition().y + 0.1f;
                            playerFacing = m.facing;
                            break;
                        }
                    }
                }
                destX = MathUtils.clamp(destX, 0.8f, mapWidth - 0.8f);
                for (int s = 1; s <= 4; s++) {
                    P2Afterimage ai = new P2Afterimage();
                    ai.x = MathUtils.lerp(pPos.x, destX, s / 5f);
                    ai.y = MathUtils.lerp(pPos.y, destY, s / 5f);
                    ai.facing = playerFacing;
                    ai.frame = denDung1Region != null ? denDung1Region : staticIdleFrame;
                    ai.color = new Color(0.65f, 0.10f, 0.90f, 0.85f);
                    p2Afterimages.add(ai);
                }
                resetTraversalState();
                playerBody.setTransform(destX, destY, 0);
                spawnHitSparks(p2CloneX, p2CloneY + 0.3f, 22, Color.PURPLE);
                SoundManager.getInstance().playDash();
                addCustomText(destX, destY + 0.8f, "ẢNH PHÂN THÂN!", Color.PURPLE);
                actionLog.setText("ẢNH PHÂN THÂN: Để lại bóng ma phát nổ & bấm X lần nữa để Hoán Đổi!");
            } else {
                skillCooldowns[2] = 13.0f;
                p2AbyssTimer = 4.2f;
                p2AbyssTick = 0.05f;
                p2AbyssShieldAccum = 0f;
                p2AbyssX = MathUtils.clamp(pPos.x + playerFacing * 2.5f, 1.5f, mapWidth - 1.5f);
                p2AbyssY = pPos.y;
                spawnHitSparks(p2AbyssX, p2AbyssY + 0.3f, 32, Color.PURPLE);
                spawnHitSparks(p2AbyssX, p2AbyssY + 0.3f, 16, Color.MAGENTA);
                cameraShakeTime = 0.30f; cameraShakeIntensity = 0.25f;
                SoundManager.getInstance().playHeavyHit();
                addCustomText(p2AbyssX, p2AbyssY + 1.0f, "HẮC ÁM HUYẾT TRẬN!", Color.MAGENTA);
                actionLog.setText("HẮC ÁM HUYẾT TRẬN: Vực thẳm hút kẻ địch & chuyển hóa sát thương thành Khiên!");
            }
        }

        // Phát sóng kỹ năng ra toàn phòng trực tuyến
        broadcastSkillCast(slot, isP2 ? 2 : 1, pPos.x, pPos.y, playerFacing);
    }

    private void updateSkills(float delta) {
        for (int s = 0; s < 3; s++) {
            if (skillCooldowns[s] > 0f) {
                skillCooldowns[s] = Math.max(0f, skillCooldowns[s] - delta);
            }
            TextButton btn = (s == 0) ? btnSkill1 : (s == 1 ? btnSkill2 : btnSkill3);
            if (btn != null) {
                int tenths = (int) Math.ceil(skillCooldowns[s] * 10f);
                boolean isP2 = "P2".equals(selectedChar);
                int stateKey = (isP2 ? 10000 : 0) + tenths;
                if (isP2 && s == 1 && p2SwapAvailableTimer > 0f) {
                    stateKey = 99990 + (int)(p2SwapAvailableTimer * 10f);
                }
                if (!isP2 && s == 0 && p1AwakenedTimer > 0f) {
                    stateKey = 88880 + (int)(p1AwakenedTimer * 10f);
                }
                if (stateKey != lastSkillBtnState[s]) {
                    lastSkillBtnState[s] = stateKey;
                    String keyHint = isDesktopPlatform()
                        ? (s == 0 ? "Z/I" : (s == 1 ? "X/O" : "C/Y"))
                        : (s == 0 ? "Z" : (s == 1 ? "X" : "C"));
                    if (isP2 && s == 1 && p2SwapAvailableTimer > 0f) {
                        btn.setStyle(skillReadyStyleP2);
                        btn.setText("HOÁN ĐỔI (" + (isDesktopPlatform() ? "X/O" : "X") + ")\n"
                            + (TextFmt.oneDecimal(p2SwapAvailableTimer) + "s"));
                    } else if (!isP2 && s == 0 && p1AwakenedTimer > 0f) {
                        btn.setStyle(skillReadyStyleP1);
                        btn.setText("CUỒNG PHONG (" + keyHint + ")\n"
                            + (TextFmt.oneDecimal(p1AwakenedTimer) + "s"));
                    } else {
                        String name = !isP2
                            ? (s == 0 ? "PHONG NHẪN" : (s == 1 ? "LÔI PHONG" : "VẠN KIẾM"))
                            : (s == 0 ? "HUYẾT TIÊU" : (s == 1 ? "PHÂN THÂN" : "HUYẾT TRẬN"));
                        if (tenths > 0) {
                            btn.setStyle(skillCdStyle);
                            btn.setText(name + "\n(" + (tenths / 10) + "." + (tenths % 10) + "s)");
                        } else {
                            btn.setStyle(isP2 ? skillReadyStyleP2 : skillReadyStyleP1);
                            btn.setText(name + " (" + keyHint + ")");
                        }
                    }
                }
            }
        }

        if (p1AwakenedTimer > 0f) {
            p1AwakenedTimer = Math.max(0f, p1AwakenedTimer - delta);
        }
        if (p1WindShieldTimer > 0f) {
            p1WindShieldTimer = Math.max(0f, p1WindShieldTimer - delta);
        }
        if (p2SwapAvailableTimer > 0f) {
            p2SwapAvailableTimer = Math.max(0f, p2SwapAvailableTimer - delta);
            if (p2SwapAvailableTimer <= 0f) {
                skillCooldowns[1] = 8.0f;
            }
        }
        if (remoteWindShieldTimer > 0f) {
            remoteWindShieldTimer = Math.max(0f, remoteWindShieldTimer - delta);
        }

        // Vạn Kiếm Quy Tông (P1 Skill 3): phóng 8 thanh thần kiếm tự tìm mục tiêu
        if (p1SwordRainRemaining > 0 && playerBody != null && !playerDead && !inCutscene()) {
            p1SwordRainTick -= delta;
            if (p1SwordRainTick <= 0f) {
                p1SwordRainTick = 0.11f;
                p1SwordRainRemaining--;
                Vector2 pPos = playerBody.getPosition();
                float sx = pPos.x + MathUtils.random(-0.85f, 0.85f);
                float sy = pPos.y + 1.25f + MathUtils.random(0f, 0.35f);
                float timeDmgScale = 1.0f + (totalMatchTime / 60f) * 0.22f;
                int swordDmg = (int)(52 * timeDmgScale) + swordDamageBonus / 3;

                Projectile sp = new Projectile(sx, sy, playerFacing * 15.0f, -2.0f, swordDmg, true, false, false, false);
                sp.skillVisualType = 2;
                sp.lifeTime = 2.8f;
                sp.ownerTeam = (game != null && game.selectedTeam == 2) ? 2 : 1;

                float best = 12.0f;
                if (currentGameMode == GameMode.PVP && remoteX != -999f && !remoteDead && !remoteStealth) {
                    sp.homingArrow = true;
                    sp.targetSpecial = 3;
                } else if (currentGameMode == GameMode.BATTLE_5V5) {
                    int pTeam = (game != null) ? game.selectedTeam : 1;
                    if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && !remoteStealth
                        && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)) {
                        float d = Vector2.dst(sx, sy, remoteX, remoteY);
                        if (d < best) { best = d; sp.homingArrow = true; sp.targetSpecial = 3; }
                    }
                    for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                        RemotePeer5v5 ep = extraPeers5v5.get(epi);
                        if (ep.team != pTeam && !ep.dead && ep.x != -999f && !ep.stealth
                            && isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) {
                            float d = Vector2.dst(sx, sy, ep.x, ep.y);
                            if (d < best) { best = d; sp.homingArrow = true; sp.targetSpecial = 0; sp.targetFighter = null; sp.targetPeerId = ep.id; }
                        }
                    }
                    for (int fi = 0; fi < fighters5v5.size; fi++) {
                        Fighter5v5 f = fighters5v5.get(fi);
                        if (f.team != pTeam && !f.isDead && f.body != null && !f.isStealth && !(f.isInBush && !playerInBush)) {
                            float d = f.body.getPosition().dst(sx, sy);
                            if (d < best) { best = d; sp.targetFighter = f; sp.targetSpecial = 0; sp.targetPeerId = null; sp.homingArrow = true; }
                        }
                    }
                } else {
                    for (int mi = 0; mi < activeMonsters.size; mi++) {
                        GroundMob m = activeMonsters.get(mi);
                        if (m.body != null && !m.isDead) {
                            float d = m.body.getPosition().dst(sx, sy);
                            if (d < best) { best = d; sp.targetMob = m; sp.homingArrow = true; }
                        }
                    }
                    if (!sp.homingArrow && hasSkyPirate && pirateAlive) {
                        sp.homingArrow = true; sp.targetSpecial = 1;
                    } else if (!sp.homingArrow && hasImperial && imperialAlive) {
                        sp.homingArrow = true; sp.targetSpecial = 2;
                    }
                }
                projectiles.add(sp);
                spawnHitSparks(sx, sy, 6, Color.CYAN);
                SoundManager.getInstance().playSlash();
            }
        }

        // Remote Vạn Kiếm Quy Tông (CharId=1) — chỉ hiển thị hiệu ứng (sát thương đi qua gói "hit" của chủ chiêu)
        if (remoteSwordRainRemaining > 0) {
            remoteSwordRainTick -= delta;
            if (remoteSwordRainTick <= 0f) {
                remoteSwordRainTick = 0.11f;
                remoteSwordRainRemaining--;
                float sx = remoteSwordRainX + MathUtils.random(-0.85f, 0.85f);
                float sy = remoteSwordRainY + 1.25f + MathUtils.random(0f, 0.35f);

                Projectile sp = new Projectile(sx, sy, remoteSwordRainFacing * 15.0f, -2.0f, 0, true, false, false, false);
                sp.skillVisualType = 2;
                sp.lifeTime = 2.8f;
                sp.fromRemotePeer = true;
                sp.ownerTeam = remoteSwordRainTeam;

                int pTeam = (game != null) ? game.selectedTeam : 1;
                if ((currentGameMode == GameMode.PVP || (currentGameMode == GameMode.BATTLE_5V5 && remoteSwordRainTeam != pTeam)) && !playerDead) {
                    sp.homingArrow = true;
                    sp.targetSpecial = 5;
                }
                projectiles.add(sp);
                spawnHitSparks(sx, sy, 6, Color.CYAN);
                SoundManager.getInstance().playSlash();
            }
        }

        // Ảnh Phân Thân phát nổ (P2 Skill 2)
        if (p2CloneActive && !inCutscene()) {
            p2CloneTimer -= delta;
            boolean touchedByEnemy = false;
            if (currentGameMode == GameMode.PVP) {
                if (remoteX != -999f && !remoteDead && Vector2.dst(remoteX, remoteY, p2CloneX, p2CloneY) <= 1.25f) {
                    touchedByEnemy = true;
                }
            } else if (currentGameMode == GameMode.BATTLE_5V5) {
                int pTeam = (game != null) ? game.selectedTeam : 1;
                if (remoteX != -999f && !remoteDead && remoteTeam != pTeam
                    && Vector2.dst(remoteX, remoteY, p2CloneX, p2CloneY) <= 1.25f) {
                    touchedByEnemy = true;
                }
                if (!touchedByEnemy) {
                    for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                        RemotePeer5v5 ep = extraPeers5v5.get(epi);
                        if (ep.team != pTeam && !ep.dead && ep.x != -999f
                            && Vector2.dst(ep.x, ep.y, p2CloneX, p2CloneY) <= 1.25f) {
                            touchedByEnemy = true;
                            break;
                        }
                    }
                }
                if (!touchedByEnemy) {
                    for (int fi = 0; fi < fighters5v5.size; fi++) {
                        Fighter5v5 f = fighters5v5.get(fi);
                        if (f.team != pTeam && !f.isDead && f.body != null
                            && f.body.getPosition().dst(p2CloneX, p2CloneY) <= 1.25f) {
                            touchedByEnemy = true;
                            break;
                        }
                    }
                }
            } else {
                for (int mi = 0; mi < activeMonsters.size; mi++) {
                    GroundMob m = activeMonsters.get(mi);
                    if (m.body != null && !m.isDead && m.body.getPosition().dst(p2CloneX, p2CloneY) <= 1.15f) {
                        touchedByEnemy = true;
                        break;
                    }
                }
            }
            if (p2CloneTimer <= 0f || touchedByEnemy) {
                p2CloneActive = false;
                if (p2SwapAvailableTimer > 0f) {
                    p2SwapAvailableTimer = 0f;
                    skillCooldowns[1] = 8.0f;
                }
                float timeDmgScale = 1.0f + (totalMatchTime / 60f) * 0.22f;
                int boomDmg = (int)(140 * timeDmgScale) + swordDamageBonus;
                dealAreaSkillDamage(p2CloneX, p2CloneY, 2.8f, boomDmg, true, Color.PURPLE);
                spawnHitSparks(p2CloneX, p2CloneY + 0.3f, 28, Color.PURPLE);
                spawnHitSparks(p2CloneX, p2CloneY + 0.3f, 14, Color.MAGENTA);
                cameraShakeTime = 0.28f; cameraShakeIntensity = 0.26f;
                SoundManager.getInstance().playHeavyHit();
                addCustomText(p2CloneX, p2CloneY + 0.7f, "PHÂN THÂN NỔ!", Color.MAGENTA);
            }
        }

        // Remote Ảnh Phân Thân phát nổ (CharId=2) — chỉ hiệu ứng hình ảnh (chủ chiêu gửi gói "hit" chuẩn)
        if (remoteCloneActive) {
            remoteCloneTimer -= delta;
            boolean touchedByPlayer = false;
            int pTeam = (game != null) ? game.selectedTeam : 1;
            boolean hostileClone = (currentGameMode == GameMode.PVP)
                || (currentGameMode == GameMode.BATTLE_5V5 && remoteCloneTeam != pTeam);
            if (hostileClone && playerBody != null && !playerDead) {
                if (Vector2.dst(remoteCloneX, remoteCloneY, playerBody.getPosition().x, playerBody.getPosition().y) <= 1.25f) {
                    touchedByPlayer = true;
                }
            }
            if (remoteCloneTimer <= 0f || touchedByPlayer) {
                remoteCloneActive = false;
                spawnHitSparks(remoteCloneX, remoteCloneY + 0.3f, 28, Color.PURPLE);
                spawnHitSparks(remoteCloneX, remoteCloneY + 0.3f, 14, Color.MAGENTA);
                cameraShakeTime = 0.28f; cameraShakeIntensity = 0.26f;
                SoundManager.getInstance().playHeavyHit();
                addCustomText(remoteCloneX, remoteCloneY + 0.7f, "PHÂN THÂN NỔ!", Color.MAGENTA);
            }
        }

        // Hắc Ám Huyết Trận (P2 Skill 3): hút quái / đối thủ vào tâm & chuyển hóa thành Khiên Hắc Ám
        if (p2AbyssTimer > 0f && !inCutscene()) {
            p2AbyssTimer -= delta;
            p2AbyssTick -= delta;
            for (int mi = 0; mi < activeMonsters.size; mi++) {
                GroundMob m = activeMonsters.get(mi);
                if (m.body != null && !m.isDead) {
                    Vector2 mp = m.body.getPosition();
                    float dx = p2AbyssX - mp.x;
                    float dy = p2AbyssY - mp.y;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d <= 3.5f && d > 0.20f) {
                        m.slowTimer = Math.max(m.slowTimer, 0.3f);
                        float pullVx = Math.signum(dx) * (m.isBoss ? 1.5f : 2.8f);
                        m.body.setLinearVelocity(pullVx, m.body.getLinearVelocity().y);
                    }
                }
            }
            if (p2AbyssTick <= 0f) {
                p2AbyssTick = 0.5f;
                float timeDmgScale = 1.0f + (totalMatchTime / 60f) * 0.22f;
                int tickDmg = (int)(44 * timeDmgScale) + swordDamageBonus / 3;
                dealAreaSkillDamage(p2AbyssX, p2AbyssY, 3.4f, tickDmg, false, Color.PURPLE);
                healPlayer(10);
                p2AbyssShieldAccum = Math.min(400, p2AbyssShieldAccum + 12);
                playerShield = Math.min(600, playerShield + 12);
                spawnHitSparks(p2AbyssX + MathUtils.random(-1.4f, 1.4f), p2AbyssY + 0.25f, 10, Color.PURPLE);
            }
        }

        // Remote Hắc Ám Huyết Trận (CharId=2) — chỉ hút vật lý khi đối địch (sát thương qua gói "hit" của chủ chiêu)
        if (remoteAbyssTimer > 0f) {
            remoteAbyssTimer -= delta;
            remoteAbyssTick -= delta;
            int pTeam = (game != null) ? game.selectedTeam : 1;
            boolean hostileAbyss = (currentGameMode == GameMode.PVP)
                || (currentGameMode == GameMode.BATTLE_5V5 && remoteAbyssTeam != pTeam);
            if (hostileAbyss && playerBody != null && !playerDead) {
                Vector2 pPos = playerBody.getPosition();
                float dst = Vector2.dst(remoteAbyssX, remoteAbyssY, pPos.x, pPos.y);
                if (dst <= 3.6f) {
                    float pull = (remoteAbyssX > pPos.x) ? 1.6f : -1.6f;
                    playerBody.applyLinearImpulse(new Vector2(pull * delta * 5.5f, 0f), playerBody.getWorldCenter(), true);
                }
            }
            if (remoteAbyssTick <= 0f) remoteAbyssTick = 0.5f;
        }
    }

    private void triggerEagleStrike() {
        if (isEagleStriking || playerDead || isAssassinating || isMatchEnded || isPaused || inCutscene()
            || isGrabbedByPirate || playerStunTimer > 0) return;

        Vector2 pos = playerBody.getPosition();
        boolean isHighGround = (pos.y >= 3.5f || isPlayerOnTowerSummit() || (isClimbing && pos.y >= 3.2f));

        if (!isHighGround) {
            actionLog.setText("CẦN LEO LÊN CAO / NÓC NHÀ ĐỂ ƯNG KÍCH!");
            return;
        }

        boolean hasHumanTarget = lockOnIsPrimaryRemote || lockOnPeerId != null;
        if (lockOnEnemy == null && lockOnFighter == null && !hasHumanTarget && currentGameMode != GameMode.PVP) {
            actionLog.setText("KHÔNG CÓ MỤC TIÊU TRONG TẦM MẮT ĐỂ ƯNG KÍCH!");
            return;
        }
        // Khóa mục tiêu ngay lúc xuất chiêu. Bản cũ đọc lockOnEnemy mỗi frame nên khi lao xuống
        // dưới 2.8m mục tiêu bị mất và nhân vật lao lệch sang con quái đầu tiên trong danh sách.
        eagleTargetMob = lockOnEnemy;
        eagleTargetFighter = lockOnFighter;
        eagleTargetIsPrimaryRemote = lockOnIsPrimaryRemote;
        eagleTargetPeerId = lockOnPeerId;
        pendingEagleGroundAbort = false;

        isEagleStriking = true;
        eagleStrikePhase = 1; // Phase 1: High-diver Athletic Leaping Arc Launch
        eagleStrikeLaunchTimer = 0.32f;
        eagleStrikeTimeout = 0f;
        if (currentSwingingRope != null) {
            currentSwingingRope.release();
            currentSwingingRope = null;
        }
        isSwinging = false;
        isClimbing = false;

        playerBody.setGravityScale(0.3f);
        playerBody.setBullet(true);

        // Athletic Launch Impulse (Jump UP into the air like a high diver)
        float targetX;
        if (eagleTargetMob != null && eagleTargetMob.body != null) targetX = eagleTargetMob.body.getPosition().x;
        else if (eagleTargetFighter != null && eagleTargetFighter.body != null) targetX = eagleTargetFighter.body.getPosition().x;
        else if (eagleTargetIsPrimaryRemote && remoteX != -999f) targetX = remoteX;
        else if (eagleTargetPeerId != null) {
            RemotePeer5v5 ep = findExtraPeer(eagleTargetPeerId);
            targetX = (ep != null && ep.x != -999f) ? ep.x : (playerBody.getPosition().x + playerFacing);
        } else if (currentGameMode == GameMode.PVP && remoteX != -999f) targetX = remoteX;
        else targetX = playerBody.getPosition().x + playerFacing;
        float pushDir = Math.signum(targetX - playerBody.getPosition().x);
        if (pushDir == 0) pushDir = playerFacing;
        playerBody.setLinearVelocity(pushDir * 2.8f, 5.8f);
        SoundManager.getInstance().playDash();

        actionLog.setText("ƯNG KÍCH: BẬT CAO LÊN KHÔNG TRUNG!");
    }

    private void stopEagleStrike(boolean hitTarget) {
        isEagleStriking = false;
        eagleStrikePhase = 0;
        pendingEagleGroundAbort = false;
        playerBody.setGravityScale(1f);
        playerBody.setBullet(false);

        eagleTargetMob = null;
        eagleTargetFighter = null;
        eagleTargetIsPrimaryRemote = false;
        eagleTargetPeerId = null;

        if (hitTarget) {
            SoundManager.getInstance().playHeavyHit();
            hitStopTimer = 0.08f;
            cameraShakeTime = 0.45f;
            cameraShakeIntensity = 0.35f;
            actionLog.setText("ƯNG KÍCH ÁM SÁT! -" + EAGLE_DMG + " SÁT THƯƠNG CHÍ MẠNG!");
            playerBody.setLinearVelocity(0, 3.2f); // Graceful rebound bounce
        } else {
            playerBody.setLinearVelocity(0, 0);
        }
    }

    /**
     * Sát thương va chạm của Ứng Kích, áp dụng MỘT lần (bản cũ cộng thêm lần nữa trong stopEagleStrike
     * nên mục tiêu khóa nhận gấp đôi). Trả về true nếu trúng ít nhất 1 mục tiêu.
     */
    private boolean applyEagleImpactDamage(float radius) {
        Vector2 pos = playerBody.getPosition();
        boolean hit = false;
        if (currentGameMode == GameMode.PVP) {
            if (remoteX != -999f && pos.dst(remoteX, remoteY) <= radius && !remoteDead) {
                sendActionPacket("hit", EAGLE_DMG, 0);
                hit = true;
            }
            return hit;
        }
        for (int i = activeMonsters.size - 1; i >= 0; i--) {
            GroundMob m = activeMonsters.get(i);
            if (m.body != null && !m.isDead && pos.dst(m.body.getPosition()) <= radius) {
                float mx = m.body.getPosition().x, my = m.body.getPosition().y;
                damageGroundMob(m, EAGLE_DMG, true);
                addCustomText(mx, my + 0.8f, "LAO XUỐNG ÁM SÁT!", Color.GOLD);
                hit = true;
            }
        }
        if (currentGameMode == GameMode.BATTLE_5V5) {
            int pTeam = (game != null) ? game.selectedTeam : 1;
            int dmg = (int) (EAGLE_DMG * playerDamageBuff * (playerHasTitanBuff ? 1.35f : 1f));
            if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && pos.dst(remoteX, remoteY) <= radius) {
                sendActionPacket("hit", dmg, 0);
                remoteHitFlashTimer = 0.25f;
                spawnHitSparks(remoteX, remoteY + 0.3f, 25, Color.GOLD);
                addDamageText(remoteX, remoteY + 0.5f, dmg, Color.GOLD);
                hit = true;
            }
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.team != pTeam && !ep.dead && ep.x != -999f && pos.dst(ep.x, ep.y) <= radius) {
                    sendActionPacketToPeer("hit", dmg, 0, ep.id);
                    ep.hitFlashTimer = 0.25f;
                    spawnHitSparks(ep.x, ep.y + 0.3f, 25, Color.GOLD);
                    addDamageText(ep.x, ep.y + 0.5f, dmg, Color.GOLD);
                    hit = true;
                }
            }
            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.team != pTeam && !f.isDead && f.body != null && pos.dst(f.body.getPosition()) <= radius) {
                    float fx = f.body.getPosition().x, fy = f.body.getPosition().y;
                    boolean killed = f.takeDamage(dmg);
                    spawnHitSparks(fx, fy + 0.3f, 25, Color.GOLD);
                    addDamageText(fx, fy + 0.5f, dmg, Color.GOLD);
                    if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                    hit = true;
                }
            }
        }
        return hit;
    }

    private void triggerDash() {
        if (playerDead || isAssassinating || isDashing || dashTime > 0 || isClimbing
            || isEagleStriking || isMatchEnded || isGrabbedByPirate || playerStunTimer > 0 || inCutscene()) return;
        if (dashCooldownTimer > 0) {
            actionLog.setText("Lướt đang hồi chiêu (" + TextFmt.oneDecimal(dashCooldownTimer) + "s)!");
            return;
        }

        if (isSwinging) {
            if (currentSwingingRope != null) {
                currentSwingingRope.release();
                currentSwingingRope = null;
            }
            isSwinging = false;
            swingCooldown = 0.5f;
        }

        if (isCrouching) {
            if (!canStandUp()) return;
            isCrouching = false;
            needsHitboxUpdate = true;
        }
        isDashing = true;
        dashTime = DASH_DURATION;
        dashCooldownTimer = 1.1f;
        hitThisDash = false;
        // FIX: reset full combo state on dash
        attackCombo = 0;
        attackTime = 0f;
        queuedAttacks = 0;
        hitThisAttack = false;
        SoundManager.getInstance().playDash();
        actionLog.setText("Lướt trảm né đòn!");
    }

    private void createMobileUI() {
        uiStage = new Stage(new FitViewport(1280, 720));

        skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", managedTex(pixmap));

        // Font & style dùng chung từ UiKit (font Linear mượt, nút bo góc viền vàng).
        // Không add font vào skin nữa: font thuộc UiKit, skin.dispose() sẽ không hủy nhầm.
        BitmapFont font = UiKit.normal();
        BitmapFont hudFont = UiKit.normal();

        TextButton.TextButtonStyle btnStyle = UiKit.buttonStyle(UiKit.GRAY, font);
        TextButton.TextButtonStyle exitBtnStyle = UiKit.buttonStyle(UiKit.RED, font);
        TextButton.TextButtonStyle eagleBtnStyle = UiKit.buttonStyle(new Color(0.78f, 0.52f, 0.08f, 1f), font);
        TextButton.TextButtonStyle cannonBtnStyle = UiKit.buttonStyle(new Color(0.74f, 0.24f, 0.10f, 1f), font);
        TextButton.TextButtonStyle weaponBtnStyle = UiKit.buttonStyle(new Color(0.18f, 0.44f, 0.66f, 1f), font);

        boolean isDeskUi = isDesktopPlatform();
        TextButton btnPause = new TextButton(isDeskUi ? "II MENU (Esc)" : "II  TẠM DỪNG", btnStyle);
        btnPause.setSize(142, 42);
        btnPause.setPosition(20, 665);
        btnPause.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { togglePause(); }
        });
        uiStage.addActor(btnPause);

        // Nút đổi nhanh 5 vũ khí ngay trên góc trái (cạnh nút tạm dừng, không đè lên HUD giữa)
        TextButton btnSwitchWeapon = new TextButton(isDeskUi ? "ĐỔI VK (Tab)" : "ĐỔI VK", weaponBtnStyle);
        btnSwitchWeapon.setSize(isDeskUi ? 124 : 110, 42);
        btnSwitchWeapon.setPosition(168, 665);
        btnSwitchWeapon.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { cycleWeapon(); }
        });
        uiStage.addActor(btnSwitchWeapon);

        // (Bỏ nút "THOÁT" cạnh nút tạm dừng – dễ bấm nhầm mất trận; thoát nằm trong menu tạm dừng)

        TextButton btnShop = new TextButton("TIỆM ĐỒ (B)", eagleBtnStyle);
        btnShop.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { toggleShop(); }
        });

        TextButton btnPotion = new TextButton("THUỐC (H)", btnStyle);
        btnPotion.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { useHealthPotion(); }
        });

        lblPlayerHp = new Label("Máu: " + playerHp + " HP", new Label.LabelStyle(hudFont, Color.GREEN));
        playerHpBar = new HealthBar(skin, new Color(0.15f, 0.85f, 0.25f, 1f), false);
        playerHpBar.setMaxHp(playerMaxHp);
        playerHpBar.setHp(playerHp);

        lblEnemyInfo = new Label(currentGameMode == GameMode.PVP ? "Đối thủ: " + remoteHp + " HP" : "SÀN ĐẤU",
            new Label.LabelStyle(UiKit.bodyBold(), Color.RED));
        lblEnemyInfo.setFontScale(0.6f);
        enemyHpBar = new HealthBar(skin, new Color(0.9f, 0.2f, 0.2f, 1f), true);
        if (currentGameMode == GameMode.PVP) {
            enemyHpBar.setMaxHp(playerMaxHp);
            enemyHpBar.setHp(remoteHp);
        }

        lblPartnerHp = new Label("Đồng đội: 100 HP", new Label.LabelStyle(font, Color.CYAN));
        partnerHpBar = new HealthBar(skin, new Color(0.2f, 0.8f, 0.9f, 1f), false);
        lblPartnerHp.setVisible(currentGameMode == GameMode.COOP);
        partnerHpBar.setVisible(currentGameMode == GameMode.COOP);

        lblCurrentWeapon = new Label("Vũ khí: Kiếm thường", new Label.LabelStyle(font, Color.YELLOW));
        lblScoreGold = new Label("Điểm: 0  •  Vàng: 0", new Label.LabelStyle(font, Color.GOLD));
        lblPotions = new Label("Thuốc: x" + healthPotions, new Label.LabelStyle(font, Color.GREEN));
        actionLog = new Label(currentGameMode == GameMode.PVP ? "ĐẤU TRƯỜNG 1V1! TUYỆT KỸ ĐÃ SẴN SÀNG (U)!" : "XUẤT TRẬN! TUYỆT KỸ ĐÃ SẴN SÀNG (U)!",
            new Label.LabelStyle(UiKit.bodyBold(), Color.GOLD));
        actionLog.setFontScale(0.6f); // font có dấu tiếng Việt

        Table topHud = new Table();
        topHud.setFillParent(true);
        topHud.top().padTop(10).padLeft(285).padRight(20);

        Table leftBox = new Table();
        leftBox.left();
        leftBox.setBackground(UiKit.framed(new Color(0.03f, 0.04f, 0.07f, 0.55f), new Color(0.55f, 0.45f, 0.25f, 0.55f)));
        leftBox.pad(6, 10, 8, 10);
        leftBox.add(lblPlayerHp).left().padBottom(3).row();
        leftBox.add(playerHpBar).size(220, 18).left().row();
        if (currentGameMode == GameMode.COOP) {
            leftBox.add(lblPartnerHp).left().padTop(3).padBottom(3).row();
            leftBox.add(partnerHpBar).size(220, 18).left().row();
        }
        leftBox.add(lblCurrentWeapon).left().padTop(3).row();
        if (currentGameMode != GameMode.PVP) {
            leftBox.add(lblScoreGold).left().padTop(3).row();
            leftBox.add(lblPotions).left().padTop(3).row();

            Table quickBar = new Table();
            quickBar.left();
            quickBar.add(btnPotion).size(115, 36).padRight(8);
            quickBar.add(btnShop).size(125, 36);
            leftBox.add(quickBar).left().padTop(6);
        }

        if (currentGameMode == GameMode.BATTLE_5V5) {
            String pName = (game != null && game.playerName != null) ? game.playerName : "Chiến Thần";
            int pTeam = (game != null) ? game.selectedTeam : 1;
            lblPlayerHp.setText("[BẠN – " + pName + "]: " + playerHp + " HP");
            lblPlayerHp.setColor(pTeam == 1 ? Color.CYAN : Color.SALMON);
            lblScore5v5 = new Label("[XANH] 0  –  0 [ĐỎ]  •  MỤC TIÊU: 30 MẠNG", new Label.LabelStyle(font, Color.GOLD));
            lblKillStreak = new Label("", new Label.LabelStyle(font, Color.YELLOW));
        }

        Table centerBox = new Table();
        centerBox.background(UiKit.framed(new Color(0.04f, 0.05f, 0.08f, 0.80f), UiKit.GOLD_DIM));
        centerBox.pad(8, 16, 8, 16);
        if (currentGameMode == GameMode.BATTLE_5V5 && lblScore5v5 != null) {
            centerBox.add(lblScore5v5).padBottom(4).center().row();
        }
        actionLog.setWrap(true);
        actionLog.setAlignment(Align.center);
        centerBox.add(actionLog).width(360).center();
        if (currentGameMode == GameMode.BATTLE_5V5 && lblKillStreak != null) {
            centerBox.row();
            centerBox.add(lblKillStreak).padTop(4).center();
        }

        Table rightBox = new Table();
        rightBox.right();
        rightBox.setBackground(UiKit.framed(new Color(0.03f, 0.04f, 0.07f, 0.55f), new Color(0.55f, 0.45f, 0.25f, 0.55f)));
        rightBox.pad(6, 10, 8, 10);
        rightBox.add(lblEnemyInfo).right().padBottom(3).row();
        rightBox.add(enemyHpBar).size(220, 18).right();
        if (currentGameMode == GameMode.BATTLE_5V5) rightBox.setVisible(false);

        topHud.add(leftBox).left().expandX();
        topHud.add(centerBox).center();
        topHud.add(rightBox).right().expandX();
        uiStage.addActor(topHud);

        btnEagleStrike = new TextButton("ƯNG KÍCH (K)", eagleBtnStyle);
        btnEagleStrike.setSize(190, 56);
        btnEagleStrike.setPosition(1066, 256);
        btnEagleStrike.setVisible(false);
        btnEagleStrike.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { triggerEagleStrike(); }
        });
        uiStage.addActor(btnEagleStrike);

        TextButton.TextButtonStyle assStyle = UiKit.buttonStyle(new Color(0.72f, 0.10f, 0.14f, 1f), font);

        btnAssassinate = new TextButton("ÁM SÁT (F)", assStyle);
        btnAssassinate.setSize(190, 56);
        btnAssassinate.setPosition(866, 256);
        btnAssassinate.setVisible(false);
        btnAssassinate.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                triggerAssassinate();
            }
        });
        uiStage.addActor(btnAssassinate);

        btnShootCannon = new TextButton("BẮN PHÁO (E)", cannonBtnStyle);
        btnShootCannon.setSize(190, 56);
        btnShootCannon.setPosition(1066, 322);
        btnShootCannon.setVisible(false);
        btnShootCannon.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { shootCannonAtImperial(); }
        });
        uiStage.addActor(btnShootCannon);

        // Nói chuyện với Tiểu Vũ (chơi đơn) – phím E
        TextButton.TextButtonStyle talkStyle = new TextButton.TextButtonStyle(eagleBtnStyle);
        talkStyle.font = UiKit.bodyBold();
        btnTalkNPC = new TextButton("NÓI CHUYỆN (E)", talkStyle);
        btnTalkNPC.getLabel().setFontScale(0.6f);
        btnTalkNPC.setSize(190, 56);
        btnTalkNPC.setPosition(866, 322);
        btnTalkNPC.setVisible(false);
        btnTalkNPC.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { talkToBoy(); }
        });
        uiStage.addActor(btnTalkNPC);

        // Tên chương/ải + mục tiêu, hiện vài giây sau phần mở đầu
        Label.LabelStyle bannerStyle = new Label.LabelStyle(UiKit.bodyBold(), new Color(1f, 0.88f, 0.55f, 1f));
        bannerStyle.background = UiKit.framed(new Color(0.03f, 0.03f, 0.06f, 0.78f), new Color(0.85f, 0.68f, 0.30f, 0.8f));
        lblStageBanner = new Label("", bannerStyle);
        lblStageBanner.setFontScale(0.72f);
        lblStageBanner.setAlignment(Align.center);
        lblStageBanner.getColor().a = 0f;
        lblStageBanner.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        uiStage.addActor(lblStageBanner);

        btnRevivePartner = new TextButton("CỨU ĐỒNG ĐỘI (E)", eagleBtnStyle);
        btnRevivePartner.setSize(190, 56);
        btnRevivePartner.setPosition(866, 322);
        btnRevivePartner.setVisible(false);
        btnRevivePartner.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { revivePartner(); }
        });
        uiStage.addActor(btnRevivePartner);

        // Full-screen Dark Modal Backdrop (chặn bấm xuyên xuống HUD)
        modalOverlay = UiKit.dimOverlay(0.65f);
        modalOverlay.setSize(1280, 720);
        modalOverlay.setVisible(false);
        uiStage.addActor(modalOverlay);

        // Pause Menu Dialog
        pauseDialog = new Table();
        pauseDialog.setSize(480, 360);
        pauseDialog.setPosition(1280 / 2f - 240, 720 / 2f - 180);
        pauseDialog.background(UiKit.panel());
        pauseDialog.setVisible(false);

        Label lblPauseTitle = new Label(isOnlineMode() ? "MENU (TRẬN TRỰC TUYẾN VẪN ĐANG DIỄN RA)" : "TẠM DỪNG",
            new Label.LabelStyle(UiKit.large(), UiKit.GOLD));
        lblPauseTitle.setAlignment(Align.center);

        TextButton btnResume = new TextButton("CHƠI TIẾP", UiKit.buttonStyle(UiKit.GREEN, font));
        TextButton btnSettings = new TextButton("CÀI ĐẶT", btnStyle);
        TextButton btnPauseExit = new TextButton("THOÁT RA MENU", exitBtnStyle);
        btnPauseResume = btnResume;
        btnPauseSettings = btnSettings;
        btnPauseExitBtn = btnPauseExit;
        updatePauseSelectionVisual();
        btnSettings.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                isSettingsModalOpen = true;
                SettingsDialog.show(uiStage, game, () -> isSettingsModalOpen = false);
            }
        });

        btnResume.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { togglePause(); }
        });
        btnPauseExit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                togglePause();
                exitToMenu();
            }
        });

        pauseDialog.add(lblPauseTitle).center().padBottom(26).row();
        pauseDialog.add(btnResume).size(310, 60).padBottom(14).row();
        pauseDialog.add(btnSettings).size(310, 60).padBottom(14).row();
        pauseDialog.add(btnPauseExit).size(310, 60);
        uiStage.addActor(pauseDialog);

        // Game Over dialog
        gameOverDialog = new Table();
        gameOverDialog.setSize(640, 360);
        gameOverDialog.setPosition(1280 / 2f - 320, 720 / 2f - 180);
        gameOverDialog.background(UiKit.panel());
        gameOverDialog.setVisible(false);

        lblEndGameTitle = new Label("KẾT THÚC", new Label.LabelStyle(UiKit.display(), Color.GOLD));
        lblEndGameTitle.setFontScale(0.48f);
        lblEndGameTitle.setAlignment(Align.center);
        lblEndStats = new Label("", new Label.LabelStyle(UiKit.body(), Color.WHITE));
        lblEndStats.setFontScale(0.64f);
        lblEndStats.setAlignment(Align.center);
        lblEndStats.setWrap(true);
        btnPlayAgain = new TextButton("CHƠI LẠI", UiKit.buttonStyle(UiKit.GREEN, font)); // nút chính: xanh
        btnWaitRevive = new TextButton("ĐỢI HỒI SINH", eagleBtnStyle);
        btnWaitRevive.setVisible(false); // chỉ hiện ở CO-OP khi gục ngã
        btnQuitGame = new TextButton("VỀ MENU", exitBtnStyle);

        btnPlayAgain.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { restartOrReturnLobby(); }
        });
        btnWaitRevive.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentGameMode == GameMode.COOP && playerDead) {
                    setGameOverDialogVisible(false);
                    actionLog.setText("ĐANG CHỜ ĐỒNG ĐỘI CỨU... (ĐANG XEM GÓC NHÌN ĐỒNG ĐỘI)");
                }
            }
        });
        btnQuitGame.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { exitToMenu(); }
        });

        gameOverDialog.add(lblEndGameTitle).center().padBottom(15).row();
        gameOverDialog.add(lblEndStats).width(580).center().padBottom(25).row();
        dialogButtons = new Table();
        updateDialogButtons();
        gameOverDialog.add(dialogButtons).center();
        uiStage.addActor(gameOverDialog);

        // Virtual Joystick Touchpad for Movement (Enlarged & Ergonomic for all screens)
        Pixmap padBgPix = new Pixmap(180, 180, Pixmap.Format.RGBA8888);
        padBgPix.setColor(new Color(0.10f, 0.12f, 0.18f, 0.70f));
        padBgPix.fillCircle(90, 90, 86);
        padBgPix.setColor(new Color(0.85f, 0.75f, 0.40f, 0.85f));
        padBgPix.drawCircle(90, 90, 86);
        padBgPix.drawCircle(90, 90, 85);
        padBgTex = managedTex(padBgPix);

        Pixmap padKnobPix = new Pixmap(78, 78, Pixmap.Format.RGBA8888);
        padKnobPix.setColor(new Color(0.85f, 0.25f, 0.2f, 0.85f));
        padKnobPix.fillCircle(39, 39, 36);
        padKnobPix.setColor(Color.WHITE);
        padKnobPix.drawCircle(39, 39, 36);
        padKnobTex = managedTex(padKnobPix);

        Touchpad.TouchpadStyle touchpadStyle = new Touchpad.TouchpadStyle();
        touchpadStyle.background = new TextureRegionDrawable(new TextureRegion(padBgTex));
        touchpadStyle.knob = new TextureRegionDrawable(new TextureRegion(padKnobTex));

        touchpad = new Touchpad(8f, touchpadStyle);
        touchpad.setBounds(45, 35, 210, 210);
        uiStage.addActor(touchpad);

        // Styled Action buttons with role colors and key hints
        TextButton.TextButtonStyle atkBtnStyle = UiKit.buttonStyle(new Color(0.72f, 0.16f, 0.16f, 1f), UiKit.large());
        TextButton.TextButtonStyle jumpBtnStyle = UiKit.buttonStyle(new Color(0.16f, 0.55f, 0.28f, 1f), font);
        TextButton.TextButtonStyle dashBtnStyle = UiKit.buttonStyle(new Color(0.16f, 0.40f, 0.70f, 1f), font);
        TextButton.TextButtonStyle crouchBtnStyle = UiKit.buttonStyle(new Color(0.34f, 0.26f, 0.50f, 1f), font);
        TextButton.TextButtonStyle climbBtnStyle = UiKit.buttonStyle(new Color(0.42f, 0.31f, 0.18f, 1f), font);

        ultBtnNormalStyle = UiKit.buttonStyle(UiKit.GRAY, font);
        ultBtnReadyStyle = UiKit.buttonStyle(new Color(0.80f, 0.60f, 0.08f, 1f), font);
        ultBtnP2LockStyle = UiKit.buttonStyle(new Color(0.80f, 0.10f, 0.12f, 1f), font);

        skillReadyStyleP1 = UiKit.buttonStyle(new Color(0.14f, 0.50f, 0.68f, 1f), UiKit.smallBold());
        skillReadyStyleP2 = UiKit.buttonStyle(new Color(0.58f, 0.14f, 0.48f, 1f), UiKit.smallBold());
        skillCdStyle = UiKit.buttonStyle(UiKit.GRAY, UiKit.smallBold());

        boolean isP1Init = !"P2".equals(selectedChar);
        btnUltimate = new TextButton(isP1Init ? "TRẤN TRỜI (U)" : "TÀNG HÌNH (U)", ultBtnReadyStyle);
        btnUltimate.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { triggerUltimate(); }
        });

        TextButton.TextButtonStyle initSkillStyle = isP1Init ? skillReadyStyleP1 : skillReadyStyleP2;
        String sk1Hint = isDeskUi ? "Z/I" : "Z";
        String sk2Hint = isDeskUi ? "X/O" : "X";
        String sk3Hint = isDeskUi ? "C/Y" : "C";
        btnSkill1 = new TextButton((isP1Init ? "PHONG NHẪN (" : "HUYẾT TIÊU (") + sk1Hint + ")", initSkillStyle);
        btnSkill2 = new TextButton((isP1Init ? "LÔI PHONG (" : "PHÂN THÂN (") + sk2Hint + ")", initSkillStyle);
        btnSkill3 = new TextButton((isP1Init ? "VẠN KIẾM (" : "HUYẾT TRẬN (") + sk3Hint + ")", initSkillStyle);
        btnSkill1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { triggerSkill(0); }
        });
        btnSkill2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { triggerSkill(1); }
        });
        btnSkill3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { triggerSkill(2); }
        });

        // Cụm nút tay phải + 3 nút kỹ năng chủ động (Z/I, X/O, C/Y) ở dưới màn hình
        rightTable = new Table();
        rightTable.setPosition(0, 0);
        TextButton btnJump = new TextButton(isDeskUi ? "NHẢY (W)" : "NHẢY", jumpBtnStyle);
        TextButton btnDash = new TextButton(isDeskUi ? "LƯỚT (L)" : "LƯỚT", dashBtnStyle);
        btnJumpRef = btnJump;
        btnDashRef = btnDash;
        TextButton btnAtk = new TextButton(isDeskUi ? "ĐÁNH (J)" : "ĐÁNH", atkBtnStyle);
        TextButton btnCrouch = new TextButton(isDeskUi ? "NGỒI (S)" : "NGỒI", crouchBtnStyle);
        TextButton btnClimb = new TextButton(isDeskUi ? "LEO (R)" : "LEO", climbBtnStyle);
        btnAtkRef = btnAtk;
        btnCrouchRef = btnCrouch;
        btnClimbRef = btnClimb;
        btnAtk.setBounds(1112, 26, 144, 132);
        btnJump.setBounds(992, 26, 108, 96);
        btnDash.setBounds(992, 134, 108, 70);
        btnUltimate.setBounds(1112, 170, 144, 70);
        btnCrouch.setBounds(884, 26, 96, 62);
        btnClimb.setBounds(884, 100, 96, 62);
        btnSkill1.setBounds(484, 26, 124, 56);
        btnSkill2.setBounds(616, 26, 124, 56);
        btnSkill3.setBounds(748, 26, 124, 56);
        for (TextButton b : new TextButton[]{btnAtk, btnJump, btnDash, btnUltimate, btnCrouch, btnClimb, btnSkill1, btnSkill2, btnSkill3}) {
            rightTable.addActor(b);
        }
        uiStage.addActor(rightTable);

        // Chat văn bản & Voice Chat thời gian thực cho các chế độ trực tuyến (PVP, CO-OP, 5v5)
        if (currentGameMode != GameMode.SOLO && commsManager != null) {
            TextButton.TextButtonStyle chatBtnStyle = UiKit.buttonStyle(new Color(0.16f, 0.38f, 0.58f, 1f), UiKit.smallBold());
            TextButton.TextButtonStyle micOffStyle = UiKit.buttonStyle(UiKit.GRAY, UiKit.smallBold());

            btnChatToggle = new TextButton("CHAT (T)", chatBtnStyle);
            btnChatToggle.setBounds(20, 468, 122, 38);
            btnChatToggle.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { toggleChatPanel(); }
            });
            uiStage.addActor(btnChatToggle);

            btnMicToggle = new TextButton(isDeskUi ? "MIC: TẮT (V/M)" : "MIC: TẮT (V)", micOffStyle);
            btnMicToggle.setBounds(148, 468, isDeskUi ? 142 : 122, 38);
            btnMicToggle.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (commsManager != null) {
                        String msg = commsManager.toggleMic();
                        if (actionLog != null) actionLog.setText(msg);
                    }
                }
            });
            uiStage.addActor(btnMicToggle);

            lblVoiceStatus = new Label("", new Label.LabelStyle(UiKit.smallBold(), UiKit.SUCCESS));
            lblVoiceStatus.setPosition(20, 444);
            lblVoiceStatus.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            uiStage.addActor(lblVoiceStatus);

            chatPanel = new Table();
            chatPanel.setBounds(20, 250, 384, 212);
            chatPanel.background(UiKit.framed(new Color(0.05f, 0.06f, 0.10f, 0.94f), UiKit.GOLD_DIM));
            chatPanel.pad(8);
            chatPanel.setVisible(false);

            lblChatHistory = new Label("Chưa có tin nhắn. Bấm nhanh F2-F5 hoặc gõ bên dưới!", new Label.LabelStyle(UiKit.small(), UiKit.TEXT));
            lblChatHistory.setWrap(true);
            lblChatHistory.setAlignment(Align.topLeft);

            Table histWrap = new Table();
            histWrap.top().left().pad(4);
            histWrap.add(lblChatHistory).width(352).left().top();
            chatScroll = new ScrollPane(histWrap, UiKit.scrollStyle());
            chatScroll.setFadeScrollBars(false);
            chatPanel.add(chatScroll).width(366).height(112).colspan(2).padBottom(6).row();

            Table quickRow = new Table();
            String[] presets = new String[]{"Tập hợp!", "Yểm trợ!", "Lên công!", "Cẩn thận!"};
            for (int pi = 0; pi < presets.length; pi++) {
                final String preset = presets[pi];
                String qbLabel = isDeskUi ? ("F" + (pi + 2) + " " + preset) : preset;
                TextButton qb = new TextButton(qbLabel, UiKit.buttonStyle(new Color(0.20f, 0.28f, 0.42f, 1f), UiKit.smallBold()));
                qb.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) { sendQuickOrTypedChat(preset); }
                });
                quickRow.add(qb).size(89, 28).padRight(3);
            }
            chatPanel.add(quickRow).colspan(2).left().padBottom(6).row();

            chatInputField = new TextField("", UiKit.textFieldStyle());
            chatInputField.setMessageText("Nhập tin nhắn... (Enter gửi • Esc đóng)");
            chatInputField.setMaxLength(120);
            TextInputBridge.attach(chatInputField, "Nhập tin nhắn");
            chatInputField.setTextFieldListener((textField, c) -> {
                if (c == '\r' || c == '\n') {
                    submitChatInput();
                }
            });

            TextButton btnSendChat = new TextButton("GỬI", UiKit.buttonStyle(UiKit.GREEN, UiKit.smallBold()));
            btnSendChat.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { submitChatInput(); }
            });

            chatPanel.add(chatInputField).width(298).height(34).padRight(6);
            chatPanel.add(btnSendChat).size(62, 34);
            uiStage.addActor(chatPanel);

            commsManager.onChatUpdated = this::refreshChatHistoryUI;
        }

        btnCrouch.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (playerDead || isMatchEnded || isGrabbedByPirate || playerStunTimer > 0) return;
                if (isCrouching && !canStandUp()) {
                    actionLog.setText("Trần quá thấp! Cần bò ra khỏi đường hầm trước.");
                    return;
                }
                isCrouching = !isCrouching;
                needsHitboxUpdate = true;
            }
        });

        btnJump.addListener(new ClickListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                super.touchDown(event, x, y, pointer, button);
                event.stop();
                event.handle();
                float kx = (touchpad != null && touchpad.isVisible()) ? touchpad.getKnobPercentX() : 0f;
                triggerJump(kx < -0.25f, kx > 0.25f);
                return true;
            }
        });

        btnDash.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { triggerDash(); }
        });
        btnAtk.addListener(new ClickListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                super.touchDown(event, x, y, pointer, button);
                event.stop();
                event.handle();
                isAttackButtonPressed = true;
                triggerAttack(false);
                return true;
            }
            @Override public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                isAttackButtonPressed = false;
            }
        });

        btnClimb.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                toggleWallClimb();
            }
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (playerDead || isMatchEnded || isGrabbedByPirate || playerStunTimer > 0) return false;
                isClimbingHold = true;
                return super.touchDown(event, x, y, pointer, button);
            }
            @Override public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                isClimbingHold = false;
            }
        });

        createShopUI(font, btnStyle);
        // Đảm bảo lớp phủ & hộp thoại tạm dừng / kết thúc luôn nằm trên tiệm đồ, bảng chat và nút HUD
        if (modalOverlay != null) modalOverlay.toFront();
        if (pauseDialog != null) pauseDialog.toFront();
        if (gameOverDialog != null) gameOverDialog.toFront();
        updateScoreGoldUI(); // hiện ngay số vàng tiêu được (kể cả vàng trong kho khi chơi đơn)
    }

    private void toggleChatPanel() {
        if (chatPanel == null || isPaused || isMatchEnded || storyDialogueActive) return;
        chatPanelOpen = !chatPanelOpen;
        chatPanel.setVisible(chatPanelOpen);
        if (chatPanelOpen) {
            if (commsManager != null) commsManager.unreadCount = 0;
            refreshChatHistoryUI();
            if (uiStage != null && chatInputField != null) {
                uiStage.setKeyboardFocus(chatInputField);
            }
        } else if (uiStage != null && uiStage.getKeyboardFocus() == chatInputField) {
            uiStage.setKeyboardFocus(null);
        }
    }

    private void submitChatInput() {
        if (chatInputField == null) return;
        String txt = chatInputField.getText();
        chatInputField.setText("");
        sendQuickOrTypedChat(txt);
        closeChatOverlay();
    }

    private void sendQuickOrTypedChat(String raw) {
        if (commsManager == null || raw == null || raw.trim().isEmpty()) return;
        String sender = (game != null && game.playerName != null && !game.playerName.trim().isEmpty())
            ? game.playerName.trim() : ("P2".equals(selectedChar) ? "Vân U" : "Vân Tiêu");
        int team = (game != null) ? game.selectedTeam : 1;
        commsManager.sendChat(raw, sender, team);
        SoundManager.getInstance().playOrbPickup();
    }

    private void refreshChatHistoryUI() {
        if (commsManager == null || lblChatHistory == null) return;
        if (commsManager.history.size == 0) {
            lblChatHistory.setText("Chưa có tin nhắn. Bấm nhanh hoặc gõ bên dưới!");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, commsManager.history.size - 25);
        for (int i = start; i < commsManager.history.size; i++) {
            OnlineCommsManager.ChatEntry e = commsManager.history.get(i);
            if (sb.length() > 0) sb.append('\n');
            sb.append(e.isMe ? "[Bạn] " : "[" + e.sender + "] ").append(e.text);
        }
        lblChatHistory.setText(sb.toString());
        if (chatScroll != null) {
            chatScroll.layout();
            chatScroll.setScrollPercentY(1f);
        }
    }

    private void updateOnlineCommsUI(float delta) {
        if (commsManager == null) return;
        commsManager.update(delta);

        if (btnChatToggle != null && commsManager.unreadCount != lastUnreadBadge) {
            lastUnreadBadge = commsManager.unreadCount;
            btnChatToggle.setText(lastUnreadBadge > 0 ? "CHAT (" + lastUnreadBadge + ")" : "CHAT (T)");
        }

        if (btnMicToggle != null) {
            int micKey = (commsManager.isMicActive() ? 2 : 0) + (commsManager.isLocalSpeaking() ? 1 : 0);
            if (micKey != lastMicStateKey) {
                lastMicStateKey = micKey;
                String micHint = isDesktopPlatform() ? " (V/M)" : " (V)";
                if (commsManager.isLocalSpeaking()) {
                    btnMicToggle.setText("MIC: ĐANG NÓI");
                } else if (commsManager.isMicActive()) {
                    btnMicToggle.setText("MIC: BẬT" + micHint);
                } else {
                    btnMicToggle.setText("MIC: TẮT" + micHint);
                }
            }
        }

        if (lblVoiceStatus != null) {
            String speaker = commsManager.getRemoteSpeaker();
            if (speaker != null && !speaker.isEmpty()) {
                lblVoiceStatus.setColor(UiKit.SUCCESS);
                lblVoiceStatus.setText("[THOẠI] " + speaker + " đang nói...");
            } else if (commsManager.toastTimer > 0f && !chatPanelOpen && !commsManager.toastText.isEmpty()) {
                lblVoiceStatus.setColor(UiKit.GOLD);
                lblVoiceStatus.setText("[CHAT] " + commsManager.toastText);
            } else if (commsManager.isLocalSpeaking()) {
                lblVoiceStatus.setColor(Color.CYAN);
                lblVoiceStatus.setText("[MIC] Bạn đang phát thoại...");
            } else {
                lblVoiceStatus.setText("");
            }
        }
    }

    private void setupContactListener() {
        world.setContactListener(new ContactListener() {
            @Override public void beginContact(Contact contact) {
                Fixture fa = contact.getFixtureA(), fb = contact.getFixtureB();
                if (fa == null || fb == null || fa.getUserData() == null || fb.getUserData() == null) return;

                // FIX: only "playerFoot" triggers player ground detection
                boolean isPlayerFoot = "playerFoot".equals(fa.getUserData()) || "playerFoot".equals(fb.getUserData());
                boolean isGround = "ground".equals(fa.getUserData()) || "ground".equals(fb.getUserData());

                if (isPlayerFoot && isGround) {
                    footContacts++;
                    isGrounded = true;
                    jumpCount = 0;
                    coyoteTimer = COYOTE_TIME_MAX;
                    climbStamina = MAX_CLIMB_STAMINA;

                    Fixture groundFix = "ground".equals(fa.getUserData()) ? fa : fb;
                    if (isEagleStriking && eagleStrikePhase == 2 && groundFix.getBody().getPosition().y <= 0.85f) {
                        pendingEagleGroundAbort = true;
                    }

                    if (isFallingFromSky) {
                        isFallingFromSky = false;
                        if (!playerDead && !isMatchEnded) {
                            playerHp = Math.max(0, playerHp - FALL_PENALTY);
                            playerHpBar.setHp(playerHp);
                            lblPlayerHp.setText("Máu: " + playerHp + " HP");
                            playerHitFlashTimer = 0.25f;
                            SoundManager.getInstance().playHurt();
                            addDamageText(playerBody.getPosition().x, playerBody.getPosition().y + 0.4f,
                                FALL_PENALTY, Color.RED);
                            actionLog.setText("RƠI TỪ TRÊN CAO! MẤT " + FALL_PENALTY + " HP!");
                            cameraShakeTime = 0.5f;
                            cameraShakeIntensity = 0.35f;
                            if (playerHp <= 0) { playerDead = true; showGameOverDialog(false); }
                        }
                    }
                }
            }

            @Override public void endContact(Contact contact) {
                Fixture fa = contact.getFixtureA(), fb = contact.getFixtureB();
                if (fa == null || fb == null || fa.getUserData() == null || fb.getUserData() == null) return;

                boolean isPlayerFoot = "playerFoot".equals(fa.getUserData()) || "playerFoot".equals(fb.getUserData());
                boolean isGround = "ground".equals(fa.getUserData()) || "ground".equals(fb.getUserData());

                if (isPlayerFoot && isGround) {
                    footContacts = Math.max(0, footContacts - 1);
                    if (footContacts == 0) {
                        isGrounded = false;
                        coyoteTimer = COYOTE_TIME_MAX;
                    }
                }
            }

            @Override public void preSolve(Contact contact, Manifold oldManifold) {
                Fixture fa = contact.getFixtureA(), fb = contact.getFixtureB();
                if (fa == null || fb == null || fa.getUserData() == null || fb.getUserData() == null) return;

                boolean isPlayer = "player".equals(fa.getUserData()) || "player".equals(fb.getUserData())
                    || "playerFoot".equals(fa.getUserData()) || "playerFoot".equals(fb.getUserData());
                boolean isMonster = "monster".equals(fa.getUserData()) || "monster".equals(fb.getUserData());
                boolean isBoy = "boy".equals(fa.getUserData()) || "boy".equals(fb.getUserData());
                boolean isGround = "ground".equals(fa.getUserData()) || "ground".equals(fb.getUserData());
                boolean isWall = "wall".equals(fa.getUserData()) || "wall".equals(fb.getUserData());

                if (isPlayer && isBoy) contact.setEnabled(false);

                if (isPlayer && isMonster) {
                    if (isDashing || isEagleStriking) contact.setEnabled(false);
                }

                if (isPlayer && isEagleStriking) {
                    // Chỉ xuyên qua các bục gỗ trên cao khi đang lao xuống; luôn giữ tường biên và mặt đất chính
                    if (isGround) {
                        Fixture groundFix = "ground".equals(fa.getUserData()) ? fa : fb;
                        if (groundFix.getBody().getPosition().y > 0.80f) {
                            contact.setEnabled(false);
                        }
                    }
                    return;
                }

                if (isPlayer && isGround) {
                    if (isClimbing && playerBody.getPosition().y > 0.8f)
                        contact.setEnabled(false);
                }

                // Cho phép Tiểu Vũ leo tường/thang xuyên lên bục cao và nhảy xuống từ bục cao (giữ sàn chính y <= 0.8f luôn rắn)
                if (isBoy && isWall && boyIsClimbing) {
                    contact.setEnabled(false);
                }
                if (isBoy && isGround && boyBody != null) {
                    Fixture groundFix = "ground".equals(fa.getUserData()) ? fa : fb;
                    if (groundFix.getBody().getPosition().y > 0.80f) {
                        if (boyIsClimbing || boyDropThroughTimer > 0f || boyBody.getLinearVelocity().y > 0.25f) {
                            contact.setEnabled(false);
                        }
                    }
                }

                // Cho phép quái đang đuổi nhảy xuyên từ dưới lên các bục gỗ trên cao
                if (isMonster && isGround) {
                    Fixture groundFix = "ground".equals(fa.getUserData()) ? fa : fb;
                    Fixture mobFix = (groundFix == fa) ? fb : fa;
                    if (groundFix.getBody().getPosition().y > 0.80f && mobFix.getBody().getLinearVelocity().y > 0.25f) {
                        contact.setEnabled(false);
                    }
                }
            }

            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void updateCamera(float delta) {
        if (playerBody == null) return;

        float targetX = (playerDead && currentGameMode == GameMode.BATTLE_5V5) ? playerDeathX : playerBody.getPosition().x;
        float targetY = (playerDead && currentGameMode == GameMode.BATTLE_5V5) ? playerDeathY + 0.6f : playerBody.getPosition().y + 0.6f;

        // Cinematic Zoom & Pan during Story Dialogue / Boss Intro / Assassination
        if (cinematicAssassinateTimer > 0) {
            targetX = assassinateFocusX;
            targetY = assassinateFocusY;
            targetCameraZoom = 0.48f; // Cận cảnh điện ảnh ám sát!
        } else if (storyDialogueActive) {
            targetX = storyFocusX;
            targetY = storyFocusY;
            targetCameraZoom = 0.62f;
        } else if (bossIntroActive) {
            targetX = bossIntroTargetX;
            targetY = bossIntroTargetY;
            targetCameraZoom = 0.65f; // Zoom in dramatic close-up on Boss!
        } else {
            targetCameraZoom = 1.0f;  // Normal gameplay zoom
        }

        currentCameraZoom += (targetCameraZoom - currentCameraZoom) * Math.min(1f, 3.5f * delta);
        camera.zoom = currentCameraZoom;

        // Co-op: Camera bám theo đồng đội nếu mình chết và đã chọn "Đợi hồi sinh"
        if (!bossIntroActive && playerDead && currentGameMode == GameMode.COOP && !remoteDead) {
            targetX = remoteX;
            targetY = remoteY + 0.6f;
        }

        float halfViewW = (viewport.getWorldWidth() * camera.zoom) / 2f;
        float halfViewH = (viewport.getWorldHeight() * camera.zoom) / 2f;

        targetX = MathUtils.clamp(targetX, halfViewW, mapWidth - halfViewW);
        targetY = MathUtils.clamp(targetY, halfViewH, mapHeight - halfViewH);

        if (cameraShakeTime > 0) {
            cameraShakeTime -= delta;
            targetX += MathUtils.random(-cameraShakeIntensity, cameraShakeIntensity);
            targetY += MathUtils.random(-cameraShakeIntensity, cameraShakeIntensity);
        }

        if (isFirstCameraFrame) {
            camera.position.set(targetX, targetY, 0);
            isFirstCameraFrame = false;
        } else {
            float camLerp = Math.min(1f, (bossIntroActive ? 4.5f : 6.5f) * delta);
            camera.position.x += (targetX - camera.position.x) * camLerp;
            camera.position.y += (targetY - camera.position.y) * camLerp;
        }
        camera.update();
    }

    private void updateRopesAndSwinging(float delta) {
        if (inCutscene() || isPaused) return;
        Vector2 pos = playerBody.getPosition();
        boolean chatTyping = isChatTyping();
        float kx = (touchpad != null && touchpad.isVisible()) ? touchpad.getKnobPercentX() : 0f;
        boolean inputL = moveLeft || kx < -0.25f || (!chatTyping && (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)));
        boolean inputR = moveRight || kx > 0.25f || (!chatTyping && (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)));

        // 1. Nếu đang đu dây
        if (isSwinging && currentSwingingRope != null) {
            // Cập nhật chuyển động con lắc dây có tính quán tính, trọng lực và lực lấy đà
            currentSwingingRope.update(delta, inputL, inputR);

            // Bám vị trí người chơi theo đầu mút dây đu
            Vector2 tip = currentSwingingRope.getTipPosition();
            playerBody.setTransform(tip.x, tip.y + 0.35f, 0);

            Vector2 tVel = currentSwingingRope.getTangentialVelocity();
            playerBody.setLinearVelocity(tVel.x, tVel.y);
            if (Math.abs(tVel.x) > 0.3f) {
                playerFacing = (tVel.x > 0) ? 1 : -1;
            }

            // Nếu người chơi chạm mặt sàn an toàn khi dây gần thẳng đứng thì tự buông dây
            if (isGrounded && Math.abs(currentSwingingRope.angle) < 0.20f) {
                isSwinging = false;
                currentSwingingRope.release();
                currentSwingingRope = null;
                swingCooldown = 0.5f;
            }
        } else {
            // 2. Kiểm tra xem người chơi nhảy vào bám dây đu không
            if (swingCooldown <= 0 && !playerDead && !isMatchEnded && playerStunTimer <= 0 && !isGrounded
                && !isEagleStriking && !isP1UltActive && !isP2DashingAssassinate && !isAssassinating && !isGrabbedByPirate) {
                for (int i = 0; i < swingRopes.size; i++) {
                    SwingRope r = swingRopes.get(i);
                    Vector2 tip = r.getTipPosition();
                    if (pos.dst(tip) < 1.45f) {
                        isSwinging = true;
                        currentSwingingRope = r;
                        r.grab(playerBody.getLinearVelocity());
                        isClimbing = false;
                        isDashing = false;
                        dashTime = 0f;
                        isCrouching = false;
                        jumpCount = 0;
                        playerBody.setLinearVelocity(0, 0);
                        SoundManager.getInstance().playWallJump();
                        actionLog.setText("ĐANG ĐU DÂY! Trái/phải để lấy đà, NHẢY để phóng người!");
                        break;
                    }
                }
            }
        }

        // 3. Cập nhật các dây còn lại (đung đưa tự do với quán tính và trọng lực suy giảm tự nhiên)
        for (int i = 0; i < swingRopes.size; i++) {
            SwingRope r = swingRopes.get(i);
            if (r != currentSwingingRope) {
                r.update(delta, false, false);

                // Quán tính chạm lướt: khi nhân vật chạy qua quẹt trúng dây làm dây lắc nhẹ
                Vector2 tip = r.getTipPosition();
                if (pos.dst(tip) < 0.8f && Math.abs(playerBody.getLinearVelocity().x) > 1.5f && Math.abs(r.angularVelocity) < 0.5f) {
                    r.applyImpulse(playerFacing * 1.5f);
                }
            }
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        totalMatchTime += delta;
        if (storyDialogueActive || shopFreezesWorld()) frozenTime += delta;
        if (playerPoisonSlowTimer > 0f) playerPoisonSlowTimer = Math.max(0f, playerPoisonSlowTimer - delta);
        if (reviveCooldownTimer > 0f) reviveCooldownTimer = Math.max(0f, reviveCooldownTimer - delta);

        // Hội thoại cốt truyện: mở đầu ải ở khung hình đầu tiên; xử lý phím trước mọi điều khiển khác
        if (story != null && !storyIntroStarted && playerBody != null) startStoryIntro();
        handleStoryDialogueInput();

        if (hitStopTimer > 0) {
            hitStopTimer -= delta;
            delta = 0.0001f;
        } else if (cinematicAssassinateTimer > 0) {
            cinematicAssassinateTimer -= delta;
            delta *= 0.22f; // Tua chậm (Slow motion ~4.5x) tạo khoảnh khắc kết liễu điện ảnh
        }

        if (playerHitFlashTimer > 0) {
            playerHitFlashTimer -= delta;
        }
        if (remoteHitFlashTimer > 0f) {
            remoteHitFlashTimer -= delta;
        }
        remoteAttackAnimTime += delta;

        // FIX: interpolate remote player position (was snapping / teleporting)
        float netLerp = Math.min(1f, 12f * delta);
        if (targetRemoteX != -999f) {
            remoteX += (targetRemoteX - remoteX) * netLerp;
            remoteY += (targetRemoteY - remoteY) * netLerp;
        }
        for (int epi = 0; epi < extraPeers5v5.size; epi++) {
            RemotePeer5v5 ep = extraPeers5v5.get(epi);
            ep.attackAnimTime += delta;
            if (ep.hitFlashTimer > 0f) ep.hitFlashTimer -= delta;
            if (ep.targetX != -999f) {
                ep.x += (ep.targetX - ep.x) * netLerp;
                ep.y += (ep.targetY - ep.y) * netLerp;
            }
        }

        // Trigger Boss Intro Cinematic (Solo / COOP)
        if (!bossIntroTriggered && !isMatchEnded && currentGameMode != GameMode.PVP && currentGameMode != GameMode.BATTLE_5V5 && playerBody != null) {
            GroundMob bossMob = null;
            for (int i = 0; i < activeMonsters.size; i++) {
                GroundMob m = activeMonsters.get(i);
                if (m.isBoss && !m.isDead && m.body != null) {
                    bossMob = m;
                    break;
                }
            }
            if (bossMob != null) {
                float distToBoss = Math.abs(playerBody.getPosition().x - bossMob.body.getPosition().x);
                // Sửa lỗi nghiêm trọng: CHỈ kích hoạt khi người chơi ĐÃ TIẾP ĐẤT AN TOÀN TRÊN SÀN ĐẤU (isGrounded)
                // và ĐÃ VÀO TRONG SÀN ĐẤU BOSS (playerX >= mapWidth - 37.0f) HOẶC ĐÃ TIẾP CẬN GẦN BOSS (distToBoss <= 12.0f)
                // Tuyệt đối không kích hoạt khi đang nhảy qua vực để tránh làm mất đà rơi xuống vực!
                if (isGrounded && !playerDead && playerBody.getPosition().y >= 0.60f && (playerBody.getPosition().x >= mapWidth - 37.0f || distToBoss <= 12.0f)) {
                    bossIntroTriggered = true;
                    bossIntroActive = true;
                    bossIntroTimer = 2.0f;
                    bossIntroTargetX = bossMob.body.getPosition().x;
                    bossIntroTargetY = bossMob.body.getPosition().y + 0.6f;
                    cameraShakeTime = 0.6f;
                    cameraShakeIntensity = 0.30f;
                    SoundManager.getInstance().playBossSmash();
                    actionLog.setText(story != null ? "!! " + story.bossName + " XUẤT HIỆN !!" : "!! CẢNH BÁO: THỦ LĨNH XUẤT HIỆN !!");
                    addCustomText(bossIntroTargetX, bossIntroTargetY + 1.2f, "THỦ LĨNH!", Color.RED);
                    playerBody.setLinearVelocity(0, 0);
                }
            }
        }

        if (bossIntroActive) {
            bossIntroTimer -= delta;
            moveLeft = false;
            moveRight = false;
            isDashing = false;
            if (playerBody != null) {
                // Giữ nhân vật an toàn đứng vững trên sàn đấu, không bị trôi hay rơi vực
                playerBody.setLinearVelocity(0, 0);
                if (playerBody.getPosition().y < 0.65f) {
                    playerBody.setTransform(Math.max(playerBody.getPosition().x, mapWidth - 36.0f), 1.2f, 0);
                    playerBody.setLinearVelocity(0, 0);
                }
            }
            if (bossIntroTimer <= 0) {
                bossIntroActive = false;
                if (story != null) {
                    actionLog.setText("Quyết chiến: " + story.bossName + "!");
                    startStoryDialogue(story.bossDialogues, null);
                } else {
                    actionLog.setText("TRẬN CHIẾN BẮT ĐẦU! HẠ THỦ LĨNH!");
                }
            }
        }

        // CO-OP: cả đội đều gục (hoặc đồng đội đã thoát) -> không ai hồi sinh được, kết thúc trận
        // (bản cũ để người chơi kẹt mãi ở màn "Đợi hồi sinh").
        if (currentGameMode == GameMode.COOP && playerDead && !coopWipeShown && (remoteHp <= 0 || partnerLeft)) {
            showCoopWipe();
        }

        checkStoryFailure();
        if (btnTalkNPC != null) {
            boolean near = story != null && boyBody != null && boyState == BoyState.RESCUED && playerBody != null
                && Math.abs(boyBody.getPosition().x - playerBody.getPosition().x) <= 2.5f;
            btnTalkNPC.setVisible(near && !storyDialogueActive && !isMatchEnded && !playerDead && !isPaused);
        }

        // Finish line check (COOP/Solo)
        if (!isMatchEnded && currentGameMode != GameMode.PVP && currentGameMode != GameMode.BATTLE_5V5 && playerBody.getPosition().x >= finishLineX) {
            boolean hasBoss = false;
            for (int mi = 0; mi < activeMonsters.size; mi++) { if (activeMonsters.get(mi).isBoss) { hasBoss = true; break; } }
            if (hasBoss) {
                actionLog.setText("PHẢI HẠ THỦ LĨNH TRƯỚC KHI QUA ẢI!");
                playerBody.setLinearVelocity(-3.5f, playerBody.getLinearVelocity().y);
            } else {
                showFinishVictoryDialog();
            }
        }

        // 5v5 Kill Streak & Multi-kill decay
        if (killStreakTimer > 0) {
            killStreakTimer -= delta;
            if (killStreakTimer <= 0 && lblKillStreak != null) lblKillStreak.setText("");
        }
        if (multiKillWindow > 0) {
            multiKillWindow -= delta;
            if (multiKillWindow <= 0) playerMultiKill = 0;
        }

        // Jump Pads, Power-up Runes & Tactical Bushes (hoạt động trên cả 5v5 và CO-OP)
        if (!inCutscene() && !isPaused) {
            if (!powerUpRunes.isEmpty()) {
                for (int i = 0; i < powerUpRunes.size; i++) {
                    PowerUpRune rune = powerUpRunes.get(i);
                    rune.pulseTime += delta;
                    if (!rune.active) {
                        rune.respawnTimer -= delta;
                        if (rune.respawnTimer <= 0) {
                            rune.active = true;
                            rune.respawnTimer = 0f;
                        }
                    }
                }
            }

            if (!jumpPads.isEmpty()) {
                for (int i = 0; i < jumpPads.size; i++) {
                    JumpPad pad = jumpPads.get(i);
                    pad.animTime += delta;
                    if (pad.cooldown > 0) pad.cooldown -= delta;
                }
            }

            if (playerBody != null && !playerDead && !isMatchEnded) {
                Vector2 pPos = playerBody.getPosition();
                for (int i = 0; i < jumpPads.size; i++) {
                    JumpPad pad = jumpPads.get(i);
                    if (pad.cooldown <= 0 && Math.abs(pPos.x - pad.x) < (pad.width / 2f + 0.3f)
                        && Math.abs(pPos.y - (pad.y + pad.height / 2f)) < 0.6f) {
                        playerBody.setLinearVelocity(playerFacing * 4.2f, pad.bounceForce);
                        pad.cooldown = 0.8f;
                        SoundManager.getInstance().playDash();
                        cameraShakeTime = 0.20f; cameraShakeIntensity = 0.20f;
                        spawnHitSparks(pad.x, pad.y + 0.2f, 20, Color.CYAN);
                        addCustomText(pad.x, pad.y + 0.6f, "BẬT NHẢY TƯỜNG!", Color.CYAN);
                        break;
                    }
                }

                for (int i = 0; i < powerUpRunes.size; i++) {
                    PowerUpRune rune = powerUpRunes.get(i);
                    if (rune.active && pPos.dst(rune.x, rune.y) <= 1.0f) {
                        rune.active = false;
                        rune.respawnTimer = rune.respawnInterval;
                        SoundManager.getInstance().playOrbPickup();
                        spawnHitSparks(rune.x, rune.y, 30, Color.GOLD);

                        switch (rune.type) {
                            case RANDOM_ULTIMATE:
                                playerCurrentUltType = MathUtils.randomBoolean() ? "P1" : "P2";
                                ultimateKills = ULTIMATE_REQ_KILLS;
                                cameraShakeTime = 0.35f; cameraShakeIntensity = 0.30f;
                                String ultName = playerCurrentUltType.equals("P1") ? "TRẤN TRỜI" : "SÁT CHIÊU HOÀN HẢO";
                                addCustomText(rune.x, rune.y + 0.8f, "TUYỆT KỸ: " + ultName + "!", Color.GOLD);
                                actionLog.setText("NHẬN ĐƯỢC TUYỆT KỸ: " + ultName + "! Nhấn (U) để ra chiêu!");
                                if (btnUltimate != null) {
                                    btnUltimate.setText(playerCurrentUltType.equals("P1") ? "TRẤN TRỜI (U)" : "TÀNG HÌNH (U)");
                                }
                                break;
                            case BERSERK_DAMAGE:
                                playerDamageBuff = 1.50f;
                                playerActiveRune = rune.type;
                                playerRuneTimer = 15.0f;
                                addCustomText(rune.x, rune.y + 0.8f, "+50% SÁT THƯƠNG CUỒNG NỘ!", Color.CORAL);
                                actionLog.setText("BÙA CUỒNG NỘ! +50% SÁT THƯƠNG TRONG 15 GIÂY!");
                                break;
                            case DIVINE_SHIELD:
                                playerShield = Math.min(600, playerShield + 350);
                                playerActiveRune = rune.type;
                                playerRuneTimer = 18.0f;
                                addCustomText(rune.x, rune.y + 0.8f, "+350 GIÁP HỘ THỂ!", Color.CYAN);
                                actionLog.setText("KHIÊN HỘ MỆNH! +350 KHIÊN!");
                                break;
                            case WIND_STRIDE:
                                playerSpeedBuff = 1.60f;
                                playerActiveRune = rune.type;
                                playerRuneTimer = 15.0f;
                                addCustomText(rune.x, rune.y + 0.8f, "+60% PHONG TỐC & NHẢY CAO!", Color.SKY);
                                actionLog.setText("BÙA PHONG TỐC! +60% TỐC ĐỘ DI CHUYỂN!");
                                break;
                            case MEGA_HEAL:
                                playerHp = Math.min(playerMaxHp, playerHp + 400);
                                playerHpBar.setHp(playerHp);
                                lblPlayerHp.setText("Máu: " + playerHp + " HP");
                                addCustomText(rune.x, rune.y + 0.8f, "+400 HP THẦN DƯỢC!", Color.GREEN);
                                actionLog.setText("HỒI PHỤC 400 HP TỨC THÌ!");
                                break;
                        }
                    }
                }

                // Ẩn nấp bụi rậm chiến thuật
                playerInBush = false;
                for (int i = 0; i < tacticalBushes.size; i++) {
                    TacticalBush bush = tacticalBushes.get(i);
                    if (bush.contains(pPos)) {
                        playerInBush = true;
                        if (!playerAmbushBuff) {
                            playerAmbushBuff = true;
                            addCustomText(pPos.x, pPos.y + 0.8f, "ẨN NẤP: PHỤC KÍCH!", Color.GREEN);
                        }
                        break;
                    }
                }
            }
        }

        // Local player rune timer
        if (playerRuneTimer > 0) {
            playerRuneTimer -= delta;
            if (playerRuneTimer <= 0) {
                playerDamageBuff = 1.0f;
                playerSpeedBuff = 1.0f;
                playerActiveRune = null;
            }
        }

        // 5v5 ARAM Battle Updates
        if (currentGameMode == GameMode.BATTLE_5V5) {
            // Local player Dragon Slayer buff timer
            if (playerTitanBuffTimer > 0) {
                playerTitanBuffTimer -= delta;
                if (playerTitanBuffTimer <= 0) {
                    playerHasTitanBuff = false;
                }
            }

            // Local player Fountain checks
            if (playerBody != null && !playerDead) {
                Vector2 pPos = playerBody.getPosition();
                int pTeam = (game != null) ? game.selectedTeam : 1;

                // Ho mau nha hoi phuc
                boolean inHomeFountain = (pTeam == 1 && pPos.x <= 5.8f) || (pTeam == 2 && pPos.x >= 54.2f);
                // Cộng dồn phần lẻ: (int)(55 * delta) = 0 ở 60 FPS nên trước đây hồ máu không hồi gì
                if (inHomeFountain && playerHp < playerMaxHp) {
                    playerFountainAccum += 55f * delta;
                    int heal = (int) playerFountainAccum;
                    if (heal > 0) {
                        playerFountainAccum -= heal;
                        playerHp = Math.min(playerMaxHp, playerHp + heal);
                        playerHpBar.setHp(playerHp);
                        lblPlayerHp.setText("Máu: " + playerHp + " HP");
                    }
                }

                // Sát thương hồ máu kẻ địch
                // Cùng vùng với hồ máu (5,8 m tính từ mép): bản trước chỉ 3,5 m nên kẻ địch đứng ở dải 54,2–56,5 m
                // vẫn chém được người đang hồi máu mà không bị tia phòng ngự bắn.
                boolean inEnemyFountain = (pTeam == 1 && pPos.x >= 54.2f) || (pTeam == 2 && pPos.x <= 5.8f);
                if (inEnemyFountain) {
                    playerZapAccum += 120f * delta;
                    int zapDmg = (int) playerZapAccum;
                    if (zapDmg > 0) {
                        playerZapAccum -= zapDmg;
                        playerHp = Math.max(0, playerHp - zapDmg);
                        playerHpBar.setHp(playerHp);
                        lblPlayerHp.setText("Máu: " + playerHp + " HP");
                        spawnHitSparks(pPos.x, pPos.y + 0.3f, 4, Color.MAGENTA);
                        if (playerHp <= 0) {
                            handleLocalPlayerDeath5v5("Tia Phòng Ngự Hồ Máu", (pTeam == 1 ? 2 : 1));
                        }
                    }
                }
            }

            // Local player respawn timer
            if (playerDead) {
                playerRespawnTimer5v5 -= delta;
                int secs = Math.max(1, (int)Math.ceil(playerRespawnTimer5v5));
                actionLog.setText("BẠN ĐÃ BỊ HẠ GỤC! HỒI SINH SAU " + secs + "s...");
                if (playerRespawnTimer5v5 <= 0) {
                    playerDead = false;
                    playerDeathBlinkTimer = 0f;
                    playerHp = playerMaxHp;
                    playerShield = 250;
                    playerHpBar.setHp(playerHp);
                    lblPlayerHp.setText("Máu: " + playerHp + " HP");
                    int pTeam = (game != null) ? game.selectedTeam : 1;
                    resetTraversalState();
                    if (pTeam == 1) {
                        playerBody.setTransform(4.5f + MathUtils.random(-1.0f, 1.0f), 1.2f, 0);
                        playerFacing = 1;
                    } else {
                        playerBody.setTransform(55.5f + MathUtils.random(-1.0f, 1.0f), 1.2f, 0);
                        playerFacing = -1;
                    }
                    playerBody.setLinearVelocity(0, 0);
                    actionLog.setText("ĐÃ HỒI SINH TẠI HỒ MÁU!");
                    SoundManager.getInstance().playOrbPickup();
                    sendSyncPacket("IDLE");
                }
            }

            // Cap nhat Than Thu Bao Chua (Ancient Titan Boss)
            if (ancientTitan != null) {
                ancientTitan.update(delta);
                Vector2 pPos = playerBody.getPosition();
                int pTeam = (game != null) ? game.selectedTeam : 1;
                if (ancientTitan.canAttack()) {
                    Vector2 tPos = ancientTitan.getPosition();
                    boolean hasVictim = false;
                    int titanFacing = 1;
                    if (!playerDead && pPos.dst(tPos) < 3.2f) {
                        hasVictim = true;
                        titanFacing = (pPos.x >= tPos.x) ? 1 : -1;
                    }
                    if (!hasVictim) {
                        for (int i = 0; i < fighters5v5.size; i++) {
                            Fighter5v5 f = fighters5v5.get(i);
                            if (!f.isDead && f.body != null && f.body.getPosition().dst(tPos) < 3.2f) {
                                hasVictim = true;
                                titanFacing = (f.body.getPosition().x >= tPos.x) ? 1 : -1;
                                break;
                            }
                        }
                    }

                    if (hasVictim && ancientTitan.triggerAttack(titanFacing)) {
                        SoundManager.getInstance().playBossSmash();
                        cameraShakeTime = 0.28f; cameraShakeIntensity = 0.22f;
                        spawnHitSparks(tPos.x, tPos.y - 0.2f, 25, Color.GOLD);

                        // AOE Slam Damage
                        if (!playerDead && pPos.dst(tPos) < 3.2f) {
                            int tDmg = absorbShield(50);
                            playerHp = Math.max(0, playerHp - tDmg);
                            playerHpBar.setHp(playerHp);
                            lblPlayerHp.setText("Máu: " + playerHp + " HP");
                            spawnHitSparks(pPos.x, pPos.y + 0.2f, 15, Color.RED);
                            addDamageText(pPos.x, pPos.y + 0.5f, tDmg, Color.RED);
                            if (playerHp <= 0) {
                                playerDead = true;
                                closeShop();
                                resetTraversalState();
                                if (playerBody != null) {
                                    playerDeathX = playerBody.getPosition().x;
                                    playerDeathY = playerBody.getPosition().y;
                                    playerDeathBlinkTimer = 0.85f;
                                }
                                playerRespawnTimer5v5 = 5.0f;
                                String titanVictim = (game != null && game.playerName != null ? game.playerName : "Chiến Thần");
                                on5v5Kill("Thần Thú Bạo Chúa", 0, titanVictim, pTeam);
                                broadcast5v5Kill("Thần Thú Bạo Chúa", 0, titanVictim, pTeam);
                                sendSyncPacket("IDLE");
                            }
                        }
                        for (int i = 0; i < fighters5v5.size; i++) {
                            Fighter5v5 f = fighters5v5.get(i);
                            if (!f.isDead && f.body != null && f.body.getPosition().dst(tPos) < 3.2f) {
                                boolean killed = f.takeDamage(50);
                                spawnHitSparks(f.body.getPosition().x, f.body.getPosition().y + 0.2f, 12, Color.RED);
                                addDamageText(f.body.getPosition().x, f.body.getPosition().y + 0.5f, 50, Color.RED);
                                if (killed) on5v5Kill("Thần Thú Bạo Chúa", 0, f.name, f.team);
                            }
                        }
                    }
                }
            }

            // Cap nhat Thap Tien Don (Outpost Turrets)
            for (int ti = 0; ti < outpostTurrets.size; ti++) {
                OutpostTurret turret = outpostTurrets.get(ti);
                turret.update(delta);
                if (turret.canShoot()) {
                    Vector2 tPos = turret.getPosition();
                    Vector2 pPos = playerBody.getPosition();
                    int pTeam = (game != null) ? game.selectedTeam : 1;

                    // 1. Uu tien ban bot khac phe dang tien vao tam ban (MOBA standard)
                    Fighter5v5 closestEnemyBot = null;
                    float closestDst = 6.5f;
                    for (int fi = 0; fi < fighters5v5.size; fi++) {
                        Fighter5v5 f = fighters5v5.get(fi);
                        if (!f.isDead && f.team != turret.team && f.body != null) {
                            float d = f.body.getPosition().dst(tPos);
                            if (d < closestDst) {
                                closestDst = d;
                                closestEnemyBot = f;
                            }
                        }
                    }

                    if (closestEnemyBot != null) {
                        turret.resetShootCooldown();
                        Color shotColor = turret.team == 1 ? Color.CYAN : Color.CORAL;
                        spawnHitSparks(turret.x, turret.y + 1.2f, 8, shotColor);
                        Vector2 ebPos = closestEnemyBot.body.getPosition();
                        for (float step = 0.25f; step <= 0.75f; step += 0.25f) {
                            spawnHitSparks(MathUtils.lerp(turret.x, ebPos.x, step), MathUtils.lerp(turret.y + 1.2f, ebPos.y + 0.3f, step), 2, shotColor);
                        }
                        boolean killed = closestEnemyBot.takeDamage(45);
                        spawnHitSparks(ebPos.x, ebPos.y + 0.2f, 12, Color.RED);
                        addDamageText(ebPos.x, ebPos.y + 0.5f, 45, Color.RED);
                        if (ebPos.dst(pPos) < 15.0f) {
                            SoundManager.getInstance().playHit();
                        }
                        if (killed) on5v5Kill("Tháp Tiền Đồn", turret.team, closestEnemyBot.name, closestEnemyBot.team);
                        continue;
                    }

                    // 2. Neu khong co bot doi thu nao trong tam, ban nguoi choi neu khac phe
                    if (!playerDead && pTeam != turret.team && pPos.dst(tPos) < 6.5f) {
                        turret.resetShootCooldown();
                        Color shotColor = turret.team == 1 ? Color.CYAN : Color.CORAL;
                        spawnHitSparks(turret.x, turret.y + 1.2f, 10, shotColor);
                        for (float step = 0.25f; step <= 0.75f; step += 0.25f) {
                            spawnHitSparks(MathUtils.lerp(turret.x, pPos.x, step), MathUtils.lerp(turret.y + 1.2f, pPos.y + 0.3f, step), 3, shotColor);
                        }
                        int tDmg = absorbShield(45);
                        playerHp = Math.max(0, playerHp - tDmg);
                        playerHpBar.setHp(playerHp);
                        lblPlayerHp.setText("Máu: " + playerHp + " HP");
                        spawnHitSparks(pPos.x, pPos.y + 0.2f, 15, Color.RED);
                        addDamageText(pPos.x, pPos.y + 0.5f, tDmg, Color.RED);
                        SoundManager.getInstance().playHeavyHit();
                        cameraShakeTime = 0.18f; cameraShakeIntensity = 0.14f;
                        if (playerHp <= 0) {
                            handleLocalPlayerDeath5v5("Tháp Tiền Đồn", turret.team);
                        }
                        continue;
                    }
                }
            }

            // (Điểm 5v5 chỉ cập nhật trong on5v5Kill - không setText mỗi frame gây relayout HUD)

            // Update 5v5 Fighters
            Vector2 pPos = playerBody.getPosition();
            int pTeam = (game != null) ? game.selectedTeam : 1;
            for (int fi = 0; fi < fighters5v5.size; fi++) {
                Fighter5v5 f = fighters5v5.get(fi);
                f.update(delta, fighters5v5, pPos, pTeam, playerDead, powerUpRunes, jumpPads, tacticalBushes, ancientTitan, outpostTurrets);
                if (f.lastPickedRune != null) {
                    spawnHitSparks(f.lastPickedRune.x, f.lastPickedRune.y, 18, f.lastPickedRune.type.getColor());
                }

                // Combat attack execution
                if (f.isAttacking && !f.hitThisAttack && f.attackTimer < 0.20f && f.body != null) {
                    f.hitThisAttack = true;
                    Vector2 fPos = f.body.getPosition();
                    float titanMult = f.hasTitanBuff ? 1.35f : 1.0f;
                    float ambushMult = f.hasAmbushBuff ? 1.40f : 1.0f;
                    f.hasAmbushBuff = false;
                    int baseDmg = MathUtils.random(25, 40);
                    if (f.attackCombo == 3) {
                        baseDmg = (int)(baseDmg * 1.5f);
                    }
                    int fDmg = (int)(baseDmg * f.damageMultiplier * titanMult * ambushMult);

                    // 1. Hit local player
                    if (f.team != pTeam && !playerDead) {
                        float dx = (pPos.x - fPos.x) * f.facing;
                        if (dx >= -0.2f && dx <= 1.6f && Math.abs(pPos.y - fPos.y) <= 1.2f) {
                            int actualFDmg = absorbShield(fDmg);
                            playerHp = Math.max(0, playerHp - actualFDmg);
                            playerHpBar.setHp(playerHp);
                            lblPlayerHp.setText("Máu: " + playerHp + " HP");
                            spawnHitSparks(pPos.x, pPos.y + 0.2f, 15, Color.RED);
                            addDamageText(pPos.x, pPos.y + 0.5f, actualFDmg, Color.RED);
                            cameraShakeTime = (f.attackCombo == 3) ? 0.24f : 0.15f;
                            cameraShakeIntensity = (f.attackCombo == 3) ? 0.20f : 0.12f;
                            SoundManager.getInstance().playHurt();

                            if (playerHp <= 0) {
                                handleLocalPlayerDeath5v5(f.name, f.team);
                            }
                        }
                    }

                    // 2. Hit other bots
                    for (int ti = 0; ti < fighters5v5.size; ti++) {
                        Fighter5v5 target = fighters5v5.get(ti);
                        if (target != f && target.team != f.team && !target.isDead && target.body != null) {
                            Vector2 tPos = target.body.getPosition();
                            float dx = (tPos.x - fPos.x) * f.facing;
                            if (dx >= -0.2f && dx <= 1.6f && Math.abs(tPos.y - fPos.y) <= 1.2f) {
                                boolean killed = target.takeDamage(fDmg);
                                spawnHitSparks(tPos.x, tPos.y + 0.2f, 12, (f.team == 1 ? Color.CYAN : Color.CORAL));
                                addDamageText(tPos.x, tPos.y + 0.5f, fDmg, Color.WHITE);
                                if (fPos.dst(pPos) < 14.0f) {
                                    SoundManager.getInstance().playSlash();
                                }
                                if (killed) {
                                    f.kills++;
                                    on5v5Kill(f.name, f.team, target.name, target.team);
                                }
                            }
                        }
                    }

                    // 3. Hit Ancient Titan
                    if (ancientTitan != null && ancientTitan.isAlive) {
                        Vector2 tPos = ancientTitan.getPosition();
                        float dx = (tPos.x - fPos.x) * f.facing;
                        if (dx >= -0.2f && dx <= 1.8f && Math.abs(tPos.y - fPos.y) <= 1.6f) {
                            boolean killed = ancientTitan.takeDamage(fDmg);
                            spawnHitSparks(tPos.x, tPos.y + 0.5f, 14, Color.GOLD);
                            addDamageText(tPos.x, tPos.y + 0.8f, fDmg, Color.GOLD);
                            if (killed) onTitanSlain(f.team);
                        }
                    }

                    // 4. Hit enemy Outpost Turret
                    for (int turI = 0; turI < outpostTurrets.size; turI++) {
                        OutpostTurret turret = outpostTurrets.get(turI);
                        if (turret.team != f.team && !turret.isDestroyed) {
                            float dx = (turret.x - fPos.x) * f.facing;
                            if (dx >= -0.2f && dx <= 1.8f && Math.abs(turret.y - fPos.y) <= 1.6f) {
                                boolean destroyed = turret.takeDamage(fDmg);
                                spawnHitSparks(turret.x, turret.y + 0.8f, 14, Color.YELLOW);
                                addDamageText(turret.x, turret.y + 1.2f, fDmg, Color.YELLOW);
                                if (destroyed) onTurretDestroyed(turret, f.team);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (playerStunTimer > 0) {
            playerStunTimer -= delta;
            moveLeft = false; moveRight = false;
            int stunTenths = (int) Math.ceil(playerStunTimer * 10f);
            if (stunTenths != lastStunTenths) {
                lastStunTenths = stunTenths;
                actionLog.setText("BẠN ĐANG BỊ CHOÁNG! (" + (stunTenths / 10) + "." + (stunTenths % 10) + "s)");
            }
        }

        updateOnlineCommsUI(delta);
        boolean chatTyping = isChatTyping();
        boolean modalConsumed = UiKit.wasModalKeyConsumedThisFrame();
        boolean escHandled = false;

        // Cho phép bấm Esc đóng khung Chat ngay cả khi đang gõ tin nhắn (không bật nhầm Menu tạm dừng)
        if (!modalConsumed && chatPanelOpen && isBackJustPressed()) {
            closeChatOverlay();
            chatTyping = false;
            escHandled = true;
        }

        // Phím F1: Ẩn / hiện cụm nút cảm ứng ảo cho người chơi bằng bàn phím Laptop (giữ lại thanh kỹ năng)
        if (!chatTyping && !modalConsumed && Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            toggleVirtualTouchHud();
        }

        if (isOnlineMode() && commsManager != null) {
            boolean holdV = !chatTyping && !isPaused && !isSettingsModalOpen && !isMatchEnded && Gdx.input.isKeyPressed(Input.Keys.V);
            commsManager.setPushToTalk(holdV);

            // Phím M: Bật / tắt mic rảnh tay khi chơi trực tuyến
            if (!chatTyping && !modalConsumed && !isPaused && !isSettingsModalOpen && Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                String msg = commsManager.toggleMic();
                if (actionLog != null) actionLog.setText(msg);
            }

            // Phím F2..F5: Gửi nhanh tín hiệu chiến thuật không cần mở bảng chat
            if (!chatTyping && !modalConsumed && !isPaused && !isSettingsModalOpen && !storyDialogueActive) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) sendQuickOrTypedChat("Tập hợp!");
                else if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) sendQuickOrTypedChat("Yểm trợ!");
                else if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) sendQuickOrTypedChat("Lên công!");
                else if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) sendQuickOrTypedChat("Cẩn thận!");
            }
        }
        if (!chatTyping && !modalConsumed && isOnlineMode() && !isPaused && !isSettingsModalOpen && !storyDialogueActive && Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            toggleChatPanel();
        }

        // Điều khiển bằng bàn phím cho Hộp thoại Kết thúc trận (Game Over / Qua ải)
        if (!modalConsumed && !storyInputUsedThisFrame() && gameOverDialog != null && gameOverDialog.isVisible()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                restartOrReturnLobby();
                return;
            }
            if (currentGameMode == GameMode.COOP && playerDead && btnWaitRevive != null && btnWaitRevive.isVisible()
                && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                setGameOverDialogVisible(false);
                actionLog.setText("ĐANG CHỜ ĐỒNG ĐỘI CỨU... (ĐANG XEM GÓC NHÌN ĐỒNG ĐỘI)");
            } else if (!escHandled && (isBackJustPressed() || Gdx.input.isKeyJustPressed(Input.Keys.Q))) {
                exitToMenu();
                return;
            }
        }

        // Điều khiển bằng bàn phím cho Menu Tạm dừng (W/S/↑/↓ chọn dòng, Enter/Space xác nhận, 1/2/3/Q phím tắt, Esc tiếp tục)
        if (!modalConsumed && isPaused && !isSettingsModalOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                pauseMenuIndex = (pauseMenuIndex + 2) % 3;
                updatePauseSelectionVisual();
                SoundManager.getInstance().playOrbPickup();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                pauseMenuIndex = (pauseMenuIndex + 1) % 3;
                updatePauseSelectionVisual();
                SoundManager.getInstance().playOrbPickup();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                activatePauseSelection(pauseMenuIndex);
                if (game.getScreen() != this || isDisposed) return;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.P)
                || (!escHandled && isBackJustPressed())
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
                escHandled = true;
                togglePause();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
                activatePauseSelection(1);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)
                || Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                activatePauseSelection(2);
                if (game.getScreen() != this || isDisposed) return;
            }
        }

        // Điều khiển bằng bàn phím cho Tiệm Đồ (B): phím 1/2/3 mua nhanh, Esc đóng tiệm mà không bật Pause
        if (!chatTyping && !modalConsumed && isShopOpen && !isPaused && !isSettingsModalOpen && !isMatchEnded) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
                buySwordUpgrade();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
                buyArmorUpgrade();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
                buyHealthPotion();
            } else if (!escHandled && isBackJustPressed()) {
                toggleShop();
                escHandled = true;
            }
        }

        // Native Keyboard & Mouse Input (hỗ trợ đầy đủ 2 kiểu cầm phím Laptop: WASD+JKL/UIOY và Mũi tên+ZXC/Ctrl/Shift)
        boolean mouseAimUp = false;
        if (!chatTyping && !modalConsumed && !isMatchEnded && !isPaused && !isShopOpen
            && playerStunTimer <= 0 && !inCutscene() && !storyInputUsedThisFrame()) {
            boolean isDesktop = isDesktopPlatform();
            boolean kl = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
            boolean kr = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

            // Jump: SPACE / W / UP
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                triggerJump(kl, kr);
            }

            // Bám tường / Leo trèo chủ động bằng phím R hoặc ;
            if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.SEMICOLON)) {
                toggleWallClimb();
            }

            // Left Mouse Click -> Attack (only on Desktop and if not clicking UI Stage Actor)
            boolean leftClick = false;
            if (isDesktop && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                Vector2 stageCoords = uiStage.screenToStageCoordinates(tmpVec.set(Gdx.input.getX(), Gdx.input.getY()));
                Actor hitActor = uiStage.hit(stageCoords.x, stageCoords.y, true);
                if (hitActor == null || hitActor == uiStage.getRoot()) {
                    leftClick = true;
                }
            }
            // Attack: J / Ctrl / Left Mouse Click / Held Attack
            if (isAttackKeyJustPressed() || leftClick || (isAttackKeyHeld(false) && weaponAttackCooldown <= 0)) {
                triggerAttack();
            }

            // Right Mouse Click -> Dash (only on Desktop and if not clicking UI Stage Actor)
            boolean rightClick = false;
            if (isDesktop && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                Vector2 stageCoords = uiStage.screenToStageCoordinates(tmpVec.set(Gdx.input.getX(), Gdx.input.getY()));
                Actor hitActor = uiStage.hit(stageCoords.x, stageCoords.y, true);
                if (hitActor == null || hitActor == uiStage.getRoot()) {
                    rightClick = true;
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.L)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)
                || rightClick) {
                triggerDash();
            }

            // Kỹ năng chủ động 1, 2, 3: Z / X / C (tay trái) hoặc I / O / Y (cụm tay phải Laptop)
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z) || Gdx.input.isKeyJustPressed(Input.Keys.I)) triggerSkill(0);
            else if (Gdx.input.isKeyJustPressed(Input.Keys.X) || Gdx.input.isKeyJustPressed(Input.Keys.O)) triggerSkill(1);
            else if (Gdx.input.isKeyJustPressed(Input.Keys.C) || Gdx.input.isKeyJustPressed(Input.Keys.Y)) triggerSkill(2);

            // Ultimate Ability: U Key
            if (Gdx.input.isKeyJustPressed(Input.Keys.U)) triggerUltimate();

            // Eagle Strike: K / Middle Mouse
            if (Gdx.input.isKeyJustPressed(Input.Keys.K) || (isDesktop && Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE))) triggerEagleStrike();

            // Assassinate: F / Q
            if (Gdx.input.isKeyJustPressed(Input.Keys.F) || Gdx.input.isKeyJustPressed(Input.Keys.Q)) triggerAssassinate();

            // Context Action (Cannon / Revive / Talk NPC): E / ENTER
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
                if (btnShootCannon != null && btnShootCannon.isVisible()) shootCannonAtImperial();
                else if (btnRevivePartner != null && btnRevivePartner.isVisible()) revivePartner();
                else if (btnTalkNPC != null && btnTalkNPC.isVisible()) talkToBoy();
            }

            // Crouch: S / Down Arrow
            if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                if (!playerDead) {
                    if (isCrouching && !canStandUp()) {
                        actionLog.setText("Trần quá thấp! Cần bò ra khỏi đường hầm trước.");
                    } else {
                        isCrouching = !isCrouching;
                        needsHitboxUpdate = true;
                    }
                }
            }

            // Đổi vũ khí nhanh: TAB (luân phiên) hoặc phím 1..5 chọn trực tiếp (khi không mở Tiệm Đồ)
            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) cycleWeapon();
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) { equipWeapon(WeaponType.SWORD, false); SoundManager.getInstance().playOrbPickup(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) { equipWeapon(WeaponType.DUAL_DAGGERS, false); SoundManager.getInstance().playOrbPickup(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) { equipWeapon(WeaponType.BOW, false); SoundManager.getInstance().playOrbPickup(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) { equipWeapon(WeaponType.SHOTGUN, false); SoundManager.getInstance().playOrbPickup(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) { equipWeapon(WeaponType.AXE, false); SoundManager.getInstance().playOrbPickup(); }

            // Chống trôi Touchpad Laptop: chỉ xoay hướng theo con trỏ chuột khi người chơi thực sự BẤM NÚT CHUỘT ngoài HUD và không đè A/D
            if (isDesktop) {
                boolean mouseBtnHeld = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
                    || Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
                    || Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
                if (mouseBtnHeld && !kl && !kr) {
                    Vector2 stageCoords = uiStage.screenToStageCoordinates(tmpVec.set(Gdx.input.getX(), Gdx.input.getY()));
                    Actor hitActor = uiStage.hit(stageCoords.x, stageCoords.y, true);
                    boolean touchOnUI = (hitActor != null && hitActor != uiStage.getRoot());
                    if (!touchOnUI) {
                        Vector3 mouseWorldPos = viewport.unproject(tmpVec3.set(Gdx.input.getX(), Gdx.input.getY(), 0));
                        float dx = mouseWorldPos.x - playerBody.getPosition().x;
                        float dy = mouseWorldPos.y - playerBody.getPosition().y;
                        if (Math.abs(dx) > 0.4f && attackCombo == 0) {
                            playerFacing = (dx > 0) ? 1 : -1;
                        }
                        if (dy > 1.8f) {
                            mouseAimUp = true;
                        }
                    }
                }
            }
        }

        // Bật Menu Tạm dừng bằng P hoặc ESCAPE (khi đang trong trận và không vướng hộp thoại khác)
        if (!chatTyping && !modalConsumed && !escHandled && !isPaused && !isSettingsModalOpen
            && !storyDialogueActive && !storyInputUsedThisFrame()
            && (gameOverDialog == null || !gameOverDialog.isVisible())) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.P) || isBackJustPressed()) {
                togglePause();
            }
        }

        // Chế độ online (PVP/CO-OP) không thể dừng thời gian của đối phương -> chỉ mở menu, trận vẫn chạy
        if (isPaused && !isOnlineMode()) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // Render world frame static
            viewport.apply();
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            drawBackgroundParallax();
            for (int i = 0; i < platformsList.size; i++) {
                MapPlatform p = platformsList.get(i);
                if (!p.isGround && p.width >= 1.2f && platformConsoleTex != null) {
                    batch.setColor(Color.WHITE);
                    batch.draw(platformConsoleTex, p.x + 0.15f, p.y - 0.50f, 0.50f, 0.50f);
                    batch.draw(platformConsoleTex, p.x + p.width - 0.65f, p.y - 0.50f, 0.50f, 0.50f, 0, 0, 32, 32, true, false);
                }
                batch.setColor(Color.WHITE);
                int srcW = Math.max(1, (int)(p.width * 32f));
                int srcH = Math.max(1, (int)(p.height * 32f));
                if (p.isGround) {
                    batch.draw(groundBrickTexture, p.x, p.y, p.width, p.height, 0, 0, srcW, srcH, false, false);
                } else {
                    batch.draw(platformWoodTexture, p.x, p.y, p.width, p.height, 0, 0, srcW, srcH, false, false);
                }
                // Ranh giới mặt trên đứng được rõ nét 100%
                batch.setColor(0.02f, 0.02f, 0.04f, 0.85f);
                batch.draw(pixel, p.x, p.y + p.height - 0.12f, p.width, 0.04f);
                Color topHi = (currentMapThemeIndex == 2)
                    ? new Color(0.55f, 0.92f, 1.0f, 0.95f)
                    : ((currentMapThemeIndex == 3)
                        ? new Color(0.92f, 0.96f, 1.0f, 0.95f)
                        : new Color(1.0f, 0.88f, 0.55f, 0.95f));
                batch.setColor(topHi);
                batch.draw(pixel, p.x, p.y + p.height - 0.08f, p.width, 0.08f);
                batch.setColor(Color.WHITE);
                batch.draw(pixel, p.x, p.y + p.height - 0.02f, p.width, 0.02f);
            }
            for (int i = 0; i < collapsingPlatforms.size; i++) {
                CollapsingPlatform cp = collapsingPlatforms.get(i);
                if (cp.collapsed) continue;
                batch.draw(platformWoodTexture, cp.x, cp.y - cp.height, cp.width, cp.height);
            }
            if (wallTexture != null) {
                for (int i = 0; i < wallsList.size; i++) {
                    MapWall w = wallsList.get(i);
                    batch.draw(wallTexture, w.x, w.y, w.width, w.height);
                }
            }
            if (playerBody != null) {
                // Vẽ đúng sát thủ đang chơi và vũ khí đang cầm khi tạm dừng
                TextureRegion pauseFrame = getCharacterWeaponIdleFrame();
                drawCharacter(pauseFrame, "IDLE", playerBody.getPosition().x, playerBody.getPosition().y, playerFacing, Color.WHITE, 1.0f, true);
            }
            batch.end();

            uiStage.getViewport().apply();
            uiStage.act(delta);
            uiStage.draw();
            return;
        }

        float padKnobX = (touchpad != null && touchpad.isVisible()) ? touchpad.getKnobPercentX() : 0f;
        float padKnobY = (touchpad != null && touchpad.isVisible()) ? touchpad.getKnobPercentY() : 0f;

        boolean padLeft = padKnobX < -0.2f;
        boolean padRight = padKnobX > 0.2f;
        boolean padUp = padKnobY > 0.4f;

        boolean kLeft = !chatTyping && (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT));
        boolean kRight = !chatTyping && (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT));
        boolean kUp = !chatTyping && (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP));
        boolean kClimbHold = !chatTyping && (Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.SEMICOLON));
        isAimingUp = isAimingUpHold || kUp || padUp || mouseAimUp;

        boolean movingLeft = (moveLeft || kLeft || padLeft) && playerStunTimer <= 0 && !isPaused && !storyDialogueActive && !shopFreezesWorld();
        boolean movingRight = (moveRight || kRight || padRight) && playerStunTimer <= 0 && !isPaused && !storyDialogueActive && !shopFreezesWorld();
        if (attackCombo == 0) {
            if (movingLeft) playerFacing = -1;
            if (movingRight) playerFacing = 1;
        }

        boolean climbKey = (isClimbingHold || kClimbHold || kUp || padUp) && playerStunTimer <= 0 && !isPaused;

        if (needsHitboxUpdate && !playerDead) {
            updatePlayerHitbox(isCrouching);
            needsHitboxUpdate = false;
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Vector2 vel = playerBody.getLinearVelocity();
        Vector2 pos = playerBody.getPosition();

        if (weaponAttackCooldown > 0) weaponAttackCooldown -= delta;
        if (swordComboWindowTimer > 0) {
            swordComboWindowTimer -= delta;
            if (swordComboWindowTimer <= 0) nextSwordCombo = 1;
        }
        if (swingCooldown > 0) swingCooldown -= delta;

        boolean nearCannon = pos.dst(cannonPos) < 1.6f;
        btnShootCannon.setVisible(nearCannon && imperialAlive && !playerDead && !isMatchEnded && !isPaused && !inCutscene()
            && playerStunTimer <= 0 && currentGameMode != GameMode.PVP && currentGameMode != GameMode.BATTLE_5V5);

        if (hasSage) {
            boolean nearSage = pos.dst(sageX, sageY) < 2.0f;
            btnRevivePartner.setVisible(nearSage && remoteHp <= 0 && !partnerLeft && reviveCooldownTimer <= 0f
                && !playerDead && !isMatchEnded && !isPaused && currentGameMode == GameMode.COOP);
        }

        // Item respawn (15s cycle)
        itemRespawnTimer += delta;
        if (itemRespawnTimer >= 15f) {
            itemRespawnTimer = 0f;
            for (int i = 0; i < hpPickupActives.size; i++) hpPickupActives.set(i, true);
            for (int i = 0; i < weaponPickupActives.size; i++) weaponPickupActives.set(i, true);
        }

        // HP & Weapon pickups (không cho xác chết nhặt vật phẩm)
        if (!playerDead && !isMatchEnded && !inCutscene()) {
            int currentHpRestore = 35 + (int)(totalMatchTime / 20f) * 10;
            for (int i = 0; i < hpPickupPositions.size; i++) {
                if (hpPickupActives.get(i) && pos.dst(hpPickupPositions.get(i)) <= 0.85f && playerHp < playerMaxHp) {
                    playerHp = Math.min(playerMaxHp, playerHp + currentHpRestore);
                    playerHpBar.setHp(playerHp);
                    lblPlayerHp.setText("Máu: " + playerHp + " HP");
                    addDamageText(pos.x, pos.y + 0.4f, currentHpRestore, Color.GREEN);
                    hpPickupActives.set(i, false);
                    SoundManager.getInstance().playCoin();
                    actionLog.setText("Nhặt cục máu! Hồi +" + currentHpRestore + " HP");
                }
            }

            // Weapon pickup — luôn đổi sang vũ khí khác vũ khí đang cầm và cộng thêm sát thương (không hạ bonus đã nâng cấp ở Tiệm Đồ)
            for (int i = 0; i < weaponPickupPositions.size; i++) {
                if (weaponPickupActives.get(i) && pos.dst(weaponPickupPositions.get(i)) <= 0.85f) {
                    WeaponType[] allWeapons = WeaponType.values();
                    WeaponType picked = allWeapons[MathUtils.random(0, allWeapons.length - 1)];
                    if (picked == currentWeapon) {
                        picked = allWeapons[(picked.ordinal() + 1) % allWeapons.length];
                    }
                    if (swordDamageBonus < 80) swordDamageBonus = Math.min(80, swordDamageBonus + 8);
                    weaponPickupActives.set(i, false);
                    equipWeapon(picked, true);
                    SoundManager.getInstance().playCoin();
                }
            }
        }

        // Rope swing (Physics Pendulum simulation with momentum & gravity)
        updateRopesAndSwinging(delta);

        updateSkyEntities(delta);

        updateCollapsingPlatforms(delta);
        updateCheckpoints();

        // Water hazard in Map 2 (Chapter 2 or Online Map 2)
        boolean isWaterMap = (story != null && story.chapter == 2) || (currentGameMode != GameMode.SOLO && game != null && game.selectedOnlineMap == 2);
        if (isWaterMap && playerBody != null && !playerDead && !isMatchEnded) {
            if (playerBody.getPosition().y <= 0.45f) {
                playerHp = Math.max(1, playerHp - 25);
                playerHpBar.setHp(playerHp);
                lblPlayerHp.setText("Máu: " + playerHp + " HP");
                SoundManager.getInstance().playHurt();
                actionLog.setText("RƠI XUỐNG VÙNG NƯỚC SÂU BẾN CẢNG! Trở lại điểm lưu an toàn.");
                float respawnX = activeCheckpointX > 1.0f ? activeCheckpointX : 8.0f;
                float respawnY = activeCheckpointY > 0.8f ? activeCheckpointY + 0.5f : 1.2f;
                resetTraversalState();
                playerBody.setTransform(respawnX, respawnY, 0);
                playerBody.setLinearVelocity(0, 0);
                spawnHitSparks(respawnX, respawnY, 14, Color.CYAN);
            }
        }

        // Lock-On Enemy Target Detection for High-Altitude Eagle Strike
        lockOnEnemy = null;
        lockOnFighter = null;
        lockOnIsPrimaryRemote = false;
        lockOnPeerId = null;
        if (!playerDead && !isMatchEnded && !isEagleStriking && !inCutscene() && !isPaused
            && (pos.y >= 2.8f || isClimbing || isPlayerOnTowerSummit())) {
            float closestDist = 9.5f;
            for (int mi = 0; mi < activeMonsters.size; mi++) {
                GroundMob m = activeMonsters.get(mi);
                if (m.body != null && !m.isDead && m.body.getPosition().y < pos.y + 1.0f) {
                    float dist = pos.dst(m.body.getPosition());
                    if (dist < closestDist) {
                        closestDist = dist;
                        lockOnEnemy = m;
                    }
                }
            }
            // 5v5: cho phép Ứng Kích lên cả người chơi đối thủ lẫn bot phe địch
            if (currentGameMode == GameMode.BATTLE_5V5) {
                int pTeam = (game != null) ? game.selectedTeam : 1;
                if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && !remoteStealth
                    && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush) && remoteY < pos.y + 1.0f) {
                    float dist = pos.dst(remoteX, remoteY);
                    if (dist < closestDist) {
                        closestDist = dist;
                        lockOnIsPrimaryRemote = true;
                        lockOnPeerId = null;
                        lockOnFighter = null;
                        lockOnEnemy = null;
                    }
                }
                for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                    RemotePeer5v5 ep = extraPeers5v5.get(epi);
                    if (ep.team != pTeam && !ep.dead && ep.x != -999f && !ep.stealth
                        && isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush) && ep.y < pos.y + 1.0f) {
                        float dist = pos.dst(ep.x, ep.y);
                        if (dist < closestDist) {
                            closestDist = dist;
                            lockOnIsPrimaryRemote = false;
                            lockOnPeerId = ep.id;
                            lockOnFighter = null;
                            lockOnEnemy = null;
                        }
                    }
                }
                for (int fi = 0; fi < fighters5v5.size; fi++) {
                    Fighter5v5 f = fighters5v5.get(fi);
                    if (f.team != pTeam && !f.isDead && f.body != null && !f.isStealth && !(f.isInBush && !playerInBush)
                        && f.body.getPosition().y < pos.y + 1.0f) {
                        float dist = pos.dst(f.body.getPosition());
                        if (dist < closestDist) {
                            closestDist = dist;
                            lockOnFighter = f;
                            lockOnIsPrimaryRemote = false;
                            lockOnPeerId = null;
                            lockOnEnemy = null;
                        }
                    }
                }
            }
        }

        // Eagle Strike button visibility
        boolean canEagleStrike = (lockOnEnemy != null || lockOnFighter != null || lockOnIsPrimaryRemote || lockOnPeerId != null
            || (currentGameMode == GameMode.PVP && pos.y >= 2.8f) || isPlayerOnTowerSummit())
            && !playerDead && !isMatchEnded && !inCutscene() && !isPaused && !isGrabbedByPirate && playerStunTimer <= 0;
        btnEagleStrike.setVisible(canEagleStrike);

        if (isEagleStriking) {
            eagleStrikeTimeout += delta;
            float targetX, targetY;
            if (currentGameMode == GameMode.PVP && remoteX != -999f && !remoteDead) {
                targetX = remoteX; targetY = remoteY;
            } else if (eagleTargetMob != null && eagleTargetMob.body != null && !eagleTargetMob.isDead) {
                targetX = eagleTargetMob.body.getPosition().x; targetY = eagleTargetMob.body.getPosition().y;
            } else if (eagleTargetFighter != null && eagleTargetFighter.body != null && !eagleTargetFighter.isDead) {
                targetX = eagleTargetFighter.body.getPosition().x; targetY = eagleTargetFighter.body.getPosition().y;
            } else if (eagleTargetIsPrimaryRemote && remoteX != -999f && !remoteDead) {
                targetX = remoteX; targetY = remoteY;
            } else if (eagleTargetPeerId != null) {
                RemotePeer5v5 ep = findExtraPeer(eagleTargetPeerId);
                if (ep != null && !ep.dead && ep.x != -999f) {
                    targetX = ep.x; targetY = ep.y;
                } else {
                    targetX = pos.x + playerFacing * 1.5f; targetY = 0.65f;
                }
            } else {
                targetX = pos.x + playerFacing * 1.5f; targetY = 0.65f; // mục tiêu đã chết -> lao thẳng xuống đất
            }

            if (eagleStrikePhase == 1) { // Phase 1: High-diver Athletic Leaping Launch Arc
                eagleStrikeLaunchTimer -= delta;
                if (eagleStrikeLaunchTimer <= 0 || playerBody.getLinearVelocity().y <= 0.5f) {
                    eagleStrikePhase = 2; // Transition to High-Speed Dive
                    playerBody.setGravityScale(0f);
                    actionLog.setText("LAO XUỐNG ÁM SÁT MỤC TIÊU!");
                }
            } else if (eagleStrikePhase == 2) { // Phase 2: Dive onto Enemy
                float dxT = targetX - pos.x, dyT = targetY - pos.y;
                float distToTarget = (float) Math.sqrt(dxT * dxT + dyT * dyT);
                if (distToTarget > 0.0001f) {
                    playerBody.setLinearVelocity(dxT / distToTarget * EAGLE_STRIKE_SPEED, dyT / distToTarget * EAGLE_STRIKE_SPEED);
                }

                boolean nearEnemy = false;
                for (int i = 0; i < activeMonsters.size && !nearEnemy; i++) {
                    GroundMob m = activeMonsters.get(i);
                    nearEnemy = m.body != null && !m.isDead && pos.dst(m.body.getPosition()) < 1.6f;
                }
                if (!nearEnemy && currentGameMode == GameMode.BATTLE_5V5) {
                    int pTeam = (game != null) ? game.selectedTeam : 1;
                    if (remoteX != -999f && !remoteDead && remoteTeam != pTeam && pos.dst(remoteX, remoteY) < 1.6f) {
                        nearEnemy = true;
                    }
                    for (int epi = 0; epi < extraPeers5v5.size && !nearEnemy; epi++) {
                        RemotePeer5v5 ep = extraPeers5v5.get(epi);
                        nearEnemy = ep.team != pTeam && !ep.dead && ep.x != -999f && pos.dst(ep.x, ep.y) < 1.6f;
                    }
                    for (int i = 0; i < fighters5v5.size && !nearEnemy; i++) {
                        Fighter5v5 f = fighters5v5.get(i);
                        nearEnemy = f.team != pTeam && !f.isDead && f.body != null && pos.dst(f.body.getPosition()) < 1.6f;
                    }
                }

                if (nearEnemy || distToTarget <= 1.5f) {
                    boolean hitAny = applyEagleImpactDamage(2.0f);
                    stopEagleStrike(hitAny);
                } else if (eagleStrikeTimeout >= 3.8f) {
                    stopEagleStrike(false);
                }
            }
        }

        // Tall Building & Wall Climbing System
        boolean nearBuildingWall = false;
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (!p.isGround && p.height < 1.0f) {
                float leftWallX = p.x;
                float rightWallX = p.x + p.width;
                if ((Math.abs(pos.x - leftWallX) < 0.55f || Math.abs(pos.x - rightWallX) < 0.55f)
                    && pos.y >= 0.5f && pos.y <= p.y + 0.5f) {
                    nearBuildingWall = true;
                    break;
                }
            }
        }

        boolean onLadder = false;
        for (int i = 0; i < laddersList.size; i++) {
            Ladder l = laddersList.get(i);
            if (Math.abs(pos.x - (l.x + l.width / 2f)) < 0.65f
                && pos.y >= l.y - 0.2f && pos.y <= l.y + l.height + 0.35f) {
                onLadder = true; break;
            }
        }
        boolean nearWall = (pos.x <= 0.45f || pos.x >= (mapWidth - 0.45f)) || nearBuildingWall;

        boolean downKey = (!chatTyping && (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))) || padKnobY < -0.4f;

        // Xử lý cơ chế bám tường (Wall Cling) và leo trèo bằng joystick / phím
        if (isWallClinging && clingingWall != null) {
            if (playerDead || isEagleStriking || isMatchEnded || isGrabbedByPirate || playerStunTimer > 0) {
                isWallClinging = false;
                clingingWall = null;
                isClimbing = false;
            } else {
                // Kiểm tra nếu leo lên tới đỉnh tường -> Tự động leo lên mặt bục trên của tường
                if (pos.y >= (clingingWall.y + clingingWall.height - 0.25f)) {
                    float topX = clingingWall.x + clingingWall.width / 2f;
                    float topY = clingingWall.y + clingingWall.height + 0.45f;
                    playerBody.setTransform(topX, topY, 0);
                    playerBody.setLinearVelocity(0, 0);
                    isWallClinging = false;
                    clingingWall = null;
                    isClimbing = false;
                    isGrounded = true;
                    actionLog.setText("ĐÃ TRÈO LÊN ĐỈNH TƯỜNG!");
                    SoundManager.getInstance().playJump();
                } else if (isGrounded && pos.y <= clingingWall.y + 0.35f && (downKey || padKnobY < -0.3f)) {
                    // Chạm đất khi tụt xuống
                    isWallClinging = false;
                    clingingWall = null;
                    isClimbing = false;
                } else if ((clingingSide == -1 && (movingRight || padKnobX > 0.55f)) || (clingingSide == 1 && (movingLeft || padKnobX < -0.55f))) {
                    // Kéo cần hoặc phím rời xa khỏi bức tường -> Buông tay khỏi tường
                    isWallClinging = false;
                    clingingWall = null;
                    isClimbing = false;
                    actionLog.setText("BUÔNG TAY RỜI KHỎI TƯỜNG!");
                } else {
                    // Đang bám tường:
                    isClimbing = true;
                    float wallClimbVy = 0f;
                    if (climbKey || padKnobY > 0.30f || kUp) {
                        wallClimbVy = 2.8f;
                    } else if (downKey || padKnobY < -0.30f) {
                        wallClimbVy = -2.8f;
                    }
                    playerBody.setLinearVelocity(0, wallClimbVy);
                    // Giữ nhân vật bám sát mép bức tường
                    float clingX = (clingingSide == -1) ? (clingingWall.x - 0.25f) : (clingingWall.x + clingingWall.width + 0.25f);
                    playerBody.setTransform(clingX, pos.y, 0);
                    playerFacing = -clingingSide;
                }
            }
        } else {
            isClimbing = false;
            if ((climbKey || downKey) && !playerDead && !isEagleStriking && !isMatchEnded && !isGrabbedByPirate && playerStunTimer <= 0) {
                if (onLadder) {
                    float ladderVy = downKey ? -2.2f : 2.4f;
                    playerBody.setLinearVelocity(0, ladderVy);
                    isClimbing = true;
                } else if (nearWall) {
                    float climbVy = downKey ? -2.0f : 2.2f;
                    playerBody.setLinearVelocity(0, climbVy);
                    isClimbing = true;
                }
            }
        }

        updateBowLockOn();
        updateSkills(delta);

        // Projectile update — hỗ trợ tên khóa mục tiêu tự dẫn đường, kiếm khí xuyên thấu, tẩm độc & hút máu
        if (!inCutscene()) {
            for (int i = projectiles.size - 1; i >= 0; i--) {
                Projectile p = projectiles.get(i);
                if (p.homingArrow && resolveProjectileTargetPos(p, tmpVec)) {
                    float dxH = tmpVec.x - p.x;
                    float dyH = tmpVec.y - p.y;
                    float distH = (float) Math.sqrt(dxH * dxH + dyH * dyH);
                    float spd = Math.max(15.5f, (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy));
                    if (distH <= Math.max(0.42f, spd * delta * 1.25f)) {
                        p.x = tmpVec.x;
                        p.y = tmpVec.y;
                    } else {
                        p.vx = (dxH / distH) * spd;
                        p.vy = (dyH / distH) * spd;
                        p.x += p.vx * delta;
                        p.y += p.vy * delta;
                    }
                } else {
                    p.x += p.vx * delta;
                    p.y += p.vy * delta;
                }
                p.lifeTime -= delta;

                boolean hit = false;

                // Platform collision (bỏ qua bục gỗ cho tên khóa mục tiêu & kiếm khí xuyên thấu để không bị cản đường)
                if (!p.homingArrow && !p.piercing) {
                    for (int pli = 0; pli < platformsList.size; pli++) {
                        MapPlatform plat = platformsList.get(pli);
                        if (p.x >= plat.x && p.x <= plat.x + plat.width
                            && p.y >= plat.y && p.y <= plat.y + plat.height - 0.15f) {
                            hit = true; break;
                        }
                    }
                }

                if (!hit && p.isCannonBall && imperialAlive
                    && Math.abs(p.x - imperialX) < 1.4f && Math.abs(p.y - imperialY) < 1.4f) {
                    imperialAlive = false;
                    imperialState = "FALLING";
                    imperialDeathTimer = 1.2f;
                    score += 500; goldEarned += 150;
                    updateScoreGoldUI();
                    actionLog.setText("PHÁO BẮN HẠ LÍNH BAY TRIỀU ĐÌNH!");
                    addDamageText(imperialX, imperialY + 0.5f, 999, Color.GOLD);
                    SoundManager.getInstance().playBossSmash();
                    cameraShakeTime = 0.5f; cameraShakeIntensity = 0.35f;
                    hit = true;
                } else if (!hit && !p.isCannonBall) {
                    if (p.fromRemotePeer) {
                        // Đạn hiển thị từ người chơi khác: chỉ tạo hiệu ứng va chạm (sát thương thật do chủ chiêu gửi qua gói "hit" / "mobHit")
                        int pTeam = (game != null) ? game.selectedTeam : 1;
                        boolean isHostile = (currentGameMode == GameMode.PVP) || (currentGameMode == GameMode.BATTLE_5V5 && p.ownerTeam != pTeam);
                        if (isHostile && !playerDead && playerBody != null) {
                            Vector2 pPos = playerBody.getPosition();
                            if (Math.abs(p.x - pPos.x) < 0.68f && Math.abs(p.y - (pPos.y + 0.20f)) < 0.85f) {
                                if (!p.piercing || p.hitIds.add(9999)) {
                                    spawnHitSparks(pPos.x, pPos.y + 0.3f, 10, p.applyPoison ? Color.PURPLE : Color.CYAN);
                                    if (!p.piercing) hit = true;
                                }
                            }
                        }
                        if (!hit && currentGameMode == GameMode.COOP) {
                            for (int j = activeMonsters.size - 1; j >= 0; j--) {
                                GroundMob m = activeMonsters.get(j);
                                if (m.body != null && !m.isDead) {
                                    Vector2 mp = m.body.getPosition();
                                    float hitX = m.isBoss ? 0.82f : 0.65f;
                                    float hitY = m.isBoss ? 0.98f : 0.78f;
                                    if (Math.abs(p.x - mp.x) < hitX && Math.abs(p.y - (mp.y + 0.18f)) < hitY) {
                                        if (p.piercing && !p.hitIds.add(System.identityHashCode(m))) continue;
                                        spawnHitSparks(mp.x, mp.y + 0.28f, 10, p.applyPoison ? Color.PURPLE : Color.CYAN);
                                        if (!p.piercing) { hit = true; break; }
                                    }
                                }
                            }
                        }
                    } else if (currentGameMode == GameMode.PVP) {
                        if (remoteX != -999f && !remoteDead
                            && Math.abs(p.x - remoteX) < 0.65f && Math.abs(p.y - (remoteY + 0.20f)) < 0.82f) {
                            if (!p.piercing || p.hitIds.add(-103)) {
                                sendActionPacketFull("hit", p.damage, 0, primaryRemoteId, false, p.applyPoison);
                                if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                spawnHitSparks(remoteX, remoteY + 0.25f, 10, p.applyPoison ? Color.MAGENTA : Color.CYAN);
                                if (!p.piercing) hit = true;
                            }
                        }
                    } else if (currentGameMode == GameMode.BATTLE_5V5) {
                        int pTeam = (game != null) ? game.selectedTeam : 1;
                        float titanMult = playerHasTitanBuff ? 1.35f : 1.0f;
                        int pDmg = (int)(p.damage * playerDamageBuff * titanMult);

                        if (remoteX != -999f && !remoteDead && remoteTeam != pTeam) {
                            if (Math.abs(p.x - remoteX) < 0.65f && Math.abs(p.y - (remoteY + 0.20f)) < 0.82f) {
                                if (!p.piercing || p.hitIds.add(-103)) {
                                    sendActionPacketFull("hit", pDmg, 0, primaryRemoteId, false, p.applyPoison);
                                    remoteHitFlashTimer = 0.20f;
                                    spawnHitSparks(remoteX, remoteY + 0.28f, 12, p.applyPoison ? Color.PURPLE : Color.WHITE);
                                    addDamageText(remoteX, remoteY + 0.42f, pDmg, Color.RED);
                                    if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                    if (!p.piercing) hit = true;
                                }
                            }
                        }

                        if (!hit) {
                            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                                if (ep.team != pTeam && !ep.dead && ep.x != -999f) {
                                    if (Math.abs(p.x - ep.x) < 0.65f && Math.abs(p.y - (ep.y + 0.20f)) < 0.82f) {
                                        if (p.piercing && !p.hitIds.add(System.identityHashCode(ep))) continue;
                                        sendActionPacketFull("hit", pDmg, 0, ep.id, false, p.applyPoison);
                                        ep.hitFlashTimer = 0.20f;
                                        spawnHitSparks(ep.x, ep.y + 0.28f, 12, p.applyPoison ? Color.PURPLE : Color.WHITE);
                                        addDamageText(ep.x, ep.y + 0.42f, pDmg, Color.RED);
                                        if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                        if (!p.piercing) { hit = true; break; }
                                    }
                                }
                            }
                        }

                        if (!hit) {
                            for (int fi = 0; fi < fighters5v5.size; fi++) {
                                Fighter5v5 f = fighters5v5.get(fi);
                                if (f.team != pTeam && !f.isDead && f.body != null) {
                                    Vector2 fPos = f.body.getPosition();
                                    if (Math.abs(p.x - fPos.x) < 0.65f && Math.abs(p.y - (fPos.y + 0.20f)) < 0.82f) {
                                        if (p.piercing && !p.hitIds.add(System.identityHashCode(f))) continue;
                                        boolean killed = f.takeDamage(pDmg);
                                        if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                        spawnHitSparks(fPos.x, fPos.y + 0.3f, 12, p.applyPoison ? Color.PURPLE : Color.WHITE);
                                        addDamageText(fPos.x, fPos.y + 0.4f, pDmg, Color.RED);
                                        if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                                        if (!p.piercing) { hit = true; break; }
                                    }
                                }
                            }
                        }
                        if (!hit && ancientTitan != null && ancientTitan.isAlive) {
                            if (Math.abs(p.x - ancientTitan.x) < 1.05f && Math.abs(p.y - (ancientTitan.y + 0.6f)) < 1.25f) {
                                if (!p.piercing || p.hitIds.add(-104)) {
                                    boolean killed = ancientTitan.takeDamage(pDmg);
                                    if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                    spawnHitSparks(ancientTitan.x, ancientTitan.y + 0.4f, 15, Color.GOLD);
                                    addDamageText(ancientTitan.x, ancientTitan.y + 0.6f, pDmg, Color.GOLD);
                                    if (killed) onTitanSlain(pTeam);
                                    if (!p.piercing) hit = true;
                                }
                            }
                        }
                        if (!hit) {
                            for (int ti = 0; ti < outpostTurrets.size; ti++) {
                                OutpostTurret turret = outpostTurrets.get(ti);
                                if (turret.team != pTeam && !turret.isDestroyed) {
                                    if (Math.abs(p.x - turret.x) < 0.85f && Math.abs(p.y - (turret.y + 0.5f)) < 1.25f) {
                                        if (p.piercing && !p.hitIds.add(System.identityHashCode(turret))) continue;
                                        boolean destroyed = turret.takeDamage(pDmg);
                                        spawnHitSparks(turret.x, turret.y + 0.6f, 15, Color.YELLOW);
                                        addDamageText(turret.x, turret.y + 0.9f, pDmg, Color.YELLOW);
                                        if (destroyed) onTurretDestroyed(turret, pTeam);
                                        if (!p.piercing) { hit = true; break; }
                                    }
                                }
                            }
                        }
                    } else {
                        boolean fromEnemy = !p.isArrow && !p.isSlingshotPellet && !p.isShotgun && !p.isCannonBall && p.skillVisualType == 0;
                        if (fromEnemy) {
                            if (Math.abs(p.x - pos.x) < 0.6f && Math.abs(p.y - pos.y) < 0.8f && !playerDead && !bossIntroActive) {
                                int actualDmg = absorbShield(p.damage);
                                playerHp = Math.max(0, playerHp - actualDmg);
                                playerHpBar.setHp(playerHp);
                                lblPlayerHp.setText("Máu: " + playerHp + " HP");
                                playerHitFlashTimer = 0.20f;
                                SoundManager.getInstance().playHurt();
                                addDamageText(pos.x, pos.y + 0.4f, actualDmg, Color.RED);
                                if (playerHp <= 0) { playerDead = true; showGameOverDialog(false); }
                                hit = true;
                            }
                        } else {
                            for (int j = activeMonsters.size - 1; j >= 0; j--) {
                                GroundMob m = activeMonsters.get(j);
                                if (m.body != null && !m.isDead) {
                                    Vector2 mp = m.body.getPosition();
                                    float hitX = m.isBoss ? 0.82f : 0.65f;
                                    float hitY = m.isBoss ? 0.98f : 0.78f;
                                    if (Math.abs(p.x - mp.x) < hitX && Math.abs(p.y - (mp.y + 0.18f)) < hitY) {
                                        if (p.piercing && !p.hitIds.add(System.identityHashCode(m))) continue;
                                        if (p.applyPoison) {
                                            m.poisonTimer = 3.5f;
                                            m.slowTimer = 2.5f;
                                        }
                                        if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                        damageGroundMob(m, p.damage, true);
                                        spawnHitSparks(mp.x, mp.y + 0.28f, 10,
                                            p.applyPoison ? Color.PURPLE : (p.homingArrow ? Color.GREEN : Color.CYAN));
                                        if (!p.piercing) { hit = true; break; }
                                    }
                                }
                            }

                            if (!hit && hasSkyPirate && pirateAlive) {
                                if (Math.abs(p.x - pirateX) < 0.88f && Math.abs(p.y - pirateY) < 1.05f) {
                                    if (!p.piercing || p.hitIds.add(-101)) {
                                        if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                        damagePirate(p.damage);
                                        if (!p.piercing) hit = true;
                                    }
                                }
                            }

                            if (!hit && hasImperial && imperialAlive) {
                                if (Math.abs(p.x - imperialX) < 0.98f && Math.abs(p.y - imperialY) < 1.15f) {
                                    if (!p.piercing || p.hitIds.add(-102)) {
                                        if (p.lifeSteal > 0) healPlayer(p.lifeSteal);
                                        damageImperial(p.damage);
                                        if (!p.piercing) hit = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (hit || p.lifeTime <= 0 || p.x < 0 || p.x > mapWidth || p.y < 0)
                    projectiles.removeIndex(i);
            }
        }

        // Hoạt ảnh tấn công theo từng vũ khí và combo kiếm
        if (attackCombo > 0 && (playerDead || isMatchEnded || playerStunTimer > 0 || isPaused || inCutscene())) {
            attackCombo = 0;
            attackTime = 0f;
            queuedAttacks = 0;
            hitThisAttack = false;
            if (isAssassinating) {
                isAssassinating = false;
                pendingAssassinateMob = null;
                pendingAssassinateFighter = null;
                pendingAssassinateRemote = false;
                pendingAssassinatePeerId = null;
            }
        }
        if (attackCombo > 0) {
            float atkSpd = (p1AwakenedTimer > 0f) ? 1.35f : 1.0f;
            attackTime += delta * atkSpd;
            boolean isP2 = "P2".equals(selectedChar) && darkAtkRegionsList.size > 0;
            Animation<TextureRegion> currentAnim = getCurrentPlayerAttackAnim();

            if (isAssassinating) {
                // Xử lý đòn ám sát điện ảnh: chơi animation chém thường, kết thúc đòn đánh kẻ địch chết luôn
                if (currentAnim != null) {
                    float animDur = currentAnim.getAnimationDuration();
                    // Khi đòn chém vung tới nhịp va chạm (>= 70% thời lượng hoạt ảnh) -> Nạn nhân chết ngay lập tức!
                    if (attackTime >= animDur * 0.70f && !assassinateExecuted) {
                        assassinateExecuted = true;
                        SoundManager.getInstance().playHeavyHit();
                        cameraShakeTime = 0.45f;
                        cameraShakeIntensity = 0.35f;
                        hitStopTimer = 0.12f;
                        spawnHitSparks(assassinateVictimX, assassinateVictimY + 0.4f, 25, Color.GOLD);
                        spawnHitSparks(assassinateVictimX, assassinateVictimY + 0.4f, 18, Color.RED);

                        if (pendingAssassinateMob != null && !pendingAssassinateMob.isDead && pendingAssassinateMob.body != null) {
                            addCustomText(assassinateVictimX, assassinateVictimY + 0.8f, "ÁM SÁT HOÀN HẢO!", Color.GOLD);
                            score += 500;
                            goldEarned += 100;
                            updateScoreGoldUI();
                            actionLog.setText("ÁM SÁT HOÀN HẢO TỪ PHÍA SAU! +100 VÀNG!");
                            damageGroundMob(pendingAssassinateMob, 9999, true); // Instakill
                        } else if (pendingAssassinateFighter != null && !pendingAssassinateFighter.isDead && pendingAssassinateFighter.body != null) {
                            addCustomText(assassinateVictimX, assassinateVictimY + 0.8f, "ÁM SÁT KẾT LIỄU!", Color.GOLD);
                            int pTeam = (game != null) ? game.selectedTeam : 1;
                            String myName = (game != null && game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần";
                            pendingAssassinateFighter.takeDamage(9999);
                            on5v5Kill(myName, pTeam, pendingAssassinateFighter.name, pendingAssassinateFighter.team);
                            broadcast5v5Kill(myName, pTeam, pendingAssassinateFighter.name, pendingAssassinateFighter.team);
                            actionLog.setText("ÁM SÁT ĐIỆN ẢNH! TIÊU DIỆT " + pendingAssassinateFighter.name.toUpperCase() + "!");
                        } else if (pendingAssassinateRemote && !remoteDead && remoteX != -999f) {
                            addCustomText(assassinateVictimX, assassinateVictimY + 0.8f, "ÁM SÁT KẾT LIỄU!", Color.GOLD);
                            sendActionPacket("hit", 9999, 1);
                            actionLog.setText("ÁM SÁT ĐIỆN ẢNH ĐỐI THỦ!");
                        } else if (pendingAssassinatePeerId != null) {
                            RemotePeer5v5 ep = findExtraPeer(pendingAssassinatePeerId);
                            if (ep != null && !ep.dead && ep.x != -999f) {
                                addCustomText(assassinateVictimX, assassinateVictimY + 0.8f, "ÁM SÁT KẾT LIỄU!", Color.GOLD);
                                sendActionPacketToPeer("hit", 9999, 1, ep.id);
                                actionLog.setText("ÁM SÁT ĐIỆN ẢNH ĐỐI THỦ!");
                            }
                        }
                    }

                    if (currentAnim.isAnimationFinished(attackTime) || attackTime >= animDur + 0.08f) {
                        isAssassinating = false;
                        attackCombo = 0;
                        attackTime = 0f;
                        hitThisAttack = false;
                        pendingAssassinateMob = null;
                        pendingAssassinateFighter = null;
                        pendingAssassinateRemote = false;
                        pendingAssassinatePeerId = null;
                    }
                } else {
                    isAssassinating = false;
                    attackCombo = 0;
                    attackTime = 0f;
                    pendingAssassinateMob = null;
                    pendingAssassinateFighter = null;
                    pendingAssassinateRemote = false;
                    pendingAssassinatePeerId = null;
                }
            } else if (currentAnim != null) {
                if (!hitThisAttack) {
                    int damage;
                    float timeDmgScale = 1.0f + (totalMatchTime / 60f) * 0.25f;
                    if (isP2) {
                        int baseDmg = (attackCombo == 1) ? 45 : (attackCombo == 2 ? 65 : 95);
                        damage = (int)(baseDmg * timeDmgScale) + swordDamageBonus;
                    } else {
                        damage = ((attackCombo == 1) ? SWORD_DMG_1 : (attackCombo == 2 ? SWORD_DMG_2 : SWORD_DMG_3)) + swordDamageBonus;
                    }

                    if (currentGameMode == GameMode.PVP) {
                        float distToTarget = remoteX - pos.x;
                        boolean isFacing = (playerFacing == 1 && distToTarget > 0) || (playerFacing == -1 && distToTarget < 0);
                        if (!remoteDead && remoteX != -999f && Math.abs(distToTarget) <= 1.85f && Math.abs(remoteY - pos.y) < 1.4f && isFacing) {
                            if (isP2 && checkP2Backstab(remoteX, remoteFacing)) {
                                damage = (int)(damage * 1.50f);
                                healPlayer(80);
                                addCustomText(remoteX, remoteY + 0.8f, "ÁM SÁT CHÍ MẠNG!", Color.MAGENTA);
                                SoundManager.getInstance().playHeavyHit();
                            }
                            sendActionPacket("hit", damage, 0);
                            hitThisAttack = true;
                            addP1PassiveStack();
                            if (isP2) {
                                spawnHitSparks(remoteX, remoteY + 0.3f, (attackCombo == 3) ? 25 : 14, (attackCombo == 3) ? Color.PURPLE : Color.MAGENTA);
                            }
                        }
                    } else if (currentGameMode == GameMode.BATTLE_5V5) {
                        int pTeam = (game != null) ? game.selectedTeam : 1;
                        float titanMult = playerHasTitanBuff ? 1.35f : 1.0f;
                        float ambushMult = playerAmbushBuff ? 1.40f : 1.0f;
                        int finalDmg = (int)(damage * playerDamageBuff * titanMult * ambushMult);
                        if (remoteX != -999f && !remoteDead && remoteTeam != pTeam) {
                            float distToRemote = remoteX - pos.x;
                            boolean isFacingR = (playerFacing == 1 && distToRemote > 0) || (playerFacing == -1 && distToRemote < 0);
                            if (Math.abs(distToRemote) <= 1.85f && Math.abs(remoteY - pos.y) < 1.4f && isFacingR) {
                                sendActionPacket("hit", finalDmg, 0);
                                remoteHitFlashTimer = 0.20f;
                                hitThisAttack = true;
                                addP1PassiveStack();
                                spawnHitSparks(remoteX, remoteY + 0.3f, 15, isP2 ? Color.PURPLE : Color.WHITE);
                                addDamageText(remoteX, remoteY + 0.5f, finalDmg, Color.RED);
                            }
                        }
                        for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                            RemotePeer5v5 ep = extraPeers5v5.get(epi);
                            if (ep.team != pTeam && !ep.dead && ep.x != -999f) {
                                float distToEp = ep.x - pos.x;
                                boolean isFacingEp = (playerFacing == 1 && distToEp > 0) || (playerFacing == -1 && distToEp < 0);
                                if (Math.abs(distToEp) <= 1.85f && Math.abs(ep.y - pos.y) < 1.4f && isFacingEp) {
                                    sendActionPacketToPeer("hit", finalDmg, 0, ep.id);
                                    ep.hitFlashTimer = 0.20f;
                                    hitThisAttack = true;
                                    addP1PassiveStack();
                                    spawnHitSparks(ep.x, ep.y + 0.3f, 15, isP2 ? Color.PURPLE : Color.WHITE);
                                    addDamageText(ep.x, ep.y + 0.5f, finalDmg, Color.RED);
                                }
                            }
                        }
                        for (int i = 0; i < fighters5v5.size; i++) {
                            Fighter5v5 f = fighters5v5.get(i);
                            if (f.team != pTeam && !f.isDead && f.body != null) {
                                Vector2 fPos = f.body.getPosition();
                                float distToF = fPos.x - pos.x;
                                boolean isFacing = (playerFacing == 1 && distToF > 0) || (playerFacing == -1 && distToF < 0);
                                if (Math.abs(distToF) <= 1.85f && Math.abs(fPos.y - pos.y) < 1.4f && isFacing) {
                                    int fDmg = finalDmg;
                                    if (isP2 && checkP2Backstab(fPos.x, f.facing)) {
                                        fDmg = (int)(fDmg * 1.50f);
                                        healPlayer(80);
                                        addCustomText(fPos.x, fPos.y + 0.8f, "ÁM SÁT CHÍ MẠNG!", Color.MAGENTA);
                                        SoundManager.getInstance().playHeavyHit();
                                    }
                                    boolean killed = f.takeDamage(fDmg);
                                    hitThisAttack = true;
                                    addP1PassiveStack();
                                    spawnHitSparks(fPos.x, fPos.y + 0.3f, 15, isP2 ? Color.PURPLE : Color.WHITE);
                                    addDamageText(fPos.x, fPos.y + 0.5f, fDmg, Color.RED);
                                    if (killed) {
                                        if (isP2) {
                                            healPlayer(120);
                                            for (int s = 0; s < 3; s++) skillCooldowns[s] = Math.max(0f, skillCooldowns[s] - 2.0f);
                                            addCustomText(pos.x, pos.y + 1.0f, "+120 HP (HÚT HỒN)", Color.MAGENTA);
                                        }
                                        on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                                    }
                                }
                            }
                        }

                        // Danh can chien vao Than Thu Bao Chua
                        if (ancientTitan != null && ancientTitan.isAlive) {
                            float distToT = ancientTitan.x - pos.x;
                            boolean isFacingT = (playerFacing == 1 && distToT > 0) || (playerFacing == -1 && distToT < 0);
                            if (Math.abs(distToT) <= 2.2f && Math.abs(ancientTitan.y - pos.y) < 1.8f && isFacingT) {
                                boolean killed = ancientTitan.takeDamage(finalDmg);
                                hitThisAttack = true;
                                spawnHitSparks(ancientTitan.x, ancientTitan.y + 0.5f, 20, Color.GOLD);
                                addDamageText(ancientTitan.x, ancientTitan.y + 0.8f, finalDmg, Color.GOLD);
                                if (killed) onTitanSlain(pTeam);
                            }
                        }

                        // Danh can chien vao Thap Tien Don doi phuong
                        for (int ti = 0; ti < outpostTurrets.size; ti++) {
                            OutpostTurret turret = outpostTurrets.get(ti);
                            if (turret.team != pTeam && !turret.isDestroyed) {
                                float distToTur = turret.x - pos.x;
                                boolean isFacingTur = (playerFacing == 1 && distToTur > 0) || (playerFacing == -1 && distToTur < 0);
                                if (Math.abs(distToTur) <= 2.2f && Math.abs(turret.y - pos.y) < 2.0f && isFacingTur) {
                                    boolean destroyed = turret.takeDamage(finalDmg);
                                    hitThisAttack = true;
                                    spawnHitSparks(turret.x, turret.y + 0.8f, 20, Color.YELLOW);
                                    addDamageText(turret.x, turret.y + 1.2f, finalDmg, Color.YELLOW);
                                    if (destroyed) onTurretDestroyed(turret, pTeam);
                                    break;
                                }
                            }
                        }

                        if (hitThisAttack && playerAmbushBuff) {
                            playerAmbushBuff = false;
                            addCustomText(pos.x, pos.y + 0.8f, "PHỤC KÍCH! +40%", Color.GOLD);
                        }
                    } else {
                        for (int i = activeMonsters.size - 1; i >= 0; i--) {
                            GroundMob m = activeMonsters.get(i);
                            if (m.body != null && !m.isDead) {
                                float distToM = m.body.getPosition().x - pos.x;
                                boolean isFacing = (playerFacing == 1 && distToM > 0) || (playerFacing == -1 && distToM < 0);
                                if (Math.abs(distToM) <= 1.85f && Math.abs(m.body.getPosition().y - pos.y) < 1.4f && isFacing) {
                                    int mDmg = damage;
                                    if (isP2 && checkP2Backstab(m.body.getPosition().x, m.facing)) {
                                        mDmg = (int)(mDmg * 1.50f);
                                        healPlayer(60);
                                        addCustomText(m.body.getPosition().x, m.body.getPosition().y + 0.8f, "ÁM SÁT CHÍ MẠNG!", Color.MAGENTA);
                                        SoundManager.getInstance().playHeavyHit();
                                    }
                                    damageGroundMob(m, mDmg, true);
                                    hitThisAttack = true;
                                    addP1PassiveStack();
                                    if (isP2) {
                                        spawnHitSparks(m.body.getPosition().x, m.body.getPosition().y + 0.3f, (attackCombo == 3) ? 22 : 12, Color.PURPLE);
                                    } else {
                                        spawnHitSparks(m.body.getPosition().x, m.body.getPosition().y + 0.3f, (attackCombo == 3) ? 22 : 12, Color.CYAN);
                                    }
                                }
                            }
                        }
                        if (hasSkyPirate && pirateAlive) {
                            float distP = pirateX - pos.x;
                            boolean isFacing = (playerFacing == 1 && distP > 0) || (playerFacing == -1 && distP < 0);
                            if (Math.abs(distP) <= 2.1f && Math.abs(pirateY - pos.y) < 1.6f && isFacing) {
                                damagePirate(damage);
                                hitThisAttack = true;
                            }
                        }
                        if (hasImperial && imperialAlive) {
                            float distI = imperialX - pos.x;
                            boolean isFacing = (playerFacing == 1 && distI > 0) || (playerFacing == -1 && distI < 0);
                            if (Math.abs(distI) <= 2.1f && Math.abs(imperialY - pos.y) < 1.6f && isFacing) {
                                damageImperial(damage);
                                hitThisAttack = true;
                            }
                        }
                    }
                }

                if (currentAnim.isAnimationFinished(attackTime)) {
                    if (currentWeapon == WeaponType.SWORD) {
                        if (queuedAttacks > 0 || isAttackKeyHeld(chatTyping) || isAimingUpHold || isAttackButtonPressed) {
                            if (queuedAttacks > 0) queuedAttacks--;
                            if (attackCombo < 3) {
                                attackCombo++;
                                attackTime = 0f;
                                hitThisAttack = false;
                                if (isP2) {
                                    if (attackCombo == 2) {
                                        SoundManager.getInstance().playSlash();
                                        actionLog.setText("VÂN U: PHI THIÊN BẠO KÍCH (NHỊP 2)!");
                                    } else {
                                        SoundManager.getInstance().playHeavyHit();
                                        actionLog.setText("VÂN U: TỬ KÍCH ĐOẠT MỆNH (NHỊP 3)!");
                                        cameraShakeTime = 0.20f;
                                        cameraShakeIntensity = 0.22f;
                                    }
                                } else {
                                    if (attackCombo == 2) {
                                        SoundManager.getInstance().playSlash();
                                        actionLog.setText("VÂN TIÊU: PHONG LÔI TRẢM (NHỊP 2)!");
                                    } else {
                                        SoundManager.getInstance().playHeavyHit();
                                        actionLog.setText("VÂN TIÊU: CUỒNG PHONG TUYỆT DIỆT (NHỊP 3)!");
                                        cameraShakeTime = 0.20f;
                                        cameraShakeIntensity = 0.22f;
                                    }
                                }
                            } else {
                                attackCombo = 1;
                                attackTime = 0f;
                                hitThisAttack = false;
                                SoundManager.getInstance().playSlash();
                                actionLog.setText(isP2 ? "VÂN U: TRẢM LIÊN HOÀN (NHỊP 1)!" : "VÂN TIÊU: BẠCH PHI KIẾM (NHỊP 1)!");
                            }
                        } else {
                            // Giữ cửa sổ nối nhịp 1.3s để bấm rời từng phát vẫn đi đủ trangchem1 -> 2 -> 3
                            nextSwordCombo = (attackCombo % 3) + 1;
                            swordComboWindowTimer = 1.3f;
                            attackCombo = 0; attackTime = 0f;
                            queuedAttacks = 0; hitThisAttack = false;
                        }
                    } else if (currentWeapon == WeaponType.DUAL_DAGGERS
                        && (queuedAttacks > 0 || isAttackKeyHeld(chatTyping) || isAttackButtonPressed)) {
                        if (queuedAttacks > 0) queuedAttacks--;
                        daggerComboStep = (daggerComboStep == 1) ? 2 : 1;
                        attackCombo = daggerComboStep;
                        attackTime = 0f;
                        hitThisAttack = true;
                        applyDaggerSlashDamage(pos);
                    } else {
                        attackCombo = 0; attackTime = 0f;
                        queuedAttacks = 0; hitThisAttack = false;
                    }
                }
            } else {
                attackCombo = 0; attackTime = 0f; queuedAttacks = 0;
            }
        }

        // Movement
        if (playerDead || isMatchEnded) {
            playerBody.setLinearVelocity(0, vel.y);
        } else if (playerStunTimer > 0) {
            playerBody.setLinearVelocity(0, vel.y);
        } else if (isAssassinating) {
            playerBody.setLinearVelocity(0, 0);
        } else if (isP1UltActive || isP2DashingAssassinate) {
            // Ultimate handles its own velocity
        } else if (!isClimbing && !isSwinging && !isEagleStriking && !isGrabbedByPirate) {
            if (isDashing) {
                dashTime -= delta;
                playerBody.setLinearVelocity(playerFacing * DASH_SPEED, vel.y);

                if (!hitThisDash) {
                    if (currentGameMode == GameMode.PVP) {
                        if (!remoteDead && remoteX != -999f && Math.abs(remoteX - pos.x) < 1.6f && Math.abs(remoteY - pos.y) < 1.0f) {
                            sendActionPacket("hit", DASH_DMG, 0);
                            hitThisDash = true;
                        }
                    } else if (currentGameMode == GameMode.BATTLE_5V5) {
                        int pTeam = (game != null) ? game.selectedTeam : 1;
                        int finalDmg = (int)(DASH_DMG * playerDamageBuff * (playerHasTitanBuff ? 1.35f : 1f));
                        if (remoteX != -999f && !remoteDead && remoteTeam != pTeam
                            && Math.abs(remoteX - pos.x) < 1.6f && Math.abs(remoteY - pos.y) < 1.0f) {
                            sendActionPacket("hit", finalDmg, 0);
                            remoteHitFlashTimer = 0.20f;
                            addDamageText(remoteX, remoteY + 0.5f, finalDmg, Color.CYAN);
                            hitThisDash = true;
                        }
                        if (!hitThisDash) {
                            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                                if (ep.team != pTeam && !ep.dead && ep.x != -999f
                                    && Math.abs(ep.x - pos.x) < 1.6f && Math.abs(ep.y - pos.y) < 1.0f) {
                                    sendActionPacketToPeer("hit", finalDmg, 0, ep.id);
                                    ep.hitFlashTimer = 0.20f;
                                    addDamageText(ep.x, ep.y + 0.5f, finalDmg, Color.CYAN);
                                    hitThisDash = true;
                                    break;
                                }
                            }
                        }
                        if (!hitThisDash) {
                            for (int i = 0; i < fighters5v5.size; i++) {
                                Fighter5v5 f = fighters5v5.get(i);
                                if (f.team != pTeam && !f.isDead && f.body != null && Math.abs(f.body.getPosition().x - pos.x) < 1.6f && Math.abs(f.body.getPosition().y - pos.y) < 1.0f) {
                                    boolean killed = f.takeDamage(finalDmg);
                                    spawnHitSparks(f.body.getPosition().x, f.body.getPosition().y + 0.3f, 15, Color.CYAN);
                                    addDamageText(f.body.getPosition().x, f.body.getPosition().y + 0.5f, finalDmg, Color.CYAN);
                                    if (killed) on5v5Kill((game != null && game.playerName != null ? game.playerName : "Chiến Thần"), pTeam, f.name, f.team);
                                    hitThisDash = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        // FIX: reverse-index
                        for (int i = activeMonsters.size - 1; i >= 0; i--) {
                            GroundMob m = activeMonsters.get(i);
                            if (m.body != null && !m.isDead && Math.abs(m.body.getPosition().x - pos.x) < 1.6f
                                && Math.abs(m.body.getPosition().y - pos.y) < 1.0f) {
                                damageGroundMob(m, DASH_DMG, true);
                                hitThisDash = true;
                            }
                        }
                    }
                }
                if (dashTime <= 0) isDashing = false;
            } else if (attackCombo > 0 && (currentWeapon == WeaponType.SWORD || currentWeapon == WeaponType.AXE)) {
                playerBody.setLinearVelocity(0, vel.y);
            } else {
                float targetVel = 0;
                float windSkillMult = (p1WindShieldTimer > 0f) ? 1.35f : 1.0f;
                float poisonSlowMult = (playerPoisonSlowTimer > 0f) ? 0.65f : 1.0f;
                float speed = isCrouching ? (PLAYER_SPEED * 0.6f) : (isP2StealthActive ? (PLAYER_SPEED * 1.85f) : PLAYER_SPEED);
                speed *= playerSpeedBuff * windSkillMult * poisonSlowMult;
                if (movingLeft) targetVel = -speed;
                if (movingRight) targetVel = speed;
                if (!isGrounded && (movingLeft || movingRight)) {
                    float air = (isP2StealthActive ? AIR_SPEED * 1.6f : AIR_SPEED) * playerSpeedBuff * windSkillMult * poisonSlowMult;
                    targetVel = movingLeft ? -air : air;
                }

                // Parkour Wall Slide trên mọi bề mặt thẳng đứng của công trình
                int wallSlideSide = checkNearWallSide(pos);
                if (!isGrounded && !isClimbing && !isDashing && vel.y < 0 && wallSlideSide != 0) {
                    isWallSliding = true;
                    float clampedVy = Math.max(vel.y, -1.8f);
                    playerBody.setLinearVelocity(targetVel * 0.65f, clampedVy);
                } else {
                    isWallSliding = false;
                    playerBody.setLinearVelocity(targetVel, vel.y);
                }

                checkLedgeGrab(delta, kUp, padUp, isPaused || inCutscene(), movingLeft, movingRight);

                if (isGrounded) {
                    coyoteTimer = COYOTE_TIME_MAX;
                    if (jumpBufferTimer > 0f) {
                        jumpBufferTimer = 0f;
                        triggerJump(movingLeft, movingRight);
                    }
                } else {
                    coyoteTimer = Math.max(0, coyoteTimer - delta);
                    if (jumpBufferTimer > 0f) jumpBufferTimer = Math.max(0, jumpBufferTimer - delta);
                }
            }
        }

        updateUltimate(delta);
        updateGroundMobs(delta);

        targetToAssassinate = null;
        targetFighterToAssassinate = null;
        targetRemoteToAssassinate = false;
        targetAssassinatePeerId = null;
        if (!playerDead && !isMatchEnded && !isPaused && !inCutscene()) {
            float px = playerBody.getPosition().x;
            float py = playerBody.getPosition().y;

            // 1. Quái đất (GroundMob)
            for (int mi = 0; mi < activeMonsters.size; mi++) {
                GroundMob m = activeMonsters.get(mi);
                if (m.isDead || m.body == null || !"PATROL".equals(m.state)) continue;
                float dist = Math.abs(m.body.getPosition().x - px);
                float yDist = Math.abs(m.body.getPosition().y - py);
                boolean behindGuard = (m.facing == 1 && px < m.body.getPosition().x) ||
                                      (m.facing == -1 && px > m.body.getPosition().x);
                boolean isStealthRange = isCrouching ? (dist < 1.7f && yDist < 1.3f) : (dist < 1.4f && yDist < 1.0f);
                if (isStealthRange && behindGuard) {
                    targetToAssassinate = m;
                    break;
                }
            }

            // 2. Võ sĩ địch trong 5v5 (Fighter5v5)
            if (targetToAssassinate == null && currentGameMode == GameMode.BATTLE_5V5) {
                int pTeam = (game != null) ? game.selectedTeam : 1;
                for (int fi = 0; fi < fighters5v5.size; fi++) {
                    Fighter5v5 f = fighters5v5.get(fi);
                    if (f.isDead || f.body == null || f.team == pTeam) continue;
                    float dist = Math.abs(f.body.getPosition().x - px);
                    float yDist = Math.abs(f.body.getPosition().y - py);
                    boolean behindEnemy = (f.facing == 1 && px < f.body.getPosition().x) ||
                                          (f.facing == -1 && px > f.body.getPosition().x);
                    boolean inRange = (dist < 1.6f && yDist < 1.2f);
                    if (inRange && behindEnemy) {
                        targetFighterToAssassinate = f;
                        break;
                    }
                }
            }

            // 3. Đối thủ trực tuyến (PVP hoặc khác phe trong 5v5)
            if (targetToAssassinate == null && targetFighterToAssassinate == null && !remoteDead && remoteX != -999f) {
                int pTeam = (game != null) ? game.selectedTeam : 1;
                boolean isEnemyRemote = (currentGameMode == GameMode.PVP)
                    || (currentGameMode == GameMode.BATTLE_5V5 && remoteTeam != pTeam
                        && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush));
                if (isEnemyRemote && !remoteStealth) {
                    float dist = Math.abs(remoteX - px);
                    float yDist = Math.abs(remoteY - py);
                    boolean behindRemote = (remoteFacing == 1 && px < remoteX) ||
                                           (remoteFacing == -1 && px > remoteX);
                    if (dist < 1.6f && yDist < 1.2f && behindRemote) {
                        targetRemoteToAssassinate = true;
                    }
                }
            }
            if (targetToAssassinate == null && targetFighterToAssassinate == null && !targetRemoteToAssassinate
                && currentGameMode == GameMode.BATTLE_5V5) {
                int pTeam = (game != null) ? game.selectedTeam : 1;
                for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                    RemotePeer5v5 ep = extraPeers5v5.get(epi);
                    if (ep.team != pTeam && !ep.dead && ep.x != -999f && !ep.stealth
                        && isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) {
                        float dist = Math.abs(ep.x - px);
                        float yDist = Math.abs(ep.y - py);
                        boolean behindEp = (ep.facing == 1 && px < ep.x) || (ep.facing == -1 && px > ep.x);
                        if (dist < 1.6f && yDist < 1.2f && behindEp) {
                            targetAssassinatePeerId = ep.id;
                            break;
                        }
                    }
                }
            }
        }
        boolean hasAssassinateTarget = (targetToAssassinate != null || targetFighterToAssassinate != null
            || targetRemoteToAssassinate || targetAssassinatePeerId != null);
        if (btnAssassinate != null) {
            btnAssassinate.setVisible(hasAssassinateTarget && !isAssassinating && !isPaused && !inCutscene());
        }
        if (!chatTyping && !modalConsumed && !isPaused && !isSettingsModalOpen && !isShopOpen && !inCutscene()
            && hasAssassinateTarget && !isAssassinating && (Gdx.input.isKeyJustPressed(Input.Keys.F) || Gdx.input.isKeyJustPressed(Input.Keys.Q))) {
            triggerAssassinate();
        }

        // Shop: B key
        if (!chatTyping && !modalConsumed && !isPaused && !isSettingsModalOpen && !storyDialogueActive && !isMatchEnded
            && Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            toggleShop();
        }

        // Health Potion: H key
        if (!chatTyping && !modalConsumed && !isPaused && !isSettingsModalOpen && !storyDialogueActive && !isMatchEnded
            && Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            useHealthPotion();
        }

        // Pit death
        if (bossIntroActive) {
            // Trong suốt thời gian Cutscene giới thiệu Boss, bảo vệ người chơi tuyệt đối không bị rơi vực
            if (playerBody != null && playerBody.getPosition().y < 0.65f) {
                playerBody.setTransform(Math.max(playerBody.getPosition().x, mapWidth - 36.0f), 1.2f, 0);
                playerBody.setLinearVelocity(0, 0);
            }
        } else if (playerBody.getPosition().y < -1.0f && !playerDead && !isMatchEnded) {
            playerDeathX = playerBody.getPosition().x;
            playerDeathY = 0.65f;
            playerDeathBlinkTimer = 0.85f;
            playerDead = true;
            playerHp = 0;
            playerHpBar.setHp(0);
            lblPlayerHp.setText("Máu: 0 HP");
            SoundManager.getInstance().playHurt();
            if (currentGameMode == GameMode.BATTLE_5V5) {
                closeShop();
                playerRespawnTimer5v5 = 5.0f;
                int pTeam = (game != null) ? game.selectedTeam : 1;
                resetTraversalState();
                playerBody.setTransform(pTeam == 1 ? 4.5f : 55.5f, 1.2f, 0);
                playerBody.setLinearVelocity(0, 0);
                actionLog.setText("BẠN ĐÃ RƠI XUỐNG VỰC! HỒI SINH SAU 5 GIÂY...");
                String pitVictim = (game != null && game.playerName != null ? game.playerName : "Chiến Thần");
                on5v5Kill("Vực Thẳm", 0, pitVictim, pTeam);
                broadcast5v5Kill("Vực Thẳm", 0, pitVictim, pTeam);
                sendSyncPacket("IDLE");
            } else {
                actionLog.setText("BẠN ĐÃ RƠI XUỐNG VỰC!");
                showGameOverDialog(false);
            }
        }

        // Bước vật lý cố định 1/60s độc lập FPS (đóng băng khi đang đọc thoại / mở tiệm đồ chơi đơn).
        if (!inCutscene()) {
            physicsAccumulator += Math.min(delta, 0.25f);
            int steps = 0;
            while (physicsAccumulator >= PHYSICS_STEP && steps < 5) {
                world.step(PHYSICS_STEP, 6, 2);
                physicsAccumulator -= PHYSICS_STEP;
                steps++;
            }
            if (steps == 5) physicsAccumulator = 0f;
            if (pendingEagleGroundAbort) {
                pendingEagleGroundAbort = false;
                if (isEagleStriking) {
                    boolean hitAny = applyEagleImpactDamage(2.0f);
                    stopEagleStrike(hitAny);
                }
            }
        }
        updateCamera(delta);

        // Animation state
        stateTime += delta;
        TextureRegion currentFrame;
        String animStateName = "IDLE";
        boolean isRunning = (movingLeft || movingRight) && !playerDead
            && Math.abs(playerBody.getLinearVelocity().x) > 0.12f;

        // Chọn hoạt ảnh theo nhân vật (P1: Sát Thủ Trắng, P2: Sát Thủ Đen) và vũ khí đang cầm
        boolean isP2Char = "P2".equals(selectedChar);
        activeIdleAnim = (isP2Char && darkIdleAnim != null) ? darkIdleAnim : whiteIdleAnim;
        TextureRegion activeStaticFrame = getCharacterWeaponIdleFrame();
        TextureRegion activeJumpUp = isP2Char ? ((denJumpUpFrame != null) ? denJumpUpFrame : activeStaticFrame)
            : ((whiteJumpUpFrame != null) ? whiteJumpUpFrame : ((jumpUpFrame != null && jumpUpFrame != staticIdleFrame) ? jumpUpFrame : activeStaticFrame));
        TextureRegion activeJumpDown = isP2Char ? ((denJumpDownFrame != null) ? denJumpDownFrame : activeStaticFrame)
            : ((whiteJumpDownFrame != null) ? whiteJumpDownFrame : ((jumpDownFrame != null && jumpDownFrame != staticIdleFrame) ? jumpDownFrame : activeStaticFrame));
        Animation<TextureRegion> activeRunAnim = isP2Char ? ((darkRunAnim != null) ? darkRunAnim : playerRunAnim)
            : ((whiteRunAnim != null) ? whiteRunAnim : playerRunAnim);
        TextureRegion activeDashFrame = isP2Char ? ((denDashFrame != null) ? denDashFrame : activeStaticFrame)
            : ((dashFrame != null && dashFrame != staticIdleFrame) ? dashFrame : activeStaticFrame);

        if (isEagleStriking) {
            currentFrame = activeJumpDown;
            animStateName = "EAGLE_STRIKE";
        } else if (isSwinging || isClimbing || isGrabbedByPirate) {
            currentFrame = activeJumpUp;
            animStateName = "CLIMB";
        } else if (isDashing) {
            currentFrame = activeDashFrame;
            animStateName = "DASH";
        } else if (attackCombo > 0) {
            Animation<TextureRegion> curAtk = getCurrentPlayerAttackAnim();
            currentFrame = (curAtk != null) ? curAtk.getKeyFrame(attackTime, false) : activeStaticFrame;
            animStateName = (attackCombo == 1) ? "HIT1" : (attackCombo == 2 ? "HIT2" : "HIT3");
        } else if (!isGrounded && !playerDead) {
            if (playerBody.getLinearVelocity().y > 0.1f) {
                currentFrame = activeJumpUp;
                animStateName = "JUMP_UP";
            } else {
                currentFrame = activeJumpDown;
                animStateName = "JUMP_DOWN";
            }
        } else if (isCrouching) {
            if (movingLeft || movingRight) {
                currentFrame = isP2Char ? ((denRollFrame != null) ? denRollFrame : activeStaticFrame)
                    : ((crouchFrame != null && crouchFrame != staticIdleFrame) ? crouchFrame : activeStaticFrame);
                animStateName = "ROLL";
            } else {
                currentFrame = isP2Char ? activeStaticFrame
                    : ((crouchFrame != null && crouchFrame != staticIdleFrame) ? crouchFrame : activeStaticFrame);
                animStateName = "CROUCH";
            }
        } else if (isRunning && activeRunAnim != null) {
            currentFrame = activeRunAnim.getKeyFrame(stateTime, true); animStateName = "RUN";
        } else {
            // Đứng yên: khi cầm vũ khí có ảnh đứng riêng (P1: súng/cung/rìu/song đao, P2: súng/rìu/cung/song đao) thì hiển thị đúng ảnh đứng đó
            boolean useWeaponStance = (!isP2Char && currentWeapon != WeaponType.SWORD)
                || (isP2Char && ((currentWeapon == WeaponType.SHOTGUN && denSungDungFrame != null)
                    || (currentWeapon == WeaponType.AXE && denRiuDungFrame != null)
                    || (currentWeapon == WeaponType.BOW && denCungDungFrame != null)
                    || (currentWeapon == WeaponType.DUAL_DAGGERS && denSongDaoDungFrame != null)));
            if (useWeaponStance) {
                currentFrame = activeStaticFrame;
            } else {
                currentFrame = (activeIdleAnim != null) ? activeIdleAnim.getKeyFrame(stateTime, true) : activeStaticFrame;
            }
            animStateName = isRunning ? "RUN" : "IDLE";
        }

        // Network sync (max 20 packets/s)
        if (currentGameMode != GameMode.SOLO) {
            networkTimer += delta;
            forceSyncTimer += delta;
            if (networkTimer >= 0.05f) {
                boolean moved = Math.abs(playerBody.getPosition().x - lastSentX) > 0.02f
                    || Math.abs(playerBody.getPosition().y - lastSentY) > 0.02f;
                boolean stateChanged = !animStateName.equals(lastSentAnim)
                    || playerFacing != lastSentFacing
                    || currentWeapon != lastSentWeapon
                    || playerDead != lastSentDead;

                if (moved || stateChanged || forceSyncTimer >= 0.5f) {
                    networkTimer = 0f; forceSyncTimer = 0f;
                    lastSentX = playerBody.getPosition().x;
                    lastSentY = playerBody.getPosition().y;
                    lastSentAnim = animStateName;
                    lastSentFacing = playerFacing;
                    lastSentWeapon = currentWeapon;
                    lastSentDead = playerDead;
                    sendSyncPacket(animStateName);
                }
            }
        }

        // === DRAW ===
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawBackgroundParallax();

        // Tối ưu hóa Frustum Culling: Tính toán biên hiển thị Camera để loại bỏ vẽ các vật thể ngoài màn hình
        float halfCamW = (viewport.getWorldWidth() * camera.zoom) / 2f;
        float camMinX = camera.position.x - halfCamW - 2.0f;
        float camMaxX = camera.position.x + halfCamW + 2.0f;

        for (int i = 0; i < laddersList.size; i++) {
            Ladder l = laddersList.get(i);
            if (l.x + l.width < camMinX || l.x > camMaxX) continue;
            float segH = 0.9f;
            for (float curY = l.y; curY < l.y + l.height; curY += segH) {
                float drawH = Math.min(segH, l.y + l.height - curY);
                batch.draw(ladderTexture, l.x, curY, l.width, drawH);
            }
        }

        if (currentGameMode != GameMode.PVP && currentGameMode != GameMode.BATTLE_5V5) {
            if (cannonPos.x >= camMinX && cannonPos.x <= camMaxX) {
                batch.draw(cannonTexture, cannonPos.x - 0.4f, cannonPos.y - 0.15f, 0.8f, 0.55f);
            }
            if (finishLineX >= camMinX && finishLineX <= camMaxX) {
                batch.draw(finishFlagTexture, finishLineX - 0.2f, 1.25f, 0.7f, 1.3f);
            }
        }

        for (int i = 0; i < swingRopes.size; i++) {
            SwingRope r = swingRopes.get(i);
            if (r.anchor.x < camMinX - 3.5f || r.anchor.x > camMaxX + 3.5f) continue;
            r.draw(batch, ropeRegion, (weaponBoxRegion != null ? weaponBoxRegion : ropeRegion));
        }

        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            if (p.x + p.width < camMinX || p.x > camMaxX) continue;

            // Chân dầm console gỗ đinh sắt chịu lực đỡ các bục cao lơ lửng
            if (!p.isGround && p.width >= 1.2f && platformConsoleTex != null) {
                batch.setColor(Color.WHITE);
                batch.draw(platformConsoleTex, p.x + 0.15f, p.y - 0.50f, 0.50f, 0.50f);
                batch.draw(platformConsoleTex, p.x + p.width - 0.65f, p.y - 0.50f, 0.50f, 0.50f, 0, 0, 32, 32, true, false);
            }

            batch.setColor(Color.WHITE);
            int srcW = Math.max(1, (int)(p.width * 32f));
            int srcH = Math.max(1, (int)(p.height * 32f));
            if (p.isGround) {
                batch.draw(groundBrickTexture, p.x, p.y, p.width, p.height, 0, 0, srcW, srcH, false, false);
            } else {
                batch.draw(platformWoodTexture, p.x, p.y, p.width, p.height, 0, 0, srcW, srcH, false, false);
            }

            // Ranh giới mặt trên đứng được cực kỳ dễ nhận biết (3D Crease + Highlight Rim + Top Edge)
            batch.setColor(0.02f, 0.02f, 0.04f, 0.85f);
            batch.draw(pixel, p.x, p.y + p.height - 0.12f, p.width, 0.04f);

            Color topHi = (currentMapThemeIndex == 2)
                ? new Color(0.55f, 0.92f, 1.0f, 0.95f)
                : ((currentMapThemeIndex == 3)
                    ? new Color(0.92f, 0.96f, 1.0f, 0.95f)
                    : new Color(1.0f, 0.88f, 0.55f, 0.95f));
            batch.setColor(topHi);
            batch.draw(pixel, p.x, p.y + p.height - 0.08f, p.width, 0.08f);

            batch.setColor(Color.WHITE);
            batch.draw(pixel, p.x, p.y + p.height - 0.02f, p.width, 0.02f);
        }
        if (wallTexture != null) {
            for (int i = 0; i < wallsList.size; i++) {
                MapWall w = wallsList.get(i);
                if (w.x + w.width < camMinX || w.x > camMaxX) continue;
                batch.setColor(Color.WHITE);
                int srcW = Math.max(1, (int)(w.width * 32f));
                int srcH = Math.max(1, (int)(w.height * 32f));
                batch.draw(wallTexture, w.x, w.y, w.width, w.height, 0, 0, srcW, srcH, false, false);

                Color wallTopHi = (currentMapThemeIndex == 2)
                    ? new Color(0.50f, 0.85f, 0.98f, 0.90f)
                    : ((currentMapThemeIndex == 3)
                        ? new Color(0.88f, 0.94f, 1.0f, 0.90f)
                        : new Color(0.98f, 0.85f, 0.55f, 0.90f));
                batch.setColor(wallTopHi);
                batch.draw(pixel, w.x, w.y + w.height - 0.08f, w.width, 0.08f);
                batch.setColor(Color.WHITE);
            }
        }

        // Collapsing Platforms
        for (int i = 0; i < collapsingPlatforms.size; i++) {
            CollapsingPlatform cp = collapsingPlatforms.get(i);
            if (cp.collapsed) continue;
            if (cp.x + cp.width < camMinX || cp.x > camMaxX) continue;
            if (cp.steppedOn) {
                batch.setColor(1.0f, 0.45f, 0.4f, 0.95f);
            } else {
                batch.setColor(0.72f, 0.52f, 0.35f, 1f);
            }
            batch.draw(platformWoodTexture, cp.x, cp.y - cp.height + cp.shakeOffset, cp.width, cp.height);
            batch.setColor(Color.WHITE);
        }

        // Safe Checkpoints
        for (int i = 0; i < checkpoints.size; i++) {
            MapCheckpoint cp = checkpoints.get(i);
            if (cp.x < camMinX - 2.0f || cp.x > camMaxX + 2.0f) continue;
            float poleX = cp.x;
            float poleY = cp.y;
            batch.setColor(0.4f, 0.3f, 0.2f, 1f);
            batch.draw(pixel, poleX - 0.04f, poleY, 0.08f, 1.6f);
            if (cp.activated) {
                batch.setColor(0.2f, 0.9f, 0.3f, 0.9f);
                batch.draw(pixel, poleX + 0.04f, poleY + 1.0f, 0.6f, 0.45f);
            } else {
                float fl = 0.7f + MathUtils.sin(stateTime * 4f) * 0.3f;
                batch.setColor(1.0f, 0.85f, 0.2f, fl);
                batch.draw(pixel, poleX + 0.04f, poleY + 1.0f, 0.6f, 0.45f);
            }
            batch.setColor(Color.WHITE);
        }

        // Map 2 Water Hazard Layer
        if ((story != null && story.chapter == 2) || (currentGameMode != GameMode.SOLO && game != null && game.selectedOnlineMap == 2)) {
            float waterWave = MathUtils.sin(stateTime * 2.5f) * 0.05f;
            batch.setColor(0.05f, 0.22f, 0.45f, 0.78f);
            batch.draw(pixel, camMinX, 0, camMaxX - camMinX, 0.55f + waterWave);
            batch.setColor(0.6f, 0.85f, 1.0f, 0.5f);
            batch.draw(pixel, camMinX, 0.52f + waterWave, camMaxX - camMinX, 0.06f);
            batch.setColor(Color.WHITE);
        }

        for (int i = 0; i < hpPickupPositions.size; i++) {
            if (hpPickupActives.get(i)) {
                Vector2 hPos = hpPickupPositions.get(i);
                if (hPos.x < camMinX - 1.0f || hPos.x > camMaxX + 1.0f) continue;
                float floatY = hPos.y + MathUtils.sin(stateTime * 3.0f + i) * 0.08f;
                float pulse = 0.88f + MathUtils.sin(stateTime * 4.0f + i) * 0.12f;
                batch.setColor(0.4f, 1.0f, 0.6f, pulse * 0.40f);
                batch.draw(hpOrbTexture, hPos.x - 0.32f, floatY - 0.04f, 0.64f, 0.64f);
                batch.setColor(Color.WHITE);
                batch.draw(hpOrbTexture, hPos.x - 0.25f, floatY, 0.50f, 0.50f);
            }
        }

        // PVP Ultimate Orbs
        if (currentGameMode == GameMode.PVP && ultimateOrbTexture != null) {
            for (int i = 0; i < pvpUltOrbs.size; i++) {
                UltOrb orb = pvpUltOrbs.get(i);
                if (orb.active) {
                    if (orb.x < camMinX - 1.0f || orb.x > camMaxX + 1.0f) continue;
                    float floatY = orb.y + MathUtils.sin(orb.pulseTime * 4.0f) * 0.10f;
                    float glow = 0.85f + MathUtils.sin(orb.pulseTime * 6.0f) * 0.15f;
                    batch.setColor(1f, 0.90f, 0.25f, glow * 0.45f);
                    batch.draw(ultimateOrbTexture, orb.x - 0.34f, floatY - 0.34f, 0.68f, 0.68f);
                    batch.setColor(1f, 0.90f, 0.25f, glow);
                    batch.draw(ultimateOrbTexture, orb.x - 0.28f, floatY - 0.28f, 0.56f, 0.56f);
                    batch.setColor(Color.WHITE);
                }
            }
        }

        // Jump Pads (Bệ phóng nảy cao Parkour — 5v5 & CO-OP)
        if (!jumpPads.isEmpty() && jumpPadTexture != null) {
            for (int i = 0; i < jumpPads.size; i++) {
                JumpPad jp = jumpPads.get(i);
                if (jp.x + jp.width < camMinX || jp.x - jp.width > camMaxX) continue;
                batch.draw(jumpPadTexture, jp.x - jp.width / 2f, jp.y, jp.width, jp.height);
            }
        }

        // Contested Power-up Runes (5v5 & CO-OP)
        if (!powerUpRunes.isEmpty()) {
            for (int i = 0; i < powerUpRunes.size; i++) {
                PowerUpRune rune = powerUpRunes.get(i);
                if (rune.active) {
                    if (rune.x < camMinX - 1.5f || rune.x > camMaxX + 1.5f) continue;
                    Texture rTex = null;
                    switch (rune.type) {
                        case RANDOM_ULTIMATE: rTex = runeUltTex; break;
                        case BERSERK_DAMAGE:  rTex = runeBerserkTex; break;
                        case DIVINE_SHIELD:   rTex = runeShieldTex; break;
                        case WIND_STRIDE:     rTex = runeWindTex; break;
                        case MEGA_HEAL:       rTex = runeHealTex; break;
                    }
                    if (rTex != null) {
                        float floatY = rune.y + MathUtils.sin(rune.pulseTime * 3.5f) * 0.10f;
                        float pulse = 0.85f + MathUtils.sin(rune.pulseTime * 5.0f) * 0.15f;
                        Color rc = rune.type.getColor();
                        batch.setColor(rc.r, rc.g, rc.b, 0.45f * pulse);
                        batch.draw(rTex, rune.x - 0.32f, floatY - 0.04f, 0.64f, 0.64f);
                        batch.setColor(Color.WHITE);
                        batch.draw(rTex, rune.x - 0.25f, floatY, 0.50f, 0.50f);
                    }
                }
            }
        }

        // 5V5 Fountain Healing Sanctum Auras
        if (currentGameMode == GameMode.BATTLE_5V5 && fountainAuraTexture != null) {
            // Blue Base (Phe Anh Sang)
            batch.setColor(0.20f, 0.75f, 1.0f, 0.35f + MathUtils.sin(stateTime * 2.5f) * 0.12f);
            batch.draw(fountainAuraTexture, 2.0f, 0.65f, 5.0f, 2.4f);
            // Red Base (Phe Bong Toi)
            batch.setColor(1.0f, 0.30f, 0.30f, 0.35f + MathUtils.sin(stateTime * 2.5f) * 0.12f);
            batch.draw(fountainAuraTexture, 53.0f, 0.65f, 5.0f, 2.4f);
            batch.setColor(Color.WHITE);
        }

        // Tactical Ambush Bushes (5v5 & CO-OP)
        if (!tacticalBushes.isEmpty() && bushTexture != null) {
            for (int i = 0; i < tacticalBushes.size; i++) {
                TacticalBush bush = tacticalBushes.get(i);
                if (bush.x + bush.width < camMinX || bush.x > camMaxX) continue;
                float sway = MathUtils.sin(stateTime * 2.2f + bush.x) * 0.04f;
                batch.setColor(1.0f, 1.0f, 1.0f, 0.92f);
                batch.draw(bushTexture, bush.x - 0.1f, bush.y, bush.width + 0.2f, bush.height + sway);
            }
            batch.setColor(Color.WHITE);
        }

        // 5V5 Outpost Defense Turrets
        if (currentGameMode == GameMode.BATTLE_5V5 && turretTexture != null) {
            for (int i = 0; i < outpostTurrets.size; i++) {
                OutpostTurret t = outpostTurrets.get(i);
                if (t.x < camMinX - 2.0f || t.x > camMaxX + 2.0f) continue;
                if (t.isDestroyed) {
                    batch.setColor(0.35f, 0.35f, 0.40f, 0.60f);
                    batch.draw(turretTexture, t.x - 0.45f, t.y, 0.90f, 0.65f);
                    batch.setColor(Color.WHITE);
                    continue;
                }
                Color tColor = (t.hitTimer > 0) ? COLOR_HIT_FLASH : (t.team == 1 ? COLOR_TURRET_BLUE : COLOR_TURRET_RED);
                batch.setColor(tColor);
                batch.draw(turretTexture, t.x - 0.55f, t.y, 1.10f, 1.95f);
                batch.setColor(Color.WHITE);

                // Mini HP bar for turret
                float barW = 1.25f, barH = 0.11f;
                float hpPct = MathUtils.clamp((float)t.hp / t.maxHp, 0f, 1f);
                float barY = t.y + 2.10f;
                batch.setColor(0.10f, 0.10f, 0.14f, 0.85f);
                batch.draw(pixel, t.x - barW / 2f - 0.02f, barY - 0.02f, barW + 0.04f, barH + 0.04f);
                batch.setColor(0.40f, 0.05f, 0.05f, 0.90f);
                batch.draw(pixel, t.x - barW / 2f, barY, barW, barH);
                batch.setColor(t.team == 1 ? COLOR_TURRET_BAR_BLUE : COLOR_TURRET_BAR_RED);
                batch.draw(pixel, t.x - barW / 2f, barY, barW * hpPct, barH);
                batch.setColor(Color.WHITE);
            }
        }

        // 5V5 Ancient Titan Boss (Trùm Thần Thú Bạo Chúa)
        if (currentGameMode == GameMode.BATTLE_5V5 && ancientTitan != null && ancientTitan.isAlive && titanBossTexture != null) {
            Color titColor = (ancientTitan.hitTimer > 0) ? COLOR_HIT_FLASH : Color.WHITE;
            batch.setColor(titColor);
            float tw = 1.75f, th = 1.95f;
            batch.draw(titanBossTexture, ancientTitan.x - tw / 2f, ancientTitan.y, tw, th);
            batch.setColor(Color.WHITE);

            // Boss HP bar
            float barW = 2.40f, barH = 0.16f;
            float hpPct = MathUtils.clamp((float)ancientTitan.hp / ancientTitan.maxHp, 0f, 1f);
            float barY = ancientTitan.y + th + 0.30f;
            batch.setColor(0.08f, 0.08f, 0.12f, 0.90f);
            batch.draw(pixel, ancientTitan.x - barW / 2f - 0.03f, barY - 0.03f, barW + 0.06f, barH + 0.06f);
            batch.setColor(0.40f, 0.05f, 0.05f, 0.90f);
            batch.draw(pixel, ancientTitan.x - barW / 2f, barY, barW, barH);
            batch.setColor(Color.GOLD);
            batch.draw(pixel, ancientTitan.x - barW / 2f, barY, barW * hpPct, barH);
            batch.setColor(Color.WHITE);
        }

        for (int i = 0; i < weaponPickupPositions.size; i++) {
            if (weaponPickupActives.get(i)) {
                Vector2 wPos = weaponPickupPositions.get(i);
                float floatY = wPos.y + MathUtils.sin(stateTime * 2.5f + i) * 0.06f;
                float glow = 0.85f + MathUtils.sin(stateTime * 3.5f + i) * 0.15f;
                batch.setColor(1f, 0.85f, 0.3f, glow * 0.40f);
                batch.draw(weaponBoxTexture, wPos.x - 0.28f, floatY - 0.04f, 0.56f, 0.56f);
                batch.setColor(Color.WHITE);
                batch.draw(weaponBoxTexture, wPos.x - 0.22f, floatY, 0.44f, 0.44f);
            }
        }

        if (hasSkyPirate && (pirateAlive || pirateState == PirateState.FALLING || pirateState == PirateState.DEAD)) {
            boolean drawPirate = true;
            TextureRegion pFrame;

            if (pirateState == PirateState.DEAD || pirateState == PirateState.FALLING) {
                if (pirateDeathTimer < 0.35f && (int)(pirateDeathTimer * 28) % 2 == 0) {
                    drawPirate = false; // Fast blinking before disappearing
                }
                if (pirateState == PirateState.FALLING || pirateY > 1.30f) {
                    pFrame = (khongTacRoiFrame != null) ? khongTacRoiFrame : staticIdleFrame;
                } else {
                    pFrame = (khongTacChetFrame != null) ? khongTacChetFrame : staticIdleFrame;
                }
            } else if (pirateState == PirateState.DIVE) {
                pFrame = (khongTacLaoFrame != null) ? khongTacLaoFrame : staticIdleFrame;
            } else if (pirateState == PirateState.CARRY || pirateState == PirateState.PATROL || pirateState == PirateState.ALERT) {
                pFrame = (khongTacFlyAnim != null) ? khongTacFlyAnim.getKeyFrame(stateTime, true) : staticIdleFrame;
            } else {
                pFrame = (khongTacFlyAnim != null) ? khongTacFlyAnim.getKeyFrame(stateTime, true) : staticIdleFrame;
            }

            if (drawPirate) {
                String pAnimState = (pirateState == PirateState.FALLING || pirateState == PirateState.DEAD) ? "DEAD" : "RUN";
                drawCharacterWithSpec(pFrame, pAnimState, pirateX, pirateY, pirateFacing,
                    Color.WHITE, 1.15f, 0.55f, false);
            }
        }

        if (hasImperial && (imperialAlive || "FALLING".equals(imperialState) || "DEAD".equals(imperialState))) {
            boolean drawImperial = true;
            TextureRegion iFrame;

            if ("DEAD".equals(imperialState) || "FALLING".equals(imperialState)) {
                if (imperialDeathTimer < 0.35f && (int)(imperialDeathTimer * 28) % 2 == 0) {
                    drawImperial = false; // Fast blinking before disappearing
                }
                if ("FALLING".equals(imperialState) || imperialY > 1.30f) {
                    iFrame = (trieuDinhRoiFrame != null) ? trieuDinhRoiFrame : staticIdleFrame;
                } else {
                    iFrame = (trieuDinhChetFrame != null) ? trieuDinhChetFrame : staticIdleFrame;
                }
            } else if ("ATTACK_PREP".equals(imperialState)) {
                iFrame = (trieuDinhBan1Frame != null) ? trieuDinhBan1Frame : staticIdleFrame;
            } else if ("ATTACK_FIRE".equals(imperialState)) {
                iFrame = (trieuDinhBan2Frame != null) ? trieuDinhBan2Frame : staticIdleFrame;
            } else {
                iFrame = (trieuDinhFlyAnim != null) ? trieuDinhFlyAnim.getKeyFrame(stateTime, true) : staticIdleFrame;
            }

            if (drawImperial) {
                String impAnimState = ("DEAD".equals(imperialState) || "FALLING".equals(imperialState)) ? "DEAD" : "IDLE";
                drawCharacterWithSpec(iFrame, impAnimState, imperialX, imperialY, imperialFacing,
                    Color.WHITE, 1.28f, 0.55f, false);
            }
        }

        if (boyState != BoyState.INACTIVE && boyBody != null) {
            float bx = boyBody.getPosition().x, by = boyBody.getPosition().y;

            if (boyHitTimer > 0) boyHitTimer -= delta;

            if (boyState == BoyState.FALLING) {
                boyDeathTimer -= delta;
                boyBody.setLinearVelocity(0, boyBody.getLinearVelocity().y);
                if (boyDeathTimer <= 0) {
                    boyState = BoyState.DEAD;
                    world.destroyBody(boyBody);
                    boyBody = null;
                }
            }

            if (boyBody != null) {
                boolean drawBoy = true;
                TextureRegion bFrame;
                boolean boyMovingOrClimbing = boyIsClimbing
                    || Math.abs(boyBody.getLinearVelocity().x) > 0.25f
                    || Math.abs(boyBody.getLinearVelocity().y) > 0.65f;

                if (boyState == BoyState.FALLING || boyState == BoyState.DEAD) {
                    if (boyDeathTimer < 0.30f && (int)(boyDeathTimer * 28) % 2 == 0) {
                        drawBoy = false;
                    }
                    if (boyDeathTimer > 0.40f) {
                        bFrame = (boyNgaFrame != null) ? boyNgaFrame : (boyIdleFrame != null ? boyIdleFrame : staticIdleFrame);
                    } else {
                        bFrame = (boyChetFrame != null) ? boyChetFrame : (boyIdleFrame != null ? boyIdleFrame : staticIdleFrame);
                    }
                } else if (boyHitTimer > 0) {
                    bFrame = (boyHitFrame != null) ? boyHitFrame : (boyIdleFrame != null ? boyIdleFrame : staticIdleFrame);
                } else if (boyAttackCooldown > 0.85f) {
                    bFrame = (boyAtkFrame != null) ? boyAtkFrame : (boyIdleFrame != null ? boyIdleFrame : staticIdleFrame);
                } else if (boyMovingOrClimbing) {
                    bFrame = (boyWalkAnim != null) ? boyWalkAnim.getKeyFrame(stateTime, true) : (boyIdleFrame != null ? boyIdleFrame : staticIdleFrame);
                } else {
                    bFrame = (boyIdleFrame != null) ? boyIdleFrame : staticIdleFrame;
                }

                if (drawBoy) {
                    String boyAnimState = (boyState == BoyState.FALLING || boyState == BoyState.DEAD) ? "DEAD"
                        : (boyHitTimer > 0 ? "HIT" : (boyMovingOrClimbing ? "RUN" : "IDLE"));
                    Color boyTint = (boyHitTimer > 0) ? COLOR_HIT_FLASH : Color.WHITE;
                    drawCharacterWithSpec(bFrame, boyAnimState, bx, by, boyFacing, boyTint, 0.82f, 0.30f, false);

                    // Boy HP bar (only when alive)
                    if (boyState != BoyState.FALLING && boyState != BoyState.DEAD) {
                        float barW = 0.55f, barH = 0.07f;
                        float barY = by + 0.60f;
                        float hpPercent = MathUtils.clamp((float)boyHp / boyMaxHp, 0f, 1f);
                        batch.setColor(0.10f, 0.10f, 0.14f, 0.85f);
                        batch.draw(pixel, bx - barW / 2f - 0.02f, barY - 0.015f, barW + 0.04f, barH + 0.03f);
                        batch.setColor(0.35f, 0.05f, 0.05f, 0.90f);
                        batch.draw(pixel, bx - barW / 2f, barY, barW, barH);
                        batch.setColor(0.20f, 0.90f, 0.35f, 1f);
                        batch.draw(pixel, bx - barW / 2f, barY, barW * hpPercent, barH);
                        batch.setColor(1f, 1f, 1f, 0.35f);
                        batch.draw(pixel, bx - barW / 2f, barY + barH * 0.55f, barW * hpPercent, barH * 0.45f);
                        batch.setColor(Color.WHITE);
                    }
                }
            }
        }

        if (hasSage)
            drawCharacter(staticIdleFrame, "IDLE", sageX, sageY, -1,
                COLOR_SAGE, 1.05f, false);

        // Hắc Ám Huyết Trận (P2 Skill 3): Xoáy sâu tím đỏ dưới mặt đất
        if (p2AbyssTimer > 0f) {
            float pulse = 0.80f + 0.20f * MathUtils.sin(stateTime * 9f);
            if (fountainAuraTexture != null) {
                batch.setColor(0.65f, 0.08f, 0.85f, 0.55f * pulse);
                batch.draw(fountainAuraTexture, p2AbyssX - 2.3f, p2AbyssY - 0.42f, 4.6f, 1.45f);
            }
            if (lockOnRegion != null) {
                float sz = 2.4f + 0.22f * MathUtils.sin(stateTime * 7f);
                batch.setColor(0.95f, 0.18f, 0.65f, 0.65f * pulse);
                batch.draw(lockOnRegion, p2AbyssX - sz / 2f, p2AbyssY + 0.15f - sz / 2f,
                    sz / 2f, sz / 2f, sz, sz, 1f, 0.45f, -stateTime * 220f);
            }
            batch.setColor(Color.WHITE);
        }

        // Ảnh Phân Thân (P2 Skill 2): Phân thân bóng tối chuẩn bị phát nổ
        if (p2CloneActive) {
            float cPulse = 0.65f + 0.35f * MathUtils.sin(stateTime * 18f);
            if (hpOrbTexture != null) {
                batch.setColor(0.75f, 0.15f, 0.95f, 0.45f * cPulse);
                batch.draw(hpOrbTexture, p2CloneX - 0.65f, p2CloneY - 0.35f, 1.30f, 1.30f);
                batch.setColor(Color.WHITE);
            }
            tempColor.set(0.78f, 0.22f, 1.0f, 0.82f * cPulse);
            TextureRegion cloneFrame = (denDung1Region != null) ? denDung1Region : currentFrame;
            drawCharacter(cloneFrame, "IDLE", p2CloneX, p2CloneY, p2CloneFacing, tempColor, 1.0f, false);
        }

        // Remote Hắc Ám Huyết Trận (CharId=2)
        if (remoteAbyssTimer > 0f) {
            float pulse = 0.80f + 0.20f * MathUtils.sin(stateTime * 9f);
            if (fountainAuraTexture != null) {
                batch.setColor(0.65f, 0.08f, 0.85f, 0.55f * pulse);
                batch.draw(fountainAuraTexture, remoteAbyssX - 2.3f, remoteAbyssY - 0.42f, 4.6f, 1.45f);
            }
            if (lockOnRegion != null) {
                float sz = 2.4f + 0.22f * MathUtils.sin(stateTime * 7f);
                batch.setColor(0.95f, 0.18f, 0.65f, 0.65f * pulse);
                batch.draw(lockOnRegion, remoteAbyssX - sz / 2f, remoteAbyssY + 0.15f - sz / 2f,
                    sz / 2f, sz / 2f, sz, sz, 1f, 0.45f, -stateTime * 220f);
            }
            batch.setColor(Color.WHITE);
        }

        // Remote Ảnh Phân Thân (CharId=2)
        if (remoteCloneActive) {
            float cPulse = 0.65f + 0.35f * MathUtils.sin(stateTime * 18f);
            if (hpOrbTexture != null) {
                batch.setColor(0.75f, 0.15f, 0.95f, 0.45f * cPulse);
                batch.draw(hpOrbTexture, remoteCloneX - 0.65f, remoteCloneY - 0.35f, 1.30f, 1.30f);
                batch.setColor(Color.WHITE);
            }
            tempColor.set(0.78f, 0.22f, 1.0f, 0.82f * cPulse);
            TextureRegion cloneFrame = (denDung1Region != null) ? denDung1Region : staticIdleFrame;
            drawCharacter(cloneFrame, "IDLE", remoteCloneX, remoteCloneY, remoteCloneFacing, tempColor, 1.0f, false);
        }

        for (int i = 0; i < projectiles.size; i++) {
            Projectile p = projectiles.get(i);
            if (p.x < camMinX - 1.2f || p.x > camMaxX + 1.2f) continue;
            if (p.isCannonBall) {
                batch.draw(cannonBallTexture, p.x - 0.25f, p.y - 0.25f, 0.5f, 0.5f);
            } else if (p.isSlingshotPellet) {
                batch.draw(slingshotPelletTexture, p.x - 0.06f, p.y - 0.06f, 0.14f, 0.14f);
            } else if (p.skillVisualType == 1) {
                // Phong Nhẫn (P1 Skill 1): Đại Kiếm Khí Bán Nguyệt hoành tráng
                float rot = (p.vx >= 0f) ? 0f : 180f;
                boolean isAwakened = p1AwakenedTimer > 0f;
                Color auraColor = isAwakened ? new Color(1.0f, 0.88f, 0.25f, 0.95f) : new Color(0.35f, 0.92f, 1.0f, 0.95f);
                batch.setColor(auraColor);
                if (p1CrescentAuraTex != null) {
                    batch.draw(p1CrescentAuraTex, p.x - 0.80f, p.y - 0.70f, 0.80f, 0.70f, 1.60f, 1.40f, 1f, 1f, rot, 0, 0,
                        p1CrescentAuraTex.getWidth(), p1CrescentAuraTex.getHeight(), false, false);
                }
                batch.setColor(Color.WHITE);
                if (sparkTexture != null) {
                    batch.draw(sparkTexture, p.x - 0.20f, p.y - 0.15f, 0.40f, 0.30f);
                }
                batch.setColor(Color.WHITE);
            } else if (p.skillVisualType == 2) {
                // Vạn Kiếm Quy Tông (P1 Skill 3): Cổ Thần Binh Thần Kiếm giáng trần
                float deg = MathUtils.atan2(p.vy, p.vx) * MathUtils.radiansToDegrees;
                if (arrowRegion != null) {
                    batch.setColor(1.0f, 0.85f, 0.25f, 0.45f);
                    batch.draw(arrowRegion, p.x - 0.75f, p.y - 0.15f, 0.75f, 0.15f, 1.50f, 0.30f, 1f, 1f, deg);
                }
                batch.setColor(Color.WHITE);
                if (p1CelestialSwordRegion != null) {
                    batch.draw(p1CelestialSwordRegion, p.x - 0.60f, p.y - 0.16f, 0.60f, 0.16f, 1.20f, 0.32f, 1f, 1f, deg);
                }
                batch.setColor(Color.WHITE);
            } else if (p.skillVisualType == 3) {
                // Huyết Tiêu Liên Hoàn (P2 Skill 1): Phi tiêu kunai ám khí độc xoay tít
                float spinDeg = stateTime * 820f;
                if (p2KunaiRegion != null) {
                    batch.draw(p2KunaiRegion, p.x - 0.26f, p.y - 0.09f, 0.26f, 0.09f, 0.52f, 0.18f, 1f, 1f, spinDeg);
                } else if (sparkTexture != null) {
                    batch.setColor(Color.MAGENTA);
                    batch.draw(sparkTexture, p.x - 0.14f, p.y - 0.14f, 0.28f, 0.28f);
                    batch.setColor(Color.WHITE);
                }
                batch.setColor(Color.WHITE);
            } else if (p.isArrow) {
                float arrowW = 0.48f, arrowH = 0.14f;
                float deg = MathUtils.atan2(p.vy, p.vx) * MathUtils.radiansToDegrees;
                if (p.homingArrow && hpOrbTexture != null) {
                    batch.setColor(0.25f, 1.0f, 0.55f, 0.42f);
                    batch.draw(hpOrbTexture, p.x - 0.22f, p.y - 0.22f, 0.44f, 0.44f);
                }
                batch.setColor(p.homingArrow ? Color.GREEN : Color.WHITE);
                if (arrowRegion != null) {
                    batch.draw(arrowRegion, p.x - arrowW / 2f, p.y - arrowH / 2f,
                        arrowW / 2f, arrowH / 2f, arrowW, arrowH, 1f, 1f, deg);
                } else {
                    if (p.vx < 0) batch.draw(arrowTexture, p.x + arrowW / 2f, p.y - arrowH / 2f, -arrowW, arrowH);
                    else batch.draw(arrowTexture, p.x - arrowW / 2f, p.y - arrowH / 2f, arrowW, arrowH);
                }
                batch.setColor(Color.WHITE);
            } else {
                batch.draw(bulletTexture, p.x - 0.08f, p.y - 0.08f, 0.16f, 0.16f);
            }
        }

        // Player
        Color pTint;
        if (playerHitFlashTimer > 0) {
            pTint = COLOR_HIT_FLASH;
        } else if (playerStunTimer > 0) {
            pTint = COLOR_STUN;
        } else if (isP2StealthActive) {
            pTint = COLOR_P2_STEALTH; // Tàng hình bóng ma (zero-GC)
        } else if (isP1UltActive) {
            pTint = COLOR_P1_ULT; // Bão kiếm lam ngọc (zero-GC)
        } else if (isCrouching) {
            pTint = COLOR_STEALTH;
        } else if (isDashing) {
            pTint = isP2Char ? COLOR_P2_DASH_GLOW : COLOR_DASH_GLOW;
        } else {
            pTint = Color.WHITE;
        }

        // P2 Stealth detection range ring
        if (isP2StealthActive && playerBody != null) {
            Vector2 pPos = playerBody.getPosition();
            for (int a = 0; a < 16; a++) {
                float rad = (float)(a * (Math.PI * 2 / 16) + stateTime * 2.5);
                float rx = pPos.x + (float)Math.cos(rad) * P2_STEALTH_RANGE;
                float ry = pPos.y + (float)Math.sin(rad) * P2_STEALTH_RANGE;
                batch.setColor(p2TargetInRange ? Color.RED : COLOR_P2_RING_NORMAL);
                batch.draw(bulletTexture, rx - 0.05f, ry - 0.05f, 0.10f, 0.10f);
            }
            batch.setColor(Color.WHITE);
        }

        // Lôi Phong Hộ Thể (P1 Skill 2): Bát Quái Kiếm Trận & 4 Thần Kiếm bay lượn hộ thể
        if (p1WindShieldTimer > 0f && playerBody != null && !playerDead) {
            Vector2 pPos = playerBody.getPosition();
            if (p1MagicCircleTex != null) {
                float circlePulse = 0.75f + 0.25f * MathUtils.sin(stateTime * 6f);
                batch.setColor(0.35f, 0.90f, 1.0f, 0.65f * circlePulse);
                batch.draw(p1MagicCircleTex, pPos.x - 1.25f, pPos.y - 0.22f, 1.25f, 0.22f, 2.50f, 0.50f, 1f, 1f, stateTime * 45f, 0, 0, 128, 128, false, false);
            }
            if (p1CelestialSwordRegion != null) {
                batch.setColor(1.0f, 1.0f, 1.0f, 0.95f);
                for (int sk = 0; sk < 4; sk++) {
                    float orbitDeg = stateTime * 340f + sk * 90f;
                    float radX = 1.15f;
                    float radY = 0.52f;
                    float sx = pPos.x + MathUtils.cosDeg(orbitDeg) * radX;
                    float sy = pPos.y + 0.35f + MathUtils.sinDeg(orbitDeg) * radY;
                    float swordRot = orbitDeg + 90f;
                    batch.draw(p1CelestialSwordRegion, sx - 0.40f, sy - 0.10f, 0.40f, 0.10f, 0.80f, 0.20f, 1f, 1f, swordRot);
                }
            }
            batch.setColor(Color.WHITE);
        }

        // Vạn Kiếm Quy Tông (P1 Skill 3): Cánh Thần Kiếm (Kiếm Dực) sau lưng chờ phóng
        if (p1SwordRainRemaining > 0 && playerBody != null && !playerDead && p1CelestialSwordRegion != null) {
            Vector2 pPos = playerBody.getPosition();
            batch.setColor(1.0f, 0.92f, 0.45f, 0.95f);
            for (int si = 0; si < p1SwordRainRemaining; si++) {
                float ang = MathUtils.PI * (0.22f + 0.56f * (si / 7f));
                float sx = pPos.x + MathUtils.cos(ang) * 0.72f;
                float sy = pPos.y + 0.55f + MathUtils.sin(ang) * 0.55f;
                float swordRot = 60f + (si / 7f) * 60f;
                batch.draw(p1CelestialSwordRegion, sx - 0.35f, sy - 0.08f, 0.35f, 0.08f, 0.70f, 0.16f, 1f, 1f, swordRot);
            }
            batch.setColor(Color.WHITE);
        }

        // Remote Vạn Kiếm Quy Tông (CharId=1)
        if (remoteSwordRainRemaining > 0 && p1CelestialSwordRegion != null && remoteSwordRainX != -999f) {
            batch.setColor(1.0f, 0.92f, 0.45f, 0.95f);
            for (int si = 0; si < remoteSwordRainRemaining; si++) {
                float ang = MathUtils.PI * (0.22f + 0.56f * (si / 7f));
                float sx = remoteSwordRainX + MathUtils.cos(ang) * 0.72f;
                float sy = remoteSwordRainY + 0.55f + MathUtils.sin(ang) * 0.55f;
                float swordRot = 60f + (si / 7f) * 60f;
                batch.draw(p1CelestialSwordRegion, sx - 0.35f, sy - 0.08f, 0.35f, 0.08f, 0.70f, 0.16f, 1f, 1f, swordRot);
            }
            batch.setColor(Color.WHITE);
        }

        // Nội tại Phong Thần Kiếm Ý (P1): Các thanh tiểu thần kiếm bay xoay quanh nhân vật
        if (!"P2".equals(selectedChar) && p1PassiveStacks > 0 && playerBody != null && !playerDead) {
            Vector2 pPos = playerBody.getPosition();
            float orbOrbitR = 0.75f;
            batch.setColor(p1AwakenedTimer > 0f ? Color.GOLD : Color.CYAN);
            for (int k = 0; k < p1PassiveStacks; k++) {
                float ang = stateTime * 4.2f + k * (MathUtils.PI2 / Math.max(1, p1PassiveStacks));
                float ox = pPos.x + MathUtils.cos(ang) * orbOrbitR;
                float oy = pPos.y + 0.35f + MathUtils.sin(ang) * 0.42f;
                if (p1CelestialSwordRegion != null) {
                    batch.draw(p1CelestialSwordRegion, ox - 0.22f, oy - 0.06f, 0.22f, 0.06f, 0.44f, 0.12f, 1f, 1f, ang * MathUtils.radiansToDegrees);
                } else if (sparkTexture != null) {
                    batch.draw(sparkTexture, ox - 0.12f, oy - 0.12f, 0.24f, 0.24f);
                }
            }
            batch.setColor(Color.WHITE);
        }

        // Dư ảnh bóng ma sát thủ của P2 (Shadow Afterimages)
        for (int i = 0; i < p2Afterimages.size; i++) {
            P2Afterimage ai = p2Afterimages.get(i);
            tempColor.set(ai.color.r, ai.color.g, ai.color.b, ai.alpha);
            drawCharacter(ai.frame != null ? ai.frame : currentFrame, "ATTACK", ai.x, ai.y, ai.facing, tempColor, 1.0f, false);
        }

        // Ghost dash / P1 Ultimate wind motion trail
        if (isDashing || isP1UltActive) {
            float trailOffset = -playerFacing * 0.28f;
            Color trailColor = isP1UltActive ? COLOR_P1_ULT_TRAIL : (isP2Char ? COLOR_P2_DASH_TRAIL : COLOR_DASH_TRAIL);
            drawCharacter(currentFrame, animStateName, playerBody.getPosition().x + trailOffset,
                playerBody.getPosition().y, playerFacing, trailColor, 1.0f, false);
        }

        if (!playerDead) {
            drawCharacter(currentFrame, animStateName, playerBody.getPosition().x,
                playerBody.getPosition().y, playerFacing, pTint, 1.0f, true);
        } else if (playerDeathBlinkTimer > 0) {
            playerDeathBlinkTimer -= delta;
            if ((int)(playerDeathBlinkTimer * 24) % 2 == 0) {
                drawCharacter(currentFrame, "DEAD", playerDeathX, playerDeathY, playerFacing, COLOR_HIT_FLASH, 1.0f, true);
            }
        }



        // Hiển thị thanh Khiên (Shield) trên đầu nhân vật ở Solo / Co-op / 1v1 khi đang có khiên
        if (currentGameMode != GameMode.BATTLE_5V5 && playerShield > 0 && playerBody != null && !playerDead) {
            float px = playerBody.getPosition().x;
            float py = playerBody.getPosition().y + 0.84f;
            float sBarW = 0.72f, sBarH = 0.065f;
            float sPct = MathUtils.clamp(playerShield / 350f, 0f, 1f);
            batch.setColor(0.08f, 0.10f, 0.14f, 0.85f);
            batch.draw(pixel, px - sBarW / 2f - 0.02f, py - 0.015f, sBarW + 0.04f, sBarH + 0.03f);
            batch.setColor(0.35f, 0.92f, 1.0f, 0.95f);
            batch.draw(pixel, px - sBarW / 2f, py, sBarW * sPct, sBarH);
            batch.setColor(Color.WHITE);
        }

        // Remote player(s) — hiển thị chuẩn xác 100% hoạt ảnh Sát Thủ Trắng (charId=1) hoặc Sát Thủ Đen (charId=2)
        if (currentGameMode != GameMode.SOLO && remoteX != -999f) {
            if (remoteDead) {
                if (remoteDeathBlinkTimer > 0) {
                    remoteDeathBlinkTimer -= delta;
                    if ((int)(remoteDeathBlinkTimer * 24) % 2 == 0) {
                        TextureRegion rFrame = resolveRemoteCharacterFrame(remoteCharId == 2, remoteWeapon, "DEAD", remoteAttackAnimTime);
                        drawCharacter(rFrame, "DEAD", remoteDeathX, remoteDeathY, remoteFacing, COLOR_HIT_FLASH, 1.0f, false);
                    }
                }
                if (remoteRespawnTimer > 0) {
                    remoteRespawnTimer -= delta;
                }
            } else if (isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)) {
                boolean isRemoteDark = (remoteCharId == 2);
                TextureRegion rFrame = resolveRemoteCharacterFrame(isRemoteDark, remoteWeapon, remoteAnimState, remoteAttackAnimTime);
                Color rTint = Color.WHITE;
                if (remoteHitFlashTimer > 0f) rTint = COLOR_HIT_FLASH;
                else if (remoteStealth) rTint = COLOR_P2_STEALTH;
                else if (currentGameMode == GameMode.BATTLE_5V5 && remoteInBush) rTint = COLOR_FIGHTER_BUSH;

                drawCharacter(rFrame, remoteAnimState, remoteX, remoteY, remoteFacing, rTint, 1.0f, false);
                // Lôi Phong Hộ Thể của đối thủ / đồng đội
                if (remoteWindShieldTimer > 0f) {
                    if (p1MagicCircleTex != null) {
                        float circlePulse = 0.75f + 0.25f * MathUtils.sin(stateTime * 6f);
                        batch.setColor(0.35f, 0.90f, 1.0f, 0.65f * circlePulse);
                        batch.draw(p1MagicCircleTex, remoteX - 1.25f, remoteY - 0.22f, 1.25f, 0.22f, 2.50f, 0.50f, 1f, 1f, stateTime * 45f, 0, 0, 128, 128, false, false);
                    }
                    if (p1CelestialSwordRegion != null) {
                        batch.setColor(1.0f, 1.0f, 1.0f, 0.95f);
                        for (int sk = 0; sk < 4; sk++) {
                            float orbitDeg = stateTime * 340f + sk * 90f;
                            float radX = 1.15f;
                            float radY = 0.52f;
                            float sx = remoteX + MathUtils.cosDeg(orbitDeg) * radX;
                            float sy = remoteY + 0.35f + MathUtils.sinDeg(orbitDeg) * radY;
                            float swordRot = orbitDeg + 90f;
                            batch.draw(p1CelestialSwordRegion, sx - 0.40f, sy - 0.10f, 0.40f, 0.10f, 0.80f, 0.20f, 1f, 1f, swordRot);
                        }
                    }
                    batch.setColor(Color.WHITE);
                }
                if (currentGameMode == GameMode.BATTLE_5V5) {
                    drawRemote5v5OverheadBar(remoteX, remoteY, remoteHp, 1000, remoteShield, remoteTeam);
                }
            }
        }
        if (currentGameMode == GameMode.BATTLE_5V5) {
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.x == -999f) continue;
                if (ep.dead) {
                    if (ep.deathBlinkTimer > 0) {
                        ep.deathBlinkTimer -= delta;
                        if ((int)(ep.deathBlinkTimer * 24) % 2 == 0) {
                            TextureRegion epFrame = resolveRemoteCharacterFrame(ep.charId == 2, ep.weapon, "DEAD", ep.attackAnimTime);
                            drawCharacter(epFrame, "DEAD", ep.deathX, ep.deathY, ep.facing, COLOR_HIT_FLASH, 1.0f, false);
                        }
                    }
                    if (ep.respawnTimer > 0) {
                        ep.respawnTimer -= delta;
                    }
                } else if (isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) {
                    TextureRegion epFrame = resolveRemoteCharacterFrame(ep.charId == 2, ep.weapon, ep.animState, ep.attackAnimTime);
                    Color epTint = Color.WHITE;
                    if (ep.hitFlashTimer > 0f) epTint = COLOR_HIT_FLASH;
                    else if (ep.stealth) epTint = COLOR_P2_STEALTH;
                    else if (ep.inBush) epTint = COLOR_FIGHTER_BUSH;
                    drawCharacter(epFrame, ep.animState, ep.x, ep.y, ep.facing, epTint, 1.0f, false);
                    drawRemote5v5OverheadBar(ep.x, ep.y, ep.hp, ep.maxHp, ep.shield, ep.team);
                }
            }
        }

        // Monsters (Quái lính / Thủ Lĩnh)
        for (int i = 0; i < activeMonsters.size; i++) {
            GroundMob m = activeMonsters.get(i);
            if (m.body != null) {
                float mx = m.body.getPosition().x;
                // Tối ưu hóa culling: bỏ qua render quái nằm ngoài màn hình
                if (mx < camMinX - 2.5f || mx > camMaxX + 2.5f) continue;

                // Rapid flickering / blinking during the last 0.35s of death
                if (m.isDead && m.deathTimer < 0.35f) {
                    int blink = (int)(m.deathTimer * 28);
                    if (blink % 2 == 0) continue;
                }

                TextureRegion mFrame;
                if (m.isBoss && bossWalkAnim != null) {
                    // BOSS (Thủ Lĩnh) ANIMATIONS
                    if (m.isDead || "DEAD".equals(m.state)) {
                        if (m.deathTimer > 0.40f) {
                            mFrame = (bossNgaFrame != null) ? bossNgaFrame : (bossChetFrame != null ? bossChetFrame : monsterIdleFrame);
                        } else {
                            mFrame = (bossChetFrame != null) ? bossChetFrame : monsterIdleFrame;
                        }
                    } else if ("SMASH_WINDUP".equals(m.state)) {
                        if (m.animTime < 0.45f) {
                            mFrame = (bossGamFrame != null) ? bossGamFrame : (bossUltiFrame != null ? bossUltiFrame : monsterIdleFrame);
                        } else {
                            mFrame = (bossUltiFrame != null) ? bossUltiFrame : monsterIdleFrame;
                        }
                    } else if ("ATTACK".equals(m.state)) {
                        if (m.animTime < 0.35f) {
                            mFrame = (bossDanh1Frame != null) ? bossDanh1Frame : monsterIdleFrame;
                        } else {
                            mFrame = (bossDanh2Frame != null) ? bossDanh2Frame : monsterIdleFrame;
                        }
                    } else if ("CHASE".equals(m.state) || "PATROL".equals(m.state)) {
                        if (m.patrolWaitTimer > 0 && "PATROL".equals(m.state)) {
                            mFrame = bossWalkAnim.getKeyFrame(0f, false);
                        } else {
                            mFrame = bossWalkAnim.getKeyFrame(m.animTime, true);
                        }
                    } else {
                        mFrame = bossWalkAnim.getKeyFrame(0f, false);
                    }
                } else if (linhDungFrame != null) {
                    // GUARD (Lính Thường) ANIMATIONS
                    if (m.isDead || "DEAD".equals(m.state)) {
                        if (m.deathTimer > 0.40f && linhNgaFrame != null) {
                            mFrame = linhNgaFrame;
                        } else {
                            mFrame = (linhChetFrame != null) ? linhChetFrame : linhDungFrame;
                        }
                    } else if (m.hitTimer > 0 || "HIT".equals(m.state)) {
                        mFrame = (linhBiDanhFrame != null) ? linhBiDanhFrame : linhDungFrame;
                    } else if ("ATTACK".equals(m.state) || "SMASH_WINDUP".equals(m.state)) {
                        if (m.animTime < 0.30f) {
                            mFrame = (linhDanh1Frame != null) ? linhDanh1Frame : (linhDonFrame != null ? linhDonFrame : linhDungFrame);
                        } else {
                            mFrame = (linhDanh2Frame != null) ? linhDanh2Frame : (linhDonFrame != null ? linhDonFrame : linhDungFrame);
                        }
                    } else if ("CHASE".equals(m.state)) {
                        mFrame = (linhDonFrame != null) ? linhDonFrame : linhDungFrame;
                    } else if ("PATROL".equals(m.state)) {
                        if (m.patrolWaitTimer > 0) {
                            mFrame = linhDungFrame;
                        } else {
                            mFrame = (linhRunAnim != null) ? linhRunAnim.getKeyFrame(m.animTime, true) : linhDungFrame;
                        }
                    } else {
                        mFrame = linhDungFrame;
                    }
                } else {
                    mFrame = monsterIdleFrame;
                    if (m.isDead || "DEAD".equals(m.state))
                        mFrame = (monsterHitFrame != null) ? monsterHitFrame : monsterIdleFrame;
                    else if (m.hitTimer > 0 || "HIT".equals(m.state))
                        mFrame = (monsterHitFrame != null) ? monsterHitFrame : monsterIdleFrame;
                    else if ("CHASE".equals(m.state) || "PATROL".equals(m.state))
                        mFrame = (monsterRunAnim != null) ? monsterRunAnim.getKeyFrame(stateTime, true) : monsterIdleFrame;
                    else if ("ATTACK".equals(m.state) || "SMASH_WINDUP".equals(m.state))
                        mFrame = (monsterHitFrame != null) ? monsterHitFrame : monsterIdleFrame;
                }

                // Bá khí màu đỏ bừng bừng tỏa ra quanh Thủ Lĩnh (zero-GC aura)
                if (m.isBoss && !m.isDead && (m.isChargingBerserk || m.isBerserk) && hpOrbTexture != null) {
                    float auraSize = m.scale * 2.1f;
                    float auraPulse = 0.65f + 0.35f * MathUtils.sin(m.berserkAuraTime * (m.isChargingBerserk ? 16f : 9f));
                    if (m.isChargingBerserk) tempColor.set(1.0f, 0.05f, 0.05f, 0.70f * auraPulse);
                    else if (m.isResting) tempColor.set(0.35f, 0.75f, 1.0f, 0.40f);
                    else tempColor.set(1.0f, 0.15f, 0.10f, 0.55f * auraPulse);
                    batch.setColor(tempColor);
                    batch.draw(hpOrbTexture, mx - auraSize / 2f, m.body.getPosition().y - 0.25f, auraSize, auraSize);
                    batch.setColor(Color.WHITE);
                }

                Color col;
                if (m.hitTimer > 0) {
                    col = COLOR_HIT_FLASH;
                } else if (m.poisonTimer > 0f) {
                    tempColor.set(0.80f, 0.48f, 1.0f, 1f);
                    col = tempColor;
                } else if (m.isBoss) {
                    if (m.isChargingBerserk) {
                        col = COLOR_BOSS_CHARGING;
                    } else if (m.isResting) {
                        col = COLOR_BOSS_RESTING;
                    } else if (m.isBerserk) {
                        col = COLOR_BOSS_BERSERK;
                    } else {
                        col = ("SMASH_WINDUP".equals(m.state) ? COLOR_BOSS_SMASH_TINT : COLOR_BOSS_TINT);
                    }
                } else {
                    col = Color.WHITE;
                }

                String drawState = m.isDead ? "DEAD" : (m.hitTimer > 0 ? "HIT" : (m.patrolWaitTimer > 0 && "PATROL".equals(m.state) ? "IDLE" : m.state));
                if (m.isBoss) {
                    drawCharacterWithSpec(mFrame, drawState, m.body.getPosition().x, m.body.getPosition().y,
                        m.facing, col, 1.68f, 0.70f, false);
                } else {
                    drawCharacter(mFrame, drawState, m.body.getPosition().x, m.body.getPosition().y,
                        m.facing, col, m.scale, false);
                }

                // Floating HP bar above mob head (only when alive)
                if (!m.isDead && (m.hp < m.maxHp || m.isBoss)) {
                    float my = m.body.getPosition().y + (m.isBoss ? 1.08f : 0.82f);
                    float mBarW = m.isBoss ? 1.15f : 0.65f;
                    float mBarH = m.isBoss ? 0.09f : 0.07f;
                    float hpPercent = MathUtils.clamp((float)m.hp / m.maxHp, 0f, 1f);

                    // Dark outer border
                    batch.setColor(0.10f, 0.10f, 0.14f, 0.85f);
                    batch.draw(pixel, mx - mBarW / 2f - 0.02f, my - 0.015f, mBarW + 0.04f, mBarH + 0.03f);

                    // Dark track
                    batch.setColor(0.35f, 0.05f, 0.05f, 0.90f);
                    batch.draw(pixel, mx - mBarW / 2f, my, mBarW, mBarH);

                    // Health fill (Boss Berserk: fiery crimson, Boss: fiery orange/gold, Mobs: ruby red)
                    if (m.isBoss) {
                        if (m.isChargingBerserk || m.isBerserk) {
                            batch.setColor(1.0f, 0.10f, 0.10f, 1f);
                        } else {
                            batch.setColor(1.0f, 0.55f, 0.05f, 1f);
                        }
                    } else {
                        batch.setColor(0.95f, 0.22f, 0.22f, 1f);
                    }
                    batch.draw(pixel, mx - mBarW / 2f, my, mBarW * hpPercent, mBarH);

                    // Glass highlight line
                    batch.setColor(1f, 1f, 1f, 0.35f);
                    batch.draw(pixel, mx - mBarW / 2f, my + mBarH * 0.55f, mBarW * hpPercent, mBarH * 0.45f);

                    batch.setColor(Color.WHITE);
                }
            }
        }

        // 5V5 Combatants & Mini HP/Shield Bars
        if (currentGameMode == GameMode.BATTLE_5V5) {
            int pTeam = (game != null) ? game.selectedTeam : 1;
            Vector2 plPos = (playerBody != null) ? playerBody.getPosition() : null;

            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.body == null) continue;

                if (f.isDead) {
                    // Hiển thị animation nhấp nháy 0.85s rồi biến mất hoàn toàn (không để xác lại)
                    if (f.deathBlinkTimer > 0) {
                        int blink = (int)(f.deathBlinkTimer * 24);
                        if (blink % 2 == 0) {
                            TextureRegion fFrame = resolveRemoteCharacterFrame(f.charId == 2, WeaponType.SWORD, "DEAD", stateTime);
                            drawCharacter(fFrame, "DEAD", f.deathX, f.deathY, f.facing, COLOR_HIT_FLASH, 1.0f, false);
                        }
                    }
                    continue;
                }

                // Co che tang hinh bui ram (Tactical Bush Camouflage)
                boolean sameBushAsPlayer = false;
                if (f.isInBush && playerInBush && plPos != null) {
                    for (int bi = 0; bi < tacticalBushes.size; bi++) {
                        TacticalBush bush = tacticalBushes.get(bi);
                        if (bush.contains(plPos) && bush.contains(f.body.getPosition())) {
                            sameBushAsPlayer = true;
                            break;
                        }
                    }
                }
                // Neu ke dich nup trong bui ma nguoi choi o ngoai -> Tang hinh hoan toan!
                if (f.isInBush && f.team != pTeam && !sameBushAsPlayer) {
                    continue;
                }

                float fx = f.body.getPosition().x;
                float fy = f.body.getPosition().y;

                // Hao quang Than Thu (Dragon Slayer Buff Aura)
                if (f.hasTitanBuff) {
                    batch.setColor(1.0f, 0.85f, 0.20f, 0.40f + MathUtils.sin(stateTime * 6f) * 0.18f);
                    batch.draw(hpOrbTexture, fx - 0.45f, fy - 0.45f, 0.90f, 0.35f);
                }

                TextureRegion fFrame = resolveRemoteCharacterFrame(f.charId == 2, WeaponType.SWORD, f.animState, stateTime);

                Color fTint = f.hitTimer > 0 ? COLOR_HIT_FLASH : (f.team == 1 ? COLOR_FIGHTER_TEAM1 : COLOR_FIGHTER_TEAM2);
                if (f.hasBerserk()) fTint = COLOR_FIGHTER_BERSERK;
                else if (f.isStealth) fTint = COLOR_P2_STEALTH;
                else if (f.isInBush) fTint = COLOR_FIGHTER_BUSH; // Mau xanh mo ao khi dang trong bui

                drawCharacter(fFrame, f.animState, fx, fy, f.facing, fTint, 1.0f, false);

                // Mini HP & Shield bar above fighter head
                float barW = 0.85f, barH = 0.08f;
                float hpPercent = MathUtils.clamp((float)f.hp / f.maxHp, 0f, 1f);
                float barY = fy + 0.85f;

                // Dark border
                batch.setColor(0.10f, 0.10f, 0.14f, 0.85f);
                batch.draw(pixel, fx - barW / 2f - 0.02f, barY - 0.02f, barW + 0.04f, barH + 0.04f);

                // Dark red background track
                batch.setColor(0.35f, 0.05f, 0.05f, 0.90f);
                batch.draw(pixel, fx - barW / 2f, barY, barW, barH);

                // HP fill (Blue team: cyan/blue, Red team: crimson/orange)
                if (f.team == 1) {
                    batch.setColor(0.20f, 0.70f, 1.0f, 1.0f);
                } else {
                    batch.setColor(0.95f, 0.25f, 0.25f, 1.0f);
                }
                batch.draw(pixel, fx - barW / 2f, barY, barW * hpPercent, barH);

                // Shield bar if any
                if (f.shield > 0) {
                    float shieldPercent = MathUtils.clamp(f.shield / 350f, 0f, 1f);
                    batch.setColor(0.95f, 0.85f, 0.25f, 0.9f);
                    batch.draw(pixel, fx - barW / 2f, barY + barH * 0.45f, barW * shieldPercent, barH * 0.55f);
                }
                batch.setColor(Color.WHITE);
            }

            // Local player mini HP & Shield bar in 5v5
            if (playerBody != null && !playerDead) {
                float px = playerBody.getPosition().x;
                float py = playerBody.getPosition().y;

                // Hao quang Dragon Slayer Buff duoi chan nguoi choi
                if (playerHasTitanBuff) {
                    batch.setColor(1.0f, 0.85f, 0.20f, 0.40f + MathUtils.sin(stateTime * 6f) * 0.18f);
                    batch.draw(hpOrbTexture, px - 0.45f, py - 0.45f, 0.90f, 0.35f);
                    batch.setColor(Color.WHITE);
                }

                float barW = 0.85f, barH = 0.08f;
                float hpPercent = MathUtils.clamp((float)playerHp / playerMaxHp, 0f, 1f);
                float barY = py + 0.85f;

                batch.setColor(0.10f, 0.10f, 0.14f, 0.85f);
                batch.draw(pixel, px - barW / 2f - 0.02f, barY - 0.02f, barW + 0.04f, barH + 0.04f);

                batch.setColor(0.35f, 0.05f, 0.05f, 0.90f);
                batch.draw(pixel, px - barW / 2f, barY, barW, barH);

                batch.setColor(0.20f, 0.90f, 0.35f, 1.0f);
                batch.draw(pixel, px - barW / 2f, barY, barW * hpPercent, barH);

                if (playerShield > 0) {
                    float shieldPercent = MathUtils.clamp(playerShield / 350f, 0f, 1f);
                    batch.setColor(0.95f, 0.85f, 0.25f, 0.9f);
                    batch.draw(pixel, px - barW / 2f, barY + barH * 0.45f, barW * shieldPercent, barH * 0.55f);
                }
                batch.setColor(Color.WHITE);
            }

            // Vòng tròn hào quang hồi sinh phát sáng tại vị trí tử trận (ARAM 5v5)
            float haloSz = 1.20f + 0.15f * MathUtils.sin(stateTime * 6f);
            if (playerDead && playerRespawnTimer5v5 > 0) {
                batch.setColor(1.0f, 0.85f, 0.20f, 0.60f);
                batch.draw(hpOrbTexture, playerDeathX - haloSz / 2f, playerDeathY - 0.25f, haloSz, 0.45f);
            }
            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.isDead && f.respawnTimer > 0) {
                    if (f.team == 1) batch.setColor(0.20f, 0.70f, 1.0f, 0.55f);
                    else batch.setColor(0.95f, 0.25f, 0.25f, 0.55f);
                    batch.draw(hpOrbTexture, f.deathX - haloSz / 2f, f.deathY - 0.25f, haloSz, 0.45f);
                }
            }
            if (remoteDead && remoteRespawnTimer > 0) {
                if (remoteTeam == 1) batch.setColor(0.20f, 0.70f, 1.0f, 0.55f);
                else batch.setColor(0.95f, 0.25f, 0.25f, 0.55f);
                batch.draw(hpOrbTexture, remoteDeathX - haloSz / 2f, remoteDeathY - 0.25f, haloSz, 0.45f);
            }
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.dead && ep.respawnTimer > 0 && ep.deathX != -999f) {
                    if (ep.team == 1) batch.setColor(0.20f, 0.70f, 1.0f, 0.55f);
                    else batch.setColor(0.95f, 0.25f, 0.25f, 0.55f);
                    batch.draw(hpOrbTexture, ep.deathX - haloSz / 2f, ep.deathY - 0.25f, haloSz, 0.45f);
                }
            }
            batch.setColor(Color.WHITE);
        }

        // Tâm ngắm Ứng Kích trên mục tiêu đang khóa
        if (lockOnRegion != null && btnEagleStrike != null && btnEagleStrike.isVisible() && !isEagleStriking) {
            float lpx = Float.NaN, lpy = Float.NaN;
            if (lockOnEnemy != null && lockOnEnemy.body != null) {
                lpx = lockOnEnemy.body.getPosition().x; lpy = lockOnEnemy.body.getPosition().y;
            } else if (lockOnFighter != null && lockOnFighter.body != null) {
                lpx = lockOnFighter.body.getPosition().x; lpy = lockOnFighter.body.getPosition().y;
            } else if (lockOnIsPrimaryRemote && remoteX != -999f && !remoteDead) {
                lpx = remoteX; lpy = remoteY;
            } else if (lockOnPeerId != null) {
                RemotePeer5v5 ep = findExtraPeer(lockOnPeerId);
                if (ep != null && !ep.dead && ep.x != -999f) { lpx = ep.x; lpy = ep.y; }
            }
            if (!Float.isNaN(lpx)) {
                float sz = 0.75f + 0.08f * MathUtils.sin(stateTime * 6f);
                batch.setColor(1f, 1f, 1f, 0.85f);
                batch.draw(lockOnRegion, lpx - sz / 2f, lpy + 0.15f - sz / 2f, sz / 2f, sz / 2f, sz, sz, 1f, 1f, stateTime * 90f);
                batch.setColor(Color.WHITE);
            }
        }

        // Tâm khóa mục tiêu tự động của Cung Tên (tên bắn ra luôn bay trúng mục tiêu đang khóa)
        if (lockOnRegion != null && currentWeapon == WeaponType.BOW && hasBowLock() && !playerDead && !isMatchEnded) {
            float sz = 0.70f + 0.07f * MathUtils.sin(stateTime * 8.5f);
            batch.setColor(0.25f, 1.0f, 0.55f, 0.92f);
            batch.draw(lockOnRegion, bowLockWorldPos.x - sz / 2f, bowLockWorldPos.y - sz / 2f,
                sz / 2f, sz / 2f, sz, sz, 1f, 1f, -stateTime * 140f);
            batch.setColor(Color.WHITE);
        }

        // Hit spark particles
        for (int i = hitSparks.size - 1; i >= 0; i--) {
            HitSpark s = hitSparks.get(i);
            s.lifeTime -= delta;
            if (s.lifeTime <= 0) {
                hitSparks.removeIndex(i);
                if (sparkPool.size < 200) sparkPool.add(s);
                continue;
            }
            s.x += s.vx * delta;
            s.y += s.vy * delta;
            s.vy -= 9.8f * delta * 0.5f; // mild gravity
            float alpha = MathUtils.clamp(s.lifeTime / s.maxLife, 0f, 1f);
            batch.setColor(s.color.r, s.color.g, s.color.b, alpha);
            batch.draw(sparkTexture, s.x - s.size / 2f, s.y - s.size / 2f, s.size, s.size);
        }
        batch.setColor(Color.WHITE);

        batch.end();

        // Floating damage text (UI-space 1280x720, khớp viewport của uiStage)
        uiStage.getViewport().apply();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 1280, 720);
        batch.begin();
        for (int i = damageTexts.size - 1; i >= 0; i--) {
            DamageText dt = damageTexts.get(i);
            dt.x += dt.vx * delta;
            dt.y += dt.vy * delta;
            dt.lifeTime -= delta;

            worldToUi(dt.x, dt.y);

            float lifeProgress = 1f - (dt.lifeTime / dt.maxLife);
            float fontScale = (lifeProgress < 0.22f) ? (1.55f - lifeProgress * 1.5f) : 1.2f;
            damageFont.getData().setScale(fontScale * dmgFontK);

            float alpha = MathUtils.clamp(dt.lifeTime / dt.maxLife, 0f, 1f);

            drawTagCentered(dt.text, screenPos.x, screenPos.y, dt.color, alpha);

            if (dt.lifeTime <= 0) damageTexts.removeIndex(i);
        }

        // 5V5 Floating Name Tags above combatants
        if (currentGameMode == GameMode.BATTLE_5V5) {
            damageFont.getData().setScale(0.95f * dmgFontK);
            int pTeam = (game != null) ? game.selectedTeam : 1;
            Vector2 plPos = (playerBody != null) ? playerBody.getPosition() : null;

            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.body == null || f.isDead) continue;

                // An ten neu ke dich nup bui
                boolean sameBush = false;
                if (f.isInBush && playerInBush && plPos != null) {
                    for (int bi = 0; bi < tacticalBushes.size; bi++) {
                        TacticalBush bush = tacticalBushes.get(bi);
                        if (bush.contains(plPos) && bush.contains(f.body.getPosition())) {
                            sameBush = true;
                            break;
                        }
                    }
                }
                if (f.isInBush && f.team != pTeam && !sameBush) continue;

                worldToUi(f.body.getPosition().x, f.body.getPosition().y + 1.15f);
                drawTagCentered(f.displayTag, screenPos.x, screenPos.y, f.team == 1 ? COLOR_TAG_BLUE : COLOR_TAG_RED, 1f);
            }

            // Ten Boss Than Thu Bao Chua
            if (ancientTitan != null && ancientTitan.isAlive) {
                worldToUi(ancientTitan.x, ancientTitan.y + 2.40f);
                String tTag = "[TRÙM] THẦN THÚ BẠO CHÚA";
                Color tColor = Color.GOLD;
                if (ancientTitan.isChargingBerserk) {
                    tTag = "[TRÙM] GỒNG BÁ KHÍ (-85% sát thương)";
                    tColor = Color.RED;
                } else if (ancientTitan.isResting) {
                    tTag = "[TRÙM] THỞ DỐC (1,2 GIÂY)";
                    tColor = Color.YELLOW;
                } else if (ancientTitan.isBerserk) {
                    tTag = "[TRÙM] CUỒNG HUYẾT (+80% tốc độ, +70% sát thương)";
                    tColor = Color.ORANGE;
                }
                drawTagCentered(tTag, screenPos.x, screenPos.y, tColor, 1f);
            }

            if (!remoteDead && remoteX != -999f && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)) {
                worldToUi(remoteX, remoteY + 1.15f);
                String rTag = (remoteTeam == 1 ? "[XANH] " : "[ĐỎ] ") + remoteName;
                drawTagCentered(rTag, screenPos.x, screenPos.y, remoteTeam == 1 ? COLOR_TAG_BLUE : COLOR_TAG_RED, 1f);
            }
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.dead || ep.x == -999f) continue;
                if (!isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) continue;
                worldToUi(ep.x, ep.y + 1.15f);
                String epTag = (ep.team == 1 ? "[XANH] " : "[ĐỎ] ") + ep.name;
                drawTagCentered(epTag, screenPos.x, screenPos.y, ep.team == 1 ? COLOR_TAG_BLUE : COLOR_TAG_RED, 1f);
            }

            if (playerBody != null && !playerDead) {
                worldToUi(playerBody.getPosition().x, playerBody.getPosition().y + 1.15f);
                drawTagCentered(playerTag5v5, screenPos.x, screenPos.y, COLOR_TAG_SELF, 1f);
            }

            // Hiển thị thời gian đếm ngược hồi sinh tại vị trí tử trận (ARAM 5v5 / Online)
            damageFont.getData().setScale(0.92f * dmgFontK);
            if (playerDead && playerRespawnTimer5v5 > 0) {
                worldToUi(playerDeathX, playerDeathY + 0.65f);
                int sec = (int) Math.ceil(playerRespawnTimer5v5);
                drawTagCentered("[BẠN] HỒI SINH: " + sec + "s", screenPos.x, screenPos.y, Color.GOLD, 0.95f);
            }
            for (int i = 0; i < fighters5v5.size; i++) {
                Fighter5v5 f = fighters5v5.get(i);
                if (f.isDead && f.respawnTimer > 0) {
                    worldToUi(f.deathX, f.deathY + 0.65f);
                    int sec = (int) Math.ceil(f.respawnTimer);
                    Color tCol = (f.team == 1) ? COLOR_TAG_BLUE : COLOR_TAG_RED;
                    drawTagCentered(f.name + " (" + sec + "s)", screenPos.x, screenPos.y, tCol, 0.90f);
                }
            }
            if (remoteDead && remoteRespawnTimer > 0) {
                worldToUi(remoteDeathX, remoteDeathY + 0.65f);
                int sec = (int) Math.ceil(remoteRespawnTimer);
                Color tCol = (remoteTeam == 1) ? COLOR_TAG_BLUE : COLOR_TAG_RED;
                drawTagCentered(remoteName + " (" + sec + "s)", screenPos.x, screenPos.y, tCol, 0.90f);
            }
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.dead && ep.respawnTimer > 0 && ep.deathX != -999f) {
                    worldToUi(ep.deathX, ep.deathY + 0.65f);
                    int sec = (int) Math.ceil(ep.respawnTimer);
                    Color tCol = (ep.team == 1) ? COLOR_TAG_BLUE : COLOR_TAG_RED;
                    drawTagCentered(ep.name + " (" + sec + "s)", screenPos.x, screenPos.y, tCol, 0.90f);
                }
            }
        }

        // GroundMob Boss Floating Status Tag (Solo / Co-op)
        if (currentGameMode != GameMode.PVP && currentGameMode != GameMode.BATTLE_5V5) {
            damageFont.getData().setScale(0.95f * dmgFontK);
            for (int i = 0; i < activeMonsters.size; i++) {
                GroundMob m = activeMonsters.get(i);
                if (m.isBoss && m.body != null && !m.isDead) {
                    worldToUi(m.body.getPosition().x, m.body.getPosition().y + 1.35f);
                    String bTag = (story != null && story.bossName != null) ? "[TRÙM] " + noAccent(story.bossName) : "[TRÙM] THỦ LĨNH";
                    Color bColor = Color.GOLD;
                    if (m.isChargingBerserk) {
                        bTag = "[TRÙM] GỒNG BÁ KHÍ (-85% sát thương)";
                        bColor = Color.RED;
                    } else if (m.isResting) {
                        bTag = "[TRÙM] THỞ DỐC (1,2 GIÂY)";
                        bColor = Color.YELLOW;
                    } else if (m.isBerserk) {
                        bTag = "[TRÙM] CUỒNG HUYẾT (+80% tốc độ, +70% sát thương)";
                        bColor = Color.ORANGE;
                    }
                    drawTagCentered(bTag, screenPos.x, screenPos.y, bColor, 1f);
                }
            }
        }

        // Nhãn khóa mục tiêu Cung Tên
        if (currentWeapon == WeaponType.BOW && hasBowLock() && !playerDead && !isMatchEnded) {
            damageFont.getData().setScale(0.85f * dmgFontK);
            worldToUi(bowLockWorldPos.x, bowLockWorldPos.y + 0.52f);
            drawTagCentered("[ĐÃ KHÓA]", screenPos.x, screenPos.y, Color.GREEN, 0.95f);
        }

        // Bong bóng thoại Chat trên đầu đồng đội / đối thủ trực tuyến (ẩn nếu kẻ địch 5v5 đang núp bụi)
        if (isOnlineMode() && commsManager != null && commsManager.remoteBubbleTimer > 0f
            && remoteX != -999f && !remoteDead
            && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)
            && commsManager.remoteBubbleText != null
            && !commsManager.remoteBubbleText.isEmpty()) {
            worldToUi(remoteX, remoteY + 1.18f);
            BitmapFont chatFont = UiKit.small();
            String msg = commsManager.remoteBubbleText;
            if (msg.length() > 48) msg = msg.substring(0, 48) + "...";
            glyphLayout.setText(chatFont, msg);
            float padX = 10f, padY = 6f;
            float bw = glyphLayout.width + padX * 2f;
            float bh = glyphLayout.height + padY * 2f;
            float bx = MathUtils.clamp(screenPos.x - bw / 2f, 12f, 1268f - bw);
            float by = MathUtils.clamp(screenPos.y, 24f, 696f - bh);
            float alpha = MathUtils.clamp(commsManager.remoteBubbleTimer * 2.0f, 0f, 1f);

            batch.setColor(0.06f, 0.08f, 0.14f, 0.90f * alpha);
            batch.draw(pixel, bx, by, bw, bh);
            batch.setColor(0.95f, 0.80f, 0.30f, 0.90f * alpha);
            batch.draw(pixel, bx, by, bw, 1.5f);
            batch.draw(pixel, bx, by + bh - 1.5f, bw, 1.5f);
            batch.draw(pixel, bx, by, 1.5f, bh);
            batch.draw(pixel, bx + bw - 1.5f, by, 1.5f, bh);

            chatFont.setColor(1f, 0.97f, 0.88f, alpha);
            chatFont.draw(batch, msg, bx + padX, by + bh - padY);
            chatFont.setColor(Color.WHITE);
            batch.setColor(Color.WHITE);
        }

        damageFont.getData().setScale(1.4f * dmgFontK);
        batch.end();

        // === MINI MAP HUD (Screen-Space 1280x720 - TOP LEFT) ===
        uiStage.getViewport().apply();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 1280, 720);
        batch.begin();

        float miniX = 20f, miniY = 515f, miniW = 250f, miniH = 125f;
        float scaleX = miniW / mapWidth, scaleY = miniH / mapHeight;

        // Dark Translucent Card Background
        batch.setColor(COLOR_MINIMAP_BG);
        batch.draw(pixel, miniX, miniY, miniW, miniH);

        // Gold Border Frame
        batch.setColor(COLOR_MINIMAP_BORDER);
        batch.draw(pixel, miniX, miniY, miniW, 2.5f);
        batch.draw(pixel, miniX, miniY + miniH - 2.5f, miniW, 2.5f);
        batch.draw(pixel, miniX, miniY, 2.5f, miniH);
        batch.draw(pixel, miniX + miniW - 2.5f, miniY, 2.5f, miniH);

        // Map Obstacles (Ground Platforms & Elevated Building Roofs)
        for (int i = 0; i < platformsList.size; i++) {
            MapPlatform p = platformsList.get(i);
            float px = miniX + p.x * scaleX;
            float py = miniY + p.y * scaleY;
            float pw = Math.max(3f, p.width * scaleX);
            float ph = Math.max(3f, p.height * scaleY);
            batch.setColor(p.isGround ? COLOR_MINIMAP_GROUND : COLOR_MINIMAP_PLAT);
            batch.draw(pixel, px, py, pw, ph);
        }

        // Map Obstacles (Walls & Brick Barriers)
        batch.setColor(0.65f, 0.60f, 0.55f, 0.85f);
        for (int i = 0; i < wallsList.size; i++) {
            MapWall w = wallsList.get(i);
            float wx = miniX + w.x * scaleX;
            float wy = miniY + w.y * scaleY;
            float ww = Math.max(3f, w.width * scaleX);
            float wh = Math.max(3f, w.height * scaleY);
            batch.draw(pixel, wx, wy, ww, wh);
        }

        // Ladders & Ropes Obstacles
        batch.setColor(COLOR_MINIMAP_LADDER);
        for (int i = 0; i < laddersList.size; i++) {
            Ladder l = laddersList.get(i);
            float lx = miniX + l.x * scaleX;
            float ly = miniY + l.y * scaleY;
            float lh = Math.max(3f, l.height * scaleY);
            batch.draw(pixel, lx, ly, 2f, lh);
        }
        batch.setColor(COLOR_MINIMAP_ROPE);
        for (int i = 0; i < swingRopes.size; i++) {
            SwingRope r = swingRopes.get(i);
            float rx = miniX + r.anchor.x * scaleX;
            float ry = miniY + (r.anchor.y - r.length) * scaleY;
            float rh = Math.max(3f, r.length * scaleY);
            batch.draw(pixel, rx - 1f, ry, 2f, rh);
        }

        // Finish Flag
        if (currentGameMode != GameMode.PVP && currentGameMode != GameMode.BATTLE_5V5) {
            float fx = miniX + finishLineX * scaleX;
            float fy = miniY + 1.25f * scaleY;
            batch.setColor(Color.GOLD);
            batch.draw(sparkTexture, fx - 3f, fy - 3f, 6f, 6f);
        }

        // Active Mobs (Ground Mobs, Bosses)
        for (int i = 0; i < activeMonsters.size; i++) {
            GroundMob m = activeMonsters.get(i);
            if (m.body != null && !m.isDead) {
                float mx = miniX + m.body.getPosition().x * scaleX;
                float my = miniY + m.body.getPosition().y * scaleY;
                batch.setColor(m.isBoss ? Color.ORANGE : Color.RED);
                float dotSize = m.isBoss ? 6f : 4f;
                batch.draw(sparkTexture, mx - dotSize / 2f, my - dotSize / 2f, dotSize, dotSize);
            }
        }

        // Sky Pirate & Imperial Soldier
        if (pirateAlive) {
            float px = miniX + pirateX * scaleX;
            float py = miniY + pirateY * scaleY;
            batch.setColor(Color.MAGENTA);
            batch.draw(sparkTexture, px - 2.5f, py - 2.5f, 5f, 5f);
        }
        if (imperialAlive) {
            float ix = miniX + imperialX * scaleX;
            float iy = miniY + imperialY * scaleY;
            batch.setColor(Color.GOLD);
            batch.draw(sparkTexture, ix - 3f, iy - 3f, 6f, 6f);
        }

        // Remote Player / Co-op Partner / 5v5 Peers
        if (currentGameMode != GameMode.SOLO && !remoteDead && remoteX != -999f
            && isRemoteVisibleIn5v5(remoteX, remoteY, remoteTeam, remoteInBush)) {
            float rx = miniX + remoteX * scaleX;
            float ry = miniY + remoteY * scaleY;
            Color dotCol = (currentGameMode == GameMode.COOP) ? Color.CYAN
                : (currentGameMode == GameMode.BATTLE_5V5 ? (remoteTeam == 1 ? COLOR_FIGHTER_BLUE : COLOR_FIGHTER_RED) : Color.RED);
            batch.setColor(dotCol);
            batch.draw(sparkTexture, rx - 3f, ry - 3f, 6f, 6f);
        }
        if (currentGameMode == GameMode.BATTLE_5V5) {
            for (int epi = 0; epi < extraPeers5v5.size; epi++) {
                RemotePeer5v5 ep = extraPeers5v5.get(epi);
                if (ep.dead || ep.x == -999f || !isRemoteVisibleIn5v5(ep.x, ep.y, ep.team, ep.inBush)) continue;
                float rx = miniX + ep.x * scaleX;
                float ry = miniY + ep.y * scaleY;
                batch.setColor(ep.team == 1 ? COLOR_FIGHTER_BLUE : COLOR_FIGHTER_RED);
                batch.draw(sparkTexture, rx - 3f, ry - 3f, 6f, 6f);
            }
        }

        // Local Player
        if (playerBody != null && !playerDead) {
            float px = miniX + playerBody.getPosition().x * scaleX;
            float py = miniY + playerBody.getPosition().y * scaleY;
            batch.setColor(Color.GREEN);
            batch.draw(sparkTexture, px - 3.5f, py - 3.5f, 7f, 7f);
        }

        // Bushes & Power-up Runes on MiniMap (5v5 & CO-OP)
        for (int bi = 0; bi < tacticalBushes.size; bi++) {
            TacticalBush b = tacticalBushes.get(bi);
            float bx = miniX + b.x * scaleX;
            float by = miniY + b.y * scaleY;
            batch.setColor(COLOR_MINIMAP_BUSH);
            batch.draw(pixel, bx, by, Math.max(3f, b.width * scaleX), Math.max(3f, b.height * scaleY));
        }
        for (int ri = 0; ri < powerUpRunes.size; ri++) {
            PowerUpRune r = powerUpRunes.get(ri);
            if (r.active) {
                float rx = miniX + r.x * scaleX;
                float ry = miniY + r.y * scaleY;
                batch.setColor(r.type.getColor());
                batch.draw(hpOrbTexture, rx - 2.5f, ry - 2.5f, 5f, 5f);
            }
        }

        // 5v5 Turrets, Ancient Titan & Fighters on MiniMap
        if (currentGameMode == GameMode.BATTLE_5V5) {
            int pTeam = (game != null) ? game.selectedTeam : 1;
            Vector2 plPos = (playerBody != null) ? playerBody.getPosition() : null;

            // 1. Turrets on MiniMap
            for (int ti = 0; ti < outpostTurrets.size; ti++) {
                OutpostTurret t = outpostTurrets.get(ti);
                if (!t.isDestroyed) {
                    float tx = miniX + t.x * scaleX;
                    float ty = miniY + t.y * scaleY;
                    batch.setColor(t.team == 1 ? COLOR_FIGHTER_BLUE : COLOR_FIGHTER_RED);
                    batch.draw(sparkTexture, tx - 3.5f, ty - 3.5f, 7f, 8f);
                }
            }

            // 2. Ancient Titan Boss on MiniMap
            if (ancientTitan != null && ancientTitan.isAlive) {
                float tx = miniX + ancientTitan.x * scaleX;
                float ty = miniY + ancientTitan.y * scaleY;
                batch.setColor(Color.GOLD);
                batch.draw(runeUltTex, tx - 4f, ty - 4f, 8f, 8f);
            }

            // 3. Fighters on MiniMap
            for (int fi = 0; fi < fighters5v5.size; fi++) {
                Fighter5v5 f = fighters5v5.get(fi);
                if (f.body != null && !f.isDead) {
                    boolean sameBush = false;
                    if (f.isInBush && playerInBush && plPos != null) {
                        for (int bi = 0; bi < tacticalBushes.size; bi++) {
                            TacticalBush bush = tacticalBushes.get(bi);
                            if (bush.contains(plPos) && bush.contains(f.body.getPosition())) {
                                sameBush = true; break;
                            }
                        }
                    }
                    if (f.isInBush && f.team != pTeam && !sameBush) continue;

                    float fx = miniX + f.body.getPosition().x * scaleX;
                    float fy = miniY + f.body.getPosition().y * scaleY;
                    batch.setColor(f.team == 1 ? COLOR_FIGHTER_BLUE : COLOR_FIGHTER_RED);
                    batch.draw(sparkTexture, fx - 2.5f, fy - 2.5f, 5f, 5f);
                }
            }
        }

        // Camera Viewport Rectangle Box
        float camViewW = viewport.getWorldWidth() * camera.zoom, camViewH = viewport.getWorldHeight() * camera.zoom;
        float camW = camViewW * scaleX;
        float camH = camViewH * scaleY;
        float camX = miniX + (camera.position.x - camViewW / 2f) * scaleX;
        float camY = miniY + (camera.position.y - camViewH / 2f) * scaleY;
        batch.setColor(COLOR_MINIMAP_VIEW_RECT);
        batch.draw(pixel, camX, camY, camW, 1.5f);
        batch.draw(pixel, camX, camY + camH, camW, 1.5f);
        batch.draw(pixel, camX, camY, 1.5f, camH);
        batch.draw(pixel, camX + camW, camY, 1.5f, camH);

        // === CINEMATIC VIGNETTE OVERLAY (Consolidated into 1280x720 pass) ===
        if (vignetteTexture != null) {
            batch.setColor(1f, 1f, 1f, 0.55f);
            batch.draw(vignetteTexture, 0, 0, 1280, 720);
        }

        batch.setColor(Color.WHITE);
        batch.end();

        uiStage.getViewport().apply();
        uiStage.act(delta);
        uiStage.draw();
        drawStoryOverlay();

        // === BOSS INTRO CINEMATIC OVERLAY (Letterbox & Alert) ===
        if (bossIntroActive) {
            uiStage.getViewport().apply();
            batch.getProjectionMatrix().setToOrtho2D(0, 0, 1280, 720);
            batch.begin();

            // Cinema Letterbox Bars (Top & Bottom)
            float barH = 75f;
            batch.setColor(0.02f, 0.02f, 0.04f, 0.95f);
            batch.draw(pixel, 0, 720 - barH, 1280, barH);
            batch.draw(pixel, 0, 0, 1280, barH);

            // Flashing Red Alert Frame Center-Top
            float pulse = 0.65f + 0.35f * MathUtils.sin(totalMatchTime * 9.0f);
            float bannerW = 620f, bannerH = 64f;
            float bannerX = (1280 - bannerW) / 2f;
            float bannerY = 720 - barH - bannerH - 12f;

            // Crimson Banner Background
            batch.setColor(0.55f * pulse, 0.05f, 0.05f, 0.88f);
            batch.draw(pixel, bannerX, bannerY, bannerW, bannerH);

            // Gold Border
            batch.setColor(1.0f, 0.84f, 0.0f, 0.9f);
            batch.draw(pixel, bannerX, bannerY, bannerW, 2.5f);
            batch.draw(pixel, bannerX, bannerY + bannerH - 2.5f, bannerW, 2.5f);
            batch.draw(pixel, bannerX, bannerY, 2.5f, bannerH);
            batch.draw(pixel, bannerX + bannerW - 2.5f, bannerY, 2.5f, bannerH);

            // Text
            if (story != null && story.bossName != null) { // tên trùm có dấu
                com.badlogic.gdx.graphics.g2d.BitmapFont bf = UiKit.display();
                bf.getData().setScale(0.52f);
                bf.setColor(0f, 0f, 0f, 0.95f);
                bf.draw(batch, story.bossName, bannerX + 2f, bannerY + 52f - 2f, bannerW, Align.center, false);
                bf.setColor(1.0f, 0.82f, 0.30f, 1f);
                bf.draw(batch, story.bossName, bannerX, bannerY + 52f, bannerW, Align.center, false);
                bf.getData().setScale(1f);
                bf.setColor(Color.WHITE);
                batch.setColor(Color.WHITE);
                batch.end();
                return;
            }
            damageFont.getData().setScale(1.4f * dmgFontK);
            String warnTitle = "! CẢNH BÁO: THỦ LĨNH XUẤT HIỆN !";
            damageFont.setColor(0f, 0f, 0f, 0.95f);
            damageFont.draw(batch, warnTitle, bannerX + 50f + 2f, bannerY + 48f - 2f);
            damageFont.setColor(1.0f, 0.25f, 0.25f, 1.0f);
            damageFont.draw(batch, warnTitle, bannerX + 50f, bannerY + 48f);

            damageFont.getData().setScale(1.0f * dmgFontK);
            String warnSub = "[TRÙM ĐÃ THỨC TỈNH – CẨN THẬN CUỒNG HUYẾT!]";
            damageFont.setColor(0f, 0f, 0f, 0.95f);
            damageFont.draw(batch, warnSub, bannerX + 75f + 1.5f, bannerY + 22f - 1.5f);
            damageFont.setColor(Color.GOLD);
            damageFont.draw(batch, warnSub, bannerX + 75f, bannerY + 22f);

            damageFont.getData().setScale(1.4f * dmgFontK);
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    /** Lớp phủ hội thoại vẽ sau cùng (gọi ở cuối render). */
    private void drawStoryOverlay() {
        if (storyDialogueActive) drawStoryDialogue();
    }

    /**
     * Chiếu tọa độ thế giới sang tọa độ UI ảo 1280x720 (cùng hệ với uiStage/minimap).
     * Bản cũ chiếu theo kích thước cửa sổ thật nên chữ bị lệch khi cửa sổ
     * không đúng tỉ lệ 16:9 (có viền đen) và quá nhỏ trên màn hình độ phân giải cao.
     */
    private Vector3 worldToUi(float wx, float wy) {
        float viewW = viewport.getWorldWidth() * camera.zoom;
        float viewH = viewport.getWorldHeight() * camera.zoom;
        screenPos.x = (wx - (camera.position.x - viewW / 2f)) / viewW * 1280f;
        screenPos.y = (wy - (camera.position.y - viewH / 2f)) / viewH * 720f;
        screenPos.z = 0f;
        return screenPos;
    }

    /** Nền cuộn parallax (dùng chung cho khung hình thường và khi tạm dừng). */
    private void drawBackgroundParallax() {
        if (arenaBackground == null) return;
        float viewW = viewport.getWorldWidth();
        float parallaxOffset = (camera.position.x * 0.35f) % 25.6f;
        float startX = camera.position.x - viewW / 2f - 25.6f;
        float endX = camera.position.x + viewW / 2f + 25.6f;
        // Mỗi chương một sắc thái (trăng máu, ngục tối, hậu cung...) – nhuộm ảnh nền theo MapTheme
        StoryChapterConfig.MapTheme th = (story != null) ? story.theme : null;
        if (th != null) batch.setColor(Math.min(1f, th.bgR), Math.min(1f, th.bgG), Math.min(1f, th.bgB), 1f);
        for (float bgX = startX - parallaxOffset; bgX < endX; bgX += 25.6f) {
            batch.draw(arenaBackground, bgX, 0, 25.6f, mapHeight);
        }
        // Phủ màn mờ không khí 28% đẩy ảnh nền ra xa, tạo độ sâu không gian giúp địa hình và nhân vật phía trước nổi bật rõ ràng
        batch.setColor(0.01f, 0.01f, 0.03f, 0.28f);
        batch.draw(pixel, startX - parallaxOffset, 0, (endX - startX) + 51.2f, mapHeight);
        batch.setColor(Color.WHITE);
    }

    /** Bỏ dấu tiếng Việt cho chữ vẽ bằng font cũ trong thế giới game (font đó không có ký tự có dấu). */
    private static String noAccent(String s) {
        if (s == null) return "";
        return TextFmt.removeVietnameseAccents(s);
    }

    /** Vẽ chữ có bóng đổ, căn giữa theo x. */
    private void drawTagCentered(String text, float cx, float y, Color color, float alpha) {
        glyphLayout.setText(damageFont, text);
        float x = cx - glyphLayout.width / 2f;
        damageFont.setColor(0f, 0f, 0f, 0.85f * alpha);
        damageFont.draw(batch, text, x + 1.5f, y - 1.5f);
        damageFont.setColor(color.r, color.g, color.b, color.a * alpha);
        damageFont.draw(batch, text, x, y);
    }

    // Vẽ nhân vật với kích thước đồng nhất dựa trên thông số khung hình đã chuẩn hóa trong GameAssets
    private void drawCharacter(TextureRegion frame, String animState, float posX, float posY,
                               int facing, Color tint, float scale) {
        drawCharacter(frame, animState, posX, posY, facing, tint, scale, false);
    }

    private void drawCharacter(TextureRegion frame, String animState, float posX, float posY,
                               int facing, Color tint, float scale, boolean isPlayer) {
        drawCharacterWithSpec(frame, animState, posX, posY, facing, tint, 1.15f * scale, 0.40f * scale, isPlayer);
    }

    /**
     * Vẽ nhân vật theo chiều cao cơ thể mục tiêu (targetBodyH) và khoảng cách từ tâm Box2D xuống sàn (groundOff).
     * Tự động bù phần viền trống của từng ảnh PNG để nhân vật không bị to nhỏ thất thường hay lơ lửng chân.
     */
    private void drawCharacterWithSpec(TextureRegion frame, String animState, float posX, float posY,
                                       int facing, Color tint, float targetBodyH, float groundOff, boolean isPlayer) {
        if (frame == null) return;

        float canvasRatio = (frame.getRegionWidth() > 0 && frame.getRegionHeight() > 0)
            ? (float) frame.getRegionWidth() / frame.getRegionHeight() : 1.0f;

        GameAssets.SpriteMetric metric = GameAssets.metricOf(frame.getTexture());
        float bodyFrac = Math.max(0.25f, metric.bodyFrac);
        float footFrac = MathUtils.clamp(metric.footFrac, 0.35f, 1.0f);
        float centerXFrac = MathUtils.clamp(metric.centerXFrac, 0.25f, 0.75f);

        boolean isLyingDead = (frame == linhChetFrame || frame == bossChetFrame
            || frame == boyChetFrame || frame == trieuDinhChetFrame || frame == khongTacChetFrame);

        float stateMult = 1.0f;
        if ("CROUCH".equals(animState) || "ROLL".equals(animState)) {
            stateMult = 0.84f;
        } else if ("DASH".equals(animState)) {
            stateMult = 0.93f;
        } else if ("EAGLE_STRIKE".equals(animState)) {
            stateMult = 1.04f;
        } else if ("DEAD".equals(animState) && isLyingDead) {
            stateMult = 0.92f;
        }

        float drawH = (targetBodyH * stateMult) / bodyFrac;
        float drawW = drawH * canvasRatio;

        float padBottomFrac = Math.max(0f, 1.0f - footFrac);
        float drawBottomY = (posY - groundOff) - (padBottomFrac * drawH);
        if (isLyingDead) {
            drawBottomY -= 0.04f * targetBodyH;
        }

        if ("ROLL".equals(animState)) {
            batch.setColor(tint);
            batch.draw(frame, posX - (drawW / 2f), drawBottomY,
                drawW / 2f, drawH / 2f, drawW, drawH, (float) facing, 1f, stateTime * 500f * -facing);
            batch.setColor(Color.WHITE);
            return;
        }

        // Nhịp thở thân trên cho tư thế đứng 1 ảnh tĩnh; các bộ đứng nhiều frame vẽ nguyên khung
        boolean isP2 = "P2".equals(selectedChar);
        boolean hasNativeMultiIdle = isP2
            ? (darkIdleAnim != null && !(currentWeapon == WeaponType.SHOTGUN && denSungDungFrame != null) && !(currentWeapon == WeaponType.AXE && denRiuDungFrame != null))
            : (currentWeapon == WeaponType.SWORD && hasMultiFrameWhiteIdle);
        boolean useSplitIdleBreathing = isPlayer && "IDLE".equals(animState) && !hasNativeMultiIdle;
        if (useSplitIdleBreathing) {
            TextureRegion baseLegFrame = frame;
            float legRatio = 0.48f;
            int legH = (int) (baseLegFrame.getRegionHeight() * legRatio);
            int legY = baseLegFrame.getRegionY() + (baseLegFrame.getRegionHeight() - legH);

            TextureRegion legRegion = idleLegRegion;
            legRegion.setTexture(baseLegFrame.getTexture());
            legRegion.setRegion(baseLegFrame.getRegionX(), legY, baseLegFrame.getRegionWidth(), legH);

            float legDrawH = drawH * legRatio;
            boolean flip = (facing == -1);
            float drawX = flip ? (posX + centerXFrac * drawW) : (posX - centerXFrac * drawW);
            float actualW = flip ? -drawW : drawW;

            batch.setColor(tint);
            batch.draw(legRegion, drawX, drawBottomY, actualW, legDrawH);

            int torsoH = Math.max(1, frame.getRegionHeight() - legH);
            TextureRegion torsoRegion = idleTorsoRegion;
            torsoRegion.setTexture(frame.getTexture());
            torsoRegion.setRegion(frame.getRegionX(), frame.getRegionY(), frame.getRegionWidth(), torsoH);
            float torsoDrawH = drawH * (1.0f - legRatio);
            float breathScaleY = 1.0f + 0.020f * MathUtils.sin(stateTime * 3.8f);
            float torsoDrawHAnimated = torsoDrawH * breathScaleY;

            batch.draw(torsoRegion, drawX, drawBottomY + legDrawH, actualW, torsoDrawHAnimated);
            batch.setColor(Color.WHITE);
            return;
        }

        // Nhích người tới trước khi chém cận chiến (không áp dụng cho cung/súng)
        if (isPlayer && (!"P2".equals(selectedChar) || currentWeapon == WeaponType.AXE)
            && currentWeapon != WeaponType.BOW && currentWeapon != WeaponType.SHOTGUN
            && ("HIT1".equals(animState) || "HIT2".equals(animState) || "HIT3".equals(animState))) {
            float lunge = MathUtils.sin(MathUtils.clamp(attackTime / 0.20f, 0f, 1f) * MathUtils.PI) * 0.16f * facing;
            posX += lunge;
        }

        batch.setColor(tint);
        boolean flip = (facing == -1);
        float drawX = flip ? (posX + centerXFrac * drawW) : (posX - centerXFrac * drawW);
        float actualW = flip ? -drawW : drawW;
        batch.draw(frame, drawX, drawBottomY, actualW, drawH);
        batch.setColor(Color.WHITE);
    }

    @Override public void resize(int w, int h) {
        viewport.update(w, h, false);
        if (playerBody != null) {
            float halfViewW = (viewport.getWorldWidth() * camera.zoom) / 2f;
            float halfViewH = (viewport.getWorldHeight() * camera.zoom) / 2f;
            float initCamX = MathUtils.clamp(playerBody.getPosition().x, halfViewW, mapWidth - halfViewW);
            float initCamY = MathUtils.clamp(playerBody.getPosition().y + 0.6f, halfViewH, mapHeight - halfViewH);
            camera.position.set(initCamX, initCamY, 0);
            camera.update();
        }
        uiStage.getViewport().update(w, h, true);
    }
    @Override public void show() {
        isHidden = false;
        if (uiStage != null) Gdx.input.setInputProcessor(uiStage);
        setupNetworkListeners();
        try {
            NetworkManager nm = NetworkManager.getInstance();
            if (nm != null) {
                String pMode = (currentGameMode == GameMode.SOLO) ? "SOLO"
                    : (currentGameMode == GameMode.COOP ? "COOP"
                    : (currentGameMode == GameMode.BATTLE_5V5 ? "5V5" : "PVP"));
                nm.updateMyPresence("IN_GAME", pMode, currentGameMode == GameMode.SOLO ? "" : nm.currentRoomCode);
            }
        } catch (Exception ignored) {}
        SoundManager.getInstance().playBattleBgm();
        isFirstCameraFrame = true;
        if (playerBody != null) {
            float halfViewW = (viewport.getWorldWidth() * camera.zoom) / 2f;
            float halfViewH = (viewport.getWorldHeight() * camera.zoom) / 2f;
            float initCamX = MathUtils.clamp(playerBody.getPosition().x, halfViewW, mapWidth - halfViewW);
            float initCamY = MathUtils.clamp(playerBody.getPosition().y + 0.6f, halfViewH, mapHeight - halfViewH);
            camera.position.set(initCamX, initCamY, 0);
            camera.update();
        }
    }
    /** Xóa mọi trạng thái leo thang, bám tường, bám mép, đu dây, Ứng Kích, tuyệt kỹ. Gọi khi nhân vật bị dịch chuyển (hồi sinh, rơi nước...)
     *  - nếu không, trạng thái cũ kéo nhân vật về chỗ cũ (VD đang bám mép thì chết -> hồi sinh xong bị kéo lại mép = "kẹt tường"). */
    private void resetTraversalState() {
        isLedgeGrabbing = false;
        ledgeHoldTowardTimer = 0f;
        ledgeRegrabCooldown = 0f;
        isWallClinging = false;
        clingingWall = null;
        isClimbing = false;
        isWallSliding = false;
        if (isSwinging && currentSwingingRope != null) currentSwingingRope.release();
        currentSwingingRope = null;
        isSwinging = false;
        if (isEagleStriking) {
            isEagleStriking = false;
            eagleStrikePhase = 0;
        }
        pendingEagleGroundAbort = false;
        eagleTargetMob = null;
        eagleTargetFighter = null;
        eagleTargetIsPrimaryRemote = false;
        eagleTargetPeerId = null;
        isP1UltActive = false;
        isP2StealthActive = false;
        p2StealthTimer = 0f;
        isP2DashingAssassinate = false;
        p1SwordRainRemaining = 0;
        isDashing = false;
        jumpCount = 0;
        if (playerBody != null) {
            playerBody.setGravityScale(1f);
            playerBody.setBullet(false);
        }
    }

    /** Esc trên PC hoặc nút Back trên Android. */
    private static boolean isBackJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK);
    }

    /** Ứng dụng bị đưa xuống nền (nghe điện thoại, chuyển app...): nhả thao tác cảm ứng & chơi đơn tự mở menu tạm dừng. */
    @Override public void pause() {
        if (uiStage != null) uiStage.cancelTouchFocus();
        isAttackButtonPressed = false;
        isClimbingHold = false;
        if (isDisposed || isPaused || isMatchEnded || isOnlineMode()) return;
        try { togglePause(); } catch (Exception e) { Gdx.app.error("GameScreen", "Khong tam dung duoc", e); }
    }
    @Override public void resume() {}
    /** Gỡ listener mạng và tắt mic/loa ngay khi rời màn (dispose chạy trễ 1 frame). */
    @Override public void hide() {
        isHidden = true;
        cleanSocketListeners();
        if (commsManager != null) {
            commsManager.dispose();
            commsManager = null;
        }
        SoundManager.getInstance().stopBgm();
    }

    @Override
    public void dispose() {
        if (isDisposed) return; // prevent double-dispose
        isDisposed = true;

        if (commsManager != null) {
            commsManager.dispose();
            commsManager = null;
        }

        // Body của cậu bé/quái được hủy cùng World
        boyBody = null;
        if (world != null) world.dispose();
        if (uiStage != null) uiStage.dispose();
        if (skin != null) skin.dispose();
        if (batch != null) batch.dispose();
        if (damageFont != null) damageFont.dispose();

        // Chỉ giải phóng texture do màn hình này TỰ TẠO (procedural).
        // Sprite PNG thuộc bộ đệm GameAssets, được dùng lại cho trận sau nên KHÔNG dispose ở đây.
        Texture[] own = {
            ladderTexture, platformWoodTexture, groundBrickTexture, wallTexture, platformConsoleTex,
            p1CrescentAuraTex, p1CelestialSwordTex, p1MagicCircleTex, p2KunaiTex,
            hpOrbTexture, weaponBoxTexture,
            arrowTexture, bulletTexture, ropeTexture, cannonTexture, cannonBallTexture,
            slingshotPelletTexture, finishFlagTexture, lockOnTexture, sparkTexture, ultimateOrbTexture,
            jumpPadTexture, runeBerserkTex, runeShieldTex, runeWindTex, runeHealTex, runeUltTex,
            bushTexture, turretTexture, titanBossTexture, fountainAuraTexture, vignetteTexture,
            idleFallbackTex, padBgTex, padKnobTex
        };
        for (Texture t : own) {
            if (t != null) {
                try { t.dispose(); } catch (Exception ignored) {}
            }
        }
        for (int i = 0; i < ownedPixmaps.size; i++) {
            Pixmap pm = ownedPixmaps.get(i);
            if (pm != null) {
                try { pm.dispose(); } catch (Exception ignored) {}
            }
        }
        ownedPixmaps.clear();
        p2Afterimages.clear();
    }
}




















