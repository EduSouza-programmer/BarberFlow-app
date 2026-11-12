package com.barberflow.dao;

import com.barberflow.model.Agendamento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para operações de persistência de Agendamento.
 * Implementa validação de conflito concorrente de horários.
 */
public class AgendamentoDAO {

    /**
     * Verifica se já existe um agendamento ativo para o barbeiro no intervalo solicitado.
     * Atende ao RF de verificação de conflitos de agenda.
     */
    public boolean existeConflitoHorario(int idBarbeiro, Timestamp inicio, Timestamp fim) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.AGENDAMENTO " +
                     "WHERE id_barbeiro = ? " +
                     "  AND status_agendamento = 'CONFIRMADO' " +
                     "  AND (data_hora_inicio < ? AND data_hora_fim > ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idBarbeiro);
            stmt.setTimestamp(2, fim);
            stmt.setTimestamp(3, inicio);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Insere um novo agendamento com validação prévia de concorrência.
     */
    public boolean salvar(Agendamento agendamento) throws SQLException {
        if (existeConflitoHorario(agendamento.getIdBarbeiro(), agendamento.getDataHoraInicio(), agendamento.getDataHoraFim())) {
            throw new SQLException("Horário conflitante! O barbeiro selecionado já possui atendimento marcado neste período.");
        }

        String sql = "INSERT INTO dbo.AGENDAMENTO " +
                     "(id_cliente, id_barbeiro, id_servico, data_hora_inicio, data_hora_fim, status_agendamento, observacoes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, agendamento.getIdCliente());
            stmt.setInt(2, agendamento.getIdBarbeiro());
            stmt.setInt(3, agendamento.getIdServico());
            stmt.setTimestamp(4, agendamento.getDataHoraInicio());
            stmt.setTimestamp(5, agendamento.getDataHoraFim());
            stmt.setString(6, agendamento.getStatusAgendamento());
            stmt.setString(7, agendamento.getObservacoes());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        agendamento.setIdAgendamento(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Consulta a lista de agendamentos de um cliente específico com dados dos serviços e barbeiros (JOIN).
     */
    public List<Agendamento> listarPorCliente(int idCliente) throws SQLException {
        List<Agendamento> lista = new ArrayList<>();
        String sql = "SELECT A.id_agendamento, A.id_cliente, A.id_barbeiro, A.id_servico, " +
                     "       A.data_hora_inicio, A.data_hora_fim, A.status_agendamento, A.observacoes, " +
                     "       B.nome AS nome_barbeiro, S.nome_servico, S.preco " +
                     "FROM dbo.AGENDAMENTO A " +
                     "INNER JOIN dbo.BARBEIRO B ON A.id_barbeiro = B.id_barbeiro " +
                     "INNER JOIN dbo.SERVICO S ON A.id_servico = S.id_servico " +
                     "WHERE A.id_cliente = ? " +
                     "ORDER BY A.data_hora_inicio DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Agendamento a = new Agendamento();
                    a.setIdAgendamento(rs.getInt("id_agendamento"));
                    a.setIdCliente(rs.getInt("id_cliente"));
                    a.setIdBarbeiro(rs.getInt("id_barbeiro"));
                    a.setIdServico(rs.getInt("id_servico"));
                    a.setDataHoraInicio(rs.getTimestamp("data_hora_inicio"));
                    a.setDataHoraFim(rs.getTimestamp("data_hora_fim"));
                    a.setStatusAgendamento(rs.getString("status_agendamento"));
                    a.setObservacoes(rs.getString("observacoes"));
                    a.setNomeBarbeiro(rs.getString("nome_barbeiro"));
                    a.setNomeServico(rs.getString("nome_servico"));
                    a.setPrecoServico(rs.getDouble("preco"));
                    lista.add(a);
                }
            }
        }
        return lista;
    }

    /**
     * Cancela um agendamento e libera a grade de horários.
     */
    public boolean cancelar(int idAgendamento) throws SQLException {
        String sql = "UPDATE dbo.AGENDAMENTO SET status_agendamento = 'CANCELADO' WHERE id_agendamento = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAgendamento);
            return stmt.executeUpdate() > 0;
        }
    }
}
