package com.db.votacao.sessao;

import java.time.Instant;

import com.db.votacao.pauta.Pauta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessao_votacao")
public class SessaoVotacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pauta_id", nullable = false)
	private Pauta pauta;

	@Column(name = "aberta_em", nullable = false)
	private Instant abertaEm;

	@Column(name = "encerra_em", nullable = false)
	private Instant encerraEm;

	protected SessaoVotacao() {
	}

	public SessaoVotacao(Pauta pauta, Instant abertaEm, Instant encerraEm) {
		this.pauta = pauta;
		this.abertaEm = abertaEm;
		this.encerraEm = encerraEm;
	}

	// A janela e fechada no fim: um voto que chega exatamente em encerraEm ja esta fora.
	public boolean estaAberta(Instant agora) {
		return !agora.isBefore(abertaEm) && agora.isBefore(encerraEm);
	}

	public StatusSessao statusEm(Instant agora) {
		return estaAberta(agora) ? StatusSessao.ABERTA : StatusSessao.ENCERRADA;
	}

	public Long getId() {
		return id;
	}

	public Pauta getPauta() {
		return pauta;
	}

	public Instant getAbertaEm() {
		return abertaEm;
	}

	public Instant getEncerraEm() {
		return encerraEm;
	}

}
