/**
 * ConnectHub push relay (Cloudflare Workers, free plan, no Firebase billing needed).
 *
 * The sender's app calls this right after it writes a message to Firestore:
 *     POST /   Authorization: Bearer <Firebase ID token>   { "messageId": "<doc id>" }
 *
 * The relay then:
 *   1. verifies the Firebase ID token (so only signed-in users can call it),
 *   2. reads messages/{messageId} from Firestore as the service account and checks the
 *      caller is the message's sender (the client can't choose who gets notified or what
 *      the text says: those come from the stored message),
 *   3. reads the receiver's FCM token from users/{receiverId}.fcmToken,
 *   4. sends a data-only push through the FCM HTTP v1 API.
 *
 * Secret:  FIREBASE_SERVICE_ACCOUNT  = the full service-account JSON.
 * Var:     FIREBASE_PROJECT_ID.
 */

const GOOGLE_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/cloud-platform";

const enc = new TextEncoder();
const dec = new TextDecoder();
const RSA = { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" };

// Kept between requests while the Worker isolate stays warm.
let cachedJwks = null;
let cachedAccess = { token: null, exp: 0 };

// ---------------------------------------------------------------- small helpers
const json = (obj, status = 200) =>
  new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });

function b64url(bytes) {
  let s = "";
  for (const b of new Uint8Array(bytes)) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
const b64urlJson = (obj) => b64url(enc.encode(JSON.stringify(obj)));

function fromB64url(s) {
  s = s.replace(/-/g, "+").replace(/_/g, "/");
  while (s.length % 4) s += "=";
  return Uint8Array.from(atob(s), (c) => c.charCodeAt(0));
}

function pemToDer(pem) {
  const body = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(body), (c) => c.charCodeAt(0)).buffer;
}

// ------------------------------------------------- 1. verify the Firebase ID token
async function getJwks(forceRefresh) {
  if (!cachedJwks || forceRefresh) {
    const res = await fetch(GOOGLE_JWKS_URL);
    if (!res.ok) throw new Error("could not fetch Google signing keys");
    cachedJwks = (await res.json()).keys;
  }
  return cachedJwks;
}

/** Returns the user's uid, or throws if the token is not a valid, unexpired token for this project. */
async function verifyIdToken(token, projectId) {
  const parts = token.split(".");
  if (parts.length !== 3) throw new Error("malformed token");
  const header = JSON.parse(dec.decode(fromB64url(parts[0])));
  const payload = JSON.parse(dec.decode(fromB64url(parts[1])));
  if (header.alg !== "RS256" || !header.kid) throw new Error("bad header");

  let jwk = (await getJwks(false)).find((k) => k.kid === header.kid);
  if (!jwk) jwk = (await getJwks(true)).find((k) => k.kid === header.kid); // keys rotate
  if (!jwk) throw new Error("unknown signing key");

  const key = await crypto.subtle.importKey("jwk", jwk, RSA, false, ["verify"]);
  const valid = await crypto.subtle.verify(
    RSA, key, fromB64url(parts[2]), enc.encode(`${parts[0]}.${parts[1]}`)
  );
  if (!valid) throw new Error("bad signature");

  const now = Math.floor(Date.now() / 1000);
  if (payload.aud !== projectId) throw new Error("wrong audience");
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) throw new Error("wrong issuer");
  if (!(payload.exp > now)) throw new Error("expired");
  if (!(payload.iat <= now + 60)) throw new Error("issued in the future");
  if (!payload.sub) throw new Error("no subject");
  return payload.sub;
}

// ------------------------------------- 2. service-account access token for Google APIs
async function getAccessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  if (cachedAccess.token && cachedAccess.exp - 120 > now) return cachedAccess.token;

  const unsigned =
    b64urlJson({ alg: "RS256", typ: "JWT" }) + "." +
    b64urlJson({ iss: sa.client_email, scope: SCOPE, aud: TOKEN_URL, iat: now, exp: now + 3600 });
  const key = await crypto.subtle.importKey("pkcs8", pemToDer(sa.private_key), RSA, false, ["sign"]);
  const signature = await crypto.subtle.sign(RSA, key, enc.encode(unsigned));

  const res = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${unsigned}.${b64url(signature)}`,
    }),
  });
  if (!res.ok) throw new Error(`token exchange failed (${res.status})`);
  const data = await res.json();
  cachedAccess = { token: data.access_token, exp: now + (data.expires_in || 3600) };
  return cachedAccess.token;
}

// ------------------------------------------------------------- Firestore REST helpers
const fsBase = (projectId) =>
  `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
const strField = (doc, name) => (doc.fields && doc.fields[name] && doc.fields[name].stringValue) || "";

// --------------------------------------------------------------------------- handler
export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ error: "POST only" }, 405);
    const projectId = env.FIREBASE_PROJECT_ID;

    // 1. who is calling?
    const authHeader = request.headers.get("Authorization") || "";
    if (!authHeader.startsWith("Bearer ")) return json({ error: "missing token" }, 401);
    let uid;
    try {
      uid = await verifyIdToken(authHeader.slice(7), projectId);
    } catch (e) {
      return json({ error: "invalid token" }, 401);
    }

    // 2. which message?
    let body;
    try {
      body = await request.json();
    } catch (e) {
      return json({ error: "bad json" }, 400);
    }
    const messageId = body && body.messageId;
    if (typeof messageId !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(messageId)) {
      return json({ error: "bad messageId" }, 400);
    }

    let sa;
    try {
      sa = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
    } catch (e) {
      return json({ error: "server not configured" }, 500);
    }
    const access = await getAccessToken(sa);
    const auth = { Authorization: `Bearer ${access}` };
    const base = fsBase(projectId);

    // 3. load the stored message; only its sender may trigger a push for it
    const msgRes = await fetch(`${base}/messages/${messageId}`, { headers: auth });
    if (!msgRes.ok) return json({ error: "message not found" }, 404);
    const msg = await msgRes.json();
    if (strField(msg, "senderId") !== uid) return json({ error: "not your message" }, 403);

    const receiverId = strField(msg, "receiverId");
    if (!receiverId) return json({ error: "no receiver" }, 400);

    // 4. find the receiver's device
    const userRes = await fetch(`${base}/users/${receiverId}`, { headers: auth });
    if (!userRes.ok) return json({ sent: false, reason: "receiver not found" });
    const fcmToken = strField(await userRes.json(), "fcmToken");
    if (!fcmToken) return json({ sent: false, reason: "receiver has no device registered" });

    // 5. send the push (data-only: the app builds the notification itself)
    const isImage = strField(msg, "type") === "IMAGE";
    const fcmRes = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
      method: "POST",
      headers: { ...auth, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token: fcmToken,
          data: {
            title: strField(msg, "senderName") || "New message",
            body: isImage ? "Sent a photo" : strField(msg, "content"),
            conversationId: strField(msg, "conversationId"),
          },
          android: { priority: "HIGH" },
        },
      }),
    });

    if (!fcmRes.ok) {
      const detail = await fcmRes.text();
      // Stale token (app uninstalled / data cleared): remove it so we stop trying.
      if (fcmRes.status === 404 || detail.includes("UNREGISTERED")) {
        await fetch(`${base}/users/${receiverId}?updateMask.fieldPaths=fcmToken`, {
          method: "PATCH",
          headers: { ...auth, "Content-Type": "application/json" },
          body: JSON.stringify({ fields: {} }),
        });
        return json({ sent: false, reason: "stale token removed" });
      }
      return json({ sent: false, reason: "fcm error", status: fcmRes.status, detail }, 502);
    }
    return json({ sent: true });
  },
};
