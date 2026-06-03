// Prints a fresh Google OAuth access token using the firebase-tools stored refresh token.
// Uses the public firebase-tools OAuth client (embedded in the open-source CLI).
// Usage: node scripts/fb-token.js
const os = require('os');
const p = require('path');
const fs = require('fs');
const https = require('https');

const CLIENT_ID = '563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com';
const CLIENT_SECRET = 'j9iVZfS8kkCEFUPaAeJV0sAi';

const cfg = p.join(os.homedir(), '.config', 'configstore', 'firebase-tools.json');
const j = JSON.parse(fs.readFileSync(cfg, 'utf8'));
const refresh = j.tokens && j.tokens.refresh_token;
if (!refresh) {
  console.error('No refresh_token found in firebase-tools config');
  process.exit(1);
}

const body = new URLSearchParams({
  client_id: CLIENT_ID,
  client_secret: CLIENT_SECRET,
  refresh_token: refresh,
  grant_type: 'refresh_token',
}).toString();

const req = https.request(
  'https://oauth2.googleapis.com/token',
  { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Content-Length': Buffer.byteLength(body) } },
  (res) => {
    let d = '';
    res.on('data', (c) => (d += c));
    res.on('end', () => {
      const o = JSON.parse(d);
      if (!o.access_token) {
        console.error('Token refresh failed:', d);
        process.exit(1);
      }
      process.stdout.write(o.access_token);
    });
  }
);
req.on('error', (e) => { console.error(e); process.exit(1); });
req.write(body);
req.end();
