const { app, BrowserWindow, ipcMain, safeStorage, shell } = require("electron");
const path = require("node:path");
const fs = require("node:fs/promises");

const APP_DIR = app.getPath("userData");
const SETTINGS_FILE = path.join(APP_DIR, "settings.json");
const STATE_FILE = path.join(APP_DIR, "state.json");

function createWindow() {
  const window = new BrowserWindow({
    width: 1380,
    height: 900,
    minWidth: 980,
    minHeight: 680,
    backgroundColor: "#070b14",
    title: "COPY Desktop",
    show: false,
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  window.once("ready-to-show", () => window.show());
  window.loadFile(path.join(__dirname, "index.html"));
  window.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith("https://")) shell.openExternal(url);
    return { action: "deny" };
  });
}

async function readJson(filePath, fallback) {
  try {
    return JSON.parse(await fs.readFile(filePath, "utf8"));
  } catch {
    return fallback;
  }
}

async function writeJson(filePath, data) {
  await fs.mkdir(APP_DIR, { recursive: true });
  await fs.writeFile(filePath, JSON.stringify(data, null, 2), "utf8");
}

async function readSettings() {
  const saved = await readJson(SETTINGS_FILE, {});
  let apiKey = "";
  if (saved.encryptedApiKey && safeStorage.isEncryptionAvailable()) {
    try {
      apiKey = safeStorage.decryptString(Buffer.from(saved.encryptedApiKey, "base64"));
    } catch {
      apiKey = "";
    }
  }
  return {
    endpoint: typeof saved.endpoint === "string" ? saved.endpoint : "http://localhost:11434/v1/chat/completions",
    model: typeof saved.model === "string" ? saved.model : "qwen2.5:3b",
    apiKey
  };
}

function validateEndpoint(value) {
  let parsed;
  try {
    parsed = new URL(value);
  } catch {
    throw new Error("Informe um endereço de API válido.");
  }
  const localHosts = ["localhost", "127.0.0.1", "::1"];
  if (parsed.protocol !== "https:" && !(parsed.protocol === "http:" && localHosts.includes(parsed.hostname))) {
    throw new Error("Por segurança, use HTTPS para serviços externos. HTTP é permitido somente para localhost.");
  }
  if (parsed.username || parsed.password) {
    throw new Error("Não coloque credenciais diretamente na URL.");
  }
  return parsed.toString();
}

ipcMain.handle("settings:load", async () => {
  const settings = await readSettings();
  return {
    endpoint: settings.endpoint,
    model: settings.model,
    apiKeyConfigured: Boolean(settings.apiKey)
  };
});

ipcMain.handle("settings:save", async (_event, input) => {
  const previous = await readSettings();
  const endpoint = validateEndpoint(String(input?.endpoint || "").trim());
  const model = String(input?.model || "").trim().slice(0, 120);
  const incomingKey = String(input?.apiKey || "").trim();
  if (!model) throw new Error("Informe o identificador do modelo.");
  let apiKey = input?.clearApiKey ? "" : (incomingKey || previous.apiKey);
  const saved = { endpoint, model, encryptedApiKey: "" };
  if (apiKey) {
    if (!safeStorage.isEncryptionAvailable()) {
      throw new Error("O armazenamento seguro do Windows não está disponível. A chave não foi salva.");
    }
    saved.encryptedApiKey = safeStorage.encryptString(apiKey).toString("base64");
  }
  await writeJson(SETTINGS_FILE, saved);
  apiKey = "";
  return { success: true, apiKeyConfigured: Boolean(saved.encryptedApiKey) };
});

