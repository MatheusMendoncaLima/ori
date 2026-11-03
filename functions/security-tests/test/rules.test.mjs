// Firestore security rules tests. They run against the local emulator, so they
// verify the same rules file that is deployed, independently of any app UI.
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

const PRIMARY = "pat"; // primary user with a routine
const OTHER_PRIMARY = "otto"; // primary user with no link to anyone
const CAREGIVER = "cara"; // active link to pat, may view and manage
const VIEW_ONLY_CAREGIVER = "viv"; // active link to pat, may only view
const REVOKED_CAREGIVER = "rex"; // link to pat was revoked
const UNLINKED_CAREGIVER = "carl"; // caregiver profile, no link
const NO_PROFILE_CAREGIVER = "ghost"; // active link but no profile document
const NO_PROFILE = "nobody"; // authenticated account without profile

let env;

const DAY = "2026-10-29";

const task = (overrides = {}) => ({
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
const newTask = (author, overrides = {}) =>
  task({
    createdBy: author,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  });

const link = (caregiverUid, status, permissions) => ({
  caregiverUid,
  primaryUserUid: PRIMARY,
  caregiverName: caregiverUid,
  primaryUserName: "Pat",
  status,
  permissions,
  createdAt: new Date(0),
  revokedAt: null,
});

async function seed() {
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

const dbAs = (uid) => env.authenticatedContext(uid).firestore();
const anonymousDb = () => env.unauthenticatedContext().firestore();
// The app always bounds list queries; the rules reject the ones that are not bounded.
const activeTasksOf = (db, ownerUid) =>
  query(
    collection(db, `users/${ownerUid}/routineTasks`),
    where("status", "==", "active"),
    limit(100),
  );
const routineOf = (db, ownerUid, max = 100) =>
  query(collection(db, `users/${ownerUid}/routineTasks`), limit(max));
const linksOf = (db, primaryUserUid, max) => {
  const base = collection(db, "careLinks");
  const byMember = where("primaryUserUid", "==", primaryUserUid);
  return max === undefined
    ? query(base, byMember)
    : query(base, byMember, limit(max));
};
const completion = (uid) => ({
  isDone: true,
  completedBy: uid,
  completedAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
});
const undoCompletion = () => ({
  isDone: false,
  completedBy: null,
  completedAt: null,
  updatedAt: serverTimestamp(),
});
// Day query exactly as the app builds it (equality filters + limit).
const dayOf = (db, ownerUid, date, { activeOnly = false } = {}) =>
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
  await seed();
});

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

  it("a profile cannot be deleted by the client", async () => {
    await assertFails(deleteDoc(doc(dbAs(PRIMARY), `users/${PRIMARY}`)));
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
    await env.withSecurityRulesDisabled(async (context) => {
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

describe("repeating activities", () => {
  const series = (author, overrides = {}) => ({
    title: "Terapia",
    date: "2026-11-02",
    scheduledTime: null,
    description: null,
    status: "active",
    ownerCanComplete: true,
    repeat: "weekly:1,3:2",
    skippedDates: [],
    createdBy: author,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  });
  const path = `users/${PRIMARY}/routineSeries/s1`;
  const stored = (overrides = {}) => ({
    ...series(PRIMARY),
    createdAt: new Date(0),
    updatedAt: new Date(0),
    ...overrides,
  });
  const seed = (overrides = {}) =>
    env.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), path), stored(overrides));
    });

  it("the person creates, edits and deletes their own", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(setDoc(doc(db, path), series(PRIMARY)));
    await assertSucceeds(
      updateDoc(doc(db, path), {
        repeat: "last_business_day",
        skippedDates: ["2026-11-09"],
        updatedAt: serverTimestamp(),
      }),
    );
    await assertSucceeds(deleteDoc(doc(db, path)));
  });

  it("refuses a rule it does not know and unknown fields", async () => {
    const db = dbAs(PRIMARY);
    for (const repeat of [
      "yearly",
      "weekly:8:1",
      "weekly:1:9",
      "weekly::1",
      "",
    ]) {
      await assertFails(setDoc(doc(db, path), series(PRIMARY, { repeat })));
    }
    await assertFails(setDoc(doc(db, path), series(PRIMARY, { extra: 1 })));
    await assertFails(
      setDoc(doc(db, path), series(PRIMARY, { skippedDates: "2026-11-09" })),
    );
  });

  it("cannot be authored in someone else's name", async () => {
    await assertFails(setDoc(doc(dbAs(PRIMARY), path), series(CAREGIVER)));
  });

  it("an activity becomes a series, and back, in one batch", async () => {
    const db = dbAs(PRIMARY);
    const task = doc(db, `users/${PRIMARY}/routineTasks/s1`);
    await assertSucceeds(setDoc(task, newTask(PRIMARY)));

    const toSeries = writeBatch(db);
    toSeries.set(doc(db, path), series(PRIMARY));
    toSeries.delete(task);
    await assertSucceeds(toSeries.commit());

    const toTask = writeBatch(db);
    toTask.set(task, newTask(PRIMARY));
    toTask.delete(doc(db, path));
    await assertSucceeds(toTask.commit());
  });

  it("lists must be bounded", async () => {
    const db = dbAs(PRIMARY);
    const all = collection(db, `users/${PRIMARY}/routineSeries`);
    await assertSucceeds(getDocs(query(all, limit(50))));
    await assertFails(getDocs(query(all, limit(51))));
    await assertFails(getDocs(all));
  });

  it("a caregiver with permission manages visible ones only", async () => {
    const db = dbAs(CAREGIVER);
    await assertSucceeds(setDoc(doc(db, path), series(CAREGIVER)));
    await assertFails(
      setDoc(
        doc(db, `users/${PRIMARY}/routineSeries/hidden`),
        series(CAREGIVER, { status: "hidden_from_caregiver" }),
      ),
    );
    await seed({ status: "hidden_from_caregiver" });
    await assertFails(getDoc(doc(db, path)));
    await assertFails(deleteDoc(doc(db, path)));
    const all = collection(db, `users/${PRIMARY}/routineSeries`);
    await assertFails(getDocs(query(all, limit(50))));
    await assertSucceeds(
      getDocs(query(all, where("status", "==", "active"), limit(50))),
    );
  });

  it("view-only sees but does not change; revoked and strangers see nothing", async () => {
    await seed();
    const viewOnly = dbAs(VIEW_ONLY_CAREGIVER);
    await assertSucceeds(getDoc(doc(viewOnly, path)));
    await assertFails(deleteDoc(doc(viewOnly, path)));
    await assertFails(getDoc(doc(dbAs(REVOKED_CAREGIVER), path)));
    await assertFails(getDoc(doc(dbAs(UNLINKED_CAREGIVER), path)));
  });

  it("the person cannot change one a caregiver reserved", async () => {
    await seed({ createdBy: CAREGIVER });
    await assertFails(deleteDoc(doc(dbAs(PRIMARY), path)));
  });
});

