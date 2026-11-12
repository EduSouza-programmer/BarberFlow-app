package com.barberflow;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.barberflow.dao.AgendamentoDAO;
import com.barberflow.dao.ClienteDAO;
import com.barberflow.model.Agendamento;
import com.barberflow.model.Cliente;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lista somente os agendamentos do cliente identificado pelo WhatsApp, lidos do SQL Server,
 * e permite o cancelamento antecipado (Anti-No-Show).
 */
public class MeusAgendamentosActivity extends AppCompatActivity {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final String MSG_SEM_AGENDAMENTOS =
        "Você ainda não possui agendamentos.\n\nToque em voltar e use “Novo Agendamento” para marcar seu horário.";

    private String telefoneCliente;
    private TextView tvSemAgendamentos;

    private final List<Agendamento> agendamentos = new ArrayList<>();
    private final List<String> linhas = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // O Android proíbe acesso à rede na thread principal: o banco é acessado nesta thread separada
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meus_agendamentos);

        telefoneCliente = getIntent().getStringExtra("CLIENTE_TELEFONE");

        TextView tvCliente = findViewById(R.id.tvClienteConsultado);
        tvCliente.setText("WhatsApp: " + telefoneCliente);

        tvSemAgendamentos = findViewById(R.id.tvSemAgendamentos);
        ListView lvAgendamentos = findViewById(R.id.lvAgendamentos);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, linhas);
        lvAgendamentos.setAdapter(adapter);
        // Lista vazia: a ListView mostra este texto no lugar (carregando, sem agendamentos ou erro)
        lvAgendamentos.setEmptyView(tvSemAgendamentos);

        lvAgendamentos.setOnItemClickListener((parent, view, position, id) -> {
            Agendamento agendamento = agendamentos.get(position);
            String status = agendamento.getStatusAgendamento();
            if (!"CONFIRMADO".equals(status) && !"PENDENTE".equals(status)) {
                Toast.makeText(this, "Este agendamento não pode mais ser cancelado.", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                .setTitle("Gerenciar Agendamento")
                .setMessage("Deseja cancelar este agendamento e liberar o horário no salão?\n\n" + linhas.get(position))
                .setPositiveButton("Sim, Cancelar", (dialog, which) -> cancelar(agendamento))
                .setNegativeButton("Voltar", null)
                .show();
        });

        carregarAgendamentos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    /** Executa um trecho na thread da interface, se a tela ainda estiver aberta. */
    private void naTela(Runnable acao) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                acao.run();
            }
        });
    }

    /**
     * Busca o cliente pelo WhatsApp e, se existir, apenas os agendamentos dele.
     * WhatsApp sem cadastro no banco = cliente sem agendamentos.
     */
    private void carregarAgendamentos() {
        mostrarLista(new ArrayList<>(), "Carregando seus agendamentos...");

        executor.execute(() -> {
            try {
                Cliente cliente = new ClienteDAO().buscarPorTelefone(telefoneCliente);
                List<Agendamento> doCliente = cliente == null
                    ? new ArrayList<>()
                    : new AgendamentoDAO().listarPorCliente(cliente.getIdCliente());
                naTela(() -> mostrarLista(doCliente, MSG_SEM_AGENDAMENTOS));
            } catch (SQLException e) {
                naTela(() -> mostrarLista(new ArrayList<>(),
                    "Não foi possível conectar ao banco de dados.\n\n" +
                    "Verifique se o SQL Server está no ar e se o celular está na mesma rede."));
            }
        });
    }

    private void cancelar(Agendamento agendamento) {
        executor.execute(() -> {
            try {
                boolean cancelado = new AgendamentoDAO().cancelar(agendamento.getIdAgendamento());
                naTela(() -> {
                    Toast.makeText(this, cancelado
                            ? "Agendamento cancelado. Horário liberado na barbearia!"
                            : "Este agendamento já não estava ativo.",
                        Toast.LENGTH_LONG).show();
                    carregarAgendamentos();
                });
            } catch (SQLException e) {
                naTela(() -> Toast.makeText(this,
                    "Não foi possível cancelar: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    /** Atualiza a lista na tela; mensagemVazia aparece quando não há itens. */
    private void mostrarLista(List<Agendamento> lista, String mensagemVazia) {
        SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", PT_BR);
        NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);

        agendamentos.clear();
        agendamentos.addAll(lista);
        linhas.clear();
        for (Agendamento a : agendamentos) {
            linhas.add(String.format("📅 %s - %s com %s (%s) [%s]",
                formatoData.format(a.getDataHoraInicio()),
                a.getNomeServico(),
                a.getNomeBarbeiro(),
                moeda.format(a.getPrecoServico()),
                a.getStatusAgendamento()));
        }
        tvSemAgendamentos.setText(mensagemVazia);
        adapter.notifyDataSetChanged();
    }
}
