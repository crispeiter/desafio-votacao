package com.db.votacao.voto;

import java.net.URI;

import org.springframework.http.ProblemDetail;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Voto registrado, com a URI da apuração no header "
					+ "Location"),
			@ApiResponse(responseCode = "400", description = "CPF ausente ou opção diferente de SIM e NAO",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "404", description = "Pauta sem sessão de votação, ou associado não apto a "
					+ "votar segundo o provedor de autorização",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "409", description = "Sessão de votação encerrada, ou associado que já votou "
					+ "nesta pauta",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			// Unico endpoint com 500 documentado: e o unico que depende de provedor externo,
			// entao aqui a indisponibilidade dele faz parte do contrato, nao e falha imprevista.
			@ApiResponse(responseCode = "500", description = "Falha na consulta ao provedor de autorização, por "
					+ "indisponibilidade ou timeout",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
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
