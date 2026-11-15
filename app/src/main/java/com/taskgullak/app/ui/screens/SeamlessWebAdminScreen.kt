package com.taskgullak.app.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.OutputStream

private const val ADMIN_WEB_URL = "https://taskgullak.space/admin#pass.Gullak#apk.@2026"
private const val LOCAL_FALLBACK_ASSET_URL = "file:///android_asset/index.html#pass.Gullak#apk.@2026"

class NativeAdminBridge(private val context: Context) {

    @JavascriptInterface
    fun saveBase64File(base64Data: String, fileName: String, mimeType: String) {
        try {
            val cleanBase64 = base64Data.substringAfter("base64,", base64Data)
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val safeFileName = fileName.ifBlank { "TaskGullak_${System.currentTimeMillis()}.png" }
            val resolvedMime = mimeType.ifBlank {
                if (safeFileName.endsWith(".zip", ignoreCase = true)) "application/zip" else "image/png"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Downloads.MIME_TYPE, resolvedMime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TaskGullakAdmin")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out: OutputStream ->
                        out.write(bytes)
                        out.flush()
                    }
                    Toast.makeText(
                        context,
                        "Saved to Downloads/TaskGullakAdmin/$safeFileName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outFile = java.io.File(dir, safeFileName)
                outFile.writeBytes(bytes)
                Toast.makeText(
                    context,
                    "Saved: ${outFile.absolutePath}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Download failed: ${e.localizedMessage ?: "error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SeamlessWebAdminScreen(
    onSwitchToNativeBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(15) }
    var hasNetworkError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback
        filePathCallback = null
        if (callback != null) {
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            callback.onReceiveValue(uris)
        }
    }

    BackHandler(enabled = true) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
        }
    }

    // Native container matching the web admin console with zero URL bar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEF0FA))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("seamless_web_admin_container")
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#EEF0FA"))

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        loadsImagesAutomatically = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        mediaPlaybackRequiresUserGesture = false
                        // Clean mobile app user agent without WebView restrictions
                        userAgentString = userAgentString.replace("; wv", "")
                    }

