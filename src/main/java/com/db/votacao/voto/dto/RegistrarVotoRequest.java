package com.db.votacao.voto.dto;

import com.db.votacao.voto.OpcaoVoto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrarVotoRequest(

		// Sem validacao de formato aqui: quem decide se o CPF existe e o provedor de
		// autorizacao, e duplicar a regra dele nesta camada esconderia divergencia de contrato.
		@NotBlank(message = "O CPF é obrigatório.")
		String cpf,

		@NotNull(message = "A opção de voto é obrigatória e deve ser SIM ou NAO.")
		OpcaoVoto opcao) {
}
