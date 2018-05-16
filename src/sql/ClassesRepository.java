package sql;

import com.sun.security.auth.NTSidDomainPrincipal;
import structure.Assignment;
import structure.Class;
import structure.GradingScales;
import structure.Settings;

import java.lang.reflect.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;

public class ClassesRepository {
    private static String url = "jdbc:sqlite:local.db";

    public ClassesRepository() {
        try {
            Connection conn = DriverManager.getConnection(url);
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Classes (" +
                    "per INTEGER PRIMARY KEY," +
                    "name VARCHAR(50)," +
                    "code INTEGER," +
                    "teacher VARCHAR(30)," +
                    "teacherImg BLOB," +
                    "term VARCHAR(3)," +
                    "grade VARCHAR(2)," +
                    "percent decimal(5,1)," +
                    "scaleUrl TEXT," +
                    "assignments BLOB)");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Class> load() {
        Map<Integer, Class> classes = new HashMap<>();

        try {
            Connection conn = DriverManager.getConnection(url);

            Statement loadStatement = conn.createStatement();
            ResultSet results = loadStatement.executeQuery("SELECT * FROM Classes");

            if(results.next()) {
                do {
                    Class c = new Class();
                    c.per = results.getInt("per");
                    c.name = results.getString("name");
                    c.code = results.getInt("code");
                    c.teacher = new Class.Teacher(results.getString("teacher"));
                    c.teacherImg = results.getString("teacherImg");
                    c.term = Class.Term.fromCode(results.getString("term"));
                    c.grade = results.getString("grade");
                    c.percent = results.getDouble("percent");
                    c.scaleUrl = results.getString("scaleUrl");

                    AssignmentRepository assignmentRepository = new AssignmentRepository();
                    for(String assignmentKey : results.getString("assignments").split(" ")) {
                        c.assignments.add(assignmentRepository.load(Integer.parseInt(assignmentKey)));
                    }

                    classes.put(c.per, c);
                } while (results.next());
            }

            loadStatement.close();
            results.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return classes;
    }

    public void save(Map<Integer, Class> classes) {
        for(Class c : classes.values()) {
            try {
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement("REPLACE INTO Classes (per,name,code,teacher,teacherImg,term,grade,percent,scaleUrl,assignments) VALUES (?,?,?,?,?,?,?,?,?,?)");

                int i = 1;
                for(Field field : Class.class.getFields()) {
                    Type type = field.getType();
                    try {
                        if(type == String.class || type == Class.Teacher.class || type == Class.Term.class) {
                            statement.setString(i, field.get(c).toString());
                            i++;
                        } else if(type == int.class) {
                            statement.setInt(i, (int)field.get(c));
                            i++;
                        } else if(type == double.class) {
                            statement.setDouble(i, (double)field.get(c));
                            i++;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    if(i >= 10) {
                        break;
                    }
                }

                StringJoiner assignmentHashes = new StringJoiner(" ");
                for(Assignment assignment : c.assignments) {
                    assignmentHashes.add(Integer.toString(assignment.hashCode()));
                }
                statement.setString(10, assignmentHashes.toString());
                new AssignmentRepository().save(c.assignments);

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
            statement.executeUpdate("DELETE FROM Classes");

            statement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
