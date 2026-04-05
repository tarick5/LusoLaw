# LusoLaw Marketplace

Marketplace de servicos juridicos de imigracao com backend Spring Boot e frontend responsivo.

## Stack

- Java 17+
- Spring Boot (Web, Security, Validation, JPA)
- H2 (dev)
- JWT para autenticacao
- Stripe (Payment Intent)

## Como executar

1. Configura Java 17+ e Maven
2. (Opcional) define variaveis de ambiente:
   - `APP_JWT_SECRET` (recomendado local e obrigatorio em producao)
   - `STRIPE_API_KEY` (se quiser criar intents reais)
   - `STRIPE_WEBHOOK_SECRET` (obrigatorio para validar webhook Stripe)
   - `APP_SEED_ENABLED=true` (se quiser dados demo no arranque)
   - `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD` (cria conta ADMIN no bootstrap)
3. Executa:

```bash
mvn clean spring-boot:run
```

Aplicacao disponivel em `http://localhost:8080`.

## Seguranca Implementada

- Autenticacao JWT (`/api/auth/login` e `/api/auth/register`)
- Autorizacao por perfil (`CLIENT`, `LAWYER`, `ADMIN`)
- DTOs de resposta sem exposicao de password
- Validacao de payload com Bean Validation
- Regras de ownership em servicos, bookings e pagamentos
- Headers de seguranca + CSP
- Respostas JSON padronizadas para `401/403`
- Rate limiting em login e criacao de contas
- Webhook Stripe com validacao de assinatura
- Role `ADMIN` com endpoints exclusivos de analytics
- Registo com upload documental obrigatorio (ID para CLIENT/LAWYER, comprovativo OA para LAWYER)
- Conta LAWYER criada como `PENDING_REVIEW` ate aprovacao manual de ADMIN

## Endpoints Principais

- `POST /api/auth/register` (multipart/form-data com documentos)
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/services`
- `POST /api/services` (LAWYER)
- `POST /api/bookings` (CLIENT)
- `GET /api/bookings/me` (auth)
- `POST /api/bookings/{id}/respond` (LAWYER dono)
- `POST /api/payments/create-intent/{bookingId}` (CLIENT dono)
- `POST /api/payments/webhook` (Stripe, assinatura obrigatoria)
- `GET /api/admin/dashboard` (ADMIN)
- `GET /api/admin/lawyers/pending` (ADMIN)
- `POST /api/admin/lawyers/{id}/approve` (ADMIN)
- `POST /api/admin/lawyers/{id}/reject` (ADMIN)
- `GET /api/admin/compliance/identifications` (ADMIN)
- `GET /api/admin/users/{id}/documents/id` (ADMIN)
- `GET /api/admin/users/{id}/documents/lawyer-credential` (ADMIN)

## Observacoes

- `app.seed.enabled` vem desativado por default.
- Sem `APP_JWT_SECRET`, a app gera segredo efemero em runtime (tokens antigos deixam de ser validos em restart).
- Para aceder ao painel admin separado, abre `http://localhost:8080/admin`.
