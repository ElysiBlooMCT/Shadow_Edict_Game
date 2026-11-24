package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import org.json.JSONObject;

/**
 * Hộp thoại hiển thị hồ sơ người chơi chi tiết (avatar, tên, ID, cấp, rank, tỷ lệ thắng, tướng tủ).
 * Có thể mở từ sảnh 1v1, sảnh 5v5, danh sách bạn bè hoặc màn hình chính.
 */
public class PlayerProfileDialog {
    private final Stage stage;
    private final Main game;
    private final String targetUserId;
    private final String initialName;
    private final int charId;
    private final String currentRoomCode;
    private final String currentMode;

    private final Group root = new Group();
    private InputListener keyListener;
    private NetworkManager.UserProfileListener profileListener;
    private boolean dismissed = false;

    // Các thành phần UI có thể cập nhật Realtime từ server
    private Image avatarImg;
    private Label lblName, lblId, lblStatus, lblRank, lblLevel;
    private Label lblMatches, lblWins, lblWinRate, lblFavHero, lblGold;
    private TextButton btnAddFriend, btnInvite;
    private Label lblToast;

    public static void show(Stage stage, Main game, String userId, String name, int charId,
                            String currentRoomCode, String currentMode) {
        new PlayerProfileDialog(stage, game, userId, name, charId, currentRoomCode, currentMode);
    }

    public static void showForFriend(Stage stage, Main game, FriendInfo friend,
                                    String currentRoomCode, String currentMode) {
        if (friend == null) return;
        new PlayerProfileDialog(stage, game, friend.userId, friend.name, 1, currentRoomCode, currentMode);
    }

    public PlayerProfileDialog(Stage stage, Main game, String userId, String name, int charId,
                               String currentRoomCode, String currentMode) {
        this.stage = stage;
        this.game = game;
        this.targetUserId = (userId != null && !userId.isEmpty()) ? userId : PlayerProfile.getInstance().userId;
        this.initialName = (name != null && !name.isEmpty()) ? name : "Chiến Thần";
        this.charId = charId == 2 ? 2 : 1;
        this.currentRoomCode = (currentRoomCode != null) ? currentRoomCode : "";
        this.currentMode = (currentMode != null) ? currentMode : "";

        buildUI();
        requestServerData();
    }

