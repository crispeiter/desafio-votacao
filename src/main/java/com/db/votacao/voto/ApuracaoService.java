package com.db.votacao.voto;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.db.votacao.sessao.SessaoVotacao;
import com.db.votacao.sessao.SessaoVotacaoService;

@Service
public class ApuracaoService {

	private static final Logger log = LoggerFactory.getLogger(ApuracaoService.class);

	private final VotoRepository votoRepository;
	private final SessaoVotacaoService sessaoVotacaoService;

	public ApuracaoService(VotoRepository votoRepository, SessaoVotacaoService sessaoVotacaoService) {
		this.votoRepository = votoRepository;
		this.sessaoVotacaoService = sessaoVotacaoService;
	}

	@Transactional(readOnly = true)
	public Apuracao apurar(Long pautaId) {
		SessaoVotacao sessao = sessaoVotacaoService.buscarPorPauta(pautaId);

		// Contagem agregada no banco: com centenas de milhares de votos, trazer as linhas para
		// somar em memoria custaria a tabela inteira por consulta.
		List<ContagemPorOpcao> contagens = votoRepository.contarPorOpcao(sessao.getId());

		// Sessao aberta devolve parcial, e nao erro: acompanhar a votacao em tempo real e o
		// comportamento util aqui. Sigilo ate o encerramento seria zerar as contagens.
		Apuracao apuracao = new Apuracao(pautaId, sessao.getPauta().getTitulo(),
				sessaoVotacaoService.statusAtual(sessao), quantidade(contagens, OpcaoVoto.SIM),
				quantidade(contagens, OpcaoVoto.NAO));

		log.info("Apuracao. pautaId={} sim={} nao={} resultado={}", pautaId, apuracao.votosSim(),
				apuracao.votosNao(), apuracao.resultado());
		return apuracao;
	}

	// A query agrupa por opcao e so devolve linha para opcao votada: opcao sem voto simplesmente
	// nao aparece no resultado.
	private static long quantidade(List<ContagemPorOpcao> contagens, OpcaoVoto opcao) {
		return contagens.stream()
				.filter(contagem -> contagem.opcao() == opcao)
				.mapToLong(ContagemPorOpcao::quantidade)
				.findFirst()
				.orElse(0L);
	}

}
