package com.db.votacao.tela.dto;

import java.util.Map;

public record BotaoResponse(String titulo, String url, Map<String, Object> body) {
}
