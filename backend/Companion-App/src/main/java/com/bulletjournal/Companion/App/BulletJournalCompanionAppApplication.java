package com.bulletjournal.Companion.App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.stabilityai.autoconfigure.StabilityAiImageAutoConfiguration;

@SpringBootApplication(exclude = {StabilityAiImageAutoConfiguration.class})
public class BulletJournalCompanionAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(BulletJournalCompanionAppApplication.class, args);
	}

}
