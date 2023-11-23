/*

    Copyright 2015-2016 Artem Stasiuk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.github.terma.jenkins.githubprcoveragestatus;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.File;

/**
 * Build step to publish pull request

/**
 * Build step to publish pull request coverage status message to GitHub pull request.
 * <p>
 * Workflow:
 * <ul>
 * <li>find coverage of current build and assume it as pull request coverage</li>
 * <li>find master coverage for repository URL could be taken by {@link MasterCoverageAction} or Sonar {@link Configuration}</li>
 * <li>Publish nice status message to GitHub PR page</li>
 * </ul>
 *
 * @see MasterCoverageAction
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CompareCoverageAction extends Recorder implements SimpleBuildStep {

    public static final String BUILD_LOG_PREFIX = "[GitHub PR Status] ";

    private static final long serialVersionUID = 1L;
    private String sonarLogin;
    private String sonarPassword;
    private Map<String, String> scmVars;
    private String jacocoCoverageCounter;
    private String publishResultAs;
    private String testCoverage;
    private String devCoverage;

    @DataBoundConstructor
    public CompareCoverageAction() {
    }

    public String getPublishResultAs() {
        return publishResultAs;
    }

    @DataBoundSetter
    public void setPublishResultAs(String publishResultAs) {
        this.publishResultAs = publishResultAs;
    }

    public String getTestCoverage() {
        return testCoverage;
    }

    @DataBoundSetter
    public void setTestCoverage(String testCoverage) {
        this.testCoverage = testCoverage;
    }

    public String getDevCoverage() {
        return devCoverage;
    }

    @DataBoundSetter
    public void setDevCoverage(String devCoverage) {
        this.devCoverage = devCoverage;
    }

    public String getSonarLogin() {
        return sonarLogin;
    }

    @DataBoundSetter
    public void setSonarLogin(String sonarLogin) {
        this.sonarLogin = sonarLogin;
    }

    public String getSonarPassword() {
        return sonarPassword;
    }

    @DataBoundSetter
    public void setSonarPassword(String sonarPassword) {
        this.sonarPassword = sonarPassword;
    }

    // TODO why is this needed for no public field ‘scmVars’ (or getter method) found in class ....
    public Map<String, String> getScmVars() {
        return scmVars;
    }

    @DataBoundSetter
    public void setScmVars(Map<String, String> scmVars) {
        this.scmVars = scmVars;
    }

    public String getJacocoCoverageCounter() {
        return jacocoCoverageCounter;
    }

    @DataBoundSetter
    public void setJacocoCoverageCounter(String jacocoCoverageCounter) {
        this.jacocoCoverageCounter = jacocoCoverageCounter;
    }

    // todo show message that addition comment in progress as it could take a while
    @SuppressWarnings("NullableProblems")
    @Override
    public void perform(
            final Run build,
            final FilePath workspace,
            final Launcher launcher,
            final TaskListener listener
    ) throws InterruptedException, IOException {
        final PrintStream buildLog = listener.getLogger();

        if (build.getResult() != Result.SUCCESS) {
            buildLog.println(BUILD_LOG_PREFIX + "skip, build is red");
            return;
        }

        buildLog.println(BUILD_LOG_PREFIX + "start");

        final SettingsRepository settingsRepository = ServiceRegistry.getSettingsRepository();

        final int prId = PrIdAndUrlUtils.getPrId(scmVars, build, listener);
        final String gitUrl = PrIdAndUrlUtils.getGitUrl(scmVars, build, listener);

        buildLog.println(BUILD_LOG_PREFIX + "getting master coverage...");
        MasterCoverageRepository masterCoverageRepository = ServiceRegistry
                .getMasterCoverageRepository(buildLog, sonarLogin, sonarPassword);
        final GHRepository gitHubRepository = ServiceRegistry.getPullRequestRepository().getGitHubRepository(PrIdAndUrlUtils.getGitUrl(scmVars, build, listener, false));
        final float masterCoverage = masterCoverageRepository.get(gitUrl);
        buildLog.println(BUILD_LOG_PREFIX + "master coverage: " + masterCoverage);

        buildLog.println(BUILD_LOG_PREFIX + "collecting coverage...");
        final float coverage = ServiceRegistry.getCoverageRepository(settingsRepository.isDisableSimpleCov(),
                jacocoCoverageCounter).get(workspace);
        buildLog.println(BUILD_LOG_PREFIX + "build coverage: " + coverage);




        final String buildUrl = Utils.getBuildUrl(build, listener);

        String jenkinsUrl = settingsRepository.getJenkinsUrl();
        if (jenkinsUrl == null) jenkinsUrl = Utils.getJenkinsUrlFromBuildUrl(buildUrl);
        Map<String, String> coverageResult = new HashMap<>();
        if (Percent.roundFourAfterDigit(coverage) < Percent.roundFourAfterDigit(masterCoverage)) {
            try {
                coverageResult = getCoverageDetails(getDevCoverage(), getTestCoverage(), buildLog);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final Message message = new Message(coverage, masterCoverage, coverageResult);
        buildLog.println(BUILD_LOG_PREFIX + message.forConsole());
        if ("comment".equalsIgnoreCase(publishResultAs)) {
            buildLog.println(BUILD_LOG_PREFIX + "publishing result as comment");
            publishComment(message, buildUrl, jenkinsUrl, settingsRepository, gitHubRepository, prId, listener);
        } else {
            buildLog.println(BUILD_LOG_PREFIX + "publishing result as status check");
            publishStatusCheck(message, gitHubRepository, prId, masterCoverage, coverage, buildUrl, listener);
        }
    }

    private void publishComment(
            Message message,
            String buildUrl,
            String jenkinsUrl,
            SettingsRepository settingsRepository,
            GHRepository gitHubRepository,
            int prId,
            TaskListener listener
    ) {
        try {
            final String comment = message.forComment(
                    buildUrl,
                    jenkinsUrl,
                    settingsRepository.getYellowThreshold(),
                    settingsRepository.getGreenThreshold(),
                    settingsRepository.isPrivateJenkinsPublicGitHub());
            ServiceRegistry.getPullRequestRepository().comment(gitHubRepository, prId, comment);
        } catch (Exception ex) {
            PrintWriter pw = listener.error("Couldn't add comment to pull request #" + prId + "!");
            ex.printStackTrace(pw);
        }
    }

    private void publishStatusCheck(
            Message message,
            GHRepository gitHubRepository,
            int prId,
            float targetCoverage,
            float coverage,
            String buildUrl,
            TaskListener listener
    ) {
        try {
            listener.getLogger().println(
                BUILD_LOG_PREFIX +
                "[debug] Rounded PR coverage: " +
                String.valueOf(Percent.roundFourAfterDigit(coverage))
            );
            listener.getLogger().println(
                BUILD_LOG_PREFIX +
                "[debug] Rounded master coverage: " +
                String.valueOf(Percent.roundFourAfterDigit(targetCoverage))
            );
            listener.getLogger().println(
                BUILD_LOG_PREFIX +
                "[debug] Mark PR as failed? " +
                String.valueOf(Percent.roundFourAfterDigit(coverage) < Percent.roundFourAfterDigit(targetCoverage))
            );
            List<GHPullRequestCommitDetail> commits = gitHubRepository.getPullRequest(prId).listCommits().asList();
            ServiceRegistry.getPullRequestRepository().createCommitStatus(
                    gitHubRepository,
                    commits.get(commits.size() - 1).getSha(),
                    Percent.roundFourAfterDigit(coverage) < Percent.roundFourAfterDigit(targetCoverage) ? GHCommitState.FAILURE : GHCommitState.SUCCESS,
                    buildUrl,
                    message.forStatusCheck()
            );
        } catch (Exception e) {
            PrintWriter pw = listener.error("Couldn't add status check to pull request #" + prId + "!");
            e.printStackTrace(pw);
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        @NonNull
        public String getDisplayName() {
            return "Publish coverage to GitHub";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

    private static Map<String, String> getCoverageDetails(String dev, String test, PrintStream log) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Map<String, String> coverageDetails = new HashMap<>();
        File fileDev = new File(dev);
        File fileTest = new File(test);
        if (!fileDev.exists() || !fileTest.exists()){
            log.println("Coverage file(s) does not exists. failed to run comparison");
            return coverageDetails;
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        org.w3c.dom.Document docDev = dBuilder.parse(fileDev);
        Document docTest = dBuilder.parse(fileTest);

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        XPathExpression expr = xpath.compile("//packages/package/classes/class");
        NodeList nListTest = (NodeList) expr.evaluate(docTest, XPathConstants.NODESET);


        for (int temp = 0; temp < nListTest.getLength(); temp++) {
            Node nNode = nListTest.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String name = eElement.getAttribute("name");
                String lineRate = eElement.getAttribute("line-rate");

                XPathExpression exprDev = xpath.compile("//packages/package/classes/class[@name='" + name + "']");
                NodeList nListDevTemp = (NodeList) exprDev.evaluate(docDev, XPathConstants.NODESET);
                if (nListDevTemp.getLength() > 0) {
                    Node nNodeDev = nListDevTemp.item(0);
                    if (nNodeDev.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElementDev = (Element) nNodeDev;
                        String lineRateDev = eElementDev.getAttribute("line-rate");
                        if (!lineRate.equals(lineRateDev) && Float.parseFloat(lineRateDev) > Float.parseFloat(lineRate)) {
                            coverageDetails.put(eElement.getAttribute("filename"), String.format("%.4f", (Float.parseFloat(lineRateDev) - Float.parseFloat(lineRate)) * 100) + "%");
                        }
                    }
                }
            }
        }

        return coverageDetails;
    }

}
