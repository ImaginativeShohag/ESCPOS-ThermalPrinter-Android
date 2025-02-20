package com.dantsu.escposprinter.sharedprint;

import android.graphics.Bitmap;
import android.util.Log;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.barcode.Barcode;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.hierynomus.smbj.share.PrinterShare;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

public class SharedPrinterCommand {

    public static final byte LF = 0x0A;

    public static final byte[] RESET_PRINTER = new byte[]{0x1B, 0x40};

    public static final byte[] TEXT_ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};
    public static final byte[] TEXT_ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01};
    public static final byte[] TEXT_ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};

    public static final byte[] TEXT_WEIGHT_NORMAL = new byte[]{0x1B, 0x45, 0x00};
    public static final byte[] TEXT_WEIGHT_BOLD = new byte[]{0x1B, 0x45, 0x01};

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

    public static final byte[] TEXT_UNDERLINE_OFF = new byte[]{0x1B, 0x2D, 0x00};
    public static final byte[] TEXT_UNDERLINE_ON = new byte[]{0x1B, 0x2D, 0x01};
    public static final byte[] TEXT_UNDERLINE_LARGE = new byte[]{0x1B, 0x2D, 0x02};

    public static final byte[] TEXT_DOUBLE_STRIKE_OFF = new byte[]{0x1B, 0x47, 0x00};
    public static final byte[] TEXT_DOUBLE_STRIKE_ON = new byte[]{0x1B, 0x47, 0x01};

    public static final byte[] TEXT_COLOR_BLACK = new byte[]{0x1B, 0x72, 0x00};
    public static final byte[] TEXT_COLOR_RED = new byte[]{0x1B, 0x72, 0x01};

    public static final byte[] TEXT_COLOR_REVERSE_OFF = new byte[]{0x1D, 0x42, 0x00};
    public static final byte[] TEXT_COLOR_REVERSE_ON = new byte[]{0x1D, 0x42, 0x01};


    public static final int BARCODE_TYPE_UPCA = 65;
    public static final int BARCODE_TYPE_UPCE = 66;
    public static final int BARCODE_TYPE_EAN13 = 67;
    public static final int BARCODE_TYPE_EAN8 = 68;
    public static final int BARCODE_TYPE_39 = 69;
    public static final int BARCODE_TYPE_ITF = 70;
    public static final int BARCODE_TYPE_128 = 73;

    public static final int BARCODE_TEXT_POSITION_NONE = 0;
    public static final int BARCODE_TEXT_POSITION_ABOVE = 1;
    public static final int BARCODE_TEXT_POSITION_BELOW = 2;

    public static final int QRCODE_1 = 49;
    public static final int QRCODE_2 = 50;

    private EscPosCharsetEncoding charsetEncoding;
    private PrinterShare printerShare;
    private boolean useEscAsteriskCommand;
    private final List<byte[]> byteArrayList;

    public static byte[] initGSv0Command(int bytesByLine, int bitmapHeight) {
        int
                xH = bytesByLine / 256,
                xL = bytesByLine - (xH * 256),
                yH = bitmapHeight / 256,
                yL = bitmapHeight - (yH * 256);

        byte[] imageBytes = new byte[8 + bytesByLine * bitmapHeight];
        imageBytes[0] = 0x1D;
        imageBytes[1] = 0x76;
        imageBytes[2] = 0x30;
        imageBytes[3] = 0x00;
        imageBytes[4] = (byte) xL;
        imageBytes[5] = (byte) xH;
        imageBytes[6] = (byte) yL;
        imageBytes[7] = (byte) yH;
        return imageBytes;
    }


    public static byte[] bitmapToBytes(Bitmap bitmap, boolean gradient) {
        int
                bitmapWidth = bitmap.getWidth(),
                bitmapHeight = bitmap.getHeight(),
                bytesByLine = (int) Math.ceil(((float) bitmapWidth) / 8f);

        byte[] imageBytes = SharedPrinterCommand.initGSv0Command(bytesByLine, bitmapHeight);

        int i = 8,
                greyscaleCoefficientInit = 0,
                gradientStep = 6;

        double
                colorLevelStep = 765.0 / (15 * gradientStep + gradientStep - 1);

        for (int posY = 0; posY < bitmapHeight; posY++) {
            int greyscaleCoefficient = greyscaleCoefficientInit,
                    greyscaleLine = posY % gradientStep;
            for (int j = 0; j < bitmapWidth; j += 8) {
                int b = 0;
                for (int k = 0; k < 8; k++) {
                    int posX = j + k;
                    if (posX < bitmapWidth) {
                        int color = bitmap.getPixel(posX, posY),
                                red = (color >> 16) & 255,
                                green = (color >> 8) & 255,
                                blue = color & 255;

                        if (
                                (gradient && (red + green + blue) < ((greyscaleCoefficient * gradientStep + greyscaleLine) * colorLevelStep)) ||
                                        (!gradient && (red < 160 || green < 160 || blue < 160))
                        ) {
                            b |= 1 << (7 - k);
                        }

                        greyscaleCoefficient += 5;
                        if (greyscaleCoefficient > 15) {
                            greyscaleCoefficient -= 16;
                        }
                    }
                }
                imageBytes[i++] = (byte) b;
            }

            greyscaleCoefficientInit += 2;
            if (greyscaleCoefficientInit > 15) {
                greyscaleCoefficientInit = 0;
            }
        }

        return imageBytes;
    }

    public static byte[][] convertGSv0ToEscAsterisk(byte[] bytes) {
        int
                xL = bytes[4] & 0xFF,
                xH = bytes[5] & 0xFF,
                yL = bytes[6] & 0xFF,
                yH = bytes[7] & 0xFF,
                bytesByLine = xH * 256 + xL,
                dotsByLine = bytesByLine * 8,
                nH = dotsByLine / 256,
                nL = dotsByLine % 256,
                imageHeight = yH * 256 + yL,
                imageLineHeightCount = (int) Math.ceil((double) imageHeight / 24.0),
                imageBytesSize = 6 + bytesByLine * 24;

        byte[][] returnedBytes = new byte[imageLineHeightCount + 2][];
        returnedBytes[0] = SharedPrinterCommand.LINE_SPACING_24;
        for (int i = 0; i < imageLineHeightCount; ++i) {
            int pxBaseRow = i * 24;
            byte[] imageBytes = new byte[imageBytesSize];
            imageBytes[0] = 0x1B;
            imageBytes[1] = 0x2A;
            imageBytes[2] = 0x21;
            imageBytes[3] = (byte) nL;
            imageBytes[4] = (byte) nH;
            for (int j = 5; j < imageBytes.length; ++j) {
                int
                        imgByte = j - 5,
                        byteRow = imgByte % 3,
                        pxColumn = imgByte / 3,
                        bitColumn = 1 << (7 - pxColumn % 8),
                        pxRow = pxBaseRow + byteRow * 8;
                for (int k = 0; k < 8; ++k) {
                    int indexBytes = bytesByLine * (pxRow + k) + pxColumn / 8 + 8;

                    if (indexBytes >= bytes.length) {
                        break;
                    }

                    boolean isBlack = (bytes[indexBytes] & bitColumn) == bitColumn;
                    if (isBlack) {
                        imageBytes[j] |= 1 << 7 - k;
                    }
                }
            }
            imageBytes[imageBytes.length - 1] = SharedPrinterCommand.LF;
            returnedBytes[i + 1] = imageBytes;
        }
        returnedBytes[returnedBytes.length - 1] = SharedPrinterCommand.LINE_SPACING_30;
        return returnedBytes;
    }


    public static byte[] QRCodeDataToBytes(String data, int size) throws EscPosBarcodeException {

        ByteMatrix byteMatrix = null;

        try {
            EnumMap<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCode code = Encoder.encode(data, ErrorCorrectionLevel.L, hints);
            byteMatrix = code.getMatrix();

        } catch (WriterException e) {
            e.printStackTrace();
            throw new EscPosBarcodeException("Unable to encode QR code");
        }

        if (byteMatrix == null) {
            return SharedPrinterCommand.initGSv0Command(0, 0);
        }

        int
                width = byteMatrix.getWidth(),
                height = byteMatrix.getHeight(),
                coefficient = Math.round((float) size / (float) width),
                imageWidth = width * coefficient,
                imageHeight = height * coefficient,
                bytesByLine = (int) Math.ceil(((float) imageWidth) / 8f),
                i = 8;

        if (coefficient < 1) {
            return SharedPrinterCommand.initGSv0Command(0, 0);
        }

        byte[] imageBytes = SharedPrinterCommand.initGSv0Command(bytesByLine, imageHeight);

        for (int y = 0; y < height; y++) {
            byte[] lineBytes = new byte[bytesByLine];
            int x = -1, multipleX = coefficient;
            boolean isBlack = false;
            for (int j = 0; j < bytesByLine; j++) {
                int b = 0;
                for (int k = 0; k < 8; k++) {
                    if (multipleX == coefficient) {
                        isBlack = ++x < width && byteMatrix.get(x, y) == 1;
                        multipleX = 0;
                    }
                    if (isBlack) {
                        b |= 1 << (7 - k);
                    }
                    ++multipleX;
                }
                lineBytes[j] = (byte) b;
            }

            for (int multipleY = 0; multipleY < coefficient; ++multipleY) {
                System.arraycopy(lineBytes, 0, imageBytes, i, lineBytes.length);
                i += lineBytes.length;
            }
        }

        return imageBytes;
    }


    public SharedPrinterCommand(EscPosCharsetEncoding charsetEncoding, PrinterShare printerShare) {

        this.charsetEncoding = charsetEncoding != null ? charsetEncoding : new EscPosCharsetEncoding("windows-1252", 6);
        byteArrayList = new ArrayList<>();
        this.printerShare = printerShare;
    }


    public SharedPrinterCommand connect() throws EscPosConnectionException {
        //  this.printerConnection.connect();
        return this;
    }


    public SharedPrinterCommand setAlign(byte[] align) {
       /* if (!this.printerConnection.isConnected()) {
            return this;
        }*/
        //  this.printerConnection.write(align);
        byteArrayList.add(align);
        return this;
    }


    private byte[] currentTextSize = new byte[0];
    private byte[] currentTextColor = new byte[0];
    private byte[] currentTextReverseColor = new byte[0];
    private byte[] currentTextBold = new byte[0];
    private byte[] currentTextUnderline = new byte[0];
    private byte[] currentTextDoubleStrike = new byte[0];


    public SharedPrinterCommand printText(String text, byte[] textSize, byte[] textColor, byte[] textReverseColor, byte[] textBold, byte[] textUnderline, byte[] textDoubleStrike) throws EscPosEncodingException {


        if (textSize == null) {
            textSize = SharedPrinterCommand.TEXT_SIZE_NORMAL;
        }
        if (textColor == null) {
            textColor = SharedPrinterCommand.TEXT_COLOR_BLACK;
        }
        if (textReverseColor == null) {
            textReverseColor = SharedPrinterCommand.TEXT_COLOR_REVERSE_OFF;
        }
        if (textBold == null) {
            textBold = SharedPrinterCommand.TEXT_WEIGHT_NORMAL;
        }
        if (textUnderline == null) {
            textUnderline = SharedPrinterCommand.TEXT_UNDERLINE_OFF;
        }
        if (textDoubleStrike == null) {
            textDoubleStrike = SharedPrinterCommand.TEXT_DOUBLE_STRIKE_OFF;
        }

        try {
            byte[] textBytes = text.getBytes(this.charsetEncoding.getName());
            //   this.printerConnection.write(this.charsetEncoding.getCommand());
            //this.printerConnection.write(EscPosPrinterCommands.TEXT_FONT_A);


            if (!Arrays.equals(this.currentTextSize, textSize)) {
                // this.printerConnection.write(textSize);
                this.currentTextSize = textSize;
                //  byteArrayList.add(textSize);

            }

            if (!Arrays.equals(this.currentTextDoubleStrike, textDoubleStrike)) {
                //      this.printerConnection.write(textDoubleStrike);
                this.currentTextDoubleStrike = textDoubleStrike;
                //   byteArrayList.add(textDoubleStrike);
            }

            if (!Arrays.equals(this.currentTextUnderline, textUnderline)) {
                //    this.printerConnection.write(textUnderline);
                this.currentTextUnderline = textUnderline;
                //  byteArrayList.add(textUnderline);

            }

            if (!Arrays.equals(this.currentTextBold, textBold)) {
                //  this.printerConnection.write(textBold);
                this.currentTextBold = textBold;
                // byteArrayList.add(textBold);
            }

            if (!Arrays.equals(this.currentTextColor, textColor)) {
                //    this.printerConnection.write(textColor);
                this.currentTextColor = textColor;
                // byteArrayList.add(textColor);
            }

            if (!Arrays.equals(this.currentTextReverseColor, textReverseColor)) {
                //     this.printerConnection.write(textReverseColor);
                byteArrayList.add(textReverseColor);
                this.currentTextReverseColor = textReverseColor;
            }
            // Log.d("Log404", "byte :  " + bytesToHex(textBytes));

            //    this.printerConnection.write(textBytes);

          /*  ByteArrayInputStream data = new ByteArrayInputStream(textBytes);
            InputStream[] streams = {data};
            SequenceInputStream fullStream = new SequenceInputStream(Collections.enumeration(Arrays.asList(streams)));
            printerShare.print(fullStream);*/
            byteArrayList.add(textBytes);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new EscPosEncodingException(e.getMessage());
        }


        return this;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexString.append(String.format("0x%02X, ", b)); // Converts each byte to a two-character hex
        }
        return hexString.toString();
    }


    public SharedPrinterCommand printCharsetEncodingCharacters(int charsetId) {
       /* if (!this.printerConnection.isConnected()) {
            return this;
        }*/
/*
        try {
            this.printerConnection.write(new byte[]{0x1B, 0x74, (byte) charsetId});
            this.printerConnection.write(EscPosPrinterCommands.TEXT_SIZE_NORMAL);
            this.printerConnection.write(EscPosPrinterCommands.TEXT_COLOR_BLACK);
            this.printerConnection.write(EscPosPrinterCommands.TEXT_COLOR_REVERSE_OFF);
            this.printerConnection.write(EscPosPrinterCommands.TEXT_WEIGHT_NORMAL);
            this.printerConnection.write(EscPosPrinterCommands.TEXT_UNDERLINE_OFF);
            this.printerConnection.write(EscPosPrinterCommands.TEXT_DOUBLE_STRIKE_OFF);
            this.printerConnection.write((":::: Charset n°" + charsetId + " : ").getBytes());
            this.printerConnection.write(new byte[]{
                    (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x0D, (byte) 0x0E, (byte) 0x0F,
                    (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1A, (byte) 0x1B, (byte) 0x1C, (byte) 0x1D, (byte) 0x1E, (byte) 0x1F,
                    (byte) 0x20, (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24, (byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2A, (byte) 0x2B, (byte) 0x2C, (byte) 0x2D, (byte) 0x2E, (byte) 0x2F,
                    (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x3A, (byte) 0x3B, (byte) 0x3C, (byte) 0x3D, (byte) 0x3E, (byte) 0x3F,
                    (byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45, (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x4A, (byte) 0x4B, (byte) 0x4C, (byte) 0x4D, (byte) 0x4E, (byte) 0x4F,
                    (byte) 0x50, (byte) 0x51, (byte) 0x52, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57, (byte) 0x58, (byte) 0x59, (byte) 0x5A, (byte) 0x5B, (byte) 0x5C, (byte) 0x5D, (byte) 0x5E, (byte) 0x5F,
                    (byte) 0x60, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F,
                    (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A, (byte) 0x7B, (byte) 0x7C, (byte) 0x7D, (byte) 0x7E, (byte) 0x7F,
                    (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x8B, (byte) 0x8C, (byte) 0x8D, (byte) 0x8E, (byte) 0x8F,
                    (byte) 0x90, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0x9B, (byte) 0x9C, (byte) 0x9D, (byte) 0x9E, (byte) 0x9F,
                    (byte) 0xA0, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5, (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0xAA, (byte) 0xAB, (byte) 0xAC, (byte) 0xAD, (byte) 0xAE, (byte) 0xAF,
                    (byte) 0xB0, (byte) 0xB1, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5, (byte) 0xB6, (byte) 0xB7, (byte) 0xB8, (byte) 0xB9, (byte) 0xBA, (byte) 0xBB, (byte) 0xBC, (byte) 0xBD, (byte) 0xBE, (byte) 0xBF,
                    (byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC, (byte) 0xCD, (byte) 0xCE, (byte) 0xCF,
                    (byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xDB, (byte) 0xDC, (byte) 0xDD, (byte) 0xDE, (byte) 0xDF,
                    (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF,
                    (byte) 0xF0, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF
            });
            this.printerConnection.write(new byte[]{EscPosPrinterCommands.LF, EscPosPrinterCommands.LF, EscPosPrinterCommands.LF, EscPosPrinterCommands.LF});
            this.printerConnection.send();
        } catch (EscPosConnectionException e) {
            e.printStackTrace();
        }*/
        return this;
    }


    public SharedPrinterCommand useEscAsteriskCommand(boolean enable) {
        this.useEscAsteriskCommand = enable;
        return this;
    }

    public SharedPrinterCommand printImage(byte[] image) throws EscPosConnectionException {

        // this.useEscAsteriskCommand = false;

        byte[][] bytesToPrint = this.useEscAsteriskCommand ? SharedPrinterCommand.convertGSv0ToEscAsterisk(image) : new byte[][]{image};
        // byteArrayList.add( new byte[]{0x0A});
        for (byte[] bytes : bytesToPrint) {
            // this.printerConnection.write(bytes);
            //

            byteArrayList.add(bytes);
            // this.printerConnection.send();
            //  printerShare.print(new ByteArrayInputStream(bytes));
        }

        return this;
    }


    public SharedPrinterCommand printBarcode(Barcode barcode) {

        String code = barcode.getCode();
        int barcodeLength = barcode.getCodeLength();
        byte[] barcodeCommand = new byte[barcodeLength + 6];
        //  0x7B, 0x42,
        Log.d("Log04", " got the barcode ");
        if (barcode.getBarcodeType() == SharedPrinterCommand.BARCODE_TYPE_128) {
            Log.d("Log04", " got the barcode 128");
            barcodeCommand = new byte[barcodeLength + 6];
            System.arraycopy(new byte[]{0x1D, 0x6B, (byte) barcode.getBarcodeType(), (byte) (barcodeLength + 2), 0x7B, 0x42}, 0, barcodeCommand, 0, 6);
            for (int i = 0; i < barcodeLength; i++) {
                barcodeCommand[i + 6] = (byte) code.charAt(i);
            }

        } else {
            barcodeCommand = new byte[barcodeLength + 4];
            System.arraycopy(new byte[]{0x1D, 0x6B, (byte) barcode.getBarcodeType(), (byte) barcodeLength}, 0, barcodeCommand, 0, 4);
            for (int i = 0; i < barcodeLength; i++) {
                barcodeCommand[i + 4] = (byte) code.charAt(i);
            }
        }


       /* this.printerConnection.write(new byte[]{0x1D, 0x48, (byte) barcode.getTextPosition()});
        this.printerConnection.write(new byte[]{0x1D, 0x77, (byte) barcode.getColWidth()});
        this.printerConnection.write(new byte[]{0x1D, 0x68, (byte) barcode.getHeight()});*/
        //  this.printerConnection.write(barcodeCommand);
/*        byteArrayList.add(new byte[]{0x1D, 0x48, (byte) barcode.getTextPosition()});
        byteArrayList.add(new byte[]{0x1D, 0x77, (byte) barcode.getColWidth()});
        byteArrayList.add(new byte[]{0x1D, 0x68, (byte) barcode.getHeight()});*/

        byteArrayList.add(new byte[]{0x0A});
        byteArrayList.add(barcodeCommand);


      /*  printerShare.print(new ByteArrayInputStream(new byte[]{0x1D, 0x48, (byte) barcode.getTextPosition()}));
        printerShare.print(new ByteArrayInputStream(new byte[]{0x1D, 0x77, (byte) barcode.getColWidth()}));
        printerShare.print(new ByteArrayInputStream(new byte[]{0x1D, 0x68, (byte) barcode.getHeight()}));
        printerShare.print(new ByteArrayInputStream(barcodeCommand));*/

        return this;
    }

    public SharedPrinterCommand reset() {
        //   printerShare.print(new ByteArrayInputStream(new byte[]{0x1B, 0x40}));
        return this;
    }

    public SharedPrinterCommand newLine() throws EscPosConnectionException {
        return this.newLine(null);
    }

    public SharedPrinterCommand newLine(byte[] align) throws EscPosConnectionException {
       /* if (!this.printerConnection.isConnected()) {
            return this;
        }*/

        //  this.printerConnection.write(new byte[]{EscPosPrinterCommands.LF});
        //  this.printerConnection.send();
        byteArrayList.add(new byte[]{SharedPrinterCommand.LF});

        if (align != null) {
            //this.printerConnection.write(align);
            byteArrayList.add(align);
        }

        return this;
    }


    public SharedPrinterCommand feedPaper(int dots) throws EscPosConnectionException {


        if (dots > 0) {
            //this.printerConnection.write(new byte[]{0x1B, 0x4A, (byte) dots});
            byteArrayList.add(new byte[]{0x1B, 0x4A, (byte) dots});
        }

        return this;
    }


    public SharedPrinterCommand cutPaper() throws EscPosConnectionException {
       /* if (!this.printerConnection.isConnected()) {
            return this;
        }*/

        // this.printerConnection.write(new byte[]{0x1D, 0x56, 0x01});
        //    this.printerConnection.send(100);

        // FIXME: 2/19/25 add cut paper
        // new byte[]{0x1B, 0x4A, (byte) dots}
        return this;
    }


    public SharedPrinterCommand openCashBox() throws EscPosConnectionException {
       /* if (!this.printerConnection.isConnected()) {
            return this;
        }*/

        //  this.printerConnection.write(new byte[]{0x1B, 0x70, 0x00, 0x3C, (byte) 0xFF});
        //  this.printerConnection.send(100);
        return this;
    }


    public EscPosCharsetEncoding getCharsetEncoding() {
        return this.charsetEncoding;
    }

    public List<byte[]> getByteArrayList() {
        return byteArrayList;
    }
}