                    addJavascriptInterface(NativeAdminBridge(ctx), "AndroidAdminBridge")

                    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        try {
                            if (url.startsWith("data:")) {
                                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                NativeAdminBridge(ctx).saveBase64File(url, fileName, mimeType ?: "image/png")
                            } else if (url.startsWith("http://") || url.startsWith("https://")) {
                                val request = DownloadManager.Request(Uri.parse(url)).apply {
                                    setMimeType(mimeType)
                                    addRequestHeader("User-Agent", userAgent)
                                    setDescription("Downloading Task Gullak proof...")
                                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                    setTitle(fileName)
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                }
                                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.enqueue(request)
                                Toast.makeText(ctx, "Downloading $url...", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Download started", Toast.LENGTH_SHORT).show()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (newProgress >= 90) {
                                isLoading = false
                            }
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallbackParam: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = filePathCallbackParam
                            return try {
                                val intent = fileChooserParams?.createIntent()
                                    ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "image/*"
                                    }
                                fileChooserLauncher.launch(intent)
                                true
                            } catch (e: Exception) {
                                filePathCallback = null
                                false
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            hasNetworkError = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            // Auto-unlock the Admin Panel using #pass.Gullak#apk.@2026 & hook blob/data downloads
                            val autoUnlockAndBridgeJs = """
                                (function() {
                                  try {
                                    try { localStorage.setItem('tg_admin_session_v2', 'snehkumarverma20111@gmail.com'); } catch (_) {}
                                    function tryAutoUnlock() {
                                      if (typeof openAdminConsole === 'function') {
                                        openAdminConsole('snehkumarverma20111@gmail.com');
                                        return true;
                                      }
                                      if (typeof checkUrlHashAutoLogin === 'function') {
                                        checkUrlHashAutoLogin();
                                        return true;
                                      }
                                      var passInput = document.getElementById('admin-pass-input');
                                      var emailInput = document.getElementById('admin-email-input');
                                      var loginScreen = document.getElementById('login-screen');
                                      if (emailInput && !emailInput.value) {
                                        emailInput.value = 'snehkumarverma20111@gmail.com';
                                      }
                                      if (passInput) {
                                        passInput.value = 'GULLAK_ADMIN_2025';
                                      }
                                      if (typeof window.unlockAdmin === 'function' && loginScreen && !loginScreen.classList.contains('hidden')) {
                                        window.unlockAdmin();
                                        return true;
                                      }
                                      return false;
                                    }
                                    if (!tryAutoUnlock()) {
                                      setTimeout(tryAutoUnlock, 300);
                                      setTimeout(tryAutoUnlock, 900);
                                      setTimeout(tryAutoUnlock, 1800);
                                    }

                                    // Auto-close mobile sidebar drawer when clicking any nav item
                                    if (!window.__tgDrawerAutoCloseHooked) {
                                      window.__tgDrawerAutoCloseHooked = true;
                                      document.addEventListener('click', function(e) {
                                        var navItem = e.target.closest('a, button, .nav-item, .sidebar-item, [data-act], [data-view]');
                                        if (navItem && !navItem.classList.contains('menu-toggle') && !navItem.closest('.menu-toggle')) {
                                          var sidebar = document.querySelector('.sidebar, .drawer, #sidebar, .nav-drawer, aside, .sidebar-drawer');
                                          if (sidebar && (sidebar.classList.contains('open') || sidebar.classList.contains('active') || sidebar.classList.contains('show'))) {
                                            sidebar.classList.remove('open', 'active', 'show');
                                          }
                                        }
                                      });

                                      var styleEl = document.createElement('style');
                                      styleEl.innerHTML = '#bk-tabs { display: flex !important; flex-wrap: wrap !important; gap: 8px !important; margin: 12px 0 !important; padding: 6px 0 !important; border-bottom: 2px solid rgba(255,255,255,0.1) !important; z-index: 10 !important; } #bk-tabs .tab { padding: 8px 14px !important; font-weight: bold !important; font-size: 12px !important; border-radius: 8px !important; background: rgba(255,255,255,0.08) !important; color: #94a3b8 !important; border: 1px solid rgba(255,255,255,0.15) !important; cursor: pointer !important; } #bk-tabs .tab.on { background: #0EA5E9 !important; color: #ffffff !important; border-color: #0EA5E9 !important; box-shadow: 0 4px 12px rgba(14,165,233,0.3) !important; }';
                                      document.head.appendChild(styleEl);
                                    }
                                      if (typeof RENDER.bulk !== 'function') {
                                        window.bulkRows = function() {
                                          const m = new Map();
                                          const safeC = id => (typeof canon === 'function' ? canon(id) : id);
                                          const findU = uid => {
                                            const c = safeC(uid);
                                            return (S.users || []).find(u => u && (u.id === c || u.id === uid || (u._aliases && u._aliases.includes(uid))));
                                          };
                                          const row = uid => {
                                            const c = safeC(uid);
                                            let r = m.get(c);
                                            if (!r) m.set(c, (r = { uid: c, req: null }));
                                            return r;
                                          };
                                          const reqList = Array.isArray(S.bulkReq) ? S.bulkReq : [];
                                          const modReqList = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];
                                          const userList = Array.isArray(S.users) ? S.users : [];
                                          reqList.forEach(q => {
                                            if (!q) return;
                                            const uid = q.userId || q.uid || q.id;
                                            if (!uid) return;
                                            const r = row(uid);
                                            if (!r.req) r.req = q;
                                          });
                                          modReqList.forEach(q => {
                                            if (!q) return;
                                            const uid = q.userId || q.uid || q.id;
                                            if (!uid) return;
                                            const r = row(uid);
                                            if (!r.req) r.req = q;
                                          });
                                          userList.forEach(u => {
                                            if (!u) return;
                                            const st = String(u.bulkStatus || '').toLowerCase();
                                            if (u.bulkMode === true || ['pending', 'active', 'approved', 'requested', 'rejected'].includes(st)) {
                                              const r = row(u.id);
                                              if (!r.req) {
                                                r.req = {
                                                  id: 'user_' + u.id,
                                                  userId: u.id,
                                                  userName: u.fullName || u.username || u.name || '',
                                                  userEmail: u.email || '',
                                                  note: u.bulkRejectReason || 'Requested via Mobile App',
                                                  status: u.bulkMode === true ? 'approved' : st,
                                                  createdAt: u.updatedAt || u.createdAt
                                                };
                                              }
                                            }
                                          });
                                          return [...m.values()].map(r => {
                                            const u = findU(r.uid), q = r.req;
                                            r.user = u;
                                            r.name = String((u && (u.fullName || u.username || u.name)) || (q && (q.userName || q.name)) || (u && u.email) || (q && q.userEmail) || r.uid || 'User').trim();
                                            r.email = String((u && u.email) || (q && q.userEmail) || '').trim();
                                            const uSt = String((u && u.bulkStatus) || '').toLowerCase();
                                            const qSt = String((q && q.status) || '').toLowerCase();
                                            const isApproved = (u && u.bulkMode === true) || ['active', 'approved'].includes(uSt) || ['active', 'approved'].includes(qSt);
                                            const isRejected = !isApproved && (['rejected', 'revoked'].includes(qSt) || ['rejected', 'revoked'].includes(uSt));
                                            r.status = isApproved ? 'active' : isRejected ? 'rejected' : 'pending';
                                            return r;
                                          });
                                        };
                                        const sendBulkNotification = async (uid, title, message, type) => {
                                          try {
                                            const bRef = doc(collection(db, 'broadcasts'));
                                            const payload = {
                                              id: bRef.id,
                                              title: title,
                                              message: message,
                                              body: message,
                                              description: message,
                                              type: type || 'info',
                                              targetUserId: uid,
                                              userId: uid,
                                              active: true,
                                              isActive: true,
                                              read: false,
                                              isRead: false,
                                              timestamp: Date.now(),
                                              createdAt: serverTimestamp(),
                                              updatedAt: serverTimestamp(),
                                              sentBy: S.admin || 'admin'
                                            };
                                            await setDoc(bRef, payload);
                                            try { await setDoc(doc(db, 'notifications', bRef.id), payload); } catch (_) {}
                                            try { await setDoc(doc(db, 'users', uid, 'notifications', bRef.id), payload); } catch (_) {}
                                          } catch (_) {}
                                        };
                                        ACT.bkTab = el => {
                                          S.bkTab = el.dataset.k;
                                          renderAll();
                                        };
                                        ACT.bkApprove = async el => {
                                          const uid = el.dataset.uid;
                                          try {
                                            await setDoc(doc(db, 'users', uid), { bulkMode: true, bulkStatus: 'approved', bulkApprovedAt: serverTimestamp(), bulkApprovedBy: S.admin, bulkRejectReason: '' }, { merge: true });
                                            await sendBulkNotification(
                                              uid,
                                              'Bulk Mode Activated! 🚀',
                                              'Congratulations! Your Bulk Mode access request has been approved by Admin. You can now select multiple devices/comments at once in the Offers tab.',
                                              'reward'
                                            );
                                            toast('Bulk mode activated & notification sent!', 'ok');
                                          } catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        ACT.bkReject = async el => {
                                          const uid = el.dataset.uid;
                                          const reason = await rejectDialog('Reject Bulk access?', '<p class="fs12 muted">Bulk mode stays hidden for this user.</p>');
                                          if (!reason) return;
                                          try {
                                            await setDoc(doc(db, 'users', uid), { bulkMode: false, bulkStatus: 'rejected', bulkRejectReason: reason }, { merge: true });
                                            await sendBulkNotification(
                                              uid,
                                              'Bulk Mode Request Rejected',
                                              'Your Bulk Mode access request was not approved. Reason: ' + reason,
                                              'alert'
                                            );
                                            toast('Rejected & notification sent!', 'ok');
                                          } catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        ACT.bkRevoke = async el => {
                                          const uid = el.dataset.uid;
                                          try {
                                            await setDoc(doc(db, 'users', uid), { bulkMode: false, bulkStatus: 'rejected' }, { merge: true });
                                            await sendBulkNotification(
                                              uid,
                                              'Bulk Mode Turned Off',
                                              'Your Bulk Mode access has been turned off by Admin. Your account is now in Normal Mode.',
                                              'warning'
                                            );
                                            toast('Bulk mode turned off & user notified', 'ok');
                                          } catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        ACT.bkSetLimit = async el => {
                                          const uid = el.dataset.uid, r = (window.bulkRows ? window.bulkRows() : []).find(x => x.uid === uid);
                                          const currentLimit = r && r.user ? (r.user.bulkDeviceCount || 2) : 2;
                                          const v = await ask({
                                            title: 'Set Device Limit for ' + (r ? r.name : 'User'),
                                            body: 'Current Limit: <b>' + currentLimit + ' Devices</b>.<br>Enter new device limit:',
                                            ok: 'Update Limit',
                                            fields: [{ id: 'limit', label: 'New Device Limit', type: 'number', required: true, value: String(currentLimit) }]
                                          });
                                          if (!v) return;
                                          const newLimit = Math.max(1, parseInt(v.limit, 10) || 1);
                                          try {
                                            await setDoc(doc(db, 'users', uid), { bulkDeviceCount: newLimit, bulkApprovedLimit: newLimit }, { merge: true });
                                            await sendBulkNotification(uid, 'Device Limit Updated! 📱', 'Your Bulk Mode device limit has been updated to ' + newLimit + ' devices by Admin.', 'info');
                                            toast('Device limit updated to ' + newLimit + ' for ' + (r ? r.name : 'user'), 'ok');
                                          } catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        ACT.bkApproveLimitAppeal = async el => {
                                          const id = el.dataset.id, ap = (S.bulkAppeals || []).find(x => x.id === id);
                                          if (!ap) return;
                                          const newLimit = ap.requestedDeviceCount || 1;
                                          try {
                                            await setDoc(doc(db, 'users', ap.userId), { bulkDeviceCount: newLimit, bulkApprovedLimit: newLimit }, { merge: true });
                                            await sendBulkNotification(ap.userId, 'Device Limit Approved! 📱', 'Your request to set device limit to ' + newLimit + ' devices has been approved by Admin.', 'reward');
                                            await updateDoc(doc(db, 'bulk_task_appeals', id), { status: 'approved', reviewedBy: S.admin, updatedAt: serverTimestamp() });
                                            toast('Approved device limit of ' + newLimit + ' devices for user!', 'ok');
                                          } catch (e) { toast('Action failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        ACT.bkApproveExitAppeal = async el => {
                                          const id = el.dataset.id, ap = (S.bulkAppeals || []).find(x => x.id === id);
                                          if (!ap) return;
                                          try {
                                            await setDoc(doc(db, 'users', ap.userId), { bulkMode: false, bulkStatus: 'none', bulkRejectReason: 'User requested exit' }, { merge: true });
                                            await sendBulkNotification(ap.userId, 'Bulk Mode Turned Off', 'Your request to exit Bulk Mode has been approved. Your account is now in Normal Mode.', 'info');
                                            await updateDoc(doc(db, 'bulk_task_appeals', id), { status: 'approved', reviewedBy: S.admin, updatedAt: serverTimestamp() });
                                            toast('Exit Bulk Mode request approved. User returned to Normal Mode!', 'ok');
                                          } catch (e) { toast('Action failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        ACT.bkApproveAppeal = async el => {
                                          const id = el.dataset.id, ap = (S.bulkAppeals || []).find(x => x.id === id);
                                          if (!ap) return;
                                          const v = await ask({
                                            title: 'Approve Price Appeal for ' + (ap.userName || ap.userId) + '?',
                                            body: 'Requested Devices: <b>' + (ap.requestedDeviceCount || 1) + '</b> • Requested Reward: <b>₹' + (ap.requestedRewardInr || 0) + '</b><br>Reason: ' + esc(ap.reason || 'No reason specified'),
                                            ok: 'Approve & Credit Wallet',
                                            fields: [{ id: 'rewardInr', label: 'Custom Payout Amount (₹)', type: 'number', required: true, value: String(ap.requestedRewardInr || 50) }]
                                          });
                                          if (!v) return;
                                          const creditAmt = Math.max(1, parseInt(v.rewardInr, 10) || 0);
                                          try {
                                            const userRef = doc(db, 'users', ap.userId);
                                            await runTransaction(db, async tx => {
                                              const snap = await tx.get(userRef);
                                              if (!snap.exists()) throw new Error('User record not found');
                                              const bal = num(snap.data().walletBalance);
                                              tx.update(userRef, { walletBalance: bal + creditAmt, updatedAt: serverTimestamp() });
                                              tx.set(doc(collection(db, 'users', ap.userId, 'transactions')), {
                                                userId: ap.userId,
                                                amount: creditAmt,
                                                type: 'admin_credit',
                                                note: 'Approved Bulk Price Appeal: ' + (ap.taskTitle || 'Task'),
                                                createdAt: serverTimestamp()
                                              });
                                            });
                                            await updateDoc(doc(db, 'bulk_task_appeals', id), {
                                              status: 'approved',
                                              approvedRewardInr: creditAmt,
                                              reviewedBy: S.admin,
                                              updatedAt: serverTimestamp()
                                            });
                                            toast('Appeal approved & ₹' + creditAmt + ' credited to wallet!', 'ok');
                                          } catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        ACT.bkRejectAppeal = async el => {
                                          const id = el.dataset.id;
                                          const reason = await rejectDialog('Reject Bulk Request / Appeal?', '<p class="fs12 muted">The request will be marked rejected.</p>');
                                          if (!reason) return;
                                          try {
                                            await updateDoc(doc(db, 'bulk_task_appeals', id), {
                                              status: 'rejected',
                                              adminNote: reason,
                                              reviewedBy: S.admin,
                                              updatedAt: serverTimestamp()
                                            });
                                            toast('Request rejected', 'ok');
                                          } catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
                                        };
                                        if (!window.__tgAppealsListening) {
                                          window.__tgAppealsListening = true;
                                          try {
                                            listen('bulk_task_appeals', collection(db, 'bulk_task_appeals'), snap => {
                                              S.bulkAppeals = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((x, y) => tsMs(y.createdAt) - tsMs(x.createdAt));
                                              renderAll();
                                            });
                                          } catch (_) {}
                                        }
                                        RENDER.bulk = () => {
                                          const rows = window.bulkRows();
                                          const f = S.bkTab || 'pending';
                                          const by = k => rows.filter(r => r && r.status === k);
                                          const tabBox = document.getElementById('bk-tabs');
                                          if (tabBox) {
                                            const pendingAppealsCount = (S.bulkAppeals || []).filter(a => a.status === 'pending').length;
                                            tabBox.innerHTML = [
                                              ['pending', 'Pending Requests'],
                                              ['active', 'Active Bulk Users (' + by('active').length + ')'],
                                              ['rejected', 'Rejected'],
                                              ['appeals', 'Appeals (' + pendingAppealsCount + ')']
                                            ].map(([k, l]) =>
                                              '<button class="tab ' + (f === k ? 'on' : '') + '" data-act="bkTab" data-k="' + k + '">' + l + '</button>'
                                            ).join('');
                                          }
                                          const listBox = document.getElementById('bk-list');
                                          if (!listBox) return;

                                          if (f === 'appeals') {
                                            const appealsList = S.bulkAppeals || [];
                                            const appealsHTML = appealsList.map(ap => {
                                              const id = esc(ap.id);
                                              const st = String(ap.status || 'pending').toLowerCase();
                                              const reqType = ap.requestType || 'price_increase';
                                              const pill = st === 'approved' ? '<span class="pill ok">Approved</span>' : st === 'rejected' ? '<span class="pill bad">Rejected</span>' : '<span class="pill warn">Pending Appeal</span>';

                                              let titleText = 'Price Increase Appeal';
                                              let detailText = 'Requested Devices: <b>' + (ap.requestedDeviceCount || 1) + '</b> • Requested Reward: <b>₹' + (ap.requestedRewardInr || 0) + '</b>';
                                              let act = '';

                                              if (reqType === 'increase_limit' || reqType === 'decrease_limit') {
                                                titleText = 'Device Limit ' + (reqType === 'increase_limit' ? 'Increase' : 'Decrease') + ' Request';
                                                detailText = 'Requested Device Limit: <b>' + (ap.requestedDeviceCount || 1) + ' Devices</b>';
                                                if (st === 'pending') {
                                                  act = '<button class="btn okfill sm" data-act="bkApproveLimitAppeal" data-id="' + id + '">Approve Device Limit (' + ap.requestedDeviceCount + ')</button><button class="btn bad sm" data-act="bkRejectAppeal" data-id="' + id + '">Reject</button>';
                                                }
                                              } else if (reqType === 'exit_bulk_mode') {
                                                titleText = 'Exit Bulk Mode Request';
                                                detailText = 'User requested to turn off Bulk Mode and revert account to Normal Mode.';
                                                if (st === 'pending') {
                                                  act = '<button class="btn bad sm" data-act="bkApproveExitAppeal" data-id="' + id + '">Approve & Turn Off Bulk Mode</button><button class="btn ghost sm" data-act="bkRejectAppeal" data-id="' + id + '">Reject</button>';
                                                }
                                              } else {
                                                if (st === 'pending') {
                                                  act = '<button class="btn okfill sm" data-act="bkApproveAppeal" data-id="' + id + '">Approve Custom Reward</button><button class="btn bad sm" data-act="bkRejectAppeal" data-id="' + id + '">Reject</button>';
                                                }
                                              }

                                              return '<div class="card"><div class="row between wrap" style="gap:6px"><div><div class="b fs14">' + esc(ap.userName || ap.userEmail || ap.userId) + '</div><div class="fs12 muted">' + esc(ap.userEmail) + ' • <b>' + titleText + '</b></div></div>' + pill + '</div><div class="fs12 mt8" style="background:rgba(14,165,233,0.06);padding:8px 12px;border-radius:8px">' + detailText + '<br>Reason: ' + esc(ap.reason || 'No reason provided') + '</div><div class="row gap6 mt8" style="justify-content:flex-end">' + act + '</div></div>';
                                            }).join('') || ('<div class="card">' + emptyHTML('bulk', 'No appeals or limit change requests submitted yet.') + '</div>');

                                            setHTML(listBox, appealsHTML);
                                            return;
                                          }

                                          const list = by(f);
                                          const pill = {
                                            pending: '<span class="pill warn">Pending</span>',
                                            active: '<span class="pill ok">Bulk Active</span>',
                                            rejected: '<span class="pill bad">Rejected</span>'
                                          };
                                          const summaryHTML = '<div class="g3" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:10px">' +
                                            '<div class="card" style="cursor:pointer;border:' + (f === 'pending' ? '2px solid #F59E0B' : '1px solid var(--line)') + '" data-act="bkTab" data-k="pending"><div class="fs12 muted b">Pending Requests</div><div class="fs20 xb" style="color:#F59E0B;margin-top:4px">' + by('pending').length + '</div></div>' +
                                            '<div class="card" style="cursor:pointer;border:' + (f === 'active' ? '2px solid #16A34A' : '1px solid var(--line)') + '" data-act="bkTab" data-k="active"><div class="fs12 muted b">Active Bulk Users</div><div class="fs20 xb" style="color:#16A34A;margin-top:4px">' + by('active').length + '</div></div>' +
                                            '<div class="card" style="cursor:pointer;border:' + (f === 'rejected' ? '2px solid #E11D48' : '1px solid var(--line)') + '" data-act="bkTab" data-k="rejected"><div class="fs12 muted b">Rejected</div><div class="fs20 xb" style="color:#E11D48;margin-top:4px">' + by('rejected').length + '</div></div>' +
                                          '</div>';
                                          const itemsHTML = list.map(r => {
                                            const uid = esc(r.uid);
                                            const userDeviceCount = r.user ? (r.user.bulkDeviceCount || 2) : 2;
                                            const act = f === 'pending'
                                              ? '<button class="btn okfill sm" data-act="bkApprove" data-uid="' + uid + '">Activate</button><button class="btn bad sm" data-act="bkReject" data-uid="' + uid + '">Reject</button>'
                                              : f === 'active'
                                              ? '<button class="btn okfill sm" style="background:#0284C7" data-act="bkSetLimit" data-uid="' + uid + '">Set Limit (' + userDeviceCount + ')</button><button class="btn bad sm" data-act="bkRevoke" data-uid="' + uid + '">Turn off</button>'
                                              : '<button class="btn okfill sm" data-act="bkApprove" data-uid="' + uid + '">Activate</button>';
                                            return '<div class="card"><div class="row between wrap"><div><div class="b fs14">' + esc(r.name) + '</div><div class="fs12 muted">' + esc(r.email) + ' • Limit: <b>' + userDeviceCount + ' Devices</b></div></div>' + pill[r.status] + '</div><div class="row gap6 mt8" style="justify-content:flex-end">' + act + '</div></div>';
                                          }).join('') || ('<div class="card">' + emptyHTML('bulk', f === 'pending' ? 'No pending requests waiting.' : f === 'active' ? 'No active bulk access users yet.' : 'No rejected requests.') + '</div>');
                                          setHTML(listBox, summaryHTML + itemsHTML);
                                        };
                                        if (S.view === 'bulk') RENDER.bulk();
                                      }
                                      return true;
                                    }
                                    setTimeout(ensureBulkAccessRendered, 600);
                                    setTimeout(ensureBulkAccessRendered, 1500);
                                    setTimeout(ensureBulkAccessRendered, 3000);

                                    // Hook anchor downloads (data: and blob: URLs for single HQ screenshot & ZIP download all)
                                    if (!window.__tgDownloadHooked) {
                                      window.__tgDownloadHooked = true;
                                      const origClick = HTMLAnchorElement.prototype.click;
                                      HTMLAnchorElement.prototype.click = function() {
                                        const href = this.getAttribute('href') || this.href || '';
                                        const downloadName = this.getAttribute('download') || ('TaskGullak_' + Date.now() + '.png');
                                        if (href.startsWith('data:') && window.AndroidAdminBridge) {
                                          window.AndroidAdminBridge.saveBase64File(href, downloadName, 'image/png');
                                          return;
                                        }
                                        if (href.startsWith('blob:') && window.AndroidAdminBridge) {
                                          fetch(href)
                                            .then(r => r.blob())
                                            .then(blob => {
                                              const reader = new FileReader();
                                              reader.onloadend = function() {
                                                window.AndroidAdminBridge.saveBase64File(
                                                  reader.result,
                                                  downloadName,
                                                  blob.type || 'application/zip'
                                                );
                                              };
                                              reader.readAsDataURL(blob);
                                            })
                                            .catch(() => origClick.apply(this, arguments));
                                          return;
                                        }
                                        return origClick.apply(this, arguments);
                                      };
                                    }
                                  } catch (err) {
                                    console.error('AutoUnlock error', err);
                                  }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(autoUnlockAndBridgeJs, null)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                // If live domain is temporarily unreachable, load the bundled identical web admin panel from GitHub
                                val currentUrl = request.url?.toString().orEmpty()
                                if (!currentUrl.startsWith("file:///android_asset/")) {
                                    view?.loadUrl(LOCAL_FALLBACK_ASSET_URL)
                                } else {
                                    hasNetworkError = true
                                    isLoading = false
                                }
                            }
                        }
                    }

                    webViewRef = this
                    loadUrl(ADMIN_WEB_URL)
                }
            }
        )

        // Subtle top progress bar during initial load
        if (isLoading && progress < 95) {
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = Color(0xFFF59E0B),
                trackColor = Color.Transparent
            )
        }

        // Splash loader while the web admin panel initializes so it feels 100% like a native app
        AnimatedVisibility(
            visible = isLoading && progress < 55,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "TG",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF090D16)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Task Gullak Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFF59E0B),
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }

        if (hasNetworkError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connection Interrupted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Unable to reach the live server. Check your internet connection and tap Retry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Button(
                        onClick = {
                            hasNetworkError = false
                            isLoading = true
                            webViewRef?.loadUrl(ADMIN_WEB_URL)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF090D16)
                        )
                    ) {
                        Text("Retry Connection", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onSwitchToNativeBackup) {
                        Text("Open Offline Native Admin", color = Color(0xFFF59E0B))
                    }
                }
            }
        }
    }
}
