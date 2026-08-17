package com.db.votacao.voto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.db.votacao.TestcontainersConfiguration;
import com.db.votacao.pauta.Pauta;
import com.db.votacao.sessao.SessaoVotacao;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class VotoRepositoryTest {

	private static final Instant AGORA = Instant.parse("2026-08-12T14:00:00Z");
	private static final String CPF = "12345678909";
	private static final String OUTRO_CPF = "52998224725";

	@Autowired
	private VotoRepository votoRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void deveRejeitarSegundoVotoDoMesmoAssociadoNaMesmaSessao() {
		SessaoVotacao sessao = novaSessao("Reforma do estatuto social");
		votoRepository.saveAndFlush(new Voto(sessao, CPF, OpcaoVoto.SIM, AGORA));

		Voto segundoVoto = new Voto(sessao, CPF, OpcaoVoto.NAO, AGORA.plusSeconds(5));
		DataIntegrityViolationException excecao = assertThrows(DataIntegrityViolationException.class,
				() -> votoRepository.saveAndFlush(segundoVoto));

		// A unicidade e do banco, e nao de uma checagem previa no servico: e a constraint que
		// falha aqui, e o nome dela no erro prova qual regra reagiu.
		assertTrue(excecao.getMostSpecificCause().getMessage().contains("uk_voto_sessao_associado"));
	}

	@Test
	void devePermitirQueOMesmoAssociadoVoteEmSessoesDiferentes() {
		votoRepository.saveAndFlush(new Voto(novaSessao("Primeira pauta"), CPF, OpcaoVoto.SIM, AGORA));
		votoRepository.saveAndFlush(new Voto(novaSessao("Segunda pauta"), CPF, OpcaoVoto.NAO, AGORA));

		assertEquals(2, votoRepository.count());
	}

	@Test
	void devePermitirVotosDeAssociadosDiferentesNaMesmaSessao() {
		SessaoVotacao sessao = novaSessao("Reforma do estatuto social");
		votoRepository.saveAndFlush(new Voto(sessao, CPF, OpcaoVoto.SIM, AGORA));
		votoRepository.saveAndFlush(new Voto(sessao, OUTRO_CPF, OpcaoVoto.SIM, AGORA));

		assertEquals(2, votoRepository.count());
	}

	@Test
	void deveContarVotosAgrupadosPorOpcao() {
		SessaoVotacao sessao = novaSessao("Reforma do estatuto social");
		registrar(sessao, OpcaoVoto.SIM, 3);
		registrar(sessao, OpcaoVoto.NAO, 2);

		Map<OpcaoVoto, Long> contagens = contagensDe(sessao);

		assertEquals(Map.of(OpcaoVoto.SIM, 3L, OpcaoVoto.NAO, 2L), contagens);
	}

	@Test
	void deveOmitirDaContagemAOpcaoQueNaoRecebeuVoto() {
		SessaoVotacao sessao = novaSessao("Reforma do estatuto social");
		registrar(sessao, OpcaoVoto.SIM, 2);

		List<ContagemPorOpcao> contagens = votoRepository.contarPorOpcao(sessao.getId());

		assertEquals(1, contagens.size());
		assertEquals(new ContagemPorOpcao(OpcaoVoto.SIM, 2L), contagens.get(0));
	}

	@Test
	void deveDevolverContagemVaziaQuandoSessaoNaoRecebeuVoto() {
		SessaoVotacao sessao = novaSessao("Reforma do estatuto social");

		assertTrue(votoRepository.contarPorOpcao(sessao.getId()).isEmpty());
	}

	@Test
	void deveContarApenasOsVotosDaSessaoInformada() {
		SessaoVotacao sessao = novaSessao("Reforma do estatuto social");
		SessaoVotacao outraSessao = novaSessao("Outra pauta em votacao");
		registrar(sessao, OpcaoVoto.SIM, 2);
		registrar(outraSessao, OpcaoVoto.SIM, 5);
		registrar(outraSessao, OpcaoVoto.NAO, 4);

		Map<OpcaoVoto, Long> contagens = contagensDe(sessao);

		assertEquals(Map.of(OpcaoVoto.SIM, 2L), contagens);
	}

	private Map<OpcaoVoto, Long> contagensDe(SessaoVotacao sessao) {
		return votoRepository.contarPorOpcao(sessao.getId()).stream()
				.collect(Collectors.toMap(ContagemPorOpcao::opcao, ContagemPorOpcao::quantidade));
	}

	private void registrar(SessaoVotacao sessao, OpcaoVoto opcao, int quantidade) {
		for (int indice = 0; indice < quantidade; indice++) {
			votoRepository.save(new Voto(sessao, cpfSequencial(sessao, opcao, indice), opcao, AGORA));
		}
		entityManager.flush();
	}

	// A unicidade e por sessao e CPF, entao cada voto do lote precisa de um CPF proprio. O valor
	// nao passa por validacao aqui: o repositorio grava o que o servico ja validou.
	private static String cpfSequencial(SessaoVotacao sessao, OpcaoVoto opcao, int indice) {
		return "%03d%s%06d".formatted(sessao.getId(), opcao == OpcaoVoto.SIM ? "1" : "2", indice);
	}

	private SessaoVotacao novaSessao(String titulo) {
		Pauta pauta = entityManager.persist(new Pauta(titulo, null, AGORA.minusSeconds(600)));
		return entityManager.persist(new SessaoVotacao(pauta, AGORA, AGORA.plusSeconds(600)));
	}

}
