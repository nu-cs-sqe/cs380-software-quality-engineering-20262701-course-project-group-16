import { packager } from '@electron/packager';
import { cp, mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import {execFileSync} from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const staging = await mkdtemp(path.join(os.tmpdir(),'river-room-package-'));
try {
  const manifest = JSON.parse(await readFile(path.join(root,'package.json'),'utf8'));
  await writeFile(path.join(staging,'package.json'),JSON.stringify({
    name:manifest.name, productName:manifest.productName, version:manifest.version, main:manifest.main
  }));
  for (const item of ['dist','electron','THIRD_PARTY_NOTICES']) {
    await cp(path.join(root,item),path.join(staging,item),{recursive:true});
  }
  const licenses = path.join(staging, 'licenses');
  await mkdir(licenses, {recursive:true});
  for (const dependency of ['react','react-dom','scheduler','@fontsource/dm-sans',
    '@fontsource/cormorant-garamond']) {
    const filename = dependency.replaceAll('/', '-') + '.txt';
    await cp(path.join(root,'node_modules',dependency,'LICENSE'),path.join(licenses,filename));
  }
  const engine = path.join(staging,'resources','engine');
  await mkdir(engine,{recursive:true});
  for (const item of ['lib','runtime']) {
    await cp(path.join(root,'.engine',item),path.join(engine,item),{recursive:true,dereference:true});
  }
  const output = await packager({
    dir:staging, name:manifest.productName, appBundleId:'edu.northwestern.cs380.riverroom',
    electronVersion:manifest.devDependencies.electron, out:path.join(root,'release'), overwrite:true,
    ignore: /^\/resources($|\/)/, extraResource:[engine], prune:false, asar:true,
    ...process.platform === 'darwin' ? {icon:path.join(root,'assets','room.icns'),osxSign:{identity:'-',identityValidation:false,
      optionsForFile:() => ({hardenedRuntime:false})}} : {}
  });
  if (process.platform === 'darwin') {
    for (const directory of output) {
      const bundle = path.join(directory,manifest.productName + '.app');
      execFileSync('codesign',['--verify','--deep','--strict',bundle],{stdio:'inherit'});
    }
  }
  console.log(output.join('\n'));
} finally {
  await rm(staging,{recursive:true,force:true});
}
