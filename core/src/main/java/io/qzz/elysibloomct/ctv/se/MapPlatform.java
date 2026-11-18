package io.qzz.elysibloomct.ctv.se;

public class MapPlatform {
    public float x, y, width, height;
    public boolean isGround;

    public MapPlatform(float x, float y, float width, float height, boolean isGround) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isGround = isGround;
    }
}
