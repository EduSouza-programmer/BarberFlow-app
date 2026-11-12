package com.barberflow.network;

import com.barberflow.model.Agendamento;
import com.barberflow.model.Servico;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Interface Retrofit para comunicação com API REST (Unidade 03).
 * Trata o envio e recebimento de dados no formato JSON utilizando a biblioteca Gson.
 */
public interface BarberApiService {

    // Consulta de serviços disponíveis no salão
    @GET("api/servicos")
    Call<List<Servico>> listarServicos();

    // Envio de novo agendamento para o backend (que grava no SQL Server)
    @POST("api/agendamentos")
    Call<Agendamento> criarAgendamento(@Body Agendamento agendamento);

    // Consulta de agendamentos por cliente
    @GET("api/agendamentos/cliente/{idCliente}")
    Call<List<Agendamento>> listarPorCliente(@Path("idCliente") int idCliente);

    // Cancelamento antecipado (Anti-No-Show)
    @PUT("api/agendamentos/{id}/cancelar")
    Call<Void> cancelarAgendamento(@Path("id") int idAgendamento);
}
