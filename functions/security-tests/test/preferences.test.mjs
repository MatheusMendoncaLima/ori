// Preferences that follow the account: personal, and limited to known fields and values.
import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, it } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";

const PERSON = "pat";
const CAREGIVER = "cara"; // active link to pat with every permission
const STRANGER = "otto";
const DOCUMENT = `users/${PERSON}/settings/preferences`;

let env;

const preferences = (overrides = {}) => ({
  textScale: "large",
  theme: "dark",
  simplified: true,
  motion: "reduced",
  updatedAt: serverTimestamp(),
  ...overrides,
});

const dbAs = (uid) => env.authenticatedContext(uid).firestore();

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-ori",
    firestore: {
      rules: readFileSync(
        new URL("../../firestore.rules", import.meta.url),
        "utf8",
      ),
    },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, `users/${PERSON}`), {
      role: "primary_user",
      displayName: "Pat",
      createdAt: new Date(0),
    });
    await setDoc(doc(db, `users/${CAREGIVER}`), {
      role: "caregiver",
      displayName: "Cara",
      createdAt: new Date(0),
    });
    await setDoc(doc(db, `careLinks/${CAREGIVER}_${PERSON}`), {
      caregiverUid: CAREGIVER,
      primaryUserUid: PERSON,
      status: "active",
      permissions: {
        viewRoutines: true,
        manageRoutines: true,
        manageCommunication: true,
      },
    });
  });
});

describe("account preferences", () => {
  it("the person creates, reads and changes their own", async () => {
    const db = dbAs(PERSON);
    await assertSucceeds(setDoc(doc(db, DOCUMENT), preferences()));
    await assertSucceeds(getDoc(doc(db, DOCUMENT)));
    await assertSucceeds(
      setDoc(doc(db, DOCUMENT), preferences({ theme: "light" })),
    );
  });

  it("an account with no preferences yet can read that there are none", async () => {
    const snapshot = await assertSucceeds(getDoc(doc(dbAs(PERSON), DOCUMENT)));
    if (snapshot.exists()) throw new Error("expected no document");
  });

  it("nobody else reads or changes them, not even a linked caregiver", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), DOCUMENT),
        preferences({ updatedAt: new Date(0) }),
      );
    });
    for (const uid of [CAREGIVER, STRANGER]) {
      await assertFails(getDoc(doc(dbAs(uid), DOCUMENT)));
      await assertFails(setDoc(doc(dbAs(uid), DOCUMENT), preferences()));
    }
    await assertFails(
      getDoc(doc(env.unauthenticatedContext().firestore(), DOCUMENT)),
    );
  });

  it("only known fields and values are accepted", async () => {
    const reference = doc(dbAs(PERSON), DOCUMENT);
    const invalid = [
      { textScale: "gigantic" },
      { theme: "neon" },
      { simplified: "yes" },
      { motion: "fast" },
      { extra: true },
      { updatedAt: new Date(0) },
    ];
    for (const overrides of invalid) {
      await assertFails(setDoc(reference, preferences(overrides)));
    }
    const { motion, ...missingField } = preferences();
    await assertFails(setDoc(reference, missingField));
    await assertSucceeds(setDoc(reference, { ...missingField, motion }));
  });

  it("only one document exists, it cannot be listed or deleted", async () => {
    const db = dbAs(PERSON);
    await assertFails(
      setDoc(doc(db, `users/${PERSON}/settings/other`), preferences()),
    );
    await assertSucceeds(setDoc(doc(db, DOCUMENT), preferences()));
    await assertFails(
      getDocs(query(collection(db, `users/${PERSON}/settings`), limit(1))),
    );
    await assertFails(deleteDoc(doc(db, DOCUMENT)));
  });
});
