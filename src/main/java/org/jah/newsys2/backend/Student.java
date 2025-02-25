package org.jah.newsys2.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Student {
    private String studentId;
    private String programCode;
    private boolean isNew;
    private Map<String, Subject> takenSubjects;
    private List<String> currentSubjects;

    public Student(String studentId, String programCode, boolean isNew) {
        this.studentId = studentId;
        this.programCode = programCode;
        this.isNew = isNew;
        this.takenSubjects = new HashMap<>();
        this.currentSubjects = new ArrayList<>();
    }

    public boolean isNew() {
        return isNew;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void addTakenSubject(Subject subject) {
        takenSubjects.put(subject.getCode(), subject);
    }

    public void setCurrentSubjects(List<String> subjects) {
        this.currentSubjects = subjects;
    }

    public List<String> getCurrentSubjects() {
        return currentSubjects;
    }

    public Map<String, Subject> getTakenSubjects() {
        return takenSubjects;
    }
}