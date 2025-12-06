package com.bulletjournal.Companion.App.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI bulletJournalCompanionOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Bullet Journal Companion App API")
						.description("REST APIs for Bullet Journal Management System with OCR, Content Extraction, and Export Features. " +
								"This API allows users to scan handwritten journal pages, extract tasks, events, notes, and emotions " +
								"using OCR technology, and export data in TaskPaper and Markdown formats.")
						.version("v1.0")
						.contact(new Contact()
								.name("Bullet Journal Companion Team")
								.email("support@bulletjournal.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
				.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
				.components(new Components()
						.addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
	}

	private SecurityScheme createAPIKeyScheme() {
		return new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.bearerFormat("JWT")
				.scheme("bearer");
	}
}
