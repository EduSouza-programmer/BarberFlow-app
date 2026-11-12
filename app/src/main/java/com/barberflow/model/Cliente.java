package com.barberflow.model;

import java.sql.Timestamp;

public class Cliente {
    private int idCliente;
    private String nome;
    private String telefone;
    private String email;
    private Timestamp dataCadastro;
    private boolean ativo;

    public Cliente() {
        this.ativo = true;
    }

    public Cliente(String nome, String telefone, String email) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.ativo = true;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Timestamp getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(Timestamp dataCadastro) { this.dataCadastro = dataCadastro; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
