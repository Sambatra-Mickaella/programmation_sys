package controller;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import model.Serveur;

public final class BackendConfig {
    private BackendConfig() {
    }

    public static Serveur newServeur() {
        String host = resolveHost();
        int port = resolvePort();
        return new Serveur(host, port);
    }

    private static String resolveHost() {
        String fromEnv = firstNonBlank(
                System.getenv("SMARTDRIVE_BACKEND_HOST"),
                System.getenv("SMARTDRIVE_SERVER_IP"));
        if (fromEnv != null) {
            return fromEnv;
        }

        String fromConfig = readConfigValue("server_ip");
        return (fromConfig != null && !fromConfig.isBlank()) ? fromConfig : "127.0.0.1";
    }

    private static int resolvePort() {
        String fromEnv = firstNonBlank(
                System.getenv("SMARTDRIVE_BACKEND_PORT"),
                System.getenv("SMARTDRIVE_SERVER_PORT"));
        if (fromEnv != null) {
            try {
                return Integer.parseInt(fromEnv);
            } catch (NumberFormatException ignored) {
            }
        }

        String fromConfig = readConfigValue("server_port");
        if (fromConfig != null) {
            try {
                return Integer.parseInt(fromConfig);
            } catch (NumberFormatException ignored) {
            }
        }

        return 2100;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String readConfigValue(String key) {
        try (InputStream is = BackendConfig.class.getClassLoader().getResourceAsStream("config.json")) {
            if (is == null) {
                return null;
            }
            JSONObject cfg = (JSONObject) new JSONParser().parse(new InputStreamReader(is));
            Object obj = cfg.get(key);
            return (obj == null) ? null : obj.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
