package com.github.terma.jenkins.githubprcoveragestatus;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class ReportCoverageAction implements RunAction2 {

    private final String text;
    private transient Run run;

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
        return "coverage-details";
    }
    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Run getRun() {
        return run;
    }
}