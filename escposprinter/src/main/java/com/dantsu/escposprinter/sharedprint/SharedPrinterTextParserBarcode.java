package com.dantsu.escposprinter.sharedprint;

import android.util.Log;

import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.barcode.Barcode;
import com.dantsu.escposprinter.barcode.Barcode128;
import com.dantsu.escposprinter.barcode.Barcode39;
import com.dantsu.escposprinter.barcode.BarcodeEAN13;
import com.dantsu.escposprinter.barcode.BarcodeEAN8;
import com.dantsu.escposprinter.barcode.BarcodeUPCA;
import com.dantsu.escposprinter.barcode.BarcodeUPCE;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.IPrinterTextParserElement;
import com.dantsu.escposprinter.textparser.PrinterTextParser;

import java.util.Hashtable;

public class SharedPrinterTextParserBarcode implements ISharedPrinterTextParserElement {

    private Barcode barcode;
    private int length;
    private byte[] align;

    public SharedPrinterTextParserBarcode(SharedPrinterTextColumn printerTextParserColumn,
                                          String textAlign,
                                          Hashtable<String, String> barcodeAttributes,
                                          String code) throws EscPosParserException, EscPosBarcodeException {

        SharedPrinterSize printer = printerTextParserColumn.getLine().getTextParser().getPrinter();
        code = code.trim();

        this.align = EscPosPrinterCommands.TEXT_ALIGN_LEFT;
        switch (textAlign) {
            case PrinterTextParser.TAGS_ALIGN_CENTER:
                this.align = EscPosPrinterCommands.TEXT_ALIGN_CENTER;
                break;
            case PrinterTextParser.TAGS_ALIGN_RIGHT:
                this.align = EscPosPrinterCommands.TEXT_ALIGN_RIGHT;
                break;
        }

        this.length = printer.getPrinterNbrCharactersPerLine();
        float height = 10f;

        if (barcodeAttributes.containsKey(PrinterTextParser.ATTR_BARCODE_HEIGHT)) {
            String barCodeAttribute = barcodeAttributes.get(PrinterTextParser.ATTR_BARCODE_HEIGHT);

            if (barCodeAttribute == null) {
                throw new EscPosParserException("Invalid barcode attribute: " + PrinterTextParser.ATTR_BARCODE_HEIGHT);
            }

            try {
                height = Float.parseFloat(barCodeAttribute);
            } catch (NumberFormatException nfe) {
                throw new EscPosParserException("Invalid barcode " + PrinterTextParser.ATTR_BARCODE_HEIGHT + " value");
            }
        }

        float width = 0f;
        if (barcodeAttributes.containsKey(SharedPrintTextParser.ATTR_BARCODE_WIDTH)) {
            String barCodeAttribute = barcodeAttributes.get(SharedPrintTextParser.ATTR_BARCODE_WIDTH);

            if (barCodeAttribute == null) {
                throw new EscPosParserException("Invalid barcode attribute: " + SharedPrintTextParser.ATTR_BARCODE_WIDTH);
            }

            try {
                width = Float.parseFloat(barCodeAttribute);
            } catch (NumberFormatException nfe) {
                throw new EscPosParserException("Invalid barcode " + SharedPrintTextParser.ATTR_BARCODE_WIDTH + " value");
            }
        }

        int textPosition = EscPosPrinterCommands.BARCODE_TEXT_POSITION_BELOW;
        if (barcodeAttributes.containsKey(SharedPrintTextParser.ATTR_BARCODE_TEXT_POSITION)) {
            String barCodeAttribute = barcodeAttributes.get(SharedPrintTextParser.ATTR_BARCODE_TEXT_POSITION);

            if (barCodeAttribute == null) {
                throw new EscPosParserException("Invalid barcode attribute: " + SharedPrintTextParser.ATTR_BARCODE_TEXT_POSITION);
            }

            switch (barCodeAttribute) {
                case SharedPrintTextParser.ATTR_BARCODE_TEXT_POSITION_NONE:
                    textPosition = EscPosPrinterCommands.BARCODE_TEXT_POSITION_NONE;
                    break;
                case SharedPrintTextParser.ATTR_BARCODE_TEXT_POSITION_ABOVE:
                    textPosition = EscPosPrinterCommands.BARCODE_TEXT_POSITION_ABOVE;
                    break;
            }
        }


        // FIXME: 1/23/25 barcode type error

        Log.d("Log404", "PrinterTextParserBarcode:    " + barcodeAttributes.toString());
        String barcodeType = SharedPrintTextParser.ATTR_BARCODE_TYPE_EAN13;

        if (barcodeAttributes.containsKey(SharedPrintTextParser.ATTR_BARCODE_TYPE)) {
            barcodeType = barcodeAttributes.get(SharedPrintTextParser.ATTR_BARCODE_TYPE);

            if (barcodeType == null) {
                throw new EscPosParserException("Invalid barcode attribute : " + SharedPrintTextParser.ATTR_BARCODE_TYPE);
            }
        }

        Log.d("Log404", "barcode128: -----   " + barcodeType);


        switch (barcodeType) {
            case SharedPrintTextParser.ATTR_BARCODE_TYPE_EAN8:
                this.barcode = new BarcodeEAN8(printer, code, width, height, textPosition);
                break;
            case SharedPrintTextParser.ATTR_BARCODE_TYPE_EAN13:
                this.barcode = new BarcodeEAN13(printer, code, width, height, textPosition);
                break;
            case SharedPrintTextParser.ATTR_BARCODE_TYPE_UPCA:
                this.barcode = new BarcodeUPCA(printer, code, width, height, textPosition);
                break;
            case SharedPrintTextParser.ATTR_BARCODE_TYPE_UPCE:
                this.barcode = new BarcodeUPCE(printer, code, width, height, textPosition);
                break;
            case SharedPrintTextParser.ATTR_BARCODE_TYPE_128:
                Log.d("Log404", " got barcode 128");
                this.barcode = new Barcode128(printer, code, width, height, textPosition);
                break;
            case SharedPrintTextParser.ATTR_BARCODE_TYPE_39:
                this.barcode = new Barcode39(printer, code, width, height, textPosition);
                break;
            default:
                Log.d("Log404", " invalid  barcode ");

                throw new EscPosParserException("Invalid barcode attribute : " + SharedPrintTextParser.ATTR_BARCODE_TYPE);
        }
    }

    /**
     * Get the barcode width in char length.
     *
     * @return int
     */
    @Override
    public int length() throws EscPosEncodingException {
        return this.length;
    }



    /**
     * Print barcode
     *
     * @param printerSocket Instance of EscPosPrinterCommands
     * @return this Fluent method
     */
    @Override
    public SharedPrinterTextParserBarcode print(SharedPrinterCommand printerSocket) {
        printerSocket
                .setAlign(this.align)
                .printBarcode(this.barcode);
        return this;
    }
}
