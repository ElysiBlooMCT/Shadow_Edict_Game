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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
 * Sảnh Đại chiến 5v5 ARAM: chọn phe, chọn tướng, đặt tên, chơi với BOT hoặc mở phòng online.
 */
public class Arena5v5LobbyScreen extends ScreenAdapter {
    private static final String[] ROOM_EVENTS = {"roomCreated", "roomJoined", "updateRoomState", "playerJoined", "playerLeft", "startGame", "startGame5v5", "errorMsg"};
    private static final String[] BLUE_BOTS = {"Lôi Thần (Trắng)", "Phi Tiêu (Đen)", "Bạch Hổ (Trắng)", "Cuồng Phong (Đen)", "Kiếm Thánh (Trắng)"};
    private static final String[] RED_BOTS = {"Tử Thần (Đen)", "Huyết Nhãn (Đen)", "Hắc Báo (Trắng)", "U Hồn (Đen)", "Ám Ảnh (Trắng)"};

    private final Main game;
    private Stage stage;
    private GameSocket socket;

    private Label lblRoomInfo, lblStatus;
    private Table blueRosterTable, redRosterTable;
    private TextField txtPlayerName, txtRoomCode;
    private TextButton btnTeamBlue, btnTeamRed, btnHeroWhite, btnHeroDark, btnReady, btnPlay, btnMapSelect;
    private UiKit.HeroHalo haloWhite, haloDark;

    private int selectedTeam;
    private int selectedHero;
    private boolean isReady = false;
    private boolean leaving = false;
    private boolean disposed = false;
    private boolean pendingRejoin = false;
    private boolean hostLegacyReadySent = false;
    private JSONArray lastOnlinePlayers = null;
    private int baseActorCount = Integer.MAX_VALUE;
    private int warmupFrames = 2;
    private float roomSyncTimer = 0f;

    public Arena5v5LobbyScreen(Main game) {
        this.game = game;
        this.selectedTeam = (game.selectedTeam == 2) ? 2 : 1;
        this.selectedHero = "P2".equals(game.selectedCharacter) ? 2 : 1;
    }

