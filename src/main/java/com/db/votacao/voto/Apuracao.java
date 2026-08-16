package com.db.votacao.voto;

import com.db.votacao.sessao.StatusSessao;

public record Apuracao(Long pautaId, String titulo, StatusSessao statusSessao, long votosSim, long votosNao) {

	public long totalVotos() {
		return votosSim + votosNao;
	}

	// Ausencia de voto e situacao diferente de empate: ninguem se manifestou, entao nao houve
	// disputa a empatar.
	public ResultadoPauta resultado() {
		if (totalVotos() == 0) {
			return ResultadoPauta.SEM_VOTOS;
		}
		if (votosSim > votosNao) {
			return ResultadoPauta.APROVADA;
		}
		return votosNao > votosSim ? ResultadoPauta.REPROVADA : ResultadoPauta.EMPATE;
	}

}
