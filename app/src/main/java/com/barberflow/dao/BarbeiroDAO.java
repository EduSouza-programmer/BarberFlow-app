package com.barberflow.dao;

import com.barberflow.model.Barbeiro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da tabela BARBEIRO: profissionais exibidos na tela de novo agendamento.
 */
public class BarbeiroDAO {

    /** Lista os barbeiros ativos, em ordem alfabética. */
    public List<Barbeiro> listarAtivos() throws SQLException {
        List<Barbeiro> lista = new ArrayList<>();
        String sql = "SELECT id_barbeiro, nome, telefone, percentual_comissao " +
                     "FROM dbo.BARBEIRO WHERE ativo = 1 ORDER BY nome";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Barbeiro b = new Barbeiro(rs.getString("nome"), rs.getString("telefone"), rs.getDouble("percentual_comissao"));
                b.setIdBarbeiro(rs.getInt("id_barbeiro"));
                lista.add(b);
            }
        }
        return lista;
    }
}
