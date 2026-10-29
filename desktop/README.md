# COPY Desktop

Versão desktop independente do COPY Android. Todo o trabalho de desktop está na pasta `desktop/` e nesta branch `copy-desktop`; os arquivos do aplicativo Android não são modificados por esta implementação.

## O que existe nesta primeira versão

- Interface desktop em tema dark azul, com esfera luminosa, navegação e layout amplo.
- Conversas e histórico guardados localmente no perfil do aplicativo.
- Memórias manuais, que só são salvas quando a pessoa as adiciona.
- Configuração opcional para endpoints compatíveis com OpenAI Chat Completions.
- Exemplo de endpoint local para Ollama.
- Chaves de API guardadas usando o armazenamento seguro do sistema operacional quando disponível.
- Versão Windows portátil gerada por GitHub Actions.

## Requisito de IA

A interface é executável sem serviço pago, mas para receber respostas reais é preciso configurar um modelo compatível em **Configurações**. O COPY Desktop não inclui pesos de modelo.

Exemplo local com Ollama:
- Endpoint: `http://localhost:11434/v1/chat/completions`
- Modelo: nome exato do modelo instalado no Ollama
- Chave de API: deixe em branco, quando o endpoint local não exigir autenticação

Para serviços externos, use HTTPS e configure somente uma chave que você confia em usar nesse computador. As mensagens enviadas ao chat são encaminhadas ao endpoint selecionado apenas quando você envia a mensagem. Verifique as políticas e custos do provedor escolhido.

## Executar em desenvolvimento

Requisitos: Node.js 22 ou compatível e npm.

No Windows PowerShell, na pasta `desktop`:

```powershell
npm install
npm start
```

## Gerar o executável Windows

```powershell
npm install
npm run package:win
```

O executável será criado em `desktop/release/`. O workflow **COPY Desktop Windows** também gera um artefato baixável no GitHub Actions quando há alterações na pasta desktop desta branch.

## Dados e limites

- Arquivos de estado e configurações são gravados na pasta de dados do usuário do Electron.
- A chave de API usa o recurso `safeStorage` do Electron quando o Windows disponibiliza a criptografia. Se esse recurso não estiver disponível, uma chave não será salva.
- Conversas e memórias locais não são criptografadas por esta versão; use uma conta Windows protegida, especialmente em computadores compartilhados.
- Esta versão não executa comandos do Windows, não acessa automaticamente documentos, calendário ou notificações e não tem integração com o modelo Qwen empacotado no APK.
- A conexão com IA ainda precisa ser validada no computador de destino.
- O executável portátil é um build inicial; não tem assinatura de código do Windows e pode exibir avisos do SmartScreen.