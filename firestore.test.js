const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_host_123";
const BOB_UID = "bob_family_456";
const CHARLIE_UID = "charlie_outsider_789";

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

function samplePhotoPayload(albumId, uploaderId, uploaderName, memberUids) {
  const now = new Date(Date.now() - 2000);
  return {
    albumId,
    uploaderId,
    uploaderName,
    caption: "Grandma carving the pie",
    momentTag: "KITCHEN_PIE",
    dayLabel: "Thu · Nov 26",
    takenAtMillis: Date.now() - 5000,
    imageBase64: "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=",
    aspectRatio: 1.5,
    starredByUids: [],
    memberUids,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("albums").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("invites").doc("NOV26A").get());
  await assertFails(unauthDb.collection("invites").get());
  await assertFails(
    unauthDb.collection("albums").doc("album_1").set({
      hostUserId: ALICE_UID,
      hostName: "Alice",
      title: "Unauthorized",
      gatheringMonth: "November 2026",
      inviteCode: "NOV26A",
      memberUids: [ALICE_UID],
      photoCount: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile: owner CRUD succeeds, cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Hoffman",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Aunt Alice",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field & future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      createdAt: new Date(Date.now() + 120000),
      updatedAt: now,
    })
  );
});

test("Album + Invite atomic creation, invite-code join, and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 3000);

  // Atomic batch creating album and invite
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("albums").doc("album_1"), {
    hostUserId: ALICE_UID,
    hostName: "Alice",
    title: "Hoffman Thanksgiving 2026",
    gatheringMonth: "November 2026",
    inviteCode: "NOV26A",
    memberUids: [ALICE_UID],
    photoCount: 0,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("invites").doc("NOV26A"), {
    albumId: "album_1",
    hostUserId: ALICE_UID,
    albumTitle: "Hoffman Thanksgiving 2026",
    gatheringMonth: "November 2026",
    createdAt: now,
  });
  await assertSucceeds(batch.commit());

  // Invites can be fetched directly by code, never listed
  await assertSucceeds(bobDb.collection("invites").doc("NOV26A").get());
  await assertFails(bobDb.collection("invites").get());

  // Unfiltered album list fails; member-scoped query succeeds
  await assertFails(aliceDb.collection("albums").get());
  await assertSucceeds(
    aliceDb.collection("albums").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Outsider cannot read album before joining
  await assertFails(charlieDb.collection("albums").doc("album_1").get());

  // Bob joins album by adding only himself to memberUids
  await assertSucceeds(
    bobDb.collection("albums").doc("album_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot hijack title during join or outside host role
  await assertFails(
    bobDb.collection("albums").doc("album_1").update({
      title: "Hijacked Album",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Host can edit album title
  await assertSucceeds(
    aliceDb.collection("albums").doc("album_1").update({
      title: "Hoffman Family Gathering 2026",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Photos subcollection: member upload, star toggle, caption permissions, and negative guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("albums").doc("album_1").set({
      hostUserId: ALICE_UID,
      hostName: "Alice",
      title: "November 2026",
      gatheringMonth: "November 2026",
      inviteCode: "NOV26B",
      memberUids: [ALICE_UID, BOB_UID],
      photoCount: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  // Alice uploads a photo (without @DocumentId "id" key)
  await assertSucceeds(
    aliceDb
      .collection("albums")
      .doc("album_1")
      .collection("photos")
      .doc("photo_1")
      .set(samplePhotoPayload("album_1", ALICE_UID, "Alice", [ALICE_UID, BOB_UID]))
  );

  // Reject payload containing @DocumentId "id" key
  await assertFails(
    aliceDb
      .collection("albums")
      .doc("album_1")
      .collection("photos")
      .doc("photo_bad_id")
      .set({
        id: "photo_bad_id",
        ...samplePhotoPayload("album_1", ALICE_UID, "Alice", [ALICE_UID, BOB_UID]),
      })
  );

  // Reject invalid momentTag enum
  await assertFails(
    aliceDb
      .collection("albums")
      .doc("album_1")
      .collection("photos")
      .doc("photo_bad_tag")
      .set({
        ...samplePhotoPayload("album_1", ALICE_UID, "Alice", [ALICE_UID, BOB_UID]),
        momentTag: "INVALID_TAG",
      })
  );

  // Member-scoped query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb
      .collection("albums")
      .doc("album_1")
      .collection("photos")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    bobDb.collection("albums").doc("album_1").collection("photos").get()
  );

  // Outsider Charlie cannot upload, read, or list photos
  await assertFails(
    charlieDb
      .collection("albums")
      .doc("album_1")
      .collection("photos")
      .doc("photo_charlie")
      .set(samplePhotoPayload("album_1", CHARLIE_UID, "Charlie", [ALICE_UID, BOB_UID, CHARLIE_UID]))
  );
  await assertFails(
    charlieDb.collection("albums").doc("album_1").collection("photos").doc("photo_1").get()
  );

  // Bob (member) can star Alice's photo for TV highlights
  await assertSucceeds(
    bobDb.collection("albums").doc("album_1").collection("photos").doc("photo_1").update({
      starredByUids: [BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot edit Alice's caption, nor delete Alice's photo (Bob is not host or uploader)
  await assertFails(
    bobDb.collection("albums").doc("album_1").collection("photos").doc("photo_1").update({
      caption: "Altered by Bob",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("albums").doc("album_1").collection("photos").doc("photo_1").delete()
  );

  // Alice (uploader) can edit caption and delete
  await assertSucceeds(
    aliceDb.collection("albums").doc("album_1").collection("photos").doc("photo_1").update({
      caption: "Updated pie caption",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    aliceDb.collection("albums").doc("album_1").collection("photos").doc("photo_1").delete()
  );
});
