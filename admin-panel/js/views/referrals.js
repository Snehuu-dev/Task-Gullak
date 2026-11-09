'use strict';
/* === REFERRAL MANAGEMENT VIEW === */
ACT.fixReferralBalances = async () => {
  if (!await confirmBox('Sync All Missing Referral Earnings?', 'This will scan ALL users, calculate missing 1% commissions and ₹5 bonuses based on their friends\' approved tasks, and credit the missing amounts to their wallets. It checks transaction history to prevent double-crediting.')) return;
  busy = true; renderAll();
  try {
    let totalFixed = 0, totalMoney = 0;
    for (const u of S.users) {
      const friends = S.users.filter(x => x.referredByUserId === u.id || (u.referralCode && x.referredByCode && x.referredByCode.toUpperCase() === u.referralCode.toUpperCase()));

      let expectedComm = 0;
      friends.forEach(f => {
        S.subs.filter(s => s.userId === f.id && subSt(s) === 'approved').forEach(s => {
          expectedComm += Math.max(1, Math.round(num(s.rewardAmount) * 0.01));
        });
      });

      let expectedBonus = num(u.totalReferralBonusEarned) || 0;
      if (expectedBonus === 0 && friends.length > 0) expectedBonus = friends.length * 5;
      else expectedBonus = Math.max(expectedBonus, friends.length * 5);

      if (expectedComm === 0 && expectedBonus === 0) continue;

      const txs = await getDocs(collection(db, 'users', u.id, 'transactions'));
      let creditedComm = 0, creditedBonus = 0;
      txs.forEach(d => {
        const t = d.data();
        if (t.type === 'referral_commission') creditedComm += num(t.amount);
        if (t.type === 'referral_bonus' || t.type === 'referral_login_bonus') creditedBonus += num(t.amount);
      });

      const missingComm = Math.max(0, expectedComm - creditedComm);
      const missingBonus = Math.max(0, expectedBonus - creditedBonus);

      if (missingComm > 0 || missingBonus > 0) {
        await runTransaction(db, async tx => {
          const uRef = doc(db, 'users', u.id);
          const uSnap = await tx.get(uRef);
          if (!uSnap.exists()) return;
          tx.update(uRef, {
            walletBalance: num(uSnap.data().walletBalance) + missingComm + missingBonus,
            totalLifetimeCommissionEarned: expectedComm,
            referralCommissionEarned: expectedComm,
            totalReferralBonusEarned: expectedBonus,
            updatedAt: serverTimestamp()
          });
          if (missingComm > 0) {
            tx.set(doc(collection(db, 'users', u.id, 'transactions')), {
              userId: u.id, amount: missingComm, type: 'referral_commission',
              note: `Retroactive 1% Lifetime Comm Sync (+₹${missingComm})`, createdAt: serverTimestamp()
            });
          }
          if (missingBonus > 0) {
            tx.set(doc(collection(db, 'users', u.id, 'transactions')), {
              userId: u.id, amount: missingBonus, type: 'referral_bonus',
              note: `Retroactive Referral Bonus Sync (+₹${missingBonus})`, createdAt: serverTimestamp()
            });
          }
        });
        totalFixed++; totalMoney += (missingComm + missingBonus);
      }
    }
    toast(`Success! Credited ₹${totalMoney} to ${totalFixed} users.`, 'ok');
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
    const referredCount = users.filter(x => x.referredByUserId === u.id || (u.referralCode && x.referredByCode === u.referralCode)).length;
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