    @Override
    public void show() {
        SoundManager.getInstance().playMenuBgm();
        NetworkManager nm = NetworkManager.getInstance();
        socket = nm.socket;
        nm.myCharacter = selectedHero;
        nm.myTeam = selectedTeam;
        nm.ensureConnected();

        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Texture bg = GameAssets.background("background1.jpg"); // ảnh không có logo – tiêu đề không bị đè
        if (bg != null) {
            Image bgImg = new Image(bg);
            bgImg.setSize(1280, 720);
            bgImg.setScaling(Scaling.fill);
            stage.addActor(bgImg);
        }
        Image dim = new Image(UiKit.solid(new Color(0.02f, 0.02f, 0.04f, 0.66f)));
        dim.setFillParent(true);
        stage.addActor(dim);

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(18);

        root.add(UiKit.label("ĐẠI CHIẾN ĐẤU TRƯỜNG KỸ NĂNG", UiKit.title(), UiKit.GOLD)).padBottom(0).row();
        root.add(UiKit.label("Thuần chiến đấu kỹ năng đối kháng • Tự do giao tranh (1v1, 2v2, 3v3...)",
            UiKit.normal(), UiKit.TEXT)).padBottom(12).row();

        // ---- Hàng 1: tên + tướng ----
        Table settings = new Table();
        settings.add(UiKit.label("TÊN (N):", UiKit.normal(), UiKit.GOLD)).padRight(8);
        String currentName = (game.playerName != null && !game.playerName.isEmpty()) ? game.playerName : "Chiến Thần";
        txtPlayerName = new TextField(currentName, UiKit.textFieldStyle());
        txtPlayerName.setMaxLength(16);
        TextInputBridge.attach(txtPlayerName, "Tên của bạn");
        txtPlayerName.setTextFieldListener((f, c) -> {
            updateRosterDisplay(lastOnlinePlayers);
            if (c == '\r' || c == '\n') stage.setKeyboardFocus(null);
        });
        settings.add(txtPlayerName).size(220, 44).padRight(24);
        btnHeroWhite = UiKit.button("VÂN TIÊU (1)", UiKit.GRAY);
        btnHeroDark = UiKit.button("VÂN U (2)", UiKit.GRAY);
        haloWhite = new UiKit.HeroHalo(new Color(0.42f, 0.92f, 1.00f, 1f), 0.56f);
        haloDark = new UiKit.HeroHalo(new Color(1.00f, 0.34f, 0.64f, 1f), 0.56f);
        Stack stWhite = new Stack();
        stWhite.add(haloWhite);
        stWhite.add(btnHeroWhite);
        Stack stDark = new Stack();
        stDark.add(haloDark);
        stDark.add(btnHeroDark);
        settings.add(stWhite).size(210, 44).padRight(14);
        settings.add(stDark).size(210, 44);
        root.add(settings).padBottom(10).row();

        // ---- Hàng 2: chọn phe ----
        Table teams = new Table();
        btnTeamBlue = UiKit.button("PHE ÁNH SÁNG [Q]", UiKit.GRAY);
        btnTeamRed = UiKit.button("PHE BÓNG TỐI [E]", UiKit.GRAY);
        teams.add(btnTeamBlue).size(330, 46).padRight(20);
        teams.add(btnTeamRed).size(330, 46);
        root.add(teams).padBottom(12).row();

        // ---- Hàng 3: đội hình 2 phe ----
        blueRosterTable = new Table();
        redRosterTable = new Table();
        Table blueBox = rosterBox("PHE ÁNH SÁNG", new Color(0.04f, 0.14f, 0.28f, 0.90f), blueRosterTable);
        Table redBox = rosterBox("PHE BÓNG TỐI", new Color(0.28f, 0.06f, 0.10f, 0.90f), redRosterTable);
        Table rosters = new Table();
        rosters.add(blueBox).size(470, 196).padRight(20);
        rosters.add(redBox).size(470, 196);
        root.add(rosters).padBottom(10).row();

        // ---- Hàng 4: phòng online ----
        Table online = new Table();
        online.setBackground(UiKit.rounded(new Color(0.05f, 0.06f, 0.09f, 0.85f)));
        online.pad(8, 14, 8, 14);
        String rCode = nm.currentRoomCode;
        lblRoomInfo = UiKit.label(rCode.isEmpty() ? "PHÒNG TRỰC TUYẾN: --" : ("PHÒNG: " + rCode), UiKit.normal(), UiKit.GOLD);
        TextButton btnCreate = UiKit.button("TẠO PHÒNG (C)", UiKit.BLUE, UiKit.small());
        txtRoomCode = new TextField("", UiKit.textFieldStyle());
        TextInputBridge.attach(txtRoomCode, "Nhập mã phòng");
        txtRoomCode.setMessageText("Mã (Tab)");
        txtRoomCode.setAlignment(Align.center);
        txtRoomCode.setTextFieldListener((f, c) -> {
            if (c == '\r' || c == '\n') {
                stage.setKeyboardFocus(null);
                joinTypedRoom();
            }
        });
        TextButton btnJoin = UiKit.button("VÀO", UiKit.GREEN, UiKit.small());
        TextButton btnInvite = UiKit.button("+ MỜI BẠN (F)", UiKit.PURPLE, UiKit.small());
        btnReady = UiKit.button("SẴN SÀNG (R)", UiKit.GRAY, UiKit.small());
        online.add(lblRoomInfo).width(190).left().padRight(10);
        online.add(btnCreate).size(132, 40).padRight(8);
        online.add(txtRoomCode).size(110, 40).padRight(6);
        online.add(btnJoin).size(70, 40).padRight(8);
        online.add(btnInvite).size(130, 40).padRight(8);
        online.add(btnReady).size(130, 40);
        root.add(online).padBottom(8).row();

        lblStatus = UiKit.label("Chọn phe [Q/E], sát thủ [1/2], rồi tạo phòng (C) hoặc nhập mã để chơi cùng bạn bè.", UiKit.normal(), UiKit.WARNING);
        root.add(lblStatus).padBottom(8).row();

        // ---- Hàng 5: hành động ----
        Table actions = new Table();
        btnMapSelect = UiKit.button("BẢN ĐỒ: THÀNH CỔ (1)", UiKit.GOLD_DIM, UiKit.normal());
        btnPlay = UiKit.button("CHƠI NGAY (Enter)", UiKit.ORANGE, UiKit.large());
        TextButton btnBack = UiKit.button("QUAY LẠI (Esc)", UiKit.RED);
        actions.add(btnMapSelect).size(250, 56).padRight(14);
        actions.add(btnPlay).size(330, 56).padRight(14);
        actions.add(btnBack).size(160, 56);
        root.add(actions);
        stage.addActor(root);
        baseActorCount = stage.getActors().size;

        refreshSelections();
        updateRosterDisplay(null);
        updateActionButtons();
        updateMapButtonText();
        pendingRejoin = !nm.currentRoomCode.isEmpty();
        setupSocketEvents();
        tryRejoin();

        // ===== Sự kiện =====
        UiKit.onClick(btnHeroWhite, () -> selectHero(1));
        UiKit.onClick(btnHeroDark, () -> selectHero(2));
        UiKit.onClick(btnTeamBlue, () -> selectTeam(1));
        UiKit.onClick(btnTeamRed, () -> selectTeam(2));
        UiKit.onClick(btnMapSelect, this::cycleMap);

        UiKit.onClick(btnCreate, this::createOnlineRoom);
        UiKit.onClick(btnJoin, this::joinTypedRoom);
        UiKit.onClick(btnInvite, this::openInviteDialog);
        UiKit.onClick(btnReady, this::toggleReadyState);

        UiKit.onClick(btnPlay, this::startMatch);
        UiKit.onClick(btnBack, this::leaveToMenu);
    }

