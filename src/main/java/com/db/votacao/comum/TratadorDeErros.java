package com.db.votacao.comum;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Os endpoints /api/v1/telas/** nao chegam aqui: o TelaController declara os proprios
// @ExceptionHandler, que tem precedencia sobre o advice, e devolve tela em vez de
// ProblemDetail.
@RestControllerAdvice
public class TratadorDeErros {

	private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException excecao) {
		return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", excecao.getMessage());
	}

	// 404, e nao 403, por instrucao explicita do enunciado.
	@ExceptionHandler(AssociadoNaoAutorizadoException.class)
	public ProblemDetail tratarAssociadoNaoAutorizado(AssociadoNaoAutorizadoException excecao) {
		return problema(HttpStatus.NOT_FOUND, "Associado não autorizado", excecao.getMessage());
	}

	@ExceptionHandler(ConflitoException.class)
	public ProblemDetail tratarConflito(ConflitoException excecao) {
		return problema(HttpStatus.CONFLICT, "Conflito de estado", excecao.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail tratarPayloadInvalido(MethodArgumentNotValidException excecao) {
		Map<String, String> campos = new LinkedHashMap<>();
		for (FieldError erro : excecao.getBindingResult().getFieldErrors()) {
			campos.merge(erro.getField(), erro.getDefaultMessage(), (atual, novo) -> atual + "; " + novo);
		}
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Payload inválido",
				"Um ou mais campos estão inválidos.");
		problema.setProperty("campos", campos);
		return problema;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail tratarCorpoIlegivel(HttpMessageNotReadableException excecao) {
		return problema(HttpStatus.BAD_REQUEST, "Requisição malformada",
				"O corpo da requisição não pode ser lido. Verifique o JSON e os valores dos campos.");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail tratarErroInesperado(Exception excecao) {
		log.error("Erro inesperado ao processar a requisicao", excecao);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
				"Ocorreu um erro inesperado ao processar a requisição.");
	}

	private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		return problema;
	}

}
