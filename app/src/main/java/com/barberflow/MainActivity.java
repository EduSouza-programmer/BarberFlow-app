package com.barberflow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Tela inicial do BarberFlow.
 * Responsável pela identificação do cliente e navegação via Intents (Unidade 01 e 02).
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BarberFlowPrefs";
    private static final String KEY_NOME = "user_name";
    private static final String KEY_TELEFONE = "user_phone";

    private TextInputEditText etNomeCliente;
    private TextInputEditText etTelefoneCliente;
    private Button btnNovoAgendamento;
    private Button btnMeusAgendamentos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNomeCliente = findViewById(R.id.etNomeCliente);
        etTelefoneCliente = findViewById(R.id.etTelefoneCliente);
        btnNovoAgendamento = findViewById(R.id.btnNovoAgendamento);
        btnMeusAgendamentos = findViewById(R.id.btnMeusAgendamentos);

        // Unidade 03: Carrega dados salvos localmente via SharedPreferences
        carregarPreferenciasLocais();

        // Navegação para Novo Agendamento
        btnNovoAgendamento.setOnClickListener(v -> {
            String nome = etNomeCliente.getText() != null ? etNomeCliente.getText().toString().trim() : "";
            String telefone = etTelefoneCliente.getText() != null ? etTelefoneCliente.getText().toString().trim() : "";

            if (nome.isEmpty() || telefone.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha nome e telefone.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Unidade 03: Salva preferências no armazenamento interno
            salvarPreferenciasLocais(nome, telefone);

            Intent intent = new Intent(MainActivity.this, AgendamentoActivity.class);
            intent.putExtra("CLIENTE_NOME", nome);
            intent.putExtra("CLIENTE_TELEFONE", telefone);
            startActivity(intent);
        });

        // Navegação para Meus Agendamentos
        btnMeusAgendamentos.setOnClickListener(v -> {
            String telefone = etTelefoneCliente.getText() != null ? etTelefoneCliente.getText().toString().trim() : "";
            if (telefone.isEmpty()) {
                Toast.makeText(this, "Informe seu WhatsApp para consultar os agendamentos.", Toast.LENGTH_SHORT).show();
                return;
            }

            salvarPreferenciasLocais(etNomeCliente.getText().toString(), telefone);

            Intent intent = new Intent(MainActivity.this, MeusAgendamentosActivity.class);
            intent.putExtra("CLIENTE_TELEFONE", telefone);
            startActivity(intent);
        });
    }

    private void carregarPreferenciasLocais() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String nomeSalvo = prefs.getString(KEY_NOME, "");
        String telefoneSalvo = prefs.getString(KEY_TELEFONE, "");

        if (!nomeSalvo.isEmpty()) etNomeCliente.setText(nomeSalvo);
        if (!telefoneSalvo.isEmpty()) etTelefoneCliente.setText(telefoneSalvo);
    }

    private void salvarPreferenciasLocais(String nome, String telefone) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
             .putString(KEY_NOME, nome)
             .putString(KEY_TELEFONE, telefone)
             .apply();
    }
}
