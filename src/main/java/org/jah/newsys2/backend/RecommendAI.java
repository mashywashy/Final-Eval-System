package org.jah.newsys2.backend;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

public class RecommendAI {
    private static final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = "https://jahnissi-api.yeems214.xyz/ask";
    //private static final String API_URL = "http://localhost:8080/ask";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    public String recommendAI(List<Subject> subjects, String program, String name) {
        String prompt = buildPrompt(subjects, program, name);
        String response = sendPostRequest(prompt);
        return parseResponse(response);
    }

    private String buildPrompt(List<Subject> subjects, String program, String name) {
        StringJoiner subjectList = new StringJoiner(", ");
        for (Subject subject : subjects) {
            subjectList.add(subject.getCode() + " (" + subject.getUnits() + " units)");
        }

        return String.format(
                "Present the following recommended subjects for a student by categorizing which are majors and minors, " +
                        "and calculate the total units. (Student Name: %s, Program: %s) Subjects: %s",
                name, program, subjectList
        );
    }

    private String sendPostRequest(String prompt) {
        RequestBody body = RequestBody.create(String.format("{\"prompt\": \"%s\"}", prompt), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader(HEADER_ACCEPT, "application/json")
                .addHeader(HEADER_CONTENT_TYPE, "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Request failed. Code: " + response.code());
                return response.body() != null ? response.body().string() : "Error: Empty response body";
            }
            return response.body() != null ? response.body().string() : "Error: Empty response body";
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            return null;
        }
    }

    private String parseResponse(String response) {
        if (response == null) {
            System.out.println("Failed to fetch response.");
            return null;
        }
        JSONObject jsonObject = new JSONObject(response);
        String reply = jsonObject.optString("reply", "No reply found.");
        System.out.println(reply);
        return reply;
    }

    public static void main(String[] args) {
        // Example usage
    }
}
