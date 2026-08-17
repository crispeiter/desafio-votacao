package com.db.votacao;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
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

import com.db.votacao.pauta.PautaRepository;
import com.db.votacao.sessao.SessaoVotacaoRepository;
import com.db.votacao.voto.VotoRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, properties = "votacao.cpf.modo=SEMPRE_APTO")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VotacaoApiIntegracaoTest {

	private static final String CPF_PRIMEIRO = "12345678909";
	private static final String CPF_SEGUNDO = "52998224725";
	private static final String CPF_TERCEIRO = "11144477735";
	private static final String CPF_COM_DIGITO_ERRADO = "12345678900";

	// O bean do client resolve a base-url quando e criado, antes de o servidor publicar
	// local.server.port, entao a porta e reservada aqui e fixada nas duas pontas: o servidor
	// sobe nela e o client de CPF chama o provedor fake por HTTP real nessa mesma porta.
	private static final int PORTA = portaLivre();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Autowired
	private VotoRepository votoRepository;

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
	void deveExecutarOFluxoCompletoDeVotacaoAteAApuracao() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");
		abrirSessao(pautaId, null).andExpect(status().isCreated())
				.andExpect(jsonPath("$.pautaId").value(pautaId))
				.andExpect(jsonPath("$.status").value("ABERTA"));

		votar(pautaId, CPF_PRIMEIRO, "SIM").andExpect(status().isCreated())
				.andExpect(jsonPath("$.opcao").value("SIM"))
				.andExpect(header().string("Location", endsWith("/api/v1/pautas/" + pautaId + "/resultado")));
		votar(pautaId, CPF_SEGUNDO, "SIM").andExpect(status().isCreated());
		votar(pautaId, CPF_TERCEIRO, "NAO").andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", pautaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pautaId").value(pautaId))
				.andExpect(jsonPath("$.statusSessao").value("ABERTA"))
				.andExpect(jsonPath("$.totalVotos").value(3))
				.andExpect(jsonPath("$.votosSim").value(2))
				.andExpect(jsonPath("$.votosNao").value(1))
				.andExpect(jsonPath("$.resultado").value("APROVADA"));
	}

	@Test
	void deveListarAsPautasCadastradas() throws Exception {
		criarPauta("Reforma do estatuto social");
		criarPauta("Prestacao de contas de 2025");

		mockMvc.perform(get("/api/v1/pautas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").exists())
				.andExpect(jsonPath("$[*].titulo").value(
						containsInAnyOrder("Reforma do estatuto social", "Prestacao de contas de 2025")));
	}

	@Test
	void deveBuscarPautaPeloIdentificador() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");

		mockMvc.perform(get("/api/v1/pautas/{id}", pautaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(pautaId))
				.andExpect(jsonPath("$.titulo").value("Reforma do estatuto social"))
				.andExpect(jsonPath("$.descricao").value("Descricao da pauta"))
				.andExpect(jsonPath("$.criadaEm").exists());
	}

	@Test
	void deveDevolverNaoEncontradoAoBuscarPautaInexistente() throws Exception {
		mockMvc.perform(get("/api/v1/pautas/{id}", 404L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(containsString("404")));
	}

	@Test
	void deveConsultarASessaoDaPautaComOStatusDoMomento() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");
		abrirSessao(pautaId, 300).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/pautas/{pautaId}/sessao", pautaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.pautaId").value(pautaId))
				.andExpect(jsonPath("$.status").value("ABERTA"));
	}

	@Test
	void deveDevolverNaoEncontradoAoConsultarSessaoDePautaSemSessao() throws Exception {
		long pautaId = criarPauta("Pauta sem sessao");

		mockMvc.perform(get("/api/v1/pautas/{pautaId}/sessao", pautaId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(containsString("sess")));
	}

	@Test
	void deveAbrirSessaoDeSessentaSegundosQuandoARequisicaoNaoTemCorpo() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");

		// Requisicao sem corpo algum, e nao corpo vazio: e o caminho do enunciado, "um minuto por
		// default", e o unico que exercita o request nulo no controller.
		String corpo = mockMvc.perform(post("/api/v1/pautas/{pautaId}/sessao", pautaId))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ABERTA"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Instant abertaEm = Instant.parse(JsonPath.read(corpo, "$.abertaEm"));
		Instant encerraEm = Instant.parse(JsonPath.read(corpo, "$.encerraEm"));
		assertEquals(60, Duration.between(abertaEm, encerraEm).toSeconds());
	}

	@Test
	void deveDevolverConflitoNoSegundoVotoDoMesmoAssociado() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");
		abrirSessao(pautaId, null).andExpect(status().isCreated());
		votar(pautaId, CPF_PRIMEIRO, "SIM").andExpect(status().isCreated());

		votar(pautaId, CPF_PRIMEIRO, "NAO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Conflito de estado"));

		mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", pautaId))
				.andExpect(jsonPath("$.totalVotos").value(1));
	}

	@Test
	void deveDevolverConflitoAoVotarEmSessaoExpirada() throws Exception {
		long pautaId = criarPauta("Pauta de sessao curta");
		abrirSessao(pautaId, 1).andExpect(status().isCreated());

		// Unico ponto da suite que espera o relogio real: aqui o alvo e justamente a janela da
		// sessao vista pela aplicacao inteira, com o Clock de producao.
		Thread.sleep(1100);

		votar(pautaId, CPF_PRIMEIRO, "SIM")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Conflito de estado"));
	}

	@Test
	void deveDetalharOsCamposInvalidosAoCriarPauta() throws Exception {
		mockMvc.perform(post("/api/v1/pautas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\": \" \", \"descricao\": \"%s\"}".formatted("x".repeat(2001))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Payload inválido"))
				.andExpect(jsonPath("$.campos.titulo").exists())
				.andExpect(jsonPath("$.campos.descricao").exists());
	}

	@Test
	void deveDetalharOCampoInvalidoAoAbrirSessaoForaDaFaixaDeDuracao() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");

		abrirSessao(pautaId, 0)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.duracaoEmSegundos").exists());
	}

	@Test
	void deveRecusarCorpoComOpcaoDeVotoInexistente() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");
		abrirSessao(pautaId, null).andExpect(status().isCreated());

		votar(pautaId, CPF_PRIMEIRO, "TALVEZ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(containsString("corpo da requisi")));
	}

	@Test
	void deveRecusarVotoQuandoOProvedorNaoReconheceOCpf() throws Exception {
		long pautaId = criarPauta("Reforma do estatuto social");
		abrirSessao(pautaId, null).andExpect(status().isCreated());

		// O 404 nasce no provedor fake, chega ao client por HTTP real e so entao vira excecao de
		// dominio: e a fronteira de integracao inteira exercitada em uma requisicao.
		votar(pautaId, CPF_COM_DIGITO_ERRADO, "SIM")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(containsString("provedor de autoriza")));
	}

	@Test
	void deveResponderAptoNoProvedorFakeQuandoOModoESempreApto() throws Exception {
		mockMvc.perform(get("/fake/autorizacao/{cpf}", CPF_PRIMEIRO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ABLE_TO_VOTE"));

		mockMvc.perform(get("/fake/autorizacao/{cpf}", CPF_COM_DIGITO_ERRADO))
				.andExpect(status().isNotFound());
	}

	@Test
	void deveDevolverNaoEncontradoAoApurarPautaSemSessao() throws Exception {
		long pautaId = criarPauta("Pauta sem sessao");

		mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", pautaId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(containsString("sess")));
	}

	@Test
	void deveDevolverNaoEncontradoParaRotaInexistente() throws Exception {
		mockMvc.perform(get("/api/v1/rota-inexistente"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deveDevolverMetodoNaoSuportadoParaVerboSemMapeamento() throws Exception {
		mockMvc.perform(put("/api/v1/pautas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\": \"Reforma do estatuto social\"}"))
				.andExpect(status().isMethodNotAllowed());
	}

	private long criarPauta(String titulo) throws Exception {
		String corpo = mockMvc.perform(post("/api/v1/pautas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\": \"%s\", \"descricao\": \"Descricao da pauta\"}".formatted(titulo)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		return ((Number) JsonPath.read(corpo, "$.id")).longValue();
	}

	private ResultActions abrirSessao(long pautaId, Integer duracaoEmSegundos) throws Exception {
		return mockMvc.perform(post("/api/v1/pautas/{pautaId}/sessao", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(duracaoEmSegundos == null
						? "{}"
						: "{\"duracaoEmSegundos\": %d}".formatted(duracaoEmSegundos)));
	}

	private ResultActions votar(long pautaId, String cpf, String opcao) throws Exception {
		return mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cpf\": \"%s\", \"opcao\": \"%s\"}".formatted(cpf, opcao)));
	}

	private static int portaLivre() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException erro) {
			throw new UncheckedIOException("Nao foi possivel reservar porta para o servidor de teste", erro);
		}
	}

}
