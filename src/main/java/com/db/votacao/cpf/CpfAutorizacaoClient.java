package com.db.votacao.cpf;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface CpfAutorizacaoClient {

	@GetExchange("/fake/autorizacao/{cpf}")
	AutorizacaoCpfResponse consultar(@PathVariable String cpf);

}
