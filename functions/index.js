const admin = require("firebase-admin");
const crypto = require("crypto");
const { onValueCreated } = require("firebase-functions/v2/database");
const { onCall, HttpsError } = require("firebase-functions/v2/https");

admin.initializeApp();
const db = admin.database();
const REGION = "us-central1";

function requireUser(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
  }
  return request.auth.uid;
}

async function getPlayerProfile(uid) {
  const snap = await db.ref(`/profiles/${uid}`).get();
  if (!snap.exists()) {
    throw new HttpsError("failed-precondition", "Tu perfil comunitario no está completo.");
  }
  const profile = snap.val() || {};
  return {
    name: String(profile.name || "Vaquero").slice(0, 60),
    housing: String(profile.housing || "").slice(0, 10)
  };
}

function cleanCode(value) {
  return String(value || "").trim().toUpperCase();
}

function findOpponent(players, uid) {
  return Object.keys(players || {}).find((playerUid) => playerUid !== uid) || null;
}

// Community announcements: only a UID provisioned as admin may trigger a push.
exports.notifyAnnouncement = onValueCreated({
  ref: "/announcements/{communityId}/{announcementId}",
  region: REGION
}, async (event) => {
  const announcement = event.data.val();
  if (!announcement || !announcement.authorUid) return;
  const role = await db.ref(`/roles/${announcement.authorUid}`).get();
  if (role.val() !== "admin") {
    console.warn("Announcement ignored: author is not admin", announcement.authorUid);
    return;
  }
  await admin.messaging().send({
    topic: `comunidad_${event.params.communityId}`,
    data: {
      type: "announcement",
      communityId: String(event.params.communityId),
      announcementId: String(event.params.announcementId),
      title: String(announcement.title || "Anuncio comunitario"),
      body: String(announcement.body || "Hay información nueva")
    }
  });
});

// Creates a private room. The room code is short and shared with the opponent.
exports.createDuel = onCall({ region: REGION }, async (request) => {
  const uid = requireUser(request);
  const bestOf = Number(request.data && request.data.bestOf);
  if (![1, 3, 5].includes(bestOf)) {
    throw new HttpsError("invalid-argument", "Elige mejor de 1, 3 o 5.");
  }

  const profile = await getPlayerProfile(uid);
  for (let attempt = 0; attempt < 6; attempt++) {
    const code = crypto.randomBytes(3).toString("hex").toUpperCase();
    const ref = db.ref(`/duels/${code}`);
    const now = Date.now();
    const room = {
      code,
      hostUid: uid,
      players: { [uid]: { ...profile, joinedAt: now } },
      bestOf,
      targetWins: Math.ceil(bestOf / 2),
      scores: { [uid]: 0 },
      phase: "WAITING",
      roundNumber: 0,
      createdAt: now,
      updatedAt: now
    };
    const result = await ref.transaction((current) => current === null ? room : undefined, undefined, false);
    if (result.committed) return { code };
  }
  throw new HttpsError("resource-exhausted", "No se pudo reservar una sala. Intenta nuevamente.");
});

// A second authenticated user joins with the six-character room code.
exports.joinDuel = onCall({ region: REGION }, async (request) => {
  const uid = requireUser(request);
  const code = cleanCode(request.data && request.data.code);
  if (!/^[A-F0-9]{6}$/.test(code)) {
    throw new HttpsError("invalid-argument", "El código debe tener 6 caracteres.");
  }

  const profile = await getPlayerProfile(uid);
  const ref = db.ref(`/duels/${code}`);
  const now = Date.now();
  let issue = "";
  const result = await ref.transaction((current) => {
    issue = "";
    if (!current) { issue = "not-found"; return; }
    if (current.hostUid === uid || (current.players && current.players[uid])) return current;
    if (current.phase !== "WAITING") { issue = "closed"; return; }
    const players = current.players || {};
    if (Object.keys(players).length >= 2) { issue = "full"; return; }
    players[uid] = { ...profile, joinedAt: now };
    current.players = players;
    current.scores = current.scores || {};
    current.scores[uid] = 0;
    current.phase = "READY";
    current.updatedAt = now;
    return current;
  }, undefined, false);

  if (!result.committed) {
    if (issue === "not-found") throw new HttpsError("not-found", "No existe una sala con ese código.");
    if (issue === "full") throw new HttpsError("resource-exhausted", "La sala ya está completa.");
    if (issue === "closed") throw new HttpsError("failed-precondition", "La partida ya comenzó o terminó.");
    throw new HttpsError("failed-precondition", "No se pudo ingresar a la sala.");
  }
  return { code };
});

