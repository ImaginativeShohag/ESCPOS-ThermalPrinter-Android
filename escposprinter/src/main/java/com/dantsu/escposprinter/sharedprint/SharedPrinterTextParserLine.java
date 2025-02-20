package com.dantsu.escposprinter.sharedprint;

import android.util.Log;

import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParser;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SharedPrinterTextParserLine {
    private SharedPrintTextParser textParser;
    private int nbrColumns;
    private int nbrCharColumn;
    private int nbrCharForgetted;
    private int nbrCharColumnExceeded;
    private SharedPrinterTextColumn[] columns;

    public SharedPrinterTextParserLine(SharedPrintTextParser textParser, String textLine) throws EscPosParserException, EscPosBarcodeException, EscPosEncodingException {
        this.textParser = textParser;
        int nbrCharactersPerLine = this.getTextParser().getPrinter().getPrinterNbrCharactersPerLine();

        Pattern pattern = Pattern.compile(PrinterTextParser.getRegexAlignTags());
        Matcher matcher = pattern.matcher(textLine);


        ArrayList<String> columnsList = new ArrayList<String>();
        int lastPosition = 0;

        while (matcher.find()) {
            int startPosition = matcher.start();
            Log.d("Log404", "Printing3:   " + matcher.start());

            if (startPosition > 0) {
                Log.d("Log404", "Printing4:   " + textLine.substring(lastPosition, startPosition));

                columnsList.add(textLine.substring(lastPosition, startPosition));
            }
            lastPosition = startPosition;

        }

        columnsList.add(textLine.substring(lastPosition));
        Log.d("Log404", "Printing5:   " + textLine.substring(lastPosition) + "  " + columnsList.size());

        this.nbrColumns = columnsList.size();
        this.nbrCharColumn = (int) Math.floor(((float) nbrCharactersPerLine) / ((float) this.nbrColumns));
        this.nbrCharForgetted = nbrCharactersPerLine - (nbrCharColumn * this.nbrColumns);
        this.nbrCharColumnExceeded = 0;
        this.columns = new SharedPrinterTextColumn[this.nbrColumns];

        int i = 0;
        for (String column : columnsList) {
            Log.d("Log404", "Printing6:   " + column);
            this.columns[i++] = new SharedPrinterTextColumn(this, column);
        }
    }


    public SharedPrintTextParser getTextParser() {
        return this.textParser;
    }

    public SharedPrinterTextColumn[] getColumns() {
        return this.columns;
    }

    public int getNbrColumns() {
        return this.nbrColumns;
    }


    public SharedPrinterTextParserLine setNbrCharColumn(int newValue) {
        this.nbrCharColumn = newValue;
        return this;
    }

    public int getNbrCharColumn() {
        return this.nbrCharColumn;
    }


    public SharedPrinterTextParserLine setNbrCharForgetted(int newValue) {
        this.nbrCharForgetted = newValue;
        return this;
    }

    public int getNbrCharForgetted() {
        return this.nbrCharForgetted;
    }


    public SharedPrinterTextParserLine setNbrCharColumnExceeded(int newValue) {
        this.nbrCharColumnExceeded = newValue;
        return this;
    }

    public int getNbrCharColumnExceeded() {
        return this.nbrCharColumnExceeded;
    }
}
