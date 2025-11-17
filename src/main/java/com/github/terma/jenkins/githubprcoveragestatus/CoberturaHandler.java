package com.github.terma.jenkins.githubprcoveragestatus;

import hudson.FilePath;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static Map<String, Coverage> parse(FilePath xml) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        saxParser.getXMLReader().setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        CoberturaHandler handler = new CoberturaHandler();
        saxParser.parse(xml.read(), handler);
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

    public static float coverageDelta(Map<String, Coverage> coverage, Map<String, Coverage> coverageOrig, Map<String, Coverage> masterCoverage, Map<String, Coverage> masterCoverageOrig, PrintStream log){
        long totalLinesOrig = coverageOrig.values()
                .stream()
                .flatMap(c -> c.lines.values().stream())
                .mapToLong(Long::parseLong)
                .filter(h -> h >= 0)
                .count();
        log.println("PR total lines: " + totalLinesOrig);
        long totalLinesMasterOrig = masterCoverageOrig.values()
                .stream()
                .flatMap(c -> c.lines.values().stream())
                .mapToLong(Long::parseLong)
                .filter(h -> h >= 0)
                .count();
        log.println("Master total lines: " + totalLinesMasterOrig);
        long coveredLines = coverage.values()
                .stream()
                .flatMap(c -> c.lines.values().stream())
                .mapToLong(Long::parseLong)
                .filter(h -> h > 0)
                .count();
        log.println("PR covered lines: " + coveredLines);
        long coveredLinesMaster = masterCoverage.values()
                .stream()
                .flatMap(c -> c.lines.values().stream())
                .mapToLong(Long::parseLong)
                .filter(h -> h > 0)
                .count();
        log.println("Master covered lines: " + coveredLinesMaster);


        float cov = (float) coveredLines /totalLinesOrig;
        log.println("PR coverage: " + Percent.toString(cov));
        float masterCov = (float) coveredLinesMaster /totalLinesMasterOrig;
        log.println("Master coverage: " + Percent.toString(masterCov));
        return Percent.change(cov, masterCov);
    }

    public static Map<String, Coverage> filter(Map<String, Coverage> coverage, List<String> exclude){
        return coverage.entrySet()
                .stream()
                .filter(e -> !isExcluded(e.getValue().file, exclude))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
    private static boolean isExcluded(String file, List<String> exclude) {
        return exclude.stream().anyMatch(file::endsWith);
    }
}
