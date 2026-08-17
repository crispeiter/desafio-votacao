package com.db.votacao.tela;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.db.votacao.TestcontainersConfiguration;
import com.db.votacao.pauta.Pauta;
import com.db.votacao.pauta.PautaRepository;
import com.db.votacao.sessao.SessaoVotacao;
import com.db.votacao.sessao.SessaoVotacaoRepository;
import com.db.votacao.voto.VotoRepository;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, properties = {
		"votacao.cpf.modo=SEMPRE_APTO",
		// Base distinta da porta do servidor de teste: se o assembler montasse a URL a partir da
		// requisicao em vez da propriedade, a assercao apontaria para localhost e falharia.
		"votacao.callback-base-url=http://mobile.teste:9000" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TelaApiIntegracaoTest {

	private static final String BASE_DE_CALLBACK = "http://mobile.teste:9000/api/v1/telas/pautas/";

	private static final String CPF_PRIMEIRO = "12345678909";
	private static final String CPF_SEGUNDO = "52998224725";

	private static final String TITULO_ERRO_VOTACAO = "Não foi possível votar";
	private static final String TITULO_ERRO_RESULTADO = "Não foi possível exibir o resultado";

	private static final int PORTA = portaLivre();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Autowired
	private VotoRepository votoRepository;

	@Autowired
	private Clock clock;

	@DynamicPropertySource
	static void apontarProvedorDeCpfParaOServidorDoTeste(DynamicPropertyRegistry registro) {
		registro.add("server.port", () -> PORTA);
		registro.add("votacao.cpf.base-url", () -> "http://localhost:" + PORTA);
	}

	@BeforeEach
	void limparBase() {
		votoRepository.deleteAllInBatch();
		sessaoVotacaoRepository.deleteAllInBatch();
		pautaRepository.deleteAllInBatch();
	}

	@Test
	void deveListarApenasPautasComSessaoAbertaNaTelaDeSelecao() throws Exception {
		Pauta emVotacao = pautaComSessaoAberta("Reforma do estatuto social");
		pautaComSessaoEncerrada("Prestacao de contas de 2025");
		pautaRepository.save(new Pauta("Pauta ainda sem sessao", null, clock.instant()));

		mockMvc.perform(get("/api/v1/telas/pautas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("SELECAO"))
				.andExpect(jsonPath("$.titulo").value("Pautas em votação"))
				.andExpect(jsonPath("$.itens", hasSize(1)))
				.andExpect(jsonPath("$.itens[0].titulo").value("Reforma do estatuto social"))
				.andExpect(jsonPath("$.itens[0].descricao").value(containsString("Encerra em ")))
				.andExpect(jsonPath("$.itens[0].url").value(BASE_DE_CALLBACK + emVotacao.getId() + "/votacao"))
				.andExpect(jsonPath("$.itens[0].body.pautaId").value(emVotacao.getId()));
	}

	@Test
	void deveDevolverSelecaoVaziaQuandoNenhumaSessaoEstaAberta() throws Exception {
		pautaComSessaoEncerrada("Prestacao de contas de 2025");

		mockMvc.perform(get("/api/v1/telas/pautas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("SELECAO"))
				.andExpect(jsonPath("$.itens", hasSize(0)));
	}

	@Test
	void deveMontarFormularioDeVotacaoComCampoDeCpfEOsDoisBotoes() throws Exception {
		Long pautaId = pautaComSessaoAberta("Reforma do estatuto social").getId();

		mockMvc.perform(post("/api/v1/telas/pautas/{pautaId}/votacao", pautaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$.titulo").value("Reforma do estatuto social"))
				.andExpect(jsonPath("$.itens", hasSize(1)))
				.andExpect(jsonPath("$.itens[0].id").value("cpf"))
				.andExpect(jsonPath("$.itens[0].tipo").value("TEXTO"))
				.andExpect(jsonPath("$.botoes", hasSize(2)))
				.andExpect(jsonPath("$.botoes[0].titulo").value("Sim"))
				.andExpect(jsonPath("$.botoes[0].url").value(BASE_DE_CALLBACK + pautaId + "/voto"))
				.andExpect(jsonPath("$.botoes[0].body.opcao").value("SIM"))
				.andExpect(jsonPath("$.botoes[1].titulo").value("Não"))
				.andExpect(jsonPath("$.botoes[1].url").value(BASE_DE_CALLBACK + pautaId + "/voto"))
				.andExpect(jsonPath("$.botoes[1].body.opcao").value("NAO"));
	}

	@Test
	void deveRegistrarOVotoDaTelaEDevolverAConfirmacao() throws Exception {
		Long pautaId = pautaComSessaoAberta("Reforma do estatuto social").getId();

		votarPelaTela(pautaId, CPF_PRIMEIRO, "SIM")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$.titulo").value("Voto registrado"))
				.andExpect(jsonPath("$.itens", hasSize(1)))
				.andExpect(jsonPath("$.itens[0].id").value("mensagem"))
				.andExpect(jsonPath("$.itens[0].tipo").value("INFORMACAO"))
				.andExpect(jsonPath("$.itens[0].titulo").value(containsString("SIM")))
				.andExpect(jsonPath("$.botoes", hasSize(1)))
				.andExpect(jsonPath("$.botoes[0].titulo").value("Ver resultado"))
				.andExpect(jsonPath("$.botoes[0].url").value(BASE_DE_CALLBACK + pautaId + "/resultado"));

		// A tela nao e fachada: o voto tem que estar gravado na mesma base que a API REST apura.
		mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", pautaId))
				.andExpect(jsonPath("$.totalVotos").value(1))
				.andExpect(jsonPath("$.votosSim").value(1));
	}

	@Test
	void deveExibirApuracaoComOsCincoItensDeInformacao() throws Exception {
		Long pautaId = pautaComSessaoAberta("Reforma do estatuto social").getId();
		votarPelaTela(pautaId, CPF_PRIMEIRO, "SIM").andExpect(status().isOk());
		votarPelaTela(pautaId, CPF_SEGUNDO, "NAO").andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/telas/pautas/{pautaId}/resultado", pautaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$.titulo").value("Reforma do estatuto social"))
				.andExpect(jsonPath("$.itens", hasSize(5)))
				.andExpect(jsonPath("$.itens[0].id").value("situacao"))
				.andExpect(jsonPath("$.itens[0].titulo").value(containsString("aberta")))
				.andExpect(jsonPath("$.itens[1].id").value("sim"))
				.andExpect(jsonPath("$.itens[1].titulo").value("Sim: 1"))
				.andExpect(jsonPath("$.itens[2].id").value("nao"))
				.andExpect(jsonPath("$.itens[2].titulo").value("Não: 1"))
				.andExpect(jsonPath("$.itens[3].id").value("total"))
				.andExpect(jsonPath("$.itens[3].titulo").value("Total de votos: 2"))
				.andExpect(jsonPath("$.itens[4].id").value("resultado"))
				.andExpect(jsonPath("$.itens[4].titulo").value("Resultado: EMPATE"))
				.andExpect(jsonPath("$.itens[*].tipo", everyItem(equalTo("INFORMACAO"))))
				.andExpect(jsonPath("$.botoes", hasSize(0)));
	}

	@Test
	void deveDevolverTelaDeErroComStatus200QuandoOAssociadoJaVotou() throws Exception {
		Long pautaId = pautaComSessaoAberta("Reforma do estatuto social").getId();
		votarPelaTela(pautaId, CPF_PRIMEIRO, "SIM").andExpect(status().isOk());

		votarPelaTela(pautaId, CPF_PRIMEIRO, "NAO")
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$.titulo").value(TITULO_ERRO_VOTACAO))
				.andExpect(jsonPath("$.itens[0].id").value("mensagem"))
				.andExpect(jsonPath("$.itens[0].tipo").value("INFORMACAO"))
				.andExpect(jsonPath("$.itens[0].titulo").value(containsString("já votou")))
				.andExpect(jsonPath("$.botoes", hasSize(0)))
				// O aplicativo so sabe renderizar tela: nenhum campo de ProblemDetail pode vazar.
				.andExpect(jsonPath("$.status").doesNotExist())
				.andExpect(jsonPath("$.detail").doesNotExist())
				.andExpect(jsonPath("$.type").doesNotExist());
	}

	@Test
	void deveDevolverTelaDeErroQuandoOCpfVemEmBranco() throws Exception {
		Long pautaId = pautaComSessaoAberta("Reforma do estatuto social").getId();

		votarPelaTela(pautaId, " ", "SIM")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$.titulo").value(TITULO_ERRO_VOTACAO))
				// A mensagem da constraint ja e texto para o associado e chega inteira na tela.
				.andExpect(jsonPath("$.itens[0].titulo").value(containsString("CPF é obrigatório")))
				.andExpect(jsonPath("$.status").doesNotExist());
	}

	@Test
	void deveDevolverTelaDeErroQuandoOCorpoDaRequisicaoEIlegivel() throws Exception {
		Long pautaId = pautaComSessaoAberta("Reforma do estatuto social").getId();

		mockMvc.perform(post("/api/v1/telas/pautas/{pautaId}/voto", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cpf\": \"12345678909\", "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$.titulo").value(TITULO_ERRO_VOTACAO))
				.andExpect(jsonPath("$.itens[0].titulo").value(containsString("ler os dados enviados")))
				.andExpect(jsonPath("$.status").doesNotExist());
	}

	@Test
	void deveDevolverTelaDeErroQuandoAOpcaoDeVotoNaoExiste() throws Exception {
		Long pautaId = pautaComSessaoAberta("Reforma do estatuto social").getId();

		votarPelaTela(pautaId, CPF_PRIMEIRO, "TALVEZ")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$.itens[0].titulo").value(containsString("ler os dados enviados")));
	}

	@Test
	void deveTitularATelaDeErroConformeOEndpointQueFalhou() throws Exception {
		Long semSessao = pautaRepository.save(new Pauta("Pauta sem sessao", null, clock.instant())).getId();

		mockMvc.perform(post("/api/v1/telas/pautas/{pautaId}/resultado", semSessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value(TITULO_ERRO_RESULTADO));

		mockMvc.perform(post("/api/v1/telas/pautas/{pautaId}/votacao", 404L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value(TITULO_ERRO_VOTACAO));
	}

	private ResultActions votarPelaTela(Long pautaId, String cpf, String opcao) throws Exception {
		return mockMvc.perform(post("/api/v1/telas/pautas/{pautaId}/voto", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cpf\": \"%s\", \"opcao\": \"%s\"}".formatted(cpf, opcao)));
	}

	// Sessao montada direto no repositorio, e nao pela API: e o que permite testar sessao
	// encerrada sem esperar o relogio real correr.
	private Pauta pautaComSessaoAberta(String titulo) {
		return comSessao(titulo, clock.instant().minusSeconds(60), clock.instant().plusSeconds(300));
	}

	private Pauta pautaComSessaoEncerrada(String titulo) {
		return comSessao(titulo, clock.instant().minusSeconds(600), clock.instant().minusSeconds(60));
	}

	private Pauta comSessao(String titulo, Instant abertaEm, Instant encerraEm) {
		Pauta pauta = pautaRepository.save(new Pauta(titulo, null, clock.instant()));
		sessaoVotacaoRepository.save(new SessaoVotacao(pauta, abertaEm, encerraEm));
		return pauta;
	}

	private static int portaLivre() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException erro) {
			throw new UncheckedIOException("Nao foi possivel reservar porta para o servidor de teste", erro);
		}
	}

}
