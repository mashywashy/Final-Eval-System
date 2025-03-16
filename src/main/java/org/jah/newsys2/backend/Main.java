package org.jah.newsys2.backend;

import java.util.*;


public class Main {
    public static void main(String[] args) {
        StudentEval facade = new StudentEval("bsit");
        int totalUnits = 0;

        Map<String, Boolean> subMap = new HashMap<>();
        subMap.put("eng101", true);  // Passed
        subMap.put("socio102", true); // Passed
        subMap.put("math101", true);  // Passed
        subMap.put("psych101", true); // Passed
        subMap.put("cc-comprog12", true); // Passed
        subMap.put("cc-discret12", true); // Passed
        subMap.put("cc-ooprog21", true); // Passed
        subMap.put("it-sad21", true); // Passed
        RecommendAI rAi = new RecommendAI();
        String res = rAi.recommendAI(facade.getRecommendedSubjects(subMap, 2, 2), "BSIT");

        System.out.println(res);


    }
}