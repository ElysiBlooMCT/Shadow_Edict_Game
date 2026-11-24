package io.qzz.elysibloomct.ctv.se;

/**
 * Sảnh CO-OP 2 người. Toàn bộ logic dùng chung với phòng PVP nằm ở {@link LobbyScreen}
 * (trước đây là 2 file gần như trùng lặp ~380 dòng).
 */
public class CoopLobbyScreen extends LobbyScreen {
    public CoopLobbyScreen(Main game) {
        super(game, true);
    }

    public CoopLobbyScreen(Main game, String pendingJoinCode) {
        super(game, true, pendingJoinCode);
    }
}
