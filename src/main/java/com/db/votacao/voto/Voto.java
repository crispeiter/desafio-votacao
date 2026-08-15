package com.db.votacao.voto;

import java.time.Instant;

import com.db.votacao.sessao.SessaoVotacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "voto")
public class Voto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sessao_id", nullable = false)
	private SessaoVotacao sessao;

	@Column(name = "cpf_associado", nullable = false, length = 11)
	private String cpfAssociado;

	@Enumerated(EnumType.STRING)
	@Column(name = "opcao", nullable = false, length = 10)
	private OpcaoVoto opcao;

	@Column(name = "registrado_em", nullable = false)
	private Instant registradoEm;

	protected Voto() {
	}

	public Voto(SessaoVotacao sessao, String cpfAssociado, OpcaoVoto opcao, Instant registradoEm) {
		this.sessao = sessao;
		this.cpfAssociado = cpfAssociado;
		this.opcao = opcao;
		this.registradoEm = registradoEm;
	}

	public Long getId() {
		return id;
	}

	public SessaoVotacao getSessao() {
		return sessao;
	}

	public String getCpfAssociado() {
		return cpfAssociado;
	}

	public OpcaoVoto getOpcao() {
		return opcao;
	}

	public Instant getRegistradoEm() {
		return registradoEm;
	}

}
