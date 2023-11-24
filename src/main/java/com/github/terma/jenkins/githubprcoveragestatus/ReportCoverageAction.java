package com.github.terma.jenkins.githubprcoveragestatus;
import hudson.model.Action;

public class ReportCoverageAction implements Action {

    private final String text;

    public ReportCoverageAction(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String getDisplayName() {
        return "Coverage comparison";
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getUrlName() {
        return "report-coverage-action";
    }
}