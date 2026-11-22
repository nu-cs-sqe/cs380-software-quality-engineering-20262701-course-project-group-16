import { cpSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const stage = path.join(root, 'desktop', '.engine');
const windows = process.platform === 'win32';

function installedJava(version, variable) {
  if (process.env[variable]) return process.env[variable];
  if (process.platform === 'darwin') {
    return execFileSync('/usr/libexec/java_home', ['-v', version], { encoding: 'utf8' }).trim();
  }
  throw new Error('Set ' + variable + ' to your JDK ' + version + ' directory.');
}

const runtimeJdk = installedJava('11', 'JAVA_HOME_11');
const gradleJdk = process.env.GRADLE_JAVA_HOME || (process.platform === 'darwin'
  ? execFileSync('/usr/libexec/java_home', ['-v', '17'], { encoding:'utf8' }).trim()
  : installedJava('17', 'JAVA_HOME'));
execFileSync(windows ? 'gradlew.bat' : 'bash', [
  ...windows ? [] : ['./gradlew'], '--console=plain', '-Porg.gradle.java.installations.fromEnv=JAVA_HOME_11',
  'installDist'
], { cwd:root, stdio:'inherit', shell:windows, env:{...process.env, JAVA_HOME:gradleJdk, JAVA_HOME_11:runtimeJdk} });

mkdirSync(stage, {recursive:true});
const lib = path.join(stage, 'lib');
const distribution = path.join(root, 'build/install/sqe-course-project/lib');
const jars = readdirSync(distribution).filter(file => file.endsWith('.jar'));
mkdirSync(lib, {recursive:true});
for (const file of readdirSync(lib)) {
  if (!jars.includes(file)) rmSync(path.join(lib, file));
}
for (const file of jars) cpSync(path.join(distribution, file), path.join(lib,file));

const modules = 'java.base,java.sql,java.desktop,java.naming,java.management,jdk.crypto.ec,jdk.unsupported';
const runtime = path.join(stage, 'runtime');
const stamp = path.join(stage, 'runtime-version');
const version = readFileSync(path.join(runtimeJdk, 'release'), 'utf8') + modules;
if (!existsSync(stamp) || readFileSync(stamp, 'utf8') !== version
    || !existsSync(path.join(runtime, 'bin', windows ? 'java.exe' : 'java'))) {
  rmSync(runtime, {recursive:true, force:true});
  execFileSync(path.join(runtimeJdk,'bin',windows ? 'jlink.exe' : 'jlink'), [
    '--add-modules',modules,'--strip-debug','--no-header-files','--no-man-pages',
    '--compress=2','--output',runtime
  ], {stdio:'inherit'});
  writeFileSync(stamp,version);
}
