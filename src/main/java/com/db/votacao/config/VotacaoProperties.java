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

			@NotBlank String baseUrl,

			// Teto de poucos segundos porque a consulta acontece dentro da requisicao de voto:
			// provedor lento precisa falhar rapido em vez de prender a thread e o pool de conexao.
			@NotNull
			@DurationMin(millis = 100)
			@DurationMax(seconds = 10)
			Duration timeoutConexao,

			@NotNull
			@DurationMin(millis = 100)
			@DurationMax(seconds = 10)
			Duration timeoutLeitura) {
	}

}