    private void createOnlineRoom() {
        syncPlayerConfig();
        if (!NetworkManager.getInstance().isConnected()) {
            NetworkManager.getInstance().ensureConnected();
            setStatus("Chưa kết nối máy chủ! Bạn vẫn có thể LUYỆN TẬP một mình (Enter).", UiKit.DANGER);
            return;
        }
        socket.emit("createRoom", buildPacket());
        setStatus("Đang tạo phòng trực tuyến...", UiKit.WARNING);
    }

    private void joinTypedRoom() {
        syncPlayerConfig();
        String code = txtRoomCode.getText().trim();
        if (code.isEmpty()) {
            setStatus("Vui lòng nhập mã phòng!", UiKit.WARNING);
            return;
        }
        if (!NetworkManager.getInstance().isConnected()) {
            NetworkManager.getInstance().ensureConnected();
            setStatus("Chưa kết nối máy chủ!", UiKit.DANGER);
            return;
        }
        socket.emit("joinRoom", code, buildPacket());
        setStatus("Đang vào phòng " + code + "...", UiKit.WARNING);
    }

    private void openInviteDialog() {
        String code = NetworkManager.getInstance().currentRoomCode;
        if (code.isEmpty()) {
            setStatus("Hãy tạo phòng trực tuyến trước khi mời bạn!", UiKit.WARNING);
            return;
        }
        new FriendsDialog(stage, game, code, "5v5");
    }

    private void toggleReadyState() {
        NetworkManager nm2 = NetworkManager.getInstance();
        if (!nm2.isConnected() || nm2.currentRoomCode.isEmpty()) {
            setStatus("Chưa ở trong phòng trực tuyến – hãy tạo phòng hoặc nhập mã phòng để vào.", UiKit.WARNING);
            return;
        }
        if (nm2.myPlayerIndex == 1) {
            setStatus("Chủ phòng không cần bấm Sẵn sàng, hãy chờ người chơi khác sẵn sàng rồi Bắt đầu!", UiKit.WARNING);
            return;
        }
        SoundManager.getInstance().playCoin();
        isReady = !isReady;
        socket.emit("toggleReady");
        updateActionButtons();
    }

