// Firestore security rules tests: Photos of symbols and of the profile.
// They run against the local emulator, so they verify the same rules file that is deployed.
import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import {
  PRIMARY,
  OTHER_PRIMARY,
  CAREGIVER,
  VIEW_ONLY_CAREGIVER,
  REVOKED_CAREGIVER,
  UNLINKED_CAREGIVER,
  NO_PROFILE_CAREGIVER,
  NO_PROFILE,
  DAY,
  task,
  newTask,
  link,
  seed,
  dbAs,
  anonymousDb,
  activeTasksOf,
  routineOf,
  linksOf,
  completion,
  undoCompletion,
  dayOf,
  testEnv,
} from "./support/rulesFixture.mjs";
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

describe("photos", () => {
  const LIMIT = 102400;
  const photo = (size = 1000, overrides = {}) => ({
    data: Bytes.fromUint8Array(new Uint8Array(size)),
    updatedAt: serverTimestamp(),
    ...overrides,
  });
  const path = (id = "abc-123.jpg") => `users/${PRIMARY}/photos/${id}`;
  const seed = (id) =>
    testEnv().withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), path(id)), {
        data: Bytes.fromUint8Array(new Uint8Array(10)),
        updatedAt: new Date(0),
      });
    });

  it("the person saves, replaces and deletes their own", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(setDoc(doc(db, path()), photo()));
    await assertSucceeds(setDoc(doc(db, path()), photo(2000)));
    await assertSucceeds(deleteDoc(doc(db, path())));
  });

  it("refuses a photo over the limit, and accepts exactly the limit", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(setDoc(doc(db, path()), photo(LIMIT)));
    await assertFails(setDoc(doc(db, path()), photo(LIMIT + 1)));
    await assertFails(setDoc(doc(db, path()), photo(0)));
  });

  it("refuses anything that is not a photo", async () => {
    const db = dbAs(PRIMARY);
    await assertFails(setDoc(doc(db, path()), photo(10, { data: "abc" })));
    await assertFails(setDoc(doc(db, path()), photo(10, { extra: 1 })));
    await assertFails(setDoc(doc(db, path("a b.jpg")), photo()));
    await assertFails(setDoc(doc(db, path("video.mp4")), photo()));
    await assertFails(
      setDoc(doc(db, path()), photo(10, { updatedAt: new Date(0) })),
    );
  });

  it("a caregiver who manages the boards sees symbol photos only, and changes nothing", async () => {
    await testEnv().withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), `careLinks/${CAREGIVER}_${PRIMARY}`),
        link(CAREGIVER, "active", {
          viewRoutines: true,
          manageRoutines: true,
          manageCommunication: true,
        }),
      );
    });
    await seed("abc-123.jpg");
    await seed("profile.jpg");
    const db = dbAs(CAREGIVER);
    await assertSucceeds(getDoc(doc(db, path("abc-123.jpg"))));
    await assertFails(getDoc(doc(db, path("profile.jpg"))));
    await assertFails(setDoc(doc(db, path("new.jpg")), photo()));
    await assertFails(deleteDoc(doc(db, path("abc-123.jpg"))));
    await assertFails(
      getDocs(query(collection(db, `users/${PRIMARY}/photos`), limit(10))),
    );
  });

  it("without permission or link, no photo is visible", async () => {
    await seed("abc-123.jpg");
    await assertFails(getDoc(doc(dbAs(VIEW_ONLY_CAREGIVER), path())));
    await assertFails(getDoc(doc(dbAs(UNLINKED_CAREGIVER), path())));
    await assertFails(getDoc(doc(dbAs(OTHER_PRIMARY), path())));
  });

  it("the person can count their photos, but lists are bounded", async () => {
    const all = collection(dbAs(PRIMARY), `users/${PRIMARY}/photos`);
    await assertSucceeds(getCountFromServer(query(all, limit(101))));
    await assertFails(getDocs(query(all, limit(102))));
    await assertFails(getDocs(all));
  });
});
