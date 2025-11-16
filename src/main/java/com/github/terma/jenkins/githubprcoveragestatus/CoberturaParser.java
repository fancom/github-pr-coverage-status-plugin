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

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <a href="http://cobertura.sourceforge.net/xml/coverage-04.dtd">Coverage DTD</a>
 */
class CoberturaParser implements CoverageReportParser {

    private static String findFirst(String string, String pattern) {
        String result = findFirstOrNull(string, pattern);
        if (result != null) {
            return result;
        } else {
            throw new IllegalArgumentException("Can't find " + pattern + " in " + string);
        }
    }

    private static String findFirstOrNull(String string, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(string);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    @Override
    public float get(String coberturaFilePath) {
        try {
            String content = FileUtils.readFileToString(new File(coberturaFilePath));
            float lineRate = Float.parseFloat(findFirst(content, "line-rate=['\"]([0-9.]+)['\"]"));
            float branchRate = Float.parseFloat(findFirst(content, "branch-rate=['\"]([0-9.]+)['\"]"));
            if (lineRate > 0 && branchRate == 0) {
              return lineRate;
            } else if (lineRate == 0 && branchRate > 0) {
              return branchRate;
            } else {
              return lineRate / 2 + branchRate / 2;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Coverage> getFull(String coberturaFilePath, List<String> exclude){
        try {
            InputStream stream = Files.newInputStream(Paths.get(coberturaFilePath));
            Map<String, Coverage> coverage = CoberturaHandler.parse(stream);
            return coverage.entrySet()
                    .stream()
                    .filter(e -> !isExcluded(e.getValue().file, exclude))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
        }
        catch (Exception ex){
            throw new RuntimeException(ex);
        }

    }

    private boolean isExcluded(String file, List<String> exclude) {
        return exclude.stream().anyMatch(file::endsWith);
    }

}
