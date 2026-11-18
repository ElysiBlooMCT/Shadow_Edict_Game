package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class HealthBar extends Actor {
    private float maxHp = 100f, currentHp = 100f, delayedHp = 100f;
    private final Drawable bgDrawable, fillDrawable, borderDrawable, ghostDrawable, shineDrawable, notchDrawable;
    private final boolean invertDirection;

    public HealthBar(Skin skin, Color fillColor, boolean invertDirection) {
        this.bgDrawable = skin.newDrawable("white", new Color(0.08f, 0.08f, 0.12f, 0.95f));
        this.fillDrawable = skin.newDrawable("white", fillColor);
        this.ghostDrawable = skin.newDrawable("white", new Color(1.0f, 0.75f, 0.2f, 0.9f));
        this.borderDrawable = skin.newDrawable("white", new Color(0.85f, 0.72f, 0.42f, 0.95f));
        this.shineDrawable = skin.newDrawable("white", new Color(1.0f, 1.0f, 1.0f, 0.25f));
        this.notchDrawable = skin.newDrawable("white", new Color(0.05f, 0.05f, 0.08f, 0.65f));
        this.invertDirection = invertDirection;
    }

    public void setMaxHp(float max) {
        this.maxHp = Math.max(1f, max);
    }

    public void setHp(float hp) {
        this.currentHp = MathUtils.clamp(hp, 0f, maxHp);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (delayedHp > currentHp) {
            delayedHp -= (delayedHp - currentHp) * 5.0f * delta;
            if (delayedHp - currentHp < 0.5f) delayedHp = currentHp;
        } else if (delayedHp < currentHp) {
            delayedHp = currentHp;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

        float border = 2.5f;
        // Outer metallic border
        borderDrawable.draw(batch, getX(), getY(), getWidth(), getHeight());

        float innerX = getX() + border, innerY = getY() + border;
        float innerW = getWidth() - border * 2, innerH = getHeight() - border * 2;
        // Dark inner slot
        bgDrawable.draw(batch, innerX, innerY, innerW, innerH);

        // 1. Draw delayed ghost damage bar (yellow/orange)
        float ghostPct = MathUtils.clamp(delayedHp / maxHp, 0f, 1f);
        float ghostW = innerW * ghostPct;
        if (ghostW > 0) {
            float ghostX = invertDirection ? (innerX + innerW - ghostW) : innerX;
            ghostDrawable.draw(batch, ghostX, innerY, ghostW, innerH);
        }

        // 2. Draw active health bar
        float pct = MathUtils.clamp(currentHp / maxHp, 0f, 1f);
        float fillW = innerW * pct;
        if (fillW > 0) {
            float fillX = invertDirection ? (innerX + innerW - fillW) : innerX;
            fillDrawable.draw(batch, fillX, innerY, fillW, innerH);

            // 3. Glass highlight shine on upper half
            float shineH = innerH * 0.42f;
            shineDrawable.draw(batch, fillX, innerY + innerH - shineH, fillW, shineH);
        }

        // 4. Subtle notch marks every 25% of max HP
        float notchWidth = 1.5f;
        for (int i = 1; i <= 3; i++) {
            float notchX = innerX + (innerW * (i * 0.25f));
            notchDrawable.draw(batch, notchX, innerY, notchWidth, innerH);
        }
    }
}
