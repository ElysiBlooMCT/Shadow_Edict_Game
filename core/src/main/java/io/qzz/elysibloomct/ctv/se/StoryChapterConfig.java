package io.qzz.elysibloomct.ctv.se;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cốt truyện chế độ CHƠI ĐƠN: "HUYẾT LỆ HOÀNG TRIỀU" – 3 MÀN CHƠI x 3 ẢI = 9 ẢI.
 *
 * MAP 1: THÀNH CỔ THỜI CHIẾN (Ảnh: map1_thanhco.png)
 *  - Ải 1-1: Khởi Binh Ngoại Thành (Làm quen chạy nhảy, vượt thùng hàng, leo ban công)
 *  - Ải 1-2: Mái Ngói Mê Cung (Di chuyển mái nhà cao thấp, đu dây vượt hẻm, chui qua cổng kẹt, cứu Tiểu Vũ)
 *  - Ải 1-3: Quyết Chiến Tiền Đồn (Tiến vào sân thành, hạ Đội Trưởng Tiền Đồn)
 *
 * MAP 2: BẾN CẢNG BAN ĐÊM (Ảnh: map2_bencang.png)
 *  - Ải 2-1: Sóng Gió Bến Cảng (Cầu cảng gãy trên nước sâu, leo kiện hàng, sàn kho)
 *  - Ải 2-2: Chiến Thuyền Hắc Ám (Đu dây cần trục đáp boong tàu, chui khoang hàng tránh cung thủ)
 *  - Ải 2-3: Tử Thủ Quân Cảng (Sàn gỗ sập chậm, công phá cổng quân cảng, hạ Thống Chế Thủy Quân)
 *
 * MAP 3: PHÁO ĐÀI TRÊN NÚI (Ảnh: map3_phaodai.png)
 *  - Ải 3-1: Dốc Đá Nguy Hiểm (Bậc đá dốc, bám mép tường thành, nhảy tường đổ nát)
 *  - Ải 3-2: Vực Sâu Xích Treo (Đu dây xích qua vực mỏm núi, chui đường hầm tường thủng)
 *  - Ải 3-3: Ngai Vàng Bão Táp (Chuỗi thử thách kết hợp, checkpoint trước trận cuối, đại chiến Nhiếp Chính Vương)
 */
public final class StoryChapterConfig {

    public static final int MAX_CHAPTERS = 3;
    public static final int STAGES_PER_CHAPTER = 3;
    public static final int TOTAL_STAGES = MAX_CHAPTERS * STAGES_PER_CHAPTER; // 9 ải

    /** Tiểu Vũ trong ải: không có / phải giải cứu / đã đi cùng (hộ tống). */
    public enum BoyMode { NONE, RESCUE, ESCORT }

    // Mã ảnh đại diện trong hộp thoại
    public static final String AV_P1 = "P1", AV_P2 = "P2", AV_BOY = "BOY", AV_BOSS = "BOSS",
        AV_SAGE = "SAGE", AV_ELDER = "ELDER", AV_NARRATOR = "NARRATOR";

    // Tên người nói
    static final String TIEU = "VÂN TIÊU", U = "VÂN U", VU = "TIỂU VŨ", SAGE = "MẶC LÃO",
        THAIY = "LÃO THÁI Y", REGENT = "NHIẾP CHÍNH VƯƠNG", NARR = "LỜI DẪN";

    /** Một câu thoại (bất biến – GameScreen tự tính điểm camera hướng tới). */
    public static final class StoryLine {
        public final String speaker, text, avatar;
        public final boolean monologue;

        public StoryLine(String speaker, String text, String avatar, boolean monologue) {
            this.speaker = speaker;
            this.text = text;
            this.avatar = avatar;
            this.monologue = monologue;
        }
    }

    /** Thông số sinh bản đồ theo màn chơi. */
    public static final class MapTheme {
        public final String name;
        public final String bgImageName;
        public final float gapMin, gapMax, platMin, platMax;
        public final float buildingChance, buildingMinH, buildingMaxH;
        public final float midPlatChance, ladderChance, bushChance;
        public final float ropeGap;          // vực rộng >= giá trị này sẽ có dây đu
        public final int padEvery;           // cứ N đoạn sàn đặt 1 bệ nhảy (0 = không)
        public final float bgR, bgG, bgB;    // sắc thái ảnh nền
        public final float groundR, groundG, groundB; // màu địa hình lớp trước

