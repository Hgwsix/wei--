package com.xiaowei.weipicturebackend.util;

/**
 * 工具类：计算颜色相似度
 *
 * <p>目前采用 RGB 欧氏距离来衡量颜色差异，并将其映射到 0~1 的相似度区间。
 * 相似度越接近 1，颜色越相近。</p>
 */
public final class ColorSimilarityUtil {

    /**
     * RGB 空间中两点的最大距离：sqrt(255^2 * 3)
     */
    private static final double MAX_RGB_DISTANCE = Math.sqrt(255 * 255 * 3);

    private ColorSimilarityUtil() {
    }

    /**
     * 计算两个十六进制颜色（如 #FFFFFF）之间的相似度，范围 0~1。
     *
     * @param firstColor  第一个颜色（支持 #RRGGBB 或 RRGGBB）
     * @param secondColor 第二个颜色
     * @return 相似度（0 表示完全不同，1 表示完全相同）
     * @throws IllegalArgumentException 入参格式非法时抛出
     */
    public static double calculateSimilarity(String firstColor, String secondColor) {
        int[] firstRgb = parseHexColor(firstColor);
        int[] secondRgb = parseHexColor(secondColor);
        return calculateSimilarity(firstRgb[0], firstRgb[1], firstRgb[2],
                secondRgb[0], secondRgb[1], secondRgb[2]);
    }

    /**
     * 计算两个 RGB 颜色之间的相似度。
     *
     * @param r1 第一个颜色的 R
     * @param g1 第一个颜色的 G
     * @param b1 第一个颜色的 B
     * @param r2 第二个颜色的 R
     * @param g2 第二个颜色的 G
     * @param b2 第二个颜色的 B
     * @return 相似度（0~1）
     */
    public static double calculateSimilarity(int r1, int g1, int b1,
                                             int r2, int g2, int b2) {
        validateRgb(r1, g1, b1);
        validateRgb(r2, g2, b2);
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) +
                Math.pow(g1 - g2, 2) +
                Math.pow(b1 - b2, 2));
        double similarity = 1 - (distance / MAX_RGB_DISTANCE);
        if (similarity < 0) {
            return 0;
        }
        if (similarity > 1) {
            return 1;
        }
        return similarity;
    }

    private static int[] parseHexColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            throw new IllegalArgumentException("颜色值不能为空");
        }
        String normalized = hexColor.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        // 兼容纯数字颜色值（前端可能传 16777215 这样的十进制）
        if (normalized.matches("\\d+")) {
            long decimalValue = Long.parseLong(normalized);
            normalized = String.format("%06X", decimalValue & 0xFFFFFF);
        }
        if (normalized.length() == 3) {
            normalized = "" + normalized.charAt(0) + normalized.charAt(0)
                    + normalized.charAt(1) + normalized.charAt(1)
                    + normalized.charAt(2) + normalized.charAt(2);
        } else if (normalized.length() == 4) {
            normalized = "" + normalized.charAt(0) + normalized.charAt(0)
                    + normalized.charAt(1) + normalized.charAt(1)
                    + normalized.charAt(2) + normalized.charAt(2);
        } else if (normalized.length() == 8) {
            normalized = normalized.substring(0, 6);
        }
        if (normalized.length() != 6) {
            throw new IllegalArgumentException("仅支持 3 位或 6 位十六进制颜色值");
        }

        try {
            int r = Integer.parseInt(normalized.substring(0, 2), 16);
            int g = Integer.parseInt(normalized.substring(2, 4), 16);
            int b = Integer.parseInt(normalized.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("颜色值包含非法字符：" + hexColor, e);
        }
    }

    private static void validateRgb(int r, int g, int b) {
        if (!isChannelValid(r) || !isChannelValid(g) || !isChannelValid(b)) {
            throw new IllegalArgumentException("RGB 分量必须在 0~255 之间");
        }
    }

    private static boolean isChannelValid(int value) {
        return value >= 0 && value <= 255;
    }
}

