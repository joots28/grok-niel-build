const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("copyDesktop", {
  loadSettings: () => ipcRenderer.invoke("settings:load"),
  saveSettings: (settings) => ipcRenderer.invoke("settings:save", settings),
  loadState: () => ipcRenderer.invoke("state:load"),
  saveState: (state) => ipcRenderer.invoke("state:save", state),
  sendMessage: (payload) => ipcRenderer.invoke("chat:send", payload),
  getOllamaStatus: () => ipcRenderer.invoke("ollama:status"),
  openOllamaDownload: () => ipcRenderer.invoke("ollama:open-download"),
  pullOllamaModel: (model) => ipcRenderer.invoke("ollama:pull-model", model),
  onOllamaProgress: (callback) => {
    if (typeof callback !== "function") return () => {};
    const listener = (_event, payload) => callback(payload);
    ipcRenderer.on("ollama:progress", listener);
    return () => ipcRenderer.removeListener("ollama:progress", listener);
  }
});