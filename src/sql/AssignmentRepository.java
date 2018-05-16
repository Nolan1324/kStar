package sql;

import structure.Assignment;
import structure.Class;
import utils.FmtUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentRepository {
    private static String url = "jdbc:sqlite:local.db";

    public AssignmentRepository() {
        try {
            Connection conn = DriverManager.getConnection(url);
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Assignments (" +
                    "key INTEGER PRIMARY KEY," +
                    "details TEXT," +
                    "dateDue INTEGER," +
                    "dateAssigned INTEGER," +
                    "assignmentName TEXT," +
                    "ptsPossible DOUBLE," +
                    "score DOUBLE," +
                    "pctScore decimal(5,1)," +
                    "scoredAs TEXT," +
                    "extraCredit INTEGER," +
                    "notGraded INTEGER," +
                    "comments TEXT)");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Assignment load(int key) {
        Assignment a = new Assignment();

        try {
            Connection conn = DriverManager.getConnection(url);

            PreparedStatement statement = conn.prepareStatement("SELECT * FROM Assignments WHERE key = ?");
            statement.setInt(1, key);
            ResultSet results = statement.executeQuery();

            a.details = results.getString("details");
            SimpleDateFormat fmt = new SimpleDateFormat("M/d/yy");
            try { a.dateDue = fmt.parse(results.getString("dateDue")); } catch (ParseException e) {a.dateDue = null;}
            try { a.dateAssigned = fmt.parse(results.getString("dateAssigned")); } catch (ParseException e) {a.dateAssigned = null;}
            a.assignmentName = results.getString("assignmentName");
            a.ptsPossible = results.getDouble("ptsPossible");
            a.score = results.getDouble("score");
            a.pctScore = results.getDouble("pctScore");
            a.scoredAs = results.getString("scoredAs");
            a.extraCredit = results.getBoolean("extraCredit");
            a.notGraded = results.getBoolean("notGraded");
            a.comments = results.getString("comments");

            statement.close();
            results.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return a;
    }

    public void save(List<Assignment> assignments) {
        for(Assignment a : assignments) {
            try {
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement("REPLACE INTO Assignments (key,details,dateDue,dateAssigned,assignmentName,ptsPossible,score,pctScore,scoredAs,extraCredit,notGraded,comments) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
                statement.setInt(1, a.hashCode());

                int i = 2;
                for(Field field : Assignment.class.getFields()) {
                    Type type = field.getType();
                    try {
                        if(type == java.util.Date.class) {
                            statement.setString(i, FmtUtil.fmt((Date)field.get(a)));
                            i++;
                        } else if(type == String.class) {
                            statement.setString(i, field.get(a).toString());
                            i++;
                        } else if(type == int.class) {
                            statement.setInt(i, (int)field.get(a));
                            i++;
                        } else if(type == double.class) {
                            statement.setDouble(i, (double)field.get(a));
                            i++;
                        } else if(type == boolean.class) {
                            statement.setBoolean(i, (boolean)field.get(a));
                            i++;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    if(i >= 13) {
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

    public void clear() {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement statement = conn.createStatement();
            statement.executeUpdate("DELETE FROM Assignments");

            statement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
