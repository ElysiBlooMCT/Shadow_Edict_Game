package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.math.Vector2;

/**
 * Bụi rậm ẩn nấp chiến thuật (Tactical Ambush Bush)
 * Người chơi hoặc Bot khi bước vào bụi rậm sẽ được tàng hình,
 * kẻ địch bên ngoài không thấy thanh máu và đòn đánh đầu tiên gây sát thương bạo kích (+40% DMG).
 */
public class TacticalBush {
    public float x;
    public float y;
    public float width;
    public float height;

    public TacticalBush(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(Vector2 pos) {
        if (pos == null) return false;
        return pos.x >= x && pos.x <= (x + width) && pos.y >= (y - 0.25f) && pos.y <= (y + height + 0.65f);
    }
}
