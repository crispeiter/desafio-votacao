package com.db.votacao;

import org.springframework.boot.SpringApplication;

public class TestVotacaoApplication {

	public static void main(String[] args) {
		SpringApplication.from(VotacaoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
