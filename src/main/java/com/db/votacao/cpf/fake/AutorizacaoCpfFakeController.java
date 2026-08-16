package com.db.votacao.cpf.fake;

import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.db.votacao.config.VotacaoProperties;
import com.db.votacao.cpf.AutorizacaoCpfResponse;
import com.db.votacao.cpf.Cpf;
import com.db.votacao.cpf.ModoValidacaoCpf;
import com.db.votacao.cpf.StatusAssociado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Vive fora do /api/v1 porque nao faz parte da API de votacao: e o provedor externo simulado,
// alcancado por chamada HTTP real do CpfAutorizacaoClient.
@Tag(name = "Autorização de CPF (provedor simulado)",
		description = "Simula o sistema externo de autorização de associados. Não faz parte da API de votação.")
@RestController
@RequestMapping("/fake/autorizacao")
public class AutorizacaoCpfFakeController {

	private static final Logger log = LoggerFactory.getLogger(AutorizacaoCpfFakeController.class);

	private final ModoValidacaoCpf modo;

	public AutorizacaoCpfFakeController(VotacaoProperties propriedades) {
		this.modo = propriedades.cpf().modo();
	}

	@Operation(summary = "Consulta se o portador do CPF está apto a votar",
			description = "CPF com formato ou dígito verificador inválido devolve 404. CPF válido devolve 200 com "
					+ "ABLE_TO_VOTE ou UNABLE_TO_VOTE, sorteados no modo ALEATORIO e sempre apto no modo SEMPRE_APTO.")
	@GetMapping("/{cpf}")
	public ResponseEntity<AutorizacaoCpfResponse> consultar(@PathVariable String cpf) {
		// 404 para CPF malformado, e nao 400, por exigencia explicita do enunciado.
		if (!Cpf.valido(cpf)) {
			log.debug("Consulta de autorizacao recusada por CPF invalido");
			return ResponseEntity.notFound().build();
		}
		StatusAssociado status = decidirStatus();
		log.debug("Consulta de autorizacao respondida. modo={} status={}", modo, status);
		return ResponseEntity.ok(new AutorizacaoCpfResponse(status));
	}

	private StatusAssociado decidirStatus() {
		if (modo == ModoValidacaoCpf.SEMPRE_APTO) {
			return StatusAssociado.ABLE_TO_VOTE;
		}
		return ThreadLocalRandom.current().nextBoolean()
				? StatusAssociado.ABLE_TO_VOTE
				: StatusAssociado.UNABLE_TO_VOTE;
	}

}
