const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("copyDesktop", {
  loadSettings: () => ipcRenderer.invoke("settings:load"),
  saveSettings: (settings) => ipcRenderer.invoke("settings:save", settings),
  loadState: () => ipcRenderer.invoke("state:load"),
  saveState: (state) => ipcRenderer.invoke("state:save", state),
  sendMessage: (payload) => ipcRenderer.invoke("chat:send", payload)
});