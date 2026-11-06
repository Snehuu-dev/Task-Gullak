'use strict';
/* ============================================================
   TOAST + DIALOGS
   ============================================================ */
function toast(msg, kind = '') {
  const box = $('#toasts'); if (!box) return;
  const el = document.createElement('div');
  el.className = 'toast ' + kind; el.textContent = msg; box.appendChild(el);
  setTimeout(() => el.remove(), kind === 'bad' ? 6000 : 3800);
}

/** Generic dialog. fields: [{id,label,type,value,options,required,placeholder,hint}]. Resolves with values object / true / null. */
function ask({ title, body = '', ok = 'Confirm', cancel = 'Cancel', danger = false, fields = [] }) {
  return new Promise(resolve => {
    const ov = $('#dlg-overlay'), box = $('#dlg');
    const f = fields.map(x => {
      let input;
      if (x.type === 'textarea') input = `<textarea class="in" id="dlg-${x.id}" rows="3" placeholder="${esc(x.placeholder || '')}">${esc(x.value || '')}</textarea>`;
      else if (x.type === 'select') input = `<select class="in" id="dlg-${x.id}">${x.options.map(o => `<option value="${esc(o.v ?? o)}"${(o.v ?? o) === x.value ? ' selected' : ''}>${esc(o.t ?? o)}</option>`).join('')}</select>`;
      else input = `<input class="in" id="dlg-${x.id}" type="${x.type || 'text'}" value="${esc(x.value ?? '')}" placeholder="${esc(x.placeholder || '')}" ${x.min != null ? `min="${x.min}"` : ''}>`;
      return `<div class="field"><label class="lbl" for="dlg-${x.id}">${esc(x.label)}${x.required ? ' *' : ''}</label>${input}${x.hint ? `<div class="hint">${esc(x.hint)}</div>` : ''}</div>`;
    }).join('');
    box.innerHTML = `<h3>${esc(title)}</h3><div class="body">${body}</div>${f}<div class="err" id="dlg-err"></div>
      <div class="row" style="justify-content:flex-end;margin-top:14px"><button class="btn ghost" id="dlg-cancel">${esc(cancel)}</button><button class="btn ${danger ? 'bad' : 'primary'}" id="dlg-ok">${esc(ok)}</button></div>`;
    ov.classList.add('show');
    const done = v => { ov.classList.remove('show'); ov._cancel = null; resolve(v); };
    ov._cancel = () => done(null);
    $('#dlg-cancel').onclick = () => done(null);
    $('#dlg-ok').onclick = () => {
      const vals = {};
      for (const x of fields) {
        const v = $('#dlg-' + x.id).value.trim();
        if (x.required && !v) { $('#dlg-err').textContent = `${x.label} is required.`; $('#dlg-' + x.id).focus(); return; }
        vals[x.id] = v;
      }
      done(fields.length ? vals : true);
    };
    const first = fields.length ? $('#dlg-' + fields[0].id) : $('#dlg-ok');
    setTimeout(() => first && first.focus(), 30);
    box.onkeydown = e => { if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') { e.preventDefault(); $('#dlg-ok').click(); } };
  });
}
const confirmBox = (title, body, opts = {}) => ask({ title, body: `<p>${body}</p>`, ...opts });
