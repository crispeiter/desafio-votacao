package com.db.votacao.sessao;

import java.net.URI;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.db.votacao.sessao.dto.AbrirSessaoRequest;
import com.db.votacao.sessao.dto.SessaoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Sessões", description = "Abertura e consulta da sessão de votação de uma pauta")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessao")
public class SessaoController {

	private final SessaoVotacaoService sessaoVotacaoService;

	public SessaoController(SessaoVotacaoService sessaoVotacaoService) {
		this.sessaoVotacaoService = sessaoVotacaoService;
	}

	@Operation(summary = "Abre a sessão de votação da pauta",
			description = "O corpo é opcional. Sem ele, ou sem duracaoEmSegundos, a sessão dura o padrão configurado "
					+ "de 60 segundos. A duração aceita vai de 1 a 86400 segundos.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Sessão aberta, com a URI dela no header Location"),
			@ApiResponse(responseCode = "400", description = "Duração fora da faixa de 1 a 86400 segundos",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "404", description = "Não existe pauta com o identificador informado",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "409", description = "A pauta já possui sessão de votação",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PostMapping
	public ResponseEntity<SessaoResponse> abrir(@PathVariable Long pautaId,
			@RequestBody(required = false) @Valid AbrirSessaoRequest request, UriComponentsBuilder uriBuilder) {
		SessaoVotacao sessao = sessaoVotacaoService.abrir(pautaId,
				request == null ? null : request.duracaoEmSegundos());
		URI localizacao = uriBuilder.path("/api/v1/pautas/{pautaId}/sessao").buildAndExpand(pautaId).toUri();
		return ResponseEntity.created(localizacao).body(paraResponse(sessao));
	}

	@Operation(summary = "Consulta a sessão de votação da pauta")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Sessão encontrada, com o status calculado no momento "
					+ "da consulta"),
			@ApiResponse(responseCode = "404", description = "Pauta inexistente ou sem sessão de votação",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping
	public SessaoResponse buscar(@PathVariable Long pautaId) {
		return paraResponse(sessaoVotacaoService.buscarPorPauta(pautaId));
	}

	private SessaoResponse paraResponse(SessaoVotacao sessao) {
		return new SessaoResponse(sessao.getId(), sessao.getPauta().getId(), sessao.getAbertaEm(),
				sessao.getEncerraEm(), sessaoVotacaoService.statusAtual(sessao));
	}

}
