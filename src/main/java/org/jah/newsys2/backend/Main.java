package org.jah.newsys2.backend;//TIP To <b>Run</b> code, press <shortcut actionId="Run"/>
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        int totalUnits = 0;
        StudentEval se = new StudentEval("bsit");
        Map<String, Boolean> subMap = new HashMap<>();

        subMap.put("it-imdbsys31", true);
        subMap.put("it-network31", true);
        subMap.put("it-testqua31", true);
        subMap.put("cc-hci31", true);
        subMap.put("cc-rescom31", true);
        subMap.put("it-el", true);
        subMap.put("it-fre", true);

        List<Subject> subs = se.getRecommendedSubjects(subMap);
        for(Subject sub : subs) {
            System.out.println(sub.getCode() + " " + sub.getUnits());
            totalUnits += sub.getUnits();
        }

        System.out.println("Total Units: " + totalUnits);
    }
}
