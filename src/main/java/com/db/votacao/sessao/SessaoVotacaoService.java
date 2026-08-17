package com.db.votacao.sessao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.db.votacao.comum.ConflitoException;
import com.db.votacao.comum.RecursoNaoEncontradoException;
import com.db.votacao.config.VotacaoProperties;
import com.db.votacao.pauta.Pauta;
import com.db.votacao.pauta.PautaService;

@Service
public class SessaoVotacaoService {

	private static final Logger log = LoggerFactory.getLogger(SessaoVotacaoService.class);

	private final SessaoVotacaoRepository sessaoVotacaoRepository;
	private final PautaService pautaService;
	private final VotacaoProperties propriedades;
	private final Clock clock;

	public SessaoVotacaoService(SessaoVotacaoRepository sessaoVotacaoRepository, PautaService pautaService,
			VotacaoProperties propriedades, Clock clock) {
		this.sessaoVotacaoRepository = sessaoVotacaoRepository;
		this.pautaService = pautaService;
		this.propriedades = propriedades;
		this.clock = clock;
	}

	@Transactional
	public SessaoVotacao abrir(Long pautaId, Integer duracaoEmSegundos) {
		Pauta pauta = pautaService.buscarPorId(pautaId);

		// Diferente do voto, aqui a verificacao previa e adequada: abrir sessao e ato
		// administrativo unico por pauta, sem concorrencia realista de duas aberturas
		// simultaneas. A uk_sessao_pauta permanece como rede de seguranca no banco.
		if (sessaoVotacaoRepository.existsByPautaId(pautaId)) {
			throw new ConflitoException("A pauta " + pautaId + " já possui sessão de votação.");
		}

		Instant abertaEm = clock.instant();
		Instant encerraEm = abertaEm.plus(duracao(duracaoEmSegundos));

		SessaoVotacao sessao = sessaoVotacaoRepository.save(new SessaoVotacao(pauta, abertaEm, encerraEm));
		log.info("Sessao aberta. pautaId={} sessaoId={} encerraEm={}", pautaId, sessao.getId(), encerraEm);
		return sessao;
	}

	@Transactional(readOnly = true)
	public SessaoVotacao buscarPorPauta(Long pautaId) {
		return sessaoVotacaoRepository.findByPautaId(pautaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"A pauta " + pautaId + " não possui sessão de votação."));
	}

	@Transactional(readOnly = true)
	public List<SessaoVotacao> listarAbertas() {
		return sessaoVotacaoRepository.buscarAbertasEm(clock.instant());
	}

	public StatusSessao statusAtual(SessaoVotacao sessao) {
		return sessao.statusEm(clock.instant());
	}

	private Duration duracao(Integer duracaoEmSegundos) {
		return duracaoEmSegundos == null
				? propriedades.sessao().duracaoPadrao()
				: Duration.ofSeconds(duracaoEmSegundos);
	}

}
