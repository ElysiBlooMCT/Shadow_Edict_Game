package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class DamageText {
    public float x, y;
    public float vx, vy;
    public String text;
    public Color color;
    public float lifeTime = 0.85f;
    public float maxLife = 0.85f;
    public float scale = 1.4f;

    public DamageText(float x, float y, String text, Color color) {
        this.x = x + MathUtils.random(-0.15f, 0.15f);
        this.y = y;
        this.vx = MathUtils.random(-0.25f, 0.25f);
        this.vy = MathUtils.random(0.9f, 1.3f);
        this.text = text;
        this.color = color;
    }
}
