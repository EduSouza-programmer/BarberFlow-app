package com.barberflow.model;

public class Servico {
    private int idServico;
    private String nomeServico;
    private String descricao;
    private int duracaoMinutos;
    private double preco;
    private boolean ativo;

    public Servico() {
        this.ativo = true;
    }

    public Servico(String nomeServico, int duracaoMinutos, double preco) {
        this.nomeServico = nomeServico;
        this.duracaoMinutos = duracaoMinutos;
        this.preco = preco;
        this.ativo = true;
    }

    public int getIdServico() { return idServico; }
    public void setIdServico(int idServico) { this.idServico = idServico; }

    public String getNomeServico() { return nomeServico; }
    public void setNomeServico(String nomeServico) { this.nomeServico = nomeServico; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(int duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
