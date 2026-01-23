package com.isaqcasey.aidocsassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiDocsAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDocsAssistantApplication.class, args);
    }

}
