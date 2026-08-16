package com.db.votacao.voto;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.db.votacao.voto.dto.ResultadoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Resultado", description = "Apuração dos votos de uma pauta")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/resultado")
public class ApuracaoController {

	private final ApuracaoService apuracaoService;

	public ApuracaoController(ApuracaoService apuracaoService) {
		this.apuracaoService = apuracaoService;
	}

	@Operation(summary = "Apura o resultado da votação de uma pauta",
			description = "Com a sessão ainda aberta a apuração é parcial e vem com statusSessao ABERTA. "
					+ "Sem nenhum voto o resultado é SEM_VOTOS, que é situação distinta de EMPATE.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Apuração da pauta, parcial enquanto a sessão estiver "
					+ "aberta"),
			@ApiResponse(responseCode = "404", description = "Pauta inexistente ou sem sessão de votação: não existe "
					+ "resultado de uma votação que nunca começou",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping
	public ResultadoResponse apurar(@PathVariable Long pautaId) {
		return paraResponse(apuracaoService.apurar(pautaId));
	}

	private static ResultadoResponse paraResponse(Apuracao apuracao) {
		return new ResultadoResponse(apuracao.pautaId(), apuracao.titulo(), apuracao.statusSessao(),
				apuracao.totalVotos(), apuracao.votosSim(), apuracao.votosNao(), apuracao.resultado());
	}

}
