package org.jah.newsys2.backend;//TIP To <b>Run</b> code, press <shortcut actionId="Run"/>
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        int totalUnits = 0;
        StudentEval se = new StudentEval("src/curr2.xml");
        Map<String, Boolean> subMap = new HashMap<>();

        // Adding 2nd year 2nd sem subjects with random status
        subMap.put("sts101", false);
        subMap.put("philo101", false);
        subMap.put("cc-quameth22", false);
        subMap.put("it-platech22", true);
        subMap.put("cc-appsdev22", true);
        subMap.put("cc-datastruc22", true);
        subMap.put("cc-datacom22", true);
        subMap.put("pe104", true);

        List<Subject> subs = se.getRecommendedSubjects(subMap);
        for(Subject sub : subs) {
            System.out.println(sub.getCode() + " " + sub.getUnits());
            totalUnits += sub.getUnits();
        }

        System.out.println("Total Units: " + totalUnits);
    }
}
