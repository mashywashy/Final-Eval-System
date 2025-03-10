package org.jah.newsys2.backend;

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
        subMap.put("it-el1", true);
        subMap.put("it-fre1", true);
        subMap.put("it-fre2", true);

        List<Subject> subs = se.getRecommendedSubjects(subMap, 3, 1);
        for(Subject sub : subs) {
            System.out.println(sub.getCode() + " " + sub.getUnits());
            totalUnits += sub.getUnits();
        }

        System.out.println("Total Units: " + totalUnits);
    }
}
