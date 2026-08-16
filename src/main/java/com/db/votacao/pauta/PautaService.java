package com.db.votacao.pauta;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.db.votacao.comum.RecursoNaoEncontradoException;

@Service
public class PautaService {

	private static final Logger log = LoggerFactory.getLogger(PautaService.class);

	private final PautaRepository pautaRepository;
	private final Clock clock;

	public PautaService(PautaRepository pautaRepository, Clock clock) {
		this.pautaRepository = pautaRepository;
		this.clock = clock;
	}

	@Transactional
	public Pauta criar(String titulo, String descricao) {
		Pauta pauta = pautaRepository.save(new Pauta(titulo, descricao, clock.instant()));
		log.info("Pauta criada. id={} titulo={}", pauta.getId(), pauta.getTitulo());
		return pauta;
	}

	@Transactional(readOnly = true)
	public List<Pauta> listar() {
		return pautaRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Pauta buscarPorId(Long id) {
		return pautaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pauta " + id + " não encontrada."));
	}

}
