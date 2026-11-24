package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Màn hình chính kiểu "game launcher":
 *  - Trái: nút kiểu Minecraft (Chơi đơn / Chơi mạng / biểu tượng / Cài đặt | Thoát), chọn bằng chuột hoặc phím, Enter để mở.
 *  - Trên trái: tên + #ID, dòng dưới là trạng thái trực tuyến (bấm vào -> Bạn bè, đổi tên ở đó).
 *  - Phải: thẻ chế độ nổi bật (5v5), thẻ tiến độ (ải, kỷ lục, vàng).
 *  - Chữ vàng nghiêng nhấp nháy cạnh logo, dòng phiên bản ở góc dưới.
 */
public class MainMenuScreen extends ScreenAdapter implements NetworkManager.FriendsUpdateListener {
    public static final String VERSION = BuildInfo.VERSION;

    private static final float W = 1280f, H = 720f;
    private static final Color ACCENT = new Color(1.00f, 0.34f, 0.36f, 1f);
    private static final Color SPLASH = new Color(1.00f, 0.92f, 0.20f, 1f);
    private static final Color SOFT = new Color(0.93f, 0.91f, 0.87f, 1f);
    private static final Color DIM = new Color(0.70f, 0.70f, 0.74f, 1f);
    private static final Color CARD_BG = new Color(0.05f, 0.06f, 0.09f, 0.82f);
    private static final Color CARD_LINE = new Color(1f, 1f, 1f, 0.22f);

    private static final String[] SPLASHES = {
        "Săn! Leo! Chiến! Sống sót!",
        "Giờ đã có 5 đấu 5!",
        "Rủ bạn chơi Co-op đi!",
        "Đừng nhìn xuống vực!",
        "Bước vào Hư Không...",
        "Bóng tối không chờ ai!",
        "Ải 5 khó lắm đấy!",
    };

    private final Main game;
    private Stage stage;
    private final Array<Texture> ownedTextures = new Array<>();
    private final Array<Pixmap> ownedPixmaps = new Array<>();
    private TextureRegion texWhite, texHGrad, texVGrad, texCircle, texDiamond;

    private Group mainPanel, playPanel, activePanel;
    private final com.badlogic.gdx.utils.ObjectMap<Group, Array<MenuButton>> panelButtons = new com.badlogic.gdx.utils.ObjectMap<>();
    private Array<MenuButton> buttons = new Array<>();
    private MenuButton btnFriends;
    private int selected = -1;
    private TextureRegion texPeople, texNews;
    private Label lblDesc, lblName, lblId, lblNet, lblInitial, lblProgress, lblBest;
    private String shownName = "";
    private ProgressBarActor progressBar;
    private Image netDot;
    private Group identity;
    private Group splashGroup;
    private float time = 0f, netCheckTimer = 0f;
    private boolean disposed = false;
    private int baseActorCount = Integer.MAX_VALUE;
    private boolean enterArmed = false;

    public MainMenuScreen(Main game) {
        this.game = game;
    }

