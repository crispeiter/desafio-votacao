package com.db.votacao.voto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoRepository extends JpaRepository<Voto, Long> {

	@Query("""
	       select new com.db.votacao.voto.ContagemPorOpcao(v.opcao, count(v))
	       from Voto v where v.sessao.id = :sessaoId group by v.opcao
	       """)
	List<ContagemPorOpcao> contarPorOpcao(@Param("sessaoId") Long sessaoId);

}