describe("photos", () => {
  const LIMIT = 102400;
  const photo = (size = 1000, overrides = {}) => ({
    data: Bytes.fromUint8Array(new Uint8Array(size)),
    updatedAt: serverTimestamp(),
    ...overrides,
  });
  const path = (id = "abc-123.jpg") => `users/${PRIMARY}/photos/${id}`;
  const seed = (id) =>
    env.withSecurityRulesDisabled(async (context) => {
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
    await env.withSecurityRulesDisabled(async (context) => {
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

describe("primary user", () => {
  it("reads the whole own routine", async () => {
    await assertSucceeds(getDocs(routineOf(dbAs(PRIMARY), PRIMARY)));
  });

  it("unbounded or oversized routine queries are rejected", async () => {
    const db = dbAs(PRIMARY);
    await assertFails(getDocs(collection(db, `users/${PRIMARY}/routineTasks`)));
    await assertFails(getDocs(routineOf(db, PRIMARY, 101)));
  });

  it("marks and unmarks an authorized task as done", async () => {
    const taskRef = doc(
      dbAs(PRIMARY),
      `users/${PRIMARY}/routineTasks/selfDone`,
    );
    await assertSucceeds(updateDoc(taskRef, completion(PRIMARY)));
    await assertSucceeds(updateDoc(taskRef, undoCompletion()));
  });

  it("cannot complete a task reserved to the caregiver", async () => {
    await assertFails(
      updateDoc(
        doc(dbAs(PRIMARY), `users/${PRIMARY}/routineTasks/caregiverOnly`),
        completion(PRIMARY),
      ),
    );
  });

  it("cannot record a completion in someone else's name", async () => {
    await assertFails(
      updateDoc(
        doc(dbAs(PRIMARY), `users/${PRIMARY}/routineTasks/selfDone`),
        completion(CAREGIVER),
      ),
    );
  });

  it("cannot edit or delete activities reserved to the caregiver", async () => {
    const db = dbAs(PRIMARY);
    const reserved = doc(db, `users/${PRIMARY}/routineTasks/caregiverOnly`);
    await assertFails(
      updateDoc(reserved, { title: "Outra", updatedAt: serverTimestamp() }),
    );
    await assertFails(deleteDoc(reserved));
  });

  it("cannot create an activity in the caregiver's name", async () => {
    await assertFails(
      setDoc(
        doc(dbAs(PRIMARY), `users/${PRIMARY}/routineTasks/forged`),
        newTask(CAREGIVER),
      ),
    );
  });

  it("cannot read or change another person's routine", async () => {
    const db = dbAs(OTHER_PRIMARY);
    await assertFails(getDocs(routineOf(db, PRIMARY)));
    await assertFails(
      updateDoc(
        doc(db, `users/${PRIMARY}/routineTasks/selfDone`),
        completion(OTHER_PRIMARY),
      ),
    );
  });
});

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

  it("invitations are never readable or writable by clients", async () => {
    await assertFails(getDoc(doc(dbAs(CAREGIVER), "invitations/ABCDEFGH")));
    await assertFails(
      setDoc(doc(dbAs(CAREGIVER), "invitations/ZZZZZZZZ"), {
        caregiverUid: CAREGIVER,
      }),
    );
  });
});

describe("routine activities: create, edit, delete and query by date", () => {
  const ownPath = `users/${PRIMARY}/routineTasks/own`;

  it("the primary user creates, edits, completes and deletes own activities", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(setDoc(doc(db, ownPath), newTask(PRIMARY)));
    await assertSucceeds(
      updateDoc(doc(db, ownPath), {
        title: "Ler um livro",
        scheduledTime: null,
        updatedAt: serverTimestamp(),
      }),
    );
    await assertSucceeds(updateDoc(doc(db, ownPath), completion(PRIMARY)));
    await assertSucceeds(deleteDoc(doc(db, ownPath)));
  });

  it("own activities stay completable by their owner", async () => {
    await assertFails(
      setDoc(
        doc(dbAs(PRIMARY), ownPath),
        newTask(PRIMARY, { ownerCanComplete: false }),
      ),
    );
  });

  it("the primary user may hide own activities; the caregiver then cannot touch them", async () => {
    await assertSucceeds(
      setDoc(
        doc(dbAs(PRIMARY), ownPath),
        newTask(PRIMARY, { status: "hidden_from_caregiver" }),
      ),
    );
    const asCaregiver = doc(dbAs(CAREGIVER), ownPath);
    await assertFails(getDoc(asCaregiver));
    await assertFails(updateDoc(asCaregiver, completion(CAREGIVER)));
    await assertFails(deleteDoc(asCaregiver));
  });

  it("the caregiver can manage activities the primary user created, if not hidden", async () => {
    await setDoc(doc(dbAs(PRIMARY), ownPath), newTask(PRIMARY));
    await assertSucceeds(
      updateDoc(doc(dbAs(CAREGIVER), ownPath), {
        title: "Ler 10 páginas",
        updatedAt: serverTimestamp(),
      }),
    );
  });

  it("the caregiver can neither create hidden activities nor hide existing ones", async () => {
    const db = dbAs(CAREGIVER);
    await assertFails(
      setDoc(
        doc(db, `users/${PRIMARY}/routineTasks/hidden`),
        newTask(CAREGIVER, { status: "hidden_from_caregiver" }),
      ),
    );
    await assertFails(
      updateDoc(doc(db, `users/${PRIMARY}/routineTasks/selfDone`), {
        status: "hidden_from_caregiver",
        updatedAt: serverTimestamp(),
      }),
    );
  });

  it("dates, times and sizes are validated by the server", async () => {
    const db = dbAs(PRIMARY);
    for (const invalid of [
      { date: "29/10/2026" },
      { scheduledTime: "25:00" },
      { scheduledTime: "7:30" },
      { title: "" },
      { description: "x".repeat(501) },
    ]) {
      await assertFails(setDoc(doc(db, ownPath), newTask(PRIMARY, invalid)));
    }
  });

  it("edits must keep authorship and stamp the server time", async () => {
    const db = dbAs(CAREGIVER);
    const ref = doc(db, `users/${PRIMARY}/routineTasks/selfDone`);
    await assertFails(updateDoc(ref, { title: "Sem data de alteração" }));
    await assertFails(
      updateDoc(ref, { createdBy: PRIMARY, updatedAt: serverTimestamp() }),
    );
  });

  it("days are queried with equality filters and a limit", async () => {
    await assertSucceeds(getDocs(dayOf(dbAs(PRIMARY), PRIMARY, DAY)));
    await assertSucceeds(
      getDocs(dayOf(dbAs(CAREGIVER), PRIMARY, DAY, { activeOnly: true })),
    );
    // Without the status filter the caregiver could see hidden activities.
    await assertFails(getDocs(dayOf(dbAs(CAREGIVER), PRIMARY, DAY)));
    await assertFails(getDocs(dayOf(dbAs(OTHER_PRIMARY), PRIMARY, DAY)));
  });

  it("the owner reads a period with one bounded range query (reports)", async () => {
    const tasks = collection(dbAs(PRIMARY), `users/${PRIMARY}/routineTasks`);
    const range = [
      where("date", ">=", "2026-10-01"),
      where("date", "<=", "2026-10-31"),
      orderBy("date"),
    ];
    await assertSucceeds(getDocs(query(tasks, ...range, limit(100))));
    await assertFails(getDocs(query(tasks, ...range)));
  });

  it("a query by date returns only that day", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), `users/${PRIMARY}/routineTasks/tomorrow`),
        task({ date: "2026-10-30" }),
      );
    });

    const day = await getDocs(dayOf(dbAs(PRIMARY), PRIMARY, DAY));

    const dates = new Set(day.docs.map((snapshot) => snapshot.get("date")));
    assert.deepEqual([...dates], [DAY]);
    assert.equal(day.size, 3);
  });
});
