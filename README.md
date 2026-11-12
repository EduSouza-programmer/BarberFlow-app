# BarberFlow

Sistema de agendamento para barbearias de bairro, criado para reduzir as faltas sem aviso (*no-show*) e impedir a dupla marcação de um mesmo horário.

Projeto acadêmico da Universidade Salgado de Oliveira (UNIVERSO), que integra o TCC I e a disciplina N379 – Sistemas de Computação Móvel.

## O que foi feito

- **Aplicativo Android nativo (Java)** com três telas: identificação do cliente, novo agendamento e meus agendamentos. Inclui notificação ao confirmar a reserva e botão que abre a rota no Google Maps.
- **Banco de dados Microsoft SQL Server**, com script de criação das tabelas (chaves primárias e estrangeiras, restrições de horário) e carga inicial de dados.
- **Arquivo Docker Compose** que sobe o SQL Server pronto para uso, sem instalar nada além do Docker.

O app grava e consulta tudo no SQL Server: serviços e barbeiros vêm do banco, o cliente é cadastrado pelo WhatsApp na primeira reserva, e o banco recusa um segundo agendamento para o mesmo barbeiro em um horário já ocupado.

### Decisão de arquitetura

Nesta etapa (prova de conceito), o banco roda localmente em Docker e o app o acessa pela rede local. No TCC II, o app deixa de acessar o banco diretamente: ele passará a chamar uma **API REST (backend)**, e só o backend falará com o SQL Server. Assim, usuário e senha do banco ficam apenas no servidor, e a regra de conflito de horário passa a ser verificada em um único lugar.

## Como executar

O banco de dados roda no computador, em Docker. O aplicativo pode rodar de **duas formas**, e a única diferença entre elas é o endereço do banco no arquivo `.env`:

| | Opção A: emulador | Opção B: celular real |
|---|---|---|
| Onde o app roda | No emulador do Android Studio, no mesmo computador do banco | No seu celular Android, conectado por cabo USB |
| `BARBERFLOW_DB_HOST` no `.env` | Fica comentado (não precisa) | Obrigatório: o IP do computador na rede |
| Rede | Não importa | Celular e computador na mesma rede |
| Recomendado para | Avaliação e demonstração (não depende da rede) | Testar no aparelho de verdade |

