'use strict';
/* ============================================================
   USERS & WALLET
   ============================================================ */
const isOnline = u => userSeen(u) > Date.now() - 5 * 60 * 1000;
function usersFiltered() {
  const f = S.us, q = f.q.trim().toLowerCase();
  const from = f.from ? new Date(f.from + 'T00:00:00').getTime() : 0, to = f.to ? new Date(f.to + 'T23:59:59.999').getTime() : Infinity;
  let list = S.users.filter(u => {
    if (q && ![u.fullName, u.email, u.id, u.userId, u.upiId, u.phone].join(' ').toLowerCase().includes(q)) return false;
    if (f.status === 'active' && u.isBanned) return false; if (f.status === 'banned' && !u.isBanned) return false; if (f.status === 'balance' && !num(u.walletBalance)) return false;
    const t = userMs(u); if ((f.from || f.to) && (t < from || t > to)) return false;
    return true;
  });
  const sorters = { recent: (a, b) => userMs(b) - userMs(a), balance: (a, b) => num(b.walletBalance) - num(a.walletBalance), seen: (a, b) => userSeen(b) - userSeen(a), name: (a, b) => String(a.fullName || '').localeCompare(String(b.fullName || '')) };
  return list.sort(sorters[f.sort] || sorters.recent);
}
RENDER.users = () => {
  const online = S.users.filter(isOnline).length, banned = S.users.filter(u => u.isBanned).length;
  const k = (cls, icon, l, v, sub) => `<div class="kpi s ${cls}"><div class="ico">${ic(icon, 22)}</div><div class="grow"><div class="lbl">${l}</div><div class="val">${v}</div>${sub ? `<div class="fs11 muted">${sub}</div>` : ''}</div></div>`;
  setHTML($('#us-kpi'), k('blue', 'users', 'Total Users', S.users.length, '') + k('green', 'check', 'Online now', online, 'seen in last 5 min') + k('red', 'ban', 'Blocked', banned, '') + k('orange', 'wallet', 'Wallet Liability', inr(S.users.reduce((n, u) => n + num(u.walletBalance), 0)), ''));
  if (!S.live.users && !S.users.length) { setHTML($('#us-list'), skelHTML(5)); return; }
  const p = paginate(usersFiltered(), 'users', 15, S.us);
  setHTML($('#us-list'), p.items.map(u => {
    const seen = userSeen(u), on = isOnline(u);
    return `<div class="trow cols-users"><div class="row" style="min-width:0"><span class="avatar" style="width:38px;height:38px;background:var(--peach);color:var(--brand-deep)">${esc((u.fullName || u.email || 'U')[0].toUpperCase())}</span><div style="min-width:0"><div class="b fs13 trunc">${esc(u.fullName || 'User')}</div><div class="fs12 muted trunc">${esc(u.email || '')}</div><div class="mono fs11 brand-t trunc">${esc(u.userId || u.id)}</div></div></div>
      <span class="fs12" data-l="Registered">${fmtD(userMs(u))}</span><span data-l="Status">${u.isBanned ? '<span class="pill bad">Blocked</span>' : '<span class="pill ok">Active</span>'}</span>
      <span class="b ok-t" data-l="Wallet">${inr(u.walletBalance)}</span>
      <span class="fs12" data-l="Last active">${on ? '<span class="pill ok">● Online</span>' : seen ? `<span class="muted">${ago(seen)}</span>` : '<span class="faint">—</span>'}</span>
      <span><button class="btn soft xs" data-act="openUser" data-id="${esc(u.id)}">View</button></span></div>`;
  }).join('') || emptyHTML('users', 'No users match.'));
  setHTML($('#us-pager'), pagerHTML(p));
};
INP.usQ = el => { S.us.q = el.value; S.us.page = 1; renderAll(); };
INP.usStatus = el => { S.us.status = el.value; S.us.page = 1; renderAll(); };
INP.usSort = el => { S.us.sort = el.value; renderAll(); };
INP.usFrom = el => { S.us.from = el.value; S.us.page = 1; renderAll(); };
INP.usTo = el => { S.us.to = el.value; S.us.page = 1; renderAll(); };

