'use strict';
/* ============================================================
   WITHDRAWALS
   ============================================================ */
function wdBase() {
  const f = S.wd, q = f.q.trim().toLowerCase(); let a = 0, b = Infinity;
  if (f.month) [a, b] = monthRange(f.month);
  if (f.from) a = Math.max(a, new Date(f.from + 'T00:00:00').getTime()); if (f.to) b = Math.min(b, new Date(f.to + 'T23:59:59.999').getTime());
  return S.wds.filter(w => {
    if (f.method !== 'all' && wdMethod(w) !== f.method) return false;
    if (q && ![w.id, w.userId, w.userName, w.userEmail, w.upiId, w.utrNumber, w.accountHolderName].join(' ').toLowerCase().includes(q)) return false;
    const t = wdSt(w) === 'approved' ? wdPaidMs(w) : wdMs(w); return t >= a && t <= b;
  });
}
function wdFiltered() { const t = S.wd.tab; return wdBase().filter(w => { const st = wdSt(w); return t === 'all' || (t === 'pending' ? ['pending', 'processing'].includes(st) : st === t); }); }
function wdExtras(B) {
  const f = S.wd, set = (sel, v) => { const e = $(sel); if (e && document.activeElement !== e) e.value = v; };
  set('[data-in="wdMonth"]', f.month); set('[data-in="wdFrom"]', f.from); set('[data-in="wdTo"]', f.to);
  $('#wd-mlabel').textContent = (f.from || f.to) ? `${f.from || '…'} → ${f.to || '…'}` : f.month ? monthLabel(f.month) : 'All time';
  const days = {};
  B.forEach(w => { const st = wdSt(w), t = st === 'approved' ? wdPaidMs(w) : wdMs(w), k = new Date(t).toLocaleDateString('en-CA'), d = days[k] = days[k] || { k, n: 0, paid: 0, pend: 0, rej: 0 }, amt = num(w.amount); d.n++; if (st === 'approved') d.paid += amt; else if (st === 'rejected') d.rej += amt; else d.pend += amt; });
  const rows = Object.values(days).sort((a, b) => b.k.localeCompare(a.k));
  setHTML($('#wd-days'), rows.length ? `<div class="table"><div class="thead cols-day"><span>Date</span><span>Requests</span><span>Paid</span><span>Pending</span><span>Rejected</span></div>${rows.map(d => `<button class="trow cols-day" style="width:100%;border:0;text-align:left;cursor:pointer;font:inherit;color:inherit" data-act="wdDay" data-d="${d.k}"><span class="b" data-l="Date">${fmtD(new Date(d.k + 'T00:00:00').getTime())}</span><span data-l="Requests">${d.n}</span><span class="ok-t b" data-l="Paid">${inr(d.paid)}</span><span class="brand-t b" data-l="Pending">${inr(d.pend)}</span><span class="bad-t" data-l="Rejected">${inr(d.rej)}</span></button>`).join('')}</div>` : emptyHTML('calendar', 'No payment records in this period.'));
}
const wdShift = d => { S.wd.month = shiftMonth(S.wd.month || monthKey(Date.now()), d); S.wd.from = S.wd.to = ''; S.wd.page = 1; renderAll(); };
ACT.wdPrev = () => wdShift(-1); ACT.wdNext = () => wdShift(1);
ACT.wdAll = () => { S.wd.month = S.wd.from = S.wd.to = ''; S.wd.page = 1; renderAll(); };
ACT.wdDay = el => { S.wd.from = S.wd.to = el.dataset.d; S.wd.month = ''; S.wd.tab = 'all'; S.wd.page = 1; renderAll(); };
RENDER.withdrawals = () => {
  const B = wdBase(), pend = B.filter(w => ['pending', 'processing'].includes(wdSt(w))), sum = l => l.reduce((n, w) => n + num(w.amount), 0);
  const k = (cls, icon, l, v, sub) => `<div class="kpi s ${cls}"><div class="ico">${ic(icon, 22)}</div><div class="grow"><div class="lbl">${l}</div><div class="val">${v}</div>${sub ? `<div class="fs11 muted">${sub}</div>` : ''}</div></div>`;
  const paid = B.filter(w => wdSt(w) === 'approved'), rej = B.filter(w => wdSt(w) === 'rejected');
  setHTML($('#wd-kpi'), k('orange', 'clock', 'Pending payout', inr(sum(pend)), `${pend.length} request${pend.length === 1 ? '' : 's'}`) + k('green', 'check', 'Paid', inr(sum(paid)), `${paid.length} payouts`) + k('red', 'x', 'Rejected', rej.length, inr(sum(rej))) + k('blue', 'wallet', 'Total requested', inr(sum(B)), `${B.length} requests`));
  const cnt = k2 => B.filter(w => k2 === 'pending' ? ['pending', 'processing'].includes(wdSt(w)) : k2 === 'all' ? true : wdSt(w) === k2).length;
  setHTML($('#wd-tabs'), [['pending', 'Pending'], ['approved', 'Paid / Completed'], ['rejected', 'Rejected'], ['all', 'All']].map(([x, l]) => `<button class="tab ${S.wd.tab === x ? 'on' : ''}" data-act="wdTab" data-k="${x}">${l} <span class="n">(${cnt(x)})</span></button>`).join(''));
  wdExtras(B);
  if (!S.live.withdrawals && !S.wds.length) { setHTML($('#wd-list'), skelHTML(4)); return; }
  const p = paginate(wdFiltered(), 'wd', 12, S.wd);
  setHTML($('#wd-list'), p.items.map(w => {
    const st = wdSt(w), m = wdMethod(w), pending = ['pending', 'processing'].includes(st), rev = S.wd.reveal.has(w.id);
    const pay = m === 'upi' ? `<div class="row gap6"><span class="mono fs12 trunc">${esc(w.upiId)}</span><button class="btn ghost xs" data-act="copyTxt" data-t="${esc(w.upiId)}" aria-label="Copy UPI">${ic('copy', 12)}</button></div>`
      : m === 'bank' ? `<div class="fs12" style="line-height:1.5">${esc(w.accountHolderName || '')}<br>${esc(w.bankName || 'Bank')}<br><span class="mono">${rev ? esc(w.accountNumber) : mask(w.accountNumber)}</span> <button class="btn ghost xs" data-act="wdReveal" data-id="${esc(w.id)}" aria-label="Toggle account number">${ic(rev ? 'eye-off' : 'eye', 12)}</button><br><span class="mono muted">IFSC ${esc(w.ifscCode || '—')}</span></div>` : '<span class="muted">—</span>';
    const act = pending ? `<div class="row gap6"><button class="btn okfill xs" data-act="wdPay" data-id="${esc(w.id)}">Mark Paid</button><button class="btn bad xs" data-act="wdReject" data-id="${esc(w.id)}">Reject</button></div>` : st === 'approved' ? `<span class="fs11 muted">Paid ${fmtDT(wdPaidMs(w))}<br>by ${esc(w.processedBy || '—')}</span>` : `<span class="fs11 bad-t">${esc(w.adminNote || '')}</span>`;
    return `<div class="trow cols-wd"><span class="mono fs12 b" data-l="ID">#${esc(w.id.slice(0, 8))}</span><div style="min-width:0" data-l="User"><div class="b fs13 trunc">${esc(w.userName || w.userEmail || 'User')}</div><div class="mono fs11 muted trunc">${esc(w.userId || '')}</div></div><span class="b brand-t fs16" data-l="Amount">${inr(w.amount)}</span><span class="fs12" data-l="Requested">${fmtDT(wdMs(w))}</span><div data-l="Details" style="min-width:0">${pay}</div><div data-l="Status">${wdPill(st)}${w.utrNumber ? `<div class="mono fs11 muted mt4">UTR ${esc(w.utrNumber)}</div>` : ''}</div><div>${act}</div></div>`;
  }).join('') || emptyHTML('wallet', 'No withdrawal requests in this folder.'));
  setHTML($('#wd-pager'), pagerHTML(p));
};
ACT.wdTab = el => { S.wd.tab = el.dataset.k; S.wd.page = 1; renderAll(); };
INP.wdQ = el => { S.wd.q = el.value; S.wd.page = 1; renderAll(); };
INP.wdMethod = el => { S.wd.method = el.value; S.wd.page = 1; renderAll(); };
INP.wdMonth = el => { S.wd.month = el.value; S.wd.page = 1; renderAll(); };
INP.wdFrom = el => { S.wd.from = el.value; S.wd.page = 1; renderAll(); };
INP.wdTo = el => { S.wd.to = el.value; S.wd.page = 1; renderAll(); };
ACT.wdReveal = async el => {
  const id = el.dataset.id;
  if (S.wd.reveal.has(id)) { S.wd.reveal.delete(id); return renderAll(); }
  if (!await confirmBox('Show full account number?', 'This is sensitive bank information. This access will be recorded in the audit log.', { ok: 'Show' })) return;
  S.wd.reveal.add(id); logAction('reveal_bank_details', id); renderAll();
};

