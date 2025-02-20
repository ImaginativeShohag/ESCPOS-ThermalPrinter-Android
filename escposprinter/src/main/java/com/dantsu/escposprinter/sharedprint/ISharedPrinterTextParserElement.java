package com.dantsu.escposprinter.sharedprint;

import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;

public interface ISharedPrinterTextParserElement {
    int length() throws EscPosEncodingException;
    ISharedPrinterTextParserElement print(SharedPrinterCommand printerSocket) throws EscPosEncodingException, EscPosConnectionException;
}