    private void updateActionButtons() {
        NetworkManager nm = NetworkManager.getInstance();
        boolean inRoom = !nm.currentRoomCode.isEmpty();

        if (btnPlay == null || btnReady == null) return;

        if (!inRoom) {
            btnPlay.setText("LUYỆN TẬP MỘT MÌNH (Enter)");
            btnPlay.setStyle(UiKit.buttonStyle(UiKit.ORANGE, UiKit.large()));
            UiKit.fitLabel(btnPlay, 330f);
            btnPlay.setDisabled(false);
            btnReady.setVisible(false);
            return;
        }

        if (nm.myPlayerIndex == 1) {
            // Chủ phòng: Nút Sẵn sàng ẩn đi (chủ phòng là người ra lệnh bắt đầu)
            btnReady.setVisible(false);

            // Kiểm tra xem tất cả người chơi khác trong phòng đã Sẵn sàng chưa
            boolean allGuestsReady = true;
            int guestCount = 0;
            if (lastOnlinePlayers != null && lastOnlinePlayers.length() > 1) {
                guestCount = lastOnlinePlayers.length() - 1;
                for (int i = 1; i < lastOnlinePlayers.length(); i++) {
                    JSONObject p = lastOnlinePlayers.optJSONObject(i);
                    if (p == null || !p.optBoolean("ready", false)) {
                        allGuestsReady = false;
                        break;
                    }
                }
            }

            if (guestCount > 0) {
                if (allGuestsReady) {
                    btnPlay.setText("BẮT ĐẦU TRẬN ĐẤU (Enter)");
                    btnPlay.setStyle(UiKit.buttonStyle(UiKit.GREEN, UiKit.large()));
                    UiKit.fitLabel(btnPlay, 330f);
                    btnPlay.setDisabled(false);
                    setStatus("Tất cả người chơi đã SẴN SÀNG! Chủ phòng hãy bấm Bắt đầu trận đấu.", UiKit.SUCCESS);
                } else {
                    btnPlay.setText("CHỜ NGƯỜI CHƠI SẴN SÀNG...");
                    btnPlay.setStyle(UiKit.buttonStyle(UiKit.GRAY, UiKit.large()));
                    UiKit.fitLabel(btnPlay, 330f);
                    btnPlay.setDisabled(true);
                    setStatus("Đang chờ " + guestCount + " người chơi khác bấm Sẵn sàng...", UiKit.WARNING);
                }
            } else {
                // Phòng chỉ có 1 mình chủ phòng: có thể vào luyện tập trước hoặc chờ bạn
                btnPlay.setText("BẮT ĐẦU TRẬN ĐẤU (Enter)");
                btnPlay.setStyle(UiKit.buttonStyle(UiKit.ORANGE, UiKit.large()));
                UiKit.fitLabel(btnPlay, 330f);
                btnPlay.setDisabled(false);
            }
        } else {
            // Khách (người chơi khác): Bắt buộc phải bấm SẴN SÀNG, KHÔNG ĐƯỢC tự ý vào trước!
            btnReady.setVisible(true);
            btnReady.setText(isReady ? "ĐÃ SẴN SÀNG (R)" : "SẴN SÀNG (R)");
            btnReady.setStyle(UiKit.buttonStyle(isReady ? UiKit.GREEN : UiKit.GRAY, UiKit.small()));
            UiKit.fitLabel(btnReady, 130f);

            btnPlay.setText(isReady ? "CHỜ CHỦ PHÒNG BẮT ĐẦU..." : "HÃY BẤM SẴN SÀNG (R)");
            btnPlay.setStyle(UiKit.buttonStyle(UiKit.GRAY, UiKit.large()));
            UiKit.fitLabel(btnPlay, 330f);
            btnPlay.setDisabled(true); // Khóa nút không cho tự ý vào
            setStatus(isReady ? "Bạn đã sẵn sàng! Chờ chủ phòng bắt đầu trận đấu." : "Hãy bấm SẴN SÀNG (R) để chủ phòng có thể bắt đầu.", isReady ? UiKit.SUCCESS : UiKit.WARNING);
        }
    }

    private boolean hasModalOpen() {
        return stage != null && stage.getActors().size > baseActorCount;
    }

    private Table rosterBox(String title, Color bg, Table content) {
        Table box = new Table();
        box.setBackground(UiKit.framed(bg, UiKit.GOLD_DIM));
        box.pad(8, 12, 8, 12);
        box.top();
        box.add(UiKit.label(title, UiKit.normal(), UiKit.GOLD)).padBottom(6).row();
        ScrollPane sp = new ScrollPane(content, UiKit.scrollStyle());
        sp.setFadeScrollBars(false);
        sp.setScrollingDisabled(true, false);
        box.add(sp).expand().fill();
        return box;
    }

    private JSONObject buildPacket() {
        JSONObject packet = new JSONObject();
        try {
            packet.put("name", game.playerName);
            packet.put("team", selectedTeam);
            packet.put("charId", selectedHero);
            packet.put("userId", PlayerProfile.getInstance().userId);
            packet.put("type", "5v5");
        } catch (Exception ignored) {}
        return packet;
    }

    private void selectHero(int hero) {
        selectedHero = hero;
        game.selectedCharacter = (hero == 2) ? "P2" : "P1";
        NetworkManager.getInstance().myCharacter = hero;
        if (NetworkManager.getInstance().isConnected() && socket != null) {
            socket.emit("selectCharacter", hero);
        }
        if (lastOnlinePlayers != null) {
            applyLocalPlayerSelection(lastOnlinePlayers);
        }
        refreshSelections();
        updateRosterDisplay(lastOnlinePlayers);
    }

