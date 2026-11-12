package com.barberflow.dao;

import com.barberflow.BuildConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gerenciador de conexão com o banco de dados Microsoft SQL Server.
 *
 * Os dados de conexão vêm do BuildConfig, gerado no build a partir do .env
 * (arquivo não versionado, o mesmo usado pelo docker compose).
 * Assim, a senha não fica escrita no código-fonte.
 *
 * Observação: nesta prova de conceito o app acessa o banco diretamente pela rede local.
 * No TCC II esse acesso passa a ser feito por uma API REST.
 */
public class DatabaseConnection {

    // ssl=off: o jTDS 1.3.1 não negocia TLS 1.2, então o SQL Server não pode estar com "Force Encryption" ativo.
    // loginTimeout/socketTimeout: evita que o app fique travado se o servidor estiver fora do ar.
    private static final String CONNECTION_URL = String.format(
        "jdbc:jtds:sqlserver://%s:%s/%s;ssl=off;loginTimeout=8;socketTimeout=20",
        BuildConfig.DB_HOST, BuildConfig.DB_PORT, BuildConfig.DB_NAME
    );

    /**
     * Obtém uma conexão ativa com o SQL Server.
     * Deve ser chamado fora da thread principal: o Android proíbe acesso à rede na thread da interface.
     *
     * @return Objeto Connection pronto para operações SQL
     * @throws SQLException Em caso de falha de autenticação ou rede
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            return DriverManager.getConnection(CONNECTION_URL, BuildConfig.DB_USER, BuildConfig.DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC jTDS não localizado no classpath: " + e.getMessage());
        }
    }
}
