import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class LoadBalancer {
    private static List<ServerInfo> servers = new ArrayList<>();
    private static Map<ServerInfo, Integer> serverClients = new HashMap<>();
    private static int maxClientsPerServer = 3;
    private static int lbPort = 2100;

    public static void main(String[] args) throws Exception {
        loadConfig();
        if (servers.isEmpty()) {
            servers.add(new ServerInfo("127.0.0.1", 2121));
            servers.add(new ServerInfo("127.0.0.1", 2122));
        }
        for (ServerInfo s : servers) {
            serverClients.put(s, 0);
        }

        ServerSocket lbSocket = new ServerSocket(lbPort);
        System.out.println("Load Balancer running on port " + lbPort + "...");

        while (true) {
            Socket clientSocket = lbSocket.accept();
            ServerInfo target = chooseServer();
            if (target == null) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("All servers busy. Try later.");
                clientSocket.close();
                continue;
            }
            serverClients.put(target, serverClients.get(target) + 1);
            System.out.println("Connexion sur " + target.ip + ":" + target.port +
                    " | Clients connectés: " + serverClients.get(target));
            new Thread(new ClientForwarder(clientSocket, target)).start();
        }
    }

    private static ServerInfo chooseServer() {
        for (ServerInfo s : servers) {
            if (serverClients.get(s) < maxClientsPerServer)
                return s;
        }
        return null;
    }

    private static void loadConfig() {
        File configFile = new File("resources/lb_config.json");
        if (!configFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            JSONParser parser = new JSONParser();
            JSONObject cfg = (JSONObject) parser.parse(reader);

            Object lbPortObj = cfg.get("lb_port");
            if (lbPortObj != null) {
                lbPort = Integer.parseInt(lbPortObj.toString());
            }

            Object maxObj = cfg.get("max_clients_per_server");
            if (maxObj != null) {
                maxClientsPerServer = Integer.parseInt(maxObj.toString());
            }

            Object serversObj = cfg.get("servers");
            if (serversObj instanceof JSONArray) {
                servers.clear();
                JSONArray arr = (JSONArray) serversObj;
                for (Object o : arr) {
                    if (!(o instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject s = (JSONObject) o;
                    Object ipObj = s.get("ip");
                    Object portObj = s.get("port");
                    if (ipObj == null || portObj == null) {
                        continue;
                    }
                    servers.add(new ServerInfo(ipObj.toString(), Integer.parseInt(portObj.toString())));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load lb_config.json: " + e.getMessage());
        }
    }

    static class ServerInfo {
        String ip;
        int port;

        ServerInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ServerInfo that = (ServerInfo) o;
            return port == that.port && Objects.equals(ip, that.ip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, port);
        }
    }

    static class ClientForwarder implements Runnable {
        Socket clientSocket;
        ServerInfo server;

        ClientForwarder(Socket c, ServerInfo s) {
            clientSocket = c;
            server = s;
        }

        @Override
        public void run() {
            try (
                Socket serverSocket = new Socket(server.ip, server.port)
            ) {
                final Socket sSocket = serverSocket; 
                Thread t1 = new Thread(() -> forwardData(clientSocket, sSocket));
                Thread t2 = new Thread(() -> forwardData(sSocket, clientSocket));
                t1.start();
                t2.start();
                t1.join();
                t2.join();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                synchronized (serverClients) {
                    serverClients.put(server, serverClients.get(server) - 1);
                    System.out.println("Déconnexion sur " + server.ip + ":" + server.port +
                            " | Clients restants: " + serverClients.get(server));
                }
                try {
                    if (clientSocket != null)
                        clientSocket.close();
                } catch (Exception e) {
                }
            }
        }

        private void forwardData(Socket inSocket, Socket outSocket) {
            try {
                InputStream in = inSocket.getInputStream();
                OutputStream out = outSocket.getOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
                outSocket.shutdownOutput();
            } catch (Exception e) {
            }
        }
    }
}