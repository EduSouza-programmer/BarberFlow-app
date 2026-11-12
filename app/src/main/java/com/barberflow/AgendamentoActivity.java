package com.barberflow;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.barberflow.dao.AgendamentoDAO;
import com.barberflow.dao.BarbeiroDAO;
import com.barberflow.dao.ClienteDAO;
import com.barberflow.dao.HorarioIndisponivelException;
import com.barberflow.dao.ServicoDAO;
import com.barberflow.model.Agendamento;
import com.barberflow.model.Barbeiro;
import com.barberflow.model.Cliente;
import com.barberflow.model.Servico;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity responsável pelo formulário de agendamento de serviços.
 * Aplica os conceitos da Unidade 02 (Spinners, Dialogs, Eventos e feedback visual).
 */
public class AgendamentoActivity extends AppCompatActivity {

    private Spinner spServicos;
    private Spinner spBarbeiros;
    private Button btnSelecionarData;
    private Button btnSelecionarHora;
    private Button btnConfirmar;
    private TextView tvResumo;

    private int ano, mes, dia, hora = -1, minuto = -1;
    private String clienteNome;
    private String clienteTelefone;

    // Catálogos carregados do SQL Server (tabelas SERVICO e BARBEIRO)
    private final List<Servico> servicos = new ArrayList<>();
    private final List<Barbeiro> barbeiros = new ArrayList<>();

