package com.db.votacao.sessao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.db.votacao.comum.ConflitoException;
import com.db.votacao.comum.RecursoNaoEncontradoException;
import com.db.votacao.config.VotacaoProperties;
import com.db.votacao.cpf.ModoValidacaoCpf;
import com.db.votacao.pauta.Pauta;
import com.db.votacao.pauta.PautaService;

@ExtendWith(MockitoExtension.class)
class SessaoVotacaoServiceTest {

	private static final Instant AGORA = Instant.parse("2026-08-12T14:00:00Z");
	private static final Long PAUTA_ID = 1L;

	@Mock
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Mock
	private PautaService pautaService;

	private Pauta pauta;
	private SessaoVotacaoService sessaoVotacaoService;

	@BeforeEach
	void prepararServico() {
		pauta = new Pauta("Reforma do estatuto social", "Alteracao dos artigos 12 e 15", AGORA);
		sessaoVotacaoService = new SessaoVotacaoService(sessaoVotacaoRepository, pautaService,
				propriedadesComDuracaoPadrao(Duration.ofSeconds(60)),
				Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	@Test
	void deveAbrirSessaoComDuracaoPadraoQuandoDuracaoNaoInformada() {
		when(pautaService.buscarPorId(PAUTA_ID)).thenReturn(pauta);
		when(sessaoVotacaoRepository.existsByPautaId(PAUTA_ID)).thenReturn(false);
		when(sessaoVotacaoRepository.save(any(SessaoVotacao.class))).thenAnswer(chamada -> chamada.getArgument(0));

		SessaoVotacao sessao = sessaoVotacaoService.abrir(PAUTA_ID, null);

		assertSame(pauta, sessao.getPauta());
		assertEquals(AGORA, sessao.getAbertaEm());
		assertEquals(AGORA.plusSeconds(60), sessao.getEncerraEm());
	}

	@Test
	void deveAbrirSessaoComDuracaoInformadaEmSegundos() {
		when(pautaService.buscarPorId(PAUTA_ID)).thenReturn(pauta);
		when(sessaoVotacaoRepository.existsByPautaId(PAUTA_ID)).thenReturn(false);
		when(sessaoVotacaoRepository.save(any(SessaoVotacao.class))).thenAnswer(chamada -> chamada.getArgument(0));

		sessaoVotacaoService.abrir(PAUTA_ID, 300);

		ArgumentCaptor<SessaoVotacao> capturada = ArgumentCaptor.forClass(SessaoVotacao.class);
		verify(sessaoVotacaoRepository).save(capturada.capture());
		assertEquals(AGORA, capturada.getValue().getAbertaEm());
		assertEquals(AGORA.plusSeconds(300), capturada.getValue().getEncerraEm());
	}

	@Test
	void deveRejeitarAberturaQuandoPautaNaoExiste() {
		when(pautaService.buscarPorId(PAUTA_ID))
				.thenThrow(new RecursoNaoEncontradoException("Pauta " + PAUTA_ID + " não encontrada."));

		assertThrows(RecursoNaoEncontradoException.class, () -> sessaoVotacaoService.abrir(PAUTA_ID, null));

		verify(sessaoVotacaoRepository, never()).save(any());
	}

	@Test
	void deveRejeitarAberturaQuandoPautaJaPossuiSessao() {
		when(pautaService.buscarPorId(PAUTA_ID)).thenReturn(pauta);
		when(sessaoVotacaoRepository.existsByPautaId(PAUTA_ID)).thenReturn(true);

		assertThrows(ConflitoException.class, () -> sessaoVotacaoService.abrir(PAUTA_ID, null));

		verify(sessaoVotacaoRepository, never()).save(any());
	}

	private static VotacaoProperties propriedadesComDuracaoPadrao(Duration duracaoPadrao) {
		return new VotacaoProperties(
				new VotacaoProperties.Sessao(duracaoPadrao),
				new VotacaoProperties.ValidacaoCpf(ModoValidacaoCpf.SEMPRE_APTO, "http://localhost:8080",
						Duration.ofSeconds(2), Duration.ofSeconds(3)),
				"http://localhost:8080");
	}

}
