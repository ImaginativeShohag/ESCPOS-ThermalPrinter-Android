package com.dantsu.escposprinter.connection.tcp;

import android.util.Log;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TcpConnection extends DeviceConnection {
    private Socket socket = null;
    private String address;
    private int port;
    private int timeout;

    /**
     * Create un instance of TcpConnection.
     *
     * @param address IP address of the device
     * @param port    Port of the device
     */
    public TcpConnection(String address, int port) {
        this(address, port, 1000);
    }

    /**
     * Create un instance of TcpConnection.
     * 
     * Overload of the above function TcpConnection()
     * Include timeout parameter in milliseconds.
     *
     * @param address IP address of the device
     * @param port    Port of the device
     * @param timeout Timeout in milliseconds to establish a connection
     */
    public TcpConnection(String address, int port, int timeout) {
        super();
        this.address = address;
        this.port = port;
        this.timeout = timeout;
    }

    /**
     * Check if the TCP device is connected by socket.
     *
     * @return true if is connected
     */
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected() && super.isConnected();
    }

    /**
     * Start socket connection with the TCP device.
     */
    public TcpConnection connect() throws EscPosConnectionException {
        if (this.isConnected()) {
            Log.d("Log404", "connect:  got the connect");
            return this;
        }
        try {
            Log.d("Log404", "connect:  initialize the connect");
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(InetAddress.getByName(this.address), this.port), this.timeout);
            this.outputStream = this.socket.getOutputStream();
            this.data = new byte[0];
            Log.d("Log404", "connect:  ------- "+"success");
        } catch (IOException e) {
            e.printStackTrace();
            this.disconnect();
            Log.d("Log404", "connect:  ----failed--- "+e.getMessage());
            throw new EscPosConnectionException("Unable to connect to TCP device.");
        }
        return this;
    }

    /**
     * Close the socket connection with the TCP device.
     */
    public TcpConnection disconnect() {
        Log.d("Log404", "connect:  ----call disconnect--- ");

        this.data = new byte[0];
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
                this.outputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (this.socket != null) {
            try {
                this.socket.close();
                this.socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

}