    private void selectTeam(int team) {
        selectedTeam = team;
        game.selectedTeam = team;
        NetworkManager.getInstance().myTeam = team;
        if (NetworkManager.getInstance().isConnected() && socket != null) {
            socket.emit("selectTeam", team);
        }
        if (lastOnlinePlayers != null) {
            applyLocalPlayerSelection(lastOnlinePlayers);
        }
        refreshSelections();
        updateRosterDisplay(lastOnlinePlayers);
    }

    private void applyLocalPlayerSelection(JSONArray onlinePlayers) {
        if (onlinePlayers == null) return;
        String mySockId = (socket != null && socket.id() != null) ? socket.id() : "";
        int myIdx = NetworkManager.getInstance().myPlayerIndex;
        for (int i = 0; i < onlinePlayers.length(); i++) {
            JSONObject p = onlinePlayers.optJSONObject(i);
            if (p == null) continue;
            String pid = p.optString("id", "");
            boolean isMe;
            if (!mySockId.isEmpty() && !pid.isEmpty()) {
                isMe = mySockId.equals(pid);
            } else {
                isMe = (myIdx > 0 && i == myIdx - 1);
            }
            if (isMe) {
                try {
                    p.put("team", selectedTeam);
                    p.put("charId", selectedHero);
                    String rawName = (txtPlayerName != null) ? txtPlayerName.getText().trim() : "";
                    if (!rawName.isEmpty()) p.put("name", rawName);
                } catch (Exception ignored) {}
                break;
            }
        }
    }

    private void refreshSelections() {
        btnHeroWhite.setStyle(UiKit.buttonStyle(selectedHero == 1 ? UiKit.BLUE : UiKit.GRAY, UiKit.normal()));
        btnHeroDark.setStyle(UiKit.buttonStyle(selectedHero == 2 ? UiKit.PURPLE : UiKit.GRAY, UiKit.normal()));
        if (haloWhite != null) haloWhite.setVisible(selectedHero == 1);
        if (haloDark != null) haloDark.setVisible(selectedHero == 2);
        btnTeamBlue.setStyle(UiKit.buttonStyle(selectedTeam == 1 ? new Color(0.12f, 0.36f, 0.68f, 1f) : UiKit.GRAY, UiKit.normal()));
        btnTeamRed.setStyle(UiKit.buttonStyle(selectedTeam == 2 ? new Color(0.66f, 0.15f, 0.20f, 1f) : UiKit.GRAY, UiKit.normal()));
        btnTeamBlue.setText(selectedTeam == 1 ? "PHE ÁNH SÁNG [Q]  ✓" : "PHE ÁNH SÁNG [Q]");
        btnTeamRed.setText(selectedTeam == 2 ? "PHE BÓNG TỐI [E]  ✓" : "PHE BÓNG TỐI [E]");
    }

    private void syncPlayerConfig() {
        String rawName = (txtPlayerName != null) ? txtPlayerName.getText().trim() : "";
        if (rawName.isEmpty()) rawName = "Chiến Thần";
        game.playerName = rawName;
        game.selectedTeam = (selectedTeam == 2) ? 2 : 1;
        game.selectedCharacter = (selectedHero == 2) ? "P2" : "P1";
        NetworkManager nm = NetworkManager.getInstance();
        nm.playerName = rawName;
        nm.myTeam = game.selectedTeam;
        nm.myCharacter = selectedHero;
    }

    private void tryRejoin() {
        NetworkManager nm = NetworkManager.getInstance();
        if (!pendingRejoin || !nm.isConnected() || nm.currentRoomCode == null || nm.currentRoomCode.isEmpty()) return;
        pendingRejoin = false;
        syncPlayerConfig();
        socket.emit("joinRoom", nm.currentRoomCode, buildPacket());
        socket.emit("selectCharacter", selectedHero);
        socket.emit("selectTeam", selectedTeam);
        setStatus("Đang đồng bộ phòng 5v5 #" + nm.currentRoomCode + "...", UiKit.WARNING);
    }

    private void startMatch() {
        startMatch(true);
    }

