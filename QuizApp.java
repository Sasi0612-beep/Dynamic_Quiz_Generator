import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class QuizApp {

    private static final Scanner sc = new Scanner(System.in);
    
    // PUT YOUR Qwen-3 API KEY HERE:
    private static final String API_KEY = "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";

    private static final Map<String, List<String>> SUBJECT_TOPICS = Map.ofEntries(
        Map.entry("Java", List.of(
            "OOP", "Collections", "Streams", "Multithreading", "Exceptions", "Generics", "JVM", "Lambda Expressions", "Annotations", "Reflection"
        )),
        Map.entry("OS", List.of(
            "Memory Management", "Scheduling", "Deadlock", "File Systems", "Processes vs Threads", "I/O Management", "Paging", "Virtual Memory", "Semaphore", "System Calls"
        )),
        Map.entry("Python", List.of(
            "Data Types", "Functions", "OOP", "File Handling", "Decorators", "Iterators", "Generators", "Exception Handling", "List Comprehensions", "Modules and Packages"
        )),
        Map.entry("CN", List.of(
            "OSI Model", "TCP/IP", "Routing", "Protocols", "Subnetting", "Switching", "IP Addressing", "Congestion Control", "Firewalls", "Network Security"
        )),
        Map.entry("HTML", List.of(
            "Elements", "Forms", "Semantic Tags", "Multimedia", "Tables", "Hyperlinks", "Meta Tags", "Canvas", "SVG", "Responsive Design"
        )),
        Map.entry("CSS", List.of(
            "Selectors", "Box Model", "Flexbox", "Grid", "Animations", "Transitions", "Media Queries", "Pseudo-classes", "Variables", "Specificity"
        )),
        Map.entry("AI", List.of(
            "Search", "Heuristics", "Agents", "NLP", "Expert Systems", "Machine Learning vs AI", "Planning", "Logic", "Knowledge Representation", "Fuzzy Logic"
        )),
        Map.entry("ML", List.of(
            "Regression", "Classification", "Clustering", "Evaluation", "Supervised Learning", "Unsupervised Learning", "Overfitting", "Cross Validation", "Feature Engineering", "Bias-Variance Tradeoff"
        )),
        Map.entry("DL", List.of(
            "Neural Networks", "CNN", "RNN", "Backpropagation", "Activation Functions", "Dropout", "Autoencoders", "Transfer Learning", "Batch Normalization", "Optimizers"
        )),
        Map.entry("JS", List.of(
            "DOM", "ES6", "Events", "Async Programming", "Promises", "Fetch API", "Closures", "Prototypes", "Arrow Functions", "Modules"
        )),
        Map.entry("Aptitude", List.of(
            "Numbers", "Time & Work", "Profit & Loss", "Probability", "Percentage", "Ratio & Proportion", "Average", "Simple Interest", "Compound Interest", "Boats & Streams"
        ))
    );

    private static final String API_ENDPOINT =
        "https://openrouter.ai/api/v1/chat/completions";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Welcome to the Quiz System ===");

        String subject = select("Subject", new ArrayList<>(SUBJECT_TOPICS.keySet()));
        
        String topic;
        // Handle custom subject or predefined subject
        if (SUBJECT_TOPICS.containsKey(subject)) {
            topic = select("Topic", SUBJECT_TOPICS.get(subject));
        } else {
            // For custom subjects, ask for topic directly
            System.out.print("Enter a topic for " + subject + ": ");
            topic = sc.nextLine().trim();
        }
        
        String level = select("Level", List.of("Beginner", "Intermediate", "Advanced"));
        
        // Let user choose number of questions
        String questionCount = select("Number of Questions", List.of("15", "25", "40"));

        System.out.println("\nGenerating comprehensive summary for " + topic + "...");
        String summaryPrompt = "Create a detailed summary about " + topic + " in " + subject + 
            " for " + level + " level. Use this exact format:\n\n" +
            "Key Concepts:\n" +
            "• [concept 1]\n" +
            "• [concept 2]\n" +
            "• [concept 3]\n\n" +
            "Important Features:\n" +
            "• [feature 1]\n" +
            "• [feature 2]\n" +
            "• [feature 3]\n" +
            "• [feature 4]\n\n" +
            "Common Applications:\n" +
            "• [application 1]\n" +
            "• [application 2]\n" +
            "• [application 3]\n\n" +
            "Best Practices:\n" +
            "• [practice 1]\n" +
            "• [practice 2]\n" +
            "• [practice 3]\n\n" +
            "Common Pitfalls:\n" +
            "• [pitfall 1]\n" +
            "• [pitfall 2]\n\n" +
            "Make it comprehensive and suitable for " + level + " level understanding.";
        
        String summary = callQwen(summaryPrompt);
        System.out.println("\n=== Comprehensive Summary ===\n" + summary);

        System.out.println("\nGenerating quiz questions...");
        String questionPrompt = "Generate " + questionCount + " multiple choice questions on " + topic +
            " in " + subject + " at " + level + " level. " +
            "Each question should include fields: questionText, optionA, optionB, optionC, optionD, correctOption, explanation. " +
            "The correctOption should be A, B, C, or D. The explanation should explain why the correct answer is right. " +
            "Return as JSON array of objects.";
        String quizJson = callQwen(questionPrompt);

        // Parse and run the quiz
        runQuiz(quizJson);
    }

    private static void runQuiz(String quizJson) {
        try {
            List<Question> questions = parseQuestions(quizJson);
            if (questions.isEmpty()) {
                System.out.println("Failed to parse quiz questions. Please try again.");
                return;
            }

            System.out.println("\n" + "=".repeat(50));
            System.out.println("🎯 QUIZ TIME! Answer the following questions:");
            System.out.println("=".repeat(50));

            int score = 0;
            int totalQuestions = questions.size();

            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                System.out.println("\n📝 Question " + (i + 1) + " of " + totalQuestions);
                System.out.println("─".repeat(40));
                System.out.println(q.questionText);
                System.out.println();
                System.out.println("A) " + q.optionA);
                System.out.println("B) " + q.optionB);
                System.out.println("C) " + q.optionC);
                System.out.println("D) " + q.optionD);
                System.out.println();
                System.out.print("Your answer (A/B/C/D): ");
                
                String userAnswer = sc.nextLine().trim().toUpperCase();
                
                // Validate input
                while (!userAnswer.matches("[ABCD]")) {
                    System.out.print("Please enter A, B, C, or D: ");
                    userAnswer = sc.nextLine().trim().toUpperCase();
                }

                if (userAnswer.equals(q.correctOption.toUpperCase())) {
                    System.out.println("✅ Correct!");
                    score++;
                } else {
                    System.out.println("❌ Incorrect!");
                    System.out.println("The correct answer is: " + q.correctOption.toUpperCase());
                }
                
                System.out.println("💡 Explanation: " + q.explanation);
                
                if (i < questions.size() - 1) {
                    System.out.println("\nPress Enter to continue to the next question...");
                    sc.nextLine();
                }
            }

            // Display final results
            System.out.println("\n" + "=".repeat(50));
            System.out.println("🏆 QUIZ COMPLETED!");
            System.out.println("=".repeat(50));
            System.out.println("Your Score: " + score + "/" + totalQuestions);
            
            double percentage = (double) score / totalQuestions * 100;
            System.out.printf("Percentage: %.1f%%\n", percentage);
            
            if (percentage >= 80) {
                System.out.println("🌟 Excellent! You have a great understanding of the topic!");
            } else if (percentage >= 60) {
                System.out.println("👍 Good job! You have a solid grasp of the basics.");
            } else if (percentage >= 40) {
                System.out.println("📚 Keep studying! You're on the right track.");
            } else {
                System.out.println("💪 Don't give up! Review the material and try again.");
            }

        } catch (Exception e) {
            System.err.println("Error running quiz: " + e.getMessage());
            System.out.println("Raw quiz data received:");
            System.out.println(quizJson);
        }
    }

    private static List<Question> parseQuestions(String jsonResponse) {
        List<Question> questions = new ArrayList<>();
        try {
            // Find the JSON array in the response
            String json = jsonResponse.trim();
            
            // Look for array start
            int arrayStart = json.indexOf('[');
            int arrayEnd = json.lastIndexOf(']');
            
            if (arrayStart == -1 || arrayEnd == -1) {
                System.err.println("No JSON array found in response");
                return questions;
            }
            
            json = json.substring(arrayStart + 1, arrayEnd);
            
            // Split by objects (simple parsing)
            String[] questionObjects = json.split("\\},\\s*\\{");
            
            for (String questionObj : questionObjects) {
                // Clean up the object string
                questionObj = questionObj.replace("{", "").replace("}", "");
                
                Question q = new Question();
                
                // Parse each field
                q.questionText = extractJsonValue(questionObj, "questionText");
                q.optionA = extractJsonValue(questionObj, "optionA");
                q.optionB = extractJsonValue(questionObj, "optionB");
                q.optionC = extractJsonValue(questionObj, "optionC");
                q.optionD = extractJsonValue(questionObj, "optionD");
                q.correctOption = extractJsonValue(questionObj, "correctOption");
                q.explanation = extractJsonValue(questionObj, "explanation");
                
                // Only add if we have all required fields
                if (q.questionText != null && q.optionA != null && q.correctOption != null) {
                    questions.add(q);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing questions: " + e.getMessage());
        }
        
        return questions;
    }

    private static String extractJsonValue(String jsonObject, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(jsonObject);
            
            if (m.find()) {
                return m.group(1).replace("\\\"", "\"").replace("\\n", "\n");
            }
        } catch (Exception e) {
            System.err.println("Error extracting field " + fieldName + ": " + e.getMessage());
        }
        return null;
    }

    // Question class to hold quiz data
    private static class Question {
        String questionText;
        String optionA;
        String optionB;
        String optionC;
        String optionD;
        String correctOption;
        String explanation;
    }

    private static String select(String label, List<String> options) {
        System.out.println("\nSelect " + label + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
        
        // Add option for custom entry
        if (label.equals("Subject") || label.equals("Topic")) {
            System.out.println((options.size() + 1) + ". Enter Custom " + label);
        }
        
        System.out.print("> ");
        int choice = sc.nextInt();
        sc.nextLine();
        
        // Handle custom entry
        if ((label.equals("Subject") || label.equals("Topic")) && choice == options.size() + 1) {
            System.out.print("Enter your custom " + label.toLowerCase() + ": ");
            return sc.nextLine().trim();
        }
        
        return options.get(choice - 1);
    }

    private static String callQwen(String prompt) throws Exception {
        String payload = String.format("""
            {
              "model": "qwen/qwen-2.5-72b-instruct",
              "messages": [
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "temperature": 0.7
            }
            """, escapeJson(prompt));

        HttpURLConnection conn = (HttpURLConnection) new URL(API_ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        
        // Check response code
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.err.println("API request failed with response code: " + responseCode);
            // Read error stream
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            System.err.println("Error response: " + response.toString());
            return "API Error: " + responseCode + " - " + response.toString();
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        return extractContent(response.toString());
    }

    private static String escapeJson(String text) {
        return text.replace("\"", "\\\"");
    }

    private static String extractContent(String json) {
        try {
            // Look for the content field in the response
            int contentStart = json.indexOf("\"content\":\"");
            if (contentStart == -1) {
                System.err.println("No content field found in response: " + json);
                return "Error: No content in API response";
            }
            
            contentStart += "\"content\":\"".length();
            
            // Find the end of the content, handling escaped quotes
            int contentEnd = contentStart;
            while (contentEnd < json.length()) {
                if (json.charAt(contentEnd) == '"' && 
                    (contentEnd == 0 || json.charAt(contentEnd - 1) != '\\')) {
                    break;
                }
                contentEnd++;
            }
            
            if (contentEnd >= json.length()) {
                System.err.println("Malformed JSON response: " + json);
                return "Error: Malformed API response";
            }
            
            String content = json.substring(contentStart, contentEnd);
            // Unescape JSON strings
            return content.replace("\\n", "\n")
                         .replace("\\\"", "\"")
                         .replace("\\\\", "\\");
                         
        } catch (Exception e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());
            System.err.println("Raw response: " + json);
            return "Error parsing API response";
        }
    }
}
