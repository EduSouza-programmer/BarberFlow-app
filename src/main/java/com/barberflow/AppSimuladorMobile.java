package com.barberflow;

import com.barberflow.dao.AgendamentoDAO;
import com.barberflow.model.Agendamento;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ponto de entrada para validação e testes da lógica do BarberFlow em Java.
 * Simula a jornada do usuário no aplicativo móvel conectando ao SQL Server.
 */
public class AppSimuladorMobile {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("        BARBERFLOW - SISTEMA DE COMPUTAÇÃO MÓVEL         ");
        System.out.println("=========================================================");

        AgendamentoDAO dao = new AgendamentoDAO();

        // 1. Simulação: Cliente seleciona data e horário para agendar corte
        System.out.println("\n[1] Tentando registrar novo agendamento via aplicativo...");
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusMinutes(40);

        Agendamento novo = new Agendamento(
            1, // idCliente: João Victor
            1, // idBarbeiro: Carlos Eduardo
            2, // idServico: Corte Degradê (40 min)
            Timestamp.valueOf(inicio),
            Timestamp.valueOf(fim)
        );
        novo.setObservacoes("Agendado via App BarberFlow");

        try {
            boolean sucesso = dao.salvar(novo);
            if (sucesso) {
                System.out.printf("✅ Agendamento realizado com sucesso! ID: %d%n", novo.getIdAgendamento());
                System.out.printf("   Horário: %s até %s%n", inicio, fim);
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Não foi possível salvar (verifique se o SQL Server está ativo): " + e.getMessage());
        }

        // 2. Simulação: Teste de Concorrência (Tentativa de agendamento no mesmo horário)
        System.out.println("\n[2] Testando proteção de choque de horários (Concorrência)...");
        Agendamento concorrente = new Agendamento(
            2, // Outro cliente tentando o mesmo barbeiro e horário
            1,
            1,
            Timestamp.valueOf(inicio.plusMinutes(10)),
            Timestamp.valueOf(inicio.plusMinutes(40))
        );

        try {
            dao.salvar(concorrente);
            System.out.println("❌ Falha de segurança: permitiu sobreposição de horário!");
        } catch (SQLException e) {
            System.out.println("✅ Proteção validada com sucesso: " + e.getMessage());
        }

        // 3. Simulação: Consulta de "Meus Agendamentos"
        System.out.println("\n[3] Consultando agendamentos do cliente ID 1...");
        try {
            List<Agendamento> lista = dao.listarPorCliente(1);
            for (Agendamento a : lista) {
                System.out.printf("   - [%s] %s com %s (R$ %.2f) | Status: %s%n",
                    a.getDataHoraInicio(), a.getNomeServico(), a.getNomeBarbeiro(), a.getPrecoServico(), a.getStatusAgendamento());
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Erro na consulta: " + e.getMessage());
        }

        System.out.println("\n=========================================================");
        System.out.println(" Simulação concluída. Pronto para empacotamento da AT.   ");
        System.out.println("=========================================================");
    }
}
