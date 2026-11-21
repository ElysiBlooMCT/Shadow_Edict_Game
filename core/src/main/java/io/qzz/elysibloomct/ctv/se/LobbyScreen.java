package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Phòng chờ online 2 người, dùng chung cho PVP 1v1 và CO-OP (CoopLobbyScreen chỉ là lớp con).
 *
 * Sửa so với bản cũ:
 *  - Không còn tự sinh "mã phòng giả" khi chưa có phản hồi server.
 *  - Gỡ toàn bộ listener socket khi rời màn hình (trước đây Co-op để sót "startGame"
 *    nên có thể bị kéo vào trận mới ngay giữa trận).
 *  - Thông báo lỗi server không còn ép kiểu (String) gây crash.
 *  - Nút Quay lại / ESC đều rời phòng và cập nhật trạng thái bạn bè.
 */
public class LobbyScreen extends ScreenAdapter {
    private static final String[] ROOM_EVENTS = {"roomCreated", "roomJoined", "updateRoomState", "playerJoined", "playerLeft", "startGame", "errorMsg"};

    protected final Main game;
    private final boolean coopMode;
    private final String modeKey;
    private Stage stage;
    private GameSocket socket;

    private Label lblRoomInfo, lblStatus, lblNet;
    private Label lblP1Status, lblP2Status;
    private Image imgP1Avatar, imgP2Avatar;
    private Table slotP1, slotP2;
    private String p1UserId = "", p2UserId = "";
    private String p1Name = "", p2Name = "";
    private int p1Char = 1, p2Char = 2;
    private float roomSyncTimer = 0f;

    private TextField txtRoomCode;
    private TextButton btnReady, btnMapSelect;
    private Image frameHero1, frameHero2;
    private UiKit.HeroHalo haloHero1, haloHero2;

    private boolean isReady = false;
    private boolean p2Joined = false;
    private boolean p2Ready = false;
    private boolean pendingRejoin = false;
    private boolean hostLegacyReadySent = false;
    private boolean leaving = false;
    private boolean disposed = false;
    private float netCheckTimer = 0f;
    private int baseActorCount = Integer.MAX_VALUE;
    private int warmupFrames = 2;

    public LobbyScreen(Main game) {
        this(game, false);
    }

    protected LobbyScreen(Main game, boolean coopMode) {
        this.game = game;
        this.coopMode = coopMode;
        this.modeKey = coopMode ? "coop" : "1v1";
    }

    @Override
    public void show() {
        SoundManager.getInstance().playMenuBgm();
        NetworkManager nm = NetworkManager.getInstance();
        socket = nm.socket;
        nm.ensureConnected();

        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        buildUI();
        setupSocketEvents();

        // Quay lại từ trận đấu hoặc vào bằng lời mời -> tự vào lại phòng cũ
        if (!nm.currentRoomCode.isEmpty()) {
            lblRoomInfo.setText("MÃ PHÒNG: " + nm.currentRoomCode);
            pendingRejoin = true;
            tryRejoin();
        }
    }