    private void buildUI() {
        root.setSize(stage.getWidth(), stage.getHeight());
        root.addActor(UiKit.dimOverlay(0.65f));

        Table dialogBox = new Table();
        dialogBox.setBackground(UiKit.panel());
        dialogBox.pad(18, 24, 20, 24);

        // Header: Tiêu đề + Nút Đóng [X]
        Table header = new Table();
        header.add(UiKit.label("HỒ SƠ CHIẾN BINH", UiKit.normal(), UiKit.GOLD)).left().expandX();
        TextButton btnClose = UiKit.button("X", UiKit.RED);
        UiKit.onClick(btnClose, this::dismiss);
        header.add(btnClose).size(42, 38).right();
        dialogBox.add(header).fillX().padBottom(14).row();

        // Khung thông tin chính (Avatar + Tên/ID/Rank/Trạng thái)
        Table topCard = new Table();
        topCard.setBackground(UiKit.rounded(UiKit.PANEL_LIGHT));
        topCard.pad(12, 14, 12, 14);

        // Avatar có khung và vòng sáng
        Stack avatarStack = new Stack();
        UiKit.HeroHalo halo = new UiKit.HeroHalo(charId == 2 ? new Color(1f, 0.34f, 0.64f, 1f) : new Color(0.42f, 0.92f, 1f, 1f), 1.15f);
        avatarStack.add(halo);

        String portraitPath = (charId == 2) ? "p2.jpg" : "p1.jpg";
        Texture portraitTex = GameAssets.background(portraitPath);
        avatarImg = (portraitTex != null) ? new Image(portraitTex) : new Image(UiKit.solid(UiKit.GRAY));
        avatarImg.setScaling(Scaling.fill);
        Table padTable = new Table();
        padTable.add(avatarImg).size(88, 88).pad(4);
        avatarStack.add(padTable);
        Image frame = new Image(UiKit.framed(new Color(0, 0, 0, 0), UiKit.GOLD));
        avatarStack.add(frame);
        topCard.add(avatarStack).size(96, 96).padRight(16);

        // Thông tin bên cạnh avatar
        Table infoCol = new Table();
        lblName = UiKit.label(initialName, UiKit.large(), UiKit.GOLD);
        lblId = UiKit.label("ID: " + targetUserId, UiKit.normal(), new Color(0.55f, 0.85f, 1f, 1f));
        lblRank = UiKit.label("[ CHIẾN BINH ]", UiKit.small(), UiKit.WARNING);
        lblStatus = UiKit.label("● Trực tuyến", UiKit.small(), UiKit.SUCCESS);

        infoCol.add(lblName).left().padBottom(2).row();
        infoCol.add(lblId).left().padBottom(3).row();
        infoCol.add(lblRank).left().padBottom(3).row();
        infoCol.add(lblStatus).left();
        topCard.add(infoCol).expandX().left();
        dialogBox.add(topCard).width(540).padBottom(12).row();

        // Bảng thống kê chi tiết
        Table statsTable = new Table();
        statsTable.setBackground(UiKit.rounded(new Color(0.04f, 0.05f, 0.08f, 0.90f)));
        statsTable.pad(10, 16, 10, 16);

        lblLevel = UiKit.label("Lv. 1", UiKit.normal(), Color.WHITE);
        lblFavHero = UiKit.label(charId == 2 ? "Vân U (Đen)" : "Vân Tiêu (Trắng)", UiKit.normal(), Color.WHITE);
        lblMatches = UiKit.label("0", UiKit.normal(), Color.WHITE);
        lblWins = UiKit.label("0", UiKit.normal(), Color.WHITE);
        lblWinRate = UiKit.label("0%", UiKit.normal(), UiKit.SUCCESS);
        lblGold = UiKit.label("500", UiKit.normal(), UiKit.GOLD);

        // Hàng 1
        statsTable.add(UiKit.label("Cấp độ:", UiKit.small(), UiKit.MUTED)).left().width(110);
        statsTable.add(lblLevel).left().width(140);
        statsTable.add(UiKit.label("Tướng sở trường:", UiKit.small(), UiKit.MUTED)).left().width(130);
        statsTable.add(lblFavHero).left().row();

        // Hàng 2
        statsTable.add(UiKit.label("Số trận đấu:", UiKit.small(), UiKit.MUTED)).left().padTop(6);
        statsTable.add(lblMatches).left().padTop(6);
        statsTable.add(UiKit.label("Số trận thắng:", UiKit.small(), UiKit.MUTED)).left().padTop(6);
        statsTable.add(lblWins).left().padTop(6).row();

        // Hàng 3
        statsTable.add(UiKit.label("Tỷ lệ thắng:", UiKit.small(), UiKit.MUTED)).left().padTop(6);
        statsTable.add(lblWinRate).left().padTop(6);
        statsTable.add(UiKit.label("Vàng tích lũy:", UiKit.small(), UiKit.MUTED)).left().padTop(6);
        statsTable.add(lblGold).left().padTop(6).row();

        dialogBox.add(statsTable).width(540).padBottom(14).row();

        // Nút hành động (Kết bạn / Mời phòng / Đóng)
        Table actions = new Table();
        boolean isMe = targetUserId.equalsIgnoreCase(PlayerProfile.getInstance().userId);
        boolean alreadyFriend = false;
        for (FriendInfo fi : NetworkManager.getInstance().friendsList) {
            if (fi.userId.equalsIgnoreCase(targetUserId)) {
                alreadyFriend = true;
                break;
            }
        }

        if (!isMe && !alreadyFriend) {
            btnAddFriend = UiKit.button("+ KẾT BẠN", UiKit.GREEN);
            UiKit.onClick(btnAddFriend, () -> {
                NetworkManager.getInstance().sendFriendRequest(targetUserId);
                btnAddFriend.setText("ĐÃ GỬI LỜI MỜI!");
                btnAddFriend.setDisabled(true);
                setToast("Đã gửi lời mời kết bạn!", UiKit.SUCCESS);
            });
            actions.add(btnAddFriend).size(160, 44).padRight(10);
        }

        if (!isMe && alreadyFriend && !currentRoomCode.isEmpty()) {
            btnInvite = UiKit.button("MỜI VÀO PHÒNG", UiKit.PURPLE);
            UiKit.onClick(btnInvite, () -> {
                NetworkManager.getInstance().inviteFriendToMatch(targetUserId, currentRoomCode, currentMode);
                btnInvite.setText("ĐÃ MỜI!");
                btnInvite.setDisabled(true);
                setToast("Đã gửi lời mời vào phòng #" + currentRoomCode, UiKit.SUCCESS);
            });
            actions.add(btnInvite).size(170, 44).padRight(10);
        }

        TextButton btnDone = UiKit.button("ĐÓNG (Esc)", UiKit.GRAY);
        UiKit.onClick(btnDone, this::dismiss);
        actions.add(btnDone).size(140, 44);
        dialogBox.add(actions).padBottom(8).row();

        lblToast = UiKit.label("", UiKit.small(), UiKit.SUCCESS);
        dialogBox.add(lblToast);

        keyListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (dismissed || !UiKit.isTopModal(stage, root)) return false;
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    UiKit.consumeModalKey();
                    event.stop();
                    dismiss();
                    return true;
                }
                return false;
            }
        };

        dialogBox.pack();
        dialogBox.setPosition((root.getWidth() - dialogBox.getWidth()) / 2f,
            (root.getHeight() - dialogBox.getHeight()) / 2f);
        root.addActor(dialogBox);
        stage.addActor(root);
        stage.addCaptureListener(keyListener);

        // Nếu là chính mình, hiển thị ngay dữ liệu từ PlayerProfile
        if (isMe) {
            PlayerProfile p = PlayerProfile.getInstance();
            lblName.setText(p.playerName);
            lblLevel.setText("Lv. " + p.level);
            lblRank.setText("[ " + p.getRankTitle().toUpperCase() + " ]");
            lblGold.setText(p.gold + " Vàng");
            lblMatches.setText(String.valueOf(p.matches));
            lblWins.setText(String.valueOf(p.wins));
            lblWinRate.setText(p.getWinRateText());
            lblFavHero.setText(p.favoriteHero != null ? p.favoriteHero : (charId == 2 ? "Vân U" : "Vân Tiêu"));
            lblStatus.setText("● Trực tuyến [BẠN]");
            lblStatus.setColor(UiKit.SUCCESS);
        }
    }

    private void setToast(String text, Color color) {
        if (lblToast != null) {
            lblToast.setText(text);
            lblToast.setColor(color);
        }
    }

    private void requestServerData() {
        NetworkManager nm = NetworkManager.getInstance();
        if (!nm.isConnected() || nm.socket == null) return;

        // Đăng ký listener qua NetworkManager (tự gỡ trong dismiss, không đụng độ khi mở nhanh nhiều hồ sơ)
        profileListener = data -> {
            if (dismissed || data == null) return;
            String uId = data.optString("userId", "");
            if (!uId.equalsIgnoreCase(targetUserId)) return;

            if (!data.optBoolean("found", true)) {
                setToast("Không tìm thấy thông tin trên máy chủ.", UiKit.MUTED);
                return;
            }

            String sName = data.optString("name", initialName);
            int sLvl = Math.max(1, data.optInt("level", 1));
            int sGold = Math.max(0, data.optInt("gold", 500));
            int sMatches = Math.max(0, data.optInt("matches", 0));
            int sWins = Math.max(0, Math.min(sMatches, data.optInt("wins", 0)));
            String sFav = data.optString("favoriteHero", charId == 2 ? "Vân U" : "Vân Tiêu");
            String sStatus = data.optString("status", "ONLINE");
            String sRoom = data.optString("roomCode", "");

            lblName.setText(sName);
            lblLevel.setText("Lv. " + sLvl);
            lblRank.setText("[ " + PlayerProfile.rankTitleForLevel(sLvl).toUpperCase() + " ]");
            lblGold.setText(sGold + " Vàng");
            lblMatches.setText(String.valueOf(sMatches));
            lblWins.setText(String.valueOf(sWins));
            int wr = sMatches > 0 ? (int) Math.round((double) sWins * 100.0 / sMatches) : 0;
            lblWinRate.setText(wr + "%");
            lblFavHero.setText(sFav);

            // Cập nhật trạng thái
            if ("ONLINE".equalsIgnoreCase(sStatus)) {
                lblStatus.setText("● Trực tuyến");
                lblStatus.setColor(UiKit.SUCCESS);
            } else if ("IN_ROOM".equalsIgnoreCase(sStatus)) {
                lblStatus.setText("● Trong phòng #" + sRoom);
                lblStatus.setColor(UiKit.WARNING);
            } else if ("IN_GAME".equalsIgnoreCase(sStatus)) {
                lblStatus.setText("● Đang trong trận");
                lblStatus.setColor(UiKit.DANGER);
            } else {
                lblStatus.setText("● Ngoại tuyến");
                lblStatus.setColor(UiKit.MUTED);
            }
        };
        nm.addUserProfileListener(profileListener);
        nm.getUserProfile(targetUserId);
    }

    public void dismiss() {
        if (dismissed) return;
        dismissed = true;
        if (profileListener != null) {
            NetworkManager.getInstance().removeUserProfileListener(profileListener);
            profileListener = null;
        }
        if (stage != null && keyListener != null) {
            stage.removeCaptureListener(keyListener);
        }
        root.remove();
    }
}
