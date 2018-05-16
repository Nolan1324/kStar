import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import sql.CacheRepository;
import sql.ClassesRepository;
import sql.SettingsRepository;
import structure.Class;
import utils.*;
import structure.*;

public class Main {

    public static HttpServer server;
    public static WebClient client;

    public static SettingsRepository settingsRepo;
    public static CacheRepository cacheRepo;

    public static Map<Integer, Class> classes = new HashMap<>();
    public static int classesHash = 0;
    public static byte[] classesResponse;

    public static void main(String[] args) throws IOException {
        settingsRepo = new SettingsRepository();
        cacheRepo = new CacheRepository();

        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8000),0);
        server.createContext("/kstar", new ServerHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        client = new WebClient(settingsRepo);

        /*
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI("http://localhost:8000/kstar"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */

        String sessionId = settingsRepo.load().sessionId;

        if(!sessionId.equals("")) {
            client.setSessionId(sessionId);
        } else {
            client.obtainSessionId();
        }

        Cache classCache = cacheRepo.load("class");
        if(classCache.type != null) {
            classesHash = classCache.siteHash;
            classesResponse = classCache.compiledData;
        }

        ClassesRepository classesRepository = new ClassesRepository();
        classes = classesRepository.load();
    }

    static class ServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String uri = t.getRequestURI().toString();

            switch (uri) {
                case "/kstar":
                    handleRoot(t);
                    break;
                case "/kstar/theme.css":
                    handleTheme(t);
                    break;
                case "/kstar/change_theme":
                    handleChangeTheme(t);
                    break;
                case "/kstar/logincreds.html":
                    handleLogin(t);
                    break;
                case "/kstar/logout":
                    handleLogout(t);
                    break;
                case "/kstar/load/assignments":
                    handleAssignments(t);
                    break;
                case "/kstar/load/scale":
                    handleGradingScale(t);
                    break;
                case "/kstar/settings/highlight_changes":
                    handleHighlightChanges(t);
                default:
                    if(uri.matches("^/kstar/.+\\.html$")) {
                        handleHtml(t, uri);
                    } else if(uri.matches("^/kstar/.+$")) {
                        handleFile(t, uri);
                    } else {
                        handleFile(t, "/kstar/not_found.html");
                    }
                    break;
            }
        }
    }

    private static void handleRoot(HttpExchange t) throws IOException {
        if(!settingsRepo.load().sessionId.equals("") && client.loggedIn()) {
            t.getResponseHeaders().add("Location", "/kstar/main.html");
        } else {
            if(!settingsRepo.load().password.equals("")) {
                client.doLogIn(settingsRepo.load().pin, settingsRepo.load().password, "off");
                t.getResponseHeaders().add("Location", "/kstar/main.html");
            } else {
                client.logOut();
                t.getResponseHeaders().add("Location", "/kstar/login.html");
            }
        }

        t.sendResponseHeaders(302, 0);
    }

    private static void handleTheme(HttpExchange t) {
        try {
            String file = settingsRepo.load().theme.file;
            byte[] response = GuiFileUtil.getFile(file + ".css");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleChangeTheme(HttpExchange t) {
        try {
            Map<String, String> params = ParamsUtil.splitQuery(new String(t.getRequestBody().readAllBytes()));
            Settings settings = settingsRepo.load();
            try { settings.theme = Settings.Theme.getTheme(Integer.parseInt(params.get("code"))); } catch (Exception e) {}
            try { settings.highlightChanges = Boolean.parseBoolean(params.get("highlightChanges")); } catch (Exception e) {}
            settingsRepo.save(settings);

            t.sendResponseHeaders(200, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleLogin(HttpExchange t) {
        try {
            Map<String, String> params = ParamsUtil.splitQuery(new String(t.getRequestBody().readAllBytes()));
            Settings settings = settingsRepo.load();
            settings.pin = params.get("pin");
            settingsRepo.save(settings);

            JSONObject loginResponse = client.doLogIn(params.get("pin"), params.get("password"), params.get("remember_me"));

            byte[] response = loginResponse.toString().getBytes();
            t.getResponseHeaders().add("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleLogout(HttpExchange t) throws IOException {
        client.logOut();

        t.getResponseHeaders().add("Location", "/kstar/login.html");
        t.sendResponseHeaders(302, 0);

        classes = new HashMap<>();
        classesHash = 0;
        new ClassesRepository().clear();
        new CacheRepository().clear();

        Settings settings = settingsRepo.load();
        settings.password = "";
        settingsRepo.save(settings);
    }

    private static void handleAssignments(HttpExchange t) {
        try {
            String data = client.getAssignments();

            if(data.hashCode() != classesHash || classesResponse == null) {
                Map<Integer, Class> tClasses = new HashMap<>();
                for (Element cElement : Jsoup.parse(data).body().select("[id~=tblassign_\\d+]")) {
                    Class classO = new Class(cElement);
                    tClasses.put(classO.per, classO);
                }

                int totalNew = 0;
                if(classes.size() == tClasses.size() && !classes.isEmpty()) {
                    for(Class c : tClasses.values()) {
                        int difference = c.assignments.size() - classes.get(c.per).assignments.size();
                        if(difference > 0) {
                            c.newItems = difference;
                            totalNew += difference;
                        }
                    }
                }

                Element responseE = new Element("ul");
                responseE.addClass("collapsible");
                for (Class classO : tClasses.values()) {
                    Element classE = classO.toElement()
                            .attr("id", "assign_" + classO.per)
                            .addClass("scrollspy");

                    responseE.appendChild(classE);
                }

                classesResponse = responseE.toString().getBytes();

                byte[] response = classesResponse;
                t.getResponseHeaders().add("new", Integer.toString(totalNew));
                t.sendResponseHeaders(200, response.length);
                OutputStream os = t.getResponseBody();
                os.write(response);
                os.close();

                classesHash = data.hashCode();
                classes = tClasses;

                ClassesRepository classesRepository = new ClassesRepository();
                classesRepository.save(classes);

                if(!classes.isEmpty()) {
                    cacheRepo.save(new Cache("class", classesHash, classesResponse));
                }
            } else {
                byte[] response = classesResponse;
                t.sendResponseHeaders(200, response.length);
                OutputStream os = t.getResponseBody();
                os.write(response);
                os.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleGradingScale(HttpExchange t) {
        try {
            CacheRepository cacheRepository = new CacheRepository();
            Cache cache = cacheRepository.load("scale");

            byte[] response;
            if (cache.type == null) {
                GradingScales gradingScales = new GradingScales(classes);
                Gson gson = new Gson();
                response = gson.toJson(gradingScales.gradingScales).getBytes();
                cacheRepository.save(new Cache("scale", 0, response));
            } else {
                response = cache.compiledData;
                t.getResponseHeaders().add("fromcache", "1");
            }

            try {
                t.sendResponseHeaders(200, response.length);
                OutputStream os = t.getResponseBody();
                os.write(response);
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleHighlightChanges(HttpExchange t) {
        byte[] response = settingsRepo.load().highlightChanges ? "1".getBytes() : "0".getBytes();

        try {
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleFile(HttpExchange t, String uri) throws IOException {
        String fileName = uri.replace("/kstar/", "");

        if(GuiFileUtil.fileExists(fileName)) {
            byte[] response = GuiFileUtil.getFile(fileName);
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        } else {
            handleHtml(t, "/kstar/not_found.html", 404);
        }
    }

    private static void handleHtml(HttpExchange t, String uri) throws IOException {
        handleHtml(t, uri, 200);
    }

    private static void handleHtml(HttpExchange t, String uri, int rCode) throws IOException {
        String fileName = uri.replace("/kstar/", "");

        if(GuiFileUtil.fileExists(fileName)) {
            String response = new String(GuiFileUtil.getFile(fileName));

            //Pin replacement
            if(fileName.equals("login.html")) {
                String pin = settingsRepo.load().pin;
                if (pin.equals("")) {
                    response = response.replaceAll("\\$\\$\\$autoPins\\$\\$\\$", "");
                } else {
                    response = response.replaceAll("\\$\\$\\$autoPins\\$\\$\\$", pin);
                }
            } else if(fileName.equals("settings.html")) {
                response = response.replace("$$$highlightChecked$$$", settingsRepo.load().highlightChanges ? "checked" : "");
            }

            t.sendResponseHeaders(rCode, response.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            handleHtml(t, "/kstar/not_found.html", 404);
        }
    }
}