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
const ADMIN_EMAIL = "sanakhanjani412@gmail.com";

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

test("Unauthenticated user: cannot read or write users, rooms, or withdrawals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("party_rooms").where("roomStatus", "==", "LIVE").get()
  );
  await assertFails(
    unauthDb.collection("withdraw_requests").where("userId", "==", ALICE_UID).get()
  );
});

test("User profile: owner can create bounded profile and update wallet; cross-user get/list denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      avatarKey: "sana",
      coins: 1500,
      diamonds: 200,
      totalEarnedRs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject inflated initial coins (>2000)
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      avatarKey: "ali",
      coins: 999999,
      diamonds: 0,
      totalEarnedRs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user get & list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 1000,
      diamonds: 300,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Party room, 8 seats & chat: filtered queries succeed; seat mic toggle & gift increment work", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("room_1").set({
      roomTitle: "Mehfil-e-Yaari",
      hostUid: ALICE_UID,
      hostName: "Alice",
      roomStatus: "LIVE",
      activeSpeakersCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query matches rule
  await assertSucceeds(
    bobDb.collection("party_rooms").where("roomStatus", "==", "LIVE").get()
  );
  // Unfiltered query fails
  await assertFails(bobDb.collection("party_rooms").get());

  // Create Host seat 0 and empty Guest seat 1
  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("room_1").collection("seats").doc("0").set({
      roomId: "room_1",
      seatNumber: 0,
      seatRole: "HOST",
      occupantUid: ALICE_UID,
      occupantName: "Alice",
      avatarKey: "sana",
      micOn: true,
      occupied: true,
      diamondsReceived: 0,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("room_1").collection("seats").doc("1").set({
      roomId: "room_1",
      seatNumber: 1,
      seatRole: "GUEST",
      occupantUid: "",
      occupantName: "",
      avatarKey: "empty",
      micOn: false,
      occupied: false,
      diamondsReceived: 0,
      updatedAt: now,
    })
  );

  // Bob takes empty seat 1
  await assertSucceeds(
    bobDb.collection("party_rooms").doc("room_1").collection("seats").doc("1").update({
      occupantUid: BOB_UID,
      occupantName: "Bob",
      avatarKey: "ali",
      micOn: true,
      occupied: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob toggles own mic OFF
  await assertSucceeds(
    bobDb.collection("party_rooms").doc("room_1").collection("seats").doc("1").update({
      micOn: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob sends gift to Alice's seat (diamondsReceived increment)
  await assertSucceeds(
    bobDb.collection("party_rooms").doc("room_1").collection("seats").doc("0").update({
      diamondsReceived: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob sends chat message
  await assertSucceeds(
    bobDb.collection("party_rooms").doc("room_1").collection("messages").doc("msg_1").set({
      roomId: "room_1",
      senderUid: BOB_UID,
      senderName: "Bob",
      senderAvatarKey: "ali",
      messageText: "Assalam-o-Alaikum!",
      messageType: "CHAT",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Filtered seats & messages queries succeed
  await assertSucceeds(
    bobDb
      .collection("party_rooms")
      .doc("room_1")
      .collection("seats")
      .where("roomId", "==", "room_1")
      .get()
  );
  await assertSucceeds(
    bobDb
      .collection("party_rooms")
      .doc("room_1")
      .collection("messages")
      .where("roomId", "==", "room_1")
      .get()
  );
});

test("Withdrawals: <1000 coins rejected, self-approval rejected, spoofed admin rejected, verified admin approves & locks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext("admin_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  const now = new Date(Date.now() - 2000);

  // Reject < 1000 coins
  await assertFails(
    aliceDb.collection("withdraw_requests").doc("req_low").set({
      userId: ALICE_UID,
      userName: "Alice",
      method: "EASYPAISA",
      accountNumber: "03001234567",
      coinsAmount: 999,
      rupeesAmount: 999,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject invalid mobile format
  await assertFails(
    aliceDb.collection("withdraw_requests").doc("req_badnum").set({
      userId: ALICE_UID,
      userName: "Alice",
      method: "JAZZCASH",
      accountNumber: "12345",
      coinsAmount: 1000,
      rupeesAmount: 1000,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid >= 1000 coins request succeeds
  await assertSucceeds(
    aliceDb.collection("withdraw_requests").doc("req_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      method: "JAZZCASH",
      accountNumber: "03001234567",
      coinsAmount: 1000,
      rupeesAmount: 1000,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's withdrawal PII (accountNumber)
  await assertFails(bobDb.collection("withdraw_requests").doc("req_1").get());
  await assertFails(bobDb.collection("withdraw_requests").get());

  // Alice's filtered list succeeds
  await assertSucceeds(
    aliceDb.collection("withdraw_requests").where("userId", "==", ALICE_UID).get()
  );

  // Alice cannot self-approve
  await assertFails(
    aliceDb.collection("withdraw_requests").doc("req_1").update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Spoofed unverified admin email cannot approve
  await assertFails(
    spoofedAdminDb.collection("withdraw_requests").doc("req_1").update({
      status: "APPROVED",
      adminNote: "Spoofed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified Admin approves request
  await assertSucceeds(
    realAdminDb.collection("withdraw_requests").doc("req_1").update({
      status: "APPROVED",
      adminNote: "Paisa bhej diya gaya",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: cannot re-modify APPROVED request
  await assertFails(
    realAdminDb.collection("withdraw_requests").doc("req_1").update({
      status: "REJECTED",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
