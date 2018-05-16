package utils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import sql.SettingsRepository;
import structure.Settings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebClient {

    private SettingsRepository settingsRepo;

    public WebClient(SettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    public void obtainSessionId() {
        HttpResponse<String> response = null;
        try {
            response = Unirest.get("https://webconnect.bloomfield.org/zangle/studentportal").asString();

            String cookie = response.getHeaders().getFirst("Set-Cookie");
            Pattern pattern = Pattern.compile("ASP.NET_SessionId=([a-z0-9]+);");
            Matcher matcher = pattern.matcher(cookie);

            String sessionId = "";
            while (matcher.find()) {
                sessionId = matcher.group(1);
            }

            setSessionId(sessionId);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    public void setSessionId(String sessionId) {
        Settings settings = settingsRepo.load();
        settings.sessionId = sessionId;
        settingsRepo.save(settings);
        Unirest.setDefaultHeader("Cookie", "ASP.NET_SessionId=" + sessionId + ";");
    }

    public boolean loggedIn() {
        try {
            HttpResponse<String> timeOut = Unirest.post("https://webconnect.bloomfield.org/zangle/StudentPortal/Home/PortalMainPage")
                    .asString();

            return !timeOut.getBody().contains("Timed Out");
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return false;
    }

    public JSONObject doLogIn(String pin, String password, String rememberMe) {
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.post("https://webconnect.bloomfield.org/zangle/StudentPortal/Home/Login")
                    .field("Pin", pin)
                    .field("Password", password)
                    .asJson();

            JSONObject json = jsonResponse.getBody().getObject();

            if(json.getInt("valid") == 1) {
                if(rememberMe != null) {
                    Settings settings = settingsRepo.load();
                    settings.password = password;
                    settingsRepo.save(settings);
                }

                HttpResponse<String> banners = Unirest.get("https://webconnect.bloomfield.org/zangle/StudentPortal/Home/PortalMainPage").asString();
                Document bannersDoc = Jsoup.parse(banners.getBody());

                Unirest.get("https://webconnect.bloomfield.org/zangle/StudentPortal/StudentBanner/SetStudentBanner/" + bannersDoc.getElementsByClass("sturow").first().id()).asString();
            }

            return json;
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return new JSONObject();
    }

    public void logOut() {
        try {
            Unirest.get("https://webconnect.bloomfield.org/zangle/StudentPortal/Home/Logout").asBinary();
            Unirest.get("https://webconnect.bloomfield.org/zangle/StudentPortal").asBinary();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    public String getAssignments() {
        try {
            HttpResponse<String> assignments = Unirest.get("https://webconnect.bloomfield.org/zangle/StudentPortal/Home/LoadProfileData/Assignments")
                    .asString();

            return assignments.getBody();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return null;
    }
}
