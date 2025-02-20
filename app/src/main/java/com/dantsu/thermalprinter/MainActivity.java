package com.dantsu.thermalprinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.sharedprint.ISharedPrinterTextParserElement;
import com.dantsu.escposprinter.sharedprint.SharedPrintTextParser;
import com.dantsu.escposprinter.sharedprint.SharedPrinterCommand;
import com.dantsu.escposprinter.sharedprint.SharedPrinterSize;
import com.dantsu.escposprinter.sharedprint.SharedPrinterTextColumn;
import com.dantsu.escposprinter.sharedprint.SharedPrinterTextParserImg;
import com.dantsu.escposprinter.sharedprint.SharedPrinterTextParserLine;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.dantsu.thermalprinter.async.AsyncBluetoothEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter;
import com.dantsu.thermalprinter.async.AsyncTcpEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncUsbEscPosPrint;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.PrinterShare;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // Change format if needed
        return stream.toByteArray();
    }

    // Method to convert List<Byte> to byte[]
    public static byte[] toPrimitive(List<byte[]> byteArrays) {
        int totalSize = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            totalSize = byteArrays.stream().mapToInt(arr -> arr.length).sum();
        }

        // Create a new byte array with total size
        byte[] result = new byte[totalSize];

        // Copy all byte[] elements into the result array
        int index = 0;
        for (byte[] array : byteArrays) {
            System.arraycopy(array, 0, result, index, array.length);
            index += array.length;
        }

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView img = (ImageView) this.findViewById(R.id.img);
        Button button = (Button) this.findViewById(R.id.button_bluetooth_browse);
        button.setOnClickListener(view -> browseBluetoothDevice());
        button = (Button) findViewById(R.id.button_bluetooth);
        button.setOnClickListener(view -> printBluetooth());
        button = (Button) this.findViewById(R.id.button_usb);
        button.setOnClickListener(view -> {
            img.setImageBitmap(createBitmapFromText("ক্যাশ রিফান্ড প্রযোজ্য নয়,প্রযোজ্য ক্ষেত্রে ফেরত", Typeface.DEFAULT));
            printUsb();


        });


        button = (Button) this.findViewById(R.id.button_tcp);
        button.setOnClickListener(view -> printTcp());


        Button shared = (Button) this.findViewById(R.id.button_shared);


        shared.setOnClickListener(view -> {

            Thread yetAnotherThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    SMBClient client = new SMBClient();
                    try (Connection connection = client.connect("192.168.15.52")) {

                        AuthenticationContext ac = new AuthenticationContext("Softzino", "223355".toCharArray(), "WORKGROUP");
                        Session session = connection.authenticate(ac);

                        try (PrinterShare printer = (PrinterShare) session.connectShare("RONGTA")) {
                            printShared(printer);
                            byte[] LF = new byte[]{0x0A};  // Feed 5 lines
                            byte[] FEED_PAPER = new byte[]{0x1B, 0x64, 0x05};  // Feed 5 lines
                            byte[] CUT_PAPER = new byte[]{0x1D, 0x56, 0x00};   // Full cut

                            byte[] setCharSizeNormal = new byte[]{0x1b, 0x21, 0x01};
                            byte[] setPrintDensity = new byte[]{0x1D, 0x7C, 0x03};


                            List<ByteArrayInputStream> finalStream = new ArrayList<ByteArrayInputStream>();
                            // byte[] messageByteArray = toPrimitive(message);


                           /* ByteArrayInputStream textStream = new ByteArrayInputStream(messageByteArray);
                            ByteArrayInputStream feedStream = new ByteArrayInputStream(FEED_PAPER);
                            ByteArrayInputStream cutStream = new ByteArrayInputStream(CUT_PAPER);
                            ByteArrayInputStream lfStream = new ByteArrayInputStream(LF);
                            ByteArrayInputStream fontsize = new ByteArrayInputStream(setCharSizeNormal);
                            ByteArrayInputStream density = new ByteArrayInputStream(setPrintDensity);

                            // Merge all streams
                            //   SequenceInputStream fullStream = new SequenceInputStream(new SequenceInputStream(new SequenceInputStream(lfStream, textStream), feedStream), cutStream);
                            InputStream[] streams = {fontsize, density, lfStream, textStream, feedStream, cutStream};
                            SequenceInputStream fullStream = new SequenceInputStream(Collections.enumeration(Arrays.asList(streams)));

                            // Send the print job
                            printer.print(fullStream);


                            printer.close();*/
                            Log.d("Log404", "run:printing...... ");
                        } catch (EscPosEncodingException e) {
                            throw new RuntimeException(e);
                        } catch (EscPosBarcodeException e) {
                            throw new RuntimeException(e);
                        } catch (EscPosParserException e) {
                            throw new RuntimeException(e);
                        } catch (EscPosConnectionException e) {
                            throw new RuntimeException(e);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            });
            yetAnotherThread.start();
        });


    }


    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case MainActivity.PERMISSION_BLUETOOTH:
                case MainActivity.PERMISSION_BLUETOOTH_ADMIN:
                case MainActivity.PERMISSION_BLUETOOTH_CONNECT:
                case MainActivity.PERMISSION_BLUETOOTH_SCAN:
                    this.checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
                    break;
            }
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted onBluetoothPermissionsGranted) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MainActivity.PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MainActivity.PERMISSION_BLUETOOTH_SCAN);
        } else {
            this.onBluetoothPermissionsGranted.onPermissionsGranted();
        }
    }

    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice() {
        this.checkBluetoothPermissions(() -> {
            final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

            if (bluetoothDevicesList != null) {
                final String[] items = new String[bluetoothDevicesList.length + 1];
                items[0] = "Default printer";
                int i = 0;
                for (BluetoothConnection device : bluetoothDevicesList) {
                    items[++i] = device.getDevice().getName();
                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Bluetooth printer selection");
                alertDialog.setItems(
                        items,
                        (dialogInterface, i1) -> {
                            int index = i1 - 1;
                            if (index == -1) {
                                selectedDevice = null;
                            } else {
                                selectedDevice = bluetoothDevicesList[index];
                            }
                            Button button = (Button) findViewById(R.id.button_bluetooth_browse);
                            button.setText(items[i1]);
                        }
                );

                AlertDialog alert = alertDialog.create();
                alert.setCanceledOnTouchOutside(false);
                alert.show();
            }
        });

    }

    public void printBluetooth() {
        this.checkBluetoothPermissions(() -> {
            try {
                new AsyncBluetoothEscPosPrint(
                        this,
                        new AsyncEscPosPrint.OnPrintFinished() {
                            @Override
                            public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                                Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                            }

                            @Override
                            public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                                Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                            }
                        }
                )
                        .execute(this.getAsyncEscPosPrinter(selectedDevice));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }

    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {


        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    //mVendorId=4070,mProductId=33054
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    usbManager.getDeviceList();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        usbManager.getDeviceList().forEach((key, value) -> {
                            Log.d("log404", "   key--- " + key.toString() + "    ");

                        });
                    }
                    String key = "";
                    for (String value : usbManager.getDeviceList().keySet()) {
                        key = value;
                        break;
                    }
                    UsbDevice usbDevice = usbManager.getDeviceList().get(key);


                    if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        if (usbManager != null && usbDevice != null) {
                            //  DirectPrint(new UsbConnection(usbManager, usbDevice));

                            try {
                                new AsyncUsbEscPosPrint(
                                        context,
                                        new AsyncEscPosPrint.OnPrintFinished() {
                                            @Override
                                            public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                                                Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !" + codeException);
                                            }

                                            @Override
                                            public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                                                Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                                            }
                                        }
                                )
                                        .execute(getAsyncEscPosPrinter(new UsbConnection(usbManager, usbDevice)));
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

    };

    public void printUsb() {
        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        if (usbConnection == null || usbManager == null) {
            new AlertDialog.Builder(this)
                    .setTitle("USB Connection")
                    .setMessage("No USB printer found.")
                    .show();
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(MainActivity.ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
        );


        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
        registerReceiver(this.usbReceiver, filter, Context.RECEIVER_EXPORTED);

        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }

    /*==============================================================================================
    =========================================TCP PART===============================================
    ==============================================================================================*/

    public void printTcp() {
        final EditText ipAddress = (EditText) this.findViewById(R.id.edittext_tcp_ip);
        final EditText portAddress = (EditText) this.findViewById(R.id.edittext_tcp_port);

        try {
            new AsyncTcpEscPosPrint(
                    this,
                    new AsyncEscPosPrint.OnPrintFinished() {
                        @Override
                        public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                            Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !" + codeException + " . " + asyncEscPosPrinter.getPrinterConnection().isConnected() + " - "
                            );
                        }
                        //Soft_2024

                        @Override
                        public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                            Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                        }
                    }
            )
                    .execute(
                            this.getAsyncEscPosPrinter(
                                    new TcpConnection(
                                            ipAddress.getText().toString(),
                                            Integer.parseInt(portAddress.getText().toString())
                                    )
                            )
                    );
        } catch (NumberFormatException | UnsupportedEncodingException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Invalid TCP port address")
                    .setMessage("Port field must be an integer.")
                    .show();
            e.printStackTrace();
        }
    }

    /*==============================================================================================
    ===================================ESC/POS PRINTER PART=========================================
    ==============================================================================================*/

    public void DirectPrint(DeviceConnection printerConnection) {
        AsyncEscPosPrinter printerData = new AsyncEscPosPrinter(printerConnection, 203, 88f, 64);


        DeviceConnection deviceConnection = printerData.getPrinterConnection();

        if (deviceConnection == null) {
            return;
        }

        // FIXME: 1/23/25
        try {
            EscPosPrinter printer = new EscPosPrinter(
                    deviceConnection,
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine(),
                    new EscPosCharsetEncoding("windows-1252", 16)
            );

            printer.printFormattedText("[C]Phone: 01924547474");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection) throws UnsupportedEncodingException {
        String title = "Barnoi Lifestyle";
        String subtitle = "Building #235 (Beside of High School Playground), Taherpur Municipally, Taherpur, Bagmara, Rajshahi 6251";
        List<String> subTiles = getText(subtitle, 60);
        String finalSubtitle = "";
        for (String line : subTiles) {
            finalSubtitle = finalSubtitle + "[C]" + line + "\n";
        }
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");

        String paymentInvoice = "------------------------Payment Invoice------------------------";
        String invoiceNo = "OVWUGTTNXQ";
        String invoiceDate = format.format(new Date());
        String customerName = "John Doe";
        String customerPhone = "01924547474";
        String cashier = "Admin";
        String terminalId = "Counter-1";
        String totalProduct = "1";
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 88f, 64);

        String policyOne = "BARNOi Lifestyle-এর এক্সচেঞ্জ পলিসি অনুযায়ী পণ্যগুলি ৩ দিনের মধ্যে এক্সচেঞ্জ করা যেতে পারে প্রতিটি আইটেম একবার এক্সচেঞ্জ  যোগ্য";
        List<String> policiesOneList = getText(policyOne, 60);
        String policiesOne = "";
        for (String line : policiesOneList) {
            String hexBitmap = PrinterTextParserImg.bitmapToHexadecimalString(printer, createBitmapFromText(line, Typeface.DEFAULT));
            policiesOne = policiesOne + "[L]<img>" + hexBitmap + "</img>\n";
        }

        String policyTwo = ". ক্যাশ রিফান্ড প্রযোজ্য নয়,প্রযোজ্য ক্ষেত্রে ফেরত দেওয়া পণ্যের মূল্যের সমপরিমাণ একটি ক্রেডিট নোট প্রদান করা হবে";
        List<String> policiesTwoList = getText(policyTwo, 60);
        String policiesTwo = "";
        for (String line : policiesTwoList) {
            String hexBitmap = PrinterTextParserImg.bitmapToHexadecimalString(printer, createBitmapFromText(line, Typeface.DEFAULT));

            policiesTwo = policiesTwo + "[L]<img>" + hexBitmap + "</img>\n";
        }


        return printer.addTextToPrint(
                "[C]Phone: 01924547474\n" +
                        "[L]Cashier:" + cashier + "[R]Terminal ID:" + terminalId
                        +
                        "\n" +
                        "[L]SL[L]Product Name[R]Unit Price[R]QTY[R]UnitTotal\n" +
                        policiesOne +
                        policiesTwo +
                        "[C] <barcode type='128' width='50' height='40' text='none'>XRHR8075IH</barcode>\n"

               /* "[C]<b>" + title + "</b>\n" +
                        "[C]Phone: 01924547474\n" +
                        "[L]Cashier:" + cashier + "[R]Terminal ID:" + terminalId+

                // "[C]\n" +
                        "[L]Invoice No: <b>" + invoiceNo + "</b>" +
                        "[L]Invoice Date: " + invoiceDate + "\n" +
                        "[L]Customer Name:" + customerName + "\n" +
                        "[L]Phone:" + customerPhone + "\n" +
                        "[L]Cashier:" + cashier + "[R]Terminal ID:" + terminalId + "\n" +
                        "[C]----------------------------------------------------------------\n" +
                        // "[C]\n" +
                        "[L]SL[L]Product Name[R]Unit Price[R]QTY[R]UnitTotal\n" +
                        "[C]----------------------------------------------------------------\n" +
                        "[L]1[L]Men Premium Trouser L[R]$950.02[R]10x[R]<img>" + "TK" + "</img>\n" +
                        "[C]<b>----------------------------------------------------------------</b>\n" +
                        "[L][L][R][R]Sub Total:[R]$1200.00\n" +
                        "[L]          Discount[R]$0.00\n" +
                        "[C]          ------------------------------------------------------\n" +
                        "[L][L][R][R][R]$1200.00\n" +
                        "[L]          Vat[R]$0.00\n" +
                        "[C]<b>          ------------------------------------------------------</b>\n" +
                        "[L][L][R][R][R]$1200.00\n" +
                        "[C]----------------------------------------------------------------\n" +
                        "[L]Total Payable(" + totalProduct + "):[R]৳1200.00" + "\n" +
                        "[C]----------------------------------------------------------------\n" +
                        "[L]  Cash Payment:[R]$1200.00" + "\n" +
                        "[C]----------------------------------------------------------------\n" +
                        policiesOne +
                        policiesTwo +
                        "[C]Thanks for being with us\n" +
                        "[C] <barcode type='128' width='50' height='40' text='none'>XRHR8075IH</barcode>\n" +
                        "[C]Powered by: Softzino Technologies\n" +
                        "[C]https://softzio.com"
*/
        );
    }

  /*
       "[R]<b>BEAUTIFUL SHIRT</b>[R]9.99€\n" +
                        "[L]  + Size : S\n" +
                        "[L]\n" +
   "[C]\n" +
             "[C]------------------------------------------------\n" +
             "[R]TOTAL PRICE :[R]34.98€\n" +
             "[R]TAX :[R]4.23€\n" +
             "[C]\n" +
             "[C]================================================\n" +
             "[C]\n" +
             "[L]<u><font color='bg-black' size='tall'>Customer :</font></u>\n" +
             "[L]Raymond DUPONT\n" +
             "[L]5 rue des girafes\n" +
             "[L]31547 PERPETES\n" +
             "[L]Tel : +33801201456\n" +
             "\n" +
             "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
             "[L]\n" +
             "[C]<qrcode size='20'>https://dantsu.com/</qrcode>\n"*/


    public List<String> getText(String input, int maxLength) {
        String[] words = input.split(" ");
        List<String> result = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                result.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }

        return result;
    }

    public Bitmap createBitmapFromText(String text, Typeface typeface) {

        // dynamic canvas with text
        Paint paint = new Paint();
        paint.setTextSize(20); // Adjust font size
        paint.setTypeface(typeface); // Use a Bengali font
        paint.setAntiAlias(true);

        paint.setColor(Color.BLACK);

        int width = (int) paint.measureText(text);
        int height = (int) (paint.getFontMetrics().bottom - paint.getFontMetrics().top);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE); // Background color
        canvas.drawRect(0, 0, width, height, backgroundPaint);


        canvas.drawText(text, 0, -paint.getFontMetrics().top, paint);

        return bitmap;
    }

    public void printShared(PrinterShare printerShare) throws EscPosEncodingException, EscPosBarcodeException, EscPosParserException, EscPosConnectionException, IOException {
        String cashier = "Admin";
        String title = "Barnoi Lifestyle";

        SharedPrinterSize sharedPrinterSize = new SharedPrinterSize(203,
                88f,
                64,
                new EscPosCharsetEncoding("windows-1252", 16));

        String policyOne = "BARNOi Lifestyle-এর এক্সচেঞ্জ  দিনের মধ্যে এক্সচেঞ্জ করা যেতে পারে প্রতিটি আইটেম একবার এক্সচেঞ্জ  যোগ্য";
        List<String> policiesOneList = getText(policyOne, 60);
        String policiesOne = "";
        for (String line : policiesOneList) {
            String hexBitmap = SharedPrinterTextParserImg.bitmapToHexadecimalString(sharedPrinterSize, createBitmapFromText(line, Typeface.DEFAULT));
            policiesOne = policiesOne + "[L]<img>" + hexBitmap + "</img>\n";
        }

        String policyTwo = ". ক্যাশ রিফান্ড প্রযোজ্য নয়,প্রযোজ্য ক্ষেত্রে ফেরত দেওয়া পণ্যের মূল্যের সমপরিমাণ একটি ক্রেডিট নোট প্রদান করা হবে";
        List<String> policiesTwoList = getText(policyTwo, 60);
        String policiesTwo = "";
        for (String line : policiesTwoList) {
            String hexBitmap = PrinterTextParserImg.bitmapToHexadecimalString(sharedPrinterSize, createBitmapFromText(line, Typeface.DEFAULT));

            policiesTwo = policiesTwo + "[L]<img>" + hexBitmap + "</img>\n";
        }

        String terminalId = "Counter-1";
        String testStirng =
                "[C]Barnoi Life style\n" +
                        policiesTwo +


                        "[C]<barcode type='128' width='50' height='36' text='none'>XRHR8075IH</barcode>\n" +
                        "[C]Powered by: Softzino Technologies\n" +
                        "[C]https://softzio.com";
        //  policiesTwo +*/
        // "[C] <barcode type='128' width='50' height='40' text='none'>XRHR8075IH</barcode>\n";
        String printString = "";

        EscPosCharsetEncoding d = new EscPosCharsetEncoding("windows-1252", 16);

        SharedPrinterCommand sharedPrinterCommand = new SharedPrinterCommand(sharedPrinterSize.getEncoding(),
                printerShare);
        sharedPrinterCommand.useEscAsteriskCommand(true);
        SharedPrintTextParser textParser = new SharedPrintTextParser(sharedPrinterSize);
        String[] stringLines = testStirng.split("\n|\r\n");
        SharedPrinterTextParserLine[] linesParsed = new SharedPrinterTextParserLine[stringLines.length];
        int i = 0;
        for (String line : stringLines) {
            Log.d("Log404", "printing2 :   " + line);
            linesParsed[i++] = new SharedPrinterTextParserLine(textParser, line);
        }


        sharedPrinterCommand.reset();

        for (SharedPrinterTextParserLine line : linesParsed) {
            SharedPrinterTextColumn[] columns = line.getColumns();
            Log.d("Log404", "Printing7:   " + line.getColumns().length);

            ISharedPrinterTextParserElement lastElement = null;
            for (SharedPrinterTextColumn column : columns) {
                ISharedPrinterTextParserElement[] elements = column.getElements();
                for (ISharedPrinterTextParserElement element : elements) {
                    //  SharedPrinterTextParserString print = (SharedPrinterTextParserString) element;
                    //  Log.d("Log404", "Printing10:   " + print.getText());
                    //  printString = printString + print.getText();
                    element.print(sharedPrinterCommand);
                    lastElement = element;
                }
            }

            if (lastElement instanceof ISharedPrinterTextParserElement) {
                printString = printString + "\n";

            }
        }


        byte[] LF = new byte[]{0x0A};  // Feed 5 lines
        byte[] FEED_PAPER = new byte[]{0x1B, 0x64, 0x05};  // Feed 5 lines
        byte[] CUT_PAPER = new byte[]{0x1D, 0x56, 0x00};
        // Full cut

        byte[] setCharSizeNormal = new byte[]{0x1b, 0x21, 0x01};
        byte[] setPrintDensity = new byte[]{0x1D, 0x7C, 0x03};
        byte[] setLineSpacing = new byte[]{0x1B, 0x33, 0x02};  // 16 dots line spacing
        ByteArrayInputStream lineSpacingStream = new ByteArrayInputStream(setLineSpacing);
        ByteArrayInputStream feedStream = new ByteArrayInputStream(FEED_PAPER);
        ByteArrayInputStream cutStream = new ByteArrayInputStream(CUT_PAPER);
        ByteArrayInputStream lfStream = new ByteArrayInputStream(LF);
        ByteArrayInputStream fontsize = new ByteArrayInputStream(setCharSizeNormal);
        ByteArrayInputStream density = new ByteArrayInputStream(setPrintDensity);

        ArrayList<ByteArrayInputStream> finalStream = new ArrayList<ByteArrayInputStream>();
        byte[] messageByteArray = toPrimitive(sharedPrinterCommand.getByteArrayList());


        ByteArrayInputStream textStream = new ByteArrayInputStream(messageByteArray);
        InputStream[] streams = {fontsize,lfStream, density, textStream, feedStream};
        SequenceInputStream fullStream = new SequenceInputStream(Collections.enumeration(Arrays.asList(streams)));

        // Send the print job
        printerShare.print(fullStream);


        printerShare.close();

    }
}


