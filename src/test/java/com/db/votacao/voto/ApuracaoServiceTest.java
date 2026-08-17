package com.db.votacao.voto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.db.votacao.pauta.Pauta;
import com.db.votacao.sessao.SessaoVotacao;
import com.db.votacao.sessao.SessaoVotacaoService;
import com.db.votacao.sessao.StatusSessao;

@ExtendWith(MockitoExtension.class)
class ApuracaoServiceTest {

	private static final Instant AGORA = Instant.parse("2026-08-12T14:00:00Z");
	private static final Long PAUTA_ID = 1L;
	private static final String TITULO = "Reforma do estatuto social";

	@Mock
	private VotoRepository votoRepository;

	@Mock
	private SessaoVotacaoService sessaoVotacaoService;

	private ApuracaoService apuracaoService;

	@BeforeEach
	void prepararServico() {
		apuracaoService = new ApuracaoService(votoRepository, sessaoVotacaoService);

		SessaoVotacao sessao = new SessaoVotacao(new Pauta(TITULO, null, AGORA.minusSeconds(600)),
				AGORA.minusSeconds(300), AGORA.minusSeconds(60));
		when(sessaoVotacaoService.buscarPorPauta(PAUTA_ID)).thenReturn(sessao);
		when(sessaoVotacaoService.statusAtual(sessao)).thenReturn(StatusSessao.ENCERRADA);
	}

	@Test
	void deveApurarComoAprovadaQuandoSimSuperaNao() {
		when(votoRepository.contarPorOpcao(any())).thenReturn(List.of(
				new ContagemPorOpcao(OpcaoVoto.SIM, 2100),
				new ContagemPorOpcao(OpcaoVoto.NAO, 1312)));

		Apuracao apuracao = apuracaoService.apurar(PAUTA_ID);

		assertEquals(PAUTA_ID, apuracao.pautaId());
		assertEquals(TITULO, apuracao.titulo());
		assertEquals(StatusSessao.ENCERRADA, apuracao.statusSessao());
		assertEquals(2100, apuracao.votosSim());
		assertEquals(1312, apuracao.votosNao());
		assertEquals(3412, apuracao.totalVotos());
		assertEquals(ResultadoPauta.APROVADA, apuracao.resultado());
	}

	@Test
	void deveApurarComoReprovadaQuandoNaoSuperaSim() {
		when(votoRepository.contarPorOpcao(any())).thenReturn(List.of(
				new ContagemPorOpcao(OpcaoVoto.SIM, 4),
				new ContagemPorOpcao(OpcaoVoto.NAO, 7)));

		Apuracao apuracao = apuracaoService.apurar(PAUTA_ID);

		assertEquals(11, apuracao.totalVotos());
		assertEquals(ResultadoPauta.REPROVADA, apuracao.resultado());
	}

	@Test
	void deveApurarComoEmpateQuandoAsOpcoesTemAMesmaContagem() {
		when(votoRepository.contarPorOpcao(any())).thenReturn(List.of(
				new ContagemPorOpcao(OpcaoVoto.SIM, 5),
				new ContagemPorOpcao(OpcaoVoto.NAO, 5)));

		Apuracao apuracao = apuracaoService.apurar(PAUTA_ID);

		assertEquals(10, apuracao.totalVotos());
		assertEquals(ResultadoPauta.EMPATE, apuracao.resultado());
	}

	@Test
	void deveApurarComoSemVotosQuandoNinguemVotou() {
		when(votoRepository.contarPorOpcao(any())).thenReturn(List.of());

		Apuracao apuracao = apuracaoService.apurar(PAUTA_ID);

		assertEquals(0, apuracao.votosSim());
		assertEquals(0, apuracao.votosNao());
		assertEquals(0, apuracao.totalVotos());
		assertEquals(ResultadoPauta.SEM_VOTOS, apuracao.resultado());
	}

	@Test
	void deveContarZeroNaOpcaoQueNaoRecebeuVotoNenhum() {
		when(votoRepository.contarPorOpcao(any())).thenReturn(List.of(new ContagemPorOpcao(OpcaoVoto.SIM, 3)));

		Apuracao apuracao = apuracaoService.apurar(PAUTA_ID);

		assertEquals(3, apuracao.votosSim());
		assertEquals(0, apuracao.votosNao());
		assertEquals(ResultadoPauta.APROVADA, apuracao.resultado());
	}

}
