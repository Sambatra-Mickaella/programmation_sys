package model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.io.PrintWriter;
import java.net.Socket;

public class Serveur {
    private String ip;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // constructor
    public Serveur(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    // getters
    public String getIp() {
        return ip;
    }
    public int getPort() {
        return port;
    }

    // setters
    public void setIp(String ip) {
        this.ip = ip;
    }
    public void setPort(int port) {
        this.port = port;
    }

    private static int readEnvInt(String key, int defaultValue) {
        try {
            String v = System.getenv(key);
            if (v == null || v.isBlank())
                return defaultValue;
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // Connexion au serveur
    public void connect() throws Exception {
        int connectTimeoutMs = readEnvInt("SMARTDRIVE_CONNECT_TIMEOUT_MS", 3000);
        int readTimeoutMs = readEnvInt("SMARTDRIVE_READ_TIMEOUT_MS", 7000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress(this.ip, this.port), connectTimeoutMs);
        s.setSoTimeout(readTimeoutMs);
        this.socket = s;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public Socket getSocket() {
        return this.socket;
    }

    public BufferedReader getInReader() {
        return this.in;
    }

    public PrintWriter getOutWriter() {
        return this.out;
    }

    public void close() throws Exception {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
