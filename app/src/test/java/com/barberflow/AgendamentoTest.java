package com.barberflow;

import com.barberflow.model.Agendamento;
import com.barberflow.model.Servico;
import org.junit.Test;
import java.sql.Timestamp;

import static org.junit.Assert.*;

/**
 * Unidade 04 - Tema 3: Testes e Depuração de Aplicativos
 * Testes unitários para validação de integridade temporal e regras de negócio do BarberFlow.
 */
public class AgendamentoTest {

    @Test
    public void testValidacaoHorarioFimMaiorQueInicio() {
        // Timestamp.valueOf(LocalDateTime) nao existe no java.sql.Timestamp do Android,
        // por isso o horario e criado a partir de texto e o fim somado em milissegundos.
        Timestamp inicio = Timestamp.valueOf("2026-10-06 14:00:00");
        Timestamp fim = new Timestamp(inicio.getTime() + 40 * 60 * 1000L);

        Agendamento agendamento = new Agendamento(1, 1, 2, inicio, fim);

        assertTrue("O horário de término deve ser posterior ao horário de início",
            agendamento.getDataHoraFim().after(agendamento.getDataHoraInicio()));
    }

    @Test
    public void testStatusInicialConfirmado() {
        Agendamento agendamento = new Agendamento();
        assertEquals("O status padrão do agendamento deve ser CONFIRMADO",
            "CONFIRMADO", agendamento.getStatusAgendamento());
    }

    @Test
    public void testCalculoDuracaoServico() {
        Servico servico = new Servico("Corte Degradê", 40, 40.00);
        assertEquals("A duração do serviço deve ser de 40 minutos", 40, servico.getDuracaoMinutos());
        assertTrue("O preço do serviço deve ser positivo", servico.getPreco() > 0);
    }
}
