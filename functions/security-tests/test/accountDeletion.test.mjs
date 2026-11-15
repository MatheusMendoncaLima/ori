// Firestore security rules tests: the consent record and deleting the account from the app (LGPD art. 11, 14
// and 18). They run against the local emulator, so they verify the same rules file that is deployed.
import { describe, it } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import {
  PRIMARY,
  CAREGIVER,
  REVOKED_CAREGIVER,
  OTHER_PRIMARY,
  dbAs,
  testEnv,
} from "./support/rulesFixture.mjs";
import {
  deleteDoc,
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  updateDoc,
} from "firebase/firestore";

const CONSENT = `users/${PRIMARY}/legal/consent`;

const consent = (overrides = {}) => ({
  version: 1,
  isAdult: true,
  guardianConsent: false,
  acceptedAt: serverTimestamp(),
  ...overrides,
});

describe("consent record", () => {
  it("the person records, reads, updates and deletes their own", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(setDoc(doc(db, CONSENT), consent()));
    await assertSucceeds(getDoc(doc(db, CONSENT)));
    await assertSucceeds(setDoc(doc(db, CONSENT), consent({ version: 2 })));
    await assertSucceeds(deleteDoc(doc(db, CONSENT)));
  });

  it("an account without a profile records it before creating the profile", async () => {
    const db = dbAs("brand-new");
    await assertSucceeds(
      setDoc(doc(db, "users/brand-new/legal/consent"), consent()),
    );
  });

  it("under 18 only with the guardian, and nothing else is accepted", async () => {
    const db = dbAs(PRIMARY);
    await assertFails(setDoc(doc(db, CONSENT), consent({ isAdult: false })));
    await assertSucceeds(
      setDoc(
        doc(db, CONSENT),
        consent({ isAdult: false, guardianConsent: true }),
      ),
    );
    await assertFails(setDoc(doc(db, CONSENT), consent({ version: 0 })));
    await assertFails(setDoc(doc(db, CONSENT), consent({ extra: 1 })));
    await assertFails(
      setDoc(doc(db, CONSENT), consent({ acceptedAt: new Date(0) })),
    );
    await assertFails(
      setDoc(doc(db, `users/${PRIMARY}/legal/other`), consent()),
    );
  });

  it("is private, even from a linked caregiver", async () => {
    await testEnv().withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), CONSENT), {
        version: 1,
        isAdult: true,
        guardianConsent: false,
        acceptedAt: new Date(0),
      });
    });
    await assertFails(getDoc(doc(dbAs(CAREGIVER), CONSENT)));
    await assertFails(deleteDoc(doc(dbAs(CAREGIVER), CONSENT)));
    await assertFails(setDoc(doc(dbAs(OTHER_PRIMARY), CONSENT), consent()));
  });
});

describe("deleting the account", () => {
  const reserved = `users/${PRIMARY}/routineTasks/caregiverOnly`;

  it("an activity a caregiver reserved goes only once the account is being deleted", async () => {
    const db = dbAs(PRIMARY);
    await assertFails(deleteDoc(doc(db, reserved)));

    await assertSucceeds(
      updateDoc(doc(db, `users/${PRIMARY}`), { deletingAt: serverTimestamp() }),
    );
    await assertSucceeds(deleteDoc(doc(db, reserved)));
  });

  it("only the owner marks the profile, and only with the time of the request", async () => {
    await assertFails(
      updateDoc(doc(dbAs(CAREGIVER), `users/${PRIMARY}`), {
        deletingAt: serverTimestamp(),
      }),
    );
    await assertFails(
      updateDoc(doc(dbAs(PRIMARY), `users/${PRIMARY}`), {
        deletingAt: new Date(0),
      }),
    );
    await assertFails(
      updateDoc(doc(dbAs(PRIMARY), `users/${PRIMARY}`), {
        deletingAt: serverTimestamp(),
        displayName: "Outro",
      }),
    );
  });

  it("the owner deletes the account preferences and the profile, nobody else", async () => {
    await testEnv().withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), `users/${PRIMARY}/settings/preferences`),
        { textScale: "normal" },
      );
    });
    await assertFails(
      deleteDoc(doc(dbAs(CAREGIVER), `users/${PRIMARY}/settings/preferences`)),
    );
    await assertSucceeds(
      deleteDoc(doc(dbAs(PRIMARY), `users/${PRIMARY}/settings/preferences`)),
    );
    await assertFails(deleteDoc(doc(dbAs(CAREGIVER), `users/${PRIMARY}`)));
    await assertSucceeds(deleteDoc(doc(dbAs(PRIMARY), `users/${PRIMARY}`)));
  });

  it("a link is removed only after it ended, by one of its two people", async () => {
    const active = `careLinks/${CAREGIVER}_${PRIMARY}`;
    const ended = `careLinks/${REVOKED_CAREGIVER}_${PRIMARY}`;
    await assertFails(deleteDoc(doc(dbAs(PRIMARY), active)));
    await assertFails(deleteDoc(doc(dbAs(OTHER_PRIMARY), ended)));
    await assertSucceeds(deleteDoc(doc(dbAs(PRIMARY), ended)));

    await assertSucceeds(
      updateDoc(doc(dbAs(CAREGIVER), active), {
        status: "revoked",
        revokedAt: serverTimestamp(),
      }),
    );
    await assertSucceeds(deleteDoc(doc(dbAs(CAREGIVER), active)));
  });
});
