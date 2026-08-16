package com.db.votacao.voto;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.db.votacao.comum.AssociadoNaoAutorizadoException;
import com.db.votacao.comum.ConflitoException;
import com.db.votacao.cpf.Cpf;
import com.db.votacao.cpf.CpfAutorizacaoClient;
import com.db.votacao.cpf.StatusAssociado;
import com.db.votacao.sessao.SessaoVotacao;
import com.db.votacao.sessao.SessaoVotacaoService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class VotoService {

	private static final Logger log = LoggerFactory.getLogger(VotoService.class);

	private final VotoRepository votoRepository;
	private final SessaoVotacaoService sessaoVotacaoService;
	private final CpfAutorizacaoClient cpfAutorizacaoClient;
	private final Clock clock;
	private final Map<OpcaoVoto, Counter> votosRegistrados;

	public VotoService(VotoRepository votoRepository, SessaoVotacaoService sessaoVotacaoService,
			CpfAutorizacaoClient cpfAutorizacaoClient, Clock clock, MeterRegistry registro) {
		this.votoRepository = votoRepository;
		this.sessaoVotacaoService = sessaoVotacaoService;
		this.cpfAutorizacaoClient = cpfAutorizacaoClient;
		this.clock = clock;
		this.votosRegistrados = contadoresPorOpcao(registro);
	}

	// Os dois contadores nascem no start, e nao no primeiro voto de cada opcao: serie que so
	// aparece depois do primeiro incremento vira buraco no grafico e alerta sem dado.
	private static Map<OpcaoVoto, Counter> contadoresPorOpcao(MeterRegistry registro) {
		Map<OpcaoVoto, Counter> contadores = new EnumMap<>(OpcaoVoto.class);
		for (OpcaoVoto opcao : OpcaoVoto.values()) {
			contadores.put(opcao, Counter.builder("votacao.votos.registrados")
					.description("Total de votos registrados por opcao")
					.tag("opcao", opcao.name())
					.register(registro));
		}
		return contadores;
	}

	@Transactional
	public Voto registrarVoto(Long pautaId, String cpf, OpcaoVoto opcao) {
		SessaoVotacao sessao = sessaoVotacaoService.buscarPorPauta(pautaId);

		if (!sessao.estaAberta(clock.instant())) {
			throw new ConflitoException("A sessão de votação da pauta " + pautaId + " está encerrada.");
		}

		String cpfAssociado = Cpf.normalizar(cpf);
		garantirQueEstaApto(cpfAssociado);

		return gravar(sessao, pautaId, cpfAssociado, opcao);
	}

	private void garantirQueEstaApto(String cpfAssociado) {
		// Quem valida formato e digito verificador e o provedor, nao esta camada: uma checagem
		// local antes da chamada duplicaria a regra dele e mascararia divergencia de contrato.
		// Provedor respondendo 404 ja chega aqui como RecursoNaoEncontradoException.
		if (cpfAutorizacaoClient.consultar(cpfAssociado).status() != StatusAssociado.ABLE_TO_VOTE) {
			// 404, e nao 403, por instrucao explicita do enunciado.
			throw new AssociadoNaoAutorizadoException("O associado não está apto a votar.");
		}
	}

	private Voto gravar(SessaoVotacao sessao, Long pautaId, String cpfAssociado, OpcaoVoto opcao) {
		// Sem consulta de existencia antes do insert: entre o select e o insert cabe o voto de
		// uma requisicao concorrente do mesmo CPF, e as duas passariam. A unicidade e da
		// constraint uk_voto_sessao_associado, e a violacao vira conflito aqui. O insert sai
		// dentro do save, e nao no commit, porque o id vem de IDENTITY e o Hibernate precisa
		// do banco para obte-lo, entao a violacao e capturavel neste try.
		try {
			Voto voto = votoRepository.save(new Voto(sessao, cpfAssociado, opcao, clock.instant()));
			log.info("Voto registrado. pautaId={} sessaoId={} opcao={}", pautaId, sessao.getId(), opcao);
			votosRegistrados.get(opcao).increment();
			return voto;
		} catch (DataIntegrityViolationException excecao) {
			log.info("Tentativa de voto duplicado. pautaId={}", pautaId);
			throw new ConflitoException("O associado já votou nesta pauta.");
		}
	}

}
