package com.db.votacao.voto.dto;

import java.time.Instant;

import com.db.votacao.voto.OpcaoVoto;

public record VotoResponse(Long id, Long pautaId, OpcaoVoto opcao, Instant registradoEm) {
}
