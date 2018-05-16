package sql;

import org.sqlite.SQLiteConnection;
import structure.Class;
import structure.Settings;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.text.MessageFormat;
import java.util.Set;

public class SettingsRepository {

    private static String url = "jdbc:sqlite:local.db";

    public SettingsRepository() {
        try {
            Connection conn = DriverManager.getConnection(url);
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Settings (" +
                    "key INTEGER PRIMARY KEY," +
                    "theme INTEGER," +
                    "highlightChanges INTEGER," +
                    "pin VARCHAR(8)," +
                    "password VARCHAR(6)," +
                    "sessionId VARCHAR(40))");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Settings load() {
        Settings settings = new Settings();

        try {
            Connection conn = DriverManager.getConnection(url);

            ResultSet results = conn.createStatement().executeQuery("SELECT * FROM Settings");
            settings.theme = Settings.Theme.getTheme(results.getInt("theme"));
            settings.highlightChanges = results.getBoolean("highlightChanges");
            settings.pin = results.getString("pin");
            settings.password = results.getString("password");
            settings.sessionId = results.getString("sessionId");

            results.close();
            conn.close();
        } catch (SQLException e) {
            //e.printStackTrace();
        }

        return settings;
    }

    public void save(Settings s) {
        try {
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement statement = conn.prepareStatement("REPLACE INTO Settings (key,theme,highlightChanges,pin,password,sessionId) VALUES (0,?,?,?,?,?)");

            int i = 1;
            for(Field field : Settings.class.getFields()) {
                Type type = field.getType();
                try {
                    if(type == String.class) {
                        statement.setString(i, field.get(s).toString());
                        i++;
                    } else if(type == Settings.Theme.class) {
                        statement.setInt(i, ((Settings.Theme)field.get(s)).code);
                        i++;
                    } else if(type == boolean.class) {
                        statement.setBoolean(i, (boolean)field.get(s));
                        i++;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if(i >= 6) {
                    break;
                }
            }

            statement.executeUpdate();
            statement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
