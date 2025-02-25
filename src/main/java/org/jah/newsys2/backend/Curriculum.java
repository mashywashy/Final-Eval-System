package org.jah.newsys2.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Curriculum {
    private Map<String, List<Subject>> semesterSubjects;
    private Map<String, Subject> allSubjects;

    public Curriculum() {
        this.semesterSubjects = new HashMap<>();
        this.allSubjects = new HashMap<>();
    }

    public void addSemesterSubjects(String semester, List<Subject> subjects) {
        semesterSubjects.put(semester, subjects);
        subjects.forEach(subject -> allSubjects.put(subject.getCode(), subject));
    }

    public List<Subject> getSemesterSubjects(String semester) {
        return semesterSubjects.get(semester);
    }

    public Subject getSubject(String code) {
        return allSubjects.get(code);
    }

    public List<Subject> getNextSemesterSubjects(String currentSemester) {
        // Simple logic to get next semester - you might want to enhance this
        String year = currentSemester.split("-")[0];
        String sem = currentSemester.split("-")[1];

        if (sem.equals("1")) {
            return semesterSubjects.get(year + "-2");
        } else {
            int nextYear = Integer.parseInt(year) + 1;
            return semesterSubjects.get(nextYear + "-1");
        }
    }
}