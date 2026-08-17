package com.db.votacao.tela.dto;

import java.util.List;

public record TelaFormularioResponse(String tipo, String titulo, List<CampoResponse> itens,
		List<BotaoResponse> botoes) {

	private static final String TIPO = "FORMULARIO";

	public static TelaFormularioResponse de(String titulo, List<CampoResponse> itens, List<BotaoResponse> botoes) {
		return new TelaFormularioResponse(TIPO, titulo, itens, botoes);
	}
}
