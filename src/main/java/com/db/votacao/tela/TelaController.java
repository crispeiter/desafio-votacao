package com.db.votacao.tela;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.db.votacao.comum.ExcecaoDeNegocio;
import com.db.votacao.pauta.PautaService;
import com.db.votacao.sessao.SessaoVotacaoService;
import com.db.votacao.tela.dto.TelaFormularioResponse;
import com.db.votacao.tela.dto.TelaSelecaoResponse;
import com.db.votacao.voto.ApuracaoService;
import com.db.votacao.voto.Voto;
import com.db.votacao.voto.VotoService;
import com.db.votacao.voto.dto.RegistrarVotoRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Tag(name = "Telas", description = "Telas renderizadas pelo aplicativo mobile, conforme o Anexo 1")
@RestController
@RequestMapping("/api/v1/telas")
public class TelaController {

	private static final Logger log = LoggerFactory.getLogger(TelaController.class);

	private static final String SUFIXO_RESULTADO = "/resultado";

	private static final String PATH_PAUTAS = "/pautas";
	private static final String PATH_VOTACAO = PATH_PAUTAS + "/{pautaId}/votacao";
	private static final String PATH_VOTO = PATH_PAUTAS + "/{pautaId}/voto";
	private static final String PATH_RESULTADO = PATH_PAUTAS + "/{pautaId}" + SUFIXO_RESULTADO;

	private static final String TITULO_ERRO_VOTACAO = "Não foi possível votar";
	private static final String TITULO_ERRO_RESULTADO = "Não foi possível exibir o resultado";

	private static final String MENSAGEM_DADOS_ILEGIVEIS = "Não foi possível ler os dados enviados. Verifique as "
			+ "informações e tente novamente.";

	private final SessaoVotacaoService sessaoVotacaoService;
	private final PautaService pautaService;
	private final VotoService votoService;
	private final ApuracaoService apuracaoService;
	private final TelaAssembler telaAssembler;

	public TelaController(SessaoVotacaoService sessaoVotacaoService, PautaService pautaService,
			VotoService votoService, ApuracaoService apuracaoService, TelaAssembler telaAssembler) {
		this.sessaoVotacaoService = sessaoVotacaoService;
		this.pautaService = pautaService;
		this.votoService = votoService;
		this.apuracaoService = apuracaoService;
		this.telaAssembler = telaAssembler;
	}

	@Operation(summary = "Tela de seleção com as pautas em votação",
			description = "Lista apenas pautas com sessão aberta: oferecer uma pauta sem sessão levaria o associado "
					+ "a um formulário que só falharia. Único endpoint de tela em GET, porque o aplicativo precisa "
					+ "buscá-la sem ação prévia do usuário.")
	@ApiResponse(responseCode = "200", description = "Tela SELECAO, com lista vazia se nenhuma sessão está aberta")
	@GetMapping(PATH_PAUTAS)
	public TelaSelecaoResponse pautasEmVotacao() {
		return telaAssembler.telaDePautas(sessaoVotacaoService.listarAbertas());
	}

	@Operation(summary = "Tela de formulário para votar em uma pauta")
	@ApiResponse(responseCode = "200", description = "Tela FORMULARIO com o campo de CPF e os botões Sim e Não, ou "
			+ "tela de erro se a pauta não existe")
	@PostMapping(PATH_VOTACAO)
	public TelaFormularioResponse formularioDeVotacao(@PathVariable Long pautaId) {
		return telaAssembler.telaDeVotacao(pautaService.buscarPorId(pautaId));
	}

	@Operation(summary = "Registra o voto vindo da tela e devolve a confirmação")
	@ApiResponse(responseCode = "200", description = "Tela FORMULARIO de confirmação, ou tela de erro se a sessão "
			+ "está encerrada, o associado não está apto ou já votou")
	@PostMapping(PATH_VOTO)
	public TelaFormularioResponse votar(@PathVariable Long pautaId, @RequestBody @Valid RegistrarVotoRequest request) {
		Voto voto = votoService.registrarVoto(pautaId, request.cpf(), request.opcao());
		return telaAssembler.telaDeConfirmacao(pautaId, voto.getOpcao());
	}

	@Operation(summary = "Tela de formulário com a apuração da pauta",
			description = "POST, e não GET, porque o Anexo 1 define que botões e itens de seleção disparam POST.")
	@ApiResponse(responseCode = "200", description = "Tela FORMULARIO com a apuração, ou tela de erro se a pauta não "
			+ "tem sessão de votação")
	@PostMapping(PATH_RESULTADO)
	public TelaFormularioResponse resultado(@PathVariable Long pautaId) {
		return telaAssembler.telaDeResultado(apuracaoService.apurar(pautaId));
	}

	// Os tres handlers precedem o TratadorDeErros global: o aplicativo so sabe renderizar tela, e
	// um ProblemDetail aqui quebraria a navegacao dele. O @ResponseStatus e explicito porque o
	// framework ja associa 400 a duas destas excecoes, e na camada de telas o status e sempre 200.
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(ExcecaoDeNegocio.class)
	public TelaFormularioResponse tratarErroDeNegocio(ExcecaoDeNegocio excecao, HttpServletRequest requisicao) {
		log.info("Erro de negocio traduzido em tela. tipo={}", excecao.getClass().getSimpleName());
		return telaAssembler.telaDeErro(tituloDoErro(requisicao), excecao.getMessage());
	}

	// As mensagens das constraints do RegistrarVotoRequest ja sao texto para o associado, entao
	// vao direto para a tela em vez de virarem um "dados invalidos" generico.
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public TelaFormularioResponse tratarCampoInvalido(MethodArgumentNotValidException excecao,
			HttpServletRequest requisicao) {
		String mensagem = excecao.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(" "));
		log.info("Campo invalido na camada de telas. campos={}", excecao.getBindingResult().getFieldErrorCount());
		return telaAssembler.telaDeErro(tituloDoErro(requisicao), mensagem);
	}

	// Corpo ilegivel inclui opcao fora de SIM e NAO: o valor invalido quebra a desserializacao
	// antes de chegar ao metodo, entao nao ha campo de bind para detalhar.
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public TelaFormularioResponse tratarCorpoIlegivel(HttpServletRequest requisicao) {
		log.info("Corpo ilegivel na camada de telas. uri={}", requisicao.getRequestURI());
		return telaAssembler.telaDeErro(tituloDoErro(requisicao), MENSAGEM_DADOS_ILEGIVEIS);
	}

	private static String tituloDoErro(HttpServletRequest requisicao) {
		return requisicao.getRequestURI().endsWith(SUFIXO_RESULTADO) ? TITULO_ERRO_RESULTADO : TITULO_ERRO_VOTACAO;
	}

}
