package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Màn hình tải: nạp trước toàn bộ sprite (mỗi frame 1 ảnh để không đơ cửa sổ)
 * và âm thanh, hiển thị thanh tiến trình. Nhờ vậy vào trận gần như tức thì.
 */
public class LoadingScreen extends ScreenAdapter {
    private static final float MIN_DISPLAY_TIME = 1.2f;

    private final Main game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image barFill;
    private Label lblStatus;
    private float timeElapsed = 0f;
    private int loadIndex = 0;
    private boolean finished = false;
    private boolean disposed = false;

    private static final float BAR_W = 640f, BAR_H = 14f;

    public LoadingScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(1280, 720));

        backgroundTexture = new Texture(Gdx.files.internal("loading.jpg"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(1280, 720);
        backgroundImage.setScaling(Scaling.fill);
        stage.addActor(backgroundImage);

        float barX = (1280 - BAR_W) / 2f, barY = 70f;
        Image barBg = new Image(UiKit.framed(new Color(0f, 0f, 0f, 0.65f), UiKit.GOLD_DIM));
        barBg.setBounds(barX - 4, barY - 4, BAR_W + 8, BAR_H + 8);
        stage.addActor(barBg);

        barFill = new Image(UiKit.rounded(UiKit.GOLD));
        barFill.setBounds(barX, barY, 0, BAR_H);
        stage.addActor(barFill);

        lblStatus = UiKit.label("Đang tải tài nguyên...", UiKit.normal(), UiKit.TEXT);
        lblStatus.setAlignment(Align.center);
        lblStatus.setBounds(0, barY + 24, 1280, 30);
        stage.addActor(lblStatus);

        // Khởi tạo âm thanh sớm (nạp file .wav/.mp3)
        SoundManager.getInstance();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        timeElapsed += delta;

        String[] list = GameAssets.PRELOAD_SPRITES;
        long frameBudgetNs = 12_000_000L; // tối đa ~12ms mỗi frame để thanh tải chạy nhanh mà không đơ UI
        long startNs = System.nanoTime();
        while (loadIndex < list.length && (System.nanoTime() - startNs) < frameBudgetNs) {
            GameAssets.sprite(list[loadIndex]);
            loadIndex++;
        }
        float progress = list.length == 0 ? 1f : (float) loadIndex / list.length;
        barFill.setWidth(Math.max(BAR_H, BAR_W * progress));
        lblStatus.setText("Đang tải tài nguyên... " + (int) (progress * 100) + "%");

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();

        if (!finished && loadIndex >= list.length && timeElapsed >= MIN_DISPLAY_TIME) {
            finished = true;
            GameAssets.logSummary();
            game.setScreen(new MainMenuScreen(game));
        }
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (stage != null) stage.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
}
