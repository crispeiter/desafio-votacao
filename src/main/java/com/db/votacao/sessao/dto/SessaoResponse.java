package com.db.votacao.sessao.dto;

import java.time.Instant;

import com.db.votacao.sessao.StatusSessao;

public record SessaoResponse(Long id, Long pautaId, Instant abertaEm, Instant encerraEm, StatusSessao status) {
}
