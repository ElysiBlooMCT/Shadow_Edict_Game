package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

import java.util.Random;

/**
 * Hồ sơ người chơi lưu cục bộ (Preferences): danh tính, vàng, tiến trình ải và cài đặt âm thanh.
 */
public class PlayerProfile {
    private static final String PREF_NAME = "AssassinsLegend_Profile";
    /** Tổng số ải cốt truyện (StoryChapterConfig: 3 màn x 3 ải). */
    public static final int MAX_LEVEL = StoryChapterConfig.TOTAL_STAGES;
    private static PlayerProfile instance;

    public String userId;
    public String playerName;
    public int gold;
    public int level;
    public int matches = 0;
    public int wins = 0;
    public String favoriteHero = "Vân Tiêu";

    // Tiến trình chế độ SOLO
    public int maxUnlockedLevel = 1;
    public int bestScore = 0;
    /** Ải cốt truyện chơi gần nhất & nhân vật đã chọn (để "Chơi tiếp"). */
    public int lastStoryLevel = 1;
    public String lastCharacter = "P1";

    // Cài đặt âm thanh & ngôn ngữ
    public boolean musicEnabled = true;
    public boolean soundEnabled = true;
    public float musicVolume = 0.50f;
    public float soundVolume = 0.85f;
    public String language = "vi";

    private PlayerProfile() {
        load();
    }

    public static PlayerProfile getInstance() {
        if (instance == null) {
            instance = new PlayerProfile();
        }
        return instance;
    }

    private Preferences prefs() {
        return Gdx.app.getPreferences(PREF_NAME);
    }

    private static int safeInt(Preferences prefs, String key, int def) {
        try { return prefs.getInteger(key, def); } catch (Exception e) { return def; }
    }

