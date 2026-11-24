package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Hộp thoại cài đặt dạng modal: Ngôn ngữ (7 thứ tiếng), âm thanh (nhạc & hiệu ứng), tra cứu phím tắt.
 * Hỗ trợ điều khiển hoàn toàn bằng bàn phím laptop (↑/↓ chọn mục, ←/→ chỉnh âm lượng/đổi ngôn ngữ, Enter chọn, Esc lưu & đóng).
 */
public class SettingsDialog {
    private final Stage stage;
    private final Group root = new Group();
    private final Runnable onClose;
    private InputListener keyListener;
    private boolean closed = false;
    private int focusRow = 3; // 0: Ngôn ngữ, 1: Nhạc nền, 2: Hiệu ứng, 3: Lưu & Đóng

    public static SettingsDialog show(Stage stage, Main game, Runnable onClose) {
        SettingsDialog d = new SettingsDialog(stage, game, onClose);
        stage.addActor(d.root);
        return d;
    }

    private SettingsDialog(Stage stage, Main game, Runnable onClose) {
        this.stage = stage;
        this.onClose = onClose;
        root.setSize(stage.getWidth(), stage.getHeight());
        root.addActor(UiKit.dimOverlay(0.65f));

        final PlayerProfile profile = PlayerProfile.getInstance();
        final SoundManager sm = SoundManager.getInstance();

        Table box = new Table();
        box.setBackground(UiKit.panel());
        box.pad(22, 30, 22, 30);
        box.defaults().padBottom(10);

        final Label lblTitle = UiKit.label(LanguageManager.get("SETTINGS_TITLE"), UiKit.large(), UiKit.GOLD);
        box.add(lblTitle).colspan(3).padBottom(14).row();

        // --- 1. Ngôn ngữ (Language) ---
        final Label lblLang = UiKit.label(LanguageManager.get("LANGUAGE"), UiKit.normal(), UiKit.TEXT);
        final TextButton btnLang = UiKit.button(LanguageManager.getCurrentDisplayName(), UiKit.BLUE, UiKit.normal());
        box.add(lblLang).left().padRight(16);
        box.add(btnLang).colspan(2).width(324).height(40).left().row();

        // --- 2. Nhạc nền (Music) ---
        final Label lblMusic = UiKit.label(LanguageManager.get("MUSIC"), UiKit.normal(), UiKit.TEXT);
        final TextButton btnMusic = toggleButton(profile.musicEnabled);
        final Slider sldMusic = slider(profile.musicVolume);
        box.add(lblMusic).left().padRight(16);
        box.add(btnMusic).width(90).height(40).padRight(14);
        box.add(sldMusic).width(220).row();

        // --- 3. Hiệu ứng âm thanh (SFX) ---
        final Label lblSfx = UiKit.label(LanguageManager.get("SFX"), UiKit.normal(), UiKit.TEXT);
        final TextButton btnSfx = toggleButton(profile.soundEnabled);
        final Slider sldSfx = slider(profile.soundVolume);
        box.add(lblSfx).left().padRight(16);
        box.add(btnSfx).width(90).height(40).padRight(14);
        box.add(sldSfx).width(220).row();

        // --- 4. Bảng tra cứu phím tắt theo ngôn ngữ ---
        final Label keys = UiKit.label(LanguageManager.get("CONTROLS_GUIDE"), UiKit.small(), UiKit.MUTED);
        keys.setWrap(true);
        box.add(keys).colspan(3).width(640).padTop(6).padBottom(14).row();

        // --- 5. Nút Lưu & Đóng ---
        final TextButton btnClose = UiKit.button(LanguageManager.get("SAVE_CLOSE"), UiKit.GREEN);
        UiKit.onClick(btnClose, () -> close(game));
        box.add(btnClose).colspan(3).width(280).height(48).padBottom(0);

        final Runnable refreshAllTexts = () -> {
            lblTitle.setText(LanguageManager.get("SETTINGS_TITLE"));
            lblLang.setText(LanguageManager.get("LANGUAGE"));
            btnLang.setText(LanguageManager.getCurrentDisplayName());
            lblMusic.setText(LanguageManager.get("MUSIC"));
            btnMusic.setText(profile.musicEnabled ? LanguageManager.get("ON") : LanguageManager.get("OFF"));
            lblSfx.setText(LanguageManager.get("SFX"));
            btnSfx.setText(profile.soundEnabled ? LanguageManager.get("ON") : LanguageManager.get("OFF"));
            keys.setText(LanguageManager.get("CONTROLS_GUIDE"));
            btnClose.setText(LanguageManager.get("SAVE_CLOSE"));
        };

        final Runnable cycleLanguage = () -> {
            LanguageManager.nextLanguage();
            refreshAllTexts.run();
            sm.playCoin();
        };
        UiKit.onClick(btnLang, cycleLanguage);

        final Runnable toggleMusic = () -> {
            profile.musicEnabled = !profile.musicEnabled;
            btnMusic.setText(profile.musicEnabled ? LanguageManager.get("ON") : LanguageManager.get("OFF"));
            btnMusic.setStyle(UiKit.buttonStyle(profile.musicEnabled ? UiKit.GREEN : UiKit.GRAY, UiKit.normal()));
            sm.setMusicEnabled(profile.musicEnabled);
        };
        final Runnable toggleSfx = () -> {
            profile.soundEnabled = !profile.soundEnabled;
            btnSfx.setText(profile.soundEnabled ? LanguageManager.get("ON") : LanguageManager.get("OFF"));
            btnSfx.setStyle(UiKit.buttonStyle(profile.soundEnabled ? UiKit.GREEN : UiKit.GRAY, UiKit.normal()));
            sm.setSoundEnabled(profile.soundEnabled);
        };

        UiKit.onClick(btnMusic, toggleMusic);
        UiKit.onClick(btnSfx, toggleSfx);
        sldMusic.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                profile.musicVolume = sldMusic.getValue();
                sm.setMusicVolume(profile.musicVolume);
            }
        });
        sldSfx.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                profile.soundVolume = sldSfx.getValue();
                sm.setSoundVolume(profile.soundVolume);
            }
        });

        final Runnable refreshFocusVisual = () -> {
            lblLang.setColor(focusRow == 0 ? UiKit.GOLD : UiKit.TEXT);
            lblMusic.setColor(focusRow == 1 ? UiKit.GOLD : UiKit.TEXT);
            lblSfx.setColor(focusRow == 2 ? UiKit.GOLD : UiKit.TEXT);
            btnClose.setText(focusRow == 3 ? LanguageManager.get("SAVE_CLOSE_ENTER") : LanguageManager.get("SAVE_CLOSE"));
        };
        refreshFocusVisual.run();

        keyListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (closed || !UiKit.isTopModal(stage, root)) return false;
                switch (keycode) {
                    case Input.Keys.ESCAPE:
                    case Input.Keys.BACK:
                        UiKit.consumeModalKey();
                        event.stop();
                        close(game);
                        return true;
                    case Input.Keys.UP:
                    case Input.Keys.W:
                        UiKit.consumeModalKey();
                        focusRow = (focusRow + 3) % 4;
                        refreshFocusVisual.run();
                        event.stop();
                        return true;
                    case Input.Keys.DOWN:
                    case Input.Keys.S:
                        UiKit.consumeModalKey();
                        focusRow = (focusRow + 1) % 4;
                        refreshFocusVisual.run();
                        event.stop();
                        return true;
                    case Input.Keys.LEFT:
                    case Input.Keys.A:
                        UiKit.consumeModalKey();
                        if (focusRow == 0) cycleLanguage.run();
                        else if (focusRow == 1) sldMusic.setValue(MathUtils.clamp(sldMusic.getValue() - 0.05f, 0f, 1f));
                        else if (focusRow == 2) sldSfx.setValue(MathUtils.clamp(sldSfx.getValue() - 0.05f, 0f, 1f));
                        event.stop();
                        return true;
                    case Input.Keys.RIGHT:
                    case Input.Keys.D:
                        UiKit.consumeModalKey();
                        if (focusRow == 0) cycleLanguage.run();
                        else if (focusRow == 1) sldMusic.setValue(MathUtils.clamp(sldMusic.getValue() + 0.05f, 0f, 1f));
                        else if (focusRow == 2) sldSfx.setValue(MathUtils.clamp(sldSfx.getValue() + 0.05f, 0f, 1f));
                        event.stop();
                        return true;
                    case Input.Keys.M:
                        UiKit.consumeModalKey();
                        toggleMusic.run();
                        event.stop();
                        return true;
                    case Input.Keys.ENTER:
                    case Input.Keys.NUMPAD_ENTER:
                    case Input.Keys.SPACE:
                        UiKit.consumeModalKey();
                        event.stop();
                        if (focusRow == 0) cycleLanguage.run();
                        else if (focusRow == 1) toggleMusic.run();
                        else if (focusRow == 2) toggleSfx.run();
                        else close(game);
                        return true;
                }
                return false;
            }
        };
        stage.addCaptureListener(keyListener);

        box.pack();
        box.setPosition((root.getWidth() - box.getWidth()) / 2f, (root.getHeight() - box.getHeight()) / 2f);
        root.addActor(box);
    }

    private static TextButton toggleButton(boolean on) {
        String label = on ? LanguageManager.get("ON") : LanguageManager.get("OFF");
        TextButton b = UiKit.button(label, on ? UiKit.GREEN : UiKit.GRAY, UiKit.normal());
        return b;
    }

    private static Slider slider(float value) {
        Slider.SliderStyle st = new Slider.SliderStyle();
        st.background = UiKit.rounded(new Color(0.20f, 0.21f, 0.26f, 1f));
        st.background.setMinHeight(8f);
        st.knobBefore = UiKit.rounded(UiKit.GOLD_DIM);
        st.knobBefore.setMinHeight(8f);
        st.knob = UiKit.framed(UiKit.GOLD, Color.WHITE);
        st.knob.setMinWidth(22f);
        st.knob.setMinHeight(22f);
        Slider s = new Slider(0f, 1f, 0.05f, false, st);
        s.setValue(value);
        return s;
    }

    public void close(Main game) {
        if (closed) return;
        closed = true;
        if (stage != null && keyListener != null) {
            stage.removeCaptureListener(keyListener);
        }
        PlayerProfile.getInstance().save();
        root.remove();
        if (onClose != null) onClose.run();
    }
}
