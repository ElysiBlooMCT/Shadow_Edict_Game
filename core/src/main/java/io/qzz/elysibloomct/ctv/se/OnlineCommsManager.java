package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import org.json.JSONObject;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Quản lý Chat văn bản & Voice Chat thời gian thực cho các chế độ trực tuyến (PVP, CO-OP, 5v5).
 * Âm thanh thu ở 8kHz Mono, nén chuẩn G.711 u-law + lọc khoảng lặng (VAD) để nhẹ băng thông.
 */
public class OnlineCommsManager {
    private static final int SAMPLE_RATE = 8000;
    private static final int CHUNK_SAMPLES = 800; // 100ms mỗi gói
    private static final int MAX_HISTORY = 60;
    private static final float VAD_RMS_THRESHOLD = 360f;

    public static final class ChatEntry {
        public final String sender;
        public final int team;
        public final String text;
        public final boolean isMe;

        public ChatEntry(String sender, int team, String text, boolean isMe) {
            this.sender = sender;
            this.team = team;
            this.text = text;
            this.isMe = isMe;
        }
    }

    private final Main game;
    public final Array<ChatEntry> history = new Array<>();
    public int unreadCount = 0;
    public String toastText = "";
    public float toastTimer = 0f;
    public String remoteBubbleText = "";
    public float remoteBubbleTimer = 0f;

    private volatile boolean micEnabled = false;
    private volatile boolean pushToTalkHeld = false;
    private volatile boolean disposed = false;
    private volatile boolean localSpeaking = false;
    private volatile String remoteSpeaker = "";
    private volatile float remoteSpeakTimer = 0f;

    private Thread captureThread;
    private Thread playbackThread;
    private AudioRecorder recorder;
    private AudioDevice player;
    private final ArrayBlockingQueue<short[]> playbackQueue = new ArrayBlockingQueue<>(24);

    public Runnable onChatUpdated = null;

    public OnlineCommsManager(Main game) {
        this.game = game;
    }

    public void update(float delta) {
        if (toastTimer > 0f) toastTimer = Math.max(0f, toastTimer - delta);
        if (remoteBubbleTimer > 0f) remoteBubbleTimer = Math.max(0f, remoteBubbleTimer - delta);
        if (remoteSpeakTimer > 0f) {
            remoteSpeakTimer = Math.max(0f, remoteSpeakTimer - delta);
            if (remoteSpeakTimer <= 0f) remoteSpeaker = "";
        }
    }

    public boolean sendChat(String rawText, String senderName, int myTeam) {
        if (rawText == null) return false;
        String clean = rawText.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.isEmpty()) return false;
        if (clean.length() > 120) clean = clean.substring(0, 120);

        String name = (senderName == null || senderName.trim().isEmpty()) ? "Sát Thủ" : senderName.trim();
        addEntry(new ChatEntry(name, myTeam, clean, true), false);

