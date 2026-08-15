package com.db.votacao.comum;

public abstract class ExcecaoDeNegocio extends RuntimeException {

	protected ExcecaoDeNegocio(String mensagem) {
		super(mensagem);
	}

}