    private void buildUI() {
        Texture bg = GameAssets.background("background1.jpg");
        if (bg != null) {
            Image bgImg = new Image(bg);
            bgImg.setSize(1280, 720);
            bgImg.setScaling(Scaling.fill);
            stage.addActor(bgImg);
        }
        Image dim = new Image(UiKit.solid(new Color(0.02f, 0.03f, 0.05f, 0.62f)));
        dim.setFillParent(true);
        stage.addActor(dim);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Table panel = new Table();
        panel.setBackground(UiKit.panel());
        panel.pad(22, 34, 22, 34);

        Label lblTitle = UiKit.label(coopMode ? "SẢNH HỢP TÁC 2 NGƯỜI" : "ĐẤU TRƯỜNG 1V1 TRỰC TUYẾN", UiKit.large(), UiKit.GOLD);
        lblNet = UiKit.label("", UiKit.small(), UiKit.MUTED);
        Table head = new Table();
        head.add(lblTitle).expandX().left();
        head.add(lblNet).right();
        panel.add(head).fillX().padBottom(6).row();

        lblRoomInfo = UiKit.label("CHƯA VÀO PHÒNG", UiKit.large(), Color.WHITE);
        panel.add(lblRoomInfo).padBottom(14).row();

        // --- Hàng tạo / vào phòng ---
        TextButton btnCreate = UiKit.button("TẠO PHÒNG (C)", UiKit.GREEN);
        txtRoomCode = new TextField("", UiKit.textFieldStyle());
        TextInputBridge.attach(txtRoomCode, "Nhập mã phòng");
        txtRoomCode.setMessageText("Mã (Tab)");
        txtRoomCode.setMaxLength(8);
        txtRoomCode.setAlignment(Align.center);
        TextButton btnJoin = UiKit.button("VÀO PHÒNG", UiKit.BLUE);
        TextButton btnInvite = UiKit.button("+ MỜI BẠN (F)", UiKit.PURPLE);

        Table roomRow = new Table();
        roomRow.add(btnCreate).size(190, 50).padRight(10);
        roomRow.add(txtRoomCode).size(145, 50).padRight(8);
        roomRow.add(btnJoin).size(155, 50).padRight(10);
        roomRow.add(btnInvite).size(170, 50);
        panel.add(roomRow).padBottom(18).row();

        // --- Chọn tướng (thẻ có ảnh + vòng sáng) ---
        panel.add(UiKit.label("CHỌN SÁT THỦ  ([A/1] Vân Tiêu  •  [D/2] Vân U)", UiKit.normal(), UiKit.GOLD_DIM)).padBottom(12).row();
        Table heroes = new Table();
        frameHero1 = new Image(UiKit.framed(new Color(0, 0, 0, 0), UiKit.GOLD));
        frameHero2 = new Image(UiKit.framed(new Color(0, 0, 0, 0), UiKit.GOLD));
        haloHero1 = new UiKit.HeroHalo(new Color(0.42f, 0.92f, 1.00f, 1f), 1.44f);
        haloHero2 = new UiKit.HeroHalo(new Color(1.00f, 0.34f, 0.64f, 1f), 1.44f);
        heroes.add(heroCard("p1.jpg", "VÂN TIÊU", frameHero1, haloHero1, 1)).padRight(42);
        heroes.add(heroCard("p2.jpg", "VÂN U", frameHero2, haloHero2, 2));
        panel.add(heroes).padBottom(14).row();
        refreshHeroHighlight();

        // --- Trạng thái người chơi trong sảnh (hiển thị thẻ người chơi, bấm để xem hồ sơ) ---
        Table slots = new Table();
        slots.setBackground(UiKit.rounded(UiKit.PANEL_LIGHT));
        slots.pad(10, 14, 10, 14);

        slotP1 = new Table();
        slotP1.setBackground(UiKit.rounded(new Color(0.04f, 0.05f, 0.08f, 0.85f)));
        slotP1.pad(6, 10, 6, 10);
        Texture t1 = GameAssets.background("p1.jpg");
        imgP1Avatar = (t1 != null) ? new Image(t1) : new Image(UiKit.solid(UiKit.GRAY));
        imgP1Avatar.setScaling(Scaling.fill);
        lblP1Status = UiKit.label("Chưa vào phòng (Hãy tạo phòng hoặc nhập mã phòng)", UiKit.normal(), UiKit.MUTED);
        slotP1.add(imgP1Avatar).size(44, 44).padRight(12);
        slotP1.add(lblP1Status).expandX().left();
        UiKit.onClick(slotP1, () -> {
            if (!p1UserId.isEmpty() || (NetworkManager.getInstance().myPlayerIndex == 1)) {
                PlayerProfileDialog.show(stage, game, p1UserId.isEmpty() ? PlayerProfile.getInstance().userId : p1UserId,
                    p1Name.isEmpty() ? game.playerName : p1Name, p1Char, NetworkManager.getInstance().currentRoomCode, modeKey);
            }
        });

        slotP2 = new Table();
        slotP2.setBackground(UiKit.rounded(new Color(0.04f, 0.05f, 0.08f, 0.85f)));
        slotP2.pad(6, 10, 6, 10);
        Texture t2 = GameAssets.background("p2.jpg");
        imgP2Avatar = (t2 != null) ? new Image(t2) : new Image(UiKit.solid(UiKit.GRAY));
        imgP2Avatar.setScaling(Scaling.fill);
        imgP2Avatar.setColor(0.4f, 0.4f, 0.4f, 0.6f);
        lblP2Status = UiKit.label("P2: Đang chờ " + (coopMode ? "đồng đội" : "đối thủ") + " tham gia... [CHỜ]", UiKit.normal(), UiKit.MUTED);
        slotP2.add(imgP2Avatar).size(44, 44).padRight(12);
        slotP2.add(lblP2Status).expandX().left();
        UiKit.onClick(slotP2, () -> {
            if (!p2UserId.isEmpty() || (NetworkManager.getInstance().myPlayerIndex == 2)) {
                PlayerProfileDialog.show(stage, game, p2UserId.isEmpty() ? PlayerProfile.getInstance().userId : p2UserId,
                    p2Name.isEmpty() ? game.playerName : p2Name, p2Char, NetworkManager.getInstance().currentRoomCode, modeKey);
            }
        });

        slots.add(slotP1).fillX().padBottom(6).row();
        slots.add(slotP2).fillX();
        panel.add(slots).width(620).padBottom(16).row();

        // --- Sẵn sàng / chọn bản đồ / quay lại ---
        btnMapSelect = UiKit.button("BẢN ĐỒ: THÀNH CỔ (1)", UiKit.GOLD_DIM, UiKit.normal());
        btnReady = UiKit.button("SẴN SÀNG (Enter)", UiKit.ORANGE, UiKit.large());
        btnReady.setDisabled(true);
        TextButton btnBack = UiKit.button("RỜI PHÒNG (Esc)", UiKit.RED);
        Table bottom = new Table();
        bottom.add(btnMapSelect).size(230, 58).padRight(12);
        bottom.add(btnReady).size(240, 58).padRight(12);
        bottom.add(btnBack).size(150, 58);
        panel.add(bottom).padBottom(10).row();
        updateMapButtonText();

        lblStatus = UiKit.label("Tạo phòng mới (C) hoặc bấm Tab nhập mã phòng để vào.", UiKit.normal(), UiKit.WARNING);
        panel.add(lblStatus);

        root.add(panel);
        stage.addActor(root);
        baseActorCount = stage.getActors().size;
        updateNetStatus();

        // ===== Sự kiện =====
        UiKit.onClick(btnCreate, this::createRoom);

        UiKit.onClick(btnJoin, this::joinTypedRoom);
        txtRoomCode.setTextFieldListener((field, c) -> {
            if (c == '\r' || c == '\n') {
                stage.setKeyboardFocus(null);
                joinTypedRoom();
            }
        });

        UiKit.onClick(btnInvite, this::openInviteDialog);
        UiKit.onClick(btnMapSelect, this::cycleMap);
        UiKit.onClick(btnReady, this::toggleReadyState);

        UiKit.onClick(btnBack, this::leaveToMenu);
    }