        MapTheme(String name, String bgImageName, float gapMin, float gapMax, float platMin, float platMax,
                 float buildingChance, float buildingMinH, float buildingMaxH,
                 float midPlatChance, float ladderChance, float bushChance, float ropeGap, int padEvery,
                 float bgR, float bgG, float bgB, float groundR, float groundG, float groundB) {
            this.name = name;
            this.bgImageName = bgImageName;
            this.gapMin = gapMin; this.gapMax = gapMax; this.platMin = platMin; this.platMax = platMax;
            this.buildingChance = buildingChance; this.buildingMinH = buildingMinH; this.buildingMaxH = buildingMaxH;
            this.midPlatChance = midPlatChance; this.ladderChance = ladderChance; this.bushChance = bushChance;
            this.ropeGap = ropeGap; this.padEvery = padEvery;
            this.bgR = bgR; this.bgG = bgG; this.bgB = bgB;
            this.groundR = groundR; this.groundG = groundG; this.groundB = groundB;
        }
    }

    // 3 Màn chơi với 3 ảnh nền đặc trưng và thông số địa hình cổ điển chiến tranh
    private static final MapTheme[] THEMES = {
        // Map 1: Thành Cổ Thời Chiến (map1_thanhco.png) - Đá cũ, gỗ mục, tường thành, mái nhà hoàng hôn
        new MapTheme("THÀNH CỔ THỜI CHIẾN", "map1_thanhco.png",
            2.2f, 3.2f, 12f, 18f, 0.60f, 4.8f, 6.5f, 0.65f, 0.50f, 0.50f, 3.0f, 2,
            1.00f, 0.88f, 0.82f, 0.95f, 0.88f, 0.78f),

        // Map 2: Bến Cảng Ban Đêm (map2_bencang.png) - Cầu cảng, nước sâu hazard, sàn kho, cần trục, tàu buồm
        new MapTheme("BẾN CẢNG BAN ĐÊM", "map2_bencang.png",
            2.5f, 3.6f, 10f, 15f, 0.50f, 5.0f, 7.0f, 0.70f, 0.60f, 0.35f, 2.9f, 2,
            0.85f, 0.90f, 1.05f, 0.80f, 0.85f, 0.95f),

        // Map 3: Pháo Đài Trên Núi (map3_phaodai.png) - Bậc đá dốc, tường vỡ, vực sâu núi non, sân thượng boss
        new MapTheme("PHÁO ĐÀI TRÊN NÚI", "map3_phaodai.png",
            2.6f, 3.8f, 11f, 16f, 0.70f, 5.5f, 7.8f, 0.75f, 0.70f, 0.25f, 3.0f, 2,
            0.95f, 0.95f, 1.00f, 0.90f, 0.90f, 0.95f),
    };

    public static final class StageData {
        public final int chapter, stage, index;
        public final String chapterTitle, stageTitle, objectiveText, victoryLore;
        public final float mapWidth;
        public final int groundMobCount;
        public final boolean hasFlyingMobs, hasSkyPirates, hasBoss;
        public final int bossMaxHp;
        public final String bossName;
        public final BoyMode boyMode;
        public final MapTheme theme;
        public boolean startWithShield = false;
        public final List<StoryLine> introDialogues = new ArrayList<>();
        public final List<StoryLine> bossDialogues = new ArrayList<>();
        public final List<StoryLine> outroDialogues = new ArrayList<>();

        StageData(int chapter, int stage, String stageTitle, String objective, float mapWidth, int mobs,
                  boolean flying, boolean pirates, BoyMode boy, String bossName, int bossHp, String victoryLore) {
            this.chapter = chapter;
            this.stage = stage;
            this.index = (chapter - 1) * STAGES_PER_CHAPTER + stage;
            this.chapterTitle = CHAPTER_TITLES[chapter - 1];
            this.stageTitle = stageTitle;
            this.objectiveText = objective;
            this.mapWidth = mapWidth;
            this.groundMobCount = mobs;
            this.hasFlyingMobs = flying;
            this.hasSkyPirates = pirates;
            this.boyMode = boy;
            this.hasBoss = bossName != null;
            this.bossName = bossName;
            this.bossMaxHp = bossHp;
            this.victoryLore = victoryLore;
            this.theme = THEMES[chapter - 1];
        }

