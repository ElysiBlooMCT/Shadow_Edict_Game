package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import java.util.function.Consumer;

/**
 * Nhập chữ trên trình duyệt điện thoại.
 *
 * Bản web vẽ game trên một khung canvas, nên khi chạm vào ô nhập (TextField) bàn phím điện thoại
 * không tự bật lên. Trên web điện thoại, chạm vào ô sẽ mở hộp nhập chữ bất đồng bộ (HTML input)
 * để giữ kết nối Socket.IO không bị ngắt nhịp ping khi người chơi gõ lâu.
 *
 * PC, Android và web trên máy tính: {@link #prompt} = null, các ô nhập hoạt động như cũ.
 */
public final class TextInputBridge {

    /** Hỏi chữ bất đồng bộ từ người dùng; gọi onResult.accept(null) nếu người dùng bấm Hủy. */
    public interface Prompt {
        void ask(String title, String current, int maxLength, Consumer<String> onResult);
    }

    /** WebLauncher gán khi chạy trên thiết bị cảm ứng (pointer: coarse). */
    public static Prompt prompt;

    private TextInputBridge() {}

    /** True khi nền tảng đang dùng hộp nhập chữ cảm ứng (web trên điện thoại/máy tính bảng). */
    public static boolean isTouchPromptActive() {
        return prompt != null;
    }

    /** Gắn vào một ô nhập; không làm gì nếu nền tảng không cần. */
    public static void attach(final TextField field, final String title) {
        if (prompt == null || field == null) return;
        field.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (field.isDisabled()) return;
                if (x < 0 || y < 0 || x > field.getWidth() || y > field.getHeight()) return;
                open(field, title);
            }
        });
    }

    private static void open(final TextField field, final String title) {
        final Prompt p = prompt;
        if (p == null) return;
        try {
            p.ask(title, field.getText(), field.getMaxLength(), result -> {
                Runnable apply = () -> applyResult(field, result);
                if (Gdx.app != null) {
                    Gdx.app.postRunnable(apply);
                } else {
                    apply.run();
                }
            });
        } catch (Throwable t) {
            clearFocusIfOwned(field);
        }
    }

    private static void applyResult(TextField field, String result) {
        Stage stage = field.getStage();
        if (result == null) {
            clearFocusIfOwned(field);
            return;
        }
        int max = field.getMaxLength();
        if (max > 0 && result.length() > max) result = result.substring(0, max);
        field.setText(result);
        field.setCursorPosition(result.length());
        if (stage == null) return;
        stage.setKeyboardFocus(field);
        InputEvent enter = new InputEvent();
        enter.setType(InputEvent.Type.keyTyped);
        enter.setStage(stage);
        enter.setCharacter('\r');
        field.fire(enter);
        if (stage.getKeyboardFocus() == field) stage.setKeyboardFocus(null);
    }

    private static void clearFocusIfOwned(TextField field) {
        Stage stage = field.getStage();
        if (stage != null && stage.getKeyboardFocus() == field) {
            stage.setKeyboardFocus(null);
        }
    }
}