    private void createRoom() {
        NetworkManager nm = NetworkManager.getInstance();
        if (!nm.isConnected()) {
            nm.ensureConnected();
            setStatus("Chưa kết nối máy chủ! Đang thử kết nối lại...", UiKit.DANGER);
            return;
        }
        if (!nm.currentRoomCode.isEmpty()) nm.leaveRoom();
        JSONObject data = new JSONObject();
        try {
            data.put("type", modeKey);
            data.put("name", (game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần");
            data.put("team", 1);
            data.put("charId", nm.myCharacter);
            data.put("userId", PlayerProfile.getInstance().userId);
        } catch (Exception ignored) {}
        socket.emit("createRoom", data);
        setStatus("Đang tạo phòng...", UiKit.WARNING);
    }

    private void openInviteDialog() {
        String code = NetworkManager.getInstance().currentRoomCode;
        if (code.isEmpty()) {
            setStatus("Hãy tạo phòng trước khi mời bạn!", UiKit.WARNING);
            return;
        }
        new FriendsDialog(stage, game, code, modeKey);
    }

    private void toggleReadyState() {
        NetworkManager nm = NetworkManager.getInstance();
        if (!nm.isConnected() || nm.currentRoomCode.isEmpty()) {
            setStatus("Bạn chưa ở trong phòng nào!", UiKit.DANGER);
            return;
        }
        if (nm.myPlayerIndex == 1) {
            // Chủ phòng: chỉ bấm bắt đầu khi người chơi 2 đã tham gia và sẵn sàng
            if (p2Joined && p2Ready) {
                SoundManager.getInstance().playCoin();
                socket.emit("startGame");
                if (nm.isLegacyServer() && !hostLegacyReadySent) {
                    // Máy chủ bản cũ bỏ qua "startGame": trận chỉ bắt đầu khi CẢ HAI bấm Sẵn sàng.
                    hostLegacyReadySent = true;
                    socket.emit("toggleReady");
                }
                setStatus("Đang khởi tạo trận đấu...", UiKit.SUCCESS);
            } else if (!p2Joined) {
                setStatus("Chưa có người chơi nào tham gia phòng!", UiKit.WARNING);
            } else {
                setStatus("Vui lòng chờ người chơi 2 bấm Sẵn sàng!", UiKit.WARNING);
            }
        } else {
            // Người chơi 2 (Khách): bấm toggle Sẵn sàng
            SoundManager.getInstance().playCoin();
            isReady = !isReady;
            updateReadyButton();
            socket.emit("toggleReady");
        }
    }

    private boolean hasModalOpen() {
        return stage != null && stage.getActors().size > baseActorCount;
    }

    private Table heroCard(String portrait, String name, Image frame, UiKit.HeroHalo halo, int charId) {
        Table card = new Table();
        Stack stack = new Stack();
        stack.add(halo);
        Texture tex = GameAssets.background(portrait);
        Image img = (tex != null) ? new Image(tex) : new Image(UiKit.solid(UiKit.GRAY));
        img.setScaling(Scaling.fill);
        Table imgPad = new Table();
        imgPad.add(img).size(116, 116).pad(5);
        stack.add(imgPad);
        stack.add(frame);
        card.add(stack).size(126, 126).padTop(8).padBottom(6).row();
        card.add(UiKit.label(name, UiKit.normal(), UiKit.TEXT)).padTop(4);
        UiKit.onClick(card, () -> selectHero(charId));
        return card;
    }

    private void selectHero(int charId) {
        NetworkManager nm = NetworkManager.getInstance();
        nm.myCharacter = charId;
        game.selectedCharacter = (charId == 2) ? "P2" : "P1";
        refreshHeroHighlight();
        if (nm.isConnected()) socket.emit("selectCharacter", charId);
    }

    private void refreshHeroHighlight() {
        boolean dark = NetworkManager.getInstance().myCharacter == 2;
        frameHero1.setVisible(!dark);
        frameHero2.setVisible(dark);
        if (haloHero1 != null) haloHero1.setVisible(!dark);
        if (haloHero2 != null) haloHero2.setVisible(dark);
    }

    private void joinTypedRoom() {
        String code = txtRoomCode.getText().trim();
        if (code.isEmpty()) {
            setStatus("Vui lòng nhập mã phòng!", UiKit.WARNING);
            return;
        }
        NetworkManager nm = NetworkManager.getInstance();
        if (!nm.isConnected()) {
            nm.ensureConnected();
            setStatus("Chưa kết nối được máy chủ!", UiKit.DANGER);
            return;
        }
        JSONObject joinInfo = new JSONObject();
        try {
            joinInfo.put("type", modeKey);
            joinInfo.put("charId", nm.myCharacter);
            joinInfo.put("team", 2);
            joinInfo.put("userId", PlayerProfile.getInstance().userId);
            joinInfo.put("name", (game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần");
        } catch (Exception ignored) {}
        socket.emit("joinRoom", code, joinInfo);
        setStatus("Đang vào phòng " + code + "...", UiKit.WARNING);
    }

    private void tryRejoin() {
        NetworkManager nm = NetworkManager.getInstance();
        if (!pendingRejoin || !nm.isConnected()) return;
        pendingRejoin = false;
        JSONObject joinInfo = new JSONObject();
        try {
            joinInfo.put("type", modeKey);
            joinInfo.put("charId", nm.myCharacter);
            joinInfo.put("team", nm.myPlayerIndex == 2 ? 2 : 1);
            joinInfo.put("userId", PlayerProfile.getInstance().userId);
            joinInfo.put("name", (game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần");
        } catch (Exception ignored) {}
        socket.emit("joinRoom", nm.currentRoomCode, joinInfo);
        socket.emit("selectCharacter", nm.myCharacter);
        socket.emit("getRoomState");
        setStatus("Đang đồng bộ phòng " + nm.currentRoomCode + "...", UiKit.WARNING);
    }

    private void leaveToMenu() {
        if (leaving) return;
        leaving = true;
        cleanSocketListeners();
        NetworkManager.getInstance().leaveRoom();
        game.setScreen(new MainMenuScreen(game));
    }

    private void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setColor(color);
    }

    private void updateReadyButton() {
        NetworkManager nm = NetworkManager.getInstance();
        boolean inRoom = !nm.currentRoomCode.isEmpty();
        if (!inRoom) {
            btnReady.setText("CHƯA VÀO PHÒNG");
            btnReady.setStyle(UiKit.buttonStyle(UiKit.GRAY, UiKit.large()));
            UiKit.fitLabel(btnReady, 240f);
            btnReady.setDisabled(true);
            return;
        }

        if (nm.myPlayerIndex == 1) {
            // Chủ phòng: Phải chờ người chơi 2 tham gia và sẵn sàng thì nút mới sáng lên!
            if (p2Joined && p2Ready) {
                btnReady.setText("BẮT ĐẦU CHƠI (Enter)");
                btnReady.setStyle(UiKit.buttonStyle(UiKit.GREEN, UiKit.large()));
                UiKit.fitLabel(btnReady, 240f);
                btnReady.setDisabled(false);
            } else if (p2Joined) {
                btnReady.setText(coopMode ? "CHỜ ĐỒNG ĐỘI SẴN SÀNG..." : "CHỜ ĐỐI THỦ SẴN SÀNG...");
                btnReady.setStyle(UiKit.buttonStyle(UiKit.GRAY, UiKit.large()));
                UiKit.fitLabel(btnReady, 240f);
                btnReady.setDisabled(true);
            } else {
                btnReady.setText("CHỜ NGƯỜI CHƠI THAM GIA...");
                btnReady.setStyle(UiKit.buttonStyle(UiKit.GRAY, UiKit.large()));
                UiKit.fitLabel(btnReady, 240f);
                btnReady.setDisabled(true);
            }
        } else {
            // Người chơi 2 (Khách): Chỉ có quyền bấm SẴN SÀNG / HỦY SẴN SÀNG
            btnReady.setText(isReady ? "HỦY SẴN SÀNG (Enter)" : "SẴN SÀNG (Enter)");
            btnReady.setStyle(UiKit.buttonStyle(isReady ? UiKit.GREEN : UiKit.ORANGE, UiKit.large()));
            UiKit.fitLabel(btnReady, 240f);
            btnReady.setDisabled(false);
        }
    }

    private void updateNetStatus() {
        NetworkManager nm = NetworkManager.getInstance();
        boolean online = nm.isConnected();
        if (online && nm.isLegacyServer()) {
            lblNet.setText("● Máy chủ bản cũ – cần cập nhật server.js");
            lblNet.setColor(UiKit.WARNING);
            return;
        }
        lblNet.setText(online ? "● Trực tuyến" : "● Ngoại tuyến – đang kết nối...");
        lblNet.setColor(online ? UiKit.SUCCESS : UiKit.DANGER);
    }

    private void cleanSocketListeners() {
        if (socket == null) return;
        for (String ev : ROOM_EVENTS) socket.off(ev);
    }

    private static JSONObject asJson(Object[] args) {
        return (args != null && args.length > 0 && args[0] instanceof JSONObject) ? (JSONObject) args[0] : null;
    }

    private void setupSocketEvents() {
        if (socket == null) return;
        cleanSocketListeners();

        NetworkManager.getInstance().setMatchInviteListener(invite -> MatchInviteDialog.showOn(stage, game, invite));

        socket.on("roomCreated", args -> Gdx.app.postRunnable(() -> {
            JSONObject data = asJson(args);
            if (disposed || data == null) return;
            onEnterRoom(data.optString("roomCode", ""), data.optInt("playerIndex", 1));
            setStatus("Tạo phòng thành công! Gửi mã cho " + (coopMode ? "đồng đội." : "đối thủ."), UiKit.SUCCESS);
        }));

        socket.on("roomJoined", args -> Gdx.app.postRunnable(() -> {
            JSONObject data = asJson(args);
            if (disposed || data == null) return;
            onEnterRoom(data.optString("roomCode", ""), data.optInt("playerIndex", 2));
            setStatus("Vào phòng thành công! Chọn sát thủ rồi bấm Sẵn sàng.", UiKit.SUCCESS);
        }));

        socket.on("updateRoomState", args -> Gdx.app.postRunnable(() -> {
            JSONObject room = asJson(args);
            if (disposed || room == null) return;
            int sMap = room.optInt("selectedMap", 1);
            NetworkManager.getInstance().selectedMap = sMap;
            game.selectedOnlineMap = sMap;
            updateMapButtonText();
            applyRoomState(room.optJSONArray("players"));
        }));

        socket.on("playerJoined", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            JSONObject data = asJson(args);
            JSONObject room = data != null ? data.optJSONObject("room") : null;
            if (room != null && room.optJSONArray("players") != null) {
                applyRoomState(room.optJSONArray("players"));
            }
            JSONObject newP = data != null ? data.optJSONObject("player") : null;
            String joinedName = newP != null ? newP.optString("name", "Người chơi mới") : "Người chơi mới";
            SoundManager.getInstance().playSlash();
            setStatus("Chiến binh " + joinedName + " đã tham gia phòng!", UiKit.SUCCESS);
        }));

        socket.on("playerLeft", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            JSONObject data = asJson(args);
            JSONObject room = data != null ? data.optJSONObject("room") : null;
            if (room != null && room.optJSONArray("players") != null) {
                applyRoomState(room.optJSONArray("players"));
            } else {
                applyRoomState(new JSONArray());
            }
            isReady = false;
            hostLegacyReadySent = false; // máy chủ đặt lại trạng thái Sẵn sàng của mọi người
            updateReadyButton();
            setStatus("Người chơi đã rời phòng!", UiKit.WARNING);
        }));

        socket.on("startGame", args -> Gdx.app.postRunnable(() -> {
            if (disposed || leaving) return;
            JSONObject room = asJson(args);
            NetworkManager nm = NetworkManager.getInstance();
            int selectedMap = nm.selectedMap;
            if (room != null) {
                if (room.optJSONArray("players") != null) {
                    applyRoomState(room.optJSONArray("players"));
                }
                selectedMap = room.optInt("selectedMap", nm.selectedMap);
            }
            leaving = true;
            cleanSocketListeners();
            nm.selectedMap = selectedMap;
            game.selectedOnlineMap = selectedMap;
            nm.updateMyPresence("IN_GAME", nm.currentRoomCode, modeKey);
            game.selectedCharacter = (nm.myCharacter == 2) ? "P2" : "P1";
            game.setScreen(new GameScreen(game, coopMode ? GameScreen.GameMode.COOP : GameScreen.GameMode.PVP, selectedMap));
        }));

        socket.on("errorMsg", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            String msg = (args != null && args.length > 0 && args[0] != null) ? String.valueOf(args[0]) : "Lỗi không xác định";
            setStatus(msg, UiKit.DANGER);
        }));
    }

    private void onEnterRoom(String roomCode, int playerIndex) {
        if (roomCode == null || roomCode.isEmpty()) return;
        NetworkManager nm = NetworkManager.getInstance();
        nm.currentRoomCode = roomCode;
        nm.myPlayerIndex = playerIndex;
        nm.updateMyPresence("IN_ROOM", roomCode, modeKey);
        lblRoomInfo.setText("MÃ PHÒNG: " + roomCode);
        isReady = false;
        hostLegacyReadySent = false;
        updateReadyButton();
        if (nm.isConnected()) socket.emit("selectCharacter", nm.myCharacter);
        updateMapButtonText();
    }

    private void cycleMap() {
        NetworkManager nm = NetworkManager.getInstance();
        if (nm.myPlayerIndex != 1 && !nm.currentRoomCode.isEmpty()) {
            setStatus("Chỉ chủ phòng mới có quyền chọn bản đồ!", UiKit.WARNING);
            return;
        }
        int nextMap = (nm.selectedMap % 3) + 1;
        nm.selectedMap = nextMap;
        game.selectedOnlineMap = nextMap;
        updateMapButtonText();
        if (nm.isConnected() && !nm.currentRoomCode.isEmpty()) {
            socket.emit("selectMap", nextMap);
        }
        SoundManager.getInstance().playSlash();
    }

    private void updateMapButtonText() {
        if (btnMapSelect == null) return;
        int m = NetworkManager.getInstance().selectedMap;
        String mName = (m == 2) ? "BẾN CẢNG (2)" : ((m == 3) ? "PHÁO ĐÀI (3)" : "THÀNH CỔ (1)");
        boolean isHost = NetworkManager.getInstance().myPlayerIndex == 1 || NetworkManager.getInstance().currentRoomCode.isEmpty();
        btnMapSelect.setText("BẢN ĐỒ: " + mName + (isHost ? " [ĐỔI]" : ""));
        btnMapSelect.setStyle(UiKit.buttonStyle(isHost ? UiKit.GOLD_DIM : UiKit.GRAY, UiKit.bodyBold()));
        UiKit.fitLabel(btnMapSelect, 230f);
    }

    private void applyRoomState(JSONArray players) {
        if (players == null) return;
        NetworkManager nm = NetworkManager.getInstance();
        nm.syncRoomPlayers(players, PlayerProfile.getInstance().userId);

        p1UserId = ""; p2UserId = "";
        p1Name = ""; p2Name = "";
        p2Joined = false;
        p2Ready = false;

        String mySockId = (socket != null && socket.id() != null) ? socket.id() : "";

        for (int i = 0; i < 2; i++) {
            Label lbl = (i == 0) ? lblP1Status : lblP2Status;
            Image avatar = (i == 0) ? imgP1Avatar : imgP2Avatar;
            String role = (i == 0) ? "P1 (Chủ phòng)" : ("P2 (" + (coopMode ? "Đồng đội" : "Đối thủ") + ")");
            JSONObject p = (i < players.length()) ? players.optJSONObject(i) : null;
            if (p == null) {
                if (i == 0) {
                    lbl.setText("Chưa vào phòng (Hãy tạo phòng hoặc nhập mã phòng)");
                    lbl.setColor(UiKit.MUTED);
                } else {
                    lbl.setText("P2: Đang chờ " + (coopMode ? "đồng đội" : "đối thủ") + " tham gia... [CHỜ]");
                    lbl.setColor(UiKit.MUTED);
                    Texture p2Tex = GameAssets.background("p2.jpg");
                    if (p2Tex != null && avatar != null) avatar.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(p2Tex));
                    if (avatar != null) avatar.setColor(0.4f, 0.4f, 0.4f, 0.6f);
                }
                continue;
            }

            if (i == 1) {
                p2Joined = true;
            }

            String pid = p.optString("id", "");
            String puid = p.optString("userId", "");
            boolean isMe;
            if (!mySockId.isEmpty() && !pid.isEmpty()) {
                isMe = mySockId.equals(pid);
            } else {
                isMe = (nm.myPlayerIndex == i + 1);
            }

            int c = p.optInt("charId", i == 0 ? 1 : 2);
            boolean ready = p.optBoolean("ready", false);
            if (i == 1) {
                p2Ready = ready;
            }
            String name = p.optString("name", "Người chơi " + (i + 1));
            if (isMe && nm.playerName != null && !nm.playerName.isEmpty()) {
                name = nm.playerName;
            }

            if (i == 0) {
                p1UserId = puid;
                p1Name = name;
                p1Char = c;
            } else {
                p2UserId = puid;
                p2Name = name;
                p2Char = c;
            }

            String heroName = (c == 2) ? "Vân U" : "Vân Tiêu";
            String readyText = ready ? " [SẴN SÀNG]" : " [CHƯA SẴN SÀNG]";
            String isMeText = isMe ? " [BẠN]" : "";
            lbl.setText(role + isMeText + ": " + name + " (" + heroName + ")" + readyText);
            lbl.setColor(ready ? UiKit.SUCCESS : (isMe ? UiKit.GOLD : UiKit.TEXT));

            // Cập nhật avatar của slot
            Texture heroTex = GameAssets.background(c == 2 ? "p2.jpg" : "p1.jpg");
            if (heroTex != null && avatar != null) {
                avatar.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(heroTex));
                avatar.setColor(Color.WHITE);
            }

            if (isMe) {
                nm.myCharacter = c;
                game.selectedCharacter = (c == 2) ? "P2" : "P1";
                if (isReady != ready) {
                    isReady = ready;
                }
            } else {
                nm.partnerCharacter = c;
                nm.partnerName = name;
            }
        }
        refreshHeroHighlight();
        updateReadyButton();
        if (players.length() >= 2) {
            if (p2Ready) {
                setStatus(nm.myPlayerIndex == 1 ? "Người chơi 2 đã SẴN SÀNG! Chủ phòng hãy bấm BẮT ĐẦU CHƠI!" : "Bạn đã sẵn sàng! Chờ chủ phòng bắt đầu trận đấu.", UiKit.SUCCESS);
            } else {
                setStatus(nm.myPlayerIndex == 1 ? "Người chơi 2 đã vào phòng. Đang chờ đối thủ sẵn sàng..." : "Hãy bấm SẴN SÀNG để chủ phòng có thể bắt đầu trận.", UiKit.WARNING);
            }
        } else {
            setStatus("Đã tạo phòng! Gửi mã cho bạn bè tham gia...", UiKit.WARNING);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (warmupFrames > 0) {
            warmupFrames--;
        } else if (!UiKit.wasModalKeyConsumedThisFrame() && !hasModalOpen()) {
            boolean typing = stage.getKeyboardFocus() instanceof TextField;
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
                if (typing) {
                    stage.setKeyboardFocus(null);
                } else {
                    leaveToMenu();
                    return;
                }
            } else if (!typing) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                    selectHero(1);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                    selectHero(2);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                    createRoom();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) || Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                    if (txtRoomCode != null) {
                        stage.setKeyboardFocus(txtRoomCode);
                        txtRoomCode.setCursorPosition(txtRoomCode.getText().length());
                    }
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                    openInviteDialog();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    toggleReadyState();
                }
            }
        }

        netCheckTimer += delta;
        if (netCheckTimer >= 0.5f) {
            netCheckTimer = 0f;
            updateNetStatus();
            tryRejoin(); // vừa kết nối lại được -> vào lại phòng cũ
        }

        roomSyncTimer += delta;
        if (roomSyncTimer >= 1.5f) {
            roomSyncTimer = 0f;
            NetworkManager nm = NetworkManager.getInstance();
            if (nm.isConnected() && !nm.currentRoomCode.isEmpty() && socket != null) {
                socket.emit("getRoomState");
            }
        }

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    /**
     * Gỡ listener ở hide() (chạy NGAY khi đổi màn hình, trước show() của màn mới).
     * Không gỡ ở dispose() vì dispose chạy trễ 1 frame, lúc đó màn mới có thể đã đăng ký cùng tên sự kiện.
     */
    @Override
    public void hide() {
        cleanSocketListeners();
        NetworkManager.getInstance().setMatchInviteListener(null);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (stage != null) stage.dispose();
    }
}
