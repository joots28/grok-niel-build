# COPY — Assistente pessoal para Android

O COPY está sendo construído como um assistente Android nativo, com prioridade para privacidade, funcionamento previsível e custos opcionais.

## Estado desta base

Esta branch cria a fundação executável do aplicativo. Ela não declara o projeto finalizado.

- Interface Android nativa em Kotlin + Jetpack Compose, com tema escuro azul.
- Histórico e memórias manuais salvos localmente em SQLite.
- Configuração opcional de endpoint compatível com a API `/chat/completions`.
- Credenciais cifradas com AES-GCM e chave gerenciada pelo Android Keystore.
- Validação de endpoint HTTPS e testes unitários para a validação.
- Nenhuma chave de API incluída no repositório.

## O que ainda não está implementado

- Inferência de modelo local diretamente no aparelho.
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
- `ai/ModelRouter.kt`: ponto de entrada para seleção do mecanismo de resposta.
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

## Configurar um provedor de IA

Abra **Configurações** no aplicativo. Informe:
1. Um endpoint HTTPS compatível com o formato de chat completions.
2. O identificador do modelo que o provedor oferece.
3. Uma chave de API somente se o provedor exigir autenticação.

O aplicativo não exige um provedor pago específico. O custo e os limites são definidos pelo serviço que você escolher. As mensagens só são enviadas ao endpoint depois da configuração e do envio explícito de uma mensagem. Para apagar a configuração, deixe o endpoint vazio e salve.

Não coloque chaves, tokens, arquivos `.env` ou credenciais no GitHub.

## Princípios

1. Local-first para dados pessoais.
2. Nenhuma ação no sistema sem integração real, permissão Android e confirmação proporcional ao risco.
3. Provedores de IA substituíveis; serviços pagos nunca são uma dependência obrigatória do projeto.
4. Código dividido por responsabilidade e fácil de inspecionar.
5. O design final deve ser comparado com o Figma antes de declarar fidelidade visual.

## Interface COPY dark blue

A versão em `work/copy-figma-blue-ui` adiciona a interface mobile em tema dark azul, globo luminoso, navegação por Início/Conversa/Memória/Ajustes e integração com o `CopyViewModel` existente. Esta branch é de validação e não altera a `main`.
