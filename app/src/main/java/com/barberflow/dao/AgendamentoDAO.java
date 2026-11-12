package com.barberflow.dao;

import com.barberflow.model.Agendamento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para operações de persistência de Agendamento.
 * Implementa a regra de negócio contra dupla marcação (conflito de horários do mesmo barbeiro).
 */
public class AgendamentoDAO {

    /**
     * Dois intervalos se sobrepõem quando um começa antes de o outro terminar
     * e termina depois de o outro começar. PENDENTE e CONFIRMADO ocupam o horário;
     * CANCELADO e CONCLUIDO não.
     */
    private static final String SQL_CONFLITO =
        "SELECT COUNT(*) FROM dbo.AGENDAMENTO %s " +
        "WHERE id_barbeiro = ? " +
        "  AND status_agendamento IN ('PENDENTE', 'CONFIRMADO') " +
        "  AND (data_hora_inicio < ? AND data_hora_fim > ?)";

    /**
     * Verifica se já existe um agendamento ativo para o barbeiro no intervalo solicitado.
     * Atende ao RF de verificação de conflitos de agenda.
     */
    public boolean existeConflitoHorario(int idBarbeiro, Timestamp inicio, Timestamp fim) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return existeConflito(conn, String.format(SQL_CONFLITO, ""), idBarbeiro, inicio, fim);
        }
    }

    /**
     * Insere um novo agendamento com validação de concorrência.
     *
     * A verificação e a gravação acontecem na MESMA transação, e a consulta usa
     * UPDLOCK + HOLDLOCK: enquanto um cliente grava, outro que tente o mesmo barbeiro
     * e horário espera, e depois enxerga o agendamento recém-criado. Isso impede que
     * duas confirmações simultâneas passem pela verificação ao mesmo tempo.
     *
     * @throws HorarioIndisponivelException se o barbeiro já tiver atendimento no período
     */
    public boolean salvar(Agendamento agendamento) throws SQLException {
        String sqlInsert = "INSERT INTO dbo.AGENDAMENTO " +
                           "(id_cliente, id_barbeiro, id_servico, data_hora_inicio, data_hora_fim, status_agendamento, observacoes) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (existeConflito(conn, String.format(SQL_CONFLITO, "WITH (UPDLOCK, HOLDLOCK)"),
                        agendamento.getIdBarbeiro(), agendamento.getDataHoraInicio(), agendamento.getDataHoraFim())) {
                    conn.rollback();
                    throw new HorarioIndisponivelException();
                }

                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, agendamento.getIdCliente());
                    stmt.setInt(2, agendamento.getIdBarbeiro());
                    stmt.setInt(3, agendamento.getIdServico());
                    stmt.setTimestamp(4, agendamento.getDataHoraInicio());
                    stmt.setTimestamp(5, agendamento.getDataHoraFim());
                    stmt.setString(6, agendamento.getStatusAgendamento());
                    stmt.setString(7, agendamento.getObservacoes());
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            agendamento.setIdAgendamento(rs.getInt(1));
                        }
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                if (!(e instanceof HorarioIndisponivelException)) {
                    conn.rollback();
                }
                throw e;
            }
        }
    }

    private boolean existeConflito(Connection conn, String sql, int idBarbeiro, Timestamp inicio, Timestamp fim) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idBarbeiro);
            stmt.setTimestamp(2, fim);
            stmt.setTimestamp(3, inicio);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
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
     * Cancela um agendamento ativo e libera a grade de horários.
     * Não apaga o registro: só muda o status, mantendo o histórico.
     */
    public boolean cancelar(int idAgendamento) throws SQLException {
        String sql = "UPDATE dbo.AGENDAMENTO SET status_agendamento = 'CANCELADO' " +
                     "WHERE id_agendamento = ? AND status_agendamento IN ('PENDENTE', 'CONFIRMADO')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAgendamento);
            return stmt.executeUpdate() > 0;
        }
    }
}
