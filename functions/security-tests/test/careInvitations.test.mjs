// Firestore security rules tests: invitations made and accepted by the app, without Cloud Functions.
// The rules check everything the server used to check: a real author, a 24-hour code used once, the latest
// code of the caregiver, no self link, the names in the profiles and a link born with the least privilege.
import { describe, it } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import {
  PRIMARY,
  OTHER_PRIMARY,
  CAREGIVER,
  REVOKED_CAREGIVER,
  UNLINKED_CAREGIVER,
  NO_PROFILE,
  dbAs,
  anonymousDb,
  testEnv,
} from "./support/rulesFixture.mjs";
import {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from "firebase/firestore";

const CODE = "K7MPQR2X";
const OTHER_CODE = "W3NXHT8C";
const HOUR = 60 * 60 * 1000;
const VIEW_ONLY = {
  viewRoutines: true,
  manageRoutines: false,
  manageCommunication: false,
};

const fromNow = (millis) => Timestamp.fromMillis(Date.now() + millis);

// As the app creates it: the code, and the pointer to the latest code, in one batch.
function invite(uid, name, code = CODE, overrides = {}, pointer = {}) {
  const db = dbAs(uid);
  const batch = writeBatch(db);
  batch.set(doc(db, `invitations/${code}`), {
    caregiverUid: uid,
    caregiverName: name,
    status: "pending",
    createdAt: serverTimestamp(),
    expiresAt: fromNow(24 * HOUR),
    ...overrides,
  });
  batch.set(doc(db, `invitationOwners/${uid}`), {
    code,
    createdAt: serverTimestamp(),
    ...pointer,
  });
  return batch.commit();
}

const linkFrom = (caregiverUid, caregiverName, me, myName, code) => ({
  caregiverUid,
  primaryUserUid: me,
  caregiverName,
  primaryUserName: myName,
  status: "active",
  permissions: VIEW_ONLY,
  createdAt: serverTimestamp(),
  revokedAt: null,
  invitationCode: code,
});

// As the app accepts it: the link and the invitation marked as used, in one batch.
function accept(
  me,
  myName,
  {
    caregiverUid = UNLINKED_CAREGIVER,
    caregiverName = UNLINKED_CAREGIVER,
    code = CODE,
  } = {},
  linkChanges = {},
  invitationChanges = {},
) {
  const db = dbAs(me);
  const batch = writeBatch(db);
  batch.set(doc(db, `careLinks/${caregiverUid}_${me}`), {
    ...linkFrom(caregiverUid, caregiverName, me, myName, code),
    ...linkChanges,
  });
  batch.update(doc(db, `invitations/${code}`), {
    status: "accepted",
    acceptedBy: me,
    acceptedAt: serverTimestamp(),
    ...invitationChanges,
  });
  return batch.commit();
}

// An invitation made some time ago, with its pointer (written as the server would see it).
async function seedInvitation(uid, name, code, ageMillis, status = "pending") {
  await testEnv().withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const createdAt = Timestamp.fromMillis(Date.now() - ageMillis);
    await setDoc(doc(db, `invitations/${code}`), {
      caregiverUid: uid,
      caregiverName: name,
      status,
      createdAt,
      expiresAt: Timestamp.fromMillis(createdAt.toMillis() + 24 * HOUR),
    });
    await setDoc(doc(db, `invitationOwners/${uid}`), { code, createdAt });
  });
}

