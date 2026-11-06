'use strict';
/* ============================================================
   STATE
   ============================================================ */
const S = {
  users: [], tasks: [], subs: [], wds: [], bcasts: [], logs: [], meta: {},
  live: {}, errs: {}, admin: '', view: 'overview',
  ov: { apprTab: '24h', wdTab: 'pending', chart: 'month', q: '' },
  appr: { range: '24h', status: 'pending', from: '', to: '', q: '', groups: 6, open: {}, items: {}, folder: null, fmeta: {}, fs: 'pending', fq: '', fitems: 24 },
  sel: new Set(),
  tasksUI: { q: '', status: 'all', page: 1, day: 'today', date: '' },
  cm: { tab: 'bank', q: '', task: 'all', status: 'all', page: 1, sel: new Set(), blTask: '', folder: null },
  us: { q: '', status: 'all', sort: 'recent', from: '', to: '', page: 1 },
  wd: { tab: 'pending', q: '', method: 'all', month: monthKey(Date.now()), from: '', to: '', page: 1, reveal: new Set() },
  rp: { month: monthKey(Date.now()) },
  lg: { q: '', action: 'all', page: 1 },
  tm: { showCompleted: true },
  drawer: { uid: null, tab: 'subs', ledger: null }
};
let busy = false;
