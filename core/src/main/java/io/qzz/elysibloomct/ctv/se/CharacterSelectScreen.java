package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Chọn sát thủ + chọn chương/ải cốt truyện cho chế độ CHƠI ĐƠN.
 * Phím tắt: ←/→ hoặc 1/2 chọn nhân vật, ↑/↓ đổi ải, Enter bắt đầu, Esc quay lại.
 */
public class CharacterSelectScreen extends ScreenAdapter {
    private static final int CH = StoryChapterConfig.MAX_CHAPTERS, ST = StoryChapterConfig.STAGES_PER_CHAPTER;

    private final Main game;
    private Stage stage;
    private Image frameP1, frameP2;
    private UiKit.HeroHalo haloP1, haloP2;
    private Table cardP1, cardP2;
    private Label lblHero, lblStage, lblObjective;
    private TextButton btnStart;
    private final TextButton[] chapterButtons = new TextButton[CH];
    private final TextButton[] stageButtons = new TextButton[ST];
    private int selectedLevel; // 1..30
    private int maxUnlocked;
    private int warmupFrames = 2;
    private boolean disposed = false;
    private int customMap = 0; // 0: cốt truyện thường, 1..3: chế độ tùy chọn map
    private boolean isNewStory = false;

    public CharacterSelectScreen(Main game) {
        this.game = game;
    }

    public static CharacterSelectScreen forNewStory(Main game) {
        CharacterSelectScreen s = new CharacterSelectScreen(game);
        s.isNewStory = true;
        return s;
    }

    public static CharacterSelectScreen forCustomMap(Main game, int customMap) {
        CharacterSelectScreen s = new CharacterSelectScreen(game);
        s.customMap = customMap;
        return s;
    }

