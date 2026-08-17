package com.db.votacao.tela.dto;

import java.util.List;

public record TelaSelecaoResponse(String tipo, String titulo, List<ItemSelecaoResponse> itens) {

	private static final String TIPO = "SELECAO";

	public static TelaSelecaoResponse de(String titulo, List<ItemSelecaoResponse> itens) {
		return new TelaSelecaoResponse(TIPO, titulo, itens);
	}
}
