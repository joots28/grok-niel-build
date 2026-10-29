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
  let apiKey = incomingKey || previous.apiKey;
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
  const state = await readJson(STATE_FILE, { messages: [], memories: [] });
  return {
    messages: Array.isArray(state.messages) ? state.messages.slice(-300) : [],
    memories: Array.isArray(state.memories) ? state.memories.slice(-200) : []
  };
});

ipcMain.handle("state:save", async (_event, input) => {
  const messages = Array.isArray(input?.messages) ? input.messages.slice(-300) : [];
  const memories = Array.isArray(input?.memories) ? input.memories.slice(-200) : [];
  const safeMessages = messages.map((item) => ({
    id: String(item.id || "").slice(0, 80),
    role: item.role === "assistant" ? "assistant" : "user",
    content: String(item.content || "").slice(0, 20000),
    time: String(item.time || "").slice(0, 40)
  }));
  const safeMemories = memories.map((item) => ({
    id: String(item.id || "").slice(0, 80),
    title: String(item.title || "Memória").slice(0, 160),
    content: String(item.content || "").slice(0, 4000),
    time: String(item.time || "").slice(0, 40)
  }));
  await writeJson(STATE_FILE, { messages: safeMessages, memories: safeMemories });
  return { success: true };
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
      throw new Error("Não foi possível conectar à API. Verifique se o modelo local está ligado ou se o endpoint está correto.");
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