    @Override
    public void show() {
        SoundManager.getInstance().playMenuBgm();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        PlayerProfile profile = PlayerProfile.getInstance();
        maxUnlocked = profile.maxUnlockedLevel;
        if (!"P1".equals(game.selectedCharacter) && !"P2".equals(game.selectedCharacter)) {
            game.selectedCharacter = profile.lastCharacter;
        }
        if (NetworkManager.getInstance() != null) {
            NetworkManager.getInstance().myCharacter = "P2".equals(game.selectedCharacter) ? 2 : 1;
        }
        // Mặc định: ải chơi gần nhất ("chơi tiếp")
        int pre = game.selectedLevel > 1 ? game.selectedLevel : profile.lastStoryLevel;
        selectedLevel = MathUtils.clamp(pre, 1, maxUnlocked);

        Texture bg = GameAssets.background("chonmanchoi.png");
        if (bg == null) bg = GameAssets.background("background1.jpg");
        if (bg != null) {
            Image bgImage = new Image(bg);
            bgImage.setSize(1280, 720);
            bgImage.setScaling(Scaling.fill);
            stage.addActor(bgImage);
        }
        Image dim = new Image(UiKit.solid(new Color(0.02f, 0.02f, 0.04f, 0.55f)));
        dim.setFillParent(true);
        stage.addActor(dim);

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(10);

        String titleText = "CHỌN SÁT THỦ & MÀN CHƠI";
        String subText = "Vàng: " + profile.gold + "     Kỷ lục: " + profile.bestScore + " điểm     Đã mở: "
            + maxUnlocked + "/" + StoryChapterConfig.TOTAL_STAGES + " ải";

        if (customMap > 0) {
            String mapName = (customMap == 1) ? "THÀNH CỔ THỜI CHIẾN" : ((customMap == 2) ? "BẾN CẢNG BAN ĐÊM" : "PHÁO ĐÀI TRÊN NÚI");
            titleText = "CHỌN SÁT THỦ — CHẾ ĐỘ TÙY CHỌN";
            subText = "BẢN ĐỒ ĐÃ CHỌN: MÀN " + customMap + " · " + mapName;
        } else if (isNewStory) {
            titleText = "CỐT TRUYỆN — CHỌN SÁT THỦ";
            subText = "BẮT ĐẦU CHIẾN DỊCH MỚI TỪ MÀN 1 ẢI 1 (TIẾN TRÌNH TUẦN TỰ)";
        }

        Label title = new Label(titleText, new Label.LabelStyle(UiKit.display(), UiKit.GOLD));
        title.setFontScale(0.6f);
        root.add(title).padBottom(0).row();
        root.add(lbl(subText, UiKit.body(), 0.6f, UiKit.MUTED)).padBottom(10).row();

        // ---- 2 thẻ nhân vật + vòng sáng khi chọn ----
        frameP1 = new Image(UiKit.framed(new Color(0, 0, 0, 0), UiKit.GOLD));
        frameP2 = new Image(UiKit.framed(new Color(0, 0, 0, 0), UiKit.GOLD));
        haloP1 = new UiKit.HeroHalo(new Color(0.42f, 0.92f, 1.00f, 1f), 1.46f);
        haloP2 = new UiKit.HeroHalo(new Color(1.00f, 0.34f, 0.64f, 1f), 1.46f);
        Table heroes = new Table();
        cardP1 = heroCard("P1", "p1.jpg", "VÂN TIÊU", "Bạch Y Kiếm Thánh", frameP1, haloP1,
            "Phong Lôi & Cực Hạn Kiếm Khí (Z: Đại Kiếm Khí Bán Nguyệt · X: Kiếm Thiền Hộ Thể · C: Vạn Kiếm Triều Tông)\nTuyệt kỹ (U): TRẤN THIÊN KIẾM THẦN – trảm sát lôi bão hủy diệt.",
            4, 3, 5);
        cardP2 = heroCard("P2", "p2.jpg", "VÂN U", "Hắc Dạ Sát Thủ", frameP2, haloP2,
            "Thuộc tính Hắc Ám & Huyết Độc (Z: Huyết Tiêu Đoạt Hồn · X: Ảnh Phân Thân & Hoán Đổi · C: Hắc Ám Huyết Trận)\nTuyệt kỹ (U): SÁT CHIÊU HOÀN HẢO – ẩn thân áp sát kết liễu điện ảnh.",
            4, 5, 4);
        heroes.add(cardP1).padRight(56);
        heroes.add(cardP2);
        root.add(heroes).padBottom(10).row();

        lblHero = lbl("", UiKit.body(), 0.6f, UiKit.TEXT);
        root.add(lblHero).padBottom(8).row();

        // Nếu ở chế độ Tùy Chọn hoặc Chơi Mới Cốt Truyện -> không cần chọn màn nhảy cóc
        if (customMap == 0 && !isNewStory) {
            // ---- Chọn màn chơi ----
            Table chRow = new Table();
            chRow.add(lbl("MÀN", UiKit.bodyBold(), 0.62f, UiKit.GOLD)).width(90).left();
            for (int i = 0; i < CH; i++) {
                final int c = i + 1;
                TextButton b = smallButton(String.valueOf(c));
                b.setDisabled(StoryChapterConfig.indexOf(c, 1) > maxUnlocked);
                UiKit.onClick(b, () -> selectLevel(firstPlayable(c)));
                chapterButtons[i] = b;
                chRow.add(b).size(56, 40).padRight(8);
            }
            root.add(chRow).padBottom(6).row();

            // ---- Chọn ải trong màn ----
            Table stRow = new Table();
            stRow.add(lbl("ẢI", UiKit.bodyBold(), 0.62f, UiKit.GOLD)).width(90).left();
            for (int i = 0; i < ST; i++) {
                final int s = i + 1;
                TextButton b = smallButton(String.valueOf(s));
                UiKit.onClick(b, () -> selectLevel(StoryChapterConfig.indexOf(StoryChapterConfig.chapterOf(selectedLevel), s)));
                stageButtons[i] = b;
                stRow.add(b).size(56, 40).padRight(8);
            }
            root.add(stRow).padBottom(8).row();
        }

        lblStage = lbl("", UiKit.bodyBold(), 0.68f, UiKit.GOLD);
        root.add(lblStage).padBottom(2).row();
        lblObjective = lbl("", UiKit.body(), 0.58f, UiKit.TEXT);
        root.add(lblObjective).padBottom(10).row();

        Table actionRow = new Table();
        if (customMap > 0) {
            btnStart = new TextButton("VÀO TRẬN (MÀN " + customMap + ") (Enter)", UiKit.buttonStyle(UiKit.GREEN, UiKit.buttonFont()));
            UiKit.onClick(btnStart, this::startGame);
            btnStart.getLabel().setFontScale(0.78f);
            actionRow.add(btnStart).size(420, 50);
        } else if (isNewStory) {
            btnStart = new TextButton("BẮT ĐẦU CHIẾN DỊCH (MÀN 1 ẢI 1) (Enter)", UiKit.buttonStyle(UiKit.GREEN, UiKit.buttonFont()));
            UiKit.onClick(btnStart, this::startGame);
            btnStart.getLabel().setFontScale(0.72f);
            actionRow.add(btnStart).size(540, 50);
        } else {
            if (StorySaveManager.hasSave()) {
                int chSave = StorySaveManager.getSavedChapter();
                int stSave = StorySaveManager.getSavedStage();
                TextButton btnContinue = new TextButton("CHƠI TIẾP (MÀN " + chSave + " · ẢI " + stSave + ")", UiKit.buttonStyle(UiKit.GOLD, UiKit.buttonFont()));
                btnContinue.getLabel().setFontScale(0.70f);
                UiKit.onClick(btnContinue, () -> {
                    game.freePlayMode = false;
                    int lvl = StoryChapterConfig.indexOf(chSave, stSave);
                    game.selectedCharacter = StorySaveManager.getSavedCharacter();
                    game.selectedLevel = lvl;
                    GameScreen gs = new GameScreen(game, GameScreen.GameMode.SOLO, lvl);
                    gs.applySaveData(StorySaveManager.getSavedHp(), StorySaveManager.getSavedGold(),
                        StorySaveManager.getSavedScore(), StorySaveManager.getSavedPotions(),
                        StorySaveManager.getSavedCheckpointX());
                    game.setScreen(gs);
                });
                actionRow.add(btnContinue).size(250, 50).padRight(12);

                TextButton btnNewGame = new TextButton("CHƠI MỚI (ẢI 1-1)", UiKit.buttonStyle(new Color(0.85f, 0.25f, 0.15f, 1f), UiKit.buttonFont()));
                btnNewGame.getLabel().setFontScale(0.70f);
                UiKit.onClick(btnNewGame, () -> {
                    StorySaveManager.clearSave();
                    game.setScreen(CharacterSelectScreen.forNewStory(game));
                });
                actionRow.add(btnNewGame).size(210, 50).padRight(12);
            }
            btnStart = new TextButton("VÀO ẢI " + selectedLevel + " (Enter)", UiKit.buttonStyle(UiKit.GREEN, UiKit.buttonFont()));
            UiKit.onClick(btnStart, this::startGame);
            btnStart.getLabel().setFontScale(0.78f);
            actionRow.add(btnStart).size(StorySaveManager.hasSave() ? 210 : 490, 50);
        }
        root.add(actionRow);
        stage.addActor(root);

        TextButton btnBack = new TextButton("QUAY LẠI (Esc)", UiKit.buttonStyle(UiKit.GRAY, UiKit.bodyBold()));
        btnBack.getLabel().setFontScale(0.68f);
        btnBack.setBounds(24, 660, 168, 42);
        UiKit.onClick(btnBack, () -> {
            if (customMap > 0) {
                game.setScreen(new MapSelectScreen(game));
            } else {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        stage.addActor(btnBack);

        selectLevel(selectedLevel);
        selectHero(game.selectedCharacter);
    }

    private Label lbl(String text, BitmapFont font, float scale, Color c) {
        Label l = new Label(text, new Label.LabelStyle(font, c));
        l.setFontScale(scale);
        return l;
    }

    private TextButton smallButton(String text) {
        TextButton b = new TextButton(text, UiKit.buttonStyle(UiKit.GRAY, UiKit.bodyBold()));
        b.getLabel().setFontScale(0.75f);
        return b;
    }

    /** Ải đầu tiên chơi được của chương c (ải mới nhất nếu chương đang dở). */
    private int firstPlayable(int c) {
        int last = StoryChapterConfig.indexOf(c, ST);
        return Math.min(last, Math.max(StoryChapterConfig.indexOf(c, 1), Math.min(maxUnlocked, last)));
    }

    private Table heroCard(String id, String portrait, String name, String title, Image frame,
                           UiKit.HeroHalo halo, String desc, int speed, int damage, int ult) {
        Table card = new Table();
        card.setBackground(UiKit.framed(new Color(0.05f, 0.06f, 0.09f, 0.88f), UiKit.GOLD_DIM));
        card.pad(12, 16, 10, 16);

        Stack stack = new Stack();
        stack.add(halo);
        Texture tex = GameAssets.background(portrait);
        Image img = (tex != null) ? new Image(tex) : new Image(UiKit.solid(UiKit.GRAY));
        img.setScaling(Scaling.fill);
        Table imgPad = new Table();
        imgPad.add(img).size(104, 104).pad(5);
        stack.add(imgPad);
        stack.add(frame);
        card.add(stack).size(114, 114).padTop(4).padBottom(6).row();

        Table nameRow = new Table();
        nameRow.add(lbl(name, UiKit.bodyBold(), 0.8f, UiKit.GOLD)).padRight(8);
        nameRow.add(lbl(title, UiKit.body(), 0.6f, UiKit.MUTED));
        card.add(nameRow).padBottom(2).row();
        Label d = lbl(desc, UiKit.body(), 0.5f, UiKit.TEXT);
        d.setAlignment(Align.center);
        d.setWrap(true); // mô tả kỹ năng dài: tự xuống dòng, không đẩy thẻ tràn ra ngoài màn hình
        card.add(d).width(470).padBottom(4).row();
        card.add(statRow("Tốc độ", speed)).left().row();
        card.add(statRow("Sát thương", damage)).left().row();
        card.add(statRow("Tuyệt kỹ", ult)).left();

        UiKit.onClick(card, () -> selectHero(id));
        return card;
    }

    private Table statRow(String name, int value) {
        Table row = new Table();
        row.add(lbl(name, UiKit.body(), 0.5f, UiKit.MUTED)).width(96).left();
        for (int i = 0; i < 5; i++) {
            Image pip = new Image(UiKit.rounded(i < value ? UiKit.GOLD : new Color(0.25f, 0.26f, 0.30f, 1f)));
            row.add(pip).size(30, 8).padRight(4);
        }
        return row;
    }

    private void selectHero(String id) {
        game.selectedCharacter = id;
        if (NetworkManager.getInstance() != null) {
            NetworkManager.getInstance().myCharacter = "P2".equals(id) ? 2 : 1;
        }
        boolean p1 = "P1".equals(id);
        frameP1.setVisible(p1);
        frameP2.setVisible(!p1);
        if (haloP1 != null) haloP1.setVisible(p1);
        if (haloP2 != null) haloP2.setVisible(!p1);
        if (cardP1 != null) {
            cardP1.setBackground(UiKit.framed(
                p1 ? new Color(0.08f, 0.14f, 0.22f, 0.94f) : new Color(0.05f, 0.06f, 0.09f, 0.86f),
                p1 ? UiKit.GOLD : UiKit.GOLD_DIM));
        }
        if (cardP2 != null) {
            cardP2.setBackground(UiKit.framed(
                !p1 ? new Color(0.18f, 0.07f, 0.14f, 0.94f) : new Color(0.05f, 0.06f, 0.09f, 0.86f),
                !p1 ? UiKit.GOLD : UiKit.GOLD_DIM));
        }
        lblHero.setText(!p1
            ? "Đã chọn: VÂN U – Hắc Ám & Huyết Độc (con đường Báo Thù)"
            : "Đã chọn: VÂN TIÊU – Phong & Kiếm Khí (con đường Chính Nghĩa)");
    }

    private void selectLevel(int index) {
        selectedLevel = MathUtils.clamp(index, 1, maxUnlocked);
        game.selectedLevel = selectedLevel;
        int ch = StoryChapterConfig.chapterOf(selectedLevel), st = StoryChapterConfig.stageOf(selectedLevel);
        for (int i = 0; i < CH; i++) {
            TextButton b = chapterButtons[i];
            if (b == null) continue;
            if (b.isDisabled()) continue;
            restyle(b, i + 1 == ch ? UiKit.ORANGE : UiKit.GRAY);
        }
        for (int i = 0; i < ST; i++) {
            TextButton b = stageButtons[i];
            if (b == null) continue;
            boolean unlocked = StoryChapterConfig.indexOf(ch, i + 1) <= maxUnlocked;
            b.setDisabled(!unlocked);
            restyle(b, i + 1 == st ? UiKit.ORANGE : UiKit.GRAY);
        }
        if (customMap > 0) {
            String mapName = (customMap == 1) ? "THÀNH CỔ THỜI CHIẾN" : ((customMap == 2) ? "BẾN CẢNG BAN ĐÊM" : "PHÁO ĐÀI TRÊN NÚI");
            if (lblStage != null) {
                lblStage.setText("BẢN ĐỒ ĐÃ CHỌN: MÀN " + customMap + " — " + mapName);
            }
            if (lblObjective != null) {
                lblObjective.setText("Khám phá địa hình & chiến đấu phong cách trung cổ cổ điển   •   [A/D, 1/2] Chọn tướng · [Enter] Vào trận");
            }
            if (btnStart != null) {
                btnStart.setText("VÀO TRẬN (MÀN " + customMap + ") (Enter)");
            }
        } else if (isNewStory) {
            StoryChapterConfig.StageData d = StoryChapterConfig.get(1);
            if (lblStage != null) {
                lblStage.setText("Màn 1: " + d.chapterTitle + "  ·  Ải 1: " + d.stageTitle);
            }
            if (lblObjective != null) {
                lblObjective.setText("Mục tiêu: " + d.objectiveText + "   •   [A/D, 1/2] Chọn tướng · [Enter] Bắt đầu");
            }
            if (btnStart != null) {
                btnStart.setText("BẮT ĐẦU CHIẾN DỊCH (MÀN 1 ẢI 1) (Enter)");
            }
        } else {
            StoryChapterConfig.StageData d = StoryChapterConfig.get(selectedLevel);
            if (lblStage != null) {
                lblStage.setText("Màn " + ch + ": " + d.chapterTitle + "  ·  Ải " + st + ": " + d.stageTitle
                    + (d.hasBoss ? "  (TRÙM)" : ""));
            }
            if (lblObjective != null) {
                lblObjective.setText("Mục tiêu: " + d.objectiveText + "   •   [A/D, 1/2] Tướng · [W/S] Ải · [Q/E] Màn");
            }
            if (btnStart != null) {
                btnStart.setText("BẮT ĐẦU  ·  " + d.shortLabel().toUpperCase() + " (Enter)");
            }
        }
    }

    private void restyle(TextButton b, Color c) {
        float scale = b.getLabel().getFontScaleX();
        b.setStyle(UiKit.buttonStyle(c, UiKit.bodyBold()));
        b.getLabel().setFontScale(scale);
    }

    private void startGame() {
        if (!"P1".equals(game.selectedCharacter) && !"P2".equals(game.selectedCharacter)) return;
        if (NetworkManager.getInstance() != null) {
            NetworkManager.getInstance().myCharacter = "P2".equals(game.selectedCharacter) ? 2 : 1;
        }
        PlayerProfile p = PlayerProfile.getInstance();
        p.lastCharacter = game.selectedCharacter;
        p.lastStoryLevel = selectedLevel;
        p.save();
        SoundManager.getInstance().playHeavyHit();
        game.freePlayMode = customMap > 0;
        if (customMap > 0) {
            game.selectedOnlineMap = customMap;
            int lvl = StoryChapterConfig.indexOf(customMap, 1);
            game.setScreen(new GameScreen(game, GameScreen.GameMode.SOLO, lvl));
        } else if (isNewStory) {
            StorySaveManager.clearSave();
            game.setScreen(new GameScreen(game, GameScreen.GameMode.SOLO, 1));
        } else {
            game.setScreen(new GameScreen(game, GameScreen.GameMode.SOLO, selectedLevel));
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (warmupFrames > 0) {
            warmupFrames--;
        } else if (!UiKit.wasModalKeyConsumedThisFrame()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
                if (customMap > 0) {
                    game.setScreen(new MapSelectScreen(game));
                } else {
                    game.setScreen(new MainMenuScreen(game));
                }
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                selectHero("P1");
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                selectHero("P2");
            }
            if (customMap == 0 && !isNewStory) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                    selectLevel(selectedLevel + 1);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                    selectLevel(selectedLevel - 1);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.Q) || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
                    selectLevel(selectedLevel - ST);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
                    selectLevel(selectedLevel + ST);
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                startGame();
                return;
            }
        }

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (stage != null) stage.dispose();
    }
}
