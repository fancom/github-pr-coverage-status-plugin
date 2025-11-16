package com.github.terma.jenkins.githubprcoveragestatus;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;

@Extension
public class CompareCoverageConfiguration extends GlobalConfiguration {

    private List<String> excludeFiles = new ArrayList<>();

    public CompareCoverageConfiguration() {
        load(); // load persisted values
    }

    public static CompareCoverageConfiguration get() {
        return GlobalConfiguration.all().get(CompareCoverageConfiguration.class);
    }

    public List<String> getExcludeFiles() {
        return excludeFiles;
    }

    @DataBoundSetter
    public void setExcludeFiles(List<String> excludeFiles) {
        this.excludeFiles = excludeFiles != null ? new ArrayList<>(excludeFiles) : new ArrayList<>();
        save();
    }
}
