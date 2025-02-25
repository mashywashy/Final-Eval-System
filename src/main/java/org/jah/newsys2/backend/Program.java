package org.jah.newsys2.backend;

public class Program {
    private String programCode;
    private String name;
    private Curriculum curriculum;

    public Program(String programCode, String name, Curriculum curriculum) {
        this.programCode = programCode;
        this.name = name;
        this.curriculum = curriculum;
    }

    public String getProgramCode() {
        return programCode;
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }
}