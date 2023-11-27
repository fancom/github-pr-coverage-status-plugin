package com.github.terma.jenkins.githubprcoveragestatus;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
}
