package com.example.bajaj.runner;

import com.example.bajaj.model.WebhookResponse;
import com.example.bajaj.service.WebhookService;
import com.example.bajaj.util.ConsoleArt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    private final WebhookService webhookService;

    public StartupRunner(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ConsoleArt.printBanner();
        log.info("Starting automated qualifier flow...");

        WebhookResponse resp = webhookService.generateWebhook();
        if (resp == null) {
            log.error("Aborting: could not generate webhook");
            return;
        }

        webhookService.submitFinalQuery(resp);

        log.info("Process finished. If you provided final.query it will have been posted. Otherwise check downloads/ for PDFs and add final.query then re-run.");
    }
}
