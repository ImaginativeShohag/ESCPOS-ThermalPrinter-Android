package com.dantsu.escposprinter.sharedprint;

import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParser;

import java.util.Hashtable;

public class SharedPrinterTextParserQRCode extends SharedPrinterTextParserImg {

    private static byte[] initConstructor(SharedPrinterTextColumn printerTextParserColumn,
                                          Hashtable<String, String> qrCodeAttributes, String data) throws EscPosParserException, EscPosBarcodeException {
        EscPosPrinterSize printer = printerTextParserColumn.getLine().getTextParser().getPrinter();
        data = data.trim();

        int size = printer.mmToPx(20f);

        if (qrCodeAttributes.containsKey(PrinterTextParser.ATTR_QRCODE_SIZE)) {
            String qrCodeAttribute = qrCodeAttributes.get(PrinterTextParser.ATTR_QRCODE_SIZE);
            if (qrCodeAttribute == null) {
                throw new EscPosParserException("Invalid QR code attribute : " + PrinterTextParser.ATTR_QRCODE_SIZE);
            }
            try {
                size = printer.mmToPx(Float.parseFloat(qrCodeAttribute));
            } catch (NumberFormatException nfe) {
                throw new EscPosParserException("Invalid QR code " + PrinterTextParser.ATTR_QRCODE_SIZE + " value");
            }
        }

        return SharedPrinterCommand.QRCodeDataToBytes(data, size);
    }

    public SharedPrinterTextParserQRCode(SharedPrinterTextColumn printerTextParserColumn, String textAlign,
                                         Hashtable<String, String> qrCodeAttributes, String data) throws EscPosParserException, EscPosBarcodeException {
        super(
                printerTextParserColumn,
                textAlign,
                SharedPrinterTextParserQRCode.initConstructor(printerTextParserColumn, qrCodeAttributes, data)
        );
    }
}
