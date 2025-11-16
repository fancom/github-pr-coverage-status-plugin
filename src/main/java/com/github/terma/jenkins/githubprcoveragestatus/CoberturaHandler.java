package com.github.terma.jenkins.githubprcoveragestatus;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CoberturaHandler extends DefaultHandler {
    private Map<String, Coverage> coverageDetails = new HashMap<>();
    private Coverage current;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("class")) {
            current = new Coverage();
            current.file = attributes.getValue("filename");
            current.lineRate = attributes.getValue("line-rate");
        }  else if (qName.equals("line")) {
            current.lines.put( attributes.getValue("number"), attributes.getValue("hits"));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("class")) {
            coverageDetails.put(current.file, current);
        }
    }

    public Map<String, Coverage> getCoverageDetails() {
        return coverageDetails;
    }

    public static Map<String, Coverage> parse(InputStream xml) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        saxParser.getXMLReader().setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        CoberturaHandler handler = new CoberturaHandler();
        saxParser.parse(xml, handler);
        return handler.getCoverageDetails();
    }
    public static float coverage (Map<String, Coverage> coverage){
        long totalLines = coverage.values()
                .stream()
                .mapToLong(c -> c.lines.size())
                .sum();

        long coveredLines = coverage.values()
                .stream()
                .flatMap(c -> c.lines.values().stream())
                .mapToLong(Long::parseLong)
                .filter(h -> h > 0)
                .count();

        if (totalLines == 0) return 0.0f;
        return (float) coveredLines / totalLines;
    }
}
