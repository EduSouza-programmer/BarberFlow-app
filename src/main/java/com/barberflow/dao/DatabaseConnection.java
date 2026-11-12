package com.barberflow.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gerenciador de conexão com o banco de dados Microsoft SQL Server.
 * Atende às exigências da disciplina de Sistemas de Computação Móvel e TCC I.
 */
public class DatabaseConnection {

    // Parâmetros de conexão padrão com o SQL Server
    private static final String SERVER = "localhost";
    private static final String PORT = "1433";
    private static final String DATABASE = "BarberFlowDB";
    private static final String USER = "sa";
    private static final String PASSWORD = "YourStrongPassword123";

    // URL JDBC com parâmetros de segurança e encriptação adequados
    private static final String CONNECTION_URL = String.format(
        "jdbc:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s;encrypt=false;trustServerCertificate=true;",
        SERVER, PORT, DATABASE, USER, PASSWORD
    );

    /**
     * Obtém uma conexão ativa com o SQL Server.
     * @return Objeto Connection pronto para operações SQL
     * @throws SQLException Em caso de falha de autenticação ou rede
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(CONNECTION_URL);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC do SQL Server não localizado no classpath: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para testar a conectividade no terminal.
     */
    public static void main(String[] args) {
        System.out.println("Tentando conectar ao SQL Server...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Conexão com o SQL Server estabelecida com sucesso!");
            }
        } catch (SQLException e) {
            System.err.println("❌ Falha na conexão: " + e.getMessage());
        }
    }
}
