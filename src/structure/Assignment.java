package structure;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import utils.FmtUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assignment {

    public String details = "";
    public Date dateDue;
    public Date dateAssigned;
    public String assignmentName = "";
    public double ptsPossible = -1.0;
    public double score = -1.0;
    public double pctScore = -1.0;
    public String scoredAs = "";
    public boolean extraCredit;
    public boolean notGraded;
    public String comments = "";

    List<Standard> standards = new ArrayList<>();

    public Assignment(Element headersElement, Element assignmentElement) {
        List<String> headers = headersElement.select("label").eachAttr("for");

        Map<String, ParseAction> parseActions = new HashMap<>();
        parseActions.put("detail", this::parseDetail);
        parseActions.put("datedue", this::parseDateDue);
        parseActions.put("dateassigned", this::parseDateAssigned);
        parseActions.put("assignment", this::parseAssignmentName);
        parseActions.put("ptspossible", this::parsePtsPossible);
        parseActions.put("grade", this::parseGrade);
        parseActions.put("score", this::parseScore);
        parseActions.put("pctscore", this::parsePctScore);
        parseActions.put("scoredas", this::parseScoredAs);
        parseActions.put("extracredit", this::parseExtraCredit);
        parseActions.put("notyetgraded", this::parseNotGraded);
        parseActions.put("comments", this::parseComments);

        for(int i = 0; i < assignmentElement.children().size(); i++) {
            if(parseActions.containsKey(headers.get(i))) {
                parseActions.get(headers.get(i)).parse(assignmentElement.child(i));
            }
        }

        if(ptsPossible != -1 && score != -1 && pctScore == -1) {
            pctScore = ((double) Math.round(score / ptsPossible * 1000)) / 10;
        }
    }

    public Assignment() {

    }

    public Element toElement() {
        Element row = new Element("tr");

        row.appendElement("td").text(FmtUtil.fmt(dateDue));
        row.appendElement("td").text(assignmentName).addClass("width:1px;white-space:nowrap;");
        row.appendElement("td").text(FmtUtil.fmt(ptsPossible)).addClass("center-align").attr("contenteditable", true).addClass("editable ptsPossible");
        row.appendElement("td").text(FmtUtil.fmt(score)).addClass("center-align").attr("contenteditable", true).addClass("editable score");
        row.appendElement("td").text(FmtUtil.fmtPct(pctScore));
        if(notGraded) {
            row.appendElement("td").addClass("center-align").append("<i class=\"material-icons\">check</i>");
        } else {
            row.appendElement("td").addClass("center-align");
        }
        row.appendElement("td").text(comments).attr("contenteditable", true);

        return row;
    }

    void parseStandards(Element standardsElement) {
        for(Element standardElement : standardsElement.getElementsByTag("tr")) {
            standards.add(new Standard(standardElement));
        }
    }

    private void parseDetail(Element valueElement) {
        if(valueElement.getElementsByTag("a").size() != 0) {
            Pattern pattern = Pattern.compile("\'(\\d+)\'");
            Matcher matcher = pattern.matcher(valueElement.child(0).attr("href"));
            if(matcher.find()) {
                try {
                    HttpResponse<String> detailsR = Unirest.get("https://webconnect.bloomfield.org/zangle/StudentPortal/Home/AssignmentDetails/" + matcher.group(1)).asString();
                    details = Jsoup.parse(detailsR.getBody()).getElementsByTag("tr").get(1).text();
                } catch (UnirestException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseDateDue(Element valueElement) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("M/d/yy");
            dateDue = parser.parse(valueElement.text());
        } catch (ParseException e) {
            dateDue = null;
        }
    }

    private void parseDateAssigned(Element valueElement) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("M/d/yy");
            dateAssigned = parser.parse(valueElement.text());
        } catch (ParseException e) {
            dateAssigned = null;
        }
    }

    private void parseAssignmentName(Element valueElement) {
        assignmentName = valueElement.text();
    }

    private void parsePtsPossible(Element valueElement) {
        try {
            ptsPossible = Double.parseDouble(valueElement.text());
        } catch (NumberFormatException e) {
            ptsPossible = -1.0;
        }
    }

    private void parseGrade(Element valueElement) {
        //Do nothing. This is a letter grade.
    }

    private void parseScore(Element valueElement) {
        try {
            score = Double.parseDouble(valueElement.text());
        } catch (NumberFormatException e) {
            score = -1.0;
        }
    }

    private void parsePctScore(Element valueElement) {
        try {
            pctScore = Double.parseDouble(valueElement.text());
        } catch (NumberFormatException e) {
            pctScore = -1.0;
        }
    }

    private void parseScoredAs(Element valueElement) {
        scoredAs = valueElement.text();
    }

    private void parseExtraCredit(Element valueElement) {
        if(valueElement.childNodeSize() != 0) {
            extraCredit = true;
        }
    }

    private void parseNotGraded(Element valueElement) {
        if(valueElement.children().size() != 0) {
            notGraded = true;
        }
    }

    private void parseComments(Element valueElement) {
        comments = valueElement.text();
    }


    interface ParseAction {
        void parse(Element valueElement);
    }
}
