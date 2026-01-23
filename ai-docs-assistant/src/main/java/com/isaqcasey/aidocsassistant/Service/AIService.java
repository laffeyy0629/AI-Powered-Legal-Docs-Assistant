package com.isaqcasey.aidocsassistant.Service;

import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIService {

    @Value("${ai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    
    private static final int MAX_SUMMARY_SENTENCES = 5;
    private static final int MIN_SENTENCE_LENGTH = 30;
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "this", "that", "these", "those", "of", "to", "in", "for", "on", "at"
    );

    // General analyze method
    public String analyzeText(String text) {
        return summarize(text);
    }

    // Summarize method - uses intelligent extractive summarization
    public String summarize(String text) {
        // Use optimized extractive summary (API deprecated)
        return createSimpleSummary(text);
    }
    
    // Optimized extractive summarization with keyword extraction
    private String createSimpleSummary(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "No content to summarize.";
        }
        
        // Clean text and normalize whitespace
        text = text.replaceAll("\\s+", " ").trim();
        
        // Split into sentences more intelligently
        String[] allSentences = text.split("(?<=[.!?])\\s+");
        
        // Filter and score sentences
        List<ScoredSentence> scoredSentences = new ArrayList<>();
        Map<String, Integer> wordFrequency = calculateWordFrequency(text);
        
        for (int i = 0; i < allSentences.length; i++) {
            String sentence = allSentences[i].trim();
            if (sentence.length() >= MIN_SENTENCE_LENGTH && !isHeader(sentence)) {
                double score = scoreSentence(sentence, wordFrequency, i, allSentences.length);
                scoredSentences.add(new ScoredSentence(sentence, score, i));
            }
        }
        
        // Sort by score and select top sentences
        scoredSentences.sort((a, b) -> Double.compare(b.score, a.score));
        int summarySize = Math.min(MAX_SUMMARY_SENTENCES, Math.max(3, allSentences.length / 5));
        
        List<ScoredSentence> topSentences = scoredSentences.stream()
            .limit(summarySize)
            .sorted(Comparator.comparingInt(s -> s.position))
            .collect(Collectors.toList());
        
        // Build summary
        StringBuilder summary = new StringBuilder();
        summary.append("**Summary:**\n\n");
        
        for (ScoredSentence ss : topSentences) {
            summary.append("• ").append(ss.sentence);
            if (!ss.sentence.matches(".*[.!?]$")) {
                summary.append(".");
            }
            summary.append("\n");
        }
        
        // Extract keywords
        List<String> keywords = extractKeywords(wordFrequency, 5);
        if (!keywords.isEmpty()) {
            summary.append("\n**Key Topics:** ");
            summary.append(String.join(", ", keywords)).append("\n");
        }
        
        // Add statistics
        int wordCount = text.split("\\s+").length;
        summary.append("\n**Document Info:**\n");
        summary.append("- Words: ").append(wordCount);
        summary.append(" | Characters: ").append(text.length());
        summary.append(" | Sentences: ").append(allSentences.length).append("\n");
        
        return summary.toString();
    }
    
    // Calculate word frequency for scoring
    private Map<String, Integer> calculateWordFrequency(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        String[] words = text.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .split("\\s+");
        
        for (String word : words) {
            if (word.length() > 3 && !STOP_WORDS.contains(word)) {
                frequency.merge(word, 1, Integer::sum);
            }
        }
        return frequency;
    }
    
    // Score sentences based on word frequency, position, and length
    private double scoreSentence(String sentence, Map<String, Integer> wordFreq, int position, int totalSentences) {
        String[] words = sentence.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .split("\\s+");
        
        double score = 0;
        int relevantWords = 0;
        
        for (String word : words) {
            if (wordFreq.containsKey(word)) {
                score += wordFreq.get(word);
                relevantWords++;
            }
        }
        
        // Normalize by sentence length
        if (relevantWords > 0) {
            score = score / relevantWords;
        }
        
        // Boost score for sentences near the beginning
        if (position < totalSentences * 0.2) {
            score *= 1.3;
        }
        
        // Boost score for sentences with numbers (often contain key facts)
        if (sentence.matches(".*\\d+.*")) {
            score *= 1.2;
        }
        
        return score;
    }
    
    // Extract top keywords from frequency map
    private List<String> extractKeywords(Map<String, Integer> wordFreq, int count) {
        return wordFreq.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(count)
            .map(Map.Entry::getKey)
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.toList());
    }
    
    // Check if a sentence is likely a header or bullet point
    private boolean isHeader(String sentence) {
        return sentence.matches("^[●•\\*\\-].*") || 
               sentence.matches("^[A-Z][a-z]*:$") ||
               sentence.length() < 10;
    }
    
    // Inner class to hold scored sentences
    private static class ScoredSentence {
        String sentence;
        double score;
        int position;
        
        ScoredSentence(String sentence, double score, int position) {
            this.sentence = sentence;
            this.score = score;
            this.position = position;
        }
    }

    // Try calling Hugging Face API - returns error message if fails
    private String callHuggingFaceModel(String text) {
        try {
            // Truncate text if too long
            if (text.length() > 1024) {
                text = text.substring(0, 1024);
                System.out.println("Text truncated to 1024 characters for API limits");
            }
            
            // Prepare request body
            JSONObject body = new JSONObject();
            body.put("inputs", text);
            
            JSONObject parameters = new JSONObject();
            parameters.put("max_length", 150);
            parameters.put("min_length", 30);
            body.put("parameters", parameters);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            System.out.println("Attempting Hugging Face API call...");
            
            // Try the API call
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api-inference.huggingface.co/models/facebook/bart-large-cnn",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            String responseBody = response.getBody();
            System.out.println("API Response received: " + responseBody);
            
            if (responseBody == null || responseBody.isEmpty()) {
                return "Error: Empty response";
            }
            
            // Parse and extract summary
            if (responseBody.contains("summary_text")) {
                try {
                    int startIdx = responseBody.indexOf("\"summary_text\":\"") + 16;
                    int endIdx = responseBody.indexOf("\"", startIdx);
                    if (startIdx > 15 && endIdx > startIdx) {
                        return responseBody.substring(startIdx, endIdx);
                    }
                } catch (Exception parseError) {
                    // Return raw response if parsing fails
                }
            }
            
            return responseBody;
            
        } catch (Exception e) {
            System.err.println("Hugging Face API error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
