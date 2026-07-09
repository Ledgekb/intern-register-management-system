package com.internregister.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for interacting with the Face++ cloud API.
 * Uses image_base64 parameter to avoid multipart binary encoding issues.
 */
@Service
public class FacePlusPlusService {

    @Value("${facepp.api.key}")
    private String apiKey;

    @Value("${facepp.api.secret}")
    private String apiSecret;

    @Value("${facepp.api.url}")
    private String apiUrl;

    @Value("${facepp.confidence.threshold:80}")
    private double confidenceThreshold;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Detect whether a face is present in the given Base64-encoded image.
     * Uses the image_base64 parameter (no binary multipart needed).
     */
    public boolean detectFace(String base64Image) throws Exception {
        String cleanBase64 = stripDataUri(base64Image);

        String postData = "api_key=" + encode(apiKey)
                + "&api_secret=" + encode(apiSecret)
                + "&image_base64=" + encode(cleanBase64)
                + "&return_landmark=0"
                + "&return_attributes=none";

        String responseBody = doPost(apiUrl + "/detect", postData);
        System.out.println("Face++ detect response: " + responseBody.substring(0, Math.min(300, responseBody.length())));

        JsonNode root = objectMapper.readTree(responseBody);

        if (root.has("error_message")) {
            String errorMsg = root.get("error_message").asText();
            System.err.println("Face++ detect error: " + errorMsg);
            // FACE_NOT_FOUND means no face was in the image - return false instead of throwing
            if ("FACE_NOT_FOUND".equalsIgnoreCase(errorMsg)) {
                return false;
            }
            throw new RuntimeException("Face++ API error: " + errorMsg);
        }

        JsonNode faces = root.get("faces");
        return faces != null && faces.isArray() && faces.size() > 0;
    }

    /**
     * Compare two faces using the Face++ Compare API.
     * Uses image_base64_1 and image_base64_2 parameters.
     */
    public FaceCompareResult compareFaces(String base64Image1, String base64Image2) throws Exception {
        String clean1 = stripDataUri(base64Image1);
        String clean2 = stripDataUri(base64Image2);

        String postData = "api_key=" + encode(apiKey)
                + "&api_secret=" + encode(apiSecret)
                + "&image_base64_1=" + encode(clean1)
                + "&image_base64_2=" + encode(clean2);

        String responseBody = doPost(apiUrl + "/compare", postData);
        System.out.println("Face++ compare response: " + responseBody.substring(0, Math.min(300, responseBody.length())));

        JsonNode root = objectMapper.readTree(responseBody);

        if (root.has("error_message")) {
            String errorMsg = root.get("error_message").asText();
            System.err.println("Face++ compare error: " + errorMsg);
            throw new RuntimeException("Face++ API error: " + errorMsg);
        }

        double confidence = root.path("confidence").asDouble(0);
        boolean verified = confidence >= confidenceThreshold;
        System.out.println("Face++ result: confidence=" + confidence + ", threshold=" + confidenceThreshold + ", verified=" + verified);
        return new FaceCompareResult(verified, confidence);
    }

    // ─── HTTP Helper ─────────────────────────────────────────────────────────

    private String doPost(String urlStr, String postData) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);

        byte[] postBytes = postData.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(postBytes.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postBytes);
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        System.out.println("Face++ HTTP status: " + responseCode);

        InputStream is;
        try { is = conn.getInputStream(); } catch (IOException e) { is = conn.getErrorStream(); }
        if (is == null) return "{}";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    private String stripDataUri(String base64) {
        if (base64 == null) return "";
        int commaIdx = base64.indexOf(',');
        return commaIdx >= 0 ? base64.substring(commaIdx + 1) : base64;
    }

    // ─── Result DTO ─────────────────────────────────────────────────────────

    public static class FaceCompareResult {
        private final boolean verified;
        private final double confidence;

        public FaceCompareResult(boolean verified, double confidence) {
            this.verified = verified;
            this.confidence = confidence;
        }

        public boolean isVerified() { return verified; }
        public double getConfidence() { return confidence; }
    }
}