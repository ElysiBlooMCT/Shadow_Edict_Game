package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 * Nhập chữ trên trình duyệt điện thoại.
 *
 * Bản web vẽ game trên một khung canvas, nên khi chạm vào ô nhập (TextField) bàn phím điện thoại
 * không tự bật lên. Trên web điện thoại, chạm vào ô sẽ mở hộp nhập chữ của trình duyệt
 * (có bàn phím, gõ được tiếng Việt); bấm OK thì chữ được điền vào ô và xử lý như khi bấm Enter.
 *
 * PC, Android và web trên máy tính: {@link #prompt} = null, các ô nhập hoạt động như cũ.
 */
public final class TextInputBridge {

    /** Hỏi chữ từ người dùng. Trả về null nếu người dùng bấm Hủy. */
    public interface Prompt {
        String ask(String title, String current);
    }

    /** WebLauncher gán khi chạy trên điện thoại. */
    public static Prompt prompt;

    private TextInputBridge() {}

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

    private static void open(TextField field, String title) {
        String result;
        try {
            result = prompt.ask(title, field.getText());
        } catch (Throwable t) {
            return;
        }
        if (result == null) return;
        int max = field.getMaxLength();
        if (max > 0 && result.length() > max) result = result.substring(0, max);
        field.setText(result);
        field.setCursorPosition(result.length());
        // Xử lý như khi người chơi gõ xong và bấm Enter (lưu tên, vào phòng, gửi tin nhắn...)
        Stage stage = field.getStage();
        if (stage == null) return;
        stage.setKeyboardFocus(field);
        InputEvent enter = new InputEvent();
        enter.setType(InputEvent.Type.keyTyped);
        enter.setStage(stage);
        enter.setCharacter('\r');
        field.fire(enter);
        if (stage.getKeyboardFocus() == field) stage.setKeyboardFocus(null);
    }
}
