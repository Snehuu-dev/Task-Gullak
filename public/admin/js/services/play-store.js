'use strict';
/* ============================================================
   PLAY STORE FETCH  (name, icon, rating, downloads from a Play Store link)
   Set PLAY_PROXY to your own Cloudflare Worker (tools/play-proxy-worker.js) for a stable fetch.
   ============================================================ */
/* Optional but the most reliable option: deploy play-proxy-worker.js on Cloudflare Workers (free) and paste its URL here, e.g. 'https://play-proxy.yourname.workers.dev' */
const PLAY_PROXY = '';
async function fetchHtml(target, pkg) {
  const enc = encodeURIComponent, titleOf = h => { const m = h.match(/<title[^>]*>([^<]*)/i) || h.match(/^Title:\s*(.+)$/im); return m ? m[1] : ''; };
  const valid = h => typeof h === 'string' && h.length > 1200 && !/just a moment|attention required|access denied|captcha|before you continue|error 4\d\d|403 forbidden|too many requests/i.test(titleOf(h)) && (h.includes(pkg) || /og:title/i.test(h)) && /google play|og:title/i.test(h);
  const get = async (url, kind, ms) => {
    const c = new AbortController(), t = setTimeout(() => c.abort(), ms);
    try {
      const r = await fetch(url, { signal: c.signal }); if (!r.ok) throw new Error('http ' + r.status);
      const h = kind === 'json' ? ((await r.json()).contents || '') : await r.text();
      if (!valid(h)) throw new Error('bad page'); return h;
    } finally { clearTimeout(t); }
  };
  if (PLAY_PROXY) { try { return await get(PLAY_PROXY + (PLAY_PROXY.includes('?') ? '&' : '?') + 'url=' + enc(target), 'text', 12000); } catch (_) { } }
  const list = [
    [u => 'https://corsproxy.io/?url=' + enc(u)],
    [u => 'https://api.allorigins.win/get?url=' + enc(u), 'json'],
    [u => 'https://api.codetabs.com/v1/proxy/?quest=' + enc(u)],
    [u => 'https://api.allorigins.win/raw?url=' + enc(u)],
    [u => 'https://thingproxy.freeboard.io/fetch/' + u],
    [u => 'https://r.jina.ai/' + u]   // reader service: CORS-enabled, returns page text (parsed by parsePlay too)
  ];
  for (const ms of [10000, 18000]) {   // two rounds: all proxies in parallel, first valid page wins
    try { return await Promise.any(list.map(([f, k]) => get(f(target), k, ms))); } catch (_) { }
  }
  throw new Error('proxy');
}
function parsePlay(html, pkg) {
  const meta = k => { const m = html.match(new RegExp('<meta[^>]+(?:property|name|itemprop)="' + k + '"[^>]+content="([^"]*)"', 'i')) || html.match(new RegExp('<meta[^>]+content="([^"]*)"[^>]+(?:property|name|itemprop)="' + k + '"', 'i')); return m ? m[1] : ''; };
  const dec = x => { const d = document.createElement('textarea'); d.innerHTML = x; return d.value; };
  const clean = x => dec(x).replace(/\s*[-–]\s*Apps on Google Play.*$/i, '').replace(/\s*-\s*Google Play.*$/i, '').trim();
  const compact = n => { n = +n; return n >= 1e9 ? (n / 1e9) + 'B+' : n >= 1e6 ? (n / 1e6) + 'M+' : n >= 1e3 ? (n / 1e3) + 'K+' : n + '+'; };
  let ld = {}; try { const m = html.match(/<script[^>]+application\/ld\+json[^>]*>([\s\S]*?)<\/script>/i); if (m) ld = JSON.parse(m[1]); } catch (_) { }
  let appName = clean(meta('og:title') || meta('name') || ld.name || ''); if (!appName) { const m = html.match(/<title[^>]*>([^<]+)<\/title>/i) || html.match(/^Title:\s*(.+)$/im); appName = m ? clean(m[1]) : ''; }
  let img = meta('og:image') || ld.image || ''; if (typeof img !== 'string') img = (img && img.url) || '';
  if (!img) { const m = html.match(/https:\/\/play-lh\.googleusercontent\.com\/[A-Za-z0-9_\-]+/); img = m ? m[0] : ''; }
  let rating = ld.aggregateRating && ld.aggregateRating.ratingValue ? String(ld.aggregateRating.ratingValue) : '';
  if (!rating) { const rm = html.match(/Rated ([0-9.]+) stars/i) || html.match(/"ratingValue"\s*:\s*"?([0-9.]+)/i) || html.match(/itemprop="ratingValue"[^>]*content="([0-9.]+)"/i) || html.match(/>\s*([0-5]\.[0-9])\s*(?:<[^>]*>\s*)*star/i) || html.match(/\b([0-5]\.[0-9])\s*star/i); rating = rm ? rm[1] : ''; }
  let downloads = ''; const dm = html.match(/>\s*([0-9][0-9.,]*\s?[KMBkmb]\+?)\s*<\/div>\s*<div[^>]*>\s*Downloads/i) || html.match(/([0-9][0-9.,]*\s?[KMB]\+)\s*(?:<[^>]*>\s*)*Downloads/i);
  if (dm) downloads = dm[1].replace(/\s/g, '').toUpperCase(); else { const n = html.match(/"([0-9]{1,3}(?:,[0-9]{3})+)\+"/) || html.match(/"(?:numDownloads|installs)"\s*:\s*"?([0-9,]+)/i); if (n) downloads = compact(n[1].replace(/,/g, '')); else { const k = html.match(/"([0-9][0-9.]*[KMB]\+)"/); if (k) downloads = k[1]; } }
  const cat = ld.applicationCategory ? String(ld.applicationCategory).replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()) : '';
  const nice = pkg.split('.').pop().replace(/[_-]+/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  return { appName: appName || nice, iconUrl: img ? img.split('=')[0] + '=s256-rw' : '', rating: rating ? parseFloat(rating).toFixed(1) : '', downloads, category: cat };
}
let metaTok = 0;
async function fetchPlayMeta() {
  const raw = $('#tm-url').value.trim(), st = $('#tm-fetch-status'); if (!raw) return;
  const pkg = pkgOf(raw), tok = ++metaTok;
  if (!pkg) { st.textContent = 'This does not look like a valid Play Store link — enter the details manually.'; return; }
  const link = `https://play.google.com/store/apps/details?id=${pkg}`;
  if (!/play\.google\.com\/store\/apps\/details/.test(raw)) $('#tm-url').value = link;
  st.textContent = '⚡ Fetching name, icon, rating & downloads…';
  try {
    const html = await fetchHtml(link + '&hl=en&gl=IN', pkg);
    if (tok !== metaTok) return;
    const { appName, iconUrl, rating, downloads, category } = parsePlay(html, pkg);
    if (category && !$('#tm-category').value.trim()) $('#tm-category').value = category;
    if (appName) $('#tm-title').value = appName;
    $('#tm-rating').value = rating || $('#tm-rating').value; $('#tm-downloads').value = downloads || $('#tm-downloads').value;
    if (iconUrl) { $('#tm-icon-url').value = iconUrl; $('#tm-icon-preview').src = iconUrl; }
    const miss = [!iconUrl && 'icon', !rating && 'rating', !downloads && 'downloads'].filter(Boolean);
    st.textContent = `✓ Fetched: ${appName} (${rating || '—'}★ • ${downloads || '—'})` + (miss.length ? ` — ${miss.join(', ')} not found, please fill manually.` : '');
    refreshCompleted(); updateTaskPreview();
  } catch (e) {
    if (tok !== metaTok) return;
    if (!$('#tm-title').value) $('#tm-title').value = pkg.split('.').pop().replace(/[_-]+/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
    st.textContent = 'Could not reach the Play Store through the public proxies. Tap Fetch to retry, or enter the details manually. (For a permanent fix, deploy play-proxy-worker.js and set PLAY_PROXY.)'; updateTaskPreview();
  }
}