        NetworkManager nm = NetworkManager.getInstance();
        GameSocket socket = nm.socket;
        if (nm.isConnected() && socket != null && !nm.currentRoomCode.isEmpty()) {
            try {
                JSONObject pkt = new JSONObject();
                pkt.put("type", "chat");
                pkt.put("sender", name);
                pkt.put("team", myTeam);
                pkt.put("text", clean);
                socket.emit("playerAction", pkt);
                return true;
            } catch (Exception e) {
                Gdx.app.error("OnlineComms", "Loi gui chat", e);
            }
        }
        return false;
    }

    public void onIncomingChat(String sender, int team, String text, boolean chatPanelOpen) {
        if (text == null || text.trim().isEmpty()) return;
        String cleanSender = (sender == null || sender.trim().isEmpty()) ? "Đồng đạo" : sender.trim();
        String cleanText = text.trim();
        if (cleanText.length() > 140) cleanText = cleanText.substring(0, 140);

        addEntry(new ChatEntry(cleanSender, team, cleanText, false), !chatPanelOpen);
        toastText = cleanSender + ": " + cleanText;
        toastTimer = 4.5f;
        remoteBubbleText = cleanText;
        remoteBubbleTimer = 4.2f;
    }

    private void addEntry(ChatEntry entry, boolean countUnread) {
        if (history.size >= MAX_HISTORY) history.removeIndex(0);
        history.add(entry);
        if (countUnread) unreadCount++;
        if (onChatUpdated != null) onChatUpdated.run();
    }

    /** Bật/tắt chế độ thu âm rảnh tay. Trả về chuỗi thông báo cho HUD. */
    /** Trình duyệt (bản web) chưa hỗ trợ thu/phát âm thanh trực tiếp: tắt đàm thoại micro, vẫn giữ chat chữ. */
    public static boolean isVoiceSupported() {
        return Gdx.app == null || Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.WebGL;
    }

    public String toggleMic() {
        if (!isVoiceSupported()) {
            micEnabled = false;
            return "Bản trình duyệt chưa hỗ trợ đàm thoại Micro – hãy dùng chat chữ.";
        }
        if (micEnabled) {
            micEnabled = false;
            localSpeaking = false;
            return "Đã tắt Micro.";
        }
        if (!ensureMicPermission()) {
            return "Đang xin quyền Micro – hãy bấm Cho phép rồi bật lại!";
        }
        micEnabled = true;
        ensureCaptureThread();
        return "Đã bật Micro đàm thoại trực tiếp!";
    }

    public void setPushToTalk(boolean held) {
        if (!isVoiceSupported()) return;
        if (held && !pushToTalkHeld) {
            if (!ensureMicPermission()) return;
            pushToTalkHeld = true;
            ensureCaptureThread();
        } else if (!held) {
            pushToTalkHeld = false;
            if (!micEnabled) localSpeaking = false;
        }
    }

    public boolean isMicActive() {
        return micEnabled || pushToTalkHeld;
    }

    public boolean isMicToggledOn() {
        return micEnabled;
    }

    public boolean isLocalSpeaking() {
        return localSpeaking && isMicActive();
    }

    public String getRemoteSpeaker() {
        return remoteSpeakTimer > 0f ? remoteSpeaker : "";
    }

    private boolean ensureMicPermission() {
        if (game != null && game.micPermissionChecker != null) {
            try {
                if (!game.micPermissionChecker.getAsBoolean()) {
                    if (game.micPermissionRequester != null) game.micPermissionRequester.run();
                    return false;
                }
            } catch (Throwable ignored) {}
        }
        return true;
    }

    private synchronized void ensureCaptureThread() {
        if (disposed) return;
        if (captureThread != null && captureThread.isAlive()) return;

        captureThread = new Thread(() -> {
            short[] pcmBuf = new short[CHUNK_SAMPLES];
            byte[] ulawBuf = new byte[CHUNK_SAMPLES];
            int hangoverChunks = 0;

            try {
                recorder = Gdx.audio.newAudioRecorder(SAMPLE_RATE, true);
            } catch (Throwable t) {
                Gdx.app.error("OnlineComms", "Khong mo duoc AudioRecorder", t);
                micEnabled = false;
                pushToTalkHeld = false;
                localSpeaking = false;
                return;
            }

            try {
                while (!disposed) {
                    if (!micEnabled && !pushToTalkHeld) {
                        localSpeaking = false;
                        try { Thread.sleep(40); } catch (InterruptedException ie) { break; }
                        continue;
                    }
                    recorder.read(pcmBuf, 0, CHUNK_SAMPLES);

                    long sumSq = 0L;
                    for (int i = 0; i < CHUNK_SAMPLES; i++) {
                        int s = pcmBuf[i];
                        sumSq += (long) s * s;
                    }
                    float rms = (float) Math.sqrt((double) sumSq / CHUNK_SAMPLES);
                    if (rms >= VAD_RMS_THRESHOLD || pushToTalkHeld) {
                        if (rms >= VAD_RMS_THRESHOLD * 0.65f) hangoverChunks = 3;
                    }
                    if (rms >= VAD_RMS_THRESHOLD || hangoverChunks > 0) {
                        if (rms < VAD_RMS_THRESHOLD) hangoverChunks--;
                        localSpeaking = true;
                        sendVoicePacket(pcmBuf, ulawBuf);
                    } else {
                        localSpeaking = false;
                    }
                }
            } catch (Throwable t) {
                Gdx.app.error("OnlineComms", "Loi luong thu am", t);
            } finally {
                localSpeaking = false;
                if (recorder != null) {
                    try { recorder.dispose(); } catch (Throwable ignored) {}
                    recorder = null;
                }
            }
        }, "AL-VoiceCapture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void sendVoicePacket(short[] pcm, byte[] ulawOut) {
        NetworkManager nm = NetworkManager.getInstance();
        GameSocket socket = nm.socket;
        if (!nm.isConnected() || socket == null || nm.currentRoomCode.isEmpty()) return;

        for (int i = 0; i < CHUNK_SAMPLES; i++) {
            // Khuếch đại nhẹ 1.25x để giọng rõ hơn trên loa điện thoại
            int boosted = Math.max(-32767, Math.min(32767, (pcm[i] * 5) >> 2));
            ulawOut[i] = linearToUlaw(boosted);
        }
        String b64 = String.valueOf(Base64Coder.encode(ulawOut));
        String sender = (game != null && game.playerName != null && !game.playerName.isEmpty())
            ? game.playerName : "Đồng đạo";
        try {
            JSONObject pkt = new JSONObject();
            pkt.put("type", "voice");
            pkt.put("sender", sender);
            pkt.put("pcm", b64);
            socket.emit("playerAction", pkt);
        } catch (Exception ignored) {}
    }

    public void onIncomingVoice(String sender, String base64Ulaw) {
        if (!isVoiceSupported()) return;
        if (disposed || base64Ulaw == null || base64Ulaw.isEmpty()) return;
        try {
            byte[] ulaw = Base64Coder.decode(base64Ulaw);
            if (ulaw == null || ulaw.length == 0 || ulaw.length > 3200) return;
            short[] pcm = new short[ulaw.length];
            for (int i = 0; i < ulaw.length; i++) {
                pcm[i] = ulawToLinear(ulaw[i]);
            }
            if (!playbackQueue.offer(pcm)) {
                playbackQueue.poll(); // Bỏ gói cũ nếu mạng dồn cục để không trễ tiếng
                playbackQueue.offer(pcm);
            }
            remoteSpeaker = (sender == null || sender.isEmpty()) ? "Đồng đạo" : sender;
            remoteSpeakTimer = 0.35f;
            ensurePlaybackThread();
        } catch (Throwable ignored) {}
    }

    private synchronized void ensurePlaybackThread() {
        if (disposed) return;
        if (playbackThread != null && playbackThread.isAlive()) return;

        playbackThread = new Thread(() -> {
            try {
                player = Gdx.audio.newAudioDevice(SAMPLE_RATE, true);
            } catch (Throwable t) {
                Gdx.app.error("OnlineComms", "Khong mo duoc AudioDevice", t);
                return;
            }
            try {
                while (!disposed) {
                    short[] samples = playbackQueue.take();
                    if (disposed) break;
                    player.writeSamples(samples, 0, samples.length);
                }
            } catch (InterruptedException ignored) {
            } catch (Throwable t) {
                Gdx.app.error("OnlineComms", "Loi luong phat voice", t);
            } finally {
                if (player != null) {
                    try { player.dispose(); } catch (Throwable ignored) {}
                    player = null;
                }
            }
        }, "AL-VoicePlayback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    // Chuẩn nén âm thanh ITU-T G.711 u-law (16-bit PCM <-> 8-bit u-law)
    private static byte linearToUlaw(int pcm) {
        int sign = 0;
        if (pcm < 0) {
            sign = 0x80;
            pcm = -pcm;
        }
        if (pcm > 32635) pcm = 32635;
        pcm += 0x84;
        int exponent = 7;
        for (int mask = 0x4000; (pcm & mask) == 0 && exponent > 0; exponent--, mask >>= 1) {}
        int mantissa = (pcm >> (exponent + 3)) & 0x0F;
        return (byte) ~(sign | (exponent << 4) | mantissa);
    }

    private static short ulawToLinear(byte ulawByte) {
        int u = ~ulawByte & 0xFF;
        int sign = u & 0x80;
        int exponent = (u >> 4) & 0x07;
        int mantissa = u & 0x0F;
        int sample = (((mantissa << 3) + 0x84) << exponent) - 0x84;
        return (short) (sign != 0 ? -sample : sample);
    }

    public void dispose() {
        disposed = true;
        micEnabled = false;
        pushToTalkHeld = false;
        localSpeaking = false;
        playbackQueue.clear();
        if (captureThread != null) captureThread.interrupt();
        if (playbackThread != null) playbackThread.interrupt();
    }
}
