package structure;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jsoup.nodes.Element;
import pdf.Pdf;
import utils.FmtUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Class {

    public int per;
    public String name;
    public int code;
    public Teacher teacher;
    public String teacherImg;
    public Term term;
    public String grade;
    public double percent;
    public String scaleUrl = "";
    public List<Assignment> assignments = new ArrayList<Assignment>();

    public int newItems;

    public Class(Element element) {
        this.per = Integer.parseInt(element.select("[id~=classheaderinfo_.*]").first().childNode(1).toString());
        parseNameCode(element.select("[id~=classheaderinfo_.*]").first().child(1).text());
        this.teacher = new Teacher(element.getElementById("lblteacher").parent().parent().childNode(2).toString().trim());
        try {
            String gradeAndPercent = element.getElementById("lblgrade").parent().parent().childNode(2).toString().trim();
            parseGrade(gradeAndPercent);
            parsePercent(gradeAndPercent);
            this.term = Term.toTerm(element.getElementById("lblcurrent").parent().textNodes().get(0).text());
        } catch(Exception e) {
            //This happens when the class has no grade/semester data
            this.grade = "";
            this.percent = -1;
            e.printStackTrace();
        }
        if(!Files.exists(Paths.get("img/teacher_" + per))) {
            loadTeacherPicture();
        }

        Element thead = element.select("thead").first();
        loadGradingScale(thead);

        Element headersElement = thead.child(thead.children().size() - 1);
        for(Element assignmentElement : element.select("tbody").first().children()) {
            if(!assignmentElement.attr("style").contains("padding")) {
                assignments.add(new Assignment(headersElement, assignmentElement));
            } else {
                assignments.get(assignments.size() - 1).parseStandards(assignmentElement.child(1));
            }
        }
    }

    public Class() {
        //For creating empty classes when loading from SQL
    }

    public Element toElement() {
        Element li = new Element("li");
        Element headerTable = li.appendElement("div")
                .addClass("collapsible-header")
                .addClass("assign-header")
                .appendElement("table");
        headerTable.appendElement("tr")
                .appendElement("td")
                .text(String.format("Per: %d       %s (%d)", per, name, code))
                .attr("colspan", "3")
                .parent().appendElement("td")
                .text(String.format("Teacher: %s, %s", teacher.lastName, teacher.firstName))
                .attr("colspan", "4")
                .attr("class", "right-align");

        if(newItems != 0) {
            headerTable.child(0).child(0).appendElement("span").attr("class", "new badge").text(Integer.toString(newItems));
        }

        Element headerRow2 = headerTable.appendElement("tr");
        if(percent == -1) {
            headerRow2.appendElement("td")
                    .text(grade)
                    .attr("colspan", "3");
        } else {
            Element gradeElement = headerRow2.appendElement("td").attr("colspan", "3");
            gradeElement.append(MessageFormat.format("<span class=\"grade\" period=\"{0}\">{1}</span> (<span class=\"percent\" period=\"{0}\">{2}</span>%)",Integer.toString(per), grade, FmtUtil.fmt(percent)));
        }
        headerRow2.appendElement("td")
                .attr("colspan", "4")
                .attr("class", "right-align")
                .appendElement("img")
                .attr("src", teacherImg)
                .attr("onerror", "this.style.display='none'")
                .addClass("circle")
                .addClass("teacher");


        Element table = li.appendElement("div").addClass("collapsible-body").appendElement("table");
        Element thead = new Element("thead");
        Element tbody = new Element("tbody");
        table.appendChild(thead);
        table.appendChild(tbody);

        Element headers = thead.appendElement("tr").parent();
        headers.appendElement("td").text("Date Due");
        headers.appendElement("td").text("Assignment").addClass("width:1px;white-space:nowrap;");
        headers.appendElement("td").text("Points Possible").addClass("center-align");
        headers.appendElement("td").text("Score").addClass("center-align");
        headers.appendElement("td").text("Percent");
        headers.appendElement("td").text("Not Graded").addClass("center-align");
        headers.appendElement("td").text("Comments");

        for(Assignment assignment : assignments) {
            tbody.appendChild(assignment.toElement());
            if(!assignment.standards.isEmpty()) {
                for(Standard standard : assignment.standards) {
                    tbody.appendElement("tr");
                    tbody.appendChild(standard.toElement());
                }
            }
        }

        return li;
    }

    private boolean loadTeacherPicture() {
        try {
            HttpResponse<InputStream> picRaw = Unirest.get("https://www.bloomfield.org/uploaded/faculty/directory_images/small/sm-1314_" + teacher.id + ".jpg").asBinary();
            if(picRaw.getStatus() == 404) {
                HttpResponse<JsonNode> emailJson = Unirest.get("http://picasaweb.google.com/data/entry/api/user/" + teacher.email + "?alt=json").asJson();
                if(emailJson.getStatus() == 404) {
                    return false;
                }

                HttpResponse<InputStream> picRaw2 = Unirest.get(emailJson.getBody().getObject().getJSONObject("entry").getJSONObject("gphoto$thumbnail").getString("$t")).asBinary();
                teacherImg = emailJson.getBody().getObject().getJSONObject("entry").getJSONObject("gphoto$thumbnail").getString("$t");
                return true;
            }
            teacherImg = "https://www.bloomfield.org/uploaded/faculty/directory_images/small/sm-1314_" + teacher.id + ".jpg";
        } catch (UnirestException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void loadGradingScale(Element thead) {
        try {
            String onClick = thead.getElementsByTag("a").first().attr("onclick");
            Pattern pattern = Pattern.compile("\\((\\d+)\\)");
            Matcher matcher = pattern.matcher(onClick);
            if(matcher.find()) {
                scaleUrl = "https://webconnect.bloomfield.org/zangle/StudentPortal/Home/PrintProgressReport/" + matcher.group(1) + "%5E" + term;
            }
        } catch (Exception e) {
            //If it is missing, ignore it
        }
    }

    private void parseNameCode(String text) {
        Pattern term = Pattern.compile("(.+)\\s\\((\\d+)\\)");
        Matcher matcher = term.matcher(text);

        if(!matcher.find()) {
            throw new IllegalArgumentException("Text " + text + " does not contain a valid class/code combination.");
        }
        this.name = matcher.group(1);
        this.code = Integer.parseInt(matcher.group(2));
    }

    private void parseGrade(String text) {
        Pattern term = Pattern.compile("^Not Available|[A-H][+-]?");
        Matcher matcher = term.matcher(text);

        if(!matcher.find()) {
            throw new IllegalArgumentException("Text " + text + " does not contain a valid grade.");
        }
        this.grade = matcher.group(0);
    }

    private void parsePercent(String text) {
        Pattern term = Pattern.compile("\\((\\d{1,3}\\.\\d)%\\)");
        Matcher matcher = term.matcher(text);

        if(!matcher.find()) {
            this.percent = -1.0;
        } else {
            this.percent = Double.parseDouble(matcher.group(1));
        }
    }

    public enum Term {
        S1H("S1H"), S2H("S2H"), Q1H("Q1H"), Q2H("Q2H"), Q3H("Q3H"), Q4H("Q4H"), P1("P1"), P2("P2"), P3("P3"), P4("P4");

        String code;

        Term(String code) {
            this.code = code;
        }

        public static Term fromCode(String code) {
            for (Term t : Term.values()) {
                if (t.code.equals(code)) return t;
            }
            return null;
        }

        private static Term toTerm(String text) {
            Pattern term = Pattern.compile("([SQP])(?:\\w+ |\\w+ \\w+ )(\\d)(?: (\\w)|)");
            Matcher matcher = term.matcher(text);

            if(!matcher.find()) {
                throw new IllegalArgumentException("Text " + text + " does not contain a valid term.");
            }
            String code = matcher.group(1) + matcher.group(2) + ((matcher.group(3) != null) ? matcher.group(3) : "");
            for (Term t : Term.values()) {
                if (t.code.equals(code)) return t;
            }
            throw new IllegalArgumentException("No term with code " + code + " found.");
        }
    }

    public static class Teacher {
        public String text;

        public String firstName;
        public String lastName;
        public String id;
        public String email;

        public Teacher(String text) {
            this.text = text;

            String[] array = text.split(", ");
            firstName = array[1];
            lastName = array[0];
            id = firstName.charAt(0) + lastName;
            email = id + "@bloomfield.org";
        }

        public String toString() {
            return text;
        }
    }
}
