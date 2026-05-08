import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class PhilipsSystem {
    public static final String TOKEN = "h7Vs1bGtnuL04MjgNUS7ADpVHFAuO-1A0Yyan-Mi";
    public static final String DEVICE_TYPE = "5A152026";

    public PhilipsSystem() {
        System.out.println("Session Established - TOKEN = " + TOKEN);
    }

    public String getLamps() throws IOException, URISyntaxException {
        return makeRequest("http://172.16.17.200/api/" + TOKEN + "/lights", "GET", null);
    }

    public String getLampStateByID(int lampID) throws IOException, URISyntaxException {
        return makeRequest("http://172.16.17.200/api/" + TOKEN + "/lights/" + lampID, "GET", null);
    }

    public String setLampStateByIdHsl(int lampID, boolean state, int saturation, int brightness, int hue) throws IOException, URISyntaxException {
        String body = "{\"on\":" + state + ",\"sat\":" + saturation + ",\"bri\":" + brightness + ",\"hue\":" + hue + "}";
        return makeRequest("http://172.16.17.200/api/" + TOKEN + "/lights/" + lampID + "/state", "PUT", body);
    }

    public String setLampStateByIdHex(int lampID, boolean state, String hex) throws IOException, URISyntaxException {
        if (hex.startsWith("#")) hex = hex.substring(1);
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        int hue = (int) (hsb[0] * 65535);
        int sat = (int) (hsb[1] * 254);
        int bri = (int) (hsb[2] * 254);
        return setLampStateByIdHsl(lampID, state, sat, bri, hue);
    }

    private String makeRequest(String urlS, String method, String body) throws IOException, URISyntaxException {
        URI uri = new URI(urlS);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (body != null) {
            conn.setDoOutput(true);
            try (PrintWriter out = new PrintWriter(conn.getOutputStream())) {
                out.print(body);
                out.flush();
            }
        }
        StringBuilder res = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) res.append(line);
        }
        return res.toString();
    }
}

public class Main {
    public static void main(String[] args) throws IOException {
        PhilipsSystem system = new PhilipsSystem();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/", (exchange) -> {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            try {
                if (path.equals("/getLamps")) {
                    sendJsonResponse(exchange, system.getLamps());
                } else if (path.equals("/setHex")) {
                    Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(params.get("id"));
                    String hex = params.get("color");
                    sendJsonResponse(exchange, system.setLampStateByIdHex(id, true, hex));
                } else if (path.equals("/toggle")) {
                    Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(params.get("id"));
                    boolean state = Boolean.parseBoolean(params.get("on"));
                    sendJsonResponse(exchange, system.setLampStateByIdHsl(id, state, 254, 254, 10000));
                } else {
                    sendJsonResponse(exchange, "{\"error\":\"Not Found\"}", 404);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        });

        server.createContext("/getLamps", (exchange) -> {
            try {
                String json = system.getLamps();
                sendJsonResponse(exchange, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/setHex", (exchange) -> {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            try {
                int id = Integer.parseInt(params.get("id"));
                String hex = params.get("color");
                String json = system.setLampStateByIdHex(id, true, hex);
                sendJsonResponse(exchange, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"Invalid parameters\"}");
            }
        });

        server.createContext("/toggle", (exchange) -> {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            try {
                int id = Integer.parseInt(params.get("id"));
                boolean state = Boolean.parseBoolean(params.get("on"));
                String json = system.setLampStateByIdHsl(id, state, 254, 254, 10000);
                sendJsonResponse(exchange, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"Error during toggle\"}");
            }
        });

        server.setExecutor(null);
        System.out.println("Server started at http://localhost:8080");
        server.start();
    }

    private static void sendJsonResponse(HttpExchange exchange, String response, int code) throws IOException {
        if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    private static void sendJsonResponse(HttpExchange exchange, String response) throws IOException {
        sendJsonResponse(exchange, response, 200);
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) result.put(entry[0], entry[1]);
        }
        return result;
    }
}