/* ---------- user drawer ---------- */
const isCreditTx = t => { const s = String(t.type || ''); return !(/deduct|debit|penalty/i.test(s) || (/withdraw/i.test(s) && !/refund/i.test(s))); };
async function openDrawer(uid) {
  S.drawer = { uid, tab: 'subs', ledger: null };
  $('#drawer').classList.add('show'); $('#dscrim').classList.add('show'); renderDrawer();
  try {
    const snap = await getDocs(collection(db, 'users', uid, 'transactions'));
    if (S.drawer.uid === uid) { S.drawer.ledger = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); renderDrawer(); }
  } catch (e) { if (S.drawer.uid === uid) { S.drawer.ledger = []; S.drawer.ledgerErr = e.code || e.message; renderDrawer(); } }
}
function closeDrawer() { S.drawer.uid = null; $('#drawer').classList.remove('show'); $('#dscrim').classList.remove('show'); }
ACT.closeDrawer = closeDrawer;
ACT.openUser = el => openDrawer(el.dataset.id);
ACT.drTab = el => { S.drawer.tab = el.dataset.k; renderDrawer(); };
function renderDrawer() {
  const u = S.users.find(x => x.id === S.drawer.uid); if (!u) return;
  const subs = S.subs.filter(s => s.userId === u.id || s.userId === u.userId), wds = S.wds.filter(w => w.userId === u.id || w.userId === u.userId);
  const ap = subs.filter(s => subSt(s) === 'approved').length, rj = subs.filter(s => subSt(s) === 'rejected').length, pn = subs.filter(s => subSt(s) === 'pending').length;
  const tab = S.drawer.tab, seen = userSeen(u);
  const tabs = [['subs', `Submissions (${subs.length})`], ['wds', `Withdrawals (${wds.length})`], ['ledger', 'Wallet ledger'], ['acts', 'Admin activity']].map(([k, l]) => `<button class="tab ${tab === k ? 'on' : ''}" data-act="drTab" data-k="${k}">${l}</button>`).join('');
  let body = '';
  if (tab === 'subs') body = subs.slice(0, 40).map(s => `<div class="lrow"><div class="grow"><div class="b fs13 trunc">${esc(s.taskTitle || 'Task')}</div><div class="fs12 muted">${fmtDT(subMs(s))} • ${inr(s.rewardAmount)}</div></div>${statusPill(subSt(s))}</div>`).join('') || emptyHTML('tasks', 'No submissions');
  if (tab === 'wds') body = wds.map(w => `<div class="lrow"><div class="grow"><div class="b fs13">${inr(w.amount)} <span class="tag">${wdMethod(w).toUpperCase()}</span></div><div class="fs12 muted">${fmtDT(wdMs(w))}${w.utrNumber ? ' • UTR ' + esc(w.utrNumber) : ''}</div></div>${wdPill(wdSt(w))}</div>`).join('') || emptyHTML('wallet', 'No withdrawals');
  if (tab === 'ledger') body = S.drawer.ledger === null ? skelHTML(3) : (S.drawer.ledgerErr ? `<div class="note bad">Could not load the ledger: ${esc(S.drawer.ledgerErr)}</div>` : '') + S.drawer.ledger.map(t => `<div class="lrow"><div class="rico ${isCreditTx(t) ? 'green' : 'red'}">${ic(isCreditTx(t) ? 'plus' : 'trend', 16)}</div><div class="grow" style="min-width:0"><div class="b fs13 trunc">${esc(t.note || t.type || 'Transaction')}</div><div class="fs11 muted mono">${esc(t.id.slice(0, 10))} • ${esc(t.type || '')} • ${fmtDT(tsMs(t.createdAt))}</div></div><div class="b ${isCreditTx(t) ? 'ok-t' : 'bad-t'}">${isCreditTx(t) ? '+' : '−'}${inr(t.amount)}</div></div>`).join('') || (S.drawer.ledger.length ? '' : emptyHTML('wallet', 'No ledger entries'));
  if (tab === 'acts') body = S.logs.filter(l => l.target === u.id || (l.details && (l.details.user === u.id || l.details.uid === u.id))).map(l => `<div class="lrow"><div class="grow"><div class="b fs13">${esc(l.action)}</div><div class="fs12 muted">${esc(l.adminEmail || '')} • ${fmtDT(tsMs(l.createdAt) || l.createdMs)}</div></div></div>`).join('') || emptyHTML('activity', 'No admin actions logged');
  setHTML($('#drawer'), `<div class="row between" style="padding:18px 20px;border-bottom:1px solid var(--line);position:sticky;top:0;background:var(--panel);z-index:2"><div class="row" style="min-width:0"><span class="avatar" style="width:46px;height:46px;background:var(--peach);color:var(--brand-deep);font-size:18px">${esc((u.fullName || u.email || 'U')[0].toUpperCase())}</span><div style="min-width:0"><div class="b fs16 trunc">${esc(u.fullName || 'User')}</div><div class="fs12 muted trunc">${esc(u.email || '')}</div></div></div><button class="iconbtn" data-act="closeDrawer" aria-label="Close">${ic('x')}</button></div>
    <div style="padding:18px 20px" class="col" style="gap:14px">
      <div class="row wrap">${u.isBanned ? '<span class="pill bad">Blocked</span>' : '<span class="pill ok">Active</span>'}${isOnline(u) ? '<span class="pill ok">● Online</span>' : seen ? `<span class="pill mute">Last seen ${ago(seen)}</span>` : '<span class="pill mute">Last seen unknown</span>'}<button class="btn ghost xs" data-act="copyTxt" data-t="${esc(u.userId || u.id)}">${ic('copy', 12)}${esc((u.userId || u.id).slice(0, 14))}</button></div>
      ${u.isBanned && u.banReason ? `<div class="note bad mt8">${ic('ban', 16)}<div>${esc(u.banReason)}</div></div>` : ''}
      <div class="kpi orange mt12" style="padding:14px 18px"><div class="ico">${ic('wallet', 24)}</div><div class="grow"><div class="lbl">Wallet balance</div><div class="val ok-t" style="font-size:28px">${inr(u.walletBalance)}</div></div></div>
      <div class="mini mt12"><div><b>${num(u.completedTasksCount)}</b>Completed</div><div><b>${pn}</b>Pending</div><div><b>${ap}/${rj}</b>Appr/Rej</div></div>
      <dl class="kv mt12"><dt>User ID</dt><dd class="mono">${esc(u.userId || u.id)}</dd><dt>Registered</dt><dd>${fmtDT(userMs(u))}</dd><dt>Last active</dt><dd>${seen ? fmtDT(seen) : '—'}</dd>${u.upiId ? `<dt>UPI ID</dt><dd class="mono">${esc(u.upiId)}</dd>` : ''}</dl>
      <div class="row wrap mt16"><button class="btn ok sm" data-act="walletAdj" data-id="${esc(u.id)}" data-credit="1">${ic('plus', 14)}Credit ₹</button><button class="btn soft sm" data-act="walletAdj" data-id="${esc(u.id)}" data-credit="0">Deduct ₹</button><button class="btn ${u.isBanned ? 'blue' : 'bad'} sm" data-act="banToggle" data-id="${esc(u.id)}">${ic('ban', 14)}${u.isBanned ? 'Unblock' : 'Block user'}</button><button class="btn ghost sm" data-act="notifyUser" data-id="${esc(u.id)}">${ic('bell', 14)}Notify</button></div>
      <div class="tabs mt16">${tabs}</div><div class="mt8">${body}</div></div>`);
}
ACT.copyTxt = el => copyText(el.dataset.t);
ACT.notifyUser = el => { closeDrawer(); go('notifications'); $('#nt-mode').value = 'uids'; $('#nt-uids-wrap').classList.remove('hidden'); $('#nt-uids').value = el.dataset.id; updateNtPreview(); };
ACT.walletAdj = async el => {
  const u = S.users.find(x => x.id === el.dataset.id); if (!u) return; const credit = el.dataset.credit === '1';
  const v = await ask({ title: `${credit ? 'Credit' : 'Deduct'} wallet — ${u.fullName || u.id}`, body: `Current balance: <b>${inr(u.walletBalance)}</b>. This action will be recorded in the audit log.`, ok: credit ? 'Credit wallet' : 'Deduct wallet', danger: !credit, fields: [{ id: 'amount', label: 'Amount (₹)', type: 'number', min: 1, required: true, value: '10' }, { id: 'reason', label: 'Reason (shown in ledger)', type: 'textarea', required: true, placeholder: credit ? 'e.g. Manual bonus for support ticket #123' : 'e.g. Reversal of invalid reward' }] });
  if (!v) return; const amt = Math.floor(num(v.amount)); if (amt <= 0) return toast('Enter a valid amount', 'bad');
  const ref = doc(db, 'users', u.id); let after = 0;
  await runTransaction(db, async tx => {
    const snap = await tx.get(ref); if (!snap.exists()) throw new Error('User record not found');
    const bal = num(snap.data().walletBalance), nb = credit ? bal + amt : bal - amt; if (nb < 0) throw new Error('Insufficient balance for this deduction'); after = nb;
    tx.update(ref, { walletBalance: nb, updatedAt: serverTimestamp() });
    tx.set(doc(collection(db, 'users', u.id, 'transactions')), { userId: u.id, amount: amt, type: credit ? 'admin_credit' : 'admin_deduct', note: v.reason, balanceAfter: nb, adminEmail: S.admin, createdAt: serverTimestamp() });
  });
  logAction(credit ? 'wallet_credit' : 'wallet_deduct', u.id, { amount: amt, reason: v.reason, before: num(u.walletBalance), after });
  toast(`${credit ? 'Credited' : 'Deducted'} ${inr(amt)} — new balance ${inr(after)}`, 'ok');
  S.drawer.ledger = null; openDrawerLedgerRefresh(u.id);
};
async function openDrawerLedgerRefresh(uid) { try { const snap = await getDocs(collection(db, 'users', uid, 'transactions')); if (S.drawer.uid === uid) { S.drawer.ledger = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); renderDrawer(); } } catch (_) { } }
ACT.banToggle = async el => {
  const u = S.users.find(x => x.id === el.dataset.id); if (!u) return; const ban = !u.isBanned;
  const v = await ask({ title: ban ? `Block ${u.fullName || 'user'}?` : `Unblock ${u.fullName || 'user'}?`, body: ban ? 'The user will not be able to use the app. A reason is required.' : 'The user will be able to use the app again.', ok: ban ? 'Block user' : 'Unblock', danger: ban, fields: ban ? [{ id: 'reason', label: 'Reason', type: 'textarea', required: true, placeholder: 'e.g. Fake proofs submitted repeatedly' }] : [] });
  if (!v) return; const reason = ban ? v.reason : '';
  await updateDoc(doc(db, 'users', u.id), { isBanned: ban, banReason: ban ? reason : '', bannedAt: ban ? serverTimestamp() : null, accountStatus: ban ? 'banned' : 'active', updatedAt: serverTimestamp() });
  logAction(ban ? 'user_block' : 'user_unblock', u.id, { reason }); toast(ban ? 'User blocked' : 'User unblocked', 'ok');
};


