package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Định danh cố định cho từng máy, KHÔNG cần tạo tài khoản.
 *
 * - Android: dùng Settings.Secure.ANDROID_ID. Từ Android 8, giá trị này giữ nguyên khi gỡ app
 *   rồi cài lại (cùng chữ ký APK + cùng người dùng máy), chỉ đổi khi khôi phục cài đặt gốc.
 *   Không cần quyền gì. (IMEI thì từ Android 10 app thường KHÔNG được đọc nữa.)
 * - Desktop: tạo UUID ngẫu nhiên 1 lần, lưu trong Preferences ở thư mục người dùng
 *   (không mất khi xóa/cài lại bản .jar).
 *
 * Server dùng định danh này để cấp lại đúng mã AL-XXXX cũ cho máy.
 * ANDROID_ID do AndroidLauncher cung cấp qua {@link #androidIdProvider} (core không phụ thuộc thư viện Android,
 * và không dùng reflection để bản web TeaVM dịch được).
 * - Web: UUID ngẫu nhiên lưu trong localStorage của trình duyệt.
 */
public final class DeviceIdentity {
    private static final String PREF_NAME = "AssassinsLegend_Device";
    private static String cached;

    /** AndroidLauncher gán hàm đọc Settings.Secure.ANDROID_ID. Các nền tảng khác để null. */
    public static Supplier<String> androidIdProvider;

    private DeviceIdentity() {}

    public static synchronized String get() {
        if (cached != null) return cached;
        String prop = System.getProperty("al.device");
        if (prop != null && !prop.trim().isEmpty()) {
            cached = "local-" + prop.trim();
            return cached;
        }
        String id = null;
        try {
            if (androidIdProvider != null && Gdx.app != null
                && Gdx.app.getType() == Application.ApplicationType.Android) {
                id = androidIdProvider.get();
            }
        } catch (Throwable t) {
            Gdx.app.error("DeviceIdentity", "Khong doc duoc ANDROID_ID", t);
        }
        if (id == null || id.isEmpty() || "9774d56d682e549c".equals(id)) {
            // 9774d56d682e549c: giá trị lỗi đã biết trên một số máy Android 2.x -> không dùng
            id = "local-" + readOrCreateLocalUuid();
        } else {
            id = "android-" + id;
        }
        cached = id;
        return id;
    }

    private static String readOrCreateLocalUuid() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            String uuid = prefs.getString("deviceUuid", "");
            if (uuid.isEmpty()) {
                uuid = randomUuid();
                prefs.putString("deviceUuid", uuid);
                prefs.flush();
            }
            return uuid;
        } catch (Exception e) {
            return randomUuid();
        }
    }

    /** UUID ngẫu nhiên dạng chuẩn 8-4-4-4-12 (không dùng java.util.UUID để chạy được trên web). */
    static String randomUuid() {
        final String hex = "0123456789abcdef";
        Random rng = new Random();
        StringBuilder sb = new StringBuilder(36);
        for (int i = 0; i < 32; i++) {
            if (i == 8 || i == 12 || i == 16 || i == 20) sb.append('-');
            int v = rng.nextInt(16);
            if (i == 12) v = 4;                    // phiên bản 4
            if (i == 16) v = 8 | (v & 3);          // biến thể RFC 4122
            sb.append(hex.charAt(v));
        }
        return sb.toString();
    }
}
