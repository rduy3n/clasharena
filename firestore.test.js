const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const nowTs = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function baseRoom(roomCode = "K9X4M2") {
  return {
    roomCode,
    hostId: ALICE_UID,
    hostName: "Alice",
    guestId: "",
    guestName: "",
    participantIds: [ALICE_UID],
    status: "waiting",
    stageId: "cyber_street",
    hostFighterId: "ryuji",
    guestFighterId: "kaelith",
    hostReady: false,
    guestReady: false,
    roundNumber: 1,
    p1RoundsWon: 0,
    p2RoundsWon: 0,
    winnerSide: "none",
    p1X: 0.25,
    p1Y: 0.0,
    p1Facing: 1,
    p1Action: "idle",
    p1ActionSeq: 0,
    p1Blocking: false,
    p1Ulti: 0,
    p1DealtDmg: 0,
    p2X: 0.75,
    p2Y: 0.0,
    p2Facing: -1,
    p2Action: "idle",
    p2ActionSeq: 0,
    p2Blocking: false,
    p2Ulti: 0,
    p2DealtDmg: 0,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

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

test("Unauthenticated user cannot read or write users or match_rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("match_rooms").doc("K9X4M2").get());
  await assertFails(unauthDb.collection("match_rooms").where("participantIds", "array-contains", ALICE_UID).get());
});

test("Authenticated user can create and read own profile, but cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      favoriteFighterId: "ryuji",
      wins: 0,
      losses: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update test: extra ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      wins: 5,
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );
});

test("Host can create room, Bob can join, both select fighters, fight, and finished room is locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const roomRefAlice = aliceDb.collection("match_rooms").doc("K9X4M2");
  const roomRefBob = bobDb.collection("match_rooms").doc("K9X4M2");
  const roomRefCharlie = charlieDb.collection("match_rooms").doc("K9X4M2");

  // Create room
  await assertSucceeds(roomRefAlice.set(baseRoom("K9X4M2")));

  // Invalid room code rejected
  await assertFails(aliceDb.collection("match_rooms").doc("bad").set(baseRoom("bad")));

  // Bob joins waiting room
  await assertSucceeds(
    roomRefBob.update({
      guestId: BOB_UID,
      guestName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      status: "selecting",
      updatedAt: nowTs(),
    })
  );

  // Non-participant Charlie cannot read active selecting room
  await assertFails(roomRefCharlie.get());

  // List query without participantIds filter fails
  await assertFails(aliceDb.collection("match_rooms").get());

  // Scoped participantIds list query succeeds
  await assertSucceeds(
    aliceDb.collection("match_rooms").where("participantIds", "array-contains", ALICE_UID).get()
  );

  // Guest readies up
  await assertSucceeds(
    roomRefBob.update({
      guestFighterId: "sora",
      guestReady: true,
      updatedAt: nowTs(),
    })
  );

  // Host starts fight
  await assertSucceeds(
    roomRefAlice.update({
      hostFighterId: "titan",
      hostReady: true,
      status: "fighting",
      updatedAt: nowTs(),
    })
  );

  // P2 cannot modify P1 fields
  await assertFails(
    roomRefBob.update({
      p1DealtDmg: 999,
      updatedAt: nowTs(),
    })
  );

  // P2 updates P2 combat state
  await assertSucceeds(
    roomRefBob.update({
      p2X: 0.6,
      p2Action: "classic",
      p2ActionSeq: 1,
      p2DealtDmg: 45,
      updatedAt: nowTs(),
    })
  );

  // Host finishes match
  await assertSucceeds(
    roomRefAlice.update({
      p1RoundsWon: 2,
      status: "finished",
      winnerSide: "p1",
      updatedAt: nowTs(),
    })
  );

  // Terminal state lock: no further updates allowed after finished
  await assertFails(
    roomRefAlice.update({
      winnerSide: "p2",
      updatedAt: nowTs(),
    })
  );
});
