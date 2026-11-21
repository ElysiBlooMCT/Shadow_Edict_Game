package io.qzz.elysibloomct.ctv.se;

/**
 * Kết nối Socket.IO tới server game, không phụ thuộc thư viện cụ thể.
 * - PC / Android: {@link JavaGameSocket} (thư viện socket.io-client bản Java).
 * - Trình duyệt: WebGameSocket trong module teavm (thư viện socket.io bản JavaScript).
 * Launcher của từng nền tảng gán {@link NetworkManager#socketFactory} trước khi game chạy.
 *
 * Dữ liệu nhận được (args) giữ đúng kiểu như thư viện Java: JSONObject, JSONArray, String,
 * Integer/Long/Double, Boolean.
 */
public abstract class GameSocket {
    public static final String EVENT_CONNECT = "connect";
    public static final String EVENT_CONNECT_ERROR = "connect_error";
    public static final String EVENT_DISCONNECT = "disconnect";

    /** Hàm nhận sự kiện. Cùng dạng với io.socket.emitter.Emitter.Listener nên lambda cũ dùng lại được. */
    public interface Listener {
        void call(Object... args);
    }

    /** Tạo kết nối (chưa kết nối ngay, phải gọi {@link #connect()}). */
    public interface Factory {
        GameSocket create(String url) throws Exception;
    }

    public abstract GameSocket on(String event, Listener listener);

    /** Nghe sự kiện đúng một lần. */
    public abstract GameSocket once(String event, Listener listener);

    /** Bỏ mọi hàm đang nghe sự kiện này. */
    public abstract GameSocket off(String event);

    public abstract GameSocket emit(String event, Object... args);

    public abstract boolean connected();

    /** Mã kết nối do server cấp (null khi chưa kết nối). */
    public abstract String id();

    public abstract GameSocket connect();

    public abstract GameSocket disconnect();
}
