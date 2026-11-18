package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/**
 * Hộp thoại lời mời vào trận (modal). Mỗi Stage chỉ hiện tối đa 1 hộp thoại cùng lúc.
 */
public class MatchInviteDialog {
    private static MatchInviteDialog active;

    private final Group root = new Group();
    private final Stage stage;
    private final Main game;
    private final MatchInvite invite;
    private InputListener keyListener;

    /** Hiện lời mời trên stage (bỏ qua nếu stage đó đang hiện một lời mời khác). */
    public static void showOn(Stage stage, Main game, MatchInvite invite) {
        if (stage == null || invite == null) return;
        if (active != null) {
            if (active.stage == stage && active.root.getStage() != null) return;
            active.dismiss();
        }
        active = new MatchInviteDialog(stage, game, invite);
        active.show();
    }

    public static boolean isOpenOn(Stage stage) {
        return active != null && active.stage == stage && active.root.getStage() != null;
    }

    public MatchInviteDialog(Stage stage, Main game, MatchInvite invite) {
        this.stage = stage;
        this.game = game;
        this.invite = invite;
        initUI();
    }

    private void initUI() {
        root.setSize(stage.getWidth(), stage.getHeight());
        root.addActor(UiKit.dimOverlay(0.55f));

        Table box = new Table();
        box.setBackground(UiKit.panel());
        box.pad(24, 32, 24, 32);

        String modeName = "1v1".equalsIgnoreCase(invite.mode) ? "Đấu trường 1v1"
            : ("5v5".equalsIgnoreCase(invite.mode) ? "Đại chiến 5v5" : "Hợp tác 2 người");

        box.add(UiKit.label("LỜI MỜI VÀO TRẬN!", UiKit.large(), UiKit.GOLD)).padBottom(14).row();
        box.add(UiKit.label("Người mời: " + invite.inviterName + " [" + invite.inviterId + "]", UiKit.normal(), UiKit.WARNING))
            .padBottom(8).row();
        box.add(UiKit.label("Chế độ: " + modeName + "   •   Phòng #" + invite.roomCode, UiKit.normal(), UiKit.TEXT))
            .padBottom(22).row();
        if (invite.message != null && !invite.message.isEmpty()) {
            Label msg = UiKit.label("\"" + invite.message + "\"", UiKit.small(), UiKit.MUTED);
            box.add(msg).padTop(-12).padBottom(18).row();
        }

        TextButton btnAccept = UiKit.button("VÀO PHÒNG (Enter)", UiKit.GREEN);
        TextButton btnDecline = UiKit.button("TỪ CHỐI (Esc)", UiKit.RED);

        final Runnable doAccept = () -> {
            dismiss();
            NetworkManager nm = NetworkManager.getInstance();
            nm.currentRoomCode = invite.roomCode;
            nm.currentMatchInvite = null;
            if ("5v5".equalsIgnoreCase(invite.mode)) {
                game.setScreen(new Arena5v5LobbyScreen(game));
            } else if ("coop".equalsIgnoreCase(invite.mode)) {
                game.setScreen(new CoopLobbyScreen(game));
            } else {
                game.setScreen(new LobbyScreen(game));
            }
        };
        final Runnable doDecline = () -> {
            NetworkManager.getInstance().currentMatchInvite = null;
            dismiss();
        };

        UiKit.onClick(btnAccept, doAccept);
        UiKit.onClick(btnDecline, doDecline);

        keyListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (root.getStage() == null) return false;
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER || keycode == Input.Keys.Y) {
                    UiKit.consumeModalKey();
                    event.stop();
                    doAccept.run();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK || keycode == Input.Keys.N) {
                    UiKit.consumeModalKey();
                    event.stop();
                    doDecline.run();
                    return true;
                }
                return false;
            }
        };

        Table btnTable = new Table();
        btnTable.add(btnAccept).width(225).height(48).padRight(14);
        btnTable.add(btnDecline).width(185).height(48);
        box.add(btnTable);

        box.pack();
        box.setPosition((root.getWidth() - box.getWidth()) / 2f, (root.getHeight() - box.getHeight()) / 2f);
        root.addActor(box);
    }

    public void show() {
        if (stage != null) {
            stage.addActor(root);
            if (keyListener != null) stage.addCaptureListener(keyListener);
        }
    }

    public void dismiss() {
        if (stage != null && keyListener != null) {
            stage.removeCaptureListener(keyListener);
        }
        root.remove();
        if (active == this) active = null;
    }
}
