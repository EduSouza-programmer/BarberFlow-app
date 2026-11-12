-- =============================================================================
-- BANCO DE DADOS: BarberFlow
-- DISCIPLINA: N379 - Sistemas de Computação Móvel (AF / AT)
-- PROJETO INTEGRADO: TCC I - Sistemas de Informação (UNIVERSO)
-- SGBD ALVO: Microsoft SQL Server
-- AUTOR: [Seu Nome Completo]
-- =============================================================================

-- 1. Criação do Banco de Dados (Opcional caso já exista)
-- CREATE DATABASE BarberFlowDB;
-- USE BarberFlowDB;
-- GO

-- 2. Limpeza prévia de tabelas (ordem inversa de dependência de chaves estrangeiras)
IF OBJECT_ID('dbo.AGENDAMENTO', 'U') IS NOT NULL DROP TABLE dbo.AGENDAMENTO;
IF OBJECT_ID('dbo.SERVICO', 'U') IS NOT NULL DROP TABLE dbo.SERVICO;
IF OBJECT_ID('dbo.CLIENTE', 'U') IS NOT NULL DROP TABLE dbo.CLIENTE;
IF OBJECT_ID('dbo.BARBEIRO', 'U') IS NOT NULL DROP TABLE dbo.BARBEIRO;
GO

-- =============================================================================
-- 3. DDL: CRIAÇÃO DAS TABELAS COM CHAVES PRIMÁRIAS E TIPOS COMPATÍVEIS
-- =============================================================================

-- Tabela: BARBEIRO
-- Representa os profissionais prestadores de serviços da barbearia
CREATE TABLE dbo.BARBEIRO (
    id_barbeiro INT IDENTITY(1,1) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NULL,
    percentual_comissao DECIMAL(5,2) NOT NULL DEFAULT 50.00,
    ativo BIT NOT NULL DEFAULT 1,
    CONSTRAINT PK_Barbeiro PRIMARY KEY CLUSTERED (id_barbeiro)
);
GO

-- Tabela: CLIENTE
-- Representa os usuários finais que utilizam o aplicativo móvel
CREATE TABLE dbo.CLIENTE (
    id_cliente INT IDENTITY(1,1) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NULL,
    data_cadastro DATETIME NOT NULL DEFAULT GETDATE(),
    ativo BIT NOT NULL DEFAULT 1,
    CONSTRAINT PK_Cliente PRIMARY KEY CLUSTERED (id_cliente),
    CONSTRAINT UQ_Cliente_Telefone UNIQUE (telefone)
);
GO

-- Tabela: SERVICO
-- Catálogo de serviços oferecidos com preço e duração estimada em minutos
CREATE TABLE dbo.SERVICO (
    id_servico INT IDENTITY(1,1) NOT NULL,
    nome_servico VARCHAR(80) NOT NULL,
    descricao VARCHAR(255) NULL,
    duracao_minutos INT NOT NULL,
    preco DECIMAL(10,2) NOT NULL,
    ativo BIT NOT NULL DEFAULT 1,
    CONSTRAINT PK_Servico PRIMARY KEY CLUSTERED (id_servico),
    CONSTRAINT CK_Servico_Duracao CHECK (duracao_minutos > 0),
    CONSTRAINT CK_Servico_Preco CHECK (preco >= 0.00)
);
GO

-- Tabela: AGENDAMENTO
-- Registra a reserva de horário, garantindo a integridade referencial com as entidades anteriores
CREATE TABLE dbo.AGENDAMENTO (
    id_agendamento INT IDENTITY(1,1) NOT NULL,
    id_cliente INT NOT NULL,
    id_barbeiro INT NOT NULL,
    id_servico INT NOT NULL,
    data_hora_inicio DATETIME NOT NULL,
    data_hora_fim DATETIME NOT NULL,
    status_agendamento VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    observacoes VARCHAR(200) NULL,
    data_registro DATETIME NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT PK_Agendamento PRIMARY KEY CLUSTERED (id_agendamento),
    CONSTRAINT FK_Agendamento_Cliente FOREIGN KEY (id_cliente) 
        REFERENCES dbo.CLIENTE (id_cliente) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT FK_Agendamento_Barbeiro FOREIGN KEY (id_barbeiro) 
        REFERENCES dbo.BARBEIRO (id_barbeiro) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT FK_Agendamento_Servico FOREIGN KEY (id_servico) 
        REFERENCES dbo.SERVICO (id_servico) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT CK_Agendamento_Status CHECK (status_agendamento IN ('PENDENTE', 'CONFIRMADO', 'CANCELADO', 'CONCLUIDO')),
    CONSTRAINT CK_Agendamento_Horario CHECK (data_hora_fim > data_hora_inicio)
);
GO

