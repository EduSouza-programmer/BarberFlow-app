package com.barberflow.dao;

import java.sql.SQLException;

/**
 * Lançada quando o barbeiro já tem um atendimento ativo que se sobrepõe ao horário pedido.
 * Separa a regra de negócio (dupla marcação) das falhas de rede/banco, para a tela tratar cada caso.
 */
public class HorarioIndisponivelException extends SQLException {

    public HorarioIndisponivelException() {
        super("Horário indisponível! O barbeiro selecionado já possui atendimento marcado neste período.");
    }
}
