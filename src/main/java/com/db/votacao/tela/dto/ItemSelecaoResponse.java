package com.db.votacao.tela.dto;

import java.util.Map;

public record ItemSelecaoResponse(String titulo, String descricao, String url, Map<String, Object> body) {
}
