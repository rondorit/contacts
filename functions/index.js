// On IT - server side (Firebase Cloud Functions, gen 2).
// Push notifications: a technician gets a push when a ticket/task is assigned to him or the
// manager adds a note; the managers get a push when a technician changes status or adds a note.
// Device tokens live in users/{uid}.fcmTokens (written by the app / web app).
const { onDocumentWritten, onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { onSchedule } = require("firebase-functions/v2/scheduler");
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
  if (after.portalToken) await portalPage(org, after);
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
  if (after.arrivedAt && changed(before, after, "arrivedAt")) {
    const t = new Date(after.arrivedAt).toLocaleTimeString("he-IL", { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Jerusalem" });
    await send(await admins(org, after.arrivedBy || by), "\uD83D\uDCCD " + (after.assigneeName || "\u05D8\u05DB\u05E0\u05D0\u05D9") + " \u05D4\u05D2\u05D9\u05E2 \u05DC\u05DC\u05E7\u05D5\u05D7 " + t, [client, title].filter(Boolean).join(" \u2022 "), data);
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

// Task due reminders for technicians (every 5 minutes). Each task is pushed once per due time.
exports.taskDue = onSchedule({ schedule: "every 5 minutes", timeZone: "Asia/Jerusalem" }, async () => {
  const now = Date.now();
  const orgs = await db.collection("orgs").get();
  for (const o of orgs.docs) {
    const q = await db.collection(`orgs/${o.id}/tasks`).where("dueAt", "<=", now).where("dueAt", ">", now - 6 * 3600 * 1000).get();
    for (const d of q.docs) {
      const k = d.data();
      if (k.doneAt || !k.assigneeUid || k.pushedDue === k.dueAt) continue;
      await d.ref.update({ pushedDue: k.dueAt });
      await send([k.assigneeUid], "\u23F0 " + (k.title || "\u05DE\u05E9\u05D9\u05DE\u05D4"), [k.client, k.notes].filter(Boolean).join(" \u2022 ") || "\u05D4\u05D2\u05D9\u05E2 \u05DE\u05D5\u05E2\u05D3 \u05D4\u05DE\u05E9\u05D9\u05DE\u05D4",
        { kind: "task", org: o.id, doc: d.id });
    }
  }
});

// Weekly summary for managers: Sunday 07:30.
exports.weeklySummary = onSchedule({ schedule: "30 7 * * 0", timeZone: "Asia/Jerusalem" }, async () => {
  const now = Date.now(), week = now - 7 * 86400000;
  const orgs = await db.collection("orgs").get();
  for (const o of orgs.docs) {
    const t = (await db.collection(`orgs/${o.id}/tickets`).get()).docs.map((d) => d.data());
    const k = (await db.collection(`orgs/${o.id}/tasks`).get()).docs.map((d) => d.data());
    const open = t.filter((x) => x.status !== "Closed");
    const late = open.filter((x) => x.followupAt && x.followupAt < now).length;
    const closed = t.filter((x) => x.status === "Closed" && (x.updated || 0) > week).length;
    const tasksDone = k.filter((x) => x.doneAt && x.doneAt > week).length;
    const tasksOpen = k.filter((x) => !x.doneAt).length;
    const body = `\u05E0\u05E1\u05D2\u05E8\u05D5 \u05D4\u05E9\u05D1\u05D5\u05E2 ${closed} \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u2022 \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA ${open.length}${late ? ` (\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8 ${late})` : ""} \u2022 \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA: \u05D1\u05D5\u05E6\u05E2\u05D5 ${tasksDone}, \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA ${tasksOpen}`;
    await send(await admins(o.id, ""), "\uD83D\uDCCA \u05E1\u05D9\u05DB\u05D5\u05DD \u05E9\u05D1\u05D5\u05E2\u05D9 \u2013 " + (o.get("name") || ""), body, { kind: "summary", org: o.id, doc: "" });
  }
});

// ---------------------------------------------------------------- client pages

// Ticket status page for the client (public/{token}) - only what the client may see.
async function portalPage(org, t) {
  const o = await db.doc(`orgs/${org}`).get();
  const notes = (t.publicNotes || []).slice(-5);
  await db.doc(`public/${t.portalToken}`).set({
    type: "ticket", org, by: "server", orgName: o.get("name") || "",
    title: t.title || "", client: t.clientName || "", status: t.status || "Open", statusHe: he(t.status),
    tech: t.assigneeName || "", arrivedAt: t.arrivedAt || 0, followupAt: t.followupAt || 0,
    created: t.created || 0, updated: t.updated || Date.now(), notes,
  });
}

// Client answered a quote on the approval page -> push to whoever sent it.
exports.quoteAnswer = onDocumentWritten("public/{k}", async (ev) => {
  const b = ev.data.before.exists ? ev.data.before.data() : null;
  const a = ev.data.after.exists ? ev.data.after.data() : null;
  if (!a || a.type !== "quote" || !b || b.status === a.status) return;
  if (a.status !== "Approved" && a.status !== "Rejected") return;
  const ok = a.status === "Approved";
  await send([a.by], (ok ? "\u2705 " : "\u274C ") + "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 #" + a.quoteId + (ok ? " \u05D0\u05D5\u05E9\u05E8\u05D4" : " \u05E0\u05D3\u05D7\u05EA\u05D4") + " \u2013 " + (a.client || ""),
    a.reply || (ok ? "\u05D4\u05DC\u05E7\u05D5\u05D7 \u05D0\u05D9\u05E9\u05E8 \u05D1\u05E7\u05D9\u05E9\u05D5\u05E8" : "\u05D4\u05DC\u05E7\u05D5\u05D7 \u05D3\u05D7\u05D4 \u05D1\u05E7\u05D9\u05E9\u05D5\u05E8"), { kind: "quote", org: a.org || "", doc: String(a.quoteId) });
});

// Client reported a problem from the QR page -> new ticket for the managers.
exports.clientRequest = onDocumentCreated("requests/{r}", async (ev) => {
  const r = ev.data.data();
  await ev.data.ref.delete();
  const p = await db.doc(`public/${r.k}`).get();
  if (!p.exists || p.get("type") !== "client") return;
  const org = p.get("org");
  const text = String(r.text || "").slice(0, 1000);
  const now = Date.now();
  const ref = await db.collection(`orgs/${org}/tickets`).add({
    title: text.split("\n")[0].slice(0, 60), description: text + (r.name ? `\n(\u05D3\u05D5\u05D5\u05D7 \u05E2"\u05D9 ${String(r.name).slice(0, 80)})` : ""),
    status: "Open", priority: "Normal", category: "Other",
    clientName: p.get("client") || "", clientPhone: String(r.phone || p.get("phone") || "").slice(0, 30), clientAddress: "",
    equipment: "", mac: "", provider: "", outStatus: "", followupAt: 0, created: now, updated: now,
    updatedBy: "portal", assigneeName: "", assigneeUid: "", source: "qr",
  });
  await send(await admins(org, ""), "\uD83C\uDD95 \u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4 \u05DE\u05D4\u05DC\u05E7\u05D5\u05D7 \u2013 " + (p.get("client") || ""), text.slice(0, 150),
    { kind: "ticket", org, doc: ref.id });
});
