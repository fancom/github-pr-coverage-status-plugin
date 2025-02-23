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

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
class Message {

    //see http://shields.io/ for reference
    private static final String BADGE_TEMPLATE = "https://img.shields.io/badge/coverage-%s-%s.svg";

    private static final String COLOR_RED = "red";
    private static final String COLOR_YELLOW = "yellow";
    private static final String COLOR_GREEN = "brightgreen";

    private final float coverage;
    private final float masterCoverage;
    private Map<String, String> coverageResult;

    public Message(float coverage, float masterCoverage, Map<String, String> coverageResult) {
        this.coverage = Percent.roundFourAfterDigit(coverage);
        this.masterCoverage = Percent.roundFourAfterDigit(masterCoverage);
        this.coverageResult = coverageResult;
    }
    public Message(float coverage, float masterCoverage) {
        this.coverage = Percent.roundFourAfterDigit(coverage);
        this.masterCoverage = Percent.roundFourAfterDigit(masterCoverage);
        this.coverageResult = new HashMap<>();
    }

    public String forConsole() {
        return String.format("Coverage %s changed %s vs master %s",
                Percent.toWholeNoSignString(coverage),
                Percent.toString(Percent.change(coverage, masterCoverage)),
                Percent.toWholeNoSignString(masterCoverage));
    }

    public String forComment(
            final String buildUrl, final String jenkinsUrl,
            final int yellowThreshold, final int greenThreshold,
            final boolean useShieldsIo) {
        final String icon = forIcon();
        if (useShieldsIo) {
            return "[![" + icon + "](" + shieldIoUrl(icon, yellowThreshold, greenThreshold) + ")](" + buildUrl + ")";
        } else {
            return "[![" + icon + "](" + jenkinsUrl + "/coverage-status-icon/" +
                    "?coverage=" + coverage +
                    "&masterCoverage=" + masterCoverage +
                    ")](" + buildUrl + ")";
        }
    }

    public String forStatusCheck() {
        return  String.format("Coverage %s changed %s vs master %s",
                Percent.toWholeNoSignString(coverage),
                Percent.toString(Percent.change(coverage, masterCoverage)),
                Percent.toWholeNoSignString(masterCoverage));

    }

    public String forBuild() {
        StringBuilder message = new StringBuilder(String.format("Coverage %s changed %s vs master %s",
                Percent.toWholeNoSignString(coverage),
                Percent.toString(Percent.change(coverage, masterCoverage)),
                Percent.toWholeNoSignString(masterCoverage)));

        for (Map.Entry<String, String> entry : coverageResult.entrySet()) {
            message.append("<BR/>").append(entry.getKey()).append(": -").append(entry.getValue());
        }
        if (masterCoverage > coverage && coverageResult.entrySet().size() ==0){
            message.append("<BR/>").append("Failed to get the coverage reduce reason. Check coverage reports manually");
        }
        return message.toString();
    }

    private String shieldIoUrl(String icon, final int yellowThreshold, final int greenThreshold) {
        final String color = getColor(yellowThreshold, greenThreshold);
        // dash should be encoded as two dash
        icon = icon.replace("-", "--");
        try {
            return String.format(BADGE_TEMPLATE, URIUtil.encodePath(icon), color);
        } catch (URIException e) {
            throw new RuntimeException(e);
        }
    }

    private String getColor(int yellowThreshold, int greenThreshold) {
        String color = COLOR_GREEN;
        final int coveragePercent = Percent.of(coverage);
        if (coveragePercent < yellowThreshold) {
            color = COLOR_RED;
        } else if (coveragePercent < greenThreshold) {
            color = COLOR_YELLOW;
        }
        return color;
    }

    /**
     * Example: 92% (+23%) vs master 70%
     */
    public String forIcon() {
        return String.format("%s (%s) vs master %s",
                Percent.toWholeNoSignString(coverage),
                Percent.toString(Percent.change(coverage, masterCoverage)),
                Percent.toWholeNoSignString(masterCoverage));
    }
}
