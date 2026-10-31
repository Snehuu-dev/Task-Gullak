const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read user submissions subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("submissions").get()
  );
});

test("Authenticated user can create, read, and update avatarId and fullName on their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Verma",
      email: "alice@example.com",
      walletBalance: 150,
      completedTasksCount: 3,
      pendingRewards: 50,
      emailVerified: true,
      avatarId: "avatar_3d_astronaut",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice Sharma",
      avatarId: "avatar_3d_gold_mogul",
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user cannot read or write another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Verma",
      email: "alice@example.com",
      walletBalance: 150,
      completedTasksCount: 3,
      pendingRewards: 50,
      emailVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      fullName: "Bob Hijack",
      updatedAt: now,
    })
  );
});

test("Shadow update with extra field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Verma",
      email: "alice@example.com",
      walletBalance: 150,
      completedTasksCount: 3,
      pendingRewards: 50,
      emailVerified: true,
      isAdminGhost: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Email spoofing with email_verified false is rejected for admin actions", async () => {
  const spoofAdminDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "snehkumarverma2011@gmail.com",
      email_verified: false,
    })
    .firestore();
  const now = new Date();
  await assertFails(
    spoofAdminDb.collection("broadcasts").doc("b_spoof").set({
      title: "Fake Broadcast",
      message: "Spoofed admin message",
      type: "broadcast",
      active: true,
      createdAt: now,
    })
  );
});

test("Verified Admin can ban/unban user, credit wallet, add tasks, broadcast, and approve submissions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "snehkumarverma2011@gmail.com",
      email_verified: true,
    })
    .firestore();
  const now = new Date();

  // 1. Alice creates profile and submits task with screenshot proof
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Verma",
      email: "alice@example.com",
      walletBalance: 0,
      completedTasksCount: 0,
      pendingRewards: 80,
      emailVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("submissions").doc("sub_101").set({
      userId: ALICE_UID,
      userName: "Alice Verma",
      userEmail: "alice@example.com",
      taskId: "task_feedback",
      taskTitle: "Give Feedback",
      category: "Feedback",
      rewardAmount: 80,
      proofText: "Completed UX review and attached screenshot proof.",
      screenshotBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Admin approves submission and credits Alice's wallet
  await assertSucceeds(
    adminDb.collection("submissions").doc("sub_101").update({
      status: "approved",
      adminNote: "Verified screenshot proof!",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      walletBalance: 80,
      completedTasksCount: 1,
      pendingRewards: 0,
      isBanned: true,
      banReason: "Suspicious duplicate account check",
      updatedAt: now,
    })
  );

  // 3. Admin adds a task and broadcasts a notification
  await assertSucceeds(
    adminDb.collection("tasks").doc("task_new_1").set({
      title: "Install Partner App",
      subtitle: "Earn Reward • Instant Verification",
      category: "Testing",
      rewardInr: 150,
      durationMinutes: 5,
      instructions: ["Download app", "Take screenshot of dashboard"],
      proofRequirement: "Upload screenshot and write your review message.",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("broadcasts").doc("b_1").set({
      title: "New ₹150 Project Live!",
      message: "Complete the Partner App task now and upload your screenshot.",
      type: "new_task",
      targetUserId: "all",
      active: true,
      createdAt: now,
    })
  );

  // 4. Alice can query active tasks and active broadcasts
  await assertSucceeds(
    aliceDb.collection("tasks").where("active", "==", true).get()
  );
  await assertSucceeds(
    aliceDb.collection("broadcasts").where("active", "==", true).get()
  );
});