        StageData intro(String speaker, String avatar, String text) { introDialogues.add(new StoryLine(speaker, text, avatar, false)); return this; }
        StageData introThought(String speaker, String avatar, String text) { introDialogues.add(new StoryLine(speaker, text, avatar, true)); return this; }
        StageData boss(String speaker, String avatar, String text) { bossDialogues.add(new StoryLine(speaker, text, avatar, false)); return this; }
        StageData outro(String speaker, String avatar, String text) { outroDialogues.add(new StoryLine(speaker, text, avatar, false)); return this; }
        StageData shield() { startWithShield = true; return this; }

        /** "Màn 1 · Ải 2" */
        public String shortLabel() { return "Màn " + chapter + " · Ải " + stage; }
    }

    public static final String[] CHAPTER_TITLES = {
        "THÀNH CỔ THỜI CHIẾN", "BẾN CẢNG BAN ĐÊM", "PHÁO ĐÀI TRÊN NÚI"
    };

    private static final StageData[] STAGES = new StageData[TOTAL_STAGES];
    public static final List<StoryLine> PROLOGUE = new ArrayList<>();
    public static final List<StoryLine> ENDING_P1 = new ArrayList<>();
    public static final List<StoryLine> ENDING_P2 = new ArrayList<>();

    private StoryChapterConfig() {}

    public static int chapterOf(int index) { return (clampIndex(index) - 1) / STAGES_PER_CHAPTER + 1; }
    public static int stageOf(int index) { return (clampIndex(index) - 1) % STAGES_PER_CHAPTER + 1; }
    public static int indexOf(int chapter, int stage) { return (chapter - 1) * STAGES_PER_CHAPTER + stage; }
    public static int clampIndex(int index) { return Math.max(1, Math.min(TOTAL_STAGES, index)); }
    public static StageData get(int index) { return STAGES[clampIndex(index) - 1]; }
    public static StageData getStageData(int chapter, int stage) {
        int c = Math.max(1, Math.min(MAX_CHAPTERS, chapter));
        int s = Math.max(1, Math.min(STAGES_PER_CHAPTER, stage));
        return get(indexOf(c, s));
    }

    public static List<StoryLine> ending(boolean blackAssassin) {
        return Collections.unmodifiableList(blackAssassin ? ENDING_P2 : ENDING_P1);
    }

    private static StageData add(StageData d) { STAGES[d.index - 1] = d; return d; }

