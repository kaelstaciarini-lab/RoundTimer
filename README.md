# 🥊 Round Fight

App Android para timer de rounds de esportes de combate, com foco em treino de boxe, muay thai e modalidades similares. O app permite configurar rounds, duração do round, descanso e preparação, acompanhar a contagem em tela cheia e controlar o treino em segundo plano por meio de notificações.

---

## 📌 Problema real

Em treinos de combate, o atleta precisa acompanhar o tempo de forma rápida e sem distrações. Um timer comum costuma exigir visualização em tela pequena, sem foco e sem controle ao longo do treino. Este projeto resolve isso com uma interface minimalista em preto e branco, timer em tela cheia e notificação persistente para controle do treino mesmo com a tela bloqueada.

---

## ✨ Funcionalidades

- Configuração de número de rounds
- Tempo do round, descanso e preparação
- Timer em foreground service
- Campainha e beep nas transições e alertas de tempo
- Vibração configurável
- Tela full-screen de cronômetro em modo paisagem
- Notificação persistente com ações de pausar, retomar e encerrar
- Persistência das configurações e presets de treino
- Design monocromático para foco visual

---

## 🧠 Arquitetura

O projeto foi estruturado em camadas bem separadas:

- UI: Jetpack Compose + Material 3
- Navegação: Navigation Compose
- Timer: engine puro em Kotlin, sem dependência direta do Android
- Serviço em background: controle do timer em foreground
- Persistência: Room para presets e DataStore para configurações
- DI: container manual simples, sem Hilt/Dagger

### Fluxo principal

```text
SetupScreen
  → TimerCommands.start(config)
  → TimerService.ACTION_START
  → TimerEngine
  → TimerStateHolder
  → ActiveTimerScreen
```

---

## 📁 Estrutura do projeto

```text
Round_Timer/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/roundtimer/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── RoundTimerApp.kt
│       │   │   ├── data/
│       │   │   ├── timer/
│       │   │   └── ui/
│       │   └── res/
│       │       ├── font/
│       │       ├── raw/
│       │       ├── drawable/
│       │       └── values/
│       └── test/
│           └── java/com/roundtimer/app/timer/
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── settings.gradle.kts
├── AGENTS.md
├── README.md
└── local.properties
```

---

## 🧪 Build e testes

> Importante: este ambiente exige Java 21 para compilação correta.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

---

## ▶️ Como executar

1. Abra o projeto no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Configure um emulador Android ou conecte um dispositivo físico.
4. Clique em Run.

Ou, via terminal:

```bash
cd /home/kael_staciarini/Documentos/softwares/Round_Timer
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew installDebug
```

---

## 🎨 Design visual

O app segue uma identidade visual em preto e branco puro, com foco em legibilidade e menos distrações durante o treino.

- fundo principal: preto
- texto principal: branco
- descanso: fundo branco com texto preto
- fonte do cronômetro: Black Ops One
- tela do timer em modo paisagem para melhor aproveitamento da tela

---

## 📦 Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose |
| Banco de presets | Room |
| Configurações | DataStore |
| SDK | Android 35 |
| Mínimo suportado | Android 8.0 (API 26) |

---

## 🚀 Possíveis evoluções

- suporte a vários tipos de treino personalizados
- histórico de sessões concluídas
- exportação de estatísticas em CSV
- seleção de sons e estilos de vibração
- widget de relógio no launcher
- sincronização com smartwatch ou app companion

---

## 📎 Observações importantes

- O timer roda em foreground service para continuar funcionando mesmo com a tela desligada.
- A sequência de fases segue o padrão: preparação → round → descanso → round → ... → finalizado.
- A tela de timer foi pensada para foco total, sem botões visíveis durante a execução.
