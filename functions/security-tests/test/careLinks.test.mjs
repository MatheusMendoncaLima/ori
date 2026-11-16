// Firestore security rules tests: Care links and what a caregiver may see and do with them.
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

describe("caregiver with an active link", () => {
  it("reads active tasks but not tasks hidden by the primary user", async () => {
    const db = dbAs(CAREGIVER);
    await assertSucceeds(getDocs(activeTasksOf(db, PRIMARY)));
    await assertFails(getDoc(doc(db, `users/${PRIMARY}/routineTasks/private`)));
    // A (bounded) query that could return hidden tasks is rejected as a whole.
    await assertFails(getDocs(routineOf(db, PRIMARY)));
  });

  it("creates tasks only as their real author", async () => {
    const db = dbAs(CAREGIVER);
    const fresh = newTask(CAREGIVER);
    await assertSucceeds(
      setDoc(doc(db, `users/${PRIMARY}/routineTasks/new`), fresh),
    );
    await assertFails(
      setDoc(doc(db, `users/${PRIMARY}/routineTasks/forged`), {
        ...fresh,
        createdBy: PRIMARY,
      }),
    );
  });

  it("edits a task the primary user already completed without rewriting the completion", async () => {
    await updateDoc(
      doc(dbAs(PRIMARY), `users/${PRIMARY}/routineTasks/selfDone`),
      completion(PRIMARY),
    );
    await assertSucceeds(
      updateDoc(
        doc(dbAs(CAREGIVER), `users/${PRIMARY}/routineTasks/selfDone`),
        { title: "Café da manhã", updatedAt: serverTimestamp() },
      ),
    );
  });

  it("cannot edit or delete a task hidden from caregivers", async () => {
    const taskRef = doc(
      dbAs(CAREGIVER),
      `users/${PRIMARY}/routineTasks/private`,
    );
    await assertFails(
      updateDoc(taskRef, { title: "Espiar", updatedAt: serverTimestamp() }),
    );
    await assertFails(deleteDoc(taskRef));
  });

  it("cannot reach people they are not linked to", async () => {
    await assertFails(getDocs(activeTasksOf(dbAs(CAREGIVER), OTHER_PRIMARY)));
    await assertFails(
      getDocs(activeTasksOf(dbAs(UNLINKED_CAREGIVER), PRIMARY)),
    );
  });
});

describe("caregiver permissions", () => {
  it("view-only permission allows reading but not managing", async () => {
    const db = dbAs(VIEW_ONLY_CAREGIVER);
    await assertSucceeds(getDocs(activeTasksOf(db, PRIMARY)));
    await assertFails(
      setDoc(
        doc(db, `users/${PRIMARY}/routineTasks/new`),
        newTask(VIEW_ONLY_CAREGIVER),
      ),
    );
    await assertFails(
      deleteDoc(doc(db, `users/${PRIMARY}/routineTasks/selfDone`)),
    );
  });

  it("a caregiver cannot grant permissions to themselves", async () => {
    const linkRef = doc(
      dbAs(VIEW_ONLY_CAREGIVER),
      `careLinks/${VIEW_ONLY_CAREGIVER}_${PRIMARY}`,
    );
    await assertFails(
      updateDoc(linkRef, {
        permissions: { viewRoutines: true, manageRoutines: true },
      }),
    );
  });

  it("the primary user grants and removes permissions", async () => {
    const linkRef = doc(
      dbAs(PRIMARY),
      `careLinks/${VIEW_ONLY_CAREGIVER}_${PRIMARY}`,
    );
    await assertSucceeds(
      updateDoc(linkRef, {
        permissions: { viewRoutines: true, manageRoutines: true },
      }),
    );
    await assertFails(
      updateDoc(linkRef, {
        permissions: { viewRoutines: true, deleteAccount: true },
      }),
    );
  });
});

describe("care links", () => {
  it("clients cannot create links, even between themselves", async () => {
    const forged = link(UNLINKED_CAREGIVER, "active", {
      viewRoutines: true,
      manageRoutines: true,
    });
    await assertFails(
      setDoc(
        doc(
          dbAs(UNLINKED_CAREGIVER),
          `careLinks/${UNLINKED_CAREGIVER}_${PRIMARY}`,
        ),
        forged,
      ),
    );
    await assertFails(
      setDoc(
        doc(dbAs(PRIMARY), `careLinks/${UNLINKED_CAREGIVER}_${PRIMARY}`),
        forged,
      ),
    );
  });

  it("link lists must be bounded", async () => {
    await assertFails(getDocs(linksOf(dbAs(PRIMARY), PRIMARY)));
    await assertSucceeds(getDocs(linksOf(dbAs(PRIMARY), PRIMARY, 50)));
    await assertFails(getDocs(linksOf(dbAs(PRIMARY), PRIMARY, 51)));
  });

  it("a caregiver lists their own active links with the app's query", async () => {
    // Same filters as FirestoreCareLinkRepository.observeActiveLinks.
    const activeLinksOf = (db, caregiverUid, ...bound) =>
      query(
        collection(db, "careLinks"),
        where("caregiverUid", "==", caregiverUid),
        where("status", "==", "active"),
        ...bound,
      );

    const links = await assertSucceeds(
      getDocs(activeLinksOf(dbAs(CAREGIVER), CAREGIVER, limit(50))),
    );
    assert.equal(links.size, 1);
    // Without a limit, as older app versions sent it, the read is refused.
    await assertFails(getDocs(activeLinksOf(dbAs(CAREGIVER), CAREGIVER)));
    await assertFails(
      getDocs(activeLinksOf(dbAs(CAREGIVER), CAREGIVER, limit(51))),
    );
    // Nobody lists the links of another caregiver.
    await assertFails(
      getDocs(activeLinksOf(dbAs(CAREGIVER), VIEW_ONLY_CAREGIVER, limit(50))),
    );
  });

  it("only the two linked people can read a link", async () => {
    await assertSucceeds(
      getDoc(doc(dbAs(PRIMARY), `careLinks/${CAREGIVER}_${PRIMARY}`)),
    );
    await assertSucceeds(
      getDoc(doc(dbAs(CAREGIVER), `careLinks/${CAREGIVER}_${PRIMARY}`)),
    );
    await assertFails(
      getDoc(doc(dbAs(OTHER_PRIMARY), `careLinks/${CAREGIVER}_${PRIMARY}`)),
    );
  });

  it("revocation cuts access immediately and cannot be undone by the client", async () => {
    const linkRef = doc(dbAs(CAREGIVER), `careLinks/${CAREGIVER}_${PRIMARY}`);
    await assertSucceeds(
      updateDoc(linkRef, { status: "revoked", revokedAt: serverTimestamp() }),
    );
    await assertFails(getDocs(activeTasksOf(dbAs(CAREGIVER), PRIMARY)));
    await assertFails(
      updateDoc(linkRef, { status: "active", revokedAt: null }),
    );
  });

  it("a previously revoked caregiver has no access", async () => {
    await assertFails(getDocs(activeTasksOf(dbAs(REVOKED_CAREGIVER), PRIMARY)));
  });

  it("outsiders cannot revoke someone else's link", async () => {
    const linkRef = doc(
      dbAs(OTHER_PRIMARY),
      `careLinks/${CAREGIVER}_${PRIMARY}`,
    );
    await assertFails(
      updateDoc(linkRef, { status: "revoked", revokedAt: serverTimestamp() }),
    );
  });
});
