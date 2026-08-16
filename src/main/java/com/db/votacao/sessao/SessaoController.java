package com.db.votacao.sessao;

import java.net.URI;

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
	@PostMapping
	public ResponseEntity<SessaoResponse> abrir(@PathVariable Long pautaId,
			@RequestBody(required = false) @Valid AbrirSessaoRequest request, UriComponentsBuilder uriBuilder) {
		SessaoVotacao sessao = sessaoVotacaoService.abrir(pautaId,
				request == null ? null : request.duracaoEmSegundos());
		URI localizacao = uriBuilder.path("/api/v1/pautas/{pautaId}/sessao").buildAndExpand(pautaId).toUri();
		return ResponseEntity.created(localizacao).body(paraResponse(sessao));
	}

	@Operation(summary = "Consulta a sessão de votação da pauta")
	@GetMapping
	public SessaoResponse buscar(@PathVariable Long pautaId) {
		return paraResponse(sessaoVotacaoService.buscarPorPauta(pautaId));
	}

	private SessaoResponse paraResponse(SessaoVotacao sessao) {
		return new SessaoResponse(sessao.getId(), sessao.getPauta().getId(), sessao.getAbertaEm(),
				sessao.getEncerraEm(), sessaoVotacaoService.statusAtual(sessao));
	}

}
