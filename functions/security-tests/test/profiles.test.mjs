// Firestore security rules tests: Profiles, signed-out visitors, accounts without a profile and the single kind of account.
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

describe("identity: signed-out visitors", () => {
  it("cannot read profiles, routines, links or invitations", async () => {
    const db = anonymousDb();
    await assertFails(getDoc(doc(db, `users/${PRIMARY}`)));
    await assertFails(getDocs(collection(db, `users/${PRIMARY}/routineTasks`)));
    await assertFails(getDoc(doc(db, `careLinks/${CAREGIVER}_${PRIMARY}`)));
    await assertFails(getDoc(doc(db, "invitations/ABCDEFGH")));
  });
});

describe("profiles", () => {
  const newProfile = () => ({
    displayName: "Nova",
    createdAt: serverTimestamp(),
  });

  it("an account creates its own profile with just its name", async () => {
    await assertSucceeds(
      setDoc(doc(dbAs(NO_PROFILE), `users/${NO_PROFILE}`), newProfile()),
    );
  });

  it("a new profile has no role, not even one of the old ones", async () => {
    for (const role of ["primary_user", "caregiver", "admin"]) {
      await assertFails(
        setDoc(doc(dbAs(NO_PROFILE), `users/${NO_PROFILE}`), {
          ...newProfile(),
          role,
        }),
      );
    }
  });

  it("a profile needs a name", async () => {
    await assertFails(
      setDoc(doc(dbAs(NO_PROFILE), `users/${NO_PROFILE}`), {
        createdAt: serverTimestamp(),
      }),
    );
  });

  it("an account cannot create a profile for someone else", async () => {
    await assertFails(
      setDoc(doc(dbAs(NO_PROFILE), "users/someoneElse"), newProfile()),
    );
  });

  it("extra fields cannot be smuggled into a profile", async () => {
    const withExtra = { ...newProfile(), isAdmin: true };
    await assertFails(
      setDoc(doc(dbAs(NO_PROFILE), `users/${NO_PROFILE}`), withExtra),
    );
  });

  it("an older profile keeps its role unchanged, and its name can change", async () => {
    // The seeded profiles have the role that profiles had before.
    const db = dbAs(PRIMARY);
    await assertFails(
      updateDoc(doc(db, `users/${PRIMARY}`), { role: "caregiver" }),
    );
    await assertFails(setDoc(doc(db, `users/${PRIMARY}`), newProfile()));
    await assertSucceeds(
      updateDoc(doc(db, `users/${PRIMARY}`), { displayName: "Patrícia" }),
    );
  });

  it("only the owner deletes a profile (deleting the account, LGPD art. 18)", async () => {
    await assertFails(deleteDoc(doc(dbAs(OTHER_PRIMARY), `users/${PRIMARY}`)));
    await assertSucceeds(deleteDoc(doc(dbAs(PRIMARY), `users/${PRIMARY}`)));
  });

  it("profiles of unrelated people are private; linked people can see each other", async () => {
    await assertFails(getDoc(doc(dbAs(OTHER_PRIMARY), `users/${PRIMARY}`)));
    await assertFails(
      getDoc(doc(dbAs(UNLINKED_CAREGIVER), `users/${PRIMARY}`)),
    );
    await assertSucceeds(getDoc(doc(dbAs(CAREGIVER), `users/${PRIMARY}`)));
    await assertSucceeds(getDoc(doc(dbAs(PRIMARY), `users/${CAREGIVER}`)));
  });
});

describe("one kind of account", () => {
  it("whatever its stored role, an account owns its own routine", async () => {
    const db = dbAs(CAREGIVER);
    await assertSucceeds(
      setDoc(
        doc(db, `users/${CAREGIVER}/routineTasks/own`),
        newTask(CAREGIVER),
      ),
    );
    await assertSucceeds(
      getDocs(
        query(collection(db, `users/${CAREGIVER}/routineTasks`), limit(10)),
      ),
    );
  });

  it("whatever its stored role, an account owns its boards and messages", async () => {
    const db = dbAs(CAREGIVER);
    await assertSucceeds(
      getDocs(query(collection(db, `users/${CAREGIVER}/aacBoards`), limit(10))),
    );
    await assertSucceeds(
      getDocs(
        query(collection(db, `users/${CAREGIVER}/aacSavedMessages`), limit(10)),
      ),
    );
  });

  it("supporting someone comes from the link, not from a role", async () => {
    await testEnv().withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), `careLinks/${OTHER_PRIMARY}_${PRIMARY}`),
        link(OTHER_PRIMARY, "active", {
          viewRoutines: true,
          manageRoutines: false,
        }),
      );
    });

    const db = dbAs(OTHER_PRIMARY);
    await assertSucceeds(
      getDoc(doc(db, `users/${PRIMARY}/routineTasks/selfDone`)),
    );
    await assertFails(
      setDoc(
        doc(db, `users/${PRIMARY}/routineTasks/new`),
        newTask(OTHER_PRIMARY),
      ),
    );
  });

  it("without a link, being a caregiver opens nothing", async () => {
    await assertFails(
      getDoc(
        doc(dbAs(UNLINKED_CAREGIVER), `users/${PRIMARY}/routineTasks/selfDone`),
      ),
    );
  });
});

describe("accounts without profile", () => {
  it("have no access to routines, even their own path", async () => {
    await assertFails(
      getDocs(collection(dbAs(NO_PROFILE), `users/${NO_PROFILE}/routineTasks`)),
    );
  });

  it("an active link is useless without a caregiver profile", async () => {
    await assertFails(
      getDocs(activeTasksOf(dbAs(NO_PROFILE_CAREGIVER), PRIMARY)),
    );
  });
});
