// On IT - server side (Firebase Cloud Functions, gen 2).
// Push notifications: a technician gets a push when a ticket/task is assigned to him or the
// manager adds a note; the managers get a push when a technician changes status or adds a note.
// Device tokens live in users/{uid}.fcmTokens (written by the app / web app).
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 3, memory: "256MiB" });
const db = admin.firestore();

const STATUS = { "Open": "\u05E4\u05EA\u05D5\u05D7\u05D4", "In progress": "\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC", "Waiting for client": "\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05DC\u05E7\u05D5\u05D7", "Waiting for outsourcing": "\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5", "Closed": "\u05E1\u05D2\u05D5\u05E8\u05D4" };
const he = (s) => STATUS[s] || s || "";

async function tokensOf(uids) {
  const out = [];
  for (const u of new Set(uids.filter(Boolean))) {
    const d = await db.doc(`users/${u}`).get();
    for (const t of (d.get("fcmTokens") || [])) out.push({ uid: u, token: t });
  }
  return out;
}

async function admins(org, except) {
  const q = await db.collection(`orgs/${org}/members`).where("role", "==", "admin").get();
  return q.docs.map((d) => d.id).filter((u) => u !== except);
}

async function send(uids, title, body, data) {
  const list = await tokensOf(uids);
  if (!list.length) return;
  const res = await admin.messaging().sendEachForMulticast({
    tokens: list.map((x) => x.token),
    // Data message: the Android app builds the notification itself (works in foreground too).
    data: Object.assign({ title, body }, data),
    android: { priority: "high" },
    // Web / iPhone (PWA) show this one.
    webpush: {
      notification: { title, body, icon: "/contacts/icon.svg", dir: "rtl", lang: "he" },
      fcmOptions: { link: "/contacts/" },
    },
  });
  // Drop tokens of uninstalled apps / signed-out browsers.
  const dead = [];
  res.responses.forEach((r, i) => {
    const c = r.error && r.error.code;
    if (c === "messaging/registration-token-not-registered" || c === "messaging/invalid-registration-token") dead.push(list[i]);
  });
  for (const x of dead) {
    await db.doc(`users/${x.uid}`).update({ fcmTokens: admin.firestore.FieldValue.arrayRemove(x.token) }).catch(() => {});
  }
}

function changed(a, b, f) { return (a ? a[f] : undefined) !== (b ? b[f] : undefined); }

exports.ticketPush = onDocumentWritten("orgs/{org}/tickets/{id}", async (ev) => {
  const before = ev.data.before.exists ? ev.data.before.data() : null;
  const after = ev.data.after.exists ? ev.data.after.data() : null;
  if (!after) return;
  const org = ev.params.org;
  const by = after.updatedBy || "";
  const tech = after.assigneeUid || "";
  const title = after.title || "\u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA";
  const client = after.clientName || "";
  const data = { kind: "ticket", org, doc: ev.params.id };

  if (tech && changed(before, after, "assigneeUid") && tech !== by) {
    await send([tech], "\uD83C\uDFAB \u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4 \u05D4\u05D5\u05E7\u05E6\u05EA\u05D4 \u05DC\u05DA", [title, client, after.clientAddress].filter(Boolean).join(" \u2022 "), data);
    return;
  }
  const fromTech = tech && by === tech;
  if (changed(before, after, "lastNote") && after.lastNote) {
    const to = fromTech ? await admins(org, by) : [tech];
    await send(to.filter((u) => u && u !== by), "\uD83D\uDCAC " + title, after.lastNote, data);
  }
  if (before && changed(before, after, "status") && fromTech) {
    await send(await admins(org, by), "\uD83D\uDD04 " + (after.assigneeName || "\u05D8\u05DB\u05E0\u05D0\u05D9") + ": " + he(after.status), [title, client].filter(Boolean).join(" \u2022 "), data);
  }
  if (before && changed(before, after, "outStatus") && fromTech && after.outStatus) {
    await send(await admins(org, by), "\uD83D\uDCE6 " + title, "\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5: " + after.outStatus, data);
  }
});

exports.taskPush = onDocumentWritten("orgs/{org}/tasks/{id}", async (ev) => {
  const before = ev.data.before.exists ? ev.data.before.data() : null;
  const after = ev.data.after.exists ? ev.data.after.data() : null;
  if (!after) return;
  const org = ev.params.org;
  const by = after.updatedBy || "";
  const tech = after.assigneeUid || "";
  const data = { kind: "task", org, doc: ev.params.id };
  if (tech && changed(before, after, "assigneeUid") && tech !== by) {
    await send([tech], "\u2705 \u05DE\u05E9\u05D9\u05DE\u05D4 \u05D7\u05D3\u05E9\u05D4 \u05D4\u05D5\u05E7\u05E6\u05EA\u05D4 \u05DC\u05DA", [after.title, after.client].filter(Boolean).join(" \u2022 "), data);
    return;
  }
  if (before && tech && by === tech && !before.doneAt && after.doneAt) {
    await send(await admins(org, by), "\u2714 " + (after.assigneeName || "\u05D8\u05DB\u05E0\u05D0\u05D9") + " \u05E1\u05D9\u05D9\u05DD \u05DE\u05E9\u05D9\u05DE\u05D4", after.title || "", data);
  }
});
