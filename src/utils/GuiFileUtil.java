package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GuiFileUtil {

    public static byte[] getFile(String path) throws IOException {
        Path location = Paths.get("gui/" + path);

        return Files.readAllBytes(location);
    }

    public static boolean fileExists(String path) {
        return Files.exists(Paths.get("gui/" + path));
    }

    /*
    public static Document getXML(String path) {
        File fXmlFile = new File("gui/" + path);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = null;
        try {
            doc = dBuilder.parse(fXmlFile);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        doc.getDocumentElement().normalize();

        return doc;
    }

    public static String XMLToString(Document doc) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tf.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        try {
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return writer.toString();
    }


    public static Node getNodeById(Document doc, String id) throws NoSuchElementException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            NodeList nodes = (NodeList) xPath.compile("//*[@id='" + id + "']").evaluate(doc, XPathConstants.NODESET);
            if(nodes.getLength() == 0) {
                throw new NoSuchElementException("There are no elements in the document with that id.");
            } else {
                return nodes.item(0);
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return null;
        }
    }
    */
}
