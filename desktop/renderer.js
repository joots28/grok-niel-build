const state = {
  conversations: [],
  currentConversationId: null,
  memories: []
};
let settings = {
  endpoint: "http://localhost:11434/v1/chat/completions",
  model: "qwen2.5:3b",
  apiKeyConfigured: false
};
let isSending = false;
let toastTimer;

const byId = (id) => document.getElementById(id);
const pageNames = {
  home: "INÍCIO",
  chat: "CONVERSA",
  memory: "MEMÓRIA",
  history: "HISTÓRICO",
  settings: "CONFIGURAÇÕES"
};

function makeId() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") return window.crypto.randomUUID();
  return `copy-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function nowIso() {
  return new Date().toISOString();
}

function formatDate(value) {
  if (!value) return "Agora";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Agora";
  return date.toLocaleString("pt-BR", { dateStyle: "medium", timeStyle: "short" });
}

function toast(message, isError = false) {
  const node = byId("toast");
  node.textContent = message;
  node.classList.toggle("error", isError);
  node.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.classList.remove("show"), 3600);
}

function getActiveConversation() {
  return state.conversations.find((item) => item.id === state.currentConversationId) || null;
}

function createConversation() {
  const item = {
    id: makeId(),
    title: "Nova conversa",
    updatedAt: nowIso(),
    messages: []
  };
  state.conversations.push(item);
  state.currentConversationId = item.id;
  return item;
}

function ensureActiveConversation() {
  return getActiveConversation() || createConversation();
}

async function persistState() {
  try {
    await window.copyDesktop.saveState({
      conversations: state.conversations,
      currentConversationId: state.currentConversationId,
      memories: state.memories
    });
  } catch (error) {
    toast(`Não foi possível salvar localmente: ${error.message || error}`, true);
  }
}

function switchPage(name) {
  const page = pageNames[name] ? name : "home";
  document.querySelectorAll(".page").forEach((node) => node.classList.toggle("active", node.id === `page-${page}`));
  document.querySelectorAll(".nav-item[data-page]").forEach((node) => node.classList.toggle("active", node.dataset.page === page));
  byId("breadcrumb-current").textContent = pageNames[page];
  if (page === "chat") {
    renderThread();
    setTimeout(() => byId("message-input").focus(), 40);
  }
  if (page === "history") renderHistory();
  if (page === "memory") renderMemories();
  if (page === "home") renderHome();
}

function updateConnectionSummary() {
  const endpoint = settings.endpoint || "";
  const isLocal = /localhost|127\.0\.0\.1|\[::1\]/i.test(endpoint);
  byId("connection-label").textContent = settings.apiKeyConfigured || isLocal ? "Conexão configurável" : "Provedor externo";
  byId("connection-detail").textContent = isLocal ? "Endpoint local configurado" : "Endpoint configurado por você";
  byId("home-provider-label").textContent = isLocal ? "IA local (endpoint)" : "Endpoint compatível";
  byId("home-provider-detail").textContent = settings.apiKeyConfigured
    ? "Chave protegida salva neste perfil."
    : isLocal ? "Ex.: Ollama; o modelo precisa estar em execução." : "Sem chave salva; adequada a provedores locais.";
  byId("chat-mode-hint").textContent = `${settings.model || "Modelo não definido"} · mensagens salvas neste PC`;
  byId("key-help").textContent = settings.apiKeyConfigured
    ? "Já existe uma chave salva. Deixe o campo vazio para mantê-la."
    : "A chave é salva usando o armazenamento seguro do sistema quando disponível. Para modelo local sem autenticação, deixe em branco.";
}

function renderHome() {
  const conversations = state.conversations.filter((item) => item.messages && item.messages.length);
  const memoryCount = state.memories.length;
  byId("home-conversation-count").textContent = `${conversations.length} ${conversations.length === 1 ? "conversa" : "conversas"}`;
  byId("chat-count").textContent = String(conversations.length);
  byId("home-memory-count").textContent = `${memoryCount} ${memoryCount === 1 ? "memória" : "memórias"}`;
  updateConnectionSummary();
}

function escapeSafeContent(node, content) {
  node.textContent = String(content ?? "");
}

function makeMessageRow(message) {
  const row = document.createElement("article");
  row.className = `message-row ${message.role === "user" ? "user" : "assistant"}`;
  const avatar = document.createElement("div");
  avatar.className = "message-avatar";
  avatar.textContent = message.role === "user" ? "VOCÊ" : "C";
  const main = document.createElement("div");
  main.className = "message-main";
  const meta = document.createElement("div");
  meta.className = "message-meta";
  const name = document.createElement("span");
  name.textContent = message.role === "user" ? "Você" : "COPY";
  const time = document.createElement("time");
  time.textContent = formatDate(message.time);
  meta.append(name, time);
  const content = document.createElement("div");
  content.className = "message-content";
  escapeSafeContent(content, message.content);
  main.append(meta, content);
  row.append(avatar, main);
  return row;
}

function renderThread(pending = false, error = "") {
  const thread = byId("chat-thread");
  const conversation = getActiveConversation();
  const messages = conversation?.messages || [];
  const empty = byId("empty-chat");
  thread.querySelectorAll(".message-row, .loading-indicator, .transient-error").forEach((node) => node.remove());
  empty.style.display = messages.length ? "none" : "flex";
  byId("chat-title").textContent = conversation?.title || "Nova conversa";
  byId("clear-chat").disabled = !messages.length || isSending;
  for (const message of messages) thread.appendChild(makeMessageRow(message));
  if (pending) {
    const indicator = document.createElement("div");
    indicator.className = "loading-indicator";
    indicator.innerHTML = "<span></span><span></span><span></span> COPY está preparando uma resposta…";
    thread.appendChild(indicator);
  }
  if (error) {
    const errorRow = document.createElement("div");
    errorRow.className = "transient-error";
    errorRow.style.cssText = "max-width:850px;margin:10px auto;padding:12px 14px;border:1px solid #65404b;border-radius:8px;background:rgba(60,24,34,.45);color:#ffbdc6;font-size:11px;line-height:1.65;white-space:pre-wrap;";
    errorRow.textContent = error;
    thread.appendChild(errorRow);
  }
  thread.scrollTop = thread.scrollHeight;
  byId("chat-count").textContent = String(state.conversations.filter((item) => item.messages?.length).length);
}

async function sendMessage(rawValue) {
  const message = String(rawValue || "").trim();
  if (!message || isSending) return;
  const conversation = ensureActiveConversation();
  const userMessage = { id: makeId(), role: "user", content: message, time: nowIso() };
  conversation.messages.push(userMessage);
  if (conversation.title === "Nova conversa") {
    conversation.title = message.replace(/\s+/g, " ").slice(0, 58) + (message.length > 58 ? "…" : "");
  }
  conversation.updatedAt = nowIso();
  byId("message-input").value = "";
  autoResizeComposer();
  updateCharCount();
  isSending = true;
  byId("send-button").disabled = true;
  byId("new-chat").disabled = true;
  renderThread(true);
  renderHome();
  await persistState();
  try {
    const response = await window.copyDesktop.sendMessage({
      message,
      messages: conversation.messages.map(({ role, content }) => ({ role, content }))
    });
    conversation.messages.push({
      id: makeId(),
      role: "assistant",
      content: response.content,
      time: nowIso()
    });
    conversation.updatedAt = nowIso();
    await persistState();
    renderThread();
  } catch (error) {
    renderThread(false, error.message || "Não foi possível obter uma resposta.");
  } finally {
    isSending = false;
    byId("send-button").disabled = false;
    byId("new-chat").disabled = false;
    byId("clear-chat").disabled = !(getActiveConversation()?.messages?.length);
    renderHome();
    if (byId("page-chat").classList.contains("active")) byId("message-input").focus();
  }
}

function renderHistory() {
  const container = byId("history-list");
  container.replaceChildren();
  const conversations = [...state.conversations].filter((item) => item.messages?.length).sort((a, b) => String(b.updatedAt).localeCompare(String(a.updatedAt)));
  if (!conversations.length) {
    const empty = document.createElement("div");
    empty.className = "empty-list";
    empty.textContent = "Seu histórico está vazio. As conversas aparecerão aqui depois que você enviar a primeira mensagem.";
    container.appendChild(empty);
    return;
  }

  for (const conversation of conversations) {
    const row = document.createElement("div");
    row.className = "history-item";
    row.tabIndex = 0;
    row.setAttribute("role", "button");
    const icon = document.createElement("div");
    icon.className = "memory-item-icon";
    icon.textContent = "◌";
    const body = document.createElement("div");
    body.className = "history-item-body";
    const time = document.createElement("time");
    time.textContent = formatDate(conversation.updatedAt);
    const title = document.createElement("h3");
    title.textContent = conversation.title;
    const preview = document.createElement("p");
    const lastUser = [...conversation.messages].reverse().find((item) => item.role === "user");
    preview.textContent = lastUser?.content || "Conversa sem mensagem do usuário.";
    const count = document.createElement("span");
    count.className = "item-time";
    count.textContent = `${conversation.messages.length} mensagens`;
    body.append(time, title, preview, count);
    const open = document.createElement("span");
    open.className = "history-open";
    open.textContent = "↗";
    row.append(icon, body, open);
    const openConversation = () => {
      state.currentConversationId = conversation.id;
      switchPage("chat");
      persistState();
    };
    row.addEventListener("click", openConversation);
    row.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        openConversation();
      }
    });
    const remove = document.createElement("button");
    remove.className = "danger-icon-button";
    remove.title = "Excluir conversa";
    remove.setAttribute("aria-label", "Excluir conversa");
    remove.textContent = "×";
    remove.addEventListener("click", async (event) => {
      event.stopPropagation();
      state.conversations = state.conversations.filter((item) => item.id !== conversation.id);
      if (state.currentConversationId === conversation.id) state.currentConversationId = state.conversations.at(-1)?.id || null;
      await persistState();
      renderHistory();
      renderHome();
      toast("Conversa removida deste computador.");
    });
    row.appendChild(remove);
    container.appendChild(row);
  }
}

function renderMemories() {
  const container = byId("memory-list");
  container.replaceChildren();
  byId("memory-total").textContent = `${state.memories.length} ${state.memories.length === 1 ? "item" : "itens"}`;
  if (!state.memories.length) {
    const empty = document.createElement("div");
    empty.className = "empty-list";
    empty.textContent = "Nenhuma memória salva. Use o formulário acima para guardar informações que escolheu manter.";
    container.appendChild(empty);
    return;
  }
  [...state.memories].reverse().forEach((memory) => {
    const row = document.createElement("article");
    row.className = "memory-item";
    const icon = document.createElement("div");
    icon.className = "memory-item-icon";
    icon.textContent = "◇";
    const body = document.createElement("div");
    body.className = "memory-item-body";
    const title = document.createElement("h3");
    title.textContent = memory.title;
    const content = document.createElement("p");
    content.textContent = memory.content;
    const time = document.createElement("time");
    time.className = "item-time";
    time.textContent = formatDate(memory.time);
    body.append(title, content, time);
    const remove = document.createElement("button");
    remove.className = "danger-icon-button";
    remove.title = "Excluir memória";
    remove.setAttribute("aria-label", "Excluir memória");
    remove.textContent = "×";
    remove.addEventListener("click", async () => {
      state.memories = state.memories.filter((item) => item.id !== memory.id);
      await persistState();
      renderMemories();
      renderHome();
      toast("Memória excluída.");
    });
    row.append(icon, body, remove);
    container.appendChild(row);
  });
}

function autoResizeComposer() {
  const input = byId("message-input");
  input.style.height = "auto";
  input.style.height = `${Math.min(input.scrollHeight, 160)}px`;
}

function updateCharCount() {
  byId("char-count").textContent = `${byId("message-input").value.length.toLocaleString("pt-BR")} / 12000`;
}

async function loadSettings() {
  try {
    settings = await window.copyDesktop.loadSettings();
    byId("endpoint").value = settings.endpoint || "";
    byId("model").value = settings.model || "";
    byId("api-key").value = "";
    byId("api-key").placeholder = settings.apiKeyConfigured ? "Chave já salva — vazio mantém a atual" : "Informe somente se o provedor exigir";
    updateConnectionSummary();
  } catch (error) {
    toast(`Não consegui carregar as configurações: ${error.message || error}`, true);
  }
}

function bindEvents() {
  document.querySelectorAll(".nav-item[data-page]").forEach((button) => button.addEventListener("click", () => switchPage(button.dataset.page)));
  document.querySelectorAll("[data-open-page]").forEach((button) => button.addEventListener("click", () => switchPage(button.dataset.openPage)));
  byId("open-settings").addEventListener("click", () => switchPage("settings"));
  byId("home-settings").addEventListener("click", () => switchPage("settings"));
  byId("home-memory").addEventListener("click", () => switchPage("memory"));
  byId("view-memory").addEventListener("click", () => switchPage("memory"));
  byId("home-conversation-count").addEventListener?.("click", () => switchPage("history"));
  byId("continue-chat").addEventListener("click", () => {
    if (!getActiveConversation()) createConversation();
    switchPage("chat");
  });
  byId("start-chat").addEventListener("click", () => {
    if (!getActiveConversation()) createConversation();
    switchPage("chat");
  });
  byId("new-chat").addEventListener("click", async () => {
    createConversation();
    await persistState();
    renderThread();
    renderHome();
    switchPage("chat");
  });
  byId("clear-chat").addEventListener("click", async () => {
    const conversation = getActiveConversation();
    if (!conversation) return;
    conversation.messages = [];
    conversation.title = "Nova conversa";
    conversation.updatedAt = nowIso();
    await persistState();
    renderThread();
    renderHome();
    toast("A conversa atual foi limpa.");
  });
  document.querySelectorAll("[data-prompt]").forEach((button) => button.addEventListener("click", () => {
    if (!getActiveConversation()) createConversation();
    switchPage("chat");
    byId("message-input").value = button.dataset.prompt || "";
    autoResizeComposer();
    updateCharCount();
    byId("message-input").focus();
  }));
  byId("configure-from-chat").addEventListener("click", () => switchPage("settings"));
  byId("chat-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await sendMessage(byId("message-input").value);
  });
  byId("message-input").addEventListener("input", () => {
    autoResizeComposer();
    updateCharCount();
  });
  byId("message-input").addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      byId("chat-form").requestSubmit();
    }
  });
  byId("settings-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const status = byId("settings-status");
    status.textContent = "Salvando…";
    try {
      const result = await window.copyDesktop.saveSettings({
        endpoint: byId("endpoint").value.trim(),
        model: byId("model").value.trim(),
        apiKey: byId("api-key").value
      });
      byId("api-key").value = "";
      await loadSettings();
      status.textContent = "Configuração salva. A conexão será testada quando você enviar uma mensagem.";
      toast(result.apiKeyConfigured ? "Configuração salva; uma chave está protegida neste perfil." : "Configuração salva.");
    } catch (error) {
      status.textContent = error.message || "Não foi possível salvar.";
      toast(status.textContent, true);
    }
  });
  byId("memory-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const title = byId("memory-title").value.trim();
    const content = byId("memory-content").value.trim();
    if (!title || !content) return;
    state.memories.push({ id: makeId(), title, content, time: nowIso() });
    byId("memory-title").value = "";
    byId("memory-content").value = "";
    await persistState();
    renderMemories();
    renderHome();
    toast("Memória salva neste computador.");
  });
  byId("voice-input").addEventListener("click", startVoiceInput);
}

function startVoiceInput() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    toast("Entrada por voz não está disponível nesta versão do ambiente. Use a digitação por enquanto.", true);
    return;
  }
  const recognition = new SpeechRecognition();
  recognition.lang = "pt-BR";
  recognition.interimResults = false;
  recognition.onresult = (event) => {
    byId("message-input").value += (byId("message-input").value ? " " : "") + event.results[0][0].transcript;
    autoResizeComposer();
    updateCharCount();
  };
  recognition.onerror = () => toast("Não foi possível capturar a voz. Verifique o microfone e as permissões.", true);
  recognition.start();
  toast("Ouvindo… fale sua mensagem.");
}

async function init() {
  bindEvents();
  try {
    const loaded = await window.copyDesktop.loadState();
    state.conversations = Array.isArray(loaded.conversations) ? loaded.conversations : [];
    state.currentConversationId = loaded.currentConversationId;
    state.memories = Array.isArray(loaded.memories) ? loaded.memories : [];
    if (!getActiveConversation() && state.conversations.length) state.currentConversationId = state.conversations[state.conversations.length - 1].id;
  } catch (error) {
    toast(`Não consegui carregar os dados locais: ${error.message || error}`, true);
  }
  await loadSettings();
  renderHome();
  renderThread();
  renderHistory();
  renderMemories();
  updateCharCount();
}

document.addEventListener("DOMContentLoaded", init);