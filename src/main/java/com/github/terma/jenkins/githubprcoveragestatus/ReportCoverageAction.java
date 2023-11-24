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
        return "Custom Build Action";
    }

    @Override
    public String getIconFileName() {
        return "/path/to/icon.png";
    }

    @Override
    public String getUrlName() {
        return "report-coverage-action";
    }
}