    private void startMatch(boolean broadcastStart) {
        NetworkManager nm = NetworkManager.getInstance();
        boolean inRoom = nm.isConnected() && !nm.currentRoomCode.isEmpty();

        if (inRoom && broadcastStart) {
            // Trong phòng trực tuyến:
            // 1. Chỉ chủ phòng mới có quyền phát lệnh bắt đầu trận đấu
            if (nm.myPlayerIndex != 1) {
                setStatus("Chỉ chủ phòng mới có quyền bắt đầu trận đấu!", UiKit.WARNING);
                return;
            }
            // 2. Phải đợi tất cả người chơi khác trong phòng sẵn sàng
            if (lastOnlinePlayers != null && lastOnlinePlayers.length() > 1) {
                for (int i = 1; i < lastOnlinePlayers.length(); i++) {
                    JSONObject p = lastOnlinePlayers.optJSONObject(i);
                    if (p == null || !p.optBoolean("ready", false)) {
                        setStatus("Chưa thể bắt đầu! Vẫn còn người chơi chưa bấm Sẵn sàng.", UiKit.WARNING);
                        return;
                    }
                }
            }
            SoundManager.getInstance().playCoin();
            socket.emit("startGame");
            if (nm.isLegacyServer()) {
                // Máy chủ bản cũ bỏ qua "startGame": trận chỉ bắt đầu khi CẢ HAI bấm Sẵn sàng (tối đa 2 người).
                if (!hostLegacyReadySent) { hostLegacyReadySent = true; socket.emit("toggleReady"); }
                setStatus("Máy chủ đang chạy bản cũ (cần cập nhật server.js) – đang bắt đầu theo cách cũ...", UiKit.WARNING);
                return;
            }
            setStatus("Đang gửi lệnh bắt đầu trận đấu cho tất cả người chơi...", UiKit.SUCCESS);
            return; // Chờ socket.on("startGame") kích hoạt để cả phòng vào trận cùng nhau!
        }

        if (leaving) return;
        leaving = true;
        cleanSocketListeners();
        syncPlayerConfig();
        if (lastOnlinePlayers != null) {
            nm.syncRoomPlayers(lastOnlinePlayers, PlayerProfile.getInstance().userId);
        }
        nm.updateMyPresence("IN_GAME", nm.currentRoomCode, "5v5");
        game.selectedOnlineMap = nm.selectedMap;
        game.setScreen(new GameScreen(game, GameScreen.GameMode.BATTLE_5V5, nm.selectedMap));
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
        UiKit.fitLabel(btnMapSelect, 250f);
    }

    private void leaveToMenu() {
        if (leaving) return;
        leaving = true;
        cleanSocketListeners();
        NetworkManager.getInstance().leaveRoom();
        game.setScreen(new MainMenuScreen(game));
    }

