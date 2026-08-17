package com.db.votacao.pauta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.db.votacao.TestcontainersConfiguration;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class PautaRepositoryTest {

	private static final Instant CRIADA_EM = Instant.parse("2026-08-12T14:00:00Z");

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void devePersistirPautaAtribuindoIdentificadorGerado() {
		Pauta pauta = pautaRepository.save(new Pauta("Reforma do estatuto social", "Artigos 12 e 15", CRIADA_EM));

		assertNotNull(pauta.getId());
	}

	@Test
	void deveRecuperarPautaComOsDadosGravados() {
		Long id = pautaRepository.save(new Pauta("Reforma do estatuto social", "Artigos 12 e 15", CRIADA_EM)).getId();
		// Sem o clear, o findById devolveria a mesma instancia do contexto de persistencia e o
		// teste passaria sem que nada tivesse ido ao banco.
		entityManager.flush();
		entityManager.clear();

		Optional<Pauta> encontrada = pautaRepository.findById(id);

		assertTrue(encontrada.isPresent());
		assertEquals("Reforma do estatuto social", encontrada.get().getTitulo());
		assertEquals("Artigos 12 e 15", encontrada.get().getDescricao());
		assertEquals(CRIADA_EM, encontrada.get().getCriadaEm());
	}

	@Test
	void devePersistirPautaSemDescricao() {
		Long id = pautaRepository.save(new Pauta("Pauta sem descricao", null, CRIADA_EM)).getId();
		entityManager.flush();
		entityManager.clear();

		assertNull(pautaRepository.findById(id).orElseThrow().getDescricao());
	}

	@Test
	void deveListarTodasAsPautasCadastradas() {
		pautaRepository.save(new Pauta("Primeira pauta", null, CRIADA_EM));
		pautaRepository.save(new Pauta("Segunda pauta", null, CRIADA_EM));
		entityManager.flush();

		List<Pauta> pautas = pautaRepository.findAll();

		assertEquals(2, pautas.size());
		assertTrue(pautas.stream().map(Pauta::getTitulo).toList().containsAll(
				List.of("Primeira pauta", "Segunda pauta")));
	}

	@Test
	void deveDevolverVazioQuandoPautaNaoExiste() {
		assertTrue(pautaRepository.findById(404L).isEmpty());
	}

}