ACT.cleanDuplicates = async () => {
  const emailMap = {};
  const duplicateUsers = [];

  S.users.forEach(u => {
    const emailKey = (u.email || '').trim().toLowerCase();
    if (!emailKey) return;
    if (!emailMap[emailKey]) {
      emailMap[emailKey] = [];
    }
    emailMap[emailKey].push(u);
  });

  Object.keys(emailMap).forEach(key => {
    const group = emailMap[key];
    if (group.length > 1) {
      group.sort((a, b) => (num(b.walletBalance) - num(a.walletBalance)) || (userMs(b) - userMs(a)));
      for (let i = 1; i < group.length; i++) {
        duplicateUsers.push(group[i]);
      }
    }
  });

  if (duplicateUsers.length === 0) {
    return toast('No duplicate user accounts found!', 'ok');
  }

  const v = await ask({
    title: `Remove ${duplicateUsers.length} duplicate accounts?`,
    body: `Found <b>${duplicateUsers.length}</b> secondary duplicate user accounts sharing emails. Secondary accounts will be blocked (1 account per user rule). Primary account with highest balance will be kept active.`,
    ok: 'Clean Duplicates',
    danger: true
  });

  if (!v) return;

  let cleaned = 0;
  for (const du of duplicateUsers) {
    try {
      await updateDoc(doc(db, 'users', du.id), {
        isBanned: true,
        banReason: 'Duplicate account removed (1 account per user rule)',
        accountStatus: 'deleted',
        updatedAt: serverTimestamp()
      });
      cleaned++;
    } catch (_) {}
  }

  toast(`Cleaned ${cleaned} duplicate user accounts!`, 'ok');
  logAction('clean_duplicate_users', 'bulk', { count: cleaned });
};