    // O Android proíbe acesso à rede na thread principal: o banco é acessado nesta thread separada
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agendamento);

        // Recebe parâmetros passados pela MainActivity via Intent
        clienteNome = getIntent().getStringExtra("CLIENTE_NOME");
        clienteTelefone = getIntent().getStringExtra("CLIENTE_TELEFONE");

        spServicos = findViewById(R.id.spServicos);
        spBarbeiros = findViewById(R.id.spBarbeiros);
        btnSelecionarData = findViewById(R.id.btnSelecionarData);
        btnSelecionarHora = findViewById(R.id.btnSelecionarHora);
        btnConfirmar = findViewById(R.id.btnConfirmar);
        tvResumo = findViewById(R.id.tvResumo);

        Button btnVerNoMapa = findViewById(R.id.btnVerNoMapa);
        btnVerNoMapa.setOnClickListener(v -> abrirLocalizacaoGps());

        criarCanalNotificacao();
        solicitarPermissaoNotificacao();
        configurarSeletores();
        configurarBotaoConfirmar();
        carregarCatalogos();
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
     * Unidade 04 - Tema 1: Uso de GPS e Mapas
     * Dispara Intent implícita para traçar rota até a barbearia no Google Maps
     */
    private void abrirLocalizacaoGps() {
        // Coordenadas simuladas da barbearia parceira da comunidade
        android.net.Uri gmmIntentUri = android.net.Uri.parse("geo:0,0?q=Barbearia+Local");
        android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback caso não tenha o app de mapas instalado
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri));
        }
    }

    /**
     * Unidade 04 - Tema 2: Notificações Nativas e Permissões
     * Cria canal de notificação para alertar sobre o agendamento (Mecanismo Anti-No-Show)
     */
    private void criarCanalNotificacao() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Lembretes BarberFlow";
            String description = "Notificações de confirmação e lembretes de atendimento";
            int importance = android.app.NotificationManager.IMPORTANCE_HIGH;
            android.app.NotificationChannel channel = new android.app.NotificationChannel("BARBERFLOW_CHANNEL", name, importance);
            channel.setDescription(description);
            android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Android 13+ (API 33) exige que POST_NOTIFICATIONS seja concedida em tempo de execução;
     * só declarar no manifest não basta, e sem isso a notificação não aparece.
     */
    private void solicitarPermissaoNotificacao() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void dispararNotificacaoSucesso(String servico, String horario) {
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, "BARBERFLOW_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Agendamento Confirmado! ✂️")
            .setContentText(String.format("Seu atendimento para %s foi confirmado para %s.", servico, horario))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(1001, builder.build());
        } catch (SecurityException ignored) {
            // Em caso de falta de permissão explícita em versões recentes
        }
    }

    /**
     * Busca serviços e barbeiros ativos no banco (em segundo plano) e preenche os Spinners.
     * Enquanto carrega, ou se o banco estiver fora do ar, o botão Confirmar fica desabilitado.
     */
    private void carregarCatalogos() {
        btnConfirmar.setEnabled(false);
        tvResumo.setText("Carregando serviços e barbeiros...");

        executor.execute(() -> {
            try {
                List<Servico> listaServicos = new ServicoDAO().listarAtivos();
                List<Barbeiro> listaBarbeiros = new BarbeiroDAO().listarAtivos();
                naTela(() -> {
                    servicos.clear();
                    servicos.addAll(listaServicos);
                    barbeiros.clear();
                    barbeiros.addAll(listaBarbeiros);
                    configurarSpinners();
                    tvResumo.setText("Selecione as opções acima para ver o resumo");
                    btnConfirmar.setEnabled(!servicos.isEmpty() && !barbeiros.isEmpty());
                });
            } catch (SQLException e) {
                naTela(() -> {
                    tvResumo.setText("Não foi possível conectar ao banco de dados.\n" +
                        "Verifique se o SQL Server está no ar e se o celular está na mesma rede.");
                    Toast.makeText(this, "Falha ao conectar ao banco: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void configurarSpinners() {
        // Texto exibido no Spinner, ex.: "Corte Tradicional / Social - R$ 30,00 (30 min)"
        NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        List<String> rotulosServicos = new ArrayList<>();
        for (Servico s : servicos) {
            rotulosServicos.add(String.format("%s - %s (%d min)",
                s.getNomeServico(), moeda.format(s.getPreco()), s.getDuracaoMinutos()));
        }
        spServicos.setAdapter(new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, rotulosServicos));

        List<String> nomesBarbeiros = new ArrayList<>();
        for (Barbeiro b : barbeiros) {
            nomesBarbeiros.add(b.getNome());
        }
        spBarbeiros.setAdapter(new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, nomesBarbeiros));

        // Atualiza o resumo sempre que o serviço ou o barbeiro escolhido mudar
        android.widget.AdapterView.OnItemSelectedListener aoMudar = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                atualizarResumo();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        };
        spServicos.setOnItemSelectedListener(aoMudar);
        spBarbeiros.setOnItemSelectedListener(aoMudar);
    }

    private void configurarSeletores() {
        Calendar c = Calendar.getInstance();
        ano = c.get(Calendar.YEAR);
        mes = c.get(Calendar.MONTH);
        dia = c.get(Calendar.DAY_OF_MONTH);

        final CharSequence textoOriginalHora = btnSelecionarHora.getText();

        // DatePickerDialog
        btnSelecionarData.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                ano = year;
                mes = month;
                dia = dayOfMonth;
                btnSelecionarData.setText(String.format("Data: %02d/%02d/%d", dia, mes + 1, ano));
                // Trocou para hoje com um horário que já passou: descarta o horário escolhido
                if (hora >= 0 && !horarioNoFuturo(hora, minuto)) {
                    hora = -1;
                    minuto = -1;
                    btnSelecionarHora.setText(textoOriginalHora);
                    avisarHorarioPassado();
                }
                atualizarResumo();
            }, ano, mes, dia);
            // Bloqueia no calendário os dias anteriores a hoje
            dp.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis() - 1000);
            dp.show();
        });

        // TimePickerDialog
        btnSelecionarHora.setOnClickListener(v -> {
            TimePickerDialog tp = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                // O TimePicker não tem limite mínimo, então a validação é feita aqui
                if (!horarioNoFuturo(hourOfDay, minuteOfHour)) {
                    avisarHorarioPassado();
                    return;
                }
                hora = hourOfDay;
                minuto = minuteOfHour;
                btnSelecionarHora.setText(String.format("Horário: %02d:%02d", hora, minuto));
                atualizarResumo();
            }, 14, 0, true);
            tp.show();
        });
    }

    /** Data escolhida + hora informada, sem segundos. */
    private Calendar montarDataHora(int h, int m) {
        Calendar c = Calendar.getInstance();
        c.set(ano, mes, dia, h, m, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** Regra de negócio: não é permitido agendar em um horário que já passou. */
    private boolean horarioNoFuturo(int h, int m) {
        return montarDataHora(h, m).after(Calendar.getInstance());
    }

    private void avisarHorarioPassado() {
        Toast.makeText(this, "Esse horário já passou. Escolha uma data e um horário futuros.", Toast.LENGTH_LONG).show();
    }

    private void atualizarResumo() {
        if (hora >= 0 && spServicos.getSelectedItem() != null && spBarbeiros.getSelectedItem() != null) {
            String servico = spServicos.getSelectedItem().toString();
            String barbeiro = spBarbeiros.getSelectedItem().toString();
            tvResumo.setText(String.format(
                "Resumo:\nCliente: %s\nServiço: %s\nBarbeiro: %s\nData: %02d/%02d/%d às %02d:%02d",
                clienteNome, servico, barbeiro, dia, mes + 1, ano, hora, minuto
            ));
        }
    }

    /**
     * Monta o Agendamento (Modelo) e grava no SQL Server, em segundo plano:
     * 1) busca o cliente pelo WhatsApp e cadastra se for novo (tabela CLIENTE);
     * 2) grava o agendamento, e o DAO recusa se o barbeiro já estiver ocupado no período.
     * O horário de fim é calculado pela duração do serviço.
     */
    private void salvarAgendamento() {
        Servico servico = servicos.get(spServicos.getSelectedItemPosition());
        Barbeiro barbeiro = barbeiros.get(spBarbeiros.getSelectedItemPosition());

        Calendar inicio = montarDataHora(hora, minuto);
        Calendar fim = (Calendar) inicio.clone();
        fim.add(Calendar.MINUTE, servico.getDuracaoMinutos());

        Agendamento agendamento = new Agendamento();
        agendamento.setIdBarbeiro(barbeiro.getIdBarbeiro());
        agendamento.setIdServico(servico.getIdServico());
        agendamento.setDataHoraInicio(new Timestamp(inicio.getTimeInMillis()));
        agendamento.setDataHoraFim(new Timestamp(fim.getTimeInMillis()));

        String horarioTexto = String.format("%02d/%02d/%d às %02d:%02d", dia, mes + 1, ano, hora, minuto);

        btnConfirmar.setEnabled(false);
        btnConfirmar.setText("Gravando...");

        executor.execute(() -> {
            try {
                Cliente cliente = new ClienteDAO().buscarOuCadastrar(clienteNome, clienteTelefone);
                agendamento.setIdCliente(cliente.getIdCliente());
                new AgendamentoDAO().salvar(agendamento);

                naTela(() -> {
                    // Dispara notificação nativa do Android (Unidade 04 - Tema 2)
                    dispararNotificacaoSucesso(servico.getNomeServico(), horarioTexto);
                    Toast.makeText(this, "✅ Agendamento registrado com sucesso no BarberFlow!", Toast.LENGTH_LONG).show();
                    finish(); // Retorna à tela inicial
                });
            } catch (HorarioIndisponivelException e) {
                // Regra de negócio: dupla marcação recusada pelo banco
                naTela(() -> {
                    liberarBotaoConfirmar();
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Horário indisponível")
                        .setMessage(barbeiro.getNome() + " já tem atendimento em " + horarioTexto +
                            ".\n\nEscolha outro horário ou outro barbeiro.")
                        .setPositiveButton("OK", null)
                        .show();
                });
            } catch (SQLException e) {
                naTela(() -> {
                    liberarBotaoConfirmar();
                    Toast.makeText(this, "Não foi possível gravar no banco: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void liberarBotaoConfirmar() {
        btnConfirmar.setEnabled(true);
        btnConfirmar.setText(R.string.btn_confirm_booking);
    }

    private void configurarBotaoConfirmar() {
        btnConfirmar.setOnClickListener(v -> {
            if (hora < 0) {
                Toast.makeText(this, "Selecione a data e o horário antes de confirmar.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Confere de novo: o horário pode ter passado enquanto a tela estava aberta
            if (!horarioNoFuturo(hora, minuto)) {
                avisarHorarioPassado();
                return;
            }

            // Grava no banco; notificação e retorno à tela inicial só depois de gravar
            salvarAgendamento();
        });
    }
}
