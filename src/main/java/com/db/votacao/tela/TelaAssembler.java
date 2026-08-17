package com.db.votacao.tela;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.db.votacao.config.VotacaoProperties;
import com.db.votacao.pauta.Pauta;
import com.db.votacao.sessao.SessaoVotacao;
import com.db.votacao.sessao.StatusSessao;
import com.db.votacao.tela.dto.BotaoResponse;
import com.db.votacao.tela.dto.CampoResponse;
import com.db.votacao.tela.dto.ItemSelecaoResponse;
import com.db.votacao.tela.dto.TelaFormularioResponse;
import com.db.votacao.tela.dto.TelaSelecaoResponse;
import com.db.votacao.voto.Apuracao;
import com.db.votacao.voto.OpcaoVoto;

@Component
public class TelaAssembler {

	private static final String PAUTAS = "/api/v1/telas/pautas";
	private static final String VOTACAO = PAUTAS + "/{pautaId}/votacao";
	private static final String VOTO = PAUTAS + "/{pautaId}/voto";
	private static final String RESULTADO = PAUTAS + "/{pautaId}/resultado";

	private static final String CAMPO_MENSAGEM = "mensagem";

	// Fuso fixo, e nao o da JVM nem o UTC do Clock: a descricao e texto de exibicao para o
	// associado de uma cooperativa brasileira, e horario em UTC na tela seria erro visivel.
	// Os campos de data das respostas REST seguem em Instant UTC.
	private static final ZoneId FUSO_DE_EXIBICAO = ZoneId.of("America/Sao_Paulo");

	private static final DateTimeFormatter FORMATO_ENCERRAMENTO = DateTimeFormatter
			.ofPattern("dd/MM/yyyy HH:mm")
			.withZone(FUSO_DE_EXIBICAO);

	private final String baseUrl;

	public TelaAssembler(VotacaoProperties properties) {
		this.baseUrl = properties.callbackBaseUrl();
	}

	public TelaSelecaoResponse telaDePautas(List<SessaoVotacao> sessoesAbertas) {
		List<ItemSelecaoResponse> itens = sessoesAbertas.stream().map(this::itemDePauta).toList();
		return TelaSelecaoResponse.de("Pautas em votação", itens);
	}

	public TelaFormularioResponse telaDeVotacao(Pauta pauta) {
		List<CampoResponse> itens = List.of(CampoResponse.texto("cpf", "Informe seu CPF"));
		List<BotaoResponse> botoes = List.of(botaoDeVoto(pauta.getId(), OpcaoVoto.SIM, "Sim"),
				botaoDeVoto(pauta.getId(), OpcaoVoto.NAO, "Não"));
		return TelaFormularioResponse.de(pauta.getTitulo(), itens, botoes);
	}

	public TelaFormularioResponse telaDeConfirmacao(Long pautaId, OpcaoVoto opcao) {
		List<CampoResponse> itens = List.of(CampoResponse.informacao(CAMPO_MENSAGEM,
				"Seu voto " + opcao.name() + " foi registrado com sucesso."));
		List<BotaoResponse> botoes = List
				.of(new BotaoResponse("Ver resultado", url(RESULTADO, pautaId), Map.of()));
		return TelaFormularioResponse.de("Voto registrado", itens, botoes);
	}

	public TelaFormularioResponse telaDeResultado(Apuracao apuracao) {
		List<CampoResponse> itens = List.of(
				CampoResponse.informacao("situacao", situacao(apuracao.statusSessao())),
				CampoResponse.informacao("sim", "Sim: " + apuracao.votosSim()),
				CampoResponse.informacao("nao", "Não: " + apuracao.votosNao()),
				CampoResponse.informacao("total", "Total de votos: " + apuracao.totalVotos()),
				CampoResponse.informacao("resultado", "Resultado: " + apuracao.resultado().name()));
		return TelaFormularioResponse.de(apuracao.titulo(), itens, List.of());
	}

	public TelaFormularioResponse telaDeErro(String titulo, String mensagem) {
		return TelaFormularioResponse.de(titulo, List.of(CampoResponse.informacao(CAMPO_MENSAGEM, mensagem)),
				List.of());
	}

	// A pauta da sessao precisa vir carregada pela consulta: com open-in-view desligado a sessao
	// de persistencia ja esta fechada quando o assembler roda.
	private ItemSelecaoResponse itemDePauta(SessaoVotacao sessao) {
		Long pautaId = sessao.getPauta().getId();
		return new ItemSelecaoResponse(sessao.getPauta().getTitulo(),
				"Encerra em " + FORMATO_ENCERRAMENTO.format(sessao.getEncerraEm()), url(VOTACAO, pautaId),
				Map.of("pautaId", pautaId));
	}

	private BotaoResponse botaoDeVoto(Long pautaId, OpcaoVoto opcao, String titulo) {
		return new BotaoResponse(titulo, url(VOTO, pautaId), Map.of("opcao", opcao.name()));
	}

	private static String situacao(StatusSessao status) {
		return status == StatusSessao.ABERTA ? "Sessão aberta" : "Sessão encerrada";
	}

	private String url(String path, Long pautaId) {
		return UriComponentsBuilder.fromUriString(baseUrl).path(path).buildAndExpand(pautaId).toUriString();
	}

}
