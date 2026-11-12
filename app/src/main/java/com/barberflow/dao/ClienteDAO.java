package com.barberflow.dao;

import com.barberflow.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO da tabela CLIENTE. O cliente é identificado pelo WhatsApp (coluna telefone, UNIQUE).
 */
public class ClienteDAO {

    // Compara só os dígitos, para que "(21) 97111-2233" e "21971112233" sejam o mesmo cliente
    private static final String TELEFONE_SO_DIGITOS =
        "REPLACE(REPLACE(REPLACE(REPLACE(telefone, '(', ''), ')', ''), ' ', ''), '-', '')";

    /** Mantém apenas os dígitos do telefone digitado. */
    public static String normalizarTelefone(String telefone) {
        return telefone == null ? "" : telefone.replaceAll("\\D", "");
    }

    /**
     * Busca o cliente pelo WhatsApp.
     *
     * @return o cliente, ou null se não houver cadastro com esse número
     */
    public Cliente buscarPorTelefone(String telefone) throws SQLException {
        String sql = "SELECT id_cliente, nome, telefone, email, data_cadastro, ativo FROM dbo.CLIENTE " +
                     "WHERE " + TELEFONE_SO_DIGITOS + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizarTelefone(telefone));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cliente c = new Cliente();
                    c.setIdCliente(rs.getInt("id_cliente"));
                    c.setNome(rs.getString("nome"));
                    c.setTelefone(rs.getString("telefone"));
                    c.setEmail(rs.getString("email"));
                    c.setDataCadastro(rs.getTimestamp("data_cadastro"));
                    c.setAtivo(rs.getBoolean("ativo"));
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Retorna o cliente com esse WhatsApp; se ainda não existir, cadastra.
     * Usado ao confirmar um agendamento.
     */
    public Cliente buscarOuCadastrar(String nome, String telefone) throws SQLException {
        Cliente existente = buscarPorTelefone(telefone);
        if (existente != null) {
            return existente;
        }

        String sql = "INSERT INTO dbo.CLIENTE (nome, telefone) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String digitos = normalizarTelefone(telefone);
            stmt.setString(1, nome);
            stmt.setString(2, digitos);
            stmt.executeUpdate();

            Cliente novo = new Cliente(nome, digitos, null);
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    novo.setIdCliente(rs.getInt(1));
                }
            }
            return novo;
        }
    }
}
