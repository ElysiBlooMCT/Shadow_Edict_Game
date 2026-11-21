package io.qzz.elysibloomct.ctv.se;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;

/**
 * Kết nối Socket.IO dùng thư viện socket.io-client bản Java (PC, Android).
 * Không được tham chiếu từ code chung trong core, để bản web (TeaVM) không phải dịch thư viện này:
 * chỉ Lwjgl3Launcher và AndroidLauncher gán {@code NetworkManager.socketFactory = JavaGameSocket::create}.
 */
public final class JavaGameSocket extends GameSocket {
    private final Socket socket;

    private JavaGameSocket(Socket socket) {
        this.socket = socket;
    }

    public static GameSocket create(String url) throws Exception {
        IO.Options options = new IO.Options();
        options.transports = new String[] { WebSocket.NAME };
        options.reconnection = true;
        options.reconnectionAttempts = Integer.MAX_VALUE; // tự kết nối lại suốt phiên chơi
        options.reconnectionDelayMax = 8000;
        options.reconnectionDelay = 1000;
        options.timeout = 5000;
        return new JavaGameSocket(IO.socket(url, options));
    }

    @Override
    public GameSocket on(String event, Listener listener) {
        socket.on(event, listener::call);
        return this;
    }

    @Override
    public GameSocket once(String event, Listener listener) {
        socket.once(event, listener::call);
        return this;
    }

    @Override
    public GameSocket off(String event) {
        socket.off(event);
        return this;
    }

    @Override
    public GameSocket emit(String event, Object... args) {
        socket.emit(event, args);
        return this;
    }

    @Override
    public boolean connected() {
        return socket.connected();
    }

    @Override
    public String id() {
        return socket.id();
    }

    @Override
    public GameSocket connect() {
        socket.connect();
        return this;
    }

    @Override
    public GameSocket disconnect() {
        socket.disconnect();
        return this;
    }
}
