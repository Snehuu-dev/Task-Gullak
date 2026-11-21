/* =========================================================
   TASK GULLAK — INTERACTIVE & ANIMATION ENGINE (v4)
   ========================================================= */
document.addEventListener('DOMContentLoaded', () => {
  // 1. Dynamic Footer Year
  document.querySelectorAll('[data-year]').forEach(el => {
    el.textContent = new Date().getFullYear();
  });

  // 2. Mobile Navigation Drawer
  const menuBtn = document.querySelector('.menu-btn');
  const navLinks = document.querySelector('.navlinks');
  if (menuBtn && navLinks) {
    menuBtn.addEventListener('click', () => {
      navLinks.classList.toggle('open');
      menuBtn.textContent = navLinks.classList.contains('open') ? '✕' : '☰';
    });
  }

  // 3. Scroll Reveal Observer
  const revealEls = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    const obs = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          obs.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });
    revealEls.forEach(el => obs.observe(el));
  } else {
    revealEls.forEach(el => el.classList.add('visible'));
  }

  // 4. Toast Helper
  function showToast(msg) {
    let toast = document.querySelector('.tg-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.className = 'tg-toast';
      document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.classList.add('show');
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => toast.classList.remove('show'), 2800);
  }

  // 5. Particle Burst Helper
  function spawnCoinBurst(x, y, label = '+₹50') {
    const items = ['₹', '✨', label, '🪙'];
    for (let i = 0; i < 8; i++) {
      const p = document.createElement('span');
      p.className = 'coin-particle';
      p.textContent = items[i % items.length];
      const angle = (Math.PI * 2 * i) / 8;
      const dist = 48 + Math.random() * 44;
      p.style.left = `${x}px`;
      p.style.top = `${y}px`;
      p.style.setProperty('--dx', `${Math.cos(angle) * dist}px`);
      p.style.setProperty('--dy', `${Math.sin(angle) * dist - 24}px`);
      document.body.appendChild(p);
      setTimeout(() => p.remove(), 880);
    }
  }

  // 6. Hero 3D Tilt & Interactive Wallet Coin Drop
  const heroStage = document.getElementById('hero-stage');
  const tiltTarget = document.getElementById('tilt-target');
  if (heroStage && tiltTarget) {
    heroStage.addEventListener('mousemove', (e) => {
      const rect = heroStage.getBoundingClientRect();
      const nx = (e.clientX - rect.left) / rect.width - 0.5;
      const ny = (e.clientY - rect.top) / rect.height - 0.5;
      tiltTarget.style.transform = `rotateY(${nx * 16}deg) rotateX(${-ny * 14}deg)`;
    });
    heroStage.addEventListener('mouseleave', () => {
      tiltTarget.style.transform = 'rotateY(0deg) rotateX(0deg)';
    });
  }

  let currentBalance = 546;
  const balanceEl = document.getElementById('wallet-balance-val');
  const earnedEl = document.getElementById('stat-earned-val');
  const maxMetaEl = document.getElementById('withdrawal-max-val');

  function addRewardToWallet(amount, x, y) {
    if (!balanceEl) return;
    const startBal = currentBalance;
    currentBalance += amount;
    const totalEarned = currentBalance + 2040;
    const startTime = performance.now();
    const duration = 420;

    balanceEl.classList.add('bump');
    setTimeout(() => balanceEl.classList.remove('bump'), 260);

    function step(now) {
      const progress = Math.min((now - startTime) / duration, 1);
      const val = Math.floor(startBal + (currentBalance - startBal) * progress);
      balanceEl.textContent = `₹${val.toLocaleString('en-IN')}.00`;
      if (earnedEl) earnedEl.textContent = `₹${(val + 2040).toLocaleString('en-IN')}`;
      if (maxMetaEl) maxMetaEl.textContent = `₹${val.toLocaleString('en-IN')}`;
      if (progress < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);

    if (typeof x === 'number' && typeof y === 'number') {
      spawnCoinBurst(x, y, `+₹${amount}`);
    }
    showToast(`🪙 +₹${amount} reward added to your Task Gullak Wallet!`);
  }

  document.querySelectorAll('[data-coin-trigger]').forEach(el => {
    el.addEventListener('click', (e) => {
      const amt = parseInt(el.getAttribute('data-coin-trigger') || '50', 10);
      addRewardToWallet(amt, e.clientX, e.clientY);
    });
  });

  // Copy Referral Code in Wallet Mockup
  const copyRefBtn = document.getElementById('copy-ref-btn');
  if (copyRefBtn) {
    copyRefBtn.addEventListener('click', () => {
      if (navigator.clipboard) {
        navigator.clipboard.writeText('TG-ZZKJIBOW').catch(() => {});
      }
      showToast('📋 Referral code TG-ZZKJIBOW copied!');
    });
  }

  // 7. FAQ Accordion
  document.querySelectorAll('.faq-question').forEach(btn => {
    btn.addEventListener('click', () => {
      const item = btn.closest('.faq-item');
      const isOpen = item.classList.contains('open');
      document.querySelectorAll('.faq-item').forEach(i => i.classList.remove('open'));
      if (!isOpen) item.classList.add('open');
    });
  });

  // 8. Support Page Form & Email Copy
  const supportForm = document.getElementById('support-form');
  if (supportForm) {
    supportForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const nameInput = supportForm.querySelector('#name');
      const userName = nameInput && nameInput.value.trim() ? nameInput.value.trim() : 'Friend';
      showToast(`✅ Thanks ${userName}! Your message has been sent to Task Gullak Support.`);
      supportForm.reset();
    });
  }

  document.querySelectorAll('[data-copy-email]').forEach(card => {
    card.addEventListener('click', () => {
      const email = card.getAttribute('data-copy-email');
      if (navigator.clipboard && email) {
        navigator.clipboard.writeText(email).catch(() => {});
      }
      showToast(`✉️ Copied ${email} to clipboard!`);
    });
  });

  // 9. Zero-Gravity Coins on 404 Page
  document.querySelectorAll('.zero-g-coin').forEach(coin => {
    coin.addEventListener('click', (e) => {
      spawnCoinBurst(e.clientX, e.clientY, '404 ₹');
      showToast('🪙 Oops! That coin drifted off-route — head back Home!');
    });
  });
});
