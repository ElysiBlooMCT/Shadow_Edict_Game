package org.json;

/**
 * Bản org.json rút gọn cho bản web (TeaVM): cùng tên lớp và chữ ký phương thức với org.json 20090211
 * (bản đi kèm socket.io-client mà core được biên dịch với), nhưng không dùng reflection.
 */
public class JSONException extends Exception {
    private Throwable cause;

    public JSONException(String message) {
        super(message);
    }

    public JSONException(Throwable cause) {
        super(cause == null ? null : cause.getMessage());
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
