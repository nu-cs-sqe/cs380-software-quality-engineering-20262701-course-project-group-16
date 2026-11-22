const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('poker', {
  request: async (op, fields = {}) => {
    const reply = await ipcRenderer.invoke('poker:command', { ...fields, op });
    if (!reply.ok) throw new Error(reply.error);
    return reply.data;
  },
});
ipcRenderer.on('poker:conceal', () => window.dispatchEvent(new Event('table-conceal')));
