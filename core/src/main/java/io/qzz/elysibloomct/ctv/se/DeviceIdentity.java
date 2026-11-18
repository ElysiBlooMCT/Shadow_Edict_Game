package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.UUID;

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
 * Gọi qua reflection để module core không phụ thuộc thư viện Android.
 */
public final class DeviceIdentity {
    private static final String PREF_NAME = "AssassinsLegend_Device";
    private static String cached;

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
            if (Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.Android) {
                id = readAndroidId();
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

    /** Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID) qua reflection. */
    private static String readAndroidId() throws Exception {
        Object context = Gdx.app; // AndroidApplication kế thừa Activity (là một Context)
        Object resolver = context.getClass().getMethod("getContentResolver").invoke(context);
        Class<?> resolverClass = Class.forName("android.content.ContentResolver");
        Class<?> secure = Class.forName("android.provider.Settings$Secure");
        return (String) secure.getMethod("getString", resolverClass, String.class)
            .invoke(null, resolver, "android_id");
    }

    private static String readOrCreateLocalUuid() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            String uuid = prefs.getString("deviceUuid", "");
            if (uuid.isEmpty()) {
                uuid = UUID.randomUUID().toString();
                prefs.putString("deviceUuid", uuid);
                prefs.flush();
            }
            return uuid;
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
