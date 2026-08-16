package com.db.votacao.pauta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(

		@NotBlank(message = "O título é obrigatório.")
		@Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
		String titulo,

		@Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
		String descricao) {
}