// Only the host starts rounds. The server chooses a fresh 3–5 second delay.
exports.startDuelRound = onCall({ region: REGION }, async (request) => {
  const uid = requireUser(request);
  const code = cleanCode(request.data && request.data.code);
  const ref = db.ref(`/duels/${code}`);
  const now = Date.now();
  const delayMs = crypto.randomInt(3000, 5001);
  let issue = "";
  const result = await ref.transaction((current) => {
    issue = "";
    if (!current) { issue = "not-found"; return; }
    if (current.hostUid !== uid) { issue = "not-host"; return; }
    const players = current.players || {};
    if (Object.keys(players).length !== 2) { issue = "players"; return; }
    if (current.phase !== "READY" && current.phase !== "ROUND_RESULT") { issue = "phase"; return; }
    if (current.phase === "ROUND_RESULT" && current.winnerUid) {
      const winnerScore = Number((current.scores || {})[current.winnerUid] || 0);
      if (winnerScore >= Number(current.targetWins || 1)) { issue = "finished"; return; }
    }
    current.roundNumber = Number(current.roundNumber || 0) + 1;
    current.dueAt = now + delayMs;
    current.countdownMs = delayMs;
    current.roundResult = null;
    current.phase = "COUNTDOWN";
    current.updatedAt = now;
    return current;
  }, undefined, false);

  if (!result.committed) {
    if (issue === "not-host") throw new HttpsError("permission-denied", "Solo quien creó la sala puede iniciar la ronda.");
    if (issue === "players") throw new HttpsError("failed-precondition", "Falta que se una el segundo jugador.");
    if (issue === "finished") throw new HttpsError("failed-precondition", "El duelo ya terminó.");
    throw new HttpsError("failed-precondition", "No se puede iniciar una ronda en este momento.");
  }
  return { ok: true };
});

// The server adjudicates the first received shot and applies false-start penalties.
exports.shootDuel = onCall({ region: REGION }, async (request) => {
  const uid = requireUser(request);
  const code = cleanCode(request.data && request.data.code);
  const ref = db.ref(`/duels/${code}`);
  const receivedAt = Date.now();
  let issue = "";
  const result = await ref.transaction((current) => {
    issue = "";
    if (!current) { issue = "not-found"; return; }
    const players = current.players || {};
    if (!players[uid]) { issue = "not-player"; return; }
    if (current.phase !== "COUNTDOWN") { issue = "resolved"; return; }

    const opponentUid = findOpponent(players, uid);
    if (!opponentUid) { issue = "players"; return; }

    const falseStart = receivedAt < Number(current.dueAt || 0);
    const winnerUid = falseStart ? opponentUid : uid;
    const scores = { ...(current.scores || {}) };
    scores[winnerUid] = Number(scores[winnerUid] || 0) + 1;
    const targetWins = Number(current.targetWins || 1);
    const matchFinished = scores[winnerUid] >= targetWins;

    current.scores = scores;
    current.winnerUid = matchFinished ? winnerUid : null;
    current.phase = matchFinished ? "MATCH_FINISHED" : "ROUND_RESULT";
    current.roundResult = {
      winnerUid,
      shooterUid: uid,
      reason: falseStart ? "FALSE_START" : "QUICK_DRAW",
      receivedAt
    };
    current.updatedAt = receivedAt;
    return current;
  }, undefined, false);

  if (!result.committed) {
    if (issue === "not-player") throw new HttpsError("permission-denied", "No perteneces a esta partida.");
    throw new HttpsError("failed-precondition", "La ronda ya fue resuelta o no está activa.");
  }
  return { ok: true };
});
