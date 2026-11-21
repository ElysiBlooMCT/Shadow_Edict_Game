package io.qzz.elysibloomct.ctv.se;

/**
 * Định dạng số đơn giản, thay cho String.format (bản web TeaVM hỗ trợ String.format không đầy đủ).
 */
public final class TextFmt {
    private TextFmt() {}

    /** Một chữ số thập phân, dấu chấm: 2.5 -> "2.5", 3 -> "3.0", -0.04 -> "0.0". */
    public static String oneDecimal(float value) {
        long tenths = Math.round((double) value * 10.0);
        if (tenths == 0) return "0.0";
        boolean neg = tenths < 0;
        long abs = Math.abs(tenths);
        return (neg ? "-" : "") + (abs / 10) + "." + (abs % 10);
    }

    /** Tách hàng nghìn bằng dấu chấm: 1234567 -> "1.234.567". */
    public static String groupThousands(long value) {
        String digits = Long.toString(Math.abs(value));
        StringBuilder sb = new StringBuilder();
        int lead = digits.length() % 3;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - lead) % 3 == 0) sb.append('.');
            sb.append(digits.charAt(i));
        }
        return (value < 0 ? "-" : "") + sb;
    }

    private static final String VN_ACCENTED =
        "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
        + "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ";
    private static final String VN_PLAIN =
        "aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd"
        + "AAAAAAAAAAAAAAAAAEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYYD";

    /**
     * Bỏ dấu tiếng Việt ("Trùm Đại Thủ Lĩnh" -> "Trum Dai Thu Linh").
     * Dùng bảng tra thay cho java.text.Normalizer vì bản web (TeaVM) không có Normalizer.
     */
    public static String removeVietnameseAccents(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0300 && c <= 0x036F) continue; // dấu rời (tổ hợp)
            int k = VN_ACCENTED.indexOf(c);
            sb.append(k >= 0 ? VN_PLAIN.charAt(k) : c);
        }
        return sb.toString();
    }
}
