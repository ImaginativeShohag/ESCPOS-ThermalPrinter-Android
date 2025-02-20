package com.dantsu.thermalprinter;

import static android.widget.RelativeLayout.ALIGN_LEFT;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SMBParser {
    public static final byte LF = 0x0A;

    public static final byte[] RESET_PRINTER = new byte[]{0x1B, 0x40};

    public static final byte[] TEXT_ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};
    public static final byte[] TEXT_ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01};
    public static final byte[] TEXT_ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};

    public static final byte[] TEXT_WEIGHT_NORMAL = new byte[]{0x1B, 0x45, 0x00};
    public static final byte[] TEXT_WEIGHT_BOLD = new byte[]{0x1B, 0x45, 0x01};
    private static final byte[] NEW_LINE = {0x0A};


    public static final byte[] LINE_SPACING_24 = {0x1b, 0x33, 0x18};
    public static final byte[] LINE_SPACING_30 = {0x1b, 0x33, 0x1e};

    public static final byte[] TEXT_FONT_A = new byte[]{0x1B, 0x4D, 0x00};
    public static final byte[] TEXT_FONT_B = new byte[]{0x1B, 0x4D, 0x01};
    public static final byte[] TEXT_FONT_C = new byte[]{0x1B, 0x4D, 0x02};
    public static final byte[] TEXT_FONT_D = new byte[]{0x1B, 0x4D, 0x03};
    public static final byte[] TEXT_FONT_E = new byte[]{0x1B, 0x4D, 0x04};

    public static final byte[] TEXT_SIZE_NORMAL = new byte[]{0x1b, 0x21, 0x01};
    public static final byte[] TEXT_SIZE_DOUBLE_HEIGHT = new byte[]{0x1D, 0x21, 0x01};
    public static final byte[] TEXT_SIZE_DOUBLE_WIDTH = new byte[]{0x1D, 0x21, 0x10};
    public static final byte[] TEXT_SIZE_BIG = new byte[]{0x1D, 0x21, 0x11};
    public static final byte[] TEXT_SIZE_BIG_2 = new byte[]{0x1D, 0x21, 0x22};
    public static final byte[] TEXT_SIZE_BIG_3 = new byte[]{0x1D, 0x21, 0x33};
    public static final byte[] TEXT_SIZE_BIG_4 = new byte[]{0x1D, 0x21, 0x44};
    public static final byte[] TEXT_SIZE_BIG_5 = new byte[]{0x1D, 0x21, 0x55};
    public static final byte[] TEXT_SIZE_BIG_6 = new byte[]{0x1D, 0x21, 0x66};

    public static byte[] parseText(String text) {
        List<Byte> byteList = new ArrayList<>();

        String[] lines = text.split("\\n");
        for (String line : lines) {
            while (!line.isEmpty()) {
                if (line.startsWith("[L]")) {
                    addBytes(byteList, TEXT_ALIGN_LEFT);
                    line = line.substring(3); // Remove "[L]"
                } else if (line.startsWith("[C]")) {
                    addBytes(byteList, TEXT_ALIGN_CENTER);
                    line = line.substring(3); // Remove "[C]"
                } else if (line.startsWith("[R]")) {
                    addBytes(byteList, TEXT_ALIGN_RIGHT);
                    line = line.substring(3); // Remove "[R]"
                } else if (line.startsWith("<b>")) {
                    addBytes(byteList, TEXT_WEIGHT_BOLD);
                    line = line.substring(3); // Remove "<b>"
                } else if (line.startsWith("</b>")) {
                    addBytes(byteList, TEXT_WEIGHT_NORMAL);
                    line = line.substring(4); // Remove "</b>"
                } else {
                    // Find the next tag (if any)
                    int nextTagIndex = findNextTagIndex(line);
                    if (nextTagIndex == -1) {
                        addBytes(byteList, line.getBytes(StandardCharsets.ISO_8859_1));
                        break;
                    } else {
                        String segment = line.substring(0, nextTagIndex);
                        addBytes(byteList, segment.getBytes(StandardCharsets.ISO_8859_1));
                        line = line.substring(nextTagIndex); // Continue processing
                    }
                }
            }
            addBytes(byteList, NEW_LINE);
        }

        return toByteArray(byteList);
    }

    private static void addBytes(List<Byte> byteList, byte[] bytes) {
        for (byte b : bytes) {
            byteList.add(b);
        }
    }

    private static int findNextTagIndex(String line) {
        int lIndex = line.indexOf("[L]");
        int cIndex = line.indexOf("[C]");
        int rIndex = line.indexOf("[R]");
        int bOpenIndex = line.indexOf("<b>");
        int bCloseIndex = line.indexOf("</b>");

        int minIndex = Integer.MAX_VALUE;
        if (lIndex != -1) minIndex = Math.min(minIndex, lIndex);
        if (cIndex != -1) minIndex = Math.min(minIndex, cIndex);
        if (rIndex != -1) minIndex = Math.min(minIndex, rIndex);
        if (bOpenIndex != -1) minIndex = Math.min(minIndex, bOpenIndex);
        if (bCloseIndex != -1) minIndex = Math.min(minIndex, bCloseIndex);

        return (minIndex == Integer.MAX_VALUE) ? -1 : minIndex;
    }

    private static byte[] toByteArray(List<Byte> byteList) {
        byte[] array = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            array[i] = byteList.get(i);
        }
        return array;
    }
}