ipcMain.handle("state:load", async () => {
  const state = await readJson(STATE_FILE, { conversations: [], currentConversationId: null, memories: [] });
  const conversations = Array.isArray(state.conversations) ? state.conversations.slice(-200).map((conversation) => ({
    id: String(conversation.id || "").slice(0, 80),
    title: String(conversation.title || "Nova conversa").slice(0, 160),
    updatedAt: String(conversation.updatedAt || "").slice(0, 40),
    messages: (Array.isArray(conversation.messages) ? conversation.messages.slice(-300) : []).map((item) => ({
      id: String(item.id || "").slice(0, 80),
      role: item.role === "assistant" ? "assistant" : "user",
      content: String(item.content || "").slice(0, 20000),
      time: String(item.time || "").slice(0, 40)
    }))
  })) : [];
  return {
    conversations,
    currentConversationId: typeof state.currentConversationId === "string" ? state.currentConversationId : null,
    memories: Array.isArray(state.memories) ? state.memories.slice(-200) : []
  };
});

ipcMain.handle("state:save", async (_event, input) => {
  const conversations = Array.isArray(input?.conversations) ? input.conversations.slice(-200) : [];
  const memories = Array.isArray(input?.memories) ? input.memories.slice(-200) : [];
  const safeConversations = conversations.map((conversation) => ({
    id: String(conversation.id || "").slice(0, 80),
    title: String(conversation.title || "Nova conversa").slice(0, 160),
    updatedAt: String(conversation.updatedAt || "").slice(0, 40),
    messages: (Array.isArray(conversation.messages) ? conversation.messages.slice(-300) : []).map((item) => ({
      id: String(item.id || "").slice(0, 80),
      role: item.role === "assistant" ? "assistant" : "user",
      content: String(item.content || "").slice(0, 20000),
      time: String(item.time || "").slice(0, 40)
    }))
  }));
  const safeMemories = memories.map((item) => ({
    id: String(item.id || "").slice(0, 80),
    title: String(item.title || "Memória").slice(0, 160),
    content: String(item.content || "").slice(0, 4000),
    time: String(item.time || "").slice(0, 40)
  }));
  const currentConversationId = safeConversations.some((item) => item.id === input?.currentConversationId)
    ? input.currentConversationId
    : (safeConversations[safeConversations.length - 1]?.id || null);
  await writeJson(STATE_FILE, { conversations: safeConversations, currentConversationId, memories: safeMemories });
  return { success: true };
});

async function getOllamaStatus() {
  try {
    const response = await fetch("http://127.0.0.1:11434/api/tags", {
      signal: AbortSignal.timeout(3500)
    });
    if (!response.ok) {
      return { installedOrRunning: true, reachable: false, modelInstalled: false, models: [], error: `Ollama respondeu com HTTP ${response.status}.` };
    }
    const body = await response.json();
    const models = Array.isArray(body.models) ? body.models.map((item) => String(item.name || "")) : [];
    const modelInstalled = models.some((name) => name === "qwen2.5:3b" || name.startsWith("qwen2.5:3b-"));
    return { installedOrRunning: true, reachable: true, modelInstalled, models };
  } catch {
    return { installedOrRunning: false, reachable: false, modelInstalled: false, models: [], error: "Ollama não respondeu em http://127.0.0.1:11434." };
  }
}

function sendOllamaProgress(progress) {
  for (const window of BrowserWindow.getAllWindows()) {
    if (!window.isDestroyed()) window.webContents.send("ollama:progress", progress);
  }
}

ipcMain.handle("ollama:status", async () => getOllamaStatus());

ipcMain.handle("ollama:open-download", async () => {
  await shell.openExternal("https://ollama.com/download/windows");
  return { opened: true };
});

