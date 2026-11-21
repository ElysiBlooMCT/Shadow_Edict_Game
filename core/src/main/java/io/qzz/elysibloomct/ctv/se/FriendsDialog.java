package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import java.util.List;

/**
 * Hệ thống bạn bè (modal): danh sách bạn + mời vào phòng, lời mời kết bạn, tìm kiếm người chơi.
 */
public class FriendsDialog implements NetworkManager.FriendsUpdateListener {
    private static final int TAB_FRIENDS = 0, TAB_REQUESTS = 1, TAB_SEARCH = 2;

    private final Stage stage;
    private final Main game;
    private final String currentRoomCode;
    private final String currentMode;

    private final Group root = new Group();
    private Table tabBar;
    private Table contentTable;
    private Label lblStatusMessage;

    private int currentTab = TAB_FRIENDS;
    private String searchText = "";
    private boolean dismissed = false;
    private TextField txtMyName;
    private InputListener keyListener;

    /** Đổi tên hiển thị: lưu máy + báo server để bạn bè thấy tên mới. */
    private void renameMe() {
        String name = txtMyName.getText().trim();
        PlayerProfile profile = PlayerProfile.getInstance();
        if (name.isEmpty()) { setStatus("Tên không được để trống.", UiKit.WARNING); return; }
        if (name.equals(profile.playerName)) { setStatus("Tên không thay đổi.", UiKit.MUTED); return; }
        profile.playerName = name;
        profile.save();
        if (game != null) game.playerName = name;
        NetworkManager nm = NetworkManager.getInstance();
        nm.playerName = name;
        if (nm.isConnected()) {
            nm.registerUserWithServer();
            setStatus("Đã đổi tên.", UiKit.SUCCESS);
        } else {
            setStatus("Đã đổi tên trên máy. Máy chủ sẽ cập nhật khi kết nối lại.", UiKit.WARNING);
        }
        stage.unfocusAll();
    }

    public FriendsDialog(Stage stage, Main game, String currentRoomCode, String currentMode) {
        this.stage = stage;
        this.game = game;
        this.currentRoomCode = (currentRoomCode != null) ? currentRoomCode : "";
        this.currentMode = (currentMode != null) ? currentMode : "";

        buildUI();
        NetworkManager nm = NetworkManager.getInstance();
        nm.addFriendsListener(this);
        nm.refreshFriendsList();
        if (!nm.isConnected()) setStatus("Chưa kết nối máy chủ – danh sách có thể chưa cập nhật.", UiKit.WARNING);
    }

