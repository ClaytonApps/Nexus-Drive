# Nexus

Aplicativo Android nativo (Kotlin) para auxílio a motoristas de app — calcula
em tempo real a rentabilidade de cada oferta de corrida nos apps Uber Driver,
99 Driver e inDrive, exibindo um overlay (verde/amarelo/vermelho) com lucro líquido
por hora e por km.

## Como compilar

Cada `git push` para `main` dispara o workflow [.github/workflows/build.yml](.github/workflows/build.yml)
que gera um APK de debug. Para baixar:

1. Aba **Actions** no GitHub.
2. Clica no último workflow run com check verde.
3. Na seção **Artifacts**, baixa `nexus-drive-debug`.
4. Descompacta o zip — dentro tem `app-debug.apk`.

Para compilar localmente, ver `PLAY_STORE_GUIDE.md`.

## Login dos motoristas (Firebase)

O app tem uma tela de login (`LoginActivity`) baseada no **Firebase
Authentication** (e-mail/senha). O login **identifica o motorista** — o
UID da conta vira o `driverId` no histórico de corridas. Ele não é o
controle de acesso: quem libera o uso continua sendo a licença.

O login só fica ativo se o app for compilado com um `google-services.json`
válido. **Sem esse arquivo, a etapa de login é simplesmente ignorada** e o
app funciona como antes (é o caso do APK de debug gerado pelo CI).

Para ativar o login:

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/).
2. Adicione um app Android com o pacote `app.nexus.mobile`.
3. Em **Authentication → Sign-in method**, ative **E-mail/senha**.
4. Em **Firestore Database**, crie o banco (modo produção) e publique as
   regras de segurança da seção abaixo.
5. Baixe o `google-services.json` e coloque em `app/google-services.json`.
6. Commite esse arquivo — o CI passa a gerar APKs com login habilitado.
   (O plugin do Google Services é aplicado automaticamente só quando o
   arquivo existe, então o build não quebra antes desse passo.)

## Acesso: teste grátis e licença

Modelo de cobrança:

- **Teste grátis de 2 dias**, contado a partir do primeiro login da conta.
- Depois disso o app **bloqueia** até resgatar um código de ativação.
- Cada código adiciona **30 dias** de acesso. Mensalidade de **R$ 7,80**.

O estado de cada conta fica no Firestore, na coleção `entitlements`
(documento = UID do motorista), com os campos `trialStartedAt`,
`paidUntil` e `usedCodes`. Guardar no servidor impede que reinstalar o
app ou limpar os dados reinicie o teste. As `SharedPreferences` locais
apenas espelham esses valores para leitura rápida e uso offline.

Regras de segurança do Firestore (cada motorista só acessa o próprio
documento):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /entitlements/{uid} {
      allow read, create, update: if request.auth != null
                                  && request.auth.uid == uid;
      allow delete: if false;
    }
  }
}
```

**Limitação conhecida:** a verificação do código (HMAC) e a gravação de
`paidUntil` acontecem no app. Um usuário avançado com o APK decompilado
poderia gravar `paidUntil` direto no próprio documento. Para um produto
pago em escala, valide os códigos numa Cloud Function (o cliente envia o
código; só o servidor grava `paidUntil`). É a mesma ressalva do
`LICENSE_SECRET` — adequado para MVP / venda direta.

## Permissões

Ver [PLAY_STORE_GUIDE.md](PLAY_STORE_GUIDE.md) para detalhes do uso de
`BIND_ACCESSIBILITY_SERVICE` e `SYSTEM_ALERT_WINDOW`.