Os passos 1 a 3 são iguais para as duas opções. No passo 4, siga só a opção escolhida.

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) com Docker Compose (já vem no Docker Desktop). Mínimo de 2 GB de RAM livres para o SQL Server.
- [Android Studio](https://developer.android.com/studio) recente (testado no 2026.2, que já inclui o JDK 25) com o SDK Platform 34.
- **Opção A:** uma imagem de emulador com API 34 ou superior (o Android Studio baixa no passo 4).
- **Opção B:** um celular Android 7.0 ou superior e um cabo USB.

### 1. Clonar o projeto

```bash
git clone <URL-DO-REPOSITORIO> barberflow-app
cd barberflow-app
```

### 2. Subir o banco de dados (SQL Server)

1. Crie o arquivo `.env` a partir do modelo e troque a senha do usuário `sa`:

   ```bash
   cp .env.example .env
   ```

   No Windows (PowerShell): `copy .env.example .env`

   A senha precisa ter no mínimo 8 caracteres e pelo menos 3 destas categorias: maiúscula, minúscula, número e símbolo.

   O mesmo `.env` também diz ao app onde está o banco (`BARBERFLOW_DB_HOST`). Por enquanto, deixe essa linha como está; ela é ajustada no passo 4, conforme a opção escolhida.

2. Suba o contêiner (na primeira vez, o Docker baixa a imagem, que é grande):

   ```bash
   docker compose up -d
   ```

3. Aguarde o status `healthy`:

   ```bash
   docker compose ps
   ```

Todos os comandos `docker compose` precisam ser executados **dentro da pasta do projeto** (onde está o `docker-compose.yml`).

A pasta `database/` é montada dentro do contêiner em `/scripts`, então os comandos abaixo rodam os scripts direto, sem copiar arquivos. A senha é lida do `.env` automaticamente.

### 3. Criar o banco e as tabelas

Os scripts ficam em `database/` e são executados nesta ordem:

| Script | O que faz |
|---|---|
| `00_criar_banco.sql` | Cria o banco `BarberFlowDB` (não faz nada se ele já existir) |
| `01_schema_tabelas.sql` | Cria as tabelas, chaves, restrições e índices, e insere os dados iniciais |

1. Crie o banco:

   ```bash
   docker compose exec sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -b -i /scripts/00_criar_banco.sql
   ```

2. Crie as tabelas e carregue os dados iniciais:

   ```bash
   docker compose exec sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -d BarberFlowDB -f 65001 -b -i /scripts/01_schema_tabelas.sql
   ```

   > **Atenção:** o `01_schema_tabelas.sql` começa apagando as tabelas e recria tudo. Executá-lo de novo apaga os dados gravados.

3. Confira as tabelas criadas:

   ```bash
   docker compose exec sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -d BarberFlowDB -Q "SELECT name FROM sys.tables ORDER BY name"
   ```

   Devem aparecer `AGENDAMENTO`, `BARBEIRO`, `CLIENTE` e `SERVICO`.

4. Confira os dados iniciais (3 barbeiros, 4 clientes, 5 serviços e 4 agendamentos):

   ```bash
   docker compose exec sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -d BarberFlowDB -Q "SELECT (SELECT COUNT(*) FROM dbo.BARBEIRO) AS barbeiros, (SELECT COUNT(*) FROM dbo.CLIENTE) AS clientes, (SELECT COUNT(*) FROM dbo.SERVICO) AS servicos, (SELECT COUNT(*) FROM dbo.AGENDAMENTO) AS agendamentos"
   ```

### Parar e retomar o banco

```bash
docker compose stop     # para o contêiner
docker compose start    # inicia de novo, com os dados preservados
docker compose down     # remove o contêiner e mantém os dados
docker compose down -v  # remove o contêiner E apaga os dados
```

### 4. Executar o aplicativo

O app lê o `.env` **na hora do build**: a senha (`MSSQL_SA_PASSWORD`) e o endereço do banco (`BARBERFLOW_DB_HOST`). A porta (`1433`), o nome do banco (`BarberFlowDB`) e o usuário (`sa`) são fixos e não precisam ser configurados.

Siga **apenas uma** das opções abaixo.

#### Opção A: emulador Android

1. **Confira o `.env`.** A linha `BARBERFLOW_DB_HOST` deve continuar **comentada** (com `#` na frente). Sem ela, o app usa `10.0.2.2`, que é o endereço do computador visto de dentro do emulador. O arquivo fica assim:

   ```properties
   MSSQL_SA_PASSWORD=SuaSenha#2026
   # BARBERFLOW_DB_HOST=192.168.0.10
   ```

2. **Abra o projeto.** No Android Studio, escolha **File > Open**, selecione a pasta `barberflow-app` e aguarde o sincronismo do Gradle (ele baixa o Gradle 9.6 e as dependências sozinho).
3. **Crie o emulador (só na primeira vez).** Abra **Device Manager** (menu **View > Tool Windows > Device Manager**), clique em **+ > Create Virtual Device**, escolha um aparelho (por exemplo, **Pixel 8**), selecione uma imagem do sistema com **API 34 ou superior** (baixe-a se for pedido) e clique em **Finish**.
4. **Execute.** Selecione o emulador na barra superior do Android Studio e clique em **Run** (▶). O emulador abre e o app é instalado nele.

#### Opção B: celular real (pelo cabo USB)

1. **Coloque o celular e o computador na mesma rede.** Por exemplo, o computador no cabo e o celular no Wi-Fi do mesmo roteador. O celular acessa o banco pela rede, não pelo cabo USB (o cabo serve só para instalar o app).
2. **Descubra o IP do computador na rede local:**
   - Linux: `ip -4 addr` (procure o endereço da placa de rede, por exemplo `192.168.0.10`).
   - Windows: `ipconfig` (campo **Endereço IPv4**).
   - macOS: `ipconfig getifaddr en0`.

   Não use `127.0.0.1` nem `localhost`: no celular, esses endereços apontam para o próprio celular.
3. **Informe o IP no `.env`.** Tire o `#` da linha `BARBERFLOW_DB_HOST` e coloque o IP do passo anterior. O arquivo fica assim:

   ```properties
   MSSQL_SA_PASSWORD=SuaSenha#2026
   BARBERFLOW_DB_HOST=192.168.0.10
   ```

4. **Ative a depuração USB no celular (só na primeira vez).**
   - Em **Configurações > Sobre o telefone**, toque 7 vezes em **Número da versão** (ou **Número de compilação**; em aparelhos Samsung fica em **Informações do software**). Isso libera as **Opções do desenvolvedor**.
   - Em **Configurações > Opções do desenvolvedor**, ative **Depuração USB**.
   - Conecte o cabo e, no aviso que aparece no celular, toque em **Permitir**.
5. **Abra o projeto.** No Android Studio, escolha **File > Open**, selecione a pasta `barberflow-app` e aguarde o sincronismo do Gradle.
6. **Execute.** Selecione o celular na barra superior do Android Studio e clique em **Run** (▶). O app é instalado e aberto no celular.

Sem abrir o Android Studio, com o celular conectado (ou o emulador aberto), também dá para compilar e instalar pelo terminal:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.barberflow/.MainActivity
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

#### Como saber se o app conectou ao banco

Na tela inicial, informe um nome e um WhatsApp e toque em **Novo Agendamento**. Se as listas de serviços e barbeiros aparecerem preenchidas, o app está conectado ao SQL Server. Se aparecer **"Não foi possível conectar ao banco de dados"**, veja a tabela de problemas mais abaixo.

> **Trocou de opção ou mudou o `.env`?** Clique em **Run** de novo: os valores do `.env` são gravados no app durante o build, então o app antigo continua usando o endereço anterior até ser recompilado. Não é preciso mexer no contêiner, a menos que a senha tenha mudado.

### 5. Testar a persistência e o bloqueio de dupla marcação

1. **Primeiro agendamento:** informe um nome e o WhatsApp `21900000001`, toque em **Novo Agendamento**, escolha um barbeiro, uma data e um horário futuros, e confirme. O app volta à tela inicial e mostra a notificação.
2. **Dupla marcação:** volte, troque o WhatsApp para `21900000002` e tente o **mesmo barbeiro, na mesma data e horário**. O app mostra "Horário indisponível" e não grava nada.
3. **Outro barbeiro:** no mesmo horário, escolha outro barbeiro e confirme. Desta vez é aceito, porque a regra vale por barbeiro.
4. **Meus Agendamentos:** com o WhatsApp `21900000001`, toque em **Meus Agendamentos**: aparece só o agendamento desse cliente. Toque nele e cancele; o status passa a `CANCELADO`, e o horário fica livre de novo.
5. **Conferir no banco:**

   ```bash
   docker compose exec sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -d BarberFlowDB -Q "SELECT A.id_agendamento, C.nome AS cliente, C.telefone, B.nome AS barbeiro, A.data_hora_inicio, A.status_agendamento FROM dbo.AGENDAMENTO A JOIN dbo.CLIENTE C ON C.id_cliente = A.id_cliente JOIN dbo.BARBEIRO B ON B.id_barbeiro = A.id_barbeiro ORDER BY A.id_agendamento"
   ```

   Os clientes novos aparecem na tabela `CLIENTE`, e cancelar um agendamento não apaga o cliente nem o registro: só muda o status.

Também não é possível agendar em data ou horário que já passaram: o calendário bloqueia os dias anteriores, e o app recusa horários vencidos.


## Tecnologias

Java (código compilado para Java 17, build com JDK 25), Android (minSdk 24, targetSdk 34), Gradle 9.6 com Android Gradle Plugin 9.5.1, Retrofit/Gson (API prevista para o TCC II), driver JDBC jTDS e Microsoft SQL Server 2022 (edição Developer) em Docker.