    private void buildUI() {
        root.setSize(stage.getWidth(), stage.getHeight());
        root.addActor(UiKit.dimOverlay(0.55f)); // chặn click xuyên xuống menu phía sau

        Table dialogBox = new Table();
        dialogBox.setBackground(UiKit.panel());
        dialogBox.pad(18, 22, 22, 22);

        // Header
        Table header = new Table();
        header.add(UiKit.label("BẠN BÈ   •   [1/2/3] Chuyển thẻ  •  [N] Đổi tên  •  [Esc] Đóng", UiKit.normal(), UiKit.GOLD)).left().expandX();
        TextButton btnClose = UiKit.button("X", UiKit.RED);
        UiKit.onClick(btnClose, this::dismiss);
        header.add(btnClose).size(44, 40).right();
        dialogBox.add(header).fillX().padBottom(10).row();

        // Hồ sơ của bạn: đổi tên hiển thị + mã ID để bạn bè tìm
        PlayerProfile profile = PlayerProfile.getInstance();
        Table myProfileBar = new Table();
        myProfileBar.setBackground(UiKit.rounded(UiKit.PANEL_LIGHT));
        myProfileBar.pad(6, 12, 6, 12);

        Texture myTex = GameAssets.background("P2".equals(profile.lastCharacter) ? "p2.jpg" : "p1.jpg");
        Image myAv = (myTex != null) ? new Image(myTex) : new Image(UiKit.solid(UiKit.GRAY));
        myAv.setScaling(com.badlogic.gdx.utils.Scaling.fill);
        Table myAvCard = new Table();
        myAvCard.add(myAv).size(36, 36);
        UiKit.onClick(myAvCard, () -> {
            PlayerProfileDialog.show(stage, game, profile.userId, profile.playerName, "P2".equals(profile.lastCharacter) ? 2 : 1, currentRoomCode, currentMode);
        });
        myProfileBar.add(myAvCard).size(36, 36).padRight(8);

        myProfileBar.add(UiKit.label("Tên:", UiKit.small(), UiKit.MUTED)).padRight(8);
        // Font có dấu để gõ được tên tiếng Việt
        TextField.TextFieldStyle nameStyle = new TextField.TextFieldStyle(UiKit.textFieldStyle());
        nameStyle.font = UiKit.inputFont();
        nameStyle.messageFont = UiKit.inputFont();
        txtMyName = new TextField(profile.playerName, nameStyle);
        txtMyName.setMaxLength(16);
        TextInputBridge.attach(txtMyName, "Đổi tên nhân vật");
        txtMyName.setMessageText("Nhập tên...");
        txtMyName.setTextFieldListener((field, c) -> {
            if (c == '\r' || c == '\n') renameMe();
        });
        myProfileBar.add(txtMyName).width(230).height(40);
        TextButton btnRename = UiKit.button("ĐỔI TÊN", UiKit.GREEN, UiKit.small());
        UiKit.onClick(btnRename, this::renameMe);
        myProfileBar.add(btnRename).width(96).height(36).padLeft(8);

        Label lblMyId = UiKit.label("ID: " + profile.userId, UiKit.normal(), new Color(0.55f, 0.85f, 1f, 1f));
        UiKit.onClick(lblMyId, () -> {
            PlayerProfileDialog.show(stage, game, profile.userId, profile.playerName, "P2".equals(profile.lastCharacter) ? 2 : 1, currentRoomCode, currentMode);
        });
        myProfileBar.add(lblMyId).expandX().right();
        dialogBox.add(myProfileBar).fillX().padBottom(10).row();

        tabBar = new Table();
        dialogBox.add(tabBar).fillX().padBottom(10).row();

        contentTable = new Table();
        dialogBox.add(contentTable).width(660).height(320).row();

        lblStatusMessage = UiKit.label("", UiKit.small(), UiKit.WARNING);
        dialogBox.add(lblStatusMessage).padTop(8);

        keyListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (dismissed || root.getStage() == null) return false;
                boolean typing = stage.getKeyboardFocus() instanceof TextField;
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    UiKit.consumeModalKey();
                    event.stop();
                    if (typing) stage.setKeyboardFocus(null);
                    else dismiss();
                    return true;
                }
                if (typing) return false;
                switch (keycode) {
                    case Input.Keys.NUM_1:
                    case Input.Keys.F1:
                        UiKit.consumeModalKey();
                        currentTab = TAB_FRIENDS;
                        refreshContent();
                        event.stop();
                        return true;
                    case Input.Keys.NUM_2:
                    case Input.Keys.F2:
                        UiKit.consumeModalKey();
                        currentTab = TAB_REQUESTS;
                        refreshContent();
                        event.stop();
                        return true;
                    case Input.Keys.NUM_3:
                    case Input.Keys.F3:
                        UiKit.consumeModalKey();
                        currentTab = TAB_SEARCH;
                        refreshContent();
                        event.stop();
                        return true;
                    case Input.Keys.LEFT:
                    case Input.Keys.A:
                        UiKit.consumeModalKey();
                        currentTab = (currentTab + 2) % 3;
                        refreshContent();
                        event.stop();
                        return true;
                    case Input.Keys.RIGHT:
                    case Input.Keys.D:
                        UiKit.consumeModalKey();
                        currentTab = (currentTab + 1) % 3;
                        refreshContent();
                        event.stop();
                        return true;
                    case Input.Keys.N:
                        UiKit.consumeModalKey();
                        if (txtMyName != null) {
                            stage.setKeyboardFocus(txtMyName);
                            txtMyName.setCursorPosition(txtMyName.getText().length());
                        }
                        event.stop();
                        return true;
                }
                return false;
            }
        };

        dialogBox.pack();
        dialogBox.setPosition((root.getWidth() - dialogBox.getWidth()) / 2f, (root.getHeight() - dialogBox.getHeight()) / 2f);
        root.addActor(dialogBox);
        stage.addActor(root);
        stage.addCaptureListener(keyListener);

        // Tự động làm mới danh sách bạn bè và trạng thái mỗi 3 giây
        root.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(3.0f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.run(() -> {
                    if (NetworkManager.getInstance().isConnected()) {
                        NetworkManager.getInstance().refreshFriendsList();
                    }
                })
            )
        ));

        refreshContent();
    }

    private void rebuildTabs() {
        tabBar.clearChildren();
        NetworkManager nm = NetworkManager.getInstance();
        addTab("BẠN BÈ (" + nm.friendsList.size() + ")", TAB_FRIENDS);
        addTab("LỜI MỜI (" + nm.pendingFriendRequests.size() + ")", TAB_REQUESTS);
        addTab("TÌM BẠN", TAB_SEARCH);
    }

    private void addTab(String text, int tab) {
        boolean active = currentTab == tab;
        TextButton b = UiKit.button(text, active ? UiKit.BLUE : UiKit.GRAY);
        UiKit.onClick(b, () -> {
            currentTab = tab;
            refreshContent();
        });
        tabBar.add(b).width(212).height(40).padRight(tab < TAB_SEARCH ? 8 : 0);
    }

    private void refreshContent() {
        if (dismissed) return;
        if (root.getStage() == null && contentTable.hasChildren()) {
            // Màn hình chứa hộp thoại đã bị đóng -> tự gỡ listener, tránh rò rỉ
            dismiss();
            return;
        }
        rebuildTabs(); // cập nhật highlight & số lượng trên tab
        contentTable.clearChildren();
        if (currentTab == TAB_FRIENDS) renderFriendsTab();
        else if (currentTab == TAB_REQUESTS) renderRequestsTab();
        else renderSearchTab();
    }

    private Table newRow() {
        Table row = new Table();
        row.setBackground(UiKit.rounded(UiKit.PANEL_LIGHT));
        row.pad(8, 12, 8, 12);
        return row;
    }

    private void addScroll(Table into, Table list) {
        ScrollPane sp = new ScrollPane(list, UiKit.scrollStyle());
        sp.setFadeScrollBars(false);
        sp.setScrollingDisabled(true, false);
        into.add(sp).expand().fill();
    }

    // ===== TAB 1: BẠN BÈ =====
    private void renderFriendsTab() {
        List<FriendInfo> friends = NetworkManager.getInstance().friendsList;
        Table listTable = new Table();
        listTable.top().pad(6);

        if (friends.isEmpty()) {
            listTable.add(UiKit.label("Chưa có bạn bè. Vào thẻ TÌM BẠN để kết bạn!", UiKit.normal(), UiKit.MUTED)).pad(40);
        } else {
            for (FriendInfo friend : friends) {
                Table row = newRow();

                // Avatar bạn bè (bấm để xem hồ sơ)
                Texture pTex = GameAssets.background("p1.jpg");
                Image av = (pTex != null) ? new Image(pTex) : new Image(UiKit.solid(UiKit.GRAY));
                av.setScaling(com.badlogic.gdx.utils.Scaling.fill);
                Table avCard = new Table();
                avCard.add(av).size(36, 36);
                UiKit.onClick(avCard, () -> {
                    PlayerProfileDialog.show(stage, game, friend.userId, friend.name, 1, currentRoomCode, currentMode);
                });
                row.add(avCard).size(36, 36).padRight(10);

                Table infoCol = new Table();
                Label lblName = UiKit.label(friend.name + " [" + friend.userId + "]  Lv." + friend.level, UiKit.normal(), UiKit.TEXT);
                UiKit.onClick(lblName, () -> {
                    PlayerProfileDialog.show(stage, game, friend.userId, friend.name, 1, currentRoomCode, currentMode);
                });
                infoCol.add(lblName).left().row();
                infoCol.add(UiKit.label(friend.getStatusDisplayText(), UiKit.small(), friend.getStatusColor())).left();
                row.add(infoCol).expandX().left();

                if (!currentRoomCode.isEmpty() && friend.isAvailableForInvite()) {
                    TextButton btnInvite = UiKit.button("MỜI TRẬN", UiKit.GREEN, UiKit.small());
                    UiKit.onClick(btnInvite, () -> {
                        NetworkManager.getInstance().inviteFriendToMatch(friend.userId, currentRoomCode, currentMode);
                        btnInvite.setText("ĐÃ MỜI!");
                        btnInvite.setDisabled(true);
                        setStatus("Đã gửi lời mời vào phòng đến " + friend.name + "!", UiKit.SUCCESS);
                    });
                    row.add(btnInvite).size(104, 36).padRight(8);
                }

                TextButton btnUnfriend = UiKit.button("HỦY", UiKit.RED, UiKit.small());
                UiKit.onClick(btnUnfriend, () -> {
                    NetworkManager.getInstance().removeFriend(friend.userId);
                    setStatus("Đã hủy kết bạn với " + friend.name, UiKit.WARNING);
                });
                row.add(btnUnfriend).size(66, 36);
                listTable.add(row).width(620).padBottom(6).row();
            }
        }
        addScroll(contentTable, listTable);
    }

    // ===== TAB 2: LỜI MỜI KẾT BẠN =====
    private void renderRequestsTab() {
        List<String> requests = NetworkManager.getInstance().pendingFriendRequests;
        Table listTable = new Table();
        listTable.top().pad(6);

        if (requests.isEmpty()) {
            listTable.add(UiKit.label("Không có lời mời kết bạn nào.", UiKit.normal(), UiKit.MUTED)).pad(40);
        } else {
            for (String reqId : requests) {
                Table row = newRow();
                String reqName = NetworkManager.getInstance().pendingRequestNames.get(reqId);
                String who = (reqName != null ? reqName + " [" + reqId + "]" : reqId);
                row.add(UiKit.label(who + " muốn kết bạn", UiKit.normal(), UiKit.TEXT)).expandX().left();

                TextButton btnAccept = UiKit.button("ĐỒNG Ý", UiKit.GREEN, UiKit.small());
                UiKit.onClick(btnAccept, () -> {
                    NetworkManager.getInstance().acceptFriendRequest(reqId);
                    setStatus("Đã chấp nhận lời mời từ " + reqId, UiKit.SUCCESS);
                    refreshContent();
                });
                TextButton btnDecline = UiKit.button("TỪ CHỐI", UiKit.RED, UiKit.small());
                UiKit.onClick(btnDecline, () -> {
                    NetworkManager.getInstance().declineFriendRequest(reqId);
                    setStatus("Đã từ chối lời mời từ " + reqId, UiKit.WARNING);
                    refreshContent();
                });
                row.add(btnAccept).size(90, 36).padRight(8);
                row.add(btnDecline).size(90, 36);
                listTable.add(row).width(620).padBottom(6).row();
            }
        }
        addScroll(contentTable, listTable);
    }

    // ===== TAB 3: TÌM KIẾM =====
    private void renderSearchTab() {
        Table searchBox = new Table();
        searchBox.top();

        Table inputRow = new Table();
        final TextField txtSearch = new TextField(searchText, UiKit.textFieldStyle());
        TextInputBridge.attach(txtSearch, "Tìm bạn: nhập ID (VD: AL-1234) hoặc tên");
        txtSearch.setMessageText("Nhập ID (VD: AL-1234) hoặc tên");
        txtSearch.setTextFieldListener((field, c) -> {
            searchText = field.getText();
            if (c == '\r' || c == '\n') doSearch(searchText);
        });
        TextButton btnDoSearch = UiKit.button("TÌM KIẾM", UiKit.BLUE);
        UiKit.onClick(btnDoSearch, () -> doSearch(txtSearch.getText()));

        inputRow.add(txtSearch).width(460).height(44).padRight(10);
        inputRow.add(btnDoSearch).width(160).height(44);
        searchBox.add(inputRow).padBottom(10).row();

        Table resultsTable = new Table();
        resultsTable.top().pad(6);
        List<FriendInfo> results = NetworkManager.getInstance().searchResults;
        if (results.isEmpty()) {
            resultsTable.add(UiKit.label("Nhập ID hoặc tên rồi bấm TÌM KIẾM.", UiKit.small(), UiKit.MUTED)).pad(20);
        } else {
            for (FriendInfo u : results) {
                Table row = newRow();

                Texture pTex = GameAssets.background("p1.jpg");
                Image av = (pTex != null) ? new Image(pTex) : new Image(UiKit.solid(UiKit.GRAY));
                av.setScaling(com.badlogic.gdx.utils.Scaling.fill);
                Table avCard = new Table();
                avCard.add(av).size(36, 36);
                UiKit.onClick(avCard, () -> {
                    PlayerProfileDialog.show(stage, game, u.userId, u.name, 1, currentRoomCode, currentMode);
                });
                row.add(avCard).size(36, 36).padRight(10);

                Label lblUser = UiKit.label(u.name + " [" + u.userId + "] (Lv." + u.level + ")", UiKit.normal(), UiKit.TEXT);
                UiKit.onClick(lblUser, () -> {
                    PlayerProfileDialog.show(stage, game, u.userId, u.name, 1, currentRoomCode, currentMode);
                });
                row.add(lblUser).expandX().left();

                if (u.isFriend) {
                    row.add(UiKit.label("[ĐÃ LÀ BẠN]", UiKit.small(), UiKit.SUCCESS));
                } else if (u.isPending) {
                    row.add(UiKit.label("[ĐÃ GỬI LỜI MỜI]", UiKit.small(), UiKit.WARNING));
                } else if (u.userId.equals(PlayerProfile.getInstance().userId)) {
                    row.add(UiKit.label("[BẠN]", UiKit.small(), UiKit.MUTED));
                } else {
                    TextButton btnAdd = UiKit.button("+ KẾT BẠN", UiKit.GREEN, UiKit.small());
                    UiKit.onClick(btnAdd, () -> {
                        NetworkManager.getInstance().sendFriendRequest(u.userId);
                        btnAdd.setText("ĐÃ GỬI!");
                        btnAdd.setDisabled(true);
                        setStatus("Đã gửi lời mời kết bạn đến " + u.name, UiKit.SUCCESS);
                    });
                    row.add(btnAdd).size(120, 36);
                }
                resultsTable.add(row).width(620).padBottom(6).row();
            }
        }
        addScroll(searchBox, resultsTable);
        contentTable.add(searchBox).expand().fill();
        stage.setKeyboardFocus(txtSearch);
    }

    private void doSearch(String q) {
        q = (q == null) ? "" : q.trim();
        searchText = q;
        if (q.isEmpty()) return;
        if (!NetworkManager.getInstance().isConnected()) {
            setStatus("Chưa kết nối máy chủ, không thể tìm kiếm.", UiKit.DANGER);
            return;
        }
        NetworkManager.getInstance().searchUsers(q);
        setStatus("Đang tìm '" + q + "'...", UiKit.WARNING);
    }

    private void setStatus(String text, Color color) {
        if (lblStatusMessage == null) return;
        lblStatusMessage.setText(text);
        lblStatusMessage.setColor(color);
    }

    // Callback từ NetworkManager đã chạy trên luồng render (postRunnable)
    @Override public void onFriendsUpdated() { refreshContent(); }
    @Override public void onSearchResultsUpdated() { refreshContent(); }

    @Override
    public void onFriendRequestReceived(String requesterId, String requesterName) {
        if (dismissed) return;
        setStatus("Lời mời kết bạn mới từ " + requesterName + " [" + requesterId + "]!", UiKit.WARNING);
        refreshContent();
    }

    public void dismiss() {
        if (dismissed) return;
        dismissed = true;
        if (stage != null && keyListener != null) {
            stage.removeCaptureListener(keyListener);
        }
        if (stage != null) stage.unfocusAll();
        NetworkManager.getInstance().removeFriendsListener(this);
        root.remove();
    }
}
