const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const admin = require('firebase-admin');
const { randomUUID } = require('crypto');
admin.initializeApp();
const db = admin.firestore();

/* 1) PUSH: when the panel sends a notification, also send a real push.
      User app must subscribe to topic "all_users" and save its token in users/{uid}.fcmToken */
exports.pushOnBroadcast = onDocumentCreated('broadcasts/{id}', async (event) => {
  const d = event.data && event.data.data();
  if (!d || event.params.id === 'system_maintenance_banner' || d.active === false) return;
  if (d.expiresAtMillis && Date.now() > d.expiresAtMillis) return;
  const notification = { title: String(d.title || 'Task Gullak'), body: String(d.message || '') };
  const data = { id: event.params.id, type: String(d.type || 'broadcast') };
  if (!d.targetUserId || d.targetUserId === 'all') {
    await admin.messaging().send({ topic: 'all_users', notification, data });
  } else {
    const u = await db.doc(`users/${d.targetUserId}`).get();
    const token = u.get('fcmToken');
    if (token) await admin.messaging().send({ token, notification, data });
  }
});

/* 2) DAILY CLEANUP (00:05 IST): archive + delete yesterday's tasks, delete expired notifications */
exports.dailyCleanup = onSchedule({ schedule: '5 0 * * *', timeZone: 'Asia/Kolkata' }, async () => {
  const ist = new Date(Date.now() + 5.5 * 3600e3); ist.setUTCHours(0, 0, 0, 0);
  const startOfToday = new Date(ist.getTime() - 5.5 * 3600e3);
  const old = await db.collection('tasks').where('createdAt', '<', admin.firestore.Timestamp.fromDate(startOfToday)).get();
  for (const t of old.docs) {
    const x = t.data();
    await db.doc(`taskArchive/${t.id}`).set({
      id: t.id, title: x.title || x.appName || '', appId: x.appId || x.packageName || '',
      rewardInr: Number(x.rewardInr ?? x.rewardAmount ?? 0), submittedUserIds: x.submittedUserIds || [],
      blockedUserIds: x.blockedUserIds || [], archivedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    await t.ref.delete(); await db.doc(`offers/${t.id}`).delete().catch(() => {});
  }
  const exp = await db.collection('broadcasts').where('expiresAtMillis', '<', Date.now()).get();
  for (const b of exp.docs) { await b.ref.delete(); await db.doc(`notifications/${b.id}`).delete().catch(() => {}); }
});

/* 3) SCREENSHOTS: move base64 images out of Firestore into Storage (keeps docs small and cheap).
      Keep DELETE_BASE64 = false until your USER APP reads screenshotUrl, then set it to true. */
const DELETE_BASE64 = false;
exports.moveShotToStorage = onDocumentCreated('submissions/{id}', async (event) => {
  const d = event.data && event.data.data(); const b64 = d && d.screenshotBase64;
  if (!b64 || d.screenshotUrl) return;
  const m = /^data:(image\/[a-z+.-]+);base64,(.*)$/s.exec(b64);
  const type = m ? m[1] : 'image/jpeg', buf = Buffer.from(m ? m[2] : b64, 'base64');
  const bucket = admin.storage().bucket(), path = `proofs/${event.params.id}.${type.split('/')[1].replace('jpeg', 'jpg')}`, token = randomUUID();
  await bucket.file(path).save(buf, { contentType: type, metadata: { metadata: { firebaseStorageDownloadTokens: token } } });
  const url = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(path)}?alt=media&token=${token}`;
  const patch = { screenshotUrl: url };
  if (DELETE_BASE64) patch.screenshotBase64 = admin.firestore.FieldValue.delete();
  await event.data.ref.update(patch);
});