    // =====================================================================
    // DỰNG GIAO DIỆN
    // =====================================================================
    @Override
    public void show() {
        SoundManager.getInstance().playMenuBgm();
        stage = new Stage(new FitViewport(W, H));
        Gdx.input.setInputProcessor(stage);
        buildTextures();

        // 1. Nền (ảnh có sẵn logo game ở giữa phía trên)
        Texture bg = GameAssets.background("background.jpg");
        if (bg != null) {
            Image img = new Image(bg);
            img.setSize(W, H);
            img.setScaling(Scaling.fill);
            stage.addActor(img);
        }
        // Lớp tối: trái đậm (menu), phải nhẹ (thẻ), trên & dưới để chữ nổi
        addShade(texHGrad, 0, 0, 700, H, new Color(0.02f, 0.02f, 0.05f, 0.88f), false);
        addShade(texHGrad, W - 470, 0, 470, H, new Color(0.02f, 0.02f, 0.05f, 0.70f), true);
        addShade(texVGrad, 0, H - 120, W, 120, new Color(0.02f, 0.02f, 0.05f, 0.75f), true);
        addShade(texVGrad, 0, 0, W, 150, new Color(0.02f, 0.02f, 0.05f, 0.35f), false); // nhẹ để thấy dòng © và tên nhóm in trong ảnh nền

        buildSplash();
        buildMenu();
        buildIdentity();
        buildCards();

        Label ver = new Label("Shadow Edict: Ám Lệnh " + VERSION, new Label.LabelStyle(UiKit.body(), new Color(1f, 1f, 1f, 0.55f)));
        ver.setFontScale(0.55f);
        ver.pack();
        ver.setPosition(14, 34); // nằm trên dòng "© 2026 ElysiBlooMCT" in sẵn trong ảnh nền
        stage.addActor(ver);

        // Phím laptop: ↑↓ ←→ / W A S D / Tab chọn, 1..6 chọn nhanh, F mở Bạn bè, Enter / Space mở, Esc quay lại
        stage.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (hasModalOpen() || UiKit.wasModalKeyConsumedThisFrame() || buttons.size == 0) return false;
                if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_6) {
                    int idx = keycode - Input.Keys.NUM_1;
                    if (idx < buttons.size) {
                        select(idx);
                        enterArmed = true;
                        return true;
                    }
                    return false;
                }
                switch (keycode) {
                    case Input.Keys.UP: case Input.Keys.W: case Input.Keys.LEFT: case Input.Keys.A:
                        select((selected - 1 + buttons.size) % buttons.size); return true;
                    case Input.Keys.DOWN: case Input.Keys.S: case Input.Keys.RIGHT: case Input.Keys.D: case Input.Keys.TAB:
                        select((selected + 1) % buttons.size); return true;
                    case Input.Keys.F:
                        openFriends(); return true;
                    case Input.Keys.ESCAPE: case Input.Keys.BACK:
                        if (activePanel == playPanel) { showPanel(mainPanel); return true; }
                        if (keycode == Input.Keys.BACK) { Main.exitGame(); return true; }
                        return false;
                    case Input.Keys.ENTER: case Input.Keys.SPACE: case Input.Keys.NUMPAD_ENTER:
                        enterArmed = true; // chỉ mở khi NHẢ phím, tránh màn kế tiếp nhận luôn cú Enter này
                        return true;
                }
                return false;
            }

            @Override public boolean keyUp(InputEvent event, int keycode) {
                boolean isConfirmKey = (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE
                    || keycode == Input.Keys.NUMPAD_ENTER
                    || (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_6));
                if (!isConfirmKey) return false;
                boolean armed = enterArmed;
                enterArmed = false;
                if (!armed || hasModalOpen() || UiKit.wasModalKeyConsumedThisFrame() || selected < 0 || selected >= buttons.size) return false;
                buttons.get(selected).press();
                return true;
            }
        });

        refreshNet();
        refreshProfile();
        updateFriendBadge();
        baseActorCount = stage.getActors().size;

        NetworkManager nm = NetworkManager.getInstance();
        nm.updateMyPresence("ONLINE", "", "");
        nm.addFriendsListener(this);
        nm.setMatchInviteListener(invite -> MatchInviteDialog.showOn(stage, game, invite));
    }

    /** Hộp thoại (cài đặt/bạn bè/lời mời/có gì mới) được thêm vào sau cùng -> số actor tăng lên. */
    private boolean hasModalOpen() {
        return stage.getActors().size > baseActorCount;
    }

    private void addShade(TextureRegion region, float x, float y, float w, float h, Color c, boolean flip) {
        TextureRegion r = new TextureRegion(region);
        if (flip) r.flip(region == texHGrad, region == texVGrad);
        Image img = new Image(r);
        img.setBounds(x, y, w, h);
        img.setColor(c);
        img.setTouchable(Touchable.disabled);
        stage.addActor(img);
    }

    /** Chữ vàng nghiêng nhấp nháy như kiểu "splash text", đặt cạnh logo của ảnh nền. */
    private void buildSplash() {
        Label l = new Label(SPLASHES[MathUtils.random(SPLASHES.length - 1)], new Label.LabelStyle(UiKit.bodyBold(), SPLASH));
        l.setFontScale(0.80f);
        l.pack();
        Label shadow = new Label(l.getText(), new Label.LabelStyle(UiKit.bodyBold(), new Color(0.25f, 0.22f, 0f, 1f)));
        shadow.setFontScale(0.80f);
        shadow.pack();
        shadow.setPosition(2, -2);

        splashGroup = new Group();
        splashGroup.setTransform(true);
        splashGroup.setSize(l.getWidth(), l.getHeight());
        splashGroup.setOrigin(Align.center);
        splashGroup.addActor(shadow);
        splashGroup.addActor(l);
        splashGroup.setPosition(1010 - l.getWidth() / 2f, H - 128); // góc dưới-phải logo ảnh nền
        splashGroup.setRotation(16f);
        splashGroup.setTouchable(Touchable.disabled);
        stage.addActor(splashGroup);
    }

    // ---------------------------------------------------------------- MENU NÚT BÊN TRÁI
    // Kiểu Minecraft: 2 nút lớn (Chơi đơn / Chơi mạng), hàng nút biểu tượng, hàng Cài đặt | Thoát.
    // "Chơi mạng" mở bảng con: 5v5 / Đấu trường / Hợp tác / Quay lại.
    private static final float COL_X = 44f, COL_W = 400f, BTN_H = 56f, GAP = 10f;

    private void buildMenu() {
        // ===== Bảng chính =====
        mainPanel = new Group();
        Array<MenuButton> main = new Array<>();
        boolean hasSave = StorySaveManager.hasSave();
        float y = hasSave ? 505f : 475f;
        float btnHeight = hasSave ? 46f : 52f;
        float btnGap = hasSave ? 8f : 10f;
        if (hasSave) {
            int ch = StorySaveManager.getSavedChapter();
            int st = StorySaveManager.getSavedStage();
            String hero = StorySaveManager.getSavedCharacter();
            int lvl = StoryChapterConfig.indexOf(ch, st);
            main.add(addButton(mainPanel, "TIẾP TỤC (MÀN " + ch + " · ẢI " + st + ")", null,
                "Tiếp tục hành trình còn dở dang với " + ("P2".equals(hero) ? "Vân U" : "Vân Tiêu") + " tại Màn " + ch + " · Ải " + st + ".",
                0, y, COL_W, btnHeight, () -> {
                    game.selectedCharacter = hero;
                    game.selectedLevel = lvl;
                    game.freePlayMode = false;
                    GameScreen gs = new GameScreen(game, GameScreen.GameMode.SOLO, lvl);
                    gs.applySaveData(StorySaveManager.getSavedHp(), StorySaveManager.getSavedGold(),
                        StorySaveManager.getSavedScore(), StorySaveManager.getSavedPotions(),
                        StorySaveManager.getSavedCheckpointX());
                    game.setScreen(gs);
                }));
            y -= btnHeight + btnGap;

            main.add(addButton(mainPanel, "CHƠI MỚI (CỐT TRUYỆN)", null, "Bắt đầu lại chiến dịch từ Màn 1 Ải 1 (chơi tuần tự).",
                0, y, COL_W, btnHeight, () -> game.setScreen(CharacterSelectScreen.forNewStory(game))));
            y -= btnHeight + btnGap;
        } else {
            main.add(addButton(mainPanel, "CỐT TRUYỆN (CHƠI MỚI)", null, "Chiến dịch Huyết Lệ Hoàng Triều: 3 màn chơi, 9 ải tuần tự.",
                0, y, COL_W, btnHeight, () -> game.setScreen(CharacterSelectScreen.forNewStory(game))));
            y -= btnHeight + btnGap;
        }

        main.add(addButton(mainPanel, "CHỌN MÀN CHƠI", null, "Mở bản đồ cổ, tự do chọn 1 trong 3 map rồi chọn tướng vào trận.",
            0, y, COL_W, btnHeight, () -> game.setScreen(new MapSelectScreen(game))));
        y -= btnHeight + btnGap;

        main.add(addButton(mainPanel, "CHƠI MẠNG", null, "Đại chiến 5v5, đấu 1v1 hoặc hợp tác 2 người.",
            0, y, COL_W, btnHeight, () -> showPanel(playPanel)));
        y -= btnHeight + btnGap;
        float iw = (COL_W - btnGap) / 2f;
        btnFriends = addButton(mainPanel, null, texPeople, "Bạn bè: tìm bằng mã ID, xem ai đang online, mời vào trận.",
            0, y, iw, btnHeight, this::openFriends);
        main.add(btnFriends);
        main.add(addButton(mainPanel, null, texNews, "Có gì mới trong phiên bản " + VERSION + ".",
            iw + btnGap, y, iw, btnHeight, this::openPatchNotes));
        y -= btnHeight + btnGap + 14f; // khoảng hở trước hàng cuối giống Minecraft
        float hw = (COL_W - btnGap) / 2f;
        main.add(addButton(mainPanel, "CÀI ĐẶT", null, "Đổi tên, âm nhạc, âm thanh và xem phím điều khiển.",
            0, y, hw, btnHeight, this::openSettings));
        main.add(addButton(mainPanel, "THOÁT", null, "Thoát game.", hw + btnGap, y, hw, btnHeight, () -> Main.exitGame()));
        panelButtons.put(mainPanel, main);

        // ===== Bảng "Chơi mạng" =====
        playPanel = new Group();
        Array<MenuButton> play = new Array<>();
        Image dia = new Image(texDiamond);
        dia.setColor(ACCENT);
        dia.setBounds(2, 452 + BTN_H + 20, 12, 12);
        playPanel.addActor(dia);
        Label head = new Label("CHƠI MẠNG", new Label.LabelStyle(UiKit.bodyBold(), DIM));
        head.setFontScale(0.7f);
        head.pack();
        head.setPosition(22, 452 + BTN_H + 14);
        playPanel.addActor(head);
        y = 452f;
        play.add(addButton(playPanel, "ĐẠI CHIẾN 5V5", null, "Hai phe 5 người: phá trụ, giành rune, hạ Titan cổ đại.",
            0, y, COL_W, BTN_H, () -> game.setScreen(new Arena5v5LobbyScreen(game))));
        y -= BTN_H + GAP;
        play.add(addButton(playPanel, "ĐẤU TRƯỜNG 1V1", null, "Đấu trực tuyến 1 đấu 1 với người chơi khác.",
            0, y, COL_W, BTN_H, () -> game.setScreen(new LobbyScreen(game))));
        y -= BTN_H + GAP;
        play.add(addButton(playPanel, "HỢP TÁC 2 NGƯỜI", null, "Hai người cùng vượt ải, hồi sinh cho nhau.",
            0, y, COL_W, BTN_H, () -> game.setScreen(new CoopLobbyScreen(game))));
        y -= BTN_H + GAP + 18f;
        play.add(addButton(playPanel, "QUAY LẠI", null, "Về menu chính (Esc).", 0, y, hw, BTN_H, () -> showPanel(mainPanel)));
        panelButtons.put(playPanel, play);

        for (Group g : new Group[]{mainPanel, playPanel}) {
            g.setBounds(COL_X, 0, COL_W, H);
            g.setTouchable(Touchable.childrenOnly);
            stage.addActor(g);
        }
        playPanel.setVisible(false);

        lblDesc = new Label("", new Label.LabelStyle(UiKit.body(), DIM));
        lblDesc.setFontScale(0.6f);
        lblDesc.setPosition(COL_X + 4, 180);
        stage.addActor(lblDesc);

        // Hiệu ứng vào lần đầu: các nút trượt từ trái, lần lượt
        int i = 0;
        for (MenuButton b : main) {
            float tx = b.getX();
            b.getColor().a = 0f;
            b.setX(tx - 30);
            b.addAction(Actions.sequence(Actions.delay(0.05f * i++),
                Actions.parallel(Actions.fadeIn(0.28f), Actions.moveTo(tx, b.getY(), 0.32f, Interpolation.pow2Out))));
        }
        activePanel = mainPanel;
        buttons = main;
        select(0);
    }

    private MenuButton addButton(Group panel, String text, TextureRegion icon, String desc,
                                 float x, float y, float w, float h, Runnable action) {
        MenuButton b = new MenuButton(text, icon, desc, action);
        b.setBounds(x, y, w, h);
        panel.addActor(b);
        return b;
    }

    private void showPanel(Group target) {
        if (target == activePanel) return;
        Group old = activePanel;
        old.clearActions();
        old.addAction(Actions.sequence(Actions.parallel(Actions.fadeOut(0.15f), Actions.moveTo(COL_X - 24, 0, 0.15f)),
            Actions.visible(false)));
        target.clearActions();
        target.setVisible(true);
        target.getColor().a = 0f;
        target.setX(COL_X + 24);
        target.addAction(Actions.parallel(Actions.fadeIn(0.2f), Actions.moveTo(COL_X, 0, 0.22f, Interpolation.pow2Out)));
        activePanel = target;
        buttons = panelButtons.get(target);
        select(0);
    }

    private void select(int index) {
        selected = index;
        for (int i = 0; i < buttons.size; i++) buttons.get(i).selected = (i == index);
        if (lblDesc != null && index >= 0 && index < buttons.size) {
            lblDesc.setText(buttons.get(index).desc);
            lblDesc.clearActions();
            lblDesc.getColor().a = 0f;
            lblDesc.addAction(Actions.fadeIn(0.18f));
        }
    }

    // ---------------------------------------------------------------- TÊN + TRẠNG THÁI (góc trên trái)
    private void buildIdentity() {
        identity = new Group();
        float av = 46f;
        Image avatar = new Image(texCircle);
        avatar.setSize(av, av);
        avatar.setColor(ACCENT.r * 0.55f, ACCENT.g * 0.55f, ACCENT.b * 0.55f, 1f);
        identity.addActor(avatar);
        Image ring = new Image(texCircle);
        ring.setBounds(-2, -2, av + 4, av + 4);
        ring.setColor(1f, 1f, 1f, 0.25f);
        identity.addActorAt(0, ring);
        lblInitial = new Label("", new Label.LabelStyle(UiKit.bodyBold(), Color.WHITE));
        lblInitial.setFontScale(0.85f);
        lblInitial.setAlignment(Align.center);
        lblInitial.setBounds(0, 1, av, av);
        identity.addActor(lblInitial);

        lblName = new Label("", new Label.LabelStyle(UiKit.bodyBold(), SOFT));
        lblName.setFontScale(0.78f);
        lblName.setPosition(av + 12, 22);
        identity.addActor(lblName);
        lblId = new Label("", new Label.LabelStyle(UiKit.body(), DIM));
        lblId.setFontScale(0.56f);
        identity.addActor(lblId);

        netDot = new Image(texCircle);
        netDot.setBounds(av + 13, 6, 9, 9);
        identity.addActor(netDot);
        lblNet = new Label("", new Label.LabelStyle(UiKit.body(), DIM));
        lblNet.setFontScale(0.58f);
        identity.addActor(lblNet);

        identity.setSize(320, av);
        identity.setPosition(28, H - av - 22);
        identity.addListener(new ClickListener() { // bấm vào danh tính/avatar -> mở hồ sơ người chơi
            @Override public void clicked(InputEvent event, float x, float y) {
                SoundManager.getInstance().playSlash();
                PlayerProfile p = PlayerProfile.getInstance();
                PlayerProfileDialog.show(stage, game, p.userId, p.playerName, "P2".equals(p.lastCharacter) ? 2 : 1, "", "");
            }
        });
        stage.addActor(identity);
    }

    // ---------------------------------------------------------------- THẺ BÊN PHẢI
    private void buildCards() {
        float x = 880f, w = 320f;

        // Thẻ nổi bật: Đại chiến 5v5
        Card feature = new Card(x, 196, w, 312, () -> game.setScreen(new Arena5v5LobbyScreen(game)));
        Texture titan = GameAssets.exists("thulinhulti.png") ? GameAssets.sprite("thulinhulti.png") : null;
        feature.art = titan != null ? new TextureRegion(titan) : null;
        feature.artTint.set(1f, 1f, 1f, 1f);
        feature.fill.set(0.30f, 0.05f, 0.07f, 0.90f);

        Label tag = new Label("CHẾ ĐỘ NỔI BẬT", new Label.LabelStyle(UiKit.bodyBold(), SOFT));
        tag.setFontScale(0.50f);
        tag.pack();
        Table tagBox = new Table();
        tagBox.setBackground(UiKit.solid(new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.92f)));
        tagBox.add(tag).pad(3, 8, 3, 8);
        tagBox.pack();
        tagBox.setPosition(12, 312 - tagBox.getHeight() - 12);
        feature.addActor(tagBox);

        Label ft = new Label("ĐẠI CHIẾN 5V5", new Label.LabelStyle(UiKit.display(), UiKit.GOLD));
        ft.setFontScale(0.58f);
        ft.pack();
        ft.setPosition((w - ft.getPrefWidth()) / 2f, 52);
        feature.addActor(ft);
        Label fs = new Label("10 người  ///  Giành bùa  ///  Phục kích", new Label.LabelStyle(UiKit.body(), SOFT));
        fs.setFontScale(0.52f);
        fs.pack();
        fs.setPosition((w - fs.getPrefWidth()) / 2f, 24);
        feature.addActor(fs);
        stage.addActor(feature);

        // Thẻ tiến độ
        Card progress = new Card(x, 98, w, 88, () -> game.setScreen(new CharacterSelectScreen(game)));
        Label pt = new Label("CỐT TRUYỆN", new Label.LabelStyle(UiKit.bodyBold(), SOFT));
        pt.setFontScale(0.6f);
        pt.pack();
        pt.setPosition(16, 52);
        progress.addActor(pt);
        lblProgress = new Label("", new Label.LabelStyle(UiKit.bodyBold(), UiKit.GOLD));
        lblProgress.setFontScale(0.6f);
        progress.addActor(lblProgress);
        progressBar = new ProgressBarActor();
        progressBar.setBounds(16, 34, w - 32, 7);
        progress.addActor(progressBar);
        lblBest = new Label("", new Label.LabelStyle(UiKit.body(), DIM));
        lblBest.setFontScale(0.52f);
        lblBest.setPosition(16, 8);
        progress.addActor(lblBest);
        stage.addActor(progress);

        float d = 0.25f;
        for (Card c : new Card[]{feature, progress}) {
            c.getColor().a = 0f;
            c.setX(x + 30);
            c.addAction(Actions.sequence(Actions.delay(d), Actions.parallel(Actions.fadeIn(0.35f),
                Actions.moveTo(x, c.getY(), 0.4f, Interpolation.pow2Out))));
            d += 0.08f;
        }
    }

    private void updateFriendBadge() {
        if (btnFriends != null) btnFriends.badge = !NetworkManager.getInstance().pendingFriendRequests.isEmpty();
    }

    // =====================================================================
    // HÀNH ĐỘNG
    // =====================================================================
    private void openFriends() {
        new FriendsDialog(stage, game, "", "");
    }

    private void openSettings() {
        SettingsDialog.show(stage, game, this::refreshProfile);
    }

    private void openPatchNotes() {
        Image dim = UiKit.dimOverlay(0.6f);
        stage.addActor(dim);
        Table panel = new Table();
        panel.setBackground(UiKit.framed(new Color(0.06f, 0.07f, 0.10f, 0.97f), UiKit.GOLD_DIM));
        panel.pad(22, 28, 20, 28);
        Label title = new Label("CÓ GÌ MỚI  •  v" + VERSION, new Label.LabelStyle(UiKit.display(), UiKit.GOLD));
        title.setFontScale(0.5f);
        panel.add(title).left().padBottom(12).row();
        String[] notes = {
            "Cốt truyện mới: 3 màn chơi (Thành Cổ, Bến Cảng, Pháo Đài) x 3 ải, có lưu & chơi tiếp.",
            "Chọn màn chơi tự do trên bản đồ cổ (không ảnh hưởng tiến độ cốt truyện).",
            "Bám mép, bám tường, sàn sập, checkpoint, vũ khí cung / súng / song đao.",
            "Đại chiến 5v5 trực tuyến tới 10 người, chọn phe, chủ phòng chọn bản đồ.",
            "Chat chữ và đàm thoại (micro) trong trận trực tuyến.",
            "Hồ sơ chiến binh, kết bạn bằng mã ID, mời vào trận.",
            "Sửa lỗi: phòng trực tuyến không vào được trận khi máy chủ chưa cập nhật.",
        };
        for (String n : notes) {
            Table row = new Table();
            Image d = new Image(texDiamond);
            d.setColor(ACCENT);
            row.add(d).size(9, 9).padRight(10).top().padTop(9);
            Label l = new Label(n, new Label.LabelStyle(UiKit.body(), SOFT));
            l.setFontScale(0.62f);
            l.setWrap(true);
            row.add(l).width(560).left();
            panel.add(row).left().padBottom(6).row();
        }
        TextButton close = new TextButton("ĐÓNG (Esc)", UiKit.buttonStyle(UiKit.GRAY, UiKit.bodyBold()));
        close.getLabel().setFontScale(0.7f);
        final InputListener[] patchKeyHolder = new InputListener[1];
        final Runnable closePatchNotes = () -> {
            if (patchKeyHolder[0] != null) stage.removeCaptureListener(patchKeyHolder[0]);
            panel.remove();
            dim.remove();
        };
        patchKeyHolder[0] = new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (!UiKit.isTopModal(stage, panel)) return false;
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK
                    || keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER || keycode == Input.Keys.SPACE) {
                    UiKit.consumeModalKey();
                    event.stop();
                    closePatchNotes.run();
                    return true;
                }
                return false;
            }
        };
        stage.addCaptureListener(patchKeyHolder[0]);
        UiKit.onClick(close, closePatchNotes);
        panel.add(close).width(200).height(44).padTop(12);
        panel.pack();
        panel.setPosition((W - panel.getWidth()) / 2f, (H - panel.getHeight()) / 2f);
        panel.getColor().a = 0f;
        panel.addAction(Actions.fadeIn(0.2f));
        stage.addActor(panel);
    }

    private void refreshProfile() {
        PlayerProfile p = PlayerProfile.getInstance();
        shownName = p.playerName == null ? "" : p.playerName;
        if (game != null && !shownName.isEmpty()) game.playerName = shownName;
        if (lblName != null) {
            lblName.setText(shownName);
            lblName.pack();
            lblId.setText("#" + p.userId);
            lblId.pack();
            lblId.setPosition(lblName.getX() + lblName.getPrefWidth() + 8, lblName.getY() + 3);
            lblInitial.setText(shownName.isEmpty() ? "?" : shownName.substring(0, 1).toUpperCase());
        }
        if (lblProgress != null) {
            lblProgress.setText(StoryChapterConfig.get(p.maxUnlockedLevel).shortLabel());
            lblProgress.pack();
            lblProgress.setPosition(320 - 16 - lblProgress.getPrefWidth(), 52);
        }
        if (progressBar != null) progressBar.value = p.maxUnlockedLevel / (float) PlayerProfile.MAX_LEVEL;
        if (lblBest != null) {
            String gold = TextFmt.groupThousands(p.gold);
            lblBest.setText((p.bestScore > 0 ? "Kỷ lục: " + p.bestScore + " điểm" : "Chưa có kỷ lục") + "   •   Vàng: " + gold);
        }
    }

    private void refreshNet() {
        boolean online = NetworkManager.getInstance().isConnected();
        String text = online ? "Trực tuyến" : "Ngoại tuyến";
        lblNet.setText(text);
        lblNet.pack();
        lblNet.setPosition(netDot.getRight() + 6, netDot.getY() + netDot.getHeight() / 2f - lblNet.getHeight() / 2f);
        lblNet.setColor(online ? SOFT : DIM);
        netDot.setColor(online ? UiKit.SUCCESS : new Color(0.55f, 0.55f, 0.58f, 1f));
    }

    @Override
    public void onFriendsUpdated() {
        if (disposed) return;
        updateFriendBadge();
        refreshProfile();
    }

    @Override public void onSearchResultsUpdated() {}

    @Override
    public void onFriendRequestReceived(String requesterId, String requesterName) {
        if (disposed) return;
        updateFriendBadge();
    }

    // =====================================================================
    // VÒNG LẶP
    // =====================================================================
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        time += delta;

        netCheckTimer += delta;
        if (netCheckTimer >= 1f) {
            netCheckTimer = 0f;
            refreshNet();
            // Tên có thể vừa đổi trong hộp thoại Bạn bè
            if (!shownName.equals(PlayerProfile.getInstance().playerName)) refreshProfile();
        }
        if (splashGroup != null) {
            // Nhịp "đập" giống splash text: phóng to/thu nhỏ theo hàm |sin|
            float s = 1f + 0.06f * Math.abs(MathUtils.sin(time * MathUtils.PI * 1.6f));
            splashGroup.setScale(s);
        }

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    @Override
    public void hide() {
        NetworkManager.getInstance().removeFriendsListener(this);
        NetworkManager.getInstance().setMatchInviteListener(null);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        NetworkManager.getInstance().removeFriendsListener(this);
        if (stage != null) stage.dispose();
        for (Texture t : ownedTextures) t.dispose();
        for (Pixmap p : ownedPixmaps) p.dispose();
        ownedTextures.clear();
        ownedPixmaps.clear();
    }

    // =====================================================================
    // TEXTURE THỦ TỤC (tự vẽ, khử răng cưa 4x4, managed để Android mất GL context vẫn nạp lại)
    // =====================================================================
    private interface Shape { float cover(float x, float y); } // x,y trong [-1,1]; trả 0..1

    private TextureRegion procedural(int size, Shape shape) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float a = 0f;
                for (int sy = 0; sy < 4; sy++) for (int sx = 0; sx < 4; sx++) {
                    float x = ((px + (sx + 0.5f) / 4f) / size) * 2f - 1f;
                    float y = ((py + (sy + 0.5f) / 4f) / size) * 2f - 1f;
                    a += shape.cover(x, y);
                }
                pm.drawPixel(px, py, Color.rgba8888(1f, 1f, 1f, a / 16f));
            }
        }
        return new TextureRegion(managed(pm));
    }

    private Texture managed(Pixmap pm) {
        ownedPixmaps.add(pm);
        Texture t = new Texture(new PixmapTextureData(pm, null, false, false, true));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        ownedTextures.add(t);
        return t;
    }

    private void buildTextures() {
        texWhite = UiKit.whiteRegion();

        Pixmap h = new Pixmap(256, 1, Pixmap.Format.RGBA8888);
        h.setBlending(Pixmap.Blending.None);
        for (int x = 0; x < 256; x++) {
            float t = 1f - x / 255f;
            h.drawPixel(x, 0, Color.rgba8888(1f, 1f, 1f, t * t * (3f - 2f * t)));
        }
        texHGrad = new TextureRegion(managed(h)); // đậm bên trái -> trong suốt bên phải

        Pixmap v = new Pixmap(1, 256, Pixmap.Format.RGBA8888);
        v.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < 256; y++) {
            float t = 1f - y / 255f; // hàng 0 của pixmap = mép TRÊN khi vẽ -> alpha 0
            v.drawPixel(0, y, Color.rgba8888(1f, 1f, 1f, 1f - t * t * (3f - 2f * t)));
        }
        texVGrad = new TextureRegion(managed(v)); // vẽ bình thường: trong suốt phía trên -> đậm phía dưới

        texCircle = procedural(64, (x, y) -> x * x + y * y <= 0.94f ? 1f : 0f);
        texDiamond = procedural(48, (x, y) -> Math.abs(x) + Math.abs(y) <= 0.96f ? 1f : 0f);
        // Biểu tượng nút (toạ độ procedural: y = -1 là mép TRÊN -> đổi dấu để "lên trên" là dương)
        texPeople = procedural(64, (x, y) -> {
            float u = -y;
            float front = person(x + 0.20f, u, 1f);
            float back = person((x - 0.38f) / 0.85f, (u - 0.06f) / 0.85f, 1f);
            return Math.max(front, back * 0.55f);
        });
        texNews = procedural(64, (x, y) -> {
            float u = -y;
            if (Math.abs(x) > 0.62f || Math.abs(u) > 0.82f) return 0f;
            boolean border = Math.abs(x) > 0.48f || Math.abs(u) > 0.68f;
            boolean line = Math.abs(x) < 0.34f && (Math.abs(u - 0.38f) < 0.07f || Math.abs(u - 0.05f) < 0.07f
                || (Math.abs(u + 0.28f) < 0.07f && x < 0.12f));
            return (border || line) ? 1f : 0f;
        });
    }

    /** Hình người: đầu tròn + vai bán elip. */
    private static float person(float x, float u, float k) {
        if (x * x + (u - 0.38f) * (u - 0.38f) <= 0.27f * 0.27f) return k;
        if (u <= 0.02f && u >= -0.80f) {
            float ex = x / 0.55f, ey = (u + 0.80f) / 0.78f;
            if (ex * ex + ey * ey <= 1f) return k;
        }
        return 0f;
    }

    // =====================================================================
    // ACTOR TỰ VẼ
    // =====================================================================
    /**
     * Nút menu: nền tối trong mờ + viền mảnh + bóng đổ dưới chân (kiểu nút Minecraft, tông Valorant).
     * Được chọn (rê chuột / phím) -> nền ửng đỏ, viền đỏ, vạch nhấn bên trái; bấm -> lún xuống 2px.
     */
    private final class MenuButton extends Actor {
        final String text;
        TextureRegion icon;
        String desc;
        final Runnable action;
        boolean selected, badge;
        private float hl = 0f;
        private final ClickListener click;
        private final Color tmp = new Color();

        MenuButton(String text, TextureRegion icon, String desc, Runnable action) {
            this.text = text;
            this.icon = icon;
            this.desc = desc;
            this.action = action;
            setTouchable(Touchable.enabled);
            click = new ClickListener() {
                @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (pointer == -1) selectThis(); // rê chuột (desktop)
                }
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectThis();
                    press();
                }
            };
            addListener(click);
        }

        private void selectThis() {
            int i = buttons.indexOf(this, true);
            if (i >= 0 && i != MainMenuScreen.this.selected) select(i);
        }

        void press() {
            SoundManager.getInstance().playSlash();
            action.run();
        }

        @Override public void act(float delta) {
            super.act(delta);
            hl = MathUtils.lerp(hl, selected ? 1f : 0f, Math.min(1f, delta * 16f));
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            float a = getColor().a * parentAlpha;
            float x = getX(), w = getWidth(), h = getHeight();
            float y = getY() - (click.isPressed() ? 2f : 0f);

            // bóng đổ dưới chân
            batch.setColor(0f, 0f, 0f, 0.45f * a);
            batch.draw(texWhite, x + 2, y - 4, w, 4);
            // nền
            tmp.set(0.07f, 0.08f, 0.11f, 0.80f).lerp(0.30f, 0.07f, 0.09f, 0.92f, hl);
            batch.setColor(tmp.r, tmp.g, tmp.b, tmp.a * a);
            batch.draw(texWhite, x, y, w, h);
            // vệt sáng mép trên (tạo cảm giác nổi)
            batch.setColor(1f, 1f, 1f, (0.06f + 0.06f * hl) * a);
            batch.draw(texWhite, x, y + h - 3, w, 2);
            // viền
            tmp.set(1f, 1f, 1f, 0.22f).lerp(ACCENT.r, ACCENT.g, ACCENT.b, 1f, hl);
            batch.setColor(tmp.r, tmp.g, tmp.b, tmp.a * a);
            batch.draw(texWhite, x, y, w, 1);
            batch.draw(texWhite, x, y + h - 1, w, 1);
            batch.draw(texWhite, x, y, 1, h);
            batch.draw(texWhite, x + w - 1, y, 1, h);
            // vạch nhấn bên trái
            if (hl > 0.01f) {
                batch.setColor(ACCENT.r, ACCENT.g, ACCENT.b, a);
                batch.draw(texWhite, x, y, 4f * hl, h);
            }

            float textA = MathUtils.lerp(0.88f, 1f, hl) * a;
            if (icon != null) {
                float s = 28f;
                batch.setColor(0f, 0f, 0f, 0.5f * a);
                batch.draw(icon, x + (w - s) / 2f + 1.5f, y + (h - s) / 2f - 1.5f, s, s);
                tmp.set(SOFT).lerp(Color.WHITE, hl);
                batch.setColor(tmp.r, tmp.g, tmp.b, textA);
                batch.draw(icon, x + (w - s) / 2f, y + (h - s) / 2f, s, s);
            }
            if (badge) {
                batch.setColor(ACCENT.r, ACCENT.g, ACCENT.b, a);
                batch.draw(texCircle, x + w - 18, y + h - 18, 12, 12);
            }
            if (text != null) {
                BitmapFont font = UiKit.buttonFont();
                float ty = y + h / 2f + font.getCapHeight() / 2f;
                font.setColor(0f, 0f, 0f, 0.6f * a);
                font.draw(batch, text, x + 2, ty - 2, w, Align.center, false);
                tmp.set(SOFT).lerp(Color.WHITE, hl);
                font.setColor(tmp.r, tmp.g, tmp.b, textA);
                font.draw(batch, text, x, ty, w, Align.center, false);
                font.setColor(Color.WHITE);
            }
            batch.setColor(Color.WHITE);
        }
    }

    /** Thẻ bên phải: nền + (ảnh minh hoạ) + viền; rê chuột thì viền sáng & nhích lên. */
    private final class Card extends Group {
        TextureRegion art;
        final Color artTint = new Color(1f, 1f, 1f, 1f);
        final Color fill = new Color(CARD_BG);
        private float hover = 0f, hoverTarget = 0f;
        private final float baseY;

        Card(float x, float y, float w, float h, Runnable onClick) {
            setBounds(x, y, w, h);
            baseY = y;
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) { hoverTarget = 1f; }
                @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (toActor == null || !toActor.isDescendantOf(Card.this)) hoverTarget = 0f;
                }
                @Override public void clicked(InputEvent event, float x, float y) {
                    SoundManager.getInstance().playSlash();
                    onClick.run();
                }
            });
        }

        @Override public void act(float delta) {
            super.act(delta);
            hover = MathUtils.lerp(hover, hoverTarget, Math.min(1f, delta * 12f));
            if (getActions().size == 0) setY(baseY + 3f * hover);
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            float a = getColor().a * parentAlpha;
            float x = getX(), y = getY(), w = getWidth(), h = getHeight();

            batch.setColor(fill.r, fill.g, fill.b, fill.a * a);
            batch.draw(texWhite, x, y, w, h);

            if (art != null) {
                // Ảnh vuông đặt sát mép trên, rộng bằng thẻ; nửa dưới phủ gradient để chữ dễ đọc
                float size = Math.min(w, h);
                batch.setColor(artTint.r, artTint.g, artTint.b, artTint.a * a);
                batch.draw(art, x + (w - size) / 2f, y + h - size, size, size);
                batch.setColor(0.02f, 0.02f, 0.04f, 0.95f * a);
                batch.draw(texVGrad, x, y, w, h * 0.55f); // đậm ở đáy, mờ dần lên trên
            }

            // Viền 1px, sáng lên khi rê chuột
            float la = MathUtils.lerp(CARD_LINE.a, 0.9f, hover);
            batch.setColor(MathUtils.lerp(1f, ACCENT.r, hover), MathUtils.lerp(1f, ACCENT.g, hover),
                MathUtils.lerp(1f, ACCENT.b, hover), la * a);
            batch.draw(texWhite, x, y, w, 1);
            batch.draw(texWhite, x, y + h - 1, w, 1);
            batch.draw(texWhite, x, y, 1, h);
            batch.draw(texWhite, x + w - 1, y, 1, h);
            batch.setColor(Color.WHITE);

            super.draw(batch, parentAlpha);
        }
    }

    /** Thanh tiến độ mảnh (nền xám + phần đã đạt màu vàng). */
    private final class ProgressBarActor extends Actor {
        float value = 0f, shown = 0f;

        @Override public void act(float delta) {
            super.act(delta);
            shown = MathUtils.lerp(shown, value, Math.min(1f, delta * 5f));
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            float a = parentAlpha;
            batch.setColor(1f, 1f, 1f, 0.15f * a);
            batch.draw(texWhite, getX(), getY(), getWidth(), getHeight());
            batch.setColor(UiKit.GOLD.r, UiKit.GOLD.g, UiKit.GOLD.b, a);
            batch.draw(texWhite, getX(), getY(), getWidth() * MathUtils.clamp(shown, 0f, 1f), getHeight());
            batch.setColor(Color.WHITE);
        }
    }
}
