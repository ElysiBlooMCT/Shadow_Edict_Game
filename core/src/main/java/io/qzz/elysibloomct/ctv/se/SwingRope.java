package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Dây đu vật lý (Physics Pendulum Swing Rope)
 * Hoạt động theo nguyên lý con lắc vật lý: chịu ảnh hưởng của trọng lực, quán tính, lực cản không khí
 * và cho phép người chơi lấy đà (pump torque) cũng như phóng người theo hướng véc-tơ tiếp tuyến.
 */
public class SwingRope {
    public final Vector2 anchor = new Vector2();
    public float length = 2.6f;
    public float width = 0.08f;

    // Góc con lắc (radians): 0 = thẳng đứng chúc xuống dưới
    // > 0: nghiêng sang phải (+x); < 0: nghiêng sang trái (-x)
    public float angle = 0f;
    public float angularVelocity = 0f;
    public float angularAcceleration = 0f;

    public boolean isGrabbed = false;
    // Vector dùng lại để không cấp phát mỗi frame (getTipPosition được gọi nhiều lần/frame)
    private final Vector2 tmpTip = new Vector2();
    private final Vector2 tmpVel = new Vector2();
    private static final float GRAVITY_ACCEL = 14.5f; // Gia tốc trọng trường cho con lắc (m/s^2)
    private static final float MAX_ANGLE = 1.30f;     // ~75 độ tối đa tránh lộn ngược vòng

    public SwingRope(float anchorX, float anchorY, float length) {
        this.anchor.set(anchorX, anchorY);
        this.length = Math.max(0.1f, length);
    }

    public SwingRope(float anchorX, float anchorY) {
        this(anchorX, anchorY, 2.6f);
    }

    /**
     * Cập nhật chuyển động con lắc dây theo quán tính và trọng lực
     */
    public void update(float delta, boolean inputLeft, boolean inputRight) {
        float dt = MathUtils.clamp(delta, 0f, 1f / 30f);
        if (dt <= 0f) return;

        // 1. Gia tốc trọng trường khôi phục vị trí cân bằng: alpha = -(g / L) * sin(theta)
        angularAcceleration = -(GRAVITY_ACCEL / length) * MathUtils.sin(angle);

        if (isGrabbed) {
            // Lực cản không khí nhẹ khi có người đu
            angularAcceleration -= 0.18f * angularVelocity;

            // Người chơi chủ động lấy đà (Pumping force):
            if (inputRight) {
                angularAcceleration += 5.2f;
            } else if (inputLeft) {
                angularAcceleration -= 5.2f;
            }
        } else {
            // Khi không ai bám: lực cản không khí tự nhiên dập tắt dao động
            angularAcceleration -= 1.15f * angularVelocity;
        }

        // Tích phân Euler bán ẩn (Semi-implicit Euler integration)
        angularVelocity += angularAcceleration * dt;

        // Giới hạn vận tốc góc tối đa tránh quay tít
        angularVelocity = MathUtils.clamp(angularVelocity, -6.5f, 6.5f);

        angle += angularVelocity * dt;

        // Giới hạn góc dao động an toàn
        if (angle > MAX_ANGLE) {
            angle = MAX_ANGLE;
            if (angularVelocity > 0) angularVelocity = -angularVelocity * 0.25f; // Nảy nhẹ trở lại
        } else if (angle < -MAX_ANGLE) {
            angle = -MAX_ANGLE;
            if (angularVelocity < 0) angularVelocity = -angularVelocity * 0.25f;
        }

        // Tự động dừng hẳn khi dao động quá nhỏ
        if (!isGrabbed && Math.abs(angle) < 0.008f && Math.abs(angularVelocity) < 0.015f) {
            angle = 0f;
            angularVelocity = 0f;
            angularAcceleration = 0f;
        }
    }

    /**
     * Người chơi bám vào dây: chuyển hóa vận tốc người chơi thành vận tốc góc ban đầu
     */
    public void grab(Vector2 playerVel) {
        isGrabbed = true;
        if (playerVel != null) {
            // Chiếu vận tốc lên phương tiếp tuyến vòng cung: v_tangent = vx * cos(theta) + vy * sin(theta)
            float vTangent = playerVel.x * MathUtils.cos(angle) + playerVel.y * MathUtils.sin(angle);
            this.angularVelocity = MathUtils.clamp(vTangent / length, -5.5f, 5.5f);
        }
    }

    /**
     * Buông tay khỏi dây
     */
    public void release() {
        isGrabbed = false;
    }

    /**
     * Tác động lực đẩy từ bên ngoài (ví dụ gió, nhân vật chạy lướt qua quẹt trúng dây)
     */
    public void applyImpulse(float impulse) {
        this.angularVelocity += impulse / length;
    }

    /**
     * Tọa độ đầu mút đu dây (Tip Position).
     * Lưu ý: trả về vector dùng chung của dây, không giữ tham chiếu qua frame sau.
     */
    public Vector2 getTipPosition() {
        float x = anchor.x + length * MathUtils.sin(angle);
        float y = anchor.y - length * MathUtils.cos(angle);
        return tmpTip.set(x, y);
    }

    /**
     * Véc-tơ vận tốc tiếp tuyến tại đầu mút dây (dùng khi phóng người khỏi dây)
     */
    public Vector2 getTangentialVelocity() {
        float vTangent = angularVelocity * length;
        float vx = vTangent * MathUtils.cos(angle);
        float vy = vTangent * MathUtils.sin(angle);
        return tmpVel.set(vx, vy);
    }

    /**
     * Vẽ dây đu xoay quanh chốt neo (anchor)
     */
    public void draw(Batch batch, TextureRegion ropeRegion, TextureRegion mountTexture) {
        if (ropeRegion == null) return;

        // LibGDX xoay ngược chiều kim đồng hồ khi góc dương. Dây treo hướng xuống (0,-1) xoay CCW góc θ
        // sẽ có đầu mút tại (sin θ, -cos θ) -> khớp với getTipPosition(). Bản cũ dùng -angle nên
        // sợi dây được vẽ lệch sang phía đối diện với nút thắt/nhân vật đang đu.
        float rotDeg = angle * MathUtils.radiansToDegrees;

        // Vẽ dây với điểm neo xoay (origin) nằm tại đỉnh trên cùng của dây (width/2, length)
        batch.draw(ropeRegion,
                anchor.x - width / 2f, anchor.y - length,
                width / 2f, length,
                width, length,
                1.0f, 1.0f,
                rotDeg);

        // Vẽ chốt neo kim loại cố định trên trần/xà nhà
        if (mountTexture != null) {
            batch.setColor(0.35f, 0.35f, 0.40f, 1.0f);
            batch.draw(mountTexture, anchor.x - 0.10f, anchor.y - 0.08f, 0.20f, 0.16f);
            batch.setColor(Color.WHITE);
        }

        // Vẽ nút thắt đầu dây ở dưới
        Vector2 tip = getTipPosition();
        if (mountTexture != null) {
            batch.setColor(0.55f, 0.38f, 0.18f, 1.0f);
            batch.draw(mountTexture, tip.x - 0.07f, tip.y - 0.07f, 0.14f, 0.14f);
            batch.setColor(Color.WHITE);
        }
    }
}
