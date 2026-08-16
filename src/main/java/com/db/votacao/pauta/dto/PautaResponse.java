package com.db.votacao.pauta.dto;

import java.time.Instant;

public record PautaResponse(Long id, String titulo, String descricao, Instant criadaEm) {
}
