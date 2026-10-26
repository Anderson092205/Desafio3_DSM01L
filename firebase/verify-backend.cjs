// Prueba contra el proyecto dedicado. Crea y elimina sus propias cuentas y datos.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const config = JSON.parse(fs.readFileSync(path.join(__dirname, '../app/google-services.json'), 'utf8'));
const apiKey = config.client.find(client => client.client_info.android_client_info.package_name === 'com.example.desafio3').api_key[0].current_key;
const databaseUrl = config.project_info.firebase_url;
const checks = [];
const users = [];
const password = 'Qa!' + crypto.randomBytes(18).toString('hex');
async function call(url, method = 'GET', body) {
  const response = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) });
  return { status: response.status, data: await response.json() };
}
function check(name, expected, observed, success) {
  checks.push({ name, expected, observed, passed: success });
  console.log((success ? 'PASS ' : 'FAIL ') + name + ' (' + observed + ')');
}
function auth(operation, body) { return call('https://identitytoolkit.googleapis.com/v1/accounts:' + operation + '?key=' + apiKey, 'POST', body); }
function data(resource, token, method = 'GET', body) { return call(databaseUrl + '/' + resource + '.json' + (token ? '?auth=' + encodeURIComponent(token) : ''), method, body); }
async function main() {
  const stamp = Date.now();
  for (let index = 0; index < 2; index++) {
    const email = 'desafio3.qa.' + stamp + '.' + index + '@example.com';
    const result = await auth('signUp', { email, password, returnSecureToken: true });
    check('Registro Firebase cuenta ' + (index + 1), 'HTTP 200', 'HTTP ' + result.status, result.status === 200);
    if (result.status !== 200) throw new Error(result.data.error?.message || 'No se pudo crear usuario de prueba.');
    users.push({ email, uid: result.data.localId, token: result.data.idToken, refreshToken: result.data.refreshToken });
  }
  const owner = users[0], other = users[1];
  const signIn = await auth('signInWithPassword', { email: owner.email, password, returnSecureToken: true });
  check('Inicio de sesión con correo y contraseña', 'HTTP 200', 'HTTP ' + signIn.status, signIn.status === 200);
  const wrongPassword = await auth('signInWithPassword', { email: owner.email, password: password + 'incorrecta', returnSecureToken: true });
  check('Contraseña incorrecta rechazada', 'HTTP 400', 'HTTP ' + wrongPassword.status, wrongPassword.status === 400);
  const base = 'users/' + owner.uid;
  const book = { id: 'prueba-libro', title: 'El principito', author: 'Antoine de Saint-Exupéry', genre: 'Novela', totalPages: 120, pagesRead: 30, status: 'Leyendo', rating: 0, finishedAt: 0, createdAt: stamp, updatedAt: stamp };
  const save = await data(base + '/books/prueba-libro', owner.token, 'PUT', book);
  check('Guardar libro en Realtime Database', 'HTTP 200', 'HTTP ' + save.status, save.status === 200);
  const read = await data(base + '/books/prueba-libro', owner.token);
  check('Recuperar libro y progreso persistidos', '30 páginas, 25%', String(read.data?.pagesRead) + ' páginas', read.status === 200 && read.data?.pagesRead === 30 && read.data?.title === book.title);
  const goal = await data(base + '/goals/2026', owner.token, 'PUT', 12);
  check('Guardar meta anual positiva', 'HTTP 200', 'HTTP ' + goal.status, goal.status === 200);
  const invalidGoal = await data(base + '/goals/2026', owner.token, 'PUT', 0);
  check('Rechazar meta anual cero', 'HTTP 401', 'HTTP ' + invalidGoal.status, invalidGoal.status === 401);
  const invalidProgress = await data(base + '/books/prueba-libro', owner.token, 'PUT', { ...book, pagesRead: 121 });
  check('Rechazar progreso mayor al total', 'HTTP 401', 'HTTP ' + invalidProgress.status, invalidProgress.status === 401);
  const invalidRating = await data(base + '/books/prueba-libro', owner.token, 'PUT', { ...book, status: 'Terminado', finishedAt: stamp });
  check('Rechazar terminado sin calificación', 'HTTP 401', 'HTTP ' + invalidRating.status, invalidRating.status === 401);
  const completed = await data(base + '/books/prueba-libro', owner.token, 'PUT', { ...book, pagesRead: 120, status: 'Terminado', rating: 5, finishedAt: stamp });
  check('Actualizar libro terminado con fecha y estrellas', 'HTTP 200', 'HTTP ' + completed.status, completed.status === 200);
  const crossRead = await data(base, other.token);
  check('Aislamiento: otro usuario no puede leer datos', 'HTTP 401', 'HTTP ' + crossRead.status, crossRead.status === 401);
  const crossWrite = await data(base + '/goals/2026', other.token, 'PUT', 99);
  check('Aislamiento: otro usuario no puede modificar meta', 'HTTP 401', 'HTTP ' + crossWrite.status, crossWrite.status === 401);
  const anonymous = await data(base);
  check('Datos inaccesibles sin autenticación', 'HTTP 401', 'HTTP ' + anonymous.status, anonymous.status === 401);
  const refreshResponse = await fetch('https://securetoken.googleapis.com/v1/token?key=' + apiKey, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ grant_type: 'refresh_token', refresh_token: owner.refreshToken }) });
  const refreshed = await refreshResponse.json();
  const persisted = refreshed.id_token ? await data(base, refreshed.id_token) : { status: 0 };
  check('Nueva sesión recupera biblioteca y meta guardadas', 'Meta 12 y libro calificado 5', 'HTTP ' + persisted.status, persisted.status === 200 && persisted.data?.goals?.['2026'] === 12 && persisted.data?.books?.['prueba-libro']?.rating === 5);
}
main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(async () => {
  const cleanup = [];
  for (const user of users) {
    const deletion = await data('users/' + user.uid, user.token, 'DELETE');
    const account = await auth('delete', { idToken: user.token });
    cleanup.push({ dataDeleted: deletion.status === 200, accountDeleted: account.status === 200 });
  }
  const report = { testedAt: new Date().toISOString(), projectId: config.project_info.project_id, environment: 'Firebase remoto real (Authentication y Realtime Database)', results: checks, cleanup };
  const output = path.join(__dirname, '../output/pruebas');
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(path.join(output, 'firebase-results.json'), JSON.stringify(report, null, 2));
  console.log('Resultado: ' + checks.filter(item => item.passed).length + '/' + checks.length + ' pruebas correctas.');
  if (checks.some(item => !item.passed) || cleanup.some(item => !item.dataDeleted || !item.accountDeleted)) process.exitCode = 1;
});