    private void setStatus(String text, Color c) {
        lblStatus.setText(text);
        lblStatus.setColor(c);
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
            setStatus("Tạo phòng 5v5 thành công! Gửi mã cho bạn bè.", UiKit.SUCCESS);
        }));
        socket.on("roomJoined", args -> Gdx.app.postRunnable(() -> {
            JSONObject data = asJson(args);
            if (disposed || data == null) return;
            onEnterRoom(data.optString("roomCode", ""), data.optInt("playerIndex", 2));
            setStatus("Vào phòng 5v5 thành công!", UiKit.SUCCESS);
        }));
        socket.on("updateRoomState", args -> Gdx.app.postRunnable(() -> {
            JSONObject room = asJson(args);
            if (disposed || room == null) return;
            int sMap = room.optInt("selectedMap", 1);
            NetworkManager.getInstance().selectedMap = sMap;
            game.selectedOnlineMap = sMap;
            updateMapButtonText();
            lastOnlinePlayers = room.optJSONArray("players");
            NetworkManager.getInstance().syncRoomPlayers(lastOnlinePlayers, PlayerProfile.getInstance().userId);
            adoptServerTeam();
            updateRosterDisplay(lastOnlinePlayers);
            updateActionButtons();
            int n = (lastOnlinePlayers != null) ? lastOnlinePlayers.length() : 1;
            setStatus("Người chơi trong phòng: " + n + " người", UiKit.TEXT);
        }));
        socket.on("playerJoined", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            JSONObject data = asJson(args);
            JSONObject room = data != null ? data.optJSONObject("room") : null;
            if (room != null && room.optJSONArray("players") != null) {
                lastOnlinePlayers = room.optJSONArray("players");
                NetworkManager.getInstance().syncRoomPlayers(lastOnlinePlayers, PlayerProfile.getInstance().userId);
                updateRosterDisplay(lastOnlinePlayers);
            }
            updateActionButtons();
            JSONObject newP = data != null ? data.optJSONObject("player") : null;
            String joinedName = newP != null ? newP.optString("name", "Người chơi mới") : "Người chơi mới";
            SoundManager.getInstance().playSlash();
            setStatus("Chiến binh " + joinedName + " đã vào phòng 5v5!", UiKit.SUCCESS);
        }));
        socket.on("playerLeft", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            hostLegacyReadySent = false; // máy chủ đặt lại trạng thái Sẵn sàng khi có người rời phòng
            JSONObject data = asJson(args);
            JSONObject room = data != null ? data.optJSONObject("room") : null;
            if (room != null && room.optJSONArray("players") != null) {
                lastOnlinePlayers = room.optJSONArray("players");
                NetworkManager.getInstance().syncRoomPlayers(lastOnlinePlayers, PlayerProfile.getInstance().userId);
                updateRosterDisplay(lastOnlinePlayers);
            }
            updateActionButtons();
            setStatus("Một người chơi đã rời phòng!", UiKit.WARNING);
        }));
        socket.on("startGame", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            JSONObject room = asJson(args);
            if (room != null) {
                int sMap = room.optInt("selectedMap", NetworkManager.getInstance().selectedMap);
                NetworkManager.getInstance().selectedMap = sMap;
                game.selectedOnlineMap = sMap;
                if (room.optJSONArray("players") != null) {
                    lastOnlinePlayers = room.optJSONArray("players");
                    NetworkManager.getInstance().syncRoomPlayers(lastOnlinePlayers, PlayerProfile.getInstance().userId);
                }
            }
            startMatch(false);
        }));
        socket.on("startGame5v5", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            JSONObject room = asJson(args);
            if (room != null) {
                int sMap = room.optInt("selectedMap", NetworkManager.getInstance().selectedMap);
                NetworkManager.getInstance().selectedMap = sMap;
                game.selectedOnlineMap = sMap;
                if (room.optJSONArray("players") != null) {
                    lastOnlinePlayers = room.optJSONArray("players");
                    NetworkManager.getInstance().syncRoomPlayers(lastOnlinePlayers, PlayerProfile.getInstance().userId);
                }
            }
            startMatch(false);
        }));
        socket.on("errorMsg", args -> Gdx.app.postRunnable(() -> {
            if (disposed) return;
            setStatus((args != null && args.length > 0) ? String.valueOf(args[0]) : "Lỗi máy chủ", UiKit.DANGER);
        }));
    }

    private void onEnterRoom(String code, int index) {
        if (code == null || code.isEmpty()) return;
        NetworkManager nm = NetworkManager.getInstance();
        nm.currentRoomCode = code;
        nm.myPlayerIndex = index;
        nm.updateMyPresence("IN_ROOM", code, "5v5");
        hostLegacyReadySent = false;
        lblRoomInfo.setText("PHÒNG: " + code);
        isReady = false;
        updateActionButtons();
        if (nm.isConnected()) {
            // Không gửi lại "selectTeam" ở đây: phe đã nằm trong gói createRoom/joinRoom và máy chủ
            // có thể đã cân bằng phe; gửi lại sẽ kéo mọi người về cùng một phe.
            socket.emit("selectCharacter", selectedHero);
        }
    }

    /** Nhận phe mà máy chủ xếp cho mình (cân bằng phe khi vào phòng) để giao diện và trận đấu dùng đúng phe. */
    private void adoptServerTeam() {
        int serverTeam = NetworkManager.getInstance().myTeam;
        if ((serverTeam == 1 || serverTeam == 2) && serverTeam != selectedTeam && !NetworkManager.getInstance().currentRoomCode.isEmpty()) {
            selectedTeam = serverTeam;
            game.selectedTeam = serverTeam;
            refreshSelections();
        }
    }

    private void addRosterRow(Table targetTable, int order, boolean isMe, String name, String uid,
                             int charId, boolean ready) {
        Table row = new Table();
        row.setBackground(UiKit.rounded(isMe ? new Color(0.12f, 0.24f, 0.40f, 0.88f) : new Color(0.06f, 0.08f, 0.12f, 0.78f)));
        row.pad(4, 8, 4, 8);

        // Avatar icon
        Texture heroTex = GameAssets.background(charId == 2 ? "p2.jpg" : "p1.jpg");
        Image av = (heroTex != null) ? new Image(heroTex) : new Image(UiKit.solid(UiKit.GRAY));
        av.setScaling(Scaling.fill);
        row.add(av).size(30, 30).padRight(8);

        // Thứ tự + tên + tướng
        String prefix = isMe ? "[BẠN] " : "";
        String heroName = (charId == 2) ? "Vân U" : "Vân Tiêu";
        Label lbl = UiKit.label(order + ". " + prefix + name + " (" + heroName + ")", UiKit.small(), isMe ? UiKit.GOLD : Color.WHITE);
        row.add(lbl).expandX().left();

        // Trạng thái sẵn sàng
        String badge = ready ? "[SẴN SÀNG]" : "[CHỜ]";
        Label lblReady = UiKit.label(badge, UiKit.small(), ready ? UiKit.SUCCESS : UiKit.MUTED);
        row.add(lblReady).right().padRight(4);

        // Bấm vào hàng / avatar -> mở hồ sơ người chơi!
        UiKit.onClick(row, () -> {
            PlayerProfileDialog.show(stage, game, uid, name, charId, NetworkManager.getInstance().currentRoomCode, "5v5");
        });

        targetTable.add(row).fillX().padBottom(4).row();
    }

    private void updateRosterDisplay(JSONArray onlinePlayers) {
        if (blueRosterTable == null || redRosterTable == null) return;
        blueRosterTable.clearChildren();
        redRosterTable.clearChildren();
        blueRosterTable.top();
        redRosterTable.top();

        String pName = (txtPlayerName != null) ? txtPlayerName.getText().trim() : "";
        if (pName.isEmpty()) pName = "Chiến Thần";

        int blueCount = 0, redCount = 0;

        if (onlinePlayers != null && onlinePlayers.length() > 0) {
            String mySockId = (socket != null && socket.id() != null) ? socket.id() : "";
            int myIdx = NetworkManager.getInstance().myPlayerIndex;

            for (int i = 0; i < onlinePlayers.length(); i++) {
                JSONObject p = onlinePlayers.optJSONObject(i);
                if (p == null) continue;
                String pid = p.optString("id", "");
                String puid = p.optString("userId", "");

                boolean isMe;
                if (!mySockId.isEmpty() && !pid.isEmpty()) {
                    isMe = mySockId.equals(pid);
                } else {
                    isMe = (myIdx > 0 && i == myIdx - 1);
                }

                String name = isMe ? pName : p.optString("name", "Người chơi " + (i + 1));
                int team = isMe ? selectedTeam : p.optInt("team", 1);
                int charId = isMe ? selectedHero : p.optInt("charId", 1);
                boolean ready = p.optBoolean("ready", false);

                if (team == 1) {
                    blueCount++;
                    addRosterRow(blueRosterTable, blueCount, isMe, name, puid, charId, ready);
                } else {
                    redCount++;
                    addRosterRow(redRosterTable, redCount, isMe, name, puid, charId, ready);
                }
            }
        } else {
            if (selectedTeam == 1) {
                blueCount = 1;
                addRosterRow(blueRosterTable, 1, true, pName, PlayerProfile.getInstance().userId, selectedHero, true);
            } else {
                redCount = 1;
                addRosterRow(redRosterTable, 1, true, pName, PlayerProfile.getInstance().userId, selectedHero, true);
            }
        }

        if (blueCount == 0) {
            blueRosterTable.add(UiKit.label("(Chưa có người chơi phe Ánh Sáng)", UiKit.small(), UiKit.MUTED)).pad(12).row();
        }
        if (redCount == 0) {
            redRosterTable.add(UiKit.label("(Chưa có người chơi phe Bóng Tối)", UiKit.small(), UiKit.MUTED)).pad(12).row();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        tryRejoin();

        roomSyncTimer += delta;
        if (roomSyncTimer >= 1.5f) {
            roomSyncTimer = 0f;
            NetworkManager nm = NetworkManager.getInstance();
            if (nm.isConnected() && !nm.currentRoomCode.isEmpty() && socket != null) {
                socket.emit("getRoomState");
            }
        }
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
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.Q) || Gdx.input.isKeyJustPressed(Input.Keys.UP)
                    || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                    selectTeam(1);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
                    || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                    selectTeam(2);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                    createOnlineRoom();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) || Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                    if (txtRoomCode != null) {
                        stage.setKeyboardFocus(txtRoomCode);
                        txtRoomCode.setCursorPosition(txtRoomCode.getText().length());
                    }
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
                    if (txtPlayerName != null) {
                        stage.setKeyboardFocus(txtPlayerName);
                        txtPlayerName.setCursorPosition(txtPlayerName.getText().length());
                    }
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                    openInviteDialog();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    toggleReadyState();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    startMatch();
                    return;
                }
            }
        }
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

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
