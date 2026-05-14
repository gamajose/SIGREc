# SIGREc TFD/APAC

Sistema web interno para gestao de solicitacoes, regulacao, autorizacao e impressao de formularios TFD/APAC.

## Stack

- Java 21
- Spring Boot 3
- Spring Security
- Thymeleaf + Bootstrap CDN
- PostgreSQL
- Flyway
- OpenPDF

## Executar em desenvolvimento

1. Suba o PostgreSQL:

```powershell
docker compose up -d
```

2. Execute com JDK 21 e Maven:

```powershell
mvn spring-boot:run
```

3. Acesse:

```text
http://localhost:8080
```

Usuario inicial:

```text
admin / admin123
```

Troque a senha antes de usar com dados reais.

## Modulos entregues

- Login com Spring Security, BCrypt, perfis e auditoria de acesso.
- Cadastro de pacientes, unidades, profissionais, procedimentos e usuarios.
- Solicitacao TFD e APAC com fila regulatoria.
- Historico automatico de criacao, mudanca de status, anexos, impressao e reimpressao.
- Autorizacao, indeferimento, devolucao, aguardando documentos, cancelamento e finalizacao.
- Geracao de PDF preenchido para TFD/APAC e armazenamento do arquivo.
- Anexos por solicitacao.
- Relatorios de fila, status, unidade, reimpressoes e exportacao CSV.

## Variaveis principais

```text
DB_HOST=192.168.3.200
DB_PORT=5432
DB_NAME=segrec
DB_USER=jose
DB_PASSWORD=Joseluiz1
SIGREC_STORAGE_PATH=./storage
```

Com essas variaveis, a aplicacao acessa o PostgreSQL do container `postgres17` na VM `192.168.3.200`, porta `5432`, banco `segrec`.

## Observacoes

Os arquivos enviados como Excel, TXT, PDF e imagens devem ser tratados como referencia de campos e layout. A aplicacao consulta e grava dados no PostgreSQL. O PostgreSQL deve ficar acessivel apenas ao servidor da aplicacao; unidades externas devem acessar o sistema por HTTPS/VPN.
