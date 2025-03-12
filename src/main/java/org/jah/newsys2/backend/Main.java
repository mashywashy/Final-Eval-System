package org.jah.newsys2.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        int totalUnits = 0;
        StudentEval se = new StudentEval("bsit");
        Map<String, Boolean> subMap = new HashMap<>();

        subMap.put("eng101", true);
        subMap.put("socio102", true);
        subMap.put("math101", true);
        subMap.put("psych101", true);



        List<Subject> subs = se.getRecommendedSubjects(subMap, 2 , 2);
        for(Subject sub : subs) {
            System.out.println(sub.getCode() + " " + sub.getUnits());
            totalUnits += sub.getUnits();
        }

        System.out.println("Total Units: " + totalUnits);
    }
}
