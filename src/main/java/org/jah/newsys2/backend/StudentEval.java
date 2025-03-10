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
    private static final int MIN_UNITS = 18;
    private static final int IDEAL_UNITS = 21; // Target for optimal academic load

    public StudentEval(String program) {
        this.allSubjects = loadCurriculum(program);
    }

    private List<Subject> loadCurriculum(String program) {
        String path;
        switch (program.toLowerCase()) {
            case "bsit":
                path = "src/main/resources/org/jah/newsys2/bsit_curriculum.xml";
                break;
            case "bsmt":
                path = "src/main/resources/org/jah/newsys2/bsmt_curriculum.xml";
                break;
            case "bsn":
                path = "src/main/resources/org/jah/newsys2/bsn_curriculum.xml";
                break;
            case "bsa":
                path = "src/main/resources/org/jah/newsys2/bsa_curriculum.xml";
                break;
            default:
                throw new IllegalArgumentException("Program does not exist: " + program);
        }
        return parseCurriculum(path);
    }

    // For freshmen students - Only return First Year, First Sem subjects
    public List<Subject> getRecommendedSubjects() {
        return allSubjects.stream()
                .filter(subject -> "1".equals(subject.getYear()) && "1".equals(subject.getSemester()))
                .collect(Collectors.toList());
    }

    // Primary recommendation method for continuing students, maintaining explicit control
    public List<Subject> getRecommendedSubjects(Map<String, Boolean> academicHistory, int currentYear, int currentSemester) {
        // Validate input parameters
        validateRecommendationInputs(academicHistory, currentYear, currentSemester);

        // Calculate the next semester (for recommendations)
        int nextYear = currentYear;
        int nextSemester = currentSemester + 1;
        if (nextSemester > 2) {
            nextYear++;
            nextSemester = 1;
        }

        // Cap at maximum curriculum year/semester
        if (nextYear > 4) {
            nextYear = 4;
            nextSemester = 2;
        }

        // Initialize recommendation context
        RecommendationContext context = new RecommendationContext(
                academicHistory, currentYear, currentSemester, nextYear, nextSemester);

        // Step 1: Add failed subjects that need to be retaken (highest priority)
        addRetakeSubjects(context);

        // Step 2: Add core subjects for the next semester
        addNextSemesterCoreSubjects(context);

        // Step 3: If needed, add subjects from future semesters that have prerequisites satisfied
        if (context.totalUnits < MIN_UNITS) {
            addAdvancedEligibleSubjects(context);
        }

        // Step 4: If still below minimum, look for any eligible subjects, even from previous semesters
        if (context.totalUnits < MIN_UNITS) {
            addAnyEligibleSubjects(context);
        }

        return context.recommendations;
    }

    private void validateRecommendationInputs(Map<String, Boolean> academicHistory, int currentYear, int currentSemester) {
        if (academicHistory == null) {
            throw new IllegalArgumentException("Academic history cannot be null");
        }

        if (currentYear < 1 || currentYear > 4) {
            throw new IllegalArgumentException("Current year must be between 1 and 4");
        }

        if (currentSemester < 1 || currentSemester > 2) {
            throw new IllegalArgumentException("Current semester must be either 1 or 2");
        }
    }

    // Context class to maintain state during recommendation process
    private class RecommendationContext {
        final Map<String, Boolean> academicHistory;
        final Set<String> passedSubjects;
        final Set<String> takenSubjects;
        final List<Subject> recommendations;
        final int currentYear;
        final int currentSemester;
        final int nextYear;
        final int nextSemester;
        int totalUnits;

        RecommendationContext(Map<String, Boolean> academicHistory,
                              int currentYear, int currentSemester,
                              int nextYear, int nextSemester) {
            this.academicHistory = new HashMap<>(academicHistory);
            this.passedSubjects = academicHistory.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            this.takenSubjects = new HashSet<>(academicHistory.keySet());
            this.recommendations = new ArrayList<>();
            this.currentYear = currentYear;
            this.currentSemester = currentSemester;
            this.nextYear = nextYear;
            this.nextSemester = nextSemester;
            this.totalUnits = 0;
        }
    }

    // Add failed subjects first as they're highest priority for retaking
    private void addRetakeSubjects(RecommendationContext context) {
        List<Subject> retakes = context.academicHistory.entrySet().stream()
                .filter(entry -> !entry.getValue()) // Failed subjects
                .map(entry -> findSubjectByCode(entry.getKey()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Subject::getUnits)) // Start with smaller subjects
                .collect(Collectors.toList());

        for (Subject subject : retakes) {
            if (canAddSubject(context, subject)) {
                addSubjectToRecommendations(context, subject);
            }
        }
    }

    // Add core subjects for the next semester based on explicit year/semester
    private void addNextSemesterCoreSubjects(RecommendationContext context) {
        // Get all subjects for the next semester (including electives)
        List<Subject> nextSemesterSubjects = allSubjects.stream()
                .filter(subject -> String.valueOf(context.nextYear).equals(subject.getYear()) &&
                        String.valueOf(context.nextSemester).equals(subject.getSemester()))
                .filter(subject -> !context.takenSubjects.contains(subject.getCode()))
                .filter(subject -> hasPassedAllPrerequisites(subject, context.academicHistory))
                .sorted(Comparator.comparing(Subject::getCode)) // Sort by code for consistency
                .collect(Collectors.toList());

        // Add as many subjects as possible within unit limits
        for (Subject subject : nextSemesterSubjects) {
            if (canAddSubject(context, subject)) {
                addSubjectToRecommendations(context, subject);
            }
        }
    }

    // Add subjects from future semesters if prerequisites are satisfied
    private void addAdvancedEligibleSubjects(RecommendationContext context) {
        // Skip if already at max units
        if (context.totalUnits >= MAX_UNITS) {
            return;
        }

        // Define the maximum future semester to look ahead (limit to 1 year ahead)
        int maxLookAheadYear = Math.min(4, context.nextYear + 1);

        // Find all future subjects the student is eligible to take
        List<Subject> eligibleSubjects = allSubjects.stream()
                .filter(subject -> !context.takenSubjects.contains(subject.getCode()))
                .filter(subject -> !context.recommendations.contains(subject))
                .filter(subject -> !isElective(subject))
                .filter(subject -> hasPassedAllPrerequisites(subject, context.academicHistory))
                .filter(subject -> {
                    int subjYear = Integer.parseInt(subject.getYear());
                    int subjSem = Integer.parseInt(subject.getSemester());

                    // Check if subject is in a future semester but within our look-ahead limit
                    if (subjYear > context.nextYear) {
                        return subjYear <= maxLookAheadYear;
                    } else if (subjYear == context.nextYear) {
                        return subjSem > context.nextSemester;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        // Sort subjects to prioritize closer semesters first
        eligibleSubjects.sort(Comparator
                .comparing(Subject::getYear)
                .thenComparing(Subject::getSemester)
                .thenComparing(Subject::getCode));

        // Add subjects until we hit minimum units
        for (Subject subject : eligibleSubjects) {
            if (canAddSubject(context, subject)) {
                addSubjectToRecommendations(context, subject);

                // Break if we've reached minimum units
                if (context.totalUnits >= MIN_UNITS) {
                    break;
                }
            }
        }
    }

    // Add any eligible subjects, even from previous semesters if needed
    private void addAnyEligibleSubjects(RecommendationContext context) {
        // Skip if already at min units
        if (context.totalUnits >= MIN_UNITS) {
            return;
        }

        // Find any subjects the student is eligible to take but hasn't taken yet
        List<Subject> eligibleSubjects = allSubjects.stream()
                .filter(subject -> !context.takenSubjects.contains(subject.getCode()))
                .filter(subject -> !context.recommendations.contains(subject))
                .filter(subject -> !isElective(subject))
                .filter(subject -> hasPassedAllPrerequisites(subject, context.academicHistory))
                .collect(Collectors.toList());

        // Sort prioritizing lower year/semester first
        eligibleSubjects.sort(Comparator
                .comparing(Subject::getYear)
                .thenComparing(Subject::getSemester)
                .thenComparing(Subject::getCode));

        // Add subjects until we hit minimum units
        for (Subject subject : eligibleSubjects) {
            if (canAddSubject(context, subject)) {
                addSubjectToRecommendations(context, subject);

                // Break if we've reached minimum units
                if (context.totalUnits >= MIN_UNITS) {
                    break;
                }
            }
        }
    }

    // Add electives to reach optimal unit load
    private void optimizeWithElectives(RecommendationContext context) {
        if (context.totalUnits >= IDEAL_UNITS) {
            return; // Already at ideal units
        }

        // Find all electives for the next semester first
        List<Subject> nextSemesterElectives = allSubjects.stream()
                .filter(subject -> isElective(subject))
                .filter(subject -> String.valueOf(context.nextYear).equals(subject.getYear()))
                .filter(subject -> String.valueOf(context.nextSemester).equals(subject.getSemester()))
                .collect(Collectors.toList());

        // Add next semester electives first
        for (Subject elective : nextSemesterElectives) {
            if (canAddSubject(context, elective)) {
                addSubjectToRecommendations(context, elective);
                if (context.totalUnits >= IDEAL_UNITS) {
                    return;
                }
            }
        }

        // If still below ideal, look for electives from other semesters
        if (context.totalUnits < IDEAL_UNITS) {
            List<Subject> otherElectives = allSubjects.stream()
                    .filter(subject -> isElective(subject))
                    .filter(subject ->
                            !String.valueOf(context.nextYear).equals(subject.getYear()) ||
                                    !String.valueOf(context.nextSemester).equals(subject.getSemester()))
                    .filter(subject -> Integer.parseInt(subject.getYear()) <= context.nextYear)
                    .sorted(Comparator.comparing(Subject::getYear).thenComparing(Subject::getSemester))
                    .collect(Collectors.toList());

            for (Subject elective : otherElectives) {
                if (canAddSubject(context, elective)) {
                    addSubjectToRecommendations(context, elective);
                    if (context.totalUnits >= IDEAL_UNITS) {
                        return;
                    }
                }
            }
        }
    }

    // Helper method to check if a subject is an elective
    private boolean isElective(Subject subject) {
        return subject.getCode().equals("it-el") || subject.getCode().equals("it-fre");
    }

    // Check if a subject can be added to recommendations
    private boolean canAddSubject(RecommendationContext context, Subject subject) {
        if (isElective(subject)) {
            // For electives, count how many of this code are already recommended
            long recommendedCount = context.recommendations.stream()
                    .filter(s -> s.getCode().equals(subject.getCode()))
                    .count();

            // Count how many of this code were already taken
            long takenCount = 0;
            for (String code : context.takenSubjects) {
                if (code.equals(subject.getCode())) {
                    takenCount++;
                }
            }

            // Count total instances of this elective in the curriculum for the current semester
            long totalAvailable = allSubjects.stream()
                    .filter(s -> s.getCode().equals(subject.getCode()))
                    .filter(s -> String.valueOf(context.nextYear).equals(s.getYear()))
                    .filter(s -> String.valueOf(context.nextSemester).equals(s.getSemester()))
                    .count();

            // Check if more electives of this type can be taken
            return recommendedCount + takenCount < totalAvailable &&
                    context.totalUnits + subject.getUnits() <= MAX_UNITS;
        } else {
            // For non-electives, use the original logic
            return !context.recommendations.contains(subject) &&
                    !context.takenSubjects.contains(subject.getCode()) &&
                    hasPassedAllPrerequisites(subject, context.academicHistory) &&
                    context.totalUnits + subject.getUnits() <= MAX_UNITS;
        }
    }

    // Add a subject to recommendations
    private void addSubjectToRecommendations(RecommendationContext context, Subject subject) {
        context.recommendations.add(subject);
        context.totalUnits += subject.getUnits();
    }

    // Check if all prerequisites for a subject have been passed
    private boolean hasPassedAllPrerequisites(Subject subject, Map<String, Boolean> academicHistory) {
        if (subject.getPrerequisites().isEmpty()) {
            return true; // No prerequisites
        }

        return subject.getPrerequisites().stream()
                .allMatch(prereq -> academicHistory.getOrDefault(prereq, false));
    }

    // Find a subject by its code
    private Subject findSubjectByCode(String code) {
        return allSubjects.stream()
                .filter(subject -> subject.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    // Parse the curriculum XML file
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
            throw new RuntimeException("Error parsing curriculum XML: " + e.getMessage(), e);
        }
        return subjects;
    }

    // Extract year from node name
    private String extractYear(String nodeName) {
        if (nodeName.toLowerCase().contains("firstyear")) return "1";
        if (nodeName.toLowerCase().contains("secondyear")) return "2";
        if (nodeName.toLowerCase().contains("thirdyear")) return "3";
        if (nodeName.toLowerCase().contains("fourthyear")) return "4";
        return "";
    }

    // Extract semester from node name
    private String extractSemester(String nodeName) {
        if (nodeName.toLowerCase().contains("firstsem")) return "1";
        if (nodeName.toLowerCase().contains("secondsem")) return "2";
        return "";
    }

    // Get all subjects in the curriculum
    public List<Subject> getAllSubjects() {
        return new ArrayList<>(allSubjects);
    }
}