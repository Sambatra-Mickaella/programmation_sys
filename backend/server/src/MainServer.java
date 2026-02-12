import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    public static void main(String[] args) {
        int port = 2122;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch(Exception ignored) {}
        }
        try{
            NotificationService.setMyPort(port);
            NotificationService.startListener(port);

            ServerSocket serverSocket = new ServerSocket(port);
            FileManager fm = new FileManager();
            System.out.println("Server running on port " + port + "...");

            while(true){
                Socket client = serverSocket.accept();
                new Thread(new ClientThread(client,fm)).start();
            }
        } catch(Exception e){ e.printStackTrace(); }
    }
}