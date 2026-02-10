import java.io.*;
import java.net.*;
import java.util.*;

public class ProxyCache {
    private static Map<String, File> cache = new HashMap<>();
    private static int MAX_CACHE = 10;
    private static String CACHE_DIR = "../shared_storage/cache";

    public static void main(String[] args) throws Exception{
        new File(CACHE_DIR).mkdirs();
        ServerSocket proxySocket = new ServerSocket(2200);
        System.out.println("Proxy Cache running on port 2200...");

        while(true){
            Socket client = proxySocket.accept();
            new Thread(new ClientHandler(client)).start();
        }
    }

    static class ClientHandler implements Runnable {
        Socket client;
        ClientHandler(Socket s){ client=s;}

        @Override
        public void run(){
            try{
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(),true);

                String request = in.readLine();
                if(cache.containsKey(request)){
                    File f = cache.get(request);
                    out.println(f.length());
                    FileInputStream fis = new FileInputStream(f);
                    OutputStream os = client.getOutputStream();
                    byte[] buffer = new byte[4096];
                    int read;
                    while((read=fis.read(buffer))>0) os.write(buffer,0,read);
                    os.flush(); fis.close();
                } else {
                    out.println("0"); // Pas trouvé
                }
            } catch(Exception e){ e.printStackTrace();}
        }
    }
}
