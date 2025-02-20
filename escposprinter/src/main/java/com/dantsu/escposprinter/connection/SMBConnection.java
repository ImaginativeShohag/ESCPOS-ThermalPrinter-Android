package com.dantsu.escposprinter.connection;

import android.util.Log;

import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.PrinterShare;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;

public class SMBConnection {
    SMBClient socket = new SMBClient();
    private String ipAddress;
    private int port;
    private int timeout;
    private String printerName;
    private String userName;
    private String password;
    private String domainName;
    private AuthenticationContext authenticationContext;
    private Session session;

    /**
     * Create un instance of TcpConnection.
     *
     * @param ipAddress IP address of the device
     * @param port      Port of the device
     */
    public SMBConnection(String ipAddress, int port,
                         String printerName, String userName, String password, String domainName) {
        this(ipAddress, port, 1000, printerName, userName, password, domainName);
    }

    /**
     * Create un instance of TcpConnection.
     * <p>
     * Overload of the above function TcpConnection()
     * Include timeout parameter in milliseconds.
     *
     * @param ipAddress IP address of the device
     * @param port      Port of the device
     * @param timeout   Timeout in milliseconds to establish a connection
     */
    public SMBConnection(String ipAddress, int port, int timeout,
                         String printerName, String userName, String password, String domainName) {
        super();
        this.ipAddress = ipAddress;
        this.port = port;
        this.timeout = timeout;
        this.printerName = printerName;
        this.userName = userName;
        this.password = password;
        this.domainName = domainName;
    }

    /**
     * Check if the TCP device is connected by socket.
     *
     * @return true if is connected
     */
    public boolean isConnected() {
        try (Connection connection = socket.connect(ipAddress)) {
            authenticationContext = new AuthenticationContext(userName, password.toCharArray(), domainName);
            session = connection.authenticate(authenticationContext);
            return session.getConnection().isConnected();
        } catch (IOException e) {
            return false;
        }

    }

    /**
     * Start socket connection with the TCP device.
     */
    public void connect() throws EscPosConnectionException {
        if (this.isConnected()) {
            Log.d("Log404", "connect:  got the connect");
            return;

        }

        Log.d("Log404", "connect:  initialize the connect");
        try (Connection connection = socket.connect(ipAddress)) {
            authenticationContext = new AuthenticationContext(userName, password.toCharArray(), domainName);
            session = connection.authenticate(authenticationContext);

        } catch (IOException e) {
            throw new EscPosConnectionException("Unable to connect to TCP device.");

        }
        Log.d("Log404", "connect:  ------- " + "success");


    }

    /**
     * Close the socket connection with the TCP device.
     */
    public SMBConnection disconnect() {
        Log.d("Log404", "connect:  ----call disconnect--- ");

        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }
        return this;
    }

    public void print(byte[]  message) {
        try (Connection connection = socket.connect(ipAddress)) {
            //authenticationContext = new AuthenticationContext(userName, password.toCharArray(), domainName);
            session = connection.authenticate(authenticationContext);
            try (PrinterShare printer = (PrinterShare) session.connectShare(printerName)) {
                //  ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));

                byte[] FEED_PAPER = new byte[]{0x1B, 0x64, 0x05};  // Feed 5 lines
                byte[] CUT_PAPER = new byte[]{0x1D, 0x56, 0x00};   // Full cut

                // Create an input stream combining text + feed + cut
                ByteArrayInputStream textStream = new ByteArrayInputStream(message);
                ByteArrayInputStream feedStream = new ByteArrayInputStream(FEED_PAPER);
                ByteArrayInputStream cutStream = new ByteArrayInputStream(CUT_PAPER);

                // Merge all streams
                SequenceInputStream fullStream = new SequenceInputStream(new SequenceInputStream(textStream, feedStream), cutStream);

                // Send the print job
                printer.print(fullStream);


                printer.close();
                Log.d("Log404", "run:printing...... ");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
