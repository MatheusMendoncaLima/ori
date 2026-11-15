// Firestore security rules tests: Routine activities and repeating activities: the person's own and the queries by date.
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
    testEnv().withSecurityRulesDisabled(async (context) => {
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
    await assertSucceeds(
      updateDoc(doc(db, path), {
        repeat: "monthly_day",
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

  it("a caregiver reads a period only with the active filter (marks of the month)", async () => {
    const tasks = collection(dbAs(CAREGIVER), `users/${PRIMARY}/routineTasks`);
    const range = [
      where("date", ">=", "2026-10-01"),
      where("date", "<=", "2026-10-31"),
      orderBy("date"),
      limit(100),
    ];
    await assertSucceeds(
      getDocs(query(tasks, where("status", "==", "active"), ...range)),
    );
    // Without the status filter the period could include hidden activities.
    await assertFails(getDocs(query(tasks, ...range)));
  });

  it("a query by date returns only that day", async () => {
    await testEnv().withSecurityRulesDisabled(async (context) => {
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
