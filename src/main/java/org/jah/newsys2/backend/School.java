package org.jah.newsys2.backend;

import java.util.ArrayList;
import java.util.List;

public class School {
    private String name;
    private List<Program> programs;

    public School(String name) {
        this.name = name;
        this.programs = new ArrayList<>();
    }

    public void addProgram(Program program) {
        programs.add(program);
    }

    public Program getProgram(String programCode) {
        return programs.stream()
                .filter(p -> p.getProgramCode().equals(programCode))
                .findFirst()
                .orElse(null);
    }
}