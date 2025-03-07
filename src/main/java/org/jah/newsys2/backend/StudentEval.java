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

    // For continuing students - FIXED version
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

        // Determine the student's current academic standing (year and semester)
        Pair<String, String> nextSemester = determineNextSemester(passedSubjects);
        String nextYear = nextSemester.getFirst();
        String nextSem = nextSemester.getSecond();

        // Get ALL subjects from the next semester that the student hasn't taken yet
        List<Subject> nextSemesterSubjects = allSubjects.stream()
                .filter(subject -> nextYear.equals(subject.getYear()) && nextSem.equals(subject.getSemester()))
                .filter(subject -> !takenSubjects.contains(subject.getCode()))
                .filter(subject -> hasPassedAllPrerequisites(subject, recentSubjects))
                .collect(Collectors.toList());

        // Sort by subject code to maintain organization
        nextSemesterSubjects.sort(Comparator.comparing(Subject::getCode));

        // Add next semester subjects up to the unit limit
        for (Subject subject : nextSemesterSubjects) {
            // Skip if already recommended
            if (recommendations.contains(subject)) {
                continue;
            }

            // Check if unit limit is not exceeded
            if (totalUnits + subject.getUnits() <= MAX_UNITS) {
                recommendations.add(subject);
                totalUnits += subject.getUnits();
            }
        }

        // If we're still below the minimum units, look for additional subjects from the same year but different semester
        if (totalUnits < MIN_UNITS) {
            String otherSem = nextSem.equals("1") ? "2" : "1";

            List<Subject> sameYearOtherSemSubjects = allSubjects.stream()
                    .filter(subject -> nextYear.equals(subject.getYear()) && otherSem.equals(subject.getSemester()))
                    .filter(subject -> !takenSubjects.contains(subject.getCode()))
                    .filter(subject -> !recommendations.contains(subject))
                    .filter(subject -> hasPassedAllPrerequisites(subject, recentSubjects))
                    .collect(Collectors.toList());

            // Sort them to prioritize core subjects
            sameYearOtherSemSubjects.sort(Comparator.comparing(Subject::getCode));

            // Add subjects until minimum units or no more subjects available
            for (Subject subject : sameYearOtherSemSubjects) {
                if (totalUnits + subject.getUnits() <= MAX_UNITS) {
                    recommendations.add(subject);
                    totalUnits += subject.getUnits();

                    if (totalUnits >= MIN_UNITS) {
                        break;
                    }
                }
            }
        }

        // If still underloaded, add electives
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

        return recommendations;
    }

    // NEW: Determine the next semester the student should take
    private Pair<String, String> determineNextSemester(Set<String> passedSubjects) {
        // Map to count subjects passed in each year and semester
        Map<String, Map<String, Integer>> yearSemCounts = new HashMap<>();

        // Count passed subjects by year and semester
        for (String code : passedSubjects) {
            Subject subject = findSubjectByCode(code);
            if (subject == null) continue;

            String year = subject.getYear();
            String sem = subject.getSemester();

            yearSemCounts.putIfAbsent(year, new HashMap<>());
            Map<String, Integer> semCounts = yearSemCounts.get(year);
            semCounts.put(sem, semCounts.getOrDefault(sem, 0) + 1);
        }

        // Calculate the total subjects in each semester in the curriculum
        Map<String, Map<String, Integer>> totalSubjectsBySemester = new HashMap<>();
        for (Subject subject : allSubjects) {
            String year = subject.getYear();
            String sem = subject.getSemester();

            totalSubjectsBySemester.putIfAbsent(year, new HashMap<>());
            Map<String, Integer> semCounts = totalSubjectsBySemester.get(year);
            semCounts.put(sem, semCounts.getOrDefault(sem, 0) + 1);
        }

        // Find the latest year and semester where student completed most subjects
        String currentYear = "1";
        String currentSem = "1";

        for (String year : new String[]{"1", "2", "3", "4"}) {
            if (!yearSemCounts.containsKey(year)) continue;

            for (String sem : new String[]{"1", "2"}) {
                int passedCount = yearSemCounts.getOrDefault(year, Collections.emptyMap())
                        .getOrDefault(sem, 0);
                int totalCount = totalSubjectsBySemester.getOrDefault(year, Collections.emptyMap())
                        .getOrDefault(sem, 0);

                // If student has passed at least 60% of the subjects in this semester,
                // consider this as their current semester
                if (passedCount > 0 && totalCount > 0 &&
                        ((double) passedCount / totalCount) >= 0.6) {
                    currentYear = year;
                    currentSem = sem;
                }
            }
        }

        // Determine next semester
        String nextYear = currentYear;
        String nextSem = currentSem;

        if (currentSem.equals("1")) {
            nextSem = "2";  // Move to second semester of the same year
        } else {
            // Move to first semester of the next year
            nextSem = "1";
            nextYear = String.valueOf(Integer.parseInt(currentYear) + 1);

            // Don't go beyond 4th year
            if (Integer.parseInt(nextYear) > 4) {
                nextYear = "4";
                nextSem = "2";
            }
        }

        return new Pair<>(nextYear, nextSem);
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

    // Simple Pair class for returning year and semester together
    private static class Pair<F, S> {
        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }
}