package com.github.terma.jenkins.githubprcoveragestatus;

import java.util.HashMap;
import java.util.Map;

public class Coverage {
    public String file;
    public String lineRate;
    public Map<String,String> lines;
    public  Coverage(){
        lines = new HashMap<>();
    }
}
