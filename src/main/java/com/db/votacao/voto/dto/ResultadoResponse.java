package com.db.votacao.voto.dto;

import com.db.votacao.sessao.StatusSessao;
import com.db.votacao.voto.ResultadoPauta;

public record ResultadoResponse(Long pautaId, String titulo, StatusSessao statusSessao, long totalVotos,
		long votosSim, long votosNao, ResultadoPauta resultado) {
}
