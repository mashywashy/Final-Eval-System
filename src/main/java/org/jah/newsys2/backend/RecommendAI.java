package org.jah.newsys2.backend;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class RecommendAI {
    private static final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = "http://localhost:8080/ask";

    public String recommendAI(List<Subject> subjects, String program) {
        StringBuilder prompt = new StringBuilder("Present following recommended subjects for " + program + " are: ");

        for (Subject subject : subjects) {
            prompt.append(subject.getCode()).append(" (").append(subject.getUnits()).append(" units), ");
        }
        String response = sendPostRequest(prompt.toString());

        if (response != null) {
            JSONObject jsonObject = new JSONObject(response);
            String reply = jsonObject.optString("reply", "no reply found");
            System.out.println("reply" + reply);
            return reply;
        } else {
            System.out.println("Failed to fetch response.");
            return null;
        }

    }

    public static void main(String[] args) {
        String prompt = "Tell me a fun fact about space.";
        String response = sendPostRequest(prompt);

        if (response != null) {
            JSONObject jsonObject = new JSONObject(response);
            String reply = jsonObject.optString("reply", "no reply found");
            System.out.println("Response: " + reply);
        } else {
            System.out.println("Failed to fetch response.");
        }
    }

    private static String sendPostRequest(String prompt) {
        // Construct JSON request body
        String json = String.format("{\"prompt\": \"%s\"}", prompt);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        // Create the request
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute request and handle response
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
}