package com.db.votacao.tela.dto;

public record CampoResponse(String id, String tipo, String titulo) {

	private static final String TIPO_TEXTO = "TEXTO";
	private static final String TIPO_INFORMACAO = "INFORMACAO";

	public static CampoResponse texto(String id, String titulo) {
		return new CampoResponse(id, TIPO_TEXTO, titulo);
	}

	public static CampoResponse informacao(String id, String titulo) {
		return new CampoResponse(id, TIPO_INFORMACAO, titulo);
	}
}
