package com.db.votacao.sessao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AbrirSessaoRequest(

		// Nulo e valido: corpo ausente ou campo omitido cai na duracao padrao configurada.
		@Min(value = 1, message = "A duração deve ser de no mínimo 1 segundo.")
		@Max(value = 86400, message = "A duração deve ser de no máximo 86400 segundos.")
		Integer duracaoEmSegundos) {
}
