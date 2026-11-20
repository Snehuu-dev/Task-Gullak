// Task Gullak Main Website Scripts & Animations
document.addEventListener('DOMContentLoaded', () => {
  // Mobile Navigation Toggle
  const menuBtn = document.querySelector('.menu-btn');
  const navLinks = document.querySelector('.navlinks');

  if (menuBtn && navLinks) {
    menuBtn.addEventListener('click', () => {
      navLinks.classList.toggle('show');
      menuBtn.setAttribute('aria-expanded', navLinks.classList.contains('show'));
    });

    // Close mobile nav when clicking a link
    navLinks.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        navLinks.classList.remove('show');
      });
    });
  }

  // Active Link Highlighting
  const currentPath = window.location.pathname;
  document.querySelectorAll('.navlinks a').forEach(link => {
    const href = link.getAttribute('href');
    if (href === currentPath || (href !== '/' && currentPath.endsWith(href))) {
      link.classList.add('active');
    }
  });

  // IntersectionObserver for Reveal Animations
  const revealElements = document.querySelectorAll('.reveal');
  const observerOptions = {
    root: null,
    rootMargin: '0px 0px -50px 0px',
    threshold: 0.12
  };

  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, observerOptions);

  revealElements.forEach(el => revealObserver.observe(el));

  // 3D Parallax Mouse Tracking Effect for Hero Artwork & 404 Stage
  const heroArt = document.querySelector('.hero-art-container');
  if (heroArt) {
    const phone = heroArt.querySelector('.phone-mockup');
    const piggy = heroArt.querySelector('.piggy-3d');
    const coins = heroArt.querySelectorAll('.coin-3d');

    window.addEventListener('mousemove', e => {
      const { innerWidth, innerHeight } = window;
      const x = (e.clientX / innerWidth - 0.5) * 2; // -1 to 1
      const y = (e.clientY / innerHeight - 0.5) * 2;

      if (phone) {
        phone.style.transform = `rotate(${5 + x * 6}deg) rotateX(${4 - y * 8}deg) rotateY(${-4 + x * 8}deg) translate3d(${x * 12}px, ${y * 12}px, 0)`;
      }

      if (piggy) {
        piggy.style.transform = `translate3d(${-x * 20}px, ${-y * 20}px, 0) scale(${1 + Math.abs(x) * 0.05})`;
      }

      coins.forEach((coin, idx) => {
        const factor = (idx + 1) * 10;
        coin.style.transform = `translate3d(${x * factor}px, ${y * factor}px, 0) rotateY(${x * 180}deg)`;
      });
    });
  }

  // 404 Page Interactive 3D Tilt Effect
  const notfoundStage = document.querySelector('.notfound-stage');
  if (notfoundStage) {
    const card404 = notfoundStage.querySelector('.card-404-badge');
    const piggy404 = notfoundStage.querySelector('.piggy-404');

    window.addEventListener('mousemove', e => {
      const { innerWidth, innerHeight } = window;
      const x = (e.clientX / innerWidth - 0.5) * 2;
      const y = (e.clientY / innerHeight - 0.5) * 2;

      if (card404) {
        card404.style.transform = `rotate(${-6 + x * 8}deg) rotateX(${-y * 10}deg) rotateY(${x * 10}deg) translate3d(${x * 15}px, ${y * 15}px, 0)`;
      }

      if (piggy404) {
        piggy404.style.transform = `translate3d(${-x * 18}px, ${-y * 18}px, 0)`;
      }
    });
  }

  // FAQ Accordions Toggle
  const faqQuestions = document.querySelectorAll('.faq-question');
  faqQuestions.forEach(btn => {
    btn.addEventListener('click', () => {
      const currentItem = btn.closest('.faq-item');
      const isOpen = currentItem.classList.contains('open');

      // Close all accordion items for clean accordion behavior
      document.querySelectorAll('.faq-item').forEach(item => {
        item.classList.remove('open');
      });

      // Toggle current
      if (!isOpen) {
        currentItem.classList.add('open');
      }
    });
  });

  // Dynamic Copyright Year
  document.querySelectorAll('[data-year]').forEach(el => {
    el.textContent = new Date().getFullYear();
  });

  // App Link Download Handler
  const APP_DOWNLOAD_URL = 'APP_DOWNLOAD_URL_HERE';
  document.querySelectorAll('[data-app-link]').forEach(link => {
    link.href = APP_DOWNLOAD_URL === 'APP_DOWNLOAD_URL_HERE' ? '#' : APP_DOWNLOAD_URL;
    if (APP_DOWNLOAD_URL === 'APP_DOWNLOAD_URL_HERE') {
      link.addEventListener('click', e => {
        e.preventDefault();
        alert('App download link will be available soon! Check back shortly.');
      });
    }
  });

  // Support Form Handler
  const supportForm = document.getElementById('support-form');
  if (supportForm) {
    supportForm.addEventListener('submit', e => {
      e.preventDefault();
      const data = new FormData(supportForm);
      const name = data.get('name') || '';
      const email = data.get('email') || '';
      const message = data.get('message') || '';

      const subject = encodeURIComponent('Task Gullak Support Request from ' + name);
      const body = encodeURIComponent(`Name: ${name}\nEmail: ${email}\n\nMessage:\n${message}`);

      window.location.href = `mailto:support@taskgullak.space?subject=${subject}&body=${body}`;
    });
  }
});
