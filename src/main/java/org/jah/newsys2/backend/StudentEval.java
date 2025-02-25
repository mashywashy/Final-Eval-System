package org.jah.newsys2.backend;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class StudentEval {
    private List<Subject> allSubjects;
    private static final int MAX_UNITS = 26;
    private static final int MIN_UNITS = 18; // Minimum recommended units to avoid underloading

    public StudentEval(String program) {
        switch (program.toLowerCase()) {
            case "bsit":
                this.allSubjects = parseCurriculum("src/main/resources/org/jah/newsys2/bsit_curriculum.xml");
                break;
            case "bsmt":
                this.allSubjects = parseCurriculum("src/main/resources/org/jah/newsys2/bsmt_curriculum.xml");
                break;
            case "bsn":
                this.allSubjects = parseCurriculum("src/main/resources/org/jah/newsys2/bsn_curriculum.xml");
                break;
            case "bsa":
                this.allSubjects = parseCurriculum("src/main/resources/org/jah/newsys2/bsa_curriculum.xml");
                break;
            default:
                System.out.println("Program does not exist!");
        }
    }

    // For new students - Only First Year, First Sem subjects
    public List<Subject> getRecommendedSubjects() {
        return allSubjects.stream()
                .filter(subject -> "1".equals(subject.getYear()) && "1".equals(subject.getSemester()))
                .collect(Collectors.toList());
    }

    // For continuing students
    // Updated getRecommendedSubjects method with same-semester subject handling for underload
    public List<Subject> getRecommendedSubjects(Map<String, Boolean> recentSubjects) {
        List<Subject> recommendations = new ArrayList<>();
        int totalUnits = 0;

        // First, add failed subjects that need to be retaken
        List<Subject> retakes = getRetakeSubjects(recentSubjects);
        for (Subject subject : retakes) {
            if (totalUnits + subject.getUnits() <= MAX_UNITS) {
                recommendations.add(subject);
                totalUnits += subject.getUnits();
            }
        }

        // Create a set of passed subjects for easy lookup
        Set<String> passedSubjects = recentSubjects.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Create a set of all subjects the student has taken (passed or failed)
        Set<String> takenSubjects = new HashSet<>(recentSubjects.keySet());

        // Find subjects that directly follow the ones the student has passed
        List<Subject> nextSubjects = findNextSubjects(passedSubjects);

        // Sort by year and semester to maintain proper curriculum sequence
        nextSubjects.sort(Comparator
                .comparing(Subject::getYear)
                .thenComparing(Subject::getSemester));

        // Add next appropriate subjects up to the unit limit
        for (Subject subject : nextSubjects) {
            // Skip if already recommended or already taken
            if (recommendations.contains(subject) || takenSubjects.contains(subject.getCode())) {
                continue;
            }

            // Check if prerequisites are met and unit limit is not exceeded
            if (hasPassedAllPrerequisites(subject, recentSubjects) &&
                    totalUnits + subject.getUnits() <= MAX_UNITS) {
                recommendations.add(subject);
                totalUnits += subject.getUnits();
            }
        }

        // If we're still below the minimum units, add electives and free electives
        if (totalUnits < MIN_UNITS) {
            List<Subject> electiveSubjects = findAvailableElectives(takenSubjects);
            electiveSubjects.sort(Comparator
                    .comparing(Subject::getYear)
                    .thenComparing(Subject::getSemester));

            for (Subject elective : electiveSubjects) {
                if (recommendations.contains(elective) || takenSubjects.contains(elective.getCode())) {
                    continue;
                }

                if (totalUnits + elective.getUnits() <= MAX_UNITS) {
                    recommendations.add(elective);
                    totalUnits += elective.getUnits();

                    // Break if we've reached minimum units
                    if (totalUnits >= MIN_UNITS) {
                        break;
                    }
                }
            }
        }

        // If still underloaded, try to add more subjects from the same semester
        if (totalUnits < MIN_UNITS) {
            // Determine the most common semester in our current recommendations
            Map<String, Integer> semesterCounts = new HashMap<>();
            Map<String, String> subjectYearSem = new HashMap<>();

            for (Subject subject : recommendations) {
                String yearSem = subject.getYear() + "-" + subject.getSemester();
                semesterCounts.put(yearSem, semesterCounts.getOrDefault(yearSem, 0) + 1);
                subjectYearSem.put(subject.getCode(), yearSem);
            }

            // Find the most common semester
            String mostCommonSemester = semesterCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");

            if (!mostCommonSemester.isEmpty()) {
                String[] parts = mostCommonSemester.split("-");
                String targetYear = parts[0];
                String targetSem = parts[1];

                // Find additional subjects from the same semester
                List<Subject> sameSemesterSubjects = allSubjects.stream()
                        .filter(subject -> targetYear.equals(subject.getYear()) &&
                                targetSem.equals(subject.getSemester()))
                        .filter(subject -> !recommendations.contains(subject) &&
                                !takenSubjects.contains(subject.getCode()))
                        .filter(subject -> hasPassedAllPrerequisites(subject, recentSubjects))
                        .collect(Collectors.toList());

                // Sort them to prioritize core subjects
                sameSemesterSubjects.sort((s1, s2) -> {
                    // Prioritize non-elective courses
                    boolean s1Elective = s1.getCode().equals("it-el") || s1.getCode().equals("it-fre");
                    boolean s2Elective = s2.getCode().equals("it-el") || s2.getCode().equals("it-fre");

                    if (s1Elective && !s2Elective) return 1;
                    if (!s1Elective && s2Elective) return -1;

                    // If both are same type, sort by code
                    return s1.getCode().compareTo(s2.getCode());
                });

                // Add subjects until minimum units or no more subjects available
                for (Subject subject : sameSemesterSubjects) {
                    if (totalUnits + subject.getUnits() <= MAX_UNITS) {
                        recommendations.add(subject);
                        totalUnits += subject.getUnits();

                        if (totalUnits >= MIN_UNITS) {
                            break;
                        }
                    }
                }
            }
        }

        return recommendations;
    }

    // Find subjects that directly follow passed subjects in the curriculum
    private List<Subject> findNextSubjects(Set<String> passedSubjects) {
        List<Subject> nextSubjects = new ArrayList<>();

        for (Subject subject : allSubjects) {
            // Skip subjects already passed
            if (passedSubjects.contains(subject.getCode())) {
                continue;
            }

            // Check if this subject has any prerequisites that are in the passed subjects list
            boolean isDirectFollow = !subject.getPrerequisites().isEmpty() &&
                    subject.getPrerequisites().stream().anyMatch(passedSubjects::contains);

            if (isDirectFollow) {
                nextSubjects.add(subject);
            }
        }

        return nextSubjects;
    }

    // Find available electives based on student's year level
    private List<Subject> findAvailableElectives(Set<String> takenSubjects) {
        List<Subject> electives = new ArrayList<>();

        // Determine student's year level based on taken subjects
        int yearLevel = determineStudentYearLevel(takenSubjects);

        for (Subject subject : allSubjects) {
            // Check if it's an elective (it-el or it-fre)
            boolean isElective = subject.getCode().equals("it-el") || subject.getCode().equals("it-fre");

            // Check if the elective is at or below the student's year level
            boolean isInYearRange = Integer.parseInt(subject.getYear()) <= yearLevel;

            if (isElective && isInYearRange && !takenSubjects.contains(subject.getCode())) {
                electives.add(subject);
            }
        }

        return electives;
    }

    // Determine student's year level based on courses taken
    private int determineStudentYearLevel(Set<String> takenSubjects) {
        int highestYear = 1; // Default to first year

        for (String code : takenSubjects) {
            Subject subject = findSubjectByCode(code);
            if (subject != null) {
                int year = Integer.parseInt(subject.getYear());
                highestYear = Math.max(highestYear, year);
            }
        }

        // For third and fourth year students, if they've completed most of the previous year's courses
        if (highestYear >= 2) {
            int prevYearCount = countSubjectsInYear(highestYear);
            int takenPrevYearCount = countTakenSubjectsInYear(takenSubjects, highestYear);

            // If they've taken more than 75% of the previous year's courses, consider them in the next year
            if (takenPrevYearCount >= prevYearCount * 0.75 && highestYear < 4) {
                highestYear++;
            }
        }

        return highestYear;
    }

    // Count total subjects in a specific year
    private int countSubjectsInYear(int year) {
        return (int) allSubjects.stream()
                .filter(s -> Integer.parseInt(s.getYear()) == year)
                .count();
    }

    // Count how many subjects from a specific year the student has taken
    private int countTakenSubjectsInYear(Set<String> takenSubjects, int year) {
        return (int) allSubjects.stream()
                .filter(s -> Integer.parseInt(s.getYear()) == year)
                .filter(s -> takenSubjects.contains(s.getCode()))
                .count();
    }

    private List<Subject> parseCurriculum(String xmlPath) {
        List<Subject> subjects = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(xmlPath));
            doc.getDocumentElement().normalize();

            // Parse curriculum by year and semester
            NodeList years = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < years.getLength(); i++) {
                Node yearNode = years.item(i);
                if (yearNode.getNodeType() != Node.ELEMENT_NODE) continue;
                String year = extractYear(yearNode.getNodeName());

                NodeList semesters = yearNode.getChildNodes();
                for (int j = 0; j < semesters.getLength(); j++) {
                    Node semNode = semesters.item(j);
                    if (semNode.getNodeType() != Node.ELEMENT_NODE) continue;
                    String semester = extractSemester(semNode.getNodeName());

                    NodeList subjectNodes = ((Element) semNode).getElementsByTagName("subject");
                    for (int k = 0; k < subjectNodes.getLength(); k++) {
                        Element subjectElem = (Element) subjectNodes.item(k);
                        String code = subjectElem.getAttribute("subjectCode");
                        int units = Integer.parseInt(subjectElem.getAttribute("units"));

                        Subject subject = new Subject(code, units, year, semester);

                        NodeList prereqs = subjectElem.getElementsByTagName("prerequisite");
                        for (int l = 0; l < prereqs.getLength(); l++) {
                            subject.addPrerequisite(prereqs.item(l).getTextContent());
                        }

                        subjects.add(subject);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing curriculum XML: " + e.getMessage());
        }
        return subjects;
    }

    public List<Subject> getAllSubjects() {
        return allSubjects;
    }

    private String extractYear(String nodeName) {
        if (nodeName.toLowerCase().contains("firstyear")) return "1";
        if (nodeName.toLowerCase().contains("secondyear")) return "2";
        if (nodeName.toLowerCase().contains("thirdyear")) return "3";
        if (nodeName.toLowerCase().contains("fourthyear")) return "4";
        return "";
    }

    private String extractSemester(String nodeName) {
        if (nodeName.toLowerCase().contains("firstsem")) return "1";
        if (nodeName.toLowerCase().contains("secondsem")) return "2";
        return "";
    }

    private List<Subject> getRetakeSubjects(Map<String, Boolean> recentSubjects) {
        return recentSubjects.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(entry -> findSubjectByCode(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean hasPassedAllPrerequisites(Subject subject, Map<String, Boolean> recentSubjects) {
        return subject.getPrerequisites().stream()
                .allMatch(prereq -> recentSubjects.getOrDefault(prereq, false));
    }

    private Subject findSubjectByCode(String code) {
        return allSubjects.stream()
                .filter(subject -> subject.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}