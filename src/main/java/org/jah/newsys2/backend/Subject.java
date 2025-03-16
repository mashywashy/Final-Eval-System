package org.jah.newsys2.backend;

import java.util.List;

public class Subject {
    private String code;
    private int units;
    private boolean isElective;
    private boolean isMajor;
    private List<String> prerequisites;

    public Subject(String code, int units, boolean isElective, boolean isMajor, List<String> prerequisites) {
        this.code = code;
        this.units = units;
        this.isElective = isElective;
        this.isMajor = isMajor;
        this.prerequisites = prerequisites;
    }

    public String getCode() {
        return code;
    }

    public int getUnits() {
        return units;
    }

    public boolean isElective() {
        return isElective;
    }

    public boolean isMajor() {
        return isMajor;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }
}