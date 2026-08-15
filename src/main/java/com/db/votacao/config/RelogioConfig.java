package com.db.votacao.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {

	// Clock injetado no lugar de Instant.now() para que a janela da sessao possa ser
	// controlada nos testes com Clock.fixed, sem Thread.sleep.
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
