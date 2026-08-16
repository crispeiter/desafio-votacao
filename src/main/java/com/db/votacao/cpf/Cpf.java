package com.db.votacao.cpf;

public final class Cpf {

	private static final int TAMANHO = 11;
	private static final int MODULO = 11;

	private Cpf() {
	}

	public static String normalizar(String cpf) {
		if (cpf == null) {
			return "";
		}
		StringBuilder digitos = new StringBuilder(TAMANHO);
		for (int i = 0; i < cpf.length(); i++) {
			char caractere = cpf.charAt(i);
			// Faixa ASCII explicita porque Character.isDigit aceita digitos de outros alfabetos,
			// que quebrariam a aritmetica de "caractere - '0'" no calculo do verificador.
			if (caractere >= '0' && caractere <= '9') {
				digitos.append(caractere);
			}
		}
		return digitos.toString();
	}

	public static boolean valido(String cpf) {
		String digitos = normalizar(cpf);
		if (digitos.length() != TAMANHO || todosOsDigitosIguais(digitos)) {
			return false;
		}
		return calcularVerificador(digitos, 9) == valorEm(digitos, 9)
				&& calcularVerificador(digitos, 10) == valorEm(digitos, 10);
	}

	private static boolean todosOsDigitosIguais(String digitos) {
		// Sequencias como 11111111111 satisfazem o calculo do verificador, mas nao sao CPFs
		// emitidos pela Receita Federal.
		return digitos.chars().distinct().count() == 1;
	}

	private static int calcularVerificador(String digitos, int quantidade) {
		int soma = 0;
		int peso = quantidade + 1;
		for (int i = 0; i < quantidade; i++) {
			soma += valorEm(digitos, i) * peso--;
		}
		int resto = soma % MODULO;
		return resto < 2 ? 0 : MODULO - resto;
	}

	private static int valorEm(String digitos, int posicao) {
		return digitos.charAt(posicao) - '0';
	}

}
