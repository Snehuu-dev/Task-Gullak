// USER APP: Bulk mode access. The admin panel reads and writes these fields.
import { collection, addDoc, query, where, getDocs, serverTimestamp } from "firebase/firestore";   // your db import

// 1) User taps "Request Bulk mode"  -> admin sees it in "Bulk Access" > Pending
export async function requestBulkAccess(db, user, note = "") {
  const open = await getDocs(query(collection(db, "bulkRequests"), where("userId", "==", user.uid), where("status", "==", "pending")));
  if (!open.empty) return "already_pending";
  await addDoc(collection(db, "bulkRequests"), {
    userId: user.uid, userName: user.displayName || "", userEmail: user.email || "",
    note, status: "pending", createdAt: serverTimestamp()
  });
  return "sent";
}

// 2) Show Bulk mode ONLY when the admin activated it for this user (users/{uid}.bulkMode === true)
//    Rejected users have bulkMode:false and bulkStatus:"rejected" (optional: bulkRejectReason).
export const canUseBulk = (userDoc, task) =>
  userDoc?.bulkMode === true && task.bulkEnabled !== false && task.taskMode !== "normal";

export const canUseNormal = (task) => task.taskMode !== "bulk";

// 3) Comments a user may take on one app in Bulk mode: task.bulkMaxComments (e.g. 5). Normal mode = 1.
export const maxCommentsFor = (userDoc, task) => (canUseBulk(userDoc, task) ? task.bulkMaxComments || 2 : 1);

// 4) Button state text
export function bulkButtonState(userDoc, pendingRequest) {
  if (userDoc?.bulkMode === true) return "active";          // show Bulk mode
  if (pendingRequest) return "pending";                      // "Request sent"
  if (userDoc?.bulkStatus === "rejected") return "rejected"; // hide Bulk mode (show reason if you like)
  return "none";                                             // show "Request Bulk mode"
}