/* ============================================================
   REFERRAL REWARDS  (credited ONLY when admin marks a withdrawal PAID)
   ============================================================ */
const REF_FIRST_WD_BONUS = 5, REF_WD_COMM_PCT = 1, MIN_WITHDRAWAL = 100;
const refEvId = (...p) => p.join('_').replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 120);
const refCommission = amt => Math.max(1, Math.round(num(amt) * REF_WD_COMM_PCT / 100));

ACT.wdPay = async el => {
  const w = S.wds.find(x => x.id === el.dataset.id); if (!w || busy) return;
  const v = await ask({
    title: 'Mark payout as paid',
    body: `Pay <b>${inr(w.amount)}</b> to <b>${esc(w.userName || w.userId || 'user')}</b> via ${wdMethod(w) === 'upi' ? 'UPI <span class="mono">' + esc(w.upiId) + '</span>' : 'bank transfer'}.<br>Confirm only after the payment has been sent.`,
    ok: 'Confirm paid',
    fields: [
      { id: 'utr', label: 'UTR / transaction reference', required: true, placeholder: 'e.g. 4231 5678 9012' },
      { id: 'paid', label: 'Paid amount (₹)', type: 'number', value: String(num(w.amount)), required: true, min: 1 },
      { id: 'note', label: 'Note (optional)', type: 'textarea' }
    ]
  });
  if (!v) return; busy = true;
  try {
    const wRef = doc(db, 'withdrawals', w.id);
    const uRef = w.userId ? doc(db, 'users', w.userId) : null;
    const note = v.note || `Paid manually by Admin (UTR: ${v.utr})`;
    let commCredited = 0, bonusCredited = 0;

    await runTransaction(db, async tx => {
      /* === READ 1: Withdrawal doc === */
      const wSnap = await tx.get(wRef);
      if (!wSnap.exists()) throw new Error('Withdrawal not found.');
      const curStatus = String(wSnap.data()?.status || 'pending').toLowerCase();
      if (!['pending', 'processing'].includes(curStatus)) {
        throw new Error('This request is already ' + curStatus + '.');
      }

      /* === READ 2: Referred User doc === */
      const uSnap = uRef ? await tx.get(uRef) : null;
      const uData = (uSnap && uSnap.exists()) ? uSnap.data() : null;

      // Find Referrer ID
      let aid = '';
      if (uData) {
        aid = uData.referredByUserId || '';
        if (!aid && uData.referredByCode) {
          const c = String(uData.referredByCode).trim().toUpperCase();
          aid = (S.users.find(x => x.referralCode && String(x.referralCode).trim().toUpperCase() === c) || {}).id || '';
        }
      }
      if (aid === w.userId) aid = '';

      /* === READ 3: Referrer doc === */
      const aRef = aid ? doc(db, 'users', aid) : null;
      const aSnap = aRef ? await tx.get(aRef) : null;
      const aData = (aSnap && aSnap.exists()) ? aSnap.data() : null;

      /* === READ 4: Commission transaction idempotency doc === */
      const commEventId = (aid && w.id) ? refEvId('comm_wd', aid, w.userId, w.id) : null;
      const commRef = (aid && commEventId) ? doc(db, 'users', aid, 'transactions', commEventId) : null;
      const commSnap = commRef ? await tx.get(commRef) : null;

      /* === ALL WRITES (STRICTLY AFTER ALL READS) === */
      // Write 1: Update withdrawal status to approved
      tx.update(wRef, {
        status: 'approved',
        utrNumber: v.utr,
        paidAmount: num(v.paid),
        paidAt: serverTimestamp(),
        processedAt: serverTimestamp(),
        processedBy: S.admin,
        adminNote: note,
        updatedAt: serverTimestamp()
      });

      // Write 2: Process referral bonus and 1% lifetime commission if applicable
      if (uData) {
        const withdrawAmt = num(v.paid);
        const commDone = (commSnap && commSnap.exists());
        const isFirstWd = (uData.firstWithdrawalBonusClaimed !== true);

        const comm = (aid && aData && !commDone) ? refCommission(withdrawAmt) : 0;
        const bonus = (aid && aData && isFirstWd) ? REF_FIRST_WD_BONUS : 0;
        commCredited = comm;
        bonusCredited = bonus;

        const now = serverTimestamp();

        // Update Referred User B
        const bUp = { updatedAt: now };
        if (aid && !uData.referredByUserId) bUp.referredByUserId = aid;
        if (aid && isFirstWd) {
          bUp.firstWithdrawalBonusClaimed = true;
          bUp.walletBalance = num(uData.walletBalance) + bonus;
          bUp.totalReferralBonusEarned = num(uData.totalReferralBonusEarned) + bonus;
          const bTxRef = doc(db, 'users', w.userId, 'transactions', refEvId('bonus_first_wd_self', w.userId, w.id));
          tx.set(bTxRef, {
            userId: w.userId,
            amount: bonus,
            type: 'referral_bonus',
            withdrawalId: w.id,
            note: `Referral Bonus (+₹${bonus}): your 1st withdrawal was approved`,
            createdAt: now
          });
        }
        if (comm) {
          bUp.commissionGeneratedForReferrer = num(uData.commissionGeneratedForReferrer) + comm;
        }
        tx.update(uRef, bUp);

        // Update Referrer A
        if (aid && aData) {
          tx.update(aRef, {
            walletBalance: num(aData.walletBalance) + comm + bonus,
            totalLifetimeCommissionEarned: num(aData.totalLifetimeCommissionEarned) + comm,
            totalReferralBonusEarned: num(aData.totalReferralBonusEarned) + bonus,
            updatedAt: now
          });

          if (comm && commRef) {
            tx.set(commRef, {
              userId: aid,
              amount: comm,
              type: 'referral_commission',
              withdrawalId: w.id,
              fromUserId: w.userId,
              note: `1% Commission: ${w.userName || 'Friend'} withdrew ₹${withdrawAmt} (+₹${comm})`,
              createdAt: now
            });
          }

          if (bonus) {
            const aBonusTxRef = doc(db, 'users', aid, 'transactions', refEvId('bonus_first_wd', aid, w.userId, w.id));
            tx.set(aBonusTxRef, {
              userId: aid,
              amount: bonus,
              type: 'referral_bonus',
              withdrawalId: w.id,
              fromUserId: w.userId,
              note: `Referral Bonus (+₹${bonus}): ${w.userName || 'Friend'} completed 1st withdrawal`,
              createdAt: now
            });
          }
        }
      }
    });

    if (w.userId) {
      try {
        await setDoc(doc(db, 'users', w.userId, 'withdrawals', w.id), {
          status: 'approved',
          utrNumber: v.utr,
          adminNote: note,
          updatedAt: serverTimestamp()
        }, { merge: true });
      } catch (_) { }
    }
    logAction('payout_paid', w.id, { user: w.userId, amount: num(v.paid), utr: v.utr, refCommission: commCredited, refBonus: bonusCredited });
    toast(`Marked ${inr(v.paid)} as paid` + (commCredited || bonusCredited ? ` • Referral: ${bonusCredited ? '₹5 + ₹5 bonus, ' : ''}₹${commCredited} commission` : ''), 'ok');
  } catch (e) { toast(e.message, 'bad'); }
  busy = false; renderAll();
};

