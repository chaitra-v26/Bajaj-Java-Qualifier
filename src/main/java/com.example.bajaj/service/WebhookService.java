package com.example.bajaj.service;

import com.example.bajaj.model.FinalQueryRequest;
import com.example.bajaj.model.GenerateRequest;
import com.example.bajaj.model.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class WebhookService {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final RestTemplate restTemplate;

    @Value("${qualifier.name}")
    private String name;

    @Value("${qualifier.regNo}")
    private String regNo;

    @Value("${qualifier.email}")
    private String email;

    @Value("${final.query:}")
    private String finalQueryProp;

    @Value("${download.dir:./downloads}")
    private String downloadDir;

    @Value("${auto.post:true}")
    private boolean autoPost;

    public WebhookService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WebhookResponse generateWebhook() {
        try {
            String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
            GenerateRequest req = new GenerateRequest(name, regNo, email);
            log.info("Sending generateWebhook request for {} / {}", name, regNo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GenerateRequest> entity = new HttpEntity<>(req, headers);

            ResponseEntity<WebhookResponse> resp = restTemplate.postForEntity(url, entity, WebhookResponse.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                log.info("Received webhook and token");
                return resp.getBody();
            }
            log.error("Failed to generate webhook. Status: {}", resp.getStatusCodeValue());
        } catch (Exception e) {
            log.error("Exception in generateWebhook: {}", e.getMessage());
        }
        return null;
    }

    public void submitFinalQuery(WebhookResponse webhookResponse) throws Exception {
        if (webhookResponse == null) throw new IllegalArgumentException("webhookResponse is null");

        String webhookUrl = webhookResponse.getWebhook();
        String token = webhookResponse.getAccessToken();

        // priority: application.properties -> answers.properties
        String finalQuery = StringUtils.hasText(finalQueryProp) ? finalQueryProp : readFinalQueryFromAnswersFile();

        if (!StringUtils.hasText(finalQuery)) {
            log.warn("No final query provided. Will download question PDF so you can inspect and set final.query.");
            PdfHelper.downloadQuestionPdfForRegNo(regNo, downloadDir);
            log.info("Downloaded files to {}. Put your final SQL in application.properties (final.query) and re-run.", downloadDir);
            return;
        }

        FinalQueryRequest finalReq = new FinalQueryRequest(finalQuery);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        HttpEntity<FinalQueryRequest> entity = new HttpEntity<>(finalReq, headers);

        log.info("About to POST final query to webhook: {}", webhookUrl);
        if (!autoPost) {
            log.info("Auto post disabled. Final payload would be: {}", finalQuery);
            return;
        }

        ResponseEntity<String> resp = restTemplate.postForEntity(webhookUrl, entity, String.class);
        log.info("POST returned http status {} and body: {}", resp.getStatusCode(), resp.getBody());
    }

    private String readFinalQueryFromAnswersFile() {
        try {
            Path p = Paths.get("answers.properties");
            if (Files.exists(p)) {
                for (String line : Files.readAllLines(p)) {
                    line = line.trim();
                    if (line.startsWith("final.query")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) return parts[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not read answers.properties: {}", e.getMessage());
        }
        return null;
    }
}
