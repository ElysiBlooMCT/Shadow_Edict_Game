package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Quản lý lưu và đọc dữ liệu tiến độ chơi cho Chế độ Cốt Truyện (Story Mode).
 * Lưu Màn (chương) / Ải theo StoryChapterConfig (hiện 3 màn x 3 ải = 9 ải), nhân vật đã chọn, HP, Vàng, Điểm số, Checkpoint.
 * File lưu của bản 30 ải trước đây (chương > 3 hoặc ải > 3) được coi là không hợp lệ và bỏ qua.
 * Tương thích mượt mà trên cả Android (SharedPreferences) và Desktop (Preferences XML).
 */
public class StorySaveManager {
    private static final String PREF_NAME = "AssassinsLegend_StorySave";

    public static boolean hasSave() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            if (!prefs.getBoolean("hasSavedGame", false)) return false;
            int ch = prefs.getInteger("savedChapter", 1), st = prefs.getInteger("savedStage", 1);
            return ch >= 1 && ch <= StoryChapterConfig.MAX_CHAPTERS && st >= 1 && st <= StoryChapterConfig.STAGES_PER_CHAPTER;
        } catch (Exception e) {
            return false;
        }
    }

    public static int getSavedChapter() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return Math.max(1, Math.min(StoryChapterConfig.MAX_CHAPTERS, prefs.getInteger("savedChapter", 1)));
        } catch (Exception e) {
            return 1;
        }
    }

    public static int getSavedStage() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return Math.max(1, Math.min(StoryChapterConfig.STAGES_PER_CHAPTER, prefs.getInteger("savedStage", 1)));
        } catch (Exception e) {
            return 1;
        }
    }

    public static int getSavedLevel() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return StoryChapterConfig.indexOf(getSavedChapter(), getSavedStage());
        } catch (Exception e) {
            return 1;
        }
    }

    public static String getSavedCharacter() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return "P2".equals(prefs.getString("savedCharacter", "P1")) ? "P2" : "P1";
        } catch (Exception e) {
            return "P1";
        }
    }

    public static int getSavedHp() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return Math.max(25, prefs.getInteger("savedHp", 100));
        } catch (Exception e) {
            return 100;
        }
    }

    public static int getSavedGold() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return Math.max(0, prefs.getInteger("savedGold", 0));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getSavedScore() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return Math.max(0, prefs.getInteger("savedScore", 0));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getSavedPotions() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return Math.max(0, prefs.getInteger("savedPotions", 1));
        } catch (Exception e) {
            return 1;
        }
    }

    public static float getSavedCheckpointX() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            float x = prefs.getFloat("savedCheckpointX", 3.0f);
            return (Float.isFinite(x) && x >= 0f) ? x : 3.0f;
        } catch (Exception e) {
            return 3.0f;
        }
    }

    public static boolean isPrologueSeen() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return prefs.getBoolean("prologueSeen", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setPrologueSeen(boolean seen) {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            prefs.putBoolean("prologueSeen", seen);
            prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveProgress(int chapter, int stage, String character, int hp, int gold, int score, int potions, float checkpointX) {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            prefs.putBoolean("hasSavedGame", true);
            int safeCh = Math.max(1, Math.min(StoryChapterConfig.MAX_CHAPTERS, chapter));
            int safeSt = Math.max(1, Math.min(StoryChapterConfig.STAGES_PER_CHAPTER, stage));
            prefs.putInteger("savedChapter", safeCh);
            prefs.putInteger("savedStage", safeSt);
            prefs.putInteger("savedLevel", (safeCh - 1) * StoryChapterConfig.STAGES_PER_CHAPTER + safeSt);
            prefs.putString("savedCharacter", character != null ? character : "P1");
            prefs.putInteger("savedHp", Math.max(25, hp));
            prefs.putInteger("savedGold", Math.max(0, gold));
            prefs.putInteger("savedScore", Math.max(0, score));
            prefs.putInteger("savedPotions", Math.max(0, potions));
            prefs.putFloat("savedCheckpointX", (Float.isFinite(checkpointX) && checkpointX >= 0f) ? checkpointX : 3.0f);
            prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveGame(int level, String character, int hp, int gold, int score, int potions, float checkpointX) {
        int ch = StoryChapterConfig.chapterOf(level);
        int st = StoryChapterConfig.stageOf(level);
        saveProgress(ch, st, character, hp, gold, score, potions, checkpointX);
    }

    public static void advanceStage(int currentLevel, String character, int hp, int gold, int score, int potions) {
        int nextLevel = currentLevel + 1;
        if (nextLevel > StoryChapterConfig.TOTAL_STAGES) {
            clearSave();
        } else {
            saveGame(nextLevel, character, hp, gold, score, potions, 3.0f);
        }
    }

    public static void advanceStage(String character, int hp, int gold, int score, int potions) {
        int ch = getSavedChapter();
        int st = getSavedStage() + 1;
        if (st > StoryChapterConfig.STAGES_PER_CHAPTER) {
            ch++;
            st = 1;
        }
        if (ch > StoryChapterConfig.MAX_CHAPTERS) {
            clearSave(); // Đã phá đảo toàn bộ chiến dịch!
        } else {
            saveProgress(ch, st, character, hp, gold, score, potions, 3.0f);
        }
    }

    public static void clearSave() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            // Ghi tường minh false trước khi flush: WebPreferences của gdx-teavm có lỗi xóa vòng lặp xuôi
            // khiến khóa đã remove đôi khi còn sót lại trong localStorage sau khi tải lại trang.
            prefs.putBoolean("hasSavedGame", false);
            prefs.putInteger("savedChapter", 1);
            prefs.putInteger("savedStage", 1);
            prefs.putInteger("savedLevel", 1);
            prefs.putString("savedCharacter", "P1");
            prefs.putInteger("savedHp", 100);
            prefs.putInteger("savedGold", 0);
            prefs.putInteger("savedScore", 0);
            prefs.putInteger("savedPotions", 1);
            prefs.putFloat("savedCheckpointX", 3.0f);
            prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
