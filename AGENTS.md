# AGENTS.md — Round Fight (Round_Timer)

Documento de referência do código, voltado principalmente para agentes de IA que forem trabalhar neste repositório.

## Visão geral

App Android de **timer de rounds para esportes de combate** (boxe, muay thai etc.): o usuário configura
nº de rounds, duração do round, descanso e preparação; o timer roda em **foreground service** com
campainha/beeps/vibração e notificação persistente com controles (pausar/encerrar).

- **Nome do app:** Round Fight (`applicationId = "com.roundtimer.app"`)
- **Linguagem da UI:** português (strings em `res/values/strings.xml`, pt-BR; comentários do código em pt-BR)
- **Identidade visual:** interface **100% preto e branco** (ver "Regras de UI" abaixo)

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin (JVM target 17) |
| UI | Jetpack Compose + Material 3 (Compose BOM 2024.12.01) |
| Navegação | Navigation Compose 2.8.5 (rotas em `MainActivity.Routes`) |
| Persistência (presets) | Room 2.6.1 + KSP |
| Persistência (settings) | DataStore Preferences 1.1.1 |
| DI | **Sem framework** — container manual em `RoundTimerApp.AppContainer` |
| SDK | compileSdk/targetSdk 35, minSdk 26 |

## Build e testes

> ⚠️ **OBRIGATÓRIO:** o `java` padrão deste ambiente é **25-ea**, que quebra o compilador Kotlin
> (`IllegalArgumentException: 25-ea`). Exporte `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` antes
> de qualquer comando Gradle.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew assembleDebug        # compila o APK debug
./gradlew testDebugUnitTest    # roda os testes unitários (TimerEngine)
```

## Estrutura do código (`app/src/main/java/com/roundtimer/app/`)

```
├── MainActivity.kt          # NavHost (3 rotas) + intent da notificação + permissão POST_NOTIFICATIONS
├── RoundTimerApp.kt         # Application + AppContainer (database, settings, presetDao)
├── timer/                   # Núcleo do timer
│   ├── TimerModels.kt       # Phase, TimerConfig, TimerEvent, TimerSnapshot
│   ├── TimerEngine.kt       # Motor: máquina de estados pura, SEM dependências Android (testável)
│   ├── TimerService.kt      # Foreground service: tick loop, wake lock, notificação com ações
│   ├── TimerStateHolder.kt  # Singleton: ponte StateFlow<TimerSnapshot> entre Service (escreve) e UI (lê)
│   ├── TimerCommands.kt     # Atalhos da UI → comandos ao Service (start/pause/resume/skip/stop)
│   └── TimerSoundPlayer.kt  # SoundPool (bell/beep) + vibração (respeita toggles de som/vibração)
├── data/
│   ├── db/                  # Room: AppDatabase(v1), Preset (entity), PresetDao (observeAll/insert/delete)
│   └── settings/
│       └── SettingsRepository.kt  # DataStore: última config + toggles som/vibração
└── ui/
    ├── theme/Theme.kt       # Paleta P&B + HawksFont
    ├── setup/               # SetupScreen + SetupViewModel (configurações do treino)
    ├── timer/               # ActiveTimerScreen (tela cheia do contador)
    └── presets/             # PresetsScreen + PresetsViewModel (lista/seleciona/exclui presets)
```

Testes: `app/src/test/java/com/roundtimer/app/timer/TimerEngineTest.kt` — 12 testes cobrindo sequência
de fases, countdown, pause/resume, skip, eventos de aviso, campainha, stop e validação de config.

## Arquitetura do timer (fluxo de dados)

```
SetupScreen → TimerCommands.start(config) → TimerService.ACTION_START
                                             └─ cria TimerEngine + tick loop (100ms)
TimerService ──(publishStateIfChanged)──> TimerStateHolder.state (StateFlow)
                                             └─ UI (ActiveTimerScreen / banner do SetupScreen) coleta
TimerEngine ──(TimerEvent.Bell/Warning)──> TimerService → TimerSoundPlayer (som + vibração)
```

- **Sequência de fases:** `PREPARE` (opcional) → `ROUND 1` → `REST` → … → `ROUND N` → `FINISHED`.
  O descanso só existe **entre** rounds, nunca após o último.
- O `TimerEngine` recebe o relógio monotônico (`SystemClock.elapsedRealtime()`) por parâmetro —
  nunca leia relógio dentro do engine (mantém testabilidade).
- Avisos sonoros (beep) acontecem em **10s** e **3-2-1** restantes de cada fase; campainha (bell) em
  toda transição de fase.
- O Service usa wake lock parcial (teto de 12h) e notificação `IMPORTANCE_LOW` com ações de
  **pausar/retomar/encerrar** — essas ações são os **únicos controles** durante o treino (a tela do
  timer não tem botões, por decisão de design).
- Ao finalizar (`FINISHED`), o Service mantém a notificação ~4s e encerra sozinho.
- A notificação abre a tela do timer via `MainActivity` com `ACTION_OPEN_TIMER`
  (`launchMode="singleTask"`, `onNewIntent` → navega para `Routes.TIMER`).

## Regras de UI (decisões de design — NÃO reverter sem pedido explícito)

1. **Somente preto e branco.** A paleta vive em `ui/theme/Theme.kt`:
   `background`/fundo = preto puro (`#000000`), conteúdo = branco puro, cinzas só em texto secundário
   (`GrayText`) e superfícies (`DarkSurface #161616`, `SurfaceVariant #242424`). Não reintroduzir cores.
