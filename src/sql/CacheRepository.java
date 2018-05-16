package sql;

import structure.Cache;
import structure.Class;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CacheRepository {
    private static String url = "jdbc:sqlite:local.db";

    public CacheRepository() {
        try {
            Connection conn = DriverManager.getConnection(url);
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Cache (" +
                    "type TEXT PRIMARY KEY," +
                    "siteHash INTEGER," +
                    "compiledData BLOB)");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Cache load(String type) {
        Cache cache = new Cache();

        try {
            Connection conn = DriverManager.getConnection(url);

            PreparedStatement statement = conn.prepareStatement("SELECT * FROM Cache WHERE type = ?");
            statement.setString(1, type);
            ResultSet results = statement.executeQuery();

            cache.type = results.getString("type");
            cache.siteHash = results.getInt("siteHash");
            cache.compiledData = results.getBytes("compiledData");

            statement.close();
            results.close();
            conn.close();
        } catch (SQLException e) {
            //e.printStackTrace();
        }

        return cache;
    }

    public void save(Cache cache) {
        try {
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement statement = conn.prepareStatement("REPLACE INTO Cache (type,siteHash,compiledData) VALUES (?,?,?)");

            int i = 1;
            for(Field field : Cache.class.getFields()) {
                Type type = field.getType();
                try {
                    if(type == String.class) {
                        statement.setString(i, field.get(cache).toString());
                        i++;
                    } if(type == int.class) {
                        statement.setInt(i, (int)field.get(cache));
                        i++;
                    } else if(type == byte[].class) {
                        statement.setBytes(i, (byte[])field.get(cache));
                        i++;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if(i >= 4) {
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

    public void delete(String type) {
        try {
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement statement = conn.prepareStatement("DELETE FROM Cache WHERE type = ?");
            statement.setString(1, type);

            statement.close();
            conn.close();
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }

    public void clear() {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement statement = conn.createStatement();
            statement.executeUpdate("DELETE FROM Cache");

            statement.close();
            conn.close();
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }
}