-- Criação de Índices para otimização de consultas do aplicativo móvel (RNF de tempo de resposta)
CREATE NONCLUSTERED INDEX IX_Agendamento_DataBarbeiro 
ON dbo.AGENDAMENTO (data_hora_inicio, id_barbeiro);
GO

CREATE NONCLUSTERED INDEX IX_Agendamento_Cliente 
ON dbo.AGENDAMENTO (id_cliente);
GO

-- =============================================================================
-- 4. DML: COMANDOS INSERT INTO COM DADOS REAIS PARA POVOAMENTO INICIAL
-- =============================================================================

-- Inserção de Barbeiros
INSERT INTO dbo.BARBEIRO (nome, telefone, email, percentual_comissao, ativo) VALUES
('Carlos Eduardo Silva', '(21) 98765-4321', 'carlos.barber@email.com', 50.00, 1),
('Marcos Paulo Souza', '(21) 99123-4567', 'marcos.fade@email.com', 50.00, 1),
('Rafael Ribeiro Costa', '(21) 97321-8899', 'rafael.barba@email.com', 45.00, 1);
GO

-- Inserção de Clientes
INSERT INTO dbo.CLIENTE (nome, telefone, email) VALUES
('João Victor Ferreira', '(21) 97111-2233', 'joao.victor@email.com'),
('Lucas Gabriel Lima', '(21) 98222-3344', 'lucas.lima@email.com'),
('Matheus Rodrigues Santos', '(21) 99333-4455', 'matheus.santos@email.com'),
('Felipe de Almeida Rocha', '(21) 96444-5566', 'felipe.almeida@email.com');
GO

-- Inserção de Serviços
INSERT INTO dbo.SERVICO (nome_servico, descricao, duracao_minutos, preco, ativo) VALUES
('Corte Tradicional / Social', 'Corte masculino na tesoura ou máquina simples', 30, 30.00, 1),
('Corte Degradê / Fade Moderno', 'Degradê na navalha ou shaver com acabamento preciso', 40, 40.00, 1),
('Barboterapia Completa', 'Barba feita com toalha quente, hidratação e óleos essenciais', 30, 35.00, 1),
('Combo Premium (Corte + Barba)', 'Degradê completo acompanhado de barboterapia relaxante', 60, 65.00, 1),
('Sobrancelha na Navalha', 'Alinhamento e limpeza do design da sobrancelha', 15, 15.00, 1);
GO

-- Inserção de Agendamentos Reais de Exemplo
INSERT INTO dbo.AGENDAMENTO (id_cliente, id_barbeiro, id_servico, data_hora_inicio, data_hora_fim, status_agendamento, observacoes) VALUES
(1, 1, 2, '2026-10-06 14:00:00', '2026-10-06 14:40:00', 'CONFIRMADO', 'Cliente prefere degradê navalhado baixo'),
(2, 2, 4, '2026-10-06 15:00:00', '2026-10-06 16:00:00', 'CONFIRMADO', 'Combo completo'),
(3, 1, 3, '2026-10-06 16:00:00', '2026-10-06 16:30:00', 'PENDENTE', 'Aguardando confirmação pelo app'),
(4, 3, 1, '2026-10-06 16:30:00', '2026-10-06 17:00:00', 'CONCLUIDO', 'Atendimento finalizado com sucesso');
GO

-- Verificação dos dados inseridos com JOIN informativo
SELECT 
    A.id_agendamento,
    A.data_hora_inicio,
    C.nome AS nome_cliente,
    C.telefone AS telefone_cliente,
    B.nome AS nome_barbeiro,
    S.nome_servico,
    S.preco,
    A.status_agendamento
FROM dbo.AGENDAMENTO A
INNER JOIN dbo.CLIENTE C ON A.id_cliente = C.id_cliente
INNER JOIN dbo.BARBEIRO B ON A.id_barbeiro = B.id_barbeiro
INNER JOIN dbo.SERVICO S ON A.id_servico = S.id_servico
ORDER BY A.data_hora_inicio ASC;
GO
