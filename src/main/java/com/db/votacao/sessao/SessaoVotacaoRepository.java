package com.db.votacao.sessao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

	Optional<SessaoVotacao> findByPautaId(Long pautaId);

}
