package com.dantsu.escposprinter.sharedprint;

import android.graphics.Bitmap;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.EscPosPrinterSize;

public class SharedPrinterSize extends EscPosPrinterSize{
    public static final float INCH_TO_MM = 25.4f;

    private int printerDpi = 203;
    private float printerWidthMM = 88f;
    private int printerNbrCharactersPerLine = 64;
    private int printerWidthPx;
    private int printerCharSizeWidthPx;
    private EscPosCharsetEncoding charsetEncoding;

    public SharedPrinterSize(int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine, EscPosCharsetEncoding charsetEncoding) {
        super(printerDpi, printerWidthMM, printerNbrCharactersPerLine);
        this.printerDpi = printerDpi;
        this.printerWidthMM = printerWidthMM;
        this.printerNbrCharactersPerLine = printerNbrCharactersPerLine;
        int printingWidthPx = this.mmToPx(this.printerWidthMM);
        this.printerWidthPx = printingWidthPx + (printingWidthPx % 8);
        this.printerCharSizeWidthPx = printingWidthPx / this.printerNbrCharactersPerLine;
        this.charsetEncoding = charsetEncoding;

    }

    /**
     * Get the maximum number of characters that can be printed on a line.
     *
     * @return int
     */
    public int getPrinterNbrCharactersPerLine() {
        return this.printerNbrCharactersPerLine;
    }

    /**
     * Get the printing width in millimeters
     *
     * @return float
     */
    public float getPrinterWidthMM() {
        return this.printerWidthMM;
    }

    /**
     * Get the printer DPI
     *
     * @return int
     */
    public int getPrinterDpi() {
        return this.printerDpi;
    }

    /**
     * Get the printing width in dot
     *
     * @return int
     */
    public int getPrinterWidthPx() {
        return this.printerWidthPx;
    }

    /**
     * Get the number of dot that a printed character contain
     *
     * @return int
     */
    public int getPrinterCharSizeWidthPx() {
        return this.printerCharSizeWidthPx;
    }

    /**
     * Convert from millimeters to dot the mmSize variable.
     *
     * @param mmSize Distance in millimeters to be converted
     * @return int
     */
    public int mmToPx(float mmSize) {
        return Math.round(mmSize * ((float) this.printerDpi) / EscPosPrinterSize.INCH_TO_MM);
    }

    public EscPosCharsetEncoding getEncoding() {
        return this.charsetEncoding;
    }


    /**
     * Convert Bitmap object to ESC/POS image.
     *
     * @param bitmap   Instance of Bitmap
     * @param gradient false : Black and white image, true : Grayscale image
     * @return Bytes contain the image in ESC/POS command
     */
    public byte[] bitmapToBytes(Bitmap bitmap, boolean gradient) {
        boolean isSizeEdit = false;
        int bitmapWidth = bitmap.getWidth(),
                bitmapHeight = bitmap.getHeight(),
                maxWidth = this.printerWidthPx,
                maxHeight = 256;

        if (bitmapWidth > maxWidth) {
            bitmapHeight = Math.round(((float) bitmapHeight) * ((float) maxWidth) / ((float) bitmapWidth));
            bitmapWidth = maxWidth;
            isSizeEdit = true;
        }
        if (bitmapHeight > maxHeight) {
            bitmapWidth = Math.round(((float) bitmapWidth) * ((float) maxHeight) / ((float) bitmapHeight));
            bitmapHeight = maxHeight;
            isSizeEdit = true;
        }

        if (isSizeEdit) {
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, true);
        }

        return SharedPrinterCommand.bitmapToBytes(bitmap, gradient);
    }
}
