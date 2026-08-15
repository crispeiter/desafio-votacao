package com.db.votacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI votacaoOpenApi() {
		return new OpenAPI().info(new Info()
				.title("API de Votação")
				.description("Gerenciamento de pautas e sessões de votação em assembleias de cooperativa")
				.version("1.0.0"));
	}

}
