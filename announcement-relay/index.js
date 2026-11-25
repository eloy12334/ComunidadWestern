"use strict";

const fs = require("node:fs");
const path = require("node:path");

const { applicationDefault, initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "comunidad-western";
const DATABASE_URL = process.env.FIREBASE_DATABASE_URL ||
  "https://comunidad-western-default-rtdb.firebaseio.com";
const COMMUNITY_ID = process.env.FIREBASE_COMMUNITY_ID || "comunidad01";
const TOPIC = `comunidad_${COMMUNITY_ID}`;
const CREDENTIALS_PATH = process.env.GOOGLE_APPLICATION_CREDENTIALS;

function validateConfiguration() {
  if (!CREDENTIALS_PATH) {
    throw new Error(
      "Falta GOOGLE_APPLICATION_CREDENTIALS. Ejecuta iniciar-relay.ps1 y selecciona el JSON de cuenta de servicio."
    );
  }

  const credentialsPath = path.resolve(CREDENTIALS_PATH);
  if (!fs.existsSync(credentialsPath)) {
    throw new Error(`No se encontró el JSON de cuenta de servicio: ${credentialsPath}`);
  }

  if (!/^[-a-zA-Z0-9_]{1,40}$/.test(COMMUNITY_ID)) {
    throw new Error("FIREBASE_COMMUNITY_ID contiene caracteres no permitidos.");
  }

  if (!DATABASE_URL.startsWith("https://")) {
    throw new Error("FIREBASE_DATABASE_URL debe comenzar con https://.");
  }
}

function safeText(value, fallback, maxLength) {
  const text = value == null ? "" : String(value).trim();
  return (text || fallback).slice(0, maxLength);
}

async function main() {
  validateConfiguration();

  const app = initializeApp({
    credential: applicationDefault(),
    projectId: PROJECT_ID,
    databaseURL: DATABASE_URL
  });

  const database = getDatabase(app);
  const messaging = getMessaging(app);
  const announcementsRef = database.ref(`announcements/${COMMUNITY_ID}`);

  const processedIds = new Set();
  const pendingDuringStartup = new Map();
  let startupReady = false;
  let stopping = false;

  async function sendAnnouncement(snapshot) {
    const announcementId = snapshot.key;
    const announcement = snapshot.val();

    if (!announcementId || !announcement || typeof announcement !== "object") {
      console.warn("[OMITIDO] Registro de anuncio vacío o inválido.");
      return;
    }

    const authorUid = safeText(announcement.authorUid, "", 128);
    if (!authorUid) {
      console.warn(`[OMITIDO] El anuncio ${announcementId} no contiene authorUid.`);
      return;
    }

    // Verificación adicional: solamente un administrador puede originar un push.
    // Admin SDK utiliza una cuenta de servicio privilegiada; esta comprobación es explícita.
    const roleSnapshot = await database.ref(`roles/${authorUid}`).once("value");
    if (roleSnapshot.val() !== "admin") {
      console.warn(`[RECHAZADO] El autor del anuncio ${announcementId} no tiene rol admin.`);
      return;
    }

    const title = safeText(announcement.title, "Nuevo anuncio comunitario", 100);
    const body = safeText(
      announcement.body,
      "Hay información nueva en Comunidad Western.",
      1000
    );

    const message = {
      topic: TOPIC,
      data: {
        type: "announcement",
        communityId: COMMUNITY_ID,
        announcementId: String(announcementId),
        title,
        body
      },
      android: {
        priority: "high",
        ttl: 60 * 60 * 1000
      }
    };

    const messageId = await messaging.send(message);
    console.log(`[FCM OK] Anuncio ${announcementId}. Mensaje: ${messageId}`);
  }

  function processNewAnnouncement(snapshot) {
    const announcementId = snapshot.key;
    if (!announcementId || processedIds.has(announcementId)) return;

    processedIds.add(announcementId);
    sendAnnouncement(snapshot).catch((error) => {
      console.error(
        `[FCM ERROR] No se pudo notificar el anuncio ${announcementId}:`,
        error.message || error
      );
    });
  }

  const childAddedHandler = (snapshot) => {
    const announcementId = snapshot.key;
    if (!announcementId || processedIds.has(announcementId)) return;

    if (!startupReady) {
      pendingDuringStartup.set(announcementId, snapshot);
      return;
    }

    processNewAnnouncement(snapshot);
  };

  const databaseErrorHandler = (error) => {
    console.error("[Realtime Database] Error del listener:", error.message || error);
  };

  try {
    // Se conecta primero el listener para no perder anuncios publicados durante el arranque.
    announcementsRef.on("child_added", childAddedHandler, databaseErrorHandler);

    // Los anuncios que ya existían al iniciar no deben reenviarse como nuevos.
    const initialSnapshot = await announcementsRef.once("value");
    initialSnapshot.forEach((child) => {
      if (child.key) processedIds.add(child.key);
    });

    startupReady = true;

    // Procesa anuncios nuevos que llegaron mientras se cargaba el historial inicial.
    for (const [announcementId, snapshot] of pendingDuringStartup.entries()) {
      if (!processedIds.has(announcementId)) {
        processNewAnnouncement(snapshot);
      }
    }
    pendingDuringStartup.clear();

    console.log("====================================================");
    console.log(" COMUNIDAD WESTERN · RELAY LOCAL DE NOTIFICACIONES");
    console.log("====================================================");
    console.log(`Proyecto:  ${PROJECT_ID}`);
    console.log(`Comunidad: ${COMMUNITY_ID}`);
    console.log(`Topic FCM: ${TOPIC}`);
    console.log(`Ruta RTDB: announcements/${COMMUNITY_ID}`);
    console.log("Estado: escuchando nuevos anuncios.");
    console.log("Los anuncios existentes al iniciar no se reenvían.");
    console.log("Mantén esta ventana abierta para enviar notificaciones.");
    console.log("Presiona Ctrl + C para detener el relay.");
    console.log("====================================================");
  } catch (error) {
    announcementsRef.off("child_added", childAddedHandler);
    await app.delete();
    throw error;
  }

  async function shutdown() {
    if (stopping) return;
    stopping = true;
    console.log("\nDeteniendo relay FCM...");
    announcementsRef.off("child_added", childAddedHandler);
    await app.delete();
    console.log("Relay detenido.");
    process.exit(0);
  }

  process.once("SIGINT", shutdown);
  process.once("SIGTERM", shutdown);
}

main().catch((error) => {
  console.error("\nNo se pudo iniciar el relay FCM:");
  console.error(error.message || error);
  process.exitCode = 1;
});
