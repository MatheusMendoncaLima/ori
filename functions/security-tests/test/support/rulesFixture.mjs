// Shared setup of the Firestore rules tests: the people, their links, the seed data and the helpers.
// Importing it starts the emulator environment for the test file (before/after hooks).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, it } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Bytes,
  collection,
  deleteDoc,
  doc,
  getCountFromServer,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";

export const PRIMARY = "pat"; // primary user with a routine
export const OTHER_PRIMARY = "otto"; // primary user with no link to anyone
export const CAREGIVER = "cara"; // active link to pat, may view and manage
export const VIEW_ONLY_CAREGIVER = "viv"; // active link to pat, may only view
export const REVOKED_CAREGIVER = "rex"; // link to pat was revoked
export const UNLINKED_CAREGIVER = "carl"; // caregiver profile, no link
export const NO_PROFILE_CAREGIVER = "ghost"; // active link but no profile document
export const NO_PROFILE = "nobody"; // authenticated account without profile

let env;

/** The emulator environment, created before the tests of each file. */
export const testEnv = () => env;

export const DAY = "2026-10-29";

export const task = (overrides = {}) => ({
  title: "Tomar café",
  date: DAY,
  scheduledTime: "07:30",
  description: null,
  status: "active",
  ownerCanComplete: true,
  isDone: false,
  completedAt: null,
  completedBy: null,
  createdBy: CAREGIVER,
  createdAt: new Date(0),
  updatedAt: new Date(0),
  ...overrides,
});

// A new activity as the app sends it: server times and an empty completion.
export const newTask = (author, overrides = {}) =>
  task({
    createdBy: author,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  });

export const link = (caregiverUid, status, permissions) => ({
  caregiverUid,
  primaryUserUid: PRIMARY,
  caregiverName: caregiverUid,
  primaryUserName: "Pat",
  status,
  permissions,
  createdAt: new Date(0),
  revokedAt: null,
});

export async function seed() {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const profile = (role, name) => ({
      role,
      displayName: name,
      createdAt: new Date(0),
    });
    await setDoc(doc(db, `users/${PRIMARY}`), profile("primary_user", "Pat"));
    await setDoc(
      doc(db, `users/${OTHER_PRIMARY}`),
      profile("primary_user", "Otto"),
    );
    for (const uid of [
      CAREGIVER,
      VIEW_ONLY_CAREGIVER,
      REVOKED_CAREGIVER,
      UNLINKED_CAREGIVER,
    ]) {
      await setDoc(doc(db, `users/${uid}`), profile("caregiver", uid));
    }
    const all = { viewRoutines: true, manageRoutines: true };
    await setDoc(
      doc(db, `careLinks/${CAREGIVER}_${PRIMARY}`),
      link(CAREGIVER, "active", all),
    );
    await setDoc(
      doc(db, `careLinks/${VIEW_ONLY_CAREGIVER}_${PRIMARY}`),
      link(VIEW_ONLY_CAREGIVER, "active", {
        viewRoutines: true,
        manageRoutines: false,
      }),
    );
    await setDoc(
      doc(db, `careLinks/${REVOKED_CAREGIVER}_${PRIMARY}`),
      link(REVOKED_CAREGIVER, "revoked", all),
    );
    await setDoc(
      doc(db, `careLinks/${NO_PROFILE_CAREGIVER}_${PRIMARY}`),
      link(NO_PROFILE_CAREGIVER, "active", all),
    );
    await setDoc(doc(db, `users/${PRIMARY}/routineTasks/selfDone`), task());
    await setDoc(
      doc(db, `users/${PRIMARY}/routineTasks/caregiverOnly`),
      task({ ownerCanComplete: false }),
    );
    await setDoc(
      doc(db, `users/${PRIMARY}/routineTasks/private`),
      task({ status: "hidden_from_caregiver" }),
    );
    await setDoc(
      doc(db, `users/${OTHER_PRIMARY}/routineTasks/ottoTask`),
      task(),
    );
    await setDoc(doc(db, "invitations/ABCDEFGH"), {
      caregiverUid: CAREGIVER,
      status: "pending",
    });
  });
}

export const dbAs = (uid) => env.authenticatedContext(uid).firestore();
export const anonymousDb = () => env.unauthenticatedContext().firestore();
// The app always bounds list queries; the rules reject the ones that are not bounded.
export const activeTasksOf = (db, ownerUid) =>
  query(
    collection(db, `users/${ownerUid}/routineTasks`),
    where("status", "==", "active"),
    limit(100),
  );
export const routineOf = (db, ownerUid, max = 100) =>
  query(collection(db, `users/${ownerUid}/routineTasks`), limit(max));
export const linksOf = (db, primaryUserUid, max) => {
  const base = collection(db, "careLinks");
  const byMember = where("primaryUserUid", "==", primaryUserUid);
  return max === undefined
    ? query(base, byMember)
    : query(base, byMember, limit(max));
};
export const completion = (uid) => ({
  isDone: true,
  completedBy: uid,
  completedAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
});
export const undoCompletion = () => ({
  isDone: false,
  completedBy: null,
  completedAt: null,
  updatedAt: serverTimestamp(),
});
// Day query exactly as the app builds it (equality filters + limit).
export const dayOf = (db, ownerUid, date, { activeOnly = false } = {}) =>
  query(
    collection(db, `users/${ownerUid}/routineTasks`),
    ...[where("date", "==", date)],
    ...(activeOnly ? [where("status", "==", "active")] : []),
    limit(100),
  );

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-ori",
    firestore: {
      rules: readFileSync(
        new URL("../../../firestore.rules", import.meta.url),
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
  await seed();
});
