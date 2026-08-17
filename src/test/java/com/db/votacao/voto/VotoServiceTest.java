package com.db.votacao.voto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.db.votacao.comum.AssociadoNaoAutorizadoException;
import com.db.votacao.comum.ConflitoException;
import com.db.votacao.comum.RecursoNaoEncontradoException;
import com.db.votacao.cpf.AutorizacaoCpfResponse;
import com.db.votacao.cpf.CpfAutorizacaoClient;
import com.db.votacao.cpf.StatusAssociado;
import com.db.votacao.pauta.Pauta;
import com.db.votacao.sessao.SessaoVotacao;
import com.db.votacao.sessao.SessaoVotacaoService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

	private static final Instant AGORA = Instant.parse("2026-08-12T14:00:00Z");
	private static final Long PAUTA_ID = 1L;
	private static final String CPF_COM_MASCARA = "123.456.789-09";
	private static final String CPF_NORMALIZADO = "12345678909";

	@Mock
	private VotoRepository votoRepository;

	@Mock
	private SessaoVotacaoService sessaoVotacaoService;

	@Mock
	private CpfAutorizacaoClient cpfAutorizacaoClient;

	private MeterRegistry registro;
	private VotoService votoService;

	@BeforeEach
	void prepararServico() {
		registro = new SimpleMeterRegistry();
		votoService = new VotoService(votoRepository, sessaoVotacaoService, cpfAutorizacaoClient,
				Clock.fixed(AGORA, ZoneOffset.UTC), registro);
	}

	@Test
	void deveRegistrarVotoDeAssociadoAptoEmSessaoAberta() {
		SessaoVotacao sessao = sessaoQueEncerraEm(AGORA.plusSeconds(60));
		when(sessaoVotacaoService.buscarPorPauta(PAUTA_ID)).thenReturn(sessao);
		when(cpfAutorizacaoClient.consultar(CPF_NORMALIZADO))
				.thenReturn(new AutorizacaoCpfResponse(StatusAssociado.ABLE_TO_VOTE));
		when(votoRepository.save(any(Voto.class))).thenAnswer(chamada -> chamada.getArgument(0));

		Voto voto = votoService.registrarVoto(PAUTA_ID, CPF_COM_MASCARA, OpcaoVoto.SIM);

		ArgumentCaptor<Voto> gravado = ArgumentCaptor.forClass(Voto.class);
		verify(votoRepository).save(gravado.capture());
		assertSame(sessao, voto.getSessao());
		assertEquals(CPF_NORMALIZADO, gravado.getValue().getCpfAssociado());
		assertEquals(OpcaoVoto.SIM, gravado.getValue().getOpcao());
		assertEquals(AGORA, gravado.getValue().getRegistradoEm());
	}

	@Test
	void deveContabilizarVotoRegistradoNaMetricaDaOpcao() {
		SessaoVotacao sessao = sessaoQueEncerraEm(AGORA.plusSeconds(60));
		when(sessaoVotacaoService.buscarPorPauta(PAUTA_ID)).thenReturn(sessao);
		when(cpfAutorizacaoClient.consultar(CPF_NORMALIZADO))
				.thenReturn(new AutorizacaoCpfResponse(StatusAssociado.ABLE_TO_VOTE));
		when(votoRepository.save(any(Voto.class))).thenAnswer(chamada -> chamada.getArgument(0));

		votoService.registrarVoto(PAUTA_ID, CPF_NORMALIZADO, OpcaoVoto.NAO);

		assertEquals(1d, contadorDaOpcao(OpcaoVoto.NAO));
		assertEquals(0d, contadorDaOpcao(OpcaoVoto.SIM));
	}

	@Test
	void deveRejeitarVotoQuandoPautaNaoPossuiSessao() {
		when(sessaoVotacaoService.buscarPorPauta(PAUTA_ID)).thenThrow(
				new RecursoNaoEncontradoException("A pauta " + PAUTA_ID + " não possui sessão de votação."));

		assertThrows(RecursoNaoEncontradoException.class,
				() -> votoService.registrarVoto(PAUTA_ID, CPF_NORMALIZADO, OpcaoVoto.SIM));

		verify(votoRepository, never()).save(any());
	}

	@Test
	void deveRejeitarVotoQuandoSessaoJaEncerrada() {
		when(sessaoVotacaoService.buscarPorPauta(PAUTA_ID)).thenReturn(sessaoQueEncerraEm(AGORA));

		assertThrows(ConflitoException.class,
				() -> votoService.registrarVoto(PAUTA_ID, CPF_NORMALIZADO, OpcaoVoto.SIM));

		verify(cpfAutorizacaoClient, never()).consultar(any());
		verify(votoRepository, never()).save(any());
	}

	@Test
	void deveRejeitarVotoDeAssociadoSemAutorizacaoParaVotar() {
		when(sessaoVotacaoService.buscarPorPauta(PAUTA_ID)).thenReturn(sessaoQueEncerraEm(AGORA.plusSeconds(60)));
		when(cpfAutorizacaoClient.consultar(CPF_NORMALIZADO))
				.thenReturn(new AutorizacaoCpfResponse(StatusAssociado.UNABLE_TO_VOTE));

		assertThrows(AssociadoNaoAutorizadoException.class,
				() -> votoService.registrarVoto(PAUTA_ID, CPF_NORMALIZADO, OpcaoVoto.SIM));

		verify(votoRepository, never()).save(any());
	}

	@Test
	void deveRejeitarSegundoVotoDoMesmoAssociadoTraduzindoViolacaoDeUnicidade() {
		when(sessaoVotacaoService.buscarPorPauta(PAUTA_ID)).thenReturn(sessaoQueEncerraEm(AGORA.plusSeconds(60)));
		when(cpfAutorizacaoClient.consultar(CPF_NORMALIZADO))
				.thenReturn(new AutorizacaoCpfResponse(StatusAssociado.ABLE_TO_VOTE));
		when(votoRepository.save(any(Voto.class)))
				.thenThrow(new DataIntegrityViolationException("uk_voto_sessao_associado"));

		assertThrows(ConflitoException.class,
				() -> votoService.registrarVoto(PAUTA_ID, CPF_NORMALIZADO, OpcaoVoto.SIM));

		assertEquals(0d, contadorDaOpcao(OpcaoVoto.SIM));
	}

	private static SessaoVotacao sessaoQueEncerraEm(Instant encerraEm) {
		Pauta pauta = new Pauta("Reforma do estatuto social", null, AGORA.minusSeconds(600));
		return new SessaoVotacao(pauta, AGORA.minusSeconds(30), encerraEm);
	}

	private double contadorDaOpcao(OpcaoVoto opcao) {
		return registro.get("votacao.votos.registrados").tag("opcao", opcao.name()).counter().count();
	}

}
