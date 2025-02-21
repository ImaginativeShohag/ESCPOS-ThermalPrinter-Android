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

import java.io.ByteArrayInputStream;
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

    public static final byte[] LINE_SPACING_24 = {0x1b, 0x33, 0x00};
    public static final byte[] LINE_SPACING_30 = {0x1b, 0x33, 0x01};

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
        return this;
    }


    public SharedPrinterCommand setAlign(byte[] align) {
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

            if (!Arrays.equals(this.currentTextSize, textSize)) {
                byteArrayList.add(textSize);
                this.currentTextSize = textSize;


            }

            if (!Arrays.equals(this.currentTextDoubleStrike, textDoubleStrike)) {
                // byteArrayList.add(textDoubleStrike);
                this.currentTextDoubleStrike = textDoubleStrike;

            }

            if (!Arrays.equals(this.currentTextUnderline, textUnderline)) {
                //   byteArrayList.add(textUnderline);
                this.currentTextUnderline = textUnderline;


            }

            if (!Arrays.equals(this.currentTextBold, textBold)) {
                byteArrayList.add(textBold);
                this.currentTextBold = textBold;

            }

            if (!Arrays.equals(this.currentTextColor, textColor)) {
                byteArrayList.add(textColor);
                this.currentTextColor = textColor;

            }

            if (!Arrays.equals(this.currentTextReverseColor, textReverseColor)) {
                //  byteArrayList.add(textReverseColor);
                this.currentTextReverseColor = textReverseColor;
            }
            byteArrayList.add(textBytes);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new EscPosEncodingException(e.getMessage());
        }
        return this;
    }


    public SharedPrinterCommand useEscAsteriskCommand(boolean enable) {
        this.useEscAsteriskCommand = enable;
        return this;
    }

    public SharedPrinterCommand printImage(byte[] image) throws EscPosConnectionException {
        byte[][] bytesToPrint = this.useEscAsteriskCommand ? SharedPrinterCommand.convertGSv0ToEscAsterisk(image) : new byte[][]{image};

        byteArrayList.add(new byte[]{0x0A});
        byteArrayList.add(  new byte[]{0x1B, 0x33, 0x00});
        for (byte[] bytes : bytesToPrint) {
            byteArrayList.add(bytes);
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


        byteArrayList.add(new byte[]{0x0A});
        byteArrayList.add(new byte[]{0x1D, 0x48, (byte) barcode.getTextPosition()});
        byteArrayList.add(new byte[]{0x1D, 0x77, (byte) barcode.getColWidth()});
        byteArrayList.add(new byte[]{0x1D, 0x68, (byte) barcode.getHeight()});
        byteArrayList.add(barcodeCommand);


      /*  printerShare.print(new ByteArrayInputStream(new byte[]{0x1D, 0x48, (byte) barcode.getTextPosition()}));
        printerShare.print(new ByteArrayInputStream(new byte[]{0x1D, 0x77, (byte) barcode.getColWidth()}));
        printerShare.print(new ByteArrayInputStream(new byte[]{0x1D, 0x68, (byte) barcode.getHeight()}));
        printerShare.print(new ByteArrayInputStream(barcodeCommand));*/

        return this;
    }

    public SharedPrinterCommand reset() {
        printerShare.print(new ByteArrayInputStream(new byte[]{0x1B, 0x40}));
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

    public List<byte[]> getByteArrayList() {
        return byteArrayList;
    }
}