ACT.wdReject = async el => {
  const w = S.wds.find(x => x.id === el.dataset.id); if (!w || busy) return;
  const v = await ask({ title: 'Reject & refund?', body: `<b>${inr(w.amount)}</b> will be credited back to the user’s wallet. The reason will be shown to the user.`, ok: 'Reject & refund', danger: true, fields: [{ id: 'reason', label: 'Reason', type: 'textarea', required: true, value: 'Invalid payment details' }] });
  if (!v) return; busy = true;
  try {
    const ref = doc(db, 'withdrawals', w.id), uref = w.userId ? doc(db, 'users', w.userId) : null;
    await runTransaction(db, async tx => {
      /* === ALL READS FIRST === */
      const snap = await tx.get(ref);
      if (!snap.exists()) throw new Error('Withdrawal not found.');
      const cur = String(snap.data()?.status || 'pending').toLowerCase();
      if (!['pending', 'processing'].includes(cur)) throw new Error('This request is already ' + cur + '.');
      const us = uref ? await tx.get(uref) : null;

      /* === ALL WRITES SECOND === */
      tx.update(ref, { status: 'rejected', adminNote: v.reason, rejectionReason: v.reason, processedBy: S.admin, updatedAt: serverTimestamp() });
      if (us && us.exists()) {
        tx.update(uref, { walletBalance: num(us.data().walletBalance) + num(w.amount), updatedAt: serverTimestamp() });
        tx.set(doc(collection(db, 'users', w.userId, 'transactions')), { userId: w.userId, amount: num(w.amount), type: 'withdrawal_refund', note: `Withdrawal rejected: ${v.reason}`, createdAt: serverTimestamp() });
      }
    });
    if (w.userId) { try { await setDoc(doc(db, 'users', w.userId, 'withdrawals', w.id), { status: 'rejected', adminNote: v.reason, updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { } }
    logAction('payout_reject', w.id, { user: w.userId, amount: num(w.amount), reason: v.reason }); toast(`Rejected & refunded ${inr(w.amount)}`, 'ok');
  } catch (e) { toast(e.message, 'bad'); }
  busy = false; renderAll();
};

ACT.wdCsv = () => {
  const rows = [['Withdrawal ID', 'User ID', 'Name', 'Amount', 'Requested', 'Status', 'Method', 'UPI / Bank', 'Account (masked)', 'IFSC', 'UTR', 'Paid at', 'Processed by', 'Note']];
  wdFiltered().forEach(w => rows.push([w.id, w.userId, w.userName || w.userEmail, num(w.amount), fmtDT(wdMs(w)), wdSt(w), wdMethod(w), w.upiId || w.bankName || '', mask(w.accountNumber), w.ifscCode || '', w.utrNumber || '', wdSt(w) === 'approved' ? fmtDT(wdPaidMs(w)) : '', w.processedBy || '', w.adminNote || '']));
  saveText(`payouts_${S.wd.tab}_${Date.now()}.csv`, toCsv(rows), 'text/csv'); logAction('export_payouts_csv', S.wd.tab, { rows: rows.length - 1 });
};
