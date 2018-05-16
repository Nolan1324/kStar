package structure;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import pdf.Pdf;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class GradingScales {
    public Map<Integer, GradingScale> gradingScales = new HashMap<>();

    public GradingScales(Map<Integer, Class> classes) {
        for(Class c : classes.values()) {
            if(!c.scaleUrl.equals("")) {
                try {
                    HttpResponse<InputStream> reportResponse = Unirest.get(c.scaleUrl).asBinary();
                    String scaleText = Pdf.getGradingScale(reportResponse.getBody());
                    if(!scaleText.equals("")) {
                        gradingScales.put(c.per, new GradingScale(scaleText));
                    }
                } catch (UnirestException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class GradingScale {
        Map<Double, String> grades = new TreeMap<>(Collections.reverseOrder());

        GradingScale(String scale) {
            for(String gradeMap : scale.split("\r\n")) {
                String[] gradeMapArr = gradeMap.split(" ");
                grades.put(Double.parseDouble(gradeMapArr[1]), gradeMapArr[0]);
            }
        }
    }
}
