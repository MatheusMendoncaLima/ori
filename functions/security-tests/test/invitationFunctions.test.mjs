// The invitation Cloud Functions (functions/functions), run against the Firestore emulator through the Admin
// SDK. They are not published (they need the Blaze plan): the app accepts invitations through the rules
// (careInvitations.test.mjs). These tests keep the functions right for the day they are published.
import assert from "node:assert/strict";
import { before, beforeEach, describe, it } from "node:test";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import {
  acceptInvitation,
  createInvitation,
  normalizeInvitationCode,
} from "../../functions/lib/careInvitations.js";

const PROJECT_ID = "demo-ori";
const NOW = new Date("2026-10-28T12:00:00Z");
const ONE_DAY_LATER = new Date(NOW.getTime() + 24 * 60 * 60 * 1000);
let db;

const expectCode = (code) => (error) => {
  assert.equal(error.code, code);
  return true;
};

async function clearDatabase() {
  const host = process.env.FIRESTORE_EMULATOR_HOST;
  const url = `http://${host}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`;
  const response = await fetch(url, { method: "DELETE" });
  assert.ok(response.ok, "could not clear the emulator");
}

async function seedProfiles() {
  // Profiles as the app creates them now: a name, no role.
  const profile = (name) => ({ displayName: name, createdAt: NOW });
  await db.doc("users/cara").set(profile("Cara"));
  await db.doc("users/pat").set(profile("Pat"));
  await db.doc("users/otto").set(profile("Otto"));
}

before(() => {
  initializeApp({ projectId: PROJECT_ID });
  db = getFirestore();
});

beforeEach(async () => {
  await clearDatabase();
  await seedProfiles();
});

describe("invitation codes", () => {
  it("accepts lowercase codes with spaces and rejects malformed input", () => {
    assert.equal(normalizeInvitationCode(" abcd efgh "), "ABCDEFGH");
    for (const bad of ["", "ABC", "ABCDEFGH1", "ABCDEF0O", 12345678, null]) {
      assert.throws(
        () => normalizeInvitationCode(bad),
        expectCode("invalid-argument"),
      );
    }
  });
});

describe("createInvitation", () => {
  it("stores a pending invitation that expires in 24 hours", async () => {
    const { code, expiresAtMillis } = await createInvitation(db, "cara", NOW);

    const stored = await db.doc(`invitations/${code}`).get();
    assert.equal(stored.get("status"), "pending");
    assert.equal(stored.get("caregiverUid"), "cara");
    assert.equal(expiresAtMillis, ONE_DAY_LATER.getTime());
  });

  it("any account with a profile can invite, and only those", async () => {
    const { code } = await createInvitation(db, "pat", NOW);
    assert.equal(
      (await db.doc(`invitations/${code}`).get()).get("caregiverUid"),
      "pat",
    );
    await assert.rejects(
      createInvitation(db, "nobody", NOW),
      expectCode("permission-denied"),
    );
  });

  it("retries when a generated code is already taken", async () => {
    await db.doc("invitations/AAAAAAAA").set({ status: "pending" });
    const codes = ["AAAAAAAA", "BBBBBBBB"];

    const { code } = await createInvitation(db, "cara", NOW, () =>
      codes.shift(),
    );

    assert.equal(code, "BBBBBBBB");
  });
});

describe("acceptInvitation", () => {
  it("creates an active link with view-only permission and consumes the code", async () => {
    const { code } = await createInvitation(db, "cara", NOW);

    const result = await acceptInvitation(db, "pat", code.toLowerCase(), NOW);

    assert.deepEqual(result, { linkId: "cara_pat", caregiverName: "Cara" });
    const link = await db.doc("careLinks/cara_pat").get();
    assert.equal(link.get("status"), "active");
    assert.deepEqual(link.get("permissions"), {
      viewRoutines: true,
      manageRoutines: false,
      manageCommunication: false,
    });
    assert.equal(link.get("primaryUserName"), "Pat");
    assert.equal(
      (await db.doc(`invitations/${code}`).get()).get("status"),
      "accepted",
    );
  });

  it("repeating an accepted call by the same person is a safe retry", async () => {
    const { code } = await createInvitation(db, "cara", NOW);
    const first = await acceptInvitation(db, "pat", code, NOW);

    const retry = await acceptInvitation(db, "pat", code, NOW);

    assert.deepEqual(retry, first);
  });

  it("a retry does not revive a link that was revoked meanwhile", async () => {
    const { code } = await createInvitation(db, "cara", NOW);
    await acceptInvitation(db, "pat", code, NOW);
    await db
      .doc("careLinks/cara_pat")
      .update({ status: "revoked", revokedAt: NOW });

    await assert.rejects(
      acceptInvitation(db, "pat", code, NOW),
      expectCode("failed-precondition"),
    );
  });

  it("a code works only once", async () => {
    const { code } = await createInvitation(db, "cara", NOW);
    await acceptInvitation(db, "pat", code, NOW);

    await assert.rejects(
      acceptInvitation(db, "otto", code, NOW),
      expectCode("failed-precondition"),
    );
    assert.equal((await db.doc("careLinks/cara_otto").get()).exists, false);
  });

  it("rejects unknown and expired codes", async () => {
    await assert.rejects(
      acceptInvitation(db, "pat", "ZZZZZZZZ", NOW),
      expectCode("not-found"),
    );
    const { code } = await createInvitation(db, "cara", NOW);
    await assert.rejects(
      acceptInvitation(db, "pat", code, ONE_DAY_LATER),
      expectCode("failed-precondition"),
    );
  });

  it("any account with a profile can accept, and only those", async () => {
    await db.doc("users/carl").set({ displayName: "Carl", createdAt: NOW });
    const { code } = await createInvitation(db, "cara", NOW);

    await assert.rejects(
      acceptInvitation(db, "nobody", code, NOW),
      expectCode("permission-denied"),
    );
    assert.equal(
      (await db.doc(`invitations/${code}`).get()).get("status"),
      "pending",
    );

    const accepted = await acceptInvitation(db, "carl", code, NOW);
    assert.equal(accepted.linkId, "cara_carl");
  });

  it("does not duplicate an active link", async () => {
    await acceptInvitation(
      db,
      "pat",
      (await createInvitation(db, "cara", NOW)).code,
      NOW,
    );
    const second = await createInvitation(db, "cara", NOW);

    await assert.rejects(
      acceptInvitation(db, "pat", second.code, NOW),
      expectCode("already-exists"),
    );
  });

  it("a new invitation re-creates a revoked link with default permissions", async () => {
    await acceptInvitation(
      db,
      "pat",
      (await createInvitation(db, "cara", NOW)).code,
      NOW,
    );
    await db.doc("careLinks/cara_pat").update({
      status: "revoked",
      revokedAt: NOW,
      permissions: { viewRoutines: true, manageRoutines: true },
    });

    await acceptInvitation(
      db,
      "pat",
      (await createInvitation(db, "cara", NOW)).code,
      NOW,
    );

    const link = await db.doc("careLinks/cara_pat").get();
    assert.equal(link.get("status"), "active");
    assert.equal(link.get("permissions").manageRoutines, false);
  });

  it("rejects an invitation whose author no longer has a profile", async () => {
    const { code } = await createInvitation(db, "cara", NOW);
    await db.doc("users/cara").delete();

    await assert.rejects(
      acceptInvitation(db, "pat", code, NOW),
      expectCode("failed-precondition"),
    );
  });
});
