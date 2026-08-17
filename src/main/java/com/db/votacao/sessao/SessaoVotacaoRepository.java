package com.db.votacao.sessao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

	Optional<SessaoVotacao> findByPautaId(Long pautaId);

	// Fetch join da pauta porque a tela usa o titulo dela e o open-in-view esta desligado. A
	// mesma janela fechada no fim de SessaoVotacao.estaAberta.
	@Query("""
	       select s from SessaoVotacao s join fetch s.pauta
	       where s.abertaEm <= :agora and s.encerraEm > :agora order by s.encerraEm
	       """)
	List<SessaoVotacao> buscarAbertasEm(@Param("agora") Instant agora);

	boolean existsByPautaId(Long pautaId);

}
