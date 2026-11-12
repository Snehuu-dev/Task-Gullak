'use strict';
/* === REFERRAL MANAGEMENT VIEW === */
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