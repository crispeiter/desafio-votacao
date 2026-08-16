package com.db.votacao.voto;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.db.votacao.voto.dto.RegistrarVotoRequest;
import com.db.votacao.voto.dto.VotoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Votos", description = "Registro do voto do associado na pauta")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/votos")
public class VotoController {

	private final VotoService votoService;

	public VotoController(VotoService votoService) {
		this.votoService = votoService;
	}

	@Operation(summary = "Registra o voto de um associado na pauta",
			description = "Cada associado vota uma única vez por pauta. Sessão inexistente ou associado não apto "
					+ "devolvem 404; sessão encerrada e voto repetido devolvem 409.")
	@PostMapping
	public ResponseEntity<VotoResponse> registrar(@PathVariable Long pautaId,
			@RequestBody @Valid RegistrarVotoRequest request, UriComponentsBuilder uriBuilder) {
		Voto voto = votoService.registrarVoto(pautaId, request.cpf(), request.opcao());
		// Aponta para a apuracao, e nao para o voto: nao existe GET de voto individual, porque
		// expor quem votou o que quebraria o sigilo. A apuracao e o efeito observavel do insert.
		URI localizacao = uriBuilder.path("/api/v1/pautas/{pautaId}/resultado").buildAndExpand(pautaId).toUri();
		return ResponseEntity.created(localizacao).body(paraResponse(voto, pautaId));
	}

	// pautaId vem do path, e nao de voto.getSessao().getPauta(): com open-in-view desligado a
	// sessao de persistencia ja esta fechada aqui e navegar a associacao lazy quebraria.
	private static VotoResponse paraResponse(Voto voto, Long pautaId) {
		return new VotoResponse(voto.getId(), pautaId, voto.getOpcao(), voto.getRegistradoEm());
	}

}
