package com.barberflow.dao;

import com.barberflow.model.Servico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da tabela SERVICO: catálogo exibido na tela de novo agendamento.
 */
public class ServicoDAO {

    /** Lista os serviços ativos, em ordem alfabética. */
    public List<Servico> listarAtivos() throws SQLException {
        List<Servico> lista = new ArrayList<>();
        String sql = "SELECT id_servico, nome_servico, descricao, duracao_minutos, preco " +
                     "FROM dbo.SERVICO WHERE ativo = 1 ORDER BY nome_servico";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Servico s = new Servico(rs.getString("nome_servico"), rs.getInt("duracao_minutos"), rs.getDouble("preco"));
                s.setIdServico(rs.getInt("id_servico"));
                s.setDescricao(rs.getString("descricao"));
                lista.add(s);
            }
        }
        return lista;
    }
}