    static {
        final BoyMode N = BoyMode.NONE, R = BoyMode.RESCUE, E = BoyMode.ESCORT;

        // ============================ LỜI DẪN CỐT TRUYỆN ============================
        PROLOGUE.add(new StoryLine(NARR, "Mười năm trước, Tiên Hoàng băng hà sau chén độc dược. Nhiếp Chính Vương Triệu Vô Cực chiếm binh quyền, đổ oan cho Vân Gia sát quân.", AV_NARRATOR, false));
        PROLOGUE.add(new StoryLine(NARR, "Trang viên Vân Gia chìm trong biển lửa. Vân Tiêu được Mặc lão cứu sống, Vân U bị giam mười năm ở ngục tối trước khi thoát thân.", AV_NARRATOR, false));
        PROLOGUE.add(new StoryLine(NARR, "Rằm tháng Tám – ngày Vô Cực định đăng cơ đã cận kề. Hai anh em Vân Gia cùng tái xuất để đoạt lại công lý và vạch trần kẻ cướp ngôi!", AV_NARRATOR, false));

        // ============================ MAP 1: THÀNH CỔ THỜI CHIẾN ============================
        // Ải 1-1: Làm quen chạy nhảy, vượt thùng hàng, leo ban công
        add(new StageData(1, 1, "Khởi Binh Ngoại Thành", "Làm quen nhịp chạy nhảy, vượt thùng hàng và bám mép leo lên ban công.",
            95f, 4, false, false, N, null, 0,
            "Đã vượt qua chòi canh ngoại ô. Phía trước là mê lộ mái nhà thành cổ."))
            .intro(TIEU, AV_P1, "Bắt đầu rồi, U. Ngoại thành đang thiết quân luật, toán lính tuần gác khắp các ngõ đá.")
            .intro(U, AV_P2, "Mười năm qua ta chỉ đợi ngày này. Thùng hàng và mái hiên kia là đường di chuyển hoàn hảo.")
            .intro(TIEU, AV_P1, "Nhớ giữ nhịp khinh công: nhảy sát mép tường để bám mép và leo lên ban công!");

        // Ải 1-2: Mái nhà cao thấp, đu dây vượt hẻm, chui qua chân cổng kẹt, cứu Tiểu Vũ
        add(new StageData(1, 2, "Mái Ngói Mê Cung", "Di chuyển qua mái nhà, đu dây vượt con hẻm, chui dưới chân cổng thành kẹt để cứu Cửu Hoàng Tử.",
            110f, 6, false, false, R, null, 0,
            "Tiểu Vũ đã được giải cứu an toàn cùng Huyết Thư di chiếu của Tiên Hoàng!"))
            .intro(VU, AV_BOY, "Cứu ta với...! Đừng để bọn sát thủ cướp bọc gấm của mẹ!")
            .intro(U, AV_P2, "Tiếng kêu từ phía con hẻm sâu! Cầu đã bị phá hủy, phải đu dây treo trên xà gỗ để vượt qua.")
            .intro(TIEU, AV_P1, "Cổng thành phía trước bị kẹt đá thấp – cúi thấp người chui qua khe hẹp để tập kích bọn lính áp giải!")
            .outro(VU, AV_BOY, "Ta là Triệu Khang... đây là Huyết Thư cha ta để lại trước khi bị Vô Cực hạ độc!")
            .outro(TIEU, AV_P1, "Huyết Thư bằng máu của Tiên Hoàng! Chúng ta phải bảo vệ hoàng tử đến cùng.");

        // Ải 1-3: Sân thành cổ, quyết chiến Đội Trưởng Tiền Đồn
        add(new StageData(1, 3, "Huyết Chiến Sân Thành", "Tiến vào sân thành cổ bằng phẳng, hạ gục Đội Trưởng Tiền Đồn để mở đường ra bến cảng.",
            120f, 6, false, false, E, "ĐỘI TRƯỞNG TIỀN ĐỒN", 1100,
            "Đội Trưởng Tiền Đồn đại bại. Cổng thành phía đông mở ra hướng về Bến Cảng Ban Đêm! – HẾT MÀN 1 –"))
            .intro(TIEU, AV_P1, "Sân thành phía trước rất rộng – chỗ hoàn hảo cho một trận quyết chiến.")
            .intro(VU, AV_BOY, "Đội Trưởng Tiền Đồn đang chắn cổng đông với đại đao!")
            .boss("ĐỘI TRƯỞNG TIỀN ĐỒN", AV_BOSS, "Tàn dư Vân Gia và thằng nhóc mang di chiếu! Các ngươi không bao giờ ra khỏi cổng thành này!")
            .boss(U, AV_P2, "Hãy để đao của ta trả lời ngươi!");

        // ============================ MAP 2: BẾN CẢNG BAN ĐÊM ============================
        // Ải 2-1: Cầu cảng gãy trên nước sâu (hazard), leo thùng hàng vào sàn kho
        add(new StageData(2, 1, "Sóng Gió Bến Cảng", "Nhảy giữa các đoạn cầu cảng gãy trên mặt nước sâu, leo thùng hàng vào sàn kho cảng.",
            115f, 6, false, false, E, null, 0,
            "Đã vượt qua cầu cảng gãy an toàn, thâm nhập thành công vào khu vực kho hàng bến tàu."))
            .intro(TIEU, AV_P1, "Bến Cảng Ban Đêm... Mù sương và gió biển rít từng cơn. Nước sâu dưới cầu cảng cực kỳ nguy hiểm, rơi xuống sẽ cuốn trôi ngay!")
            .intro(U, AV_P2, "Nhảy dứt khoát qua từng mố cầu gãy, bám vào các thùng hàng chất cao để leo lên sàn nhà kho.")
            .intro(VU, AV_BOY, "Ta sẽ theo sát hai huynh, không dám nhìn xuống nước đâu!");

        // Ải 2-2: Đu dây từ cần trục hàng xuống boong tàu, chui khoang hàng thấp tránh cung thủ
        add(new StageData(2, 2, "Chiến Thuyền Hắc Ám", "Đu dây cần trục đáp xuống boong tàu buồm, chui qua khoang hàng thấp để né cung thủ bắn tỉa.",
            125f, 7, false, true, E, null, 0,
            "Khoang chiến thuyền đã được kiểm soát. Cung thủ trên cột buồm bị triệt hạ."))
            .intro(U, AV_P2, "Bọn cung thủ mai phục trên cột buồm chiến thuyền ngoài xa. Không thể đi bộ qua được!")
            .intro(TIEU, AV_P1, "Cần trục bốc dỡ hàng có dây cáp dài buông xuống. Đu dây lấy đà phóng thẳng xuống boong tàu!")
            .intro(TIEU, AV_P1, "Khi tiếp đất boong tàu, lập tức cúi người chui qua khoang hàng thấp để tránh làn mưa tên!");

        // Ải 2-3: Sàn gỗ sập chậm sau khi dẫm 0.8s, tiến vào cổng quân cảng, hạ Thống Chế Thủy Quân
        add(new StageData(2, 3, "Tử Thủ Quân Cảng", "Vượt sàn gỗ mục sập chậm khi bước lên, công phá cổng quân cảng và hạ Thống Chế Thủy Quân.",
            135f, 8, true, true, E, "THỐNG CHẾ THỦY QUÂN", 1450,
            "Thống Chế Thủy Quân gục ngã. Chiến thuyền bọc thép sẵn sàng đưa đoàn người lên Pháo Đài Trên Núi! – HẾT MÀN 2 –"))
            .intro(VU, AV_BOY, "Sàn gỗ dẫn vào cổng quân cảng đã mục nát, nó đang rung lên kìa!")
            .intro(U, AV_P2, "Chỉ cần dẫm chân lên 0.8 giây là sàn sẽ gãy sập! Phải lướt nhanh không được dừng lại!")
            .boss("THỐNG CHẾ THỦY QUÂN", AV_BOSS, "Thủy quân hoàng gia phục mệnh Nhiếp Chính Vương! Không ai được rời bến cảng này sống sót!")
            .boss(TIEU, AV_P1, "Bến cảng này đã thuộc về Cửu Hoàng Tử. Ngươi hãy thoái vị!");

        // ============================ MAP 3: PHÁO ĐÀI TRÊN NÚI ============================
        // Ải 3-1: Bậc đá dốc, bám mép tường thành, nhảy qua đoạn tường bị phá hủy
        add(new StageData(3, 1, "Dốc Đá Nguy Hiểm", "Chinh phục bậc đá dốc đứng, bám mép tường thành đổ nát và nhảy qua các đoạn tường vỡ.",
            125f, 7, false, true, E, null, 0,
            "Đã leo lên tầng lũy giữa của pháo đài. Bão tuyết và sấm sét bắt đầu nổi lên dữ dội."))
            .intro(TIEU, AV_P1, "Chân núi pháo đài sừng sững trong giông bão. Vách đá dựng đứng với các bậc đá sứt mẻ.")
            .intro(U, AV_P2, "Đại bác công thành năm xưa đã phạt vỡ nhiều đoạn tường. Hãy tận dụng mép tường đá để bám và phóng lên.")
            .intro(VU, AV_BOY, "Gió trên núi mạnh quá... Hãy cẩn thận vực thẳm dưới chân!");

        // Ải 3-2: Đu xích vượt vực sâu giữa hai mỏm núi tới giàn giáo, chui qua đường hầm dưới cổng sắt
        add(new StageData(3, 2, "Vực Sâu Xích Treo", "Đu dây xích vượt vực thẳm giữa hai mỏm núi sang giàn giáo, chui qua đường hầm đá dưới cổng sắt.",
            135f, 8, true, true, E, null, 0,
            "Đã thâm nhập vào chân tháp tối cao. Mặc lão đã chuẩn bị Hộ Tâm Kính để đỡ Lôi Công!"))
            .intro(U, AV_P2, "Hai mỏm núi bị chia cắt bởi vực sâu vạn trượng! Chỉ có sợi xích neo cần cẩu vắt ngang.")
            .intro(TIEU, AV_P1, "Bắt lấy xích, đung đưa lấy đà tối đa để đáp sang giàn giáo kiên cố bên kia.")
            .intro(TIEU, AV_P1, "Phía sau giàn giáo là đường hầm nứt toác dưới cổng sắt kiên cố – cúi người chui qua!");

        // Ải 3-3: Chuỗi thử thách kết hợp, checkpoint trước trận cuối, sân thượng bằng phẳng hạ Nhiếp Chính Vương
        add(new StageData(3, 3, "Ngai Vàng Bão Táp", "Vượt chuỗi thử thách, kích hoạt checkpoint an toàn, đại chiến Nhiếp Chính Vương Triệu Vô Cực trên đỉnh pháo đài.",
            145f, 9, true, true, E, "NHIẾP CHÍNH VƯƠNG", 2000,
            "Nhiếp Chính Vương đền tội. Ám mây đen tan biến, bình minh rạng rỡ chiếu soi giang sơn! – HOÀN THÀNH CỐT TRUYỆN –"))
            .shield()
            .intro(SAGE, AV_SAGE, "Tiêu nhi, U nhi! Cầm lấy Hộ Tâm Kính. Ta đã mở sẵn Điểm Lưu (Checkpoint) an toàn trước thềm điện.")
            .intro(VU, AV_BOY, "Sân thượng pháo đài phẳng lặng và rộng lớn... Vô Cực đang đứng đợi trước ngai rồng!")
            .boss("NHIẾP CHÍNH VƯƠNG", AV_BOSS, "Ha ha ha! Lũ chuột nhắt họ Vân và thằng nghịch tử! Hôm nay máu các ngươi sẽ tế lễ đăng cơ của trẫm!")
            .boss(TIEU, AV_P1, "Vô Cực! Đêm nay là đêm tàn của bạo chúa! Hãy trả nợ cho Tiên Hoàng và Vân Gia!")
            .boss(U, AV_P2, "Phong Lôi Trảm! Ám Ảnh Tuyệt Diệt! Đền mạng đi!");

        // ============================ HAI KẾT CỤC HOÀNG TRIỀU ============================
        ENDING_P1.add(new StoryLine(NARR, "Vân Tiêu quỳ một gối, trao lại Ngọc Tỷ và Huyết Thư di chiếu cho Cửu Hoàng Tử Triệu Khang.", AV_NARRATOR, false));
        ENDING_P1.add(new StoryLine(TIEU, "Vân Gia đời đời ẩn trong bóng tối hộ quốc. Ánh sáng tân triều từ nay thuộc về Hoàng Thượng.", AV_P1, false));
        ENDING_P1.add(new StoryLine(VU, "Vân huynh! Huynh mãi mãi là Đệ Nhất Công Thần của Đại Triều!", AV_BOY, false));
        ENDING_P1.add(new StoryLine(NARR, "Oan án mười năm được rửa sạch. Vân Tiêu tiếp tục khoác áo trắng bảo vệ bình yên xã tắc trên nóc kinh thành.\n– KẾT CỤC I: ÁNH SÁNG TÂN TRIỀU –", AV_NARRATOR, false));

        ENDING_P2.add(new StoryLine(NARR, "Vân U lạnh lùng bước lên đỉnh pháo đài, vung song đao chém gãy ngai vàng nhuốm máu của Vô Cực.", AV_NARRATOR, false));
        ENDING_P2.add(new StoryLine(U, "Mười năm ngục tối dạy ta rằng: quyền lực mù quáng sẽ luôn sinh ra một bạo chúa mới.", AV_P2, false));
        ENDING_P2.add(new StoryLine(U, "Tiểu Vũ, hãy làm một minh quân. Nếu một ngày ngươi biến chất, song đao của ta sẽ tìm đến ngươi.", AV_P2, false));
        ENDING_P2.add(new StoryLine(VU, "Ta khắc cốt ghi tâm lời huynh dặn!", AV_BOY, false));
        ENDING_P2.add(new StoryLine(NARR, "Vân U quay lưng bước vào màn sương, lập ra Ám Ảnh Minh Hội trừng phạt gian thần khắp thiên hạ.\n– KẾT CỤC II: TRO TÀN VƯƠNG QUYỀN –", AV_NARRATOR, false));

        for (int i = 0; i < STAGES.length; i++) {
            if (STAGES[i] == null) throw new IllegalStateException("Thiếu dữ liệu ải " + (i + 1));
        }
    }
}
