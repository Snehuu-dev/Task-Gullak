'use strict';
/* ============================================================
   REALTIME LISTENERS
   ============================================================ */
let raf = 0;
const schedule = () => { cancelAnimationFrame(raf); raf = requestAnimationFrame(() => renderAll()); };

/* Submitted = hidden: once a user has submitted a task, add them to that task's blocked list so it never shows for them again.
   You decide per upload: edit the task and choose "Show it again to everyone who already submitted" (sets reshowAt = now). Only submissions made after that are counted. */
let syncT = 0, syncBusy = false;
const queueSync = () => { clearTimeout(syncT); syncT = setTimeout(syncSubmitters, 1500); };
async function syncSubmitters() {
  if (syncBusy || !S.admin || !S.live.tasks || !S.live.submissions) return;
  const jobs = {}, mig = [];
  for (const t of S.tasks) if ((t.autoBlockedUserIds || []).length) mig.push(t);   // tidy-up: earlier version had put submitters into the blocked list
  for (const sb of S.subs) {
    if (!sb.userId) continue; const t = taskOfSub(sb); if (!t || t.hideAfterComplete === false) continue;
    const re = num(t.reshowAt); if (re && subMs(sb) && subMs(sb) <= re) continue;
    if ((t.submittedUserIds || []).includes(sb.userId) || (t.autoBlockedUserIds || []).includes(sb.userId)) continue;
    (jobs[t.id] = jobs[t.id] || new Set()).add(sb.userId);
  }
  if (!mig.length && !Object.keys(jobs).length) return;
  syncBusy = true;
  try {
    for (const t of mig) { const u = t.autoBlockedUserIds; await updateDoc(doc(db, 'tasks', t.id), { blockedUserIds: arrayRemove(...u), submittedUserIds: arrayUnion(...u), autoBlockedUserIds: [] }); }
    for (const tid of Object.keys(jobs)) { const u = [...jobs[tid]]; await updateDoc(doc(db, 'tasks', tid), { submittedUserIds: arrayUnion(...u), updatedAt: serverTimestamp() }); }
  } catch (e) { console.warn('submitter sync', e.code || e.message); }
  syncBusy = false;
}
let started = false;
function listen(key, ref, apply) {
  onSnapshot(ref, snap => { S.live[key] = true; S.errs[key] = null; apply(snap); schedule(); if (key === 'tasks' || key === 'submissions') queueSync(); },
    err => { S.live[key] = false; S.errs[key] = err.code || err.message; if (!['adminLogs', 'adminMeta'].includes(key)) console.warn('Listener', key, err.code || err.message); schedule(); });
}
function startListeners() {
  if (started) return; started = true;
  listen('users', collection(db, 'users'), snap => { S.users = snap.docs.map(d => ({ id: d.id, ...d.data() })); });
  listen('tasks', collection(db, 'tasks'), snap => { S.tasks = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); setTimeout(sweepExpired, 800); });
  listen('submissions', collection(db, 'submissions'), snap => { S.subs = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => subMs(b) - subMs(a)); });
  listen('withdrawals', collection(db, 'withdrawals'), snap => { S.wds = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => wdMs(b) - wdMs(a)); });
  listen('broadcasts', collection(db, 'broadcasts'), snap => { S.bcasts = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); });
  listen('adminLogs', query(collection(db, 'adminLogs'), orderBy('createdMs', 'desc'), limit(300)), snap => { S.logs = snap.docs.map(d => ({ id: d.id, ...d.data() })); });
  listen('adminMeta', collection(db, 'adminMeta'), snap => { S.meta = {}; snap.docs.forEach(d => { S.meta[d.id] = d.data(); }); });
}
