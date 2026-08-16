package com.db.votacao.config;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.db.votacao.cpf.ModoValidacaoCpf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "votacao")
public record VotacaoProperties(

		@NotNull @Valid Sessao sessao,

		@NotNull @Valid ValidacaoCpf cpf,

		@NotBlank String callbackBaseUrl) {

	public record Sessao(

			// Mesma faixa aceita em AbrirSessaoRequest: o limite inferior impede sessao
			// nascida encerrada e o superior impede sessao perpetua por erro de digitacao.
			@NotNull
			@DurationMin(seconds = 1)
			@DurationMax(hours = 24)
			Duration duracaoPadrao) {
	}

	public record ValidacaoCpf(

			@NotNull ModoValidacaoCpf modo,

			@NotBlank String baseUrl) {
	}

}