    private static float safeFloat(Preferences prefs, String key, float def) {
        try {
            float v = prefs.getFloat(key, def);
            return Float.isFinite(v) ? v : def;
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean safeBool(Preferences prefs, String key, boolean def) {
        try { return prefs.getBoolean(key, def); } catch (Exception e) { return def; }
    }

    private static String safeStr(Preferences prefs, String key, String def) {
        try {
            String v = prefs.getString(key, def);
            return v != null ? v : def;
        } catch (Exception e) {
            return def;
        }
    }

    public void load() {
        Preferences prefs = null;
        try {
            prefs = prefs();
        } catch (Exception e) {
            if (userId == null || userId.isEmpty()) userId = "AL-" + (1000 + new Random().nextInt(9000));
            if (playerName == null || playerName.isEmpty()) playerName = "Chiến Thần";
            if (gold < 0) gold = 500;
            if (level < 1) level = 1;
            return;
        }

        boolean generatedUserId = false;
        userId = safeStr(prefs, "userId", "");
        if (userId.isEmpty()) {
            userId = "AL-" + (1000 + new Random().nextInt(9000));
            generatedUserId = true;
        }
        playerName = safeStr(prefs, "playerName", "Chiến Thần");
        if (playerName.trim().isEmpty()) playerName = "Chiến Thần";
        gold = Math.max(0, safeInt(prefs, "gold", 500));
        level = Math.max(1, safeInt(prefs, "level", 1));
        matches = Math.max(0, safeInt(prefs, "matches", 0));
        wins = MathUtils.clamp(safeInt(prefs, "wins", 0), 0, matches);
        favoriteHero = safeStr(prefs, "favoriteHero", "Vân Tiêu");
        if (favoriteHero.trim().isEmpty()) favoriteHero = "Vân Tiêu";
        maxUnlockedLevel = MathUtils.clamp(safeInt(prefs, "maxUnlockedLevel", 1), 1, MAX_LEVEL);
        bestScore = Math.max(0, safeInt(prefs, "bestScore", 0));
        lastStoryLevel = MathUtils.clamp(safeInt(prefs, "lastStoryLevel", 1), 1, maxUnlockedLevel);
        lastCharacter = "P2".equals(safeStr(prefs, "lastCharacter", "P1")) ? "P2" : "P1";
        musicEnabled = safeBool(prefs, "musicEnabled", true);
        soundEnabled = safeBool(prefs, "soundEnabled", true);
        musicVolume = MathUtils.clamp(safeFloat(prefs, "musicVolume", 0.50f), 0f, 1f);
        soundVolume = MathUtils.clamp(safeFloat(prefs, "soundVolume", 0.85f), 0f, 1f);
        language = safeStr(prefs, "language", "vi");

        if (generatedUserId) {
            try {
                prefs.putString("userId", userId);
                prefs.flush();
            } catch (Exception e) {
                Gdx.app.error("PlayerProfile", "Khong luu duoc userId moi", e);
            }
        }
    }

    public void save() {
        try {
            Preferences prefs = prefs();
            prefs.putString("userId", userId);
            prefs.putString("playerName", playerName);
            prefs.putInteger("gold", gold);
            prefs.putInteger("level", level);
            prefs.putInteger("matches", matches);
            prefs.putInteger("wins", wins);
            prefs.putString("favoriteHero", favoriteHero);
            prefs.putInteger("maxUnlockedLevel", maxUnlockedLevel);
            prefs.putInteger("bestScore", bestScore);
            prefs.putInteger("lastStoryLevel", lastStoryLevel);
            prefs.putString("lastCharacter", lastCharacter);
            prefs.putBoolean("musicEnabled", musicEnabled);
            prefs.putBoolean("soundEnabled", soundEnabled);
            prefs.putFloat("musicVolume", musicVolume);
            prefs.putFloat("soundVolume", soundVolume);
            prefs.putString("language", language);
            prefs.flush();
        } catch (Exception e) {
            Gdx.app.error("PlayerProfile", "Khong luu duoc ho so", e);
        }
    }

    /** Cộng vàng mang về sau trận và lưu lại. */
    public void addGold(int amount) {
        if (amount <= 0) return;
        gold += amount;
        save();
    }

    /** Ghi nhận hoàn thành ải SOLO: mở khóa ải tiếp theo + cập nhật điểm cao nhất. */
    public void onLevelCleared(int clearedLevel, int score) {
        maxUnlockedLevel = MathUtils.clamp(Math.max(maxUnlockedLevel, clearedLevel + 1), 1, MAX_LEVEL);
        lastStoryLevel = MathUtils.clamp(clearedLevel + 1, 1, MAX_LEVEL);
        if (score > bestScore) bestScore = score;
        save();
    }

    /** Ghi nhận kết quả trận đấu online/custom. */
    public void recordMatchResult(boolean won) {
        matches++;
        if (won) wins++;
        save();
    }

    public String getRankTitle() {
        return rankTitleForLevel(level);
    }

    public static String rankTitleForLevel(int lvl) {
        if (lvl <= 1) return "Tập Sự";
        if (lvl <= 4) return "Chiến Binh";
        if (lvl <= 9) return "Tinh Anh Hư Không";
        if (lvl <= 14) return "Sát Thủ Ám Ảnh";
        if (lvl <= 19) return "Bậc Thầy Song Đao";
        return "Huyền Thoại Bất Diệt";
    }

    public String getWinRateText() {
        if (matches <= 0) return "0%";
        int rate = (int) Math.round((double) wins * 100.0 / matches);
        return rate + "%";
    }

    /** Áp dụng cài đặt âm thanh đã lưu cho SoundManager. */
    public void applyAudioSettings() {
        SoundManager sm = SoundManager.getInstance();
        sm.setSoundVolume(soundVolume);
        sm.setSoundEnabled(soundEnabled);
        sm.setMusicVolume(musicVolume);
        sm.setMusicEnabled(musicEnabled);
    }

    public void updateFromServer(String serverUserId, String serverName, int serverGold, int serverLevel) {
        if (serverUserId != null && !serverUserId.isEmpty()) {
            this.userId = serverUserId;
        }
        if (serverName != null && !serverName.isEmpty()) {
            this.playerName = serverName;
        }
        // Server hiện chưa nhận vàng từ client -> giữ giá trị lớn hơn để không mất vàng kiếm được offline
        if (serverGold > this.gold) {
            this.gold = serverGold;
        }
        if (serverLevel > 0) {
            this.level = serverLevel;
        }
        save();
    }
}
