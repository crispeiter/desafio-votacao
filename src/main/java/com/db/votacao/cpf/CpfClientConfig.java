package com.db.votacao.cpf;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.db.votacao.comum.RecursoNaoEncontradoException;
import com.db.votacao.config.VotacaoProperties;
import com.db.votacao.config.VotacaoProperties.ValidacaoCpf;

@Configuration
public class CpfClientConfig {

	@Bean
	CpfAutorizacaoClient cpfAutorizacaoClient(VotacaoProperties propriedades) {
		ValidacaoCpf configuracao = propriedades.cpf();
		RestClient restClient = RestClient.builder()
				.baseUrl(configuracao.baseUrl())
				.requestFactory(fabricaDeRequisicoes(configuracao))
				// O 404 do provedor significa CPF recusado, nao indisponibilidade, entao vira
				// excecao de dominio aqui e nao HttpClientErrorException vazando para o servico.
				.defaultStatusHandler(HttpStatus.NOT_FOUND::isSameCodeAs, (requisicao, resposta) -> {
					throw new RecursoNaoEncontradoException(
							"CPF inválido ou não reconhecido pelo provedor de autorização.");
				})
				.build();
		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.build()
				.createClient(CpfAutorizacaoClient.class);
	}

	private static ClientHttpRequestFactory fabricaDeRequisicoes(ValidacaoCpf configuracao) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(configuracao.timeoutConexao())
				.build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
		fabrica.setReadTimeout(configuracao.timeoutLeitura());
		return fabrica;
	}

}