describe("making an invitation", () => {
  it("stores a pending code with the real name and the pointer to it", async () => {
    await assertSucceeds(invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER));
    await assertSucceeds(
      getDoc(
        doc(dbAs(UNLINKED_CAREGIVER), `invitationOwners/${UNLINKED_CAREGIVER}`),
      ),
    );
  });

  it("refuses another author, a forged name, a used status or a long life", async () => {
    await assertFails(
      invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, CODE, {
        caregiverUid: CAREGIVER,
      }),
    );
    await assertFails(invite(UNLINKED_CAREGIVER, "Someone else"));
    await assertFails(
      invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, CODE, {
        status: "accepted",
      }),
    );
    await assertFails(
      invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, CODE, {
        expiresAt: fromNow(72 * HOUR),
      }),
    );
    await assertFails(
      invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, CODE, {
        createdAt: new Date(0),
      }),
    );
    await assertFails(
      invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, CODE, { extra: true }),
    );
  });

  it("refuses a code outside the alphabet, and a code without its pointer", async () => {
    for (const bad of [
      "ABCDEFG",
      "ABCDEFGHJ",
      "ABCDEFG0",
      "ABCDEFGI",
      "abcdefgh",
    ]) {
      await assertFails(invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, bad));
    }
    const db = dbAs(UNLINKED_CAREGIVER);
    await assertFails(
      setDoc(doc(db, `invitations/${CODE}`), {
        caregiverUid: UNLINKED_CAREGIVER,
        caregiverName: UNLINKED_CAREGIVER,
        status: "pending",
        createdAt: serverTimestamp(),
        expiresAt: fromNow(24 * HOUR),
      }),
    );
    await assertFails(
      invite(
        UNLINKED_CAREGIVER,
        UNLINKED_CAREGIVER,
        CODE,
        {},
        { code: OTHER_CODE },
      ),
    );
  });

  it("only an account with a profile invites", async () => {
    await assertFails(invite(NO_PROFILE, NO_PROFILE));
  });

  it("a code that is taken cannot be taken again", async () => {
    await assertSucceeds(invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER));
    await assertFails(invite(OTHER_PRIMARY, "Otto"));
  });

  it("a new code waits 30 seconds, and then only the new code can be accepted", async () => {
    await assertSucceeds(invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER));
    await assertFails(
      invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, OTHER_CODE),
    );

    await seedInvitation(
      UNLINKED_CAREGIVER,
      UNLINKED_CAREGIVER,
      CODE,
      60 * 1000,
    );
    await assertSucceeds(
      invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, OTHER_CODE),
    );
    await assertFails(accept(PRIMARY, "Pat"));
    await assertSucceeds(accept(PRIMARY, "Pat", { code: OTHER_CODE }));
  });
});

describe("reading an invitation", () => {
  it("one code by its name, by someone with a profile, never a list", async () => {
    await assertSucceeds(invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER));
    await assertSucceeds(getDoc(doc(dbAs(PRIMARY), `invitations/${CODE}`)));
    await assertSucceeds(
      getDoc(doc(dbAs(PRIMARY), `invitations/${OTHER_CODE}`)),
    );
    await assertFails(
      getDocs(query(collection(dbAs(PRIMARY), "invitations"), limit(1))),
    );
    await assertFails(getDoc(doc(dbAs(NO_PROFILE), `invitations/${CODE}`)));
    await assertFails(getDoc(doc(anonymousDb(), `invitations/${CODE}`)));
    await assertFails(
      getDoc(doc(dbAs(PRIMARY), `invitationOwners/${UNLINKED_CAREGIVER}`)),
    );
  });
});

