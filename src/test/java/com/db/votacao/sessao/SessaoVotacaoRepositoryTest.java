package com.db.votacao.sessao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.db.votacao.TestcontainersConfiguration;
import com.db.votacao.pauta.Pauta;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class SessaoVotacaoRepositoryTest {

	private static final Instant AGORA = Instant.parse("2026-08-12T14:00:00Z");

	@Autowired
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void deveEncontrarSessaoPelaPauta() {
		Pauta pauta = novaPauta("Reforma do estatuto social");
		sessaoVotacaoRepository.save(new SessaoVotacao(pauta, AGORA, AGORA.plusSeconds(60)));
		entityManager.flush();
		entityManager.clear();

		Optional<SessaoVotacao> encontrada = sessaoVotacaoRepository.findByPautaId(pauta.getId());

		assertTrue(encontrada.isPresent());
		assertEquals(AGORA, encontrada.get().getAbertaEm());
		assertEquals(AGORA.plusSeconds(60), encontrada.get().getEncerraEm());
	}

	@Test
	void deveDevolverVazioQuandoPautaNaoTemSessao() {
		Pauta pauta = novaPauta("Pauta sem sessao");

		assertTrue(sessaoVotacaoRepository.findByPautaId(pauta.getId()).isEmpty());
	}

	@Test
	void deveInformarQuePautaJaPossuiSessao() {
		Pauta comSessao = novaPauta("Pauta com sessao");
		Pauta semSessao = novaPauta("Pauta sem sessao");
		sessaoVotacaoRepository.save(new SessaoVotacao(comSessao, AGORA, AGORA.plusSeconds(60)));
		entityManager.flush();

		assertTrue(sessaoVotacaoRepository.existsByPautaId(comSessao.getId()));
		assertFalse(sessaoVotacaoRepository.existsByPautaId(semSessao.getId()));
	}

	@Test
	void deveRejeitarSegundaSessaoParaAMesmaPauta() {
		Pauta pauta = novaPauta("Reforma do estatuto social");
		sessaoVotacaoRepository.saveAndFlush(new SessaoVotacao(pauta, AGORA, AGORA.plusSeconds(60)));

		SessaoVotacao segunda = new SessaoVotacao(pauta, AGORA.plusSeconds(120), AGORA.plusSeconds(180));
		DataIntegrityViolationException excecao = assertThrows(DataIntegrityViolationException.class,
				() -> sessaoVotacaoRepository.saveAndFlush(segunda));

		assertTrue(excecao.getMostSpecificCause().getMessage().contains("uk_sessao_pauta"));
	}

	@Test
	void deveBuscarApenasSessoesAbertasNoInstanteInformado() {
		SessaoVotacao aberta = sessaoDe("Aberta", AGORA.minusSeconds(30), AGORA.plusSeconds(30));
		sessaoDe("Encerrada", AGORA.minusSeconds(120), AGORA.minusSeconds(60));
		sessaoDe("Ainda nao aberta", AGORA.plusSeconds(60), AGORA.plusSeconds(120));
		// A janela e fechada no fim: sessao que encerra exatamente agora ja saiu da lista.
		sessaoDe("Encerrando agora", AGORA.minusSeconds(60), AGORA);
		entityManager.flush();
		entityManager.clear();

		List<SessaoVotacao> abertas = sessaoVotacaoRepository.buscarAbertasEm(AGORA);

		assertEquals(1, abertas.size());
		assertEquals(aberta.getEncerraEm(), abertas.get(0).getEncerraEm());
		assertEquals("Aberta", abertas.get(0).getPauta().getTitulo());
	}

	@Test
	void deveOrdenarSessoesAbertasPeloEncerramentoMaisProximo() {
		sessaoDe("Encerra depois", AGORA.minusSeconds(30), AGORA.plusSeconds(300));
		sessaoDe("Encerra antes", AGORA.minusSeconds(30), AGORA.plusSeconds(60));
		entityManager.flush();
		entityManager.clear();

		List<SessaoVotacao> abertas = sessaoVotacaoRepository.buscarAbertasEm(AGORA);

		assertEquals(List.of("Encerra antes", "Encerra depois"),
				abertas.stream().map(sessao -> sessao.getPauta().getTitulo()).toList());
	}

	@Test
	void deveCarregarPautaJuntoDaSessaoAberta() {
		sessaoDe("Reforma do estatuto social", AGORA.minusSeconds(30), AGORA.plusSeconds(30));
		entityManager.flush();
		entityManager.clear();

		SessaoVotacao aberta = sessaoVotacaoRepository.buscarAbertasEm(AGORA).get(0);

		// Sem o join fetch a pauta viria como proxy e a tela quebraria com open-in-view desligado.
		assertTrue(Hibernate.isInitialized(aberta.getPauta()));
	}

	private SessaoVotacao sessaoDe(String titulo, Instant abertaEm, Instant encerraEm) {
		return sessaoVotacaoRepository.save(new SessaoVotacao(novaPauta(titulo), abertaEm, encerraEm));
	}

	private Pauta novaPauta(String titulo) {
		return entityManager.persist(new Pauta(titulo, null, AGORA.minusSeconds(600)));
	}

}