ipcMain.handle("ollama:pull-model", async (_event, modelInput) => {
  const status = await getOllamaStatus();
  if (!status.reachable) {
    throw new Error("Ollama não está aberto. Instale pelo botão oficial e abra o aplicativo Ollama; depois tente novamente.");
  }
  const model = String(modelInput || "qwen2.5:3b").trim();
  if (!/^[a-zA-Z0-9._:/-]{1,120}$/.test(model)) {
    throw new Error("Identificador do modelo inválido.");
  }
  sendOllamaProgress({ phase: "starting", model, status: "Preparando download do modelo…", percent: 0 });
  const response = await fetch("http://127.0.0.1:11434/api/pull", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ model, stream: true }),
    signal: AbortSignal.timeout(1000 * 60 * 60)
  });
  if (!response.ok || !response.body) {
    const detail = await response.text().catch(() => "");
    throw new Error(`Ollama não conseguiu iniciar o download (${response.status}). ${detail.slice(0, 300)}`);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let latestStatus = "Download iniciado…";
  let percent = 0;
  let sawSuccess = false;
  const processLine = (line) => {
    if (!line.trim()) return;
    let item;
    try { item = JSON.parse(line); } catch { return; }
    if (item.error) throw new Error(String(item.error));
    latestStatus = String(item.status || latestStatus);
    if (Number(item.total) > 0 && Number(item.completed) >= 0) {
      percent = Math.max(0, Math.min(100, Math.round(Number(item.completed) / Number(item.total) * 100)));
    }
    if (latestStatus.toLowerCase() === "success") {
      sawSuccess = true;
      percent = 100;
    }
    sendOllamaProgress({
      phase: sawSuccess ? "completed" : "downloading",
      model,
      status: latestStatus,
      percent
    });
  };
  try {
    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
      const lines = buffer.split("\n");
      buffer = lines.pop() || "";
      for (const line of lines) processLine(line);
      if (done) break;
    }
    if (buffer.trim()) processLine(buffer);
  } catch (error) {
    sendOllamaProgress({ phase: "error", model, status: error.message || "O download falhou.", percent });
    throw error;
  }
  if (!sawSuccess) {
    const refreshed = await getOllamaStatus();
    if (!refreshed.models.some((name) => name === model || name.startsWith(model + "-"))) {
      throw new Error("O Ollama encerrou o download sem confirmar o modelo. Confira a conexão e tente novamente.");
    }
  }
  sendOllamaProgress({ phase: "completed", model, status: "Modelo instalado e pronto para uso.", percent: 100 });
  const refreshed = await getOllamaStatus();
  return { success: true, model, status: refreshed };
});

ipcMain.handle("chat:send", async (_event, input) => {
  const settings = await readSettings();
  const endpoint = validateEndpoint(settings.endpoint);
  const model = settings.model || "qwen2.5:3b";
  const messages = Array.isArray(input?.messages) ? input.messages.slice(-40) : [];
  const userMessage = String(input?.message || "").trim().slice(0, 12000);
  if (!userMessage) throw new Error("Digite uma mensagem antes de enviar.");
  const payload = messages.map((item) => ({
    role: item.role === "assistant" ? "assistant" : "user",
    content: String(item.content || "").slice(0, 12000)
  }));
  if (payload.length === 0 || payload[payload.length - 1].content !== userMessage) {
    payload.push({ role: "user", content: userMessage });
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 90000);
  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(settings.apiKey ? { Authorization: `Bearer ${settings.apiKey}` } : {})
      },
      body: JSON.stringify({
        model,
        messages: [
          {
            role: "system",
            content: "Você é COPY, um assistente pessoal direto, preciso e útil. Responda em português brasileiro quando o usuário escrever em português. Não afirme ter executado ações no computador sem uma integração real. Seja transparente sobre limitações."
          },
          ...payload
        ],
        stream: false
      }),
      signal: controller.signal
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      const detail = body?.error?.message || body?.message || `HTTP ${response.status}`;
      throw new Error(`A API respondeu com erro: ${detail}`);
    }
    const content = body?.choices?.[0]?.message?.content;
    if (typeof content !== "string" || !content.trim()) {
      throw new Error("A API não retornou uma resposta de texto no formato chat/completions.");
    }
    return { content: content.trim(), model };
  } catch (error) {
    if (error.name === "AbortError") throw new Error("A resposta demorou mais de 90 segundos. Verifique o modelo e tente novamente.");
    if (error instanceof TypeError) {
      throw new Error("Não foi possível conectar à IA local. Abra Configurações → IA local, confira se o Ollama está instalado e em execução, e prepare o modelo gratuito qwen2.5:3b.");
    }
    throw error;
  } finally {
    clearTimeout(timeout);
  }
});

app.whenReady().then(() => {
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});