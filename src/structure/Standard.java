package structure;

import org.jsoup.nodes.Element;

import java.text.ParseException;

public class Standard {
    public String name;
    public int score;

    public Standard(Element standardElement) {
        name = standardElement.child(1).text();
        try {
            score = Integer.parseInt(standardElement.child(4).text());
        } catch(NumberFormatException e) {
            e.printStackTrace();
            score = -1;
        }
    }

    public Element toElement() {
        Element row = new Element("tr");

        row.appendElement("td")
                .attr("colspan", "1");

        row.appendElement("td")
                .addClass("standard")
                .attr("colspan", "6")
                .text(name + ": " + score);

        return row;
    }
}
