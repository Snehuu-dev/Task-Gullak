// USER-SIDE Task Details page — nothing hardcoded, everything comes from Firestore tasks/{taskId}
// (the same document the Admin Panel writes: title/appName, iconUrl, storeUrl, packageName,
//  rewardInr, durationMinutes, appRating, appDownloads, instructions[]).
import { doc, onSnapshot } from "firebase/firestore";   // use your existing db instance

const $ = s => document.querySelector(s);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

export function mountTaskDetails(db, taskId) {
  $("#td-body").innerHTML = '<div class="skel" style="height:220px"></div>';           // loading only until first snapshot
  return onSnapshot(doc(db, "tasks", taskId), snap => {
    if (!snap.exists()) { $("#td-body").innerHTML = "<p>This task is no longer available.</p>"; return; }
    const t = { id: snap.id, ...snap.data() };
    const name = t.title || t.appName || "App";
    const icon = t.iconUrl || t.appIcon || t.taskPfpBase64 || "";
    const store = t.storeUrl || t.actionUrl || t.appLink ||
                  (t.packageName ? `https://play.google.com/store/apps/details?id=${encodeURIComponent(t.packageName)}` : "#");
    // Admin writes instructions as plain lines. "{app}" (if used) is replaced by the live app name.
    const steps = (t.instructions || []).map(l => esc(l).replaceAll("{app}", esc(name)));

    document.title = name;
    $("#td-body").innerHTML = `
      <header class="td-head">
        ${icon ? `<img class="td-icon" src="${esc(icon)}" alt="">` : ""}
        <div>
          <h1>${esc(name)}</h1>
          <p>${esc(t.category || "App")} • ₹${Number(t.rewardInr ?? t.rewardAmount ?? 0)} reward • ${Number(t.durationMinutes) || 15} min</p>
          <p>★ ${esc(t.appRating || t.rating || "—")} • ${esc(t.appDownloads || t.downloads || "—")} downloads</p>
        </div>
      </header>
      <a class="td-install" href="${esc(store)}" target="_blank" rel="noopener">Install this app</a>
      <h2>Task Details – Instructions</h2>
      <ol>${steps.map(s => `<li>${s}</li>`).join("")}</ol>`;
  }, err => { $("#td-body").innerHTML = `<p>Could not load task (${esc(err.code || err.message)}).</p>`; });
}
// usage:  const off = mountTaskDetails(db, taskIdFromRoute);   // call off() when leaving the page
