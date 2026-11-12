package com.barberflow.model;

public class Barbeiro {
    private int idBarbeiro;
    private String nome;
    private String telefone;
    private String email;
    private double percentualComissao;
    private boolean ativo;

    public Barbeiro() {
        this.percentualComissao = 50.0;
        this.ativo = true;
    }

    public Barbeiro(String nome, String telefone, double percentualComissao) {
        this.nome = nome;
        this.telefone = telefone;
        this.percentualComissao = percentualComissao;
        this.ativo = true;
    }

    public int getIdBarbeiro() { return idBarbeiro; }
    public void setIdBarbeiro(int idBarbeiro) { this.idBarbeiro = idBarbeiro; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getPercentualComissao() { return percentualComissao; }
    public void setPercentualComissao(double percentualComissao) { this.percentualComissao = percentualComissao; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
