// Cloudflare Worker — free CORS proxy only for Google Play app pages.
// Deploy: dash.cloudflare.com → Workers & Pages → Create Worker → paste this → Deploy.
// Then put the worker URL in index.html:  const PLAY_PROXY = 'https://YOUR-WORKER.workers.dev';
export default {
  async fetch(request) {
    const cors = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, OPTIONS', 'Access-Control-Allow-Headers': '*' };
    if (request.method === 'OPTIONS') return new Response(null, { headers: cors });
    const target = new URL(request.url).searchParams.get('url') || '';
    let u; try { u = new URL(target); } catch { return new Response('bad url', { status: 400, headers: cors }); }
    if (u.hostname !== 'play.google.com' || !u.pathname.startsWith('/store/apps/details'))
      return new Response('only Google Play app pages are allowed', { status: 403, headers: cors });
    const r = await fetch(u.toString(), { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36', 'Accept-Language': 'en-IN,en;q=0.9' }, cf: { cacheTtl: 3600, cacheEverything: true } });
    return new Response(await r.text(), { status: r.status, headers: { ...cors, 'Content-Type': 'text/html; charset=utf-8' } });
  }
};
