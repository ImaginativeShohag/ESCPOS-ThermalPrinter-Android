package com.dantsu.escposprinter.textparser;

import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.sharedprint.SharedPrinterCommand;

public interface IPrinterTextParserElement {
    int length() throws EscPosEncodingException;

    IPrinterTextParserElement print(EscPosPrinterCommands printerSocket) throws EscPosEncodingException, EscPosConnectionException;
}

