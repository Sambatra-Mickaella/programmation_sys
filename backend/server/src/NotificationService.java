import java.io.*;
import java.net.*;
import java.util.*;
import org.json.simple.JSONObject;

public class NotificationService {
    private static List<String> otherPrimaries = Arrays.asList("127.0.0.1:2122", "127.0.0.1:2123"); // adjust for each primary
    private static int myPort = 2121; // set per instance

    public static void setMyPort(int port) {
        myPort = port;
        otherPrimaries = new ArrayList<>();
        if (port != 2121) otherPrimaries.add("127.0.0.1:2121");
        if (port != 2122) otherPrimaries.add("127.0.0.1:2122");
        if (port != 2123) otherPrimaries.add("127.0.0.1:2123");
    }

    public static void broadcastNewFile(String username, String filename, long size) {
        JSONObject msg = new JSONObject();
        msg.put("event", "new_file");
        msg.put("user", username);
        msg.put("filename", filename);
        msg.put("size", size);
        msg.put("timestamp", System.currentTimeMillis());

        for (String addr : otherPrimaries) {
            try {
                String[] parts = addr.split(":");
                Socket sock = new Socket(parts[0], Integer.parseInt(parts[1]) + 100); // notification port = primary port + 100
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                out.println(msg.toJSONString());
                sock.close();
            } catch (Exception e) {
                System.err.println("Failed to notify " + addr + ": " + e.getMessage());
            }
        }
    }

    public static void startListener(int primaryPort) {
        int listenPort = primaryPort + 100;
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(listenPort)) {
                System.out.println("Notification listener on port " + listenPort);
                while (true) {
                    Socket sock = ss.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String line = in.readLine();
                    if (line != null) {
                        System.out.println("Received notification: " + line);
                        // TODO: notify connected clients
                    }
                    sock.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
