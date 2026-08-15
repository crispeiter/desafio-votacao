package com.db.votacao.pauta;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pauta")
public class Pauta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "titulo", nullable = false, length = 200)
	private String titulo;

	@Column(name = "descricao", length = 2000)
	private String descricao;

	@Column(name = "criada_em", nullable = false)
	private Instant criadaEm;

	protected Pauta() {
	}

	public Pauta(String titulo, String descricao, Instant criadaEm) {
		this.titulo = titulo;
		this.descricao = descricao;
		this.criadaEm = criadaEm;
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public Instant getCriadaEm() {
		return criadaEm;
	}

}
