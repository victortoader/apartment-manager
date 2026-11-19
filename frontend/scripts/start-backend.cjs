// Boots the packaged backend jar for the e2e suite. Playwright starts this script and stops it
// when the run ends. Spawning with an argv array (instead of a shell command) keeps paths with
// spaces working on Windows, where webServer commands cannot contain quotes.
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const jar = process.env.E2E_BACKEND_JAR;
const javaHome = process.env.E2E_BACKEND_JAVA_HOME || process.env.JAVA_HOME;
const javaBin = javaHome
  ? path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
  : 'java';

if (!jar) {
  console.error('E2E_BACKEND_JAR is not set. Run the suite through Gradle (./gradlew e2e) or unset it.');
  process.exit(1);
}

if (!fs.existsSync(jar)) {
  console.error(`Backend jar not found: ${jar}`);
  process.exit(1);
}

// The backend reads ../.env and stores uploads relative to its working directory, so it has to run
// from backend/ - same working directory the Gradle bootRun task used.
const cwd = path.resolve(__dirname, '..', '..', 'backend');

const backend = spawn(javaBin, ['-jar', jar], { cwd, stdio: 'inherit' });

backend.on('error', (error) => {
  console.error(`Failed to start the backend with ${javaBin}: ${error.message}`);
  process.exit(1);
});

backend.on('exit', (code, signal) => {
  process.exit(signal ? 1 : code ?? 0);
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => backend.kill(signal));
}
