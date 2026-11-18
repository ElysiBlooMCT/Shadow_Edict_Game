package io.qzz.elysibloomct.ctv.se;

public class JumpPad {
    public float x, y, width, height;
    public float bounceForce;
    public float cooldown = 0f;
    public float animTime = 0f;

    public JumpPad(float x, float y, float width, float height, float bounceForce) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounceForce = bounceForce;
    }
}
