const { app, BrowserWindow, ipcMain, session, Menu } = require('electron');
const { spawn } = require('node:child_process');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { Engine } = require('./engine.cjs');

const page = path.join(__dirname, '..', 'dist', 'index.html');
const allowed = new Set(['profiles', 'register', 'login', 'overview', 'logout', 'recovery',
  'claim', 'start', 'state', 'hide', 'reveal', 'act', 'bot', 'settle', 'next', 'topup',
  'quit', 'end']);
let engine;
let window;
let stopping = false;

function launchEngine() {
  const engineDir = app.isPackaged ? path.join(process.resourcesPath, 'engine')
    : path.join(__dirname, '..', '.engine');
  const java = path.join(engineDir, 'runtime', 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
  const args = ['-Dfile.encoding=UTF-8'];
  if (!app.isPackaged && process.env.RIVER_ROOM_DATA) {
    args.push('-Dcs380.profileDirectory=' + process.env.RIVER_ROOM_DATA);
  }
  args.push('-cp', path.join(engineDir, 'lib', '*'), 'desktop.DesktopMain');
  return new Engine(spawn(java, args, { windowsHide: true, stdio: ['pipe', 'pipe', 'ignore'] }));
}

function openRoom() {
  engine = launchEngine();
  window = new BrowserWindow({
    title: 'River Room', width: 1360, height: 900, minWidth: 900, minHeight: 650,
    backgroundColor: '#101916', show: false, autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'), sandbox: true,
      contextIsolation: true, nodeIntegration: false, devTools: !app.isPackaged
    }
  });
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', event => event.preventDefault());
  window.on('blur', () => {
    window.webContents.send('poker:conceal');
    engine.request({op:'hide'}).catch(() => {});
  });
  window.once('ready-to-show', () => window.show());
  window.loadFile(page);
}

function handleCommand(event, command) {
  if (event.sender !== window.webContents || event.senderFrame !== event.sender.mainFrame
      || event.senderFrame.url !== pathToFileURL(page).href || !allowed.has(command?.op)) {
    throw new Error('Unknown desktop request.');
  }
  if ((command.op === 'reveal' || command.op === 'act') && !window.isFocused()) {
    throw new Error('Bring the table into focus to take your turn.');
  }
  return engine.request(command);
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => { if (window && !window.isDestroyed()) window.focus(); });
  app.whenReady().then(() => {
    Menu.setApplicationMenu(Menu.buildFromTemplate([
      ...(process.platform === 'darwin' ? [{role:'appMenu'}] : []),
      {role:'editMenu'}, {role:'windowMenu'}
    ]));
    session.defaultSession.setPermissionRequestHandler((_contents, _permission, done) => done(false));
    ipcMain.handle('poker:command', async (event, command) => {
      try {
        return {ok:true, data:await handleCommand(event, command)};
      } catch (failure) {
        return {ok:false, error:failure.message};
      }
    });
    openRoom();
  });
  app.on('window-all-closed', () => app.quit());
  app.on('before-quit', event => {
    if (!engine) return;
    event.preventDefault();
    if (stopping) return;
    stopping = true;
    engine.close().finally(() => { engine = undefined; app.quit(); });
  });
}
