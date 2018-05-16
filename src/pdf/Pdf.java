package pdf;

import org.apache.pdfbox.*;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pdf {
    public static String getGradingScale(InputStream is) {
        PDFTextStripper pdfStripper = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        try {
            PDFParser parser = new PDFParser(new RandomAccessBufferedFileInputStream(is));
            parser.parse();
            cosDoc = parser.getDocument();
            pdfStripper = new PDFTextStripper();
            pdDoc = new PDDocument(cosDoc);
            String parsedText = pdfStripper.getText(pdDoc);

            Pattern pattern = Pattern.compile("Scale:\r\n((?:\\w[+-]? \\d+\\.\\d+\r\n)+)");
            Matcher matcher = pattern.matcher(parsedText);

            cosDoc.close();
            pdDoc.close();

            if(matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return "";
    }
}
