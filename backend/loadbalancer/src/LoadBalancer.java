import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class LoadBalancer {
    private static List<ServerInfo> servers = new ArrayList<>();
    private static Map<ServerInfo, Integer> serverClients = new HashMap<>();
    private static final Object serverClientsLock = new Object();
    private static final Map<String, ServerInfo> clientAffinity = new HashMap<>();
    private static final Map<String, Integer> clientActiveConnections = new HashMap<>();
    private static int rrIndex = 0;
    private static int maxClientsPerServer = 1;
    private static boolean stickyByIp = false;
    private static boolean firstServerOnly = false;
    private static boolean roundRobin = false;
    private static int lbPort = 2100;
    private static String loadedConfigPath = null;

    public static void main(String[] args) throws Exception {
        loadConfig();
        if (servers.isEmpty()) {
            servers.add(new ServerInfo("127.0.0.1", 2121));
            servers.add(new ServerInfo("127.0.0.1", 2122));
        }
        for (ServerInfo s : servers) {
            synchronized (serverClientsLock) {
                serverClients.put(s, 0);
            }
        }

        ServerSocket lbSocket = new ServerSocket(lbPort);
        System.out.println("Load Balancer running on port " + lbPort + "...");
        if (loadedConfigPath != null) {
            System.out.println("Config loaded from " + loadedConfigPath +
                    " | max_clients_per_server=" + maxClientsPerServer +
                    " | servers=" + servers.size() +
                    " | sticky_by_ip=" + stickyByIp +
                    " | first_server_only=" + firstServerOnly +
                    " | round_robin=" + roundRobin);
        } else {
            System.out.println("Config not found (using defaults) | max_clients_per_server=" + maxClientsPerServer +
                    " | servers=" + servers.size() +
                    " | sticky_by_ip=" + stickyByIp +
                    " | first_server_only=" + firstServerOnly +
                    " | round_robin=" + roundRobin);
        }

        while (true) {
            Socket clientSocket = lbSocket.accept();
            String clientAddr = String.valueOf(clientSocket.getRemoteSocketAddress());
            String clientKey = extractClientIp(clientSocket);
            ServerInfo target = allocateServer(clientKey);
            if (target == null) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("All servers busy. Try later.");
                System.out.println("Refus client " + clientAddr + " (ip=" + clientKey + ") | All servers busy.");
                clientSocket.close();
                continue;
            }
            int connectedNow;
            synchronized (serverClientsLock) {
                connectedNow = serverClients.getOrDefault(target, 0);
            }
            System.out.println("Client " + clientAddr + " -> " + target.ip + ":" + target.port +
                    " | Clients connectés: " + connectedNow);
            new Thread(new ClientForwarder(clientSocket, clientAddr, clientKey, target)).start();
        }
    }

    private static String extractClientIp(Socket clientSocket) {
        try {
            SocketAddress addr = clientSocket.getRemoteSocketAddress();
            if (addr instanceof InetSocketAddress) {
                InetAddress a = ((InetSocketAddress) addr).getAddress();
                if (a != null) return a.getHostAddress();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ServerInfo allocateServer(String clientKey) {
        return allocateServer(clientKey, Collections.emptySet());
    }

    private static ServerInfo allocateServer(String clientKey, Set<ServerInfo> exclude) {
        synchronized (serverClientsLock) {
            if (firstServerOnly) {
                if (servers.isEmpty()) return null;
                ServerInfo preferred = servers.get(0);
                if (exclude != null && exclude.contains(preferred)) {
                    return null;
                }
                int count = serverClients.getOrDefault(preferred, 0);
                if (count >= maxClientsPerServer) {
                    return null;
                }
                serverClients.put(preferred, count + 1);
                if (stickyByIp && clientKey != null) {
                    clientAffinity.put(clientKey, preferred);
                    clientActiveConnections.put(clientKey, clientActiveConnections.getOrDefault(clientKey, 0) + 1);
                }
                return preferred;
            }

            int n = servers.size();
            if (n == 0) return null;

            if (stickyByIp && clientKey != null) {
                ServerInfo pinned = clientAffinity.get(clientKey);
                if (pinned != null) {
                    if (exclude == null || !exclude.contains(pinned)) {
                    int count = serverClients.getOrDefault(pinned, 0);
                    if (count < maxClientsPerServer) {
                        serverClients.put(pinned, count + 1);
                        clientActiveConnections.put(clientKey, clientActiveConnections.getOrDefault(clientKey, 0) + 1);
                        return pinned;
                    }
                    }
                }
            }

            int start = 0;
            if (roundRobin) {
                start = Math.floorMod(rrIndex, n);
            }
            for (int offset = 0; offset < n; offset++) {
                int idx = roundRobin ? ((start + offset) % n) : offset;
                ServerInfo s = servers.get(idx);
                if (exclude != null && exclude.contains(s)) {
                    continue;
                }
                int count = serverClients.getOrDefault(s, 0);
                if (count < maxClientsPerServer) {
                    serverClients.put(s, count + 1);
                    if (roundRobin) {
                        rrIndex = (idx + 1) % n;
                    }
                    if (stickyByIp && clientKey != null) {
                        clientAffinity.put(clientKey, s);
                        clientActiveConnections.put(clientKey, clientActiveConnections.getOrDefault(clientKey, 0) + 1);
                    }
                    return s;
                }
            }
            return null;
        }
    }

    private static int releaseServer(ServerInfo server, String clientKey) {
        synchronized (serverClientsLock) {
            int prev = serverClients.getOrDefault(server, 0);
            int next = Math.max(0, prev - 1);
            serverClients.put(server, next);

            if (stickyByIp && clientKey != null) {
                int prevClient = clientActiveConnections.getOrDefault(clientKey, 0);
                int nextClient = Math.max(0, prevClient - 1);
                if (nextClient == 0) {
                    clientActiveConnections.remove(clientKey);
                    clientAffinity.remove(clientKey);
                } else {
                    clientActiveConnections.put(clientKey, nextClient);
                }
            }

            return next;
        }
    }

    private static File resolveConfigFile() {
        String override = System.getProperty("smartdrive.lbConfig");
        if (override == null || override.trim().isEmpty()) {
            override = System.getenv("SMARTDRIVE_LB_CONFIG");
        }
        if (override != null && !override.trim().isEmpty()) {
            Path p = Paths.get(override.trim());
            if (Files.exists(p)) return p.toFile();
        }

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path[] candidates = new Path[] {
                cwd.resolve("resources").resolve("lb_config.json"),
                cwd.resolve("loadbalancer").resolve("resources").resolve("lb_config.json"),
                cwd.resolve("backend").resolve("loadbalancer").resolve("resources").resolve("lb_config.json"),
                cwd.resolve("..").resolve("loadbalancer").resolve("resources").resolve("lb_config.json")
        };
        for (Path c : candidates) {
            if (Files.exists(c)) return c.toFile();
        }
        return null;
    }

    private static void loadConfig() {
        File configFile = resolveConfigFile();
        if (configFile == null || !configFile.exists()) {
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

            Object stickyObj = cfg.get("sticky_by_ip");
            if (stickyObj != null) {
                stickyByIp = Boolean.parseBoolean(stickyObj.toString());
            }

            Object firstOnlyObj = cfg.get("first_server_only");
            if (firstOnlyObj != null) {
                firstServerOnly = Boolean.parseBoolean(firstOnlyObj.toString());
            }

            Object rrObj = cfg.get("round_robin");
            if (rrObj != null) {
                roundRobin = Boolean.parseBoolean(rrObj.toString());
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
            loadedConfigPath = configFile.getPath();
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
        String clientAddr;
        String clientKey;
        ServerInfo server;

        ClientForwarder(Socket c, String clientAddr, String clientKey, ServerInfo s) {
            clientSocket = c;
            this.clientAddr = clientAddr;
            this.clientKey = clientKey;
            server = s;
        }

        @Override
        public void run() {
            Set<ServerInfo> tried = new HashSet<>();
            ServerInfo current = server;
            while (true) {
                if (current == null) {
                    current = allocateServer(clientKey, tried);
                    if (current == null) {
                        try {
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            out.println("All servers busy or unreachable. Try later.");
                        } catch (Exception ignored) {}
                        try {
                            if (clientSocket != null) clientSocket.close();
                        } catch (Exception ignored) {}
                        return;
                    }
                }

                System.out.println("Tentative de connexion vers " + current.ip + ":" + current.port);
                Socket serverSocket;
                try {
                    serverSocket = new Socket(current.ip, current.port);
                } catch (Exception e) {
                    System.err.println("Échec de connexion vers " + current.ip + ":" + current.port + " (" + e.getMessage() + ")");
                    tried.add(current);
                    releaseServer(current, clientKey);
                    current = null;
                    continue;
                }

                try (serverSocket) {
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
                    int remaining = releaseServer(current, clientKey);
                    System.out.println("Client " + clientAddr + " déconnecté de " + current.ip + ":" + current.port +
                            " | Clients restants: " + remaining);
                    try {
                        if (clientSocket != null) clientSocket.close();
                    } catch (Exception ignored) {}
                }
                return;
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
