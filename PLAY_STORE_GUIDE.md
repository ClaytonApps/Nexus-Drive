# Guia de Submissão à Google Play Store — Nexus Drive

Este documento existe porque o Google Play **bane apps que usam
`BIND_ACCESSIBILITY_SERVICE` sem justificar com transparência o uso**.
Siga estes passos exatamente; eles foram desenhados para passar na
revisão da Play Console.

---

## 1. Política de Acessibilidade — o que o Google exige

A política oficial diz: o serviço de acessibilidade só pode ser usado
para **acessibilidade real** (ajudar usuários com deficiência) **ou**
para um propósito legítimo claramente comunicado ao usuário, com
disclosure prominente *antes* da solicitação.

O Nexus Drive se enquadra na segunda categoria: somos uma ferramenta
de produtividade para motoristas que precisa ler o conteúdo da tela
de ofertas para calcular lucro. Para passar na revisão, precisamos:

1. **Disclosure prominente in-app** — feito na `OnboardingActivity`.
2. **Política de privacidade pública** — link obrigatório na Play Console.
3. **Justificativa escrita** no formulário "Declaração de uso da API
   de acessibilidade" da Play Console.

---

## 2. Texto a colar no formulário da Play Console

> **Funcionalidade principal do app:**
> O Nexus Drive é uma ferramenta de produtividade para motoristas
> de aplicativo (Uber Driver e 99 Driver). Ele calcula em tempo real
> a rentabilidade de cada oferta de corrida (lucro por hora e por
> quilômetro), considerando o custo operacional pré-configurado pelo
> motorista (combustível, manutenção, depreciação).
>
> **Por que precisamos da API de Acessibilidade:**
> Os apps de motorista exibem o valor, distância e duração de cada
> oferta apenas na tela. Como esses dados não estão disponíveis em
> nenhuma API pública, a única forma de o Nexus Drive auxiliar o
> motorista é lendo o texto já visível na tela do próprio motorista.
>
> **Escopo restrito:**
> O serviço só processa eventos dos pacotes `com.ubercab.driver` e
> `com.taxis99.driver`, configurados no atributo `android:packageNames`
> do `accessibility_service_config.xml`. Eventos de qualquer outro
> app são descartados pelo sistema operacional antes de chegarem ao
> nosso código.
>
> **Não substituímos um leitor de tela:** o Nexus Drive não pretende
> auxiliar usuários com deficiência visual; é claramente apresentado
> como ferramenta de produtividade financeira. O usuário concorda
> com isso através do disclosure exibido na primeira execução.

---

## 3. Disclosure in-app (já implementado)

A `OnboardingActivity` exibe três blocos antes de pedir qualquer
permissão. Todo o texto está em `res/values/strings.xml` nos campos
`disclosure_*` para facilitar tradução e revisão jurídica:

- `disclosure_accessibility_*` — explica leitura de tela.
- `disclosure_overlay_*` — explica sobreposição.
- `disclosure_data_*` — explica envio de dados (opt-in).

Cada bloco responde explicitamente: **por que precisamos**, **o que
NÃO fazemos**, e **como revogar**.

---

## 4. Política de Privacidade — checklist obrigatório

Hospede em URL pública (ex.: `https://nexusdrive.example.com/privacy`).
A página deve cobrir:

- [ ] Identidade do controlador (CNPJ, e-mail de contato).
- [ ] Lista de dados coletados pelo serviço de acessibilidade
      (apenas: valor, distância, duração da oferta visível).
- [ ] Lista de dados **não** coletados (mensagens, contatos,
      conteúdo de outros apps, dados do passageiro).
- [ ] Finalidade do tratamento (cálculo local + envio opcional ao
      backend para histórico do motorista).
- [ ] Base legal (LGPD: consentimento livre e informado, art. 7º, I).
- [ ] Tempo de retenção e direito de exclusão.
- [ ] Lista de subprocessadores (servidor Django/Firebase, hospedagem).
- [ ] Como o usuário revoga o consentimento (desligar acessibilidade
      e/ou desligar o switch "Enviar corridas aceitas").

---

## 5. Permissões declaradas e justificativas internas

| Permissão | Por que está no manifest | Visível ao usuário? |
|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Ler ofertas de corrida nos apps Uber/99 | Sim — tela "Acessibilidade" do Android |
| `SYSTEM_ALERT_WINDOW` | Mostrar o "semáforo" sobre o app de motorista | Sim — Configurações → Sobreposição |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Envio opcional de corridas aceitas | Não exige diálogo |
| `RECEIVE_BOOT_COMPLETED` | Reenfileirar uploads pendentes após reiniciar o aparelho | Não exige diálogo |

**Detecção de apps instalados:** o app **não** usa `QUERY_ALL_PACKAGES`.
A visibilidade de pacotes é restrita por um elemento `<queries>` no
manifest, que lista explicitamente apenas os pacotes Uber/99 — a
abordagem recomendada pelo Google, que não exige justificativa na
Play Console.

---

## 6. Erros que reprovam a submissão

- ❌ Pedir a permissão de acessibilidade **antes** de mostrar o
  disclosure → fluxo do app deve ser: tela de disclosure → botão
  "Conceder" que leva às Configurações do sistema. Nunca tentar
  conceder programaticamente.
- ❌ Cliques automáticos em apps de terceiros — o Nexus Drive **só
  lê**, nunca toca por você.
- ❌ Coletar dados de apps fora da allow-list — o filtro do XML é
  obrigatório e o filtro imperativo no `onAccessibilityEvent` é
  uma segunda barreira.
- ❌ Política de privacidade genérica copiada de gerador online sem
  citar acessibilidade — o revisor lê.
- ❌ Esquecer o vídeo demonstrativo em "Declaração de uso de
  acessibilidade" — grave 30s mostrando: (1) tela de disclosure,
  (2) ativação da permissão, (3) overlay aparecendo só sobre Uber/99.

---

## 7. Antes de enviar para revisão — checklist final

- [ ] `applicationId` em `app/build.gradle.kts` é o final
      (`com.nexusdrive.app` ou outro reservado pela sua conta).
- [ ] Política de privacidade publicada e linkada na Play Console.
- [ ] Vídeo de demonstração em mp4 ≤ 30s.
- [ ] Screenshots da tela de disclosure (`OnboardingActivity`).
- [ ] Texto da seção 2 colado em "Declaração de uso de acessibilidade".
- [ ] Build de release assinado com keystore própria, não a debug.
- [ ] Teste com a build de release em dispositivo limpo: o
      onboarding precisa aparecer antes de qualquer outra tela.
