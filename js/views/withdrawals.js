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
ACT.wdPay = async el => {
  const w = S.wds.find(x => x.id === el.dataset.id); if (!w || busy) return;
  const v = await ask({
    title: 'Mark payout as paid', body: `Pay <b>${inr(w.amount)}</b> to <b>${esc(w.userName || w.userId || 'user')}</b> via ${wdMethod(w) === 'upi' ? 'UPI <span class="mono">' + esc(w.upiId) + '</span>' : 'bank transfer'}.<br>Confirm only after the payment has been sent.`, ok: 'Confirm paid',
    fields: [{ id: 'utr', label: 'UTR / transaction reference', required: true, placeholder: 'e.g. 4231 5678 9012' }, { id: 'paid', label: 'Paid amount (₹)', type: 'number', value: String(num(w.amount)), required: true, min: 1 }, { id: 'note', label: 'Note (optional)', type: 'textarea' }]
  });
  if (!v) return; busy = true;
  try {
    const ref = doc(db, 'withdrawals', w.id), note = v.note || `Paid manually by Admin (UTR: ${v.utr})`;
    const uref = w.userId ? doc(db, 'users', w.userId) : null;
    await runTransaction(db, async tx => {
      const snap = await tx.get(ref); const cur = String(snap.data()?.status || 'pending').toLowerCase();
      if (!['pending', 'processing'].includes(cur)) throw new Error('This request is already ' + cur + '.');
      tx.update(ref, { status: 'approved', utrNumber: v.utr, paidAmount: num(v.paid), paidAt: serverTimestamp(), processedAt: serverTimestamp(), processedBy: S.admin, adminNote: note, updatedAt: serverTimestamp() });

      if (uref) {
        const us = await tx.get(uref);
        if (us && us.exists()) {
          const uData = us.data();
          const referredByUid = uData.referredByUserId || '';
          const referredByCode = uData.referredByCode || '';
          const withdrawAmt = num(v.paid);

          // 1. Check First Withdrawal Bonus Eligibility (₹5 for Referred User B, ₹5 for Referrer A)
          const firstWdClaimed = uData.firstWithdrawalBonusClaimed === true;
          const isEligibleFirstWd = (referredByUid || referredByCode) && !firstWdClaimed;

          if (isEligibleFirstWd) {
            tx.update(uref, {
              firstWithdrawalBonusClaimed: true,
              walletBalance: num(uData.walletBalance) + 5,
              totalReferralBonusEarned: num(uData.totalReferralBonusEarned) + 5,
              updatedAt: serverTimestamp()
            });
            tx.set(doc(collection(db, 'users', w.userId, 'transactions')), {
              userId: w.userId,
              amount: 5,
              type: 'referral_bonus',
              note: '1st Withdrawal Referral Bonus (+₹5)',
              createdAt: serverTimestamp()
            });
          }

          // 2. Credit 1% Lifetime Referral Commission to Referrer A
          const targetRefUid = referredByUid || (S.users.find(u => u.referralCode && u.referralCode.toUpperCase() === referredByCode.toUpperCase()) || {}).id || '';
          if (targetRefUid) {
            const referrerRef = doc(db, 'users', targetRefUid);
            const refSnap = await tx.get(referrerRef);
            if (refSnap.exists()) {
              const commAmount = Math.max(1, Math.round(withdrawAmt * 0.01));
              const refBal = num(refSnap.data().walletBalance);
              const refComm = num(refSnap.data().totalLifetimeCommissionEarned);
              const refBonus = isEligibleFirstWd ? 5 : 0;

              tx.update(referrerRef, {
                walletBalance: refBal + commAmount + refBonus,
                totalLifetimeCommissionEarned: refComm + commAmount,
                totalReferralBonusEarned: num(refSnap.data().totalReferralBonusEarned) + refBonus,
                updatedAt: serverTimestamp()
              });

              // Log 1% Lifetime Commission Transaction
              const commEventId = `comm_wd_${targetRefUid}_${w.userId}_${w.id}`.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 120);
              tx.set(doc(db, 'users', targetRefUid, 'transactions', commEventId), {
                userId: targetRefUid,
                amount: commAmount,
                type: 'referral_commission',
                note: `1% Lifetime Commission: ${w.userName || 'Friend'} withdrew ₹${withdrawAmt} (+₹${commAmount})`,
                withdrawalId: w.id,
                createdAt: serverTimestamp()
              });

              // Log ₹5 Bonus Transaction for Referrer
              if (isEligibleFirstWd) {
                const bonusEventId = `bonus_first_wd_${targetRefUid}_${w.userId}`.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 120);
                tx.set(doc(db, 'users', targetRefUid, 'transactions', bonusEventId), {
                  userId: targetRefUid,
                  amount: 5,
                  type: 'referral_bonus',
                  note: `Referral Bonus (+₹5): ${w.userName || 'Friend'} completed 1st withdrawal`,
                  createdAt: serverTimestamp()
                });
              }
            }
          }
        }
      }
    });
    if (w.userId) { try { await setDoc(doc(db, 'users', w.userId, 'withdrawals', w.id), { status: 'approved', utrNumber: v.utr, adminNote: note, updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { } }
    logAction('payout_paid', w.id, { user: w.userId, amount: num(v.paid), utr: v.utr }); toast(`Marked ${inr(v.paid)} as paid`, 'ok');
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
      const snap = await tx.get(ref); const cur = String(snap.data()?.status || 'pending').toLowerCase();
      if (!['pending', 'processing'].includes(cur)) throw new Error('This request is already ' + cur + '.');
      const us = uref ? await tx.get(uref) : null;
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
