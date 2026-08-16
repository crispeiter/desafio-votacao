package com.db.votacao.pauta;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.db.votacao.pauta.dto.CriarPautaRequest;
import com.db.votacao.pauta.dto.PautaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Pautas", description = "Cadastro e consulta de pautas de votação")
@RestController
@RequestMapping("/api/v1/pautas")
public class PautaController {

	private final PautaService pautaService;

	public PautaController(PautaService pautaService) {
		this.pautaService = pautaService;
	}

	@Operation(summary = "Cria uma pauta")
	@PostMapping
	public ResponseEntity<PautaResponse> criar(@RequestBody @Valid CriarPautaRequest request,
			UriComponentsBuilder uriBuilder) {
		Pauta pauta = pautaService.criar(request.titulo(), request.descricao());
		URI localizacao = uriBuilder.path("/api/v1/pautas/{id}").buildAndExpand(pauta.getId()).toUri();
		return ResponseEntity.created(localizacao).body(paraResponse(pauta));
	}

	@Operation(summary = "Lista todas as pautas")
	@GetMapping
	public List<PautaResponse> listar() {
		return pautaService.listar().stream().map(PautaController::paraResponse).toList();
	}

	@Operation(summary = "Busca uma pauta pelo identificador")
	@GetMapping("/{id}")
	public PautaResponse buscarPorId(@PathVariable Long id) {
		return paraResponse(pautaService.buscarPorId(id));
	}

	private static PautaResponse paraResponse(Pauta pauta) {
		return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao(), pauta.getCriadaEm());
	}

}
