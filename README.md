# COPY — Assistente pessoal para Android

O COPY está sendo construído como um assistente Android nativo, com prioridade para privacidade, funcionamento previsível e custos opcionais.

## Estado desta base

Esta branch adiciona uma primeira versão funcional de inferência local. Ela ainda não representa o assistente completo.

- Interface Android nativa em Kotlin + Jetpack Compose, com tema escuro azul.
- Modelo Qwen3-0.6B quantizado, empacotado no APK para inferência no próprio aparelho via LiteRT-LM.
- Chat local sem chave de API e sem conexão com a internet depois da instalação/preparação do modelo.
- Histórico e memórias manuais salvos localmente em SQLite.
- Provedor HTTPS compatível com `/chat/completions` como opção alternativa caso a IA local ainda não tenha sido ativada.
- Credenciais externas cifradas com AES-GCM e chave gerenciada pelo Android Keystore.
- Validação de endpoint HTTPS e testes unitários para a validação.
- Nenhuma chave de API incluída no repositório.

## O que ainda não está implementado

- Voz contínua, ativação por palavra de comando e conversa falada.
- Integração do núcleo Rust do Grok Build ao aplicativo Android.
- Ferramentas Android reais (calendário, notificações, arquivos, acessibilidade e rotinas).
- Permissões por capacidade, confirmações de ações e auditoria de execução.
- Sincronização opcional e recuperação entre dispositivos.
- Reprodução fiel do arquivo Figma Make: a conexão de design não conseguiu abrir esse arquivo nesta sessão, então a tela atual é uma fundação visual provisória, não uma cópia certificada do Figma.

Nenhuma ferramenta de sistema é executada pelo chat nesta etapa. O assistente não deve afirmar que fez uma ação no aparelho se não houver uma integração implementada e confirmação real do Android.

## Arquitetura inicial

- `ui/`: estado de apresentação.
- `data/CopyDatabase.kt`: histórico e memórias locais SQLite.
- `data/SecureSettingsStore.kt`: configuração cifrada com Android Keystore.
- `ai/ModelRouter.kt`: encaminha comandos locais, prioriza o modelo local preparado e só usa nuvem se ele não estiver preparado.
- `ai/LocalModelEngine.kt`: gerencia cópia e inferência do modelo Qwen3-0.6B no dispositivo.
- `ai/OpenAiCompatibleProvider.kt`: adaptador de rede opcional para provedores compatíveis.
- `ai/EndpointUrl.kt`: validação e normalização do endpoint HTTPS.

A implementação está separada do código-fonte do Grok Build. O repositório `joots28/grok-niel` é uma ferramenta de agente para terminal escrita principalmente em Rust; copiar toda essa aplicação para dentro do APK não a transformaria automaticamente em um assistente Android.

## Requisitos

- Android Studio compatível com Android Gradle Plugin 8.11.1.
- JDK 17.
- Android SDK Platform 36.
- Gradle 8.13 (o script `gradlew` tenta usar o Gradle instalado ou baixar a distribuição oficial).

## Compilar e testar

Na raiz do projeto:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

No Windows:

```bat
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```

O workflow do GitHub Actions executa os testes unitários e a montagem do APK em cada push desta branch e em pull requests.

## IA local incluída no APK

A tarefa Gradle `downloadBundledLocalAiModel` obtém durante a compilação o arquivo quantizado `Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm` do repositório público LiteRT Community e o inclui em `app/src/main/assets/models/`. O modelo não é versionado no Git para evitar colocar centenas de megabytes no histórico do repositório.

O APK final fica aproximadamente 330 MB maior do que um APK sem pesos de modelo. Na primeira utilização, abra **Central de controle → Ativar IA local embutida**; o aplicativo copia os pesos para o armazenamento privado e prepara o runtime. Depois disso, o chat usa inferência local sem API key e sem internet. O modelo é pequeno e mais limitado do que serviços de nuvem maiores; o desempenho real depende do aparelho.

O modelo Qwen3-0.6B publicado por LiteRT Community declara licença Apache-2.0 e oferece uma variante quantizada para dispositivos móveis:
- Modelo: https://huggingface.co/litert-community/Qwen3-0.6B
- Runtime: https://developers.google.com/edge/litert-lm/android
- Licença do modelo: https://huggingface.co/litert-community/Qwen3-0.6B/blob/main/LICENSE

## Provedor online opcional

Se preferir usar um serviço externo, abra **Configurações** no aplicativo. Informe:
1. Um endpoint HTTPS compatível com o formato de chat completions.
2. O identificador do modelo que o provedor oferece.
3. Uma chave de API somente se o provedor exigir autenticação.

O aplicativo não exige provedor pago. Com a IA local ativada, o modelo local tem prioridade e as mensagens não são enviadas a um endpoint externo. O provedor online só é usado se a IA local ainda não estiver preparada. O compartilhamento de memórias com o provedor externo está desativado por padrão.

Não coloque chaves, tokens, arquivos `.env` ou credenciais no GitHub.

## Princípios

1. Local-first para dados pessoais.
2. Nenhuma ação no sistema sem integração real, permissão Android e confirmação proporcional ao risco.
3. Provedores de IA substituíveis; serviços pagos nunca são uma dependência obrigatória do projeto.
4. Código dividido por responsabilidade e fácil de inspecionar.
5. O design final deve ser comparado com o Figma antes de declarar fidelidade visual.

## Interface COPY dark blue

A versão em `work/copy-figma-blue-ui` adiciona a interface mobile em tema dark azul, globo luminoso, navegação por Início/Conversa/Memória/Ajustes e integração com o `CopyViewModel` existente. Esta branch é de validação e não altera a `main`.