describe("accepting an invitation", () => {
  it("creates a view-only link that both people read, and uses the code up", async () => {
    await invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER);
    await assertSucceeds(accept(PRIMARY, "Pat"));
    const linkPath = `careLinks/${UNLINKED_CAREGIVER}_${PRIMARY}`;
    await assertSucceeds(getDoc(doc(dbAs(PRIMARY), linkPath)));
    await assertSucceeds(getDoc(doc(dbAs(UNLINKED_CAREGIVER), linkPath)));
    await assertFails(accept(OTHER_PRIMARY, "Otto"));
  });

  it("refuses more permissions, other names or another state", async () => {
    await invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER);
    await assertFails(
      accept(
        PRIMARY,
        "Pat",
        {},
        { permissions: { ...VIEW_ONLY, manageRoutines: true } },
      ),
    );
    await assertFails(accept(PRIMARY, "Patricia"));
    await assertFails(
      accept(PRIMARY, "Pat", { caregiverName: "Someone else" }),
    );
    await assertFails(accept(PRIMARY, "Pat", {}, { status: "revoked" }));
    await assertFails(accept(PRIMARY, "Pat", {}, { createdAt: new Date(0) }));
    await assertFails(
      accept(PRIMARY, "Pat", {}, {}, { acceptedBy: OTHER_PRIMARY }),
    );
  });

  it("the link and the invitation go together, never one without the other", async () => {
    await invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER);
    const db = dbAs(PRIMARY);
    await assertFails(
      setDoc(
        doc(db, `careLinks/${UNLINKED_CAREGIVER}_${PRIMARY}`),
        linkFrom(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER, PRIMARY, "Pat", CODE),
      ),
    );
    await assertFails(
      updateDoc(doc(db, `invitations/${CODE}`), {
        status: "accepted",
        acceptedBy: PRIMARY,
        acceptedAt: serverTimestamp(),
      }),
    );
  });

  it("the link is only between the caregiver of the code and the person who accepts", async () => {
    await invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER);
    // A link for another caregiver, or for someone else, with this code.
    await assertFails(
      accept(PRIMARY, "Pat", {
        caregiverUid: CAREGIVER,
        caregiverName: CAREGIVER,
      }),
    );
    const db = dbAs(PRIMARY);
    const batch = writeBatch(db);
    batch.set(doc(db, `careLinks/${UNLINKED_CAREGIVER}_${OTHER_PRIMARY}`), {
      ...linkFrom(
        UNLINKED_CAREGIVER,
        UNLINKED_CAREGIVER,
        OTHER_PRIMARY,
        "Otto",
        CODE,
      ),
    });
    batch.update(doc(db, `invitations/${CODE}`), {
      status: "accepted",
      acceptedBy: PRIMARY,
      acceptedAt: serverTimestamp(),
    });
    await assertFails(batch.commit());
  });

  it("refuses the caregiver's own code, an expired code and a code already used", async () => {
    await invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER);
    await assertFails(accept(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER));

    await seedInvitation(
      UNLINKED_CAREGIVER,
      UNLINKED_CAREGIVER,
      CODE,
      25 * HOUR,
    );
    await assertFails(accept(PRIMARY, "Pat"));

    await seedInvitation(
      UNLINKED_CAREGIVER,
      UNLINKED_CAREGIVER,
      CODE,
      HOUR,
      "accepted",
    );
    await assertFails(accept(PRIMARY, "Pat"));
  });

  it("only accounts with a profile accept, and only from an inviter who still has one", async () => {
    await invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER);
    await assertFails(accept(NO_PROFILE, NO_PROFILE));

    await testEnv().withSecurityRulesDisabled((context) =>
      deleteDoc(doc(context.firestore(), `users/${UNLINKED_CAREGIVER}`)),
    );
    await assertFails(accept(PRIMARY, "Pat"));
  });

  it("does not overwrite an active link", async () => {
    await invite(CAREGIVER, CAREGIVER);
    await assertFails(
      accept(PRIMARY, "Pat", {
        caregiverUid: CAREGIVER,
        caregiverName: CAREGIVER,
      }),
    );
  });

  it("a new code starts an ended link again, with the least privilege", async () => {
    await invite(REVOKED_CAREGIVER, REVOKED_CAREGIVER);
    await assertSucceeds(
      accept(PRIMARY, "Pat", {
        caregiverUid: REVOKED_CAREGIVER,
        caregiverName: REVOKED_CAREGIVER,
      }),
    );
    // A code used before does not start the link again after it ends.
    await updateDoc(
      doc(dbAs(PRIMARY), `careLinks/${REVOKED_CAREGIVER}_${PRIMARY}`),
      {
        status: "revoked",
        revokedAt: serverTimestamp(),
      },
    );
    await assertFails(
      accept(PRIMARY, "Pat", {
        caregiverUid: REVOKED_CAREGIVER,
        caregiverName: REVOKED_CAREGIVER,
      }),
    );
    // Not even the link on its own, with the invitation it was accepted with.
    await assertFails(
      setDoc(
        doc(dbAs(PRIMARY), `careLinks/${REVOKED_CAREGIVER}_${PRIMARY}`),
        linkFrom(REVOKED_CAREGIVER, REVOKED_CAREGIVER, PRIMARY, "Pat", CODE),
      ),
    );
  });
});

describe("removing an invitation", () => {
  it("only the caregiver removes their code and its pointer", async () => {
    await invite(UNLINKED_CAREGIVER, UNLINKED_CAREGIVER);
    await assertFails(deleteDoc(doc(dbAs(PRIMARY), `invitations/${CODE}`)));
    await assertFails(
      deleteDoc(doc(dbAs(PRIMARY), `invitationOwners/${UNLINKED_CAREGIVER}`)),
    );
    const db = dbAs(UNLINKED_CAREGIVER);
    await assertSucceeds(deleteDoc(doc(db, `invitations/${CODE}`)));
    await assertSucceeds(
      deleteDoc(doc(db, `invitationOwners/${UNLINKED_CAREGIVER}`)),
    );
  });
});
