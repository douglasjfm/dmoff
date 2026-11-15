<p align="center">
  <img src="letmeoff1.jpg" alt="Logo Deixe-me OFF" width="180" />
</p>

<h1 align="center">Deixe-me OFF</h1>

<p align="center">
  <b>Simplesmente um bloqueador de chamadas para Android.</b><br/>
  <i>Não está na sua agenda? Não toca.</i>
</p>

<p align="center">
  <img alt="Plataforma" src="https://img.shields.io/badge/Android-10%2B-80D4D6?style=for-the-badge&logo=android&logoColor=0F1417&labelColor=0F1417" />
  <img alt="Versão" src="https://img.shields.io/badge/vers%C3%A3o-1.11.9-E53935?style=for-the-badge&labelColor=0F1417" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Compose-80D4D6?style=for-the-badge&logo=kotlin&logoColor=80D4D6&labelColor=0F1417" />
  <img alt="Testadores" src="https://img.shields.io/badge/procura--se-testadores-E53935?style=for-the-badge&logo=googleplay&logoColor=white&labelColor=0F1417" />
</p>

---

## 📵 O que é

O **Deixe-me OFF** nasceu de uma necessidade simples: parar de ser interrompido por ligações de telemarketing, golpes e números desconhecidos. Tentei usar uns 2 apps diferentes que achei na PlayStore e não sei por que cargas d'agua não funcionaram!

A ideia é direta: **quando habilitado, o app bloqueia todas as chamadas de números que não estão na sua agenda de contatos.** Quem você conhece continua chegando normalmente; o resto fica de fora, sem toque e sem interrupção.

Sem listas negras para manter, sem cadastro, sem anúncios. É só ligar a chave e pronto.

## ✨ Funcionalidades

| | |
|---|---|
| 🔴 **Um toque para ligar/desligar** | Ative ou desative o bloqueio direto na tela inicial. |
| 📇 **Agenda como lista permitida (allowlist)** | Só tocam as chamadas de quem está nos seus contatos. |
| 🔢 **Comparação inteligente de números** | Contatos salvos com ou sem DDD, `+55` ou outros prefixos são reconhecidos pelos últimos 8 dígitos. |
| 📊 **Contador de bloqueios** | Veja quantas chamadas foram barradas desde a ativação. |
| 🗂️ **Histórico de chamadas bloqueadas** | Consulte o número e o horário de cada chamada barrada. |
| 🔐 **Painel de permissões** | Confira em um só lugar se tudo o que o app precisa foi concedido. |
| 🔒 **Tudo no aparelho** | A filtragem usa a API nativa de triagem de chamadas do Android (`CallScreeningService`). Nenhum dado sai do seu celular. |

## 📱 Telas

<p align="center">
  <img src="amostras/home.png" alt="Tela inicial" width="250" />
  &nbsp;&nbsp;
  <img src="amostras/historico.png" alt="Histórico de chamadas bloqueadas" width="250" />
  &nbsp;&nbsp;
  <img src="amostras/permissoes.png" alt="Status das permissões" width="250" />
</p>
<p align="center">
  <sub><b>Início</b> · <b>Histórico de bloqueios</b> · <b>Permissões</b></sub>
</p>

## ⚙️ Como funciona

1. Instale o app e conceda as permissões de **Contatos**, **Registro de chamadas** e **Filtragem de chamadas**.
2. Na tela inicial, ative o bloqueio. O status muda para <code>HABILITADO</code>.
3. Toda chamada recebida é comparada com a sua agenda:
   - ✅ **Está nos contatos:** a chamada toca normalmente.
   - ⛔ **Não está:** a chamada é rejeitada silenciosamente e registrada no histórico.

> **Requisitos:** Android 10 (API 29) ou superior.

---

## 🙋 Usuários voluntários...

<p align="center">
  <img src="https://img.shields.io/badge/Seja%20um%20testador-Google%20Play-E53935?style=for-the-badge&logo=googleplay&logoColor=white&labelColor=0F1417" alt="Seja um testador" />
</p>

Caso se interesse em usar/testar/contribuir, precisa fornecer seu email da PlayStore pois ainda está nas fases de testes fechados:

1. Enviar email: **[[LINK DO GOOGLE GROUP]](https://forms.gle/RqiQzw85cc4sdCMKA)**
2. Aceite o convite de teste na Play Store (precisarei ter adicionado seu email antes..rsrs): **[[LINK DE OPT-IN DO TESTE FECHADO]](https://play.google.com/apps/internaltest/4700406798509207687)**
3. Instale o app e pronto!

Ficou com alguma dúvida? Abra uma [issue com o título "Quero testar"](https://github.com/douglasjfm/dmoff/issues/new?title=Quero%20testar). *(Não publique seu e-mail na issue, porque ela é pública.)*

> Qualquer coisa só falar na issue.

---

Contribuições, sugestões e relatos de bugs são bem-vindos.

---

<p align="center">
  <img src="letmeoff1.jpg" alt="" width="48" /><br/>
  <sub>Feito com ☕ e pouca paciência para telemarketing.</sub>
</p>