2. **Tela do timer = tela cheia minimalista** (`ui/timer/ActiveTimerScreen.kt`):
   - Contador gigante no centro (formato `m:ss`, ex.: `3:02` — minutos **sem** zero à esquerda),
     tamanho calculado em tempo de composição via `TextMeasurer`: mede a referência `"00:00"` a 100sp
     e escala para até ~98% da largura e ~85% da altura disponíveis.
   - **Fonte dos números: `HawksFont`** (`res/font/hawks.ttf`, fonte "NBA Hawks", estilo jersey).
     ⚠️ A fonte **não possui o glifo `":"`** (nem `/`, `-` etc.) — os dois pontos são **desenhados
     manualmente** como dois círculos (`ColonDots`). Por isso, **nunca** renderize `:` com a HawksFont;
     use-a apenas para dígitos/letras A-Z/a-z.
   - **Inversão no descanso:** fases `PREPARE`/`ROUND`/`FINISHED` = fundo preto + números brancos;
     `REST` = fundo branco + números pretos.
   - Barras do sistema ocultas (modo imersivo) enquanto a tela está aberta; `FLAG_KEEP_SCREEN_ON`
     enquanto `state.running`.
   - Rótulo pequeno no topo (fase / `ROUND n/N`), indicação `PAUSADO` quando aplicável.
   - **Toque em qualquer ponto da tela volta às configurações SEM parar o timer** (ele continua em
     2º plano; controles ficam na notificação). Não há botões na tela.
3. **Retorno ao timer em andamento:** a `SetupScreen` mostra o botão `timer_running_return`
   ("TIMER EM ANDAMENTO — TOQUE PARA VOLTAR") sempre que `TimerStateHolder.state.running == true`.
4. Ícone do launcher: fundo preto (`colors.xml`), foreground branco (`drawable/ic_launcher_foreground.xml`).

## Modelo de dados

- **`TimerConfig`** (validação no `init`): `rounds` 1–99, `roundDurationSec` 5–3600,
  `restDurationSec` 0–1800, `prepareDurationSec` 0–300.
- **`TimerSnapshot`** (imutável, exposto à UI): `phase`, `roundNumber`, `totalRounds`, `remainingMs`,
  `phaseDurationMs`, `paused`, `running`; derivado: `progress` (0→1 na fase).
- **`Preset`** (Room, tabela `presets`): id auto, `name`, `rounds`, `roundSec`, `restSec`,
  `prepareSec`; conversores `toConfig()` / `Preset.from(name, config)`. DB version 1 (`exportSchema = false`).
- **`UserSettings`** (DataStore "settings"): `lastConfig` + `soundEnabled` + `vibrationEnabled`;
  `SetupViewModel.reload()` recarrega em `ON_RESUME`.

## Convenções

- Comentários e strings em **pt-BR**.
- Composables de UI recebem callbacks (`onFinished`, `onStartTimer`...) — a navegação fica no `NavHost`.
- ViewModels criados via `Factory` manual (sem Hilt/Dagger).
- Mudanças de fase publicadas no `TimerStateHolder` só quando segundo/fase/round/pausa mudam
  (`publishStateIfChanged`) — a notificação também é atualizada com throttle de 1s.
- Rota de presets: selecionar um preset grava em `lastConfig` (o setup recarrega em `ON_RESUME`).

## Recursos (`app/src/main/res/`)

| Recurso | Uso |
|---|---|
| `font/hawks.ttf` | NBA Hawks — dígitos do contador (sem glifo `":"`) |
| `raw/bell.wav` | Campainha nas transições de fase |
| `raw/beep.wav` | Avisos de 10s e 3-2-1 |
| `drawable/ic_notification.xml` | Ícone da notificação do timer |
| `values/strings.xml` | Todas as strings (pt-BR) |

## Permissões (AndroidManifest)

`POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_SPECIAL_USE` (service declarado como `foregroundServiceType="specialUse"`).
