# API de requisicoes TFD/APAC

## Autenticacao JWT

Gerar token:

```http
POST /api/auth/token
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Resposta:

```json
{
  "token_type": "Bearer",
  "access_token": "..."
}
```

Use em todas as chamadas:

```http
Authorization: Bearer SEU_TOKEN
```

## Endpoints

### Listar requisicoes visiveis

```http
GET /requisicoes?status=ENVIADA&tipo=TFD&q=MARIA
```

ADMIN ve todas. COLABORADOR ve apenas as criadas por ele ou vinculadas a sua unidade.

### Criar requisicao

```http
POST /requisicoes
Content-Type: application/json
Authorization: Bearer SEU_TOKEN

{
  "tipo_solicitacao": "TFD",
  "paciente_id": "1",
  "unidade_solicitante_id": "1",
  "profissional_solicitante_id": "1",
  "procedimento_principal_id": "1",
  "cid10_principal": "Z75.3",
  "cid10_secundario": "R69",
  "descricao_diagnostico": "Diagnostico informado",
  "justificativa": "Justificativa medica",
  "prioridade": "NORMAL",
  "tratamentos_previos": "Tratamentos realizados",
  "procedimento_exame_indicado": "Exame indicado",
  "sinais_sintomas": "Sinais e sintomas",
  "necessita_acompanhante": "true",
  "destino": "Maceio"
}
```

### Atualizar requisicao

```http
PUT /requisicoes/1
Content-Type: application/json
Authorization: Bearer SEU_TOKEN

{
  "cid10_principal": "Z75.3",
  "descricao_diagnostico": "Texto atualizado",
  "justificativa": "Justificativa atualizada",
  "observacao": "Correcao solicitada pela regulacao"
}
```

Somente requisicoes em `RASCUNHO`, `ENVIADA` ou `DEVOLVIDA_CORRECAO` podem ser editadas.

### Alterar status

Somente ADMIN.

```http
PATCH /requisicoes/1/status
Content-Type: application/json
Authorization: Bearer SEU_TOKEN

{
  "status": "AUTORIZADA",
  "observacao": "Autorizado pela regulacao",
  "numero_autorizacao": "AUT-2026-0001"
}
```

Status usados pelo fluxo:

- `RASCUNHO`
- `ENVIADA`
- `EM_ANALISE`
- `DEVOLVIDA_CORRECAO`
- `AGUARDANDO_DOCUMENTOS`
- `AUTORIZADA`
- `INDEFERIDA`
- `IMPRESSA`
- `CANCELADA`
- `FINALIZADA`

### Historico

```http
GET /requisicoes/1/historico
Authorization: Bearer SEU_TOKEN
```

Retorna quem alterou, quando, status anterior, status novo, acao e observacao.
