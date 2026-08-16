package com.db.votacao.cpf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

	@ParameterizedTest
	@ValueSource(strings = { "12345678909", "11144477735", "52998224725", "01234567890" })
	void deveAceitarCpfComDigitoVerificadorCorreto(String cpf) {
		assertTrue(Cpf.valido(cpf));
	}

	@ParameterizedTest
	@ValueSource(strings = { "12345678900", "11144477730", "52998224726", "01234567899" })
	void deveRejeitarCpfComDigitoVerificadorErrado(String cpf) {
		assertFalse(Cpf.valido(cpf));
	}

	@ParameterizedTest
	@ValueSource(strings = { "00000000000", "11111111111", "22222222222", "33333333333", "44444444444",
			"55555555555", "66666666666", "77777777777", "88888888888", "99999999999" })
	void deveRejeitarCpfComTodosOsDigitosIguais(String cpf) {
		assertFalse(Cpf.valido(cpf));
	}

	@ParameterizedTest
	@ValueSource(strings = { "123.456.789-09", "529.982.247-25", "111.444.777-35" })
	void deveAceitarCpfValidoComMascaraDePontuacao(String cpf) {
		assertTrue(Cpf.valido(cpf));
	}

	@Test
	void deveRejeitarCpfComMascaraEDigitoVerificadorErrado() {
		assertFalse(Cpf.valido("123.456.789-00"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "1234567890", "123456789099", "1", "abcdefghijk", "   " })
	void deveRejeitarCpfSemOnzeDigitos(String cpf) {
		assertFalse(Cpf.valido(cpf));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void deveRejeitarCpfNuloOuVazio(String cpf) {
		assertFalse(Cpf.valido(cpf));
	}

	@ParameterizedTest
	@CsvSource({
			"123.456.789-09, 12345678909",
			"  529.982.247-25  , 52998224725",
			"123 456 789 09, 12345678909",
			"cpf 12345678909, 12345678909",
			"12345678909, 12345678909"
	})
	void deveNormalizarRemovendoTudoQueNaoForDigito(String entrada, String esperado) {
		assertEquals(esperado, Cpf.normalizar(entrada));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void deveNormalizarNuloOuVazioComoTextoVazio(String cpf) {
		assertEquals("", Cpf.normalizar(cpf));
	}

	@Test
	void deveNormalizarTextoSemDigitosComoTextoVazio() {
		assertEquals("", Cpf.normalizar("...-"));
	}

}
