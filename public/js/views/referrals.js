'use strict';
/* === REFERRAL MANAGEMENT VIEW === */
const REF_FIRST_WD_BONUS = 5, REF_WD_COMM_PCT = 1, MIN_WITHDRAWAL = 100;
const refEvId = (...p) => p.join('_').replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 120);
const refCommission = amt => Math.max(1, Math.round(num(amt) * REF_WD_COMM_PCT / 100));

function refFindReferrerId(u, selfId) {
  if (!u) return '';
  let id = u.referredByUserId || '';
  if (!id && u.referredByCode) {
    const c = String(u.referredByCode).trim().toUpperCase();
    id = ((S.users || []).find(x => x && x.referralCode && String(x.referralCode).trim().toUpperCase() === c) || {}).id || '';
  }
  return id && id !== selfId ? id : '';
}

async function refReadPlan(tx, w) {
  if (!w.userId) return null;
  const uref = doc(db, 'users', w.userId), us = await tx.get(uref);
  if (!us.exists()) return null;
  const u = us.data(), aid = refFindReferrerId(u, w.userId);
  if (!aid) return null;
  const aref = doc(db, 'users', aid), as = await tx.get(aref);
  if (!as.exists()) return null;
  const commRef = doc(db, 'users', aid, 'transactions', refEvId('comm_wd', aid, w.userId, w.id));
  const commSnap = await tx.get(commRef);
  return { uref, u, aref, a: as.data(), aid, commRef, commDone: commSnap.exists(), first: u.firstWithdrawalBonusClaimed !== true };
}

function refApplyPlan(tx, p, w) {
  const comm = p.commDone ? 0 : refCommission(w.amount), bonus = p.first ? REF_FIRST_WD_BONUS : 0;
  if (!comm && !bonus) return { comm: 0, bonus: 0 };
  const who = w.userName || 'Friend', now = serverTimestamp();

  const bUp = { updatedAt: now };
  if (!p.u.referredByUserId) bUp.referredByUserId = p.aid;
  if (bonus) {
    bUp.firstWithdrawalBonusClaimed = true;
    bUp.walletBalance = num(p.u.walletBalance) + bonus;
    tx.set(doc(collection(db, 'users', w.userId, 'transactions')), {
      userId: w.userId,
      amount: bonus,
      type: 'first_withdrawal_bonus',
      note: 'Joining Bonus on First Withdrawal (+₹5)',
      createdAt: now
    });
  }
  tx.update(p.uref, bUp);

  const aUp = {
    walletBalance: num(p.a.walletBalance) + comm + bonus,
    totalReferralBonusEarned: num(p.a.totalReferralBonusEarned) + bonus,
    totalLifetimeCommissionEarned: num(p.a.totalLifetimeCommissionEarned) + comm,
    updatedAt: now
  };
  tx.update(p.aref, aUp);

  if (bonus) {
    tx.set(doc(collection(db, 'users', p.aid, 'transactions')), {
      userId: p.aid,
      amount: bonus,
      type: 'referral_bonus',
      note: `Referral Joining Bonus for ${who} (+₹5)`,
      createdAt: now
    });
  }
  if (comm) {
    tx.set(p.commRef, {
      userId: p.aid,
      referredUserId: w.userId,
      amount: comm,
      type: 'referral_commission',
      note: `1% Lifetime Commission on ${who}'s payout (+₹${comm})`,
      createdAt: now
    });
  }
  return { comm, bonus };
}

ACT.fixReferralBalances = async () => {
  if (!await confirmBox('Sync missing referral rewards?', 'Scans all APPROVED withdrawals of referred users and credits what is missing: ₹5 + ₹5 on the 1st approved withdrawal and 1% commission on every approved withdrawal. Each reward has a fixed ID, so nothing can be credited twice.')) return;
  busy = true; renderAll();
  try {
    let fixed = 0, money = 0;
    // oldest first, so the 1st withdrawal of a user gets the ₹5 bonus and later ones only commission
    const paid = S.wds.filter(w => wdSt(w) === 'approved' && w.userId).sort((x, y) => wdPaidMs(x) - wdPaidMs(y));
    for (const w of paid) {
      const u = S.users.find(x => x.id === w.userId);
      if (!u || !refFindReferrerId(u, w.userId)) continue;
      const res = await runTransaction(db, async tx => {
        const plan = await refReadPlan(tx, w);
        if (!plan || (plan.commDone && !plan.first)) return { comm: 0, bonus: 0 };
        return refApplyPlan(tx, plan, w);
      });
      if (res.comm || res.bonus) { fixed++; money += res.comm + res.bonus * 2; }
    }
    toast(`Done. Credited ₹${money} for ${fixed} withdrawal${fixed === 1 ? '' : 's'}.`, 'ok');
  } catch (e) { console.error(e); toast(e.message, 'bad'); }
  busy = false; renderAll();
};

RENDER.referrals = () => {
  const users = S.users || [];
  let totalReferrals = 0, totalBonus = 0, totalComm = 0;

  users.forEach(u => {
    if (u.referredByUserId || u.referredByCode) totalReferrals++;
    totalBonus += (num(u.totalReferralBonusEarned) || 0);
    totalComm += (num(u.totalLifetimeCommissionEarned) || num(u.referralCommissionEarned) || 0);
  });

  const countEl = $('#ref-total-count'); if (countEl) countEl.textContent = `${totalReferrals} connected referrals`;
  const bonusEl = $('#ref-total-bonus'); if (bonusEl) bonusEl.textContent = inr(totalBonus);
  const commEl = $('#ref-total-comm'); if (commEl) commEl.textContent = inr(totalComm);

  const container = $('#referrals-user-list'); if (!container) return;
  const referredUsersList = users.filter(u => (u.referralCode || u.referredByCode || u.totalReferralBonusEarned || u.totalLifetimeCommissionEarned));

  if (!referredUsersList.length) {
    setHTML(container, emptyHTML('gift', 'No active user referrals logged yet.'));
    return;
  }

  setHTML(container, referredUsersList.map(u => {
    const referredCount = users.filter(x => refFindReferrerId(x, x.id) === u.id).length;
    const bonus = num(u.totalReferralBonusEarned) || 0;
    const comm = num(u.totalLifetimeCommissionEarned) || num(u.referralCommissionEarned) || 0;

    return `<div class="card row between wrap gap10 mt10">
      <div>
        <div class="b fs14">${esc(u.fullName || u.username || 'User')} <span class="mono muted fs12">(${esc(u.email || u.id)})</span></div>
        <div class="fs12 muted">Referral Code: <span class="pill brand mono">${esc(u.referralCode || '—')}</span> • Referred By: <span class="mono">${esc(u.referredByCode || '—')}</span></div>
      </div>
      <div class="row gap12">
        <div class="text-right"><div class="muted fs11">Referred Friends</div><div class="b fs14 brand-t">${referredCount}</div></div>
        <div class="text-right"><div class="muted fs11">Bonus Earned</div><div class="b fs14 ok-t">${inr(bonus)}</div></div>
        <div class="text-right"><div class="muted fs11">1% Lifetime Comm</div><div class="b fs14 purple-t">${inr(comm)}</div></div>
        <button class="btn ghost sm" data-act="openUser" data-id="${esc(u.id)}">View Profile</button>
      </div>
    </div>`;
  }).join(''));
};