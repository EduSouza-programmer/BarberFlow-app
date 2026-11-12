package com.barberflow.model;

import java.sql.Timestamp;

public class Agendamento {
    private int idAgendamento;
    private int idCliente;
    private int idBarbeiro;
    private int idServico;
    private Timestamp dataHoraInicio;
    private Timestamp dataHoraFim;
    private String statusAgendamento;
    private String observacoes;

    // Dados complementares para exibição em tela (JOINs)
    private String nomeCliente;
    private String nomeBarbeiro;
    private String nomeServico;
    private double precoServico;

    public Agendamento() {
        this.statusAgendamento = "CONFIRMADO";
    }

    public Agendamento(int idCliente, int idBarbeiro, int idServico, Timestamp dataHoraInicio, Timestamp dataHoraFim) {
        this.idCliente = idCliente;
        this.idBarbeiro = idBarbeiro;
        this.idServico = idServico;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.statusAgendamento = "CONFIRMADO";
    }

    // Getters e Setters
    public int getIdAgendamento() { return idAgendamento; }
    public void setIdAgendamento(int idAgendamento) { this.idAgendamento = idAgendamento; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdBarbeiro() { return idBarbeiro; }
    public void setIdBarbeiro(int idBarbeiro) { this.idBarbeiro = idBarbeiro; }

    public int getIdServico() { return idServico; }
    public void setIdServico(int idServico) { this.idServico = idServico; }

    public Timestamp getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(Timestamp dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }

    public Timestamp getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(Timestamp dataHoraFim) { this.dataHoraFim = dataHoraFim; }

    public String getStatusAgendamento() { return statusAgendamento; }
    public void setStatusAgendamento(String statusAgendamento) { this.statusAgendamento = statusAgendamento; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public String getNomeBarbeiro() { return nomeBarbeiro; }
    public void setNomeBarbeiro(String nomeBarbeiro) { this.nomeBarbeiro = nomeBarbeiro; }

    public String getNomeServico() { return nomeServico; }
    public void setNomeServico(String nomeServico) { this.nomeServico = nomeServico; }

    public double getPrecoServico() { return precoServico; }
    public void setPrecoServico(double precoServico) { this.precoServico = precoServico; }
}
