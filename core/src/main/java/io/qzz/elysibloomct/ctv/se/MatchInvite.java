package io.qzz.elysibloomct.ctv.se;

public class MatchInvite {
    public String inviterId = "";
    public String inviterName = "";
    public String roomCode = "";
    public String mode = "1v1"; // "1v1", "5v5", "coop"
    public String message = "";

    public MatchInvite() {}

    public MatchInvite(String inviterId, String inviterName, String roomCode, String mode, String message) {
        this.inviterId = inviterId;
        this.inviterName = inviterName;
        this.roomCode = roomCode;
        this.mode = mode;
        this.message = message;
    }
}
