package utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FmtUtil {
    public static String fmt(double d) {
        if(d == -1.0) {
            return "";
        } else if(d == (long) d)
            return String.format("%d",(long)d);
        else
            return String.format("%s",d);
    }

    public static String fmt(Date d) {
        try {
            DateFormat formatter = new SimpleDateFormat("M/d/yy");
            return formatter.format(d);
        } catch (Exception e) {
            return "";
        }
    }

    public static String fmtPct(double d) {
        String pctString = fmt(d);

        if(pctString.equals("")) {
            return "";
        } else
            return pctString + "%";
    }

    public static String fmtGrade(String grade, double percent) {
        if(percent == -1) {
            return grade;
        } else {
            return grade + " (" + fmt(percent) + "%) ";
        }
    }
}
