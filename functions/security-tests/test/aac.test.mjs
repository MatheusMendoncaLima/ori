// Communication (AAC) rules: boards, symbols and saved messages. They run against the local
// emulator, independently of the app, so the server enforces what the app only displays.
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
  deleteField,
  doc,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from "firebase/firestore";

const PRIMARY = "pat";
const OTHER_PRIMARY = "otto";
const MANAGER = "cara"; // active link with manageCommunication
const ROUTINE_ONLY = "viv"; // active link, routine permissions only
const OLD_LINK = "olga"; // active link created before manageCommunication existed
const REVOKED = "rex"; // revoked link that had manageCommunication

const BOARD = `users/${PRIMARY}/aacBoards/basics`;
const SYMBOL = `${BOARD}/symbols/water`;
const MESSAGE = `users/${PRIMARY}/aacSavedMessages/hello`;

let env;

const board = (overrides = {}) => ({
  name: "Básico",
  position: 0,
  createdBy: PRIMARY,
  createdAt: new Date(0),
  updatedAt: new Date(0),
  ...overrides,
});

const symbol = (overrides = {}) => ({
  label: "Água",
  phrase: "Eu quero água",
  image: "emoji:💧",
  position: 0,
  createdBy: PRIMARY,
  createdAt: new Date(0),
  updatedAt: new Date(0),
  ...overrides,
});

// New documents as the app sends them: caller as author, server times.
const newBoard = (author, overrides = {}) =>
  board({
    createdBy: author,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  });
const newSymbol = (author, overrides = {}) =>
  symbol({
    createdBy: author,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  });
const edit = (fields) => ({ ...fields, updatedAt: serverTimestamp() });

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
    const profile = (role) => ({
      role,
      displayName: role,
      createdAt: new Date(0),
    });
    await setDoc(doc(db, `users/${PRIMARY}`), profile("primary_user"));
    await setDoc(doc(db, `users/${OTHER_PRIMARY}`), profile("primary_user"));
    for (const uid of [MANAGER, ROUTINE_ONLY, OLD_LINK, REVOKED]) {
      await setDoc(doc(db, `users/${uid}`), profile("caregiver"));
    }
    const all = {
      viewRoutines: true,
      manageRoutines: true,
      manageCommunication: true,
    };
    await setDoc(
      doc(db, `careLinks/${MANAGER}_${PRIMARY}`),
      link(MANAGER, "active", all),
    );
    await setDoc(
      doc(db, `careLinks/${ROUTINE_ONLY}_${PRIMARY}`),
      link(ROUTINE_ONLY, "active", { ...all, manageCommunication: false }),
    );
    await setDoc(
      doc(db, `careLinks/${OLD_LINK}_${PRIMARY}`),
      link(OLD_LINK, "active", { viewRoutines: true, manageRoutines: true }),
    );
    await setDoc(
      doc(db, `careLinks/${REVOKED}_${PRIMARY}`),
      link(REVOKED, "revoked", all),
    );
    await setDoc(doc(db, BOARD), board());
    await setDoc(doc(db, SYMBOL), symbol());
    await setDoc(
      doc(db, `${BOARD}/symbols/photo`),
      symbol({ label: "Mãe", image: "device:abc-123.jpg" }),
    );
    await setDoc(doc(db, MESSAGE), { text: "Olá", createdAt: new Date(0) });
  });
}

const dbAs = (uid) => env.authenticatedContext(uid).firestore();
// Queries exactly as the app builds them: bounded by the same limits.
const boardsOf = (db, max = 30) =>
  query(collection(db, `users/${PRIMARY}/aacBoards`), limit(max));
const symbolsOf = (db, max = 60) =>
  query(collection(db, `${BOARD}/symbols`), limit(max));
const messagesOf = (db, max = 50) =>
  query(
    collection(db, `users/${PRIMARY}/aacSavedMessages`),
    orderBy("createdAt", "desc"),
    limit(max),
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

describe("communication boards: primary user", () => {
  it("reads boards and symbols with bounded queries only", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(getDocs(boardsOf(db)));
    await assertSucceeds(getDocs(symbolsOf(db)));
    await assertFails(getDocs(boardsOf(db, 31)));
    await assertFails(getDocs(symbolsOf(db, 61)));
    await assertFails(getDocs(collection(db, `${BOARD}/symbols`)));
  });

  it("creates a board with its symbols in one batch", async () => {
    const db = dbAs(PRIMARY);
    const batch = writeBatch(db);
    batch.set(
      doc(db, `users/${PRIMARY}/aacBoards/food`),
      newBoard(PRIMARY, { name: "Comida", position: 1 }),
    );
    batch.set(
      doc(db, `users/${PRIMARY}/aacBoards/food/symbols/apple`),
      newSymbol(PRIMARY, { label: "Maçã", image: "emoji:🍎" }),
    );
    await assertSucceeds(batch.commit());
  });

  it("cannot create a symbol in a board that does not exist", async () => {
    const db = dbAs(PRIMARY);
    await assertFails(
      setDoc(
        doc(db, `users/${PRIMARY}/aacBoards/missing/symbols/s1`),
        newSymbol(PRIMARY),
      ),
    );
  });

  it("edits, reorders and deletes symbols and boards", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(
      updateDoc(doc(db, SYMBOL), edit({ label: "Beber", phrase: null })),
    );
    await assertSucceeds(updateDoc(doc(db, SYMBOL), edit({ position: 3 })));
    await assertSucceeds(updateDoc(doc(db, BOARD), edit({ name: "Início" })));
    await assertSucceeds(deleteDoc(doc(db, SYMBOL)));
    await assertSucceeds(deleteDoc(doc(db, BOARD)));
  });

  it("a symbol may have a known tile color and kind of word", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(updateDoc(doc(db, SYMBOL), edit({ color: "pink" })));
    await assertSucceeds(updateDoc(doc(db, SYMBOL), edit({ color: "none" })));
    await assertSucceeds(updateDoc(doc(db, SYMBOL), edit({ color: null })));
    await assertFails(updateDoc(doc(db, SYMBOL), edit({ color: "#ff0000" })));
    await assertFails(updateDoc(doc(db, SYMBOL), edit({ color: 3 })));
    await assertSucceeds(
      updateDoc(doc(db, SYMBOL), edit({ wordClass: "person" })),
    );
    await assertSucceeds(updateDoc(doc(db, SYMBOL), edit({ wordClass: null })));
    await assertFails(
      updateDoc(doc(db, SYMBOL), edit({ wordClass: "animal" })),
    );
  });

  it("uses a device photo or an emoji, never image data", async () => {
    const db = dbAs(PRIMARY);
    const ref = doc(db, `${BOARD}/symbols/new`);
    await assertSucceeds(
      setDoc(ref, newSymbol(PRIMARY, { image: "device:f00d-42.jpg" })),
    );
    await assertSucceeds(
      setDoc(
        doc(db, `${BOARD}/symbols/plain`),
        newSymbol(PRIMARY, { label: "Sim", image: null }),
      ),
    );
    const bytes = "data:image/png;base64," + "A".repeat(100);
    await assertFails(
      setDoc(
        doc(db, `${BOARD}/symbols/bytes`),
        newSymbol(PRIMARY, { image: bytes }),
      ),
    );
    await assertFails(
      setDoc(
        doc(db, `${BOARD}/symbols/path`),
        newSymbol(PRIMARY, { image: "device:../../secret.jpg" }),
      ),
    );
  });

  it("validates sizes, fields and authorship", async () => {
    const db = dbAs(PRIMARY);
    const ref = (id) => doc(db, `${BOARD}/symbols/${id}`);
    await assertFails(setDoc(ref("a"), newSymbol(PRIMARY, { label: "" })));
    await assertFails(
      setDoc(ref("b"), newSymbol(PRIMARY, { label: "x".repeat(41) })),
    );
    await assertFails(
      setDoc(ref("c"), newSymbol(PRIMARY, { phrase: "x".repeat(201) })),
    );
    await assertFails(setDoc(ref("d"), newSymbol(PRIMARY, { position: -1 })));
    await assertFails(setDoc(ref("e"), newSymbol(PRIMARY, { extra: true })));
    await assertFails(setDoc(ref("f"), newSymbol(MANAGER)));
    await assertFails(
      updateDoc(doc(db, SYMBOL), edit({ createdBy: OTHER_PRIMARY })),
    );
    await assertFails(
      setDoc(
        doc(db, `users/${PRIMARY}/aacBoards/long`),
        newBoard(PRIMARY, { name: "x".repeat(41) }),
      ),
    );
  });

  it("a board may record its language, as a language tag", async () => {
    const db = dbAs(PRIMARY);
    const ref = (id) => doc(db, `users/${PRIMARY}/aacBoards/${id}`);
    await assertSucceeds(
      setDoc(ref("pt"), newBoard(PRIMARY, { language: "pt-BR" })),
    );
    await assertSucceeds(
      setDoc(ref("es"), newBoard(PRIMARY, { language: "es" })),
    );
    await assertSucceeds(
      setDoc(ref("latam"), newBoard(PRIMARY, { language: "es-419" })),
    );
    await assertFails(
      setDoc(ref("empty"), newBoard(PRIMARY, { language: "" })),
    );
    await assertFails(
      setDoc(ref("word"), newBoard(PRIMARY, { language: "português" })),
    );
    await assertFails(
      setDoc(ref("number"), newBoard(PRIMARY, { language: 7 })),
    );
    await assertFails(
      setDoc(
        ref("long"),
        newBoard(PRIMARY, { language: "pt-" + "x".repeat(40) }),
      ),
    );
    // Renaming keeps the language the board was created with.
    await assertSucceeds(
      updateDoc(ref("pt"), { name: "Comida", updatedAt: serverTimestamp() }),
    );
  });

  it("cannot touch another person's boards", async () => {
    const db = dbAs(OTHER_PRIMARY);
    await assertFails(getDocs(boardsOf(db)));
    await assertFails(getDoc(doc(db, SYMBOL)));
    await assertFails(updateDoc(doc(db, BOARD), edit({ name: "Meu" })));
  });
});

describe("communication boards: caregivers", () => {
  it("a caregiver with manageCommunication organizes the boards", async () => {
    const db = dbAs(MANAGER);
    await assertSucceeds(getDocs(boardsOf(db)));
    await assertSucceeds(getDocs(symbolsOf(db)));
    await assertSucceeds(
      setDoc(doc(db, `${BOARD}/symbols/c1`), newSymbol(MANAGER)),
    );
    await assertSucceeds(updateDoc(doc(db, SYMBOL), edit({ position: 2 })));
    await assertSucceeds(deleteDoc(doc(db, SYMBOL)));
  });

  it("a caregiver cannot add device photos but keeps an existing one", async () => {
    const db = dbAs(MANAGER);
    await assertFails(
      setDoc(
        doc(db, `${BOARD}/symbols/c2`),
        newSymbol(MANAGER, { image: "device:abc.jpg" }),
      ),
    );
    await assertFails(
      updateDoc(doc(db, SYMBOL), edit({ image: "device:abc.jpg" })),
    );
    await assertSucceeds(
      updateDoc(doc(db, `${BOARD}/symbols/photo`), edit({ label: "Mamãe" })),
    );
  });

  it("routine permissions, old links and revoked links give no access", async () => {
    for (const uid of [ROUTINE_ONLY, OLD_LINK, REVOKED]) {
      const db = dbAs(uid);
      await assertFails(getDocs(boardsOf(db)));
      await assertFails(getDoc(doc(db, SYMBOL)));
      await assertFails(updateDoc(doc(db, SYMBOL), edit({ label: "X" })));
      await assertFails(deleteDoc(doc(db, BOARD)));
    }
  });
});

describe("saved messages", () => {
  it("are created, listed and deleted by the primary user only", async () => {
    const db = dbAs(PRIMARY);
    await assertSucceeds(getDocs(messagesOf(db)));
    await assertFails(getDocs(messagesOf(db, 51)));
    await assertSucceeds(
      setDoc(doc(db, `users/${PRIMARY}/aacSavedMessages/m2`), {
        text: "Eu quero água",
        createdAt: serverTimestamp(),
      }),
    );
    await assertFails(updateDoc(doc(db, MESSAGE), { text: "Oi" }));
    await assertSucceeds(deleteDoc(doc(db, MESSAGE)));
  });

  it("validate the text", async () => {
    const db = dbAs(PRIMARY);
    const ref = doc(db, `users/${PRIMARY}/aacSavedMessages/m3`);
    await assertFails(setDoc(ref, { text: "", createdAt: serverTimestamp() }));
    await assertFails(
      setDoc(ref, { text: "x".repeat(501), createdAt: serverTimestamp() }),
    );
    await assertFails(
      setDoc(ref, { text: "Oi", createdAt: serverTimestamp(), extra: 1 }),
    );
  });

  it("may record the language they are in", async () => {
    const db = dbAs(PRIMARY);
    const ref = (id) => doc(db, `users/${PRIMARY}/aacSavedMessages/${id}`);
    await assertSucceeds(
      setDoc(ref("l1"), {
        text: "Oi",
        language: "pt-BR",
        createdAt: serverTimestamp(),
      }),
    );
    await assertFails(
      setDoc(ref("l2"), {
        text: "Oi",
        language: "1",
        createdAt: serverTimestamp(),
      }),
    );
    await assertFails(
      setDoc(ref("l3"), {
        text: "Oi",
        language: null,
        createdAt: serverTimestamp(),
      }),
    );
  });

  it("may have their own emoji, which changes after saving without the text", async () => {
    const db = dbAs(PRIMARY);
    const ref = doc(db, `users/${PRIMARY}/aacSavedMessages/e1`);
    await assertSucceeds(
      setDoc(ref, { text: "Oi", emoji: "👋", createdAt: serverTimestamp() }),
    );
    await assertSucceeds(updateDoc(ref, { emoji: "🌟" }));
    await assertSucceeds(updateDoc(ref, { emoji: deleteField() }));
    await assertFails(updateDoc(ref, { emoji: "" }));
    await assertFails(updateDoc(ref, { emoji: "x".repeat(33) }));
    await assertFails(updateDoc(ref, { emoji: 1 }));
    await assertFails(updateDoc(ref, { emoji: "🌟", text: "Tchau" }));
    await assertFails(
      updateDoc(doc(dbAs(MANAGER), `users/${PRIMARY}/aacSavedMessages/e1`), {
        emoji: "🌟",
      }),
    );
  });

  it("may be moved: only their place changes, inside the range, by their owner", async () => {
    const db = dbAs(PRIMARY);
    const ref = (id) => doc(db, `users/${PRIMARY}/aacSavedMessages/${id}`);
    await assertSucceeds(
      setDoc(ref("p1"), { text: "Oi", createdAt: serverTimestamp() }),
    );
    // Brought from the device with the place it had there.
    await assertSucceeds(
      setDoc(ref("p2"), {
        text: "Tchau",
        position: 3,
        createdAt: serverTimestamp(),
      }),
    );
    await assertFails(
      setDoc(ref("p3"), {
        text: "Oi",
        position: -1,
        createdAt: serverTimestamp(),
      }),
    );
    const batch = writeBatch(db);
    batch.update(ref("p1"), { position: 1 });
    batch.update(ref("p2"), { position: 0 });
    await assertSucceeds(batch.commit());
    await assertSucceeds(updateDoc(ref("p1"), { position: 999 }));
    await assertFails(updateDoc(ref("p1"), { position: 1000 }));
    await assertFails(updateDoc(ref("p1"), { position: "1" }));
    await assertFails(updateDoc(ref("p1"), { position: 1.5 }));
    await assertFails(updateDoc(ref("p1"), { position: 2, text: "Olá" }));
    await assertFails(
      updateDoc(doc(dbAs(MANAGER), `users/${PRIMARY}/aacSavedMessages/p1`), {
        position: 0,
      }),
    );
  });

  it("stay private even from a caregiver who manages the boards", async () => {
    const db = dbAs(MANAGER);
    await assertFails(getDocs(messagesOf(db)));
    await assertFails(getDoc(doc(db, MESSAGE)));
    await assertFails(deleteDoc(doc(db, MESSAGE)));
    await assertFails(
      setDoc(doc(db, `users/${PRIMARY}/aacSavedMessages/m4`), {
        text: "Oi",
        createdAt: serverTimestamp(),
      }),
    );
  });
});

describe("communication permission on the care link", () => {
  it("only the primary user grants manageCommunication", async () => {
    const permissions = {
      viewRoutines: true,
      manageRoutines: false,
      manageCommunication: true,
    };
    await assertFails(
      updateDoc(
        doc(dbAs(ROUTINE_ONLY), `careLinks/${ROUTINE_ONLY}_${PRIMARY}`),
        {
          permissions,
        },
      ),
    );
    await assertSucceeds(
      updateDoc(doc(dbAs(PRIMARY), `careLinks/${ROUTINE_ONLY}_${PRIMARY}`), {
        permissions,
      }),
    );
    await assertFails(
      updateDoc(doc(dbAs(PRIMARY), `careLinks/${ROUTINE_ONLY}_${PRIMARY}`), {
        permissions: { ...permissions, manageCommunication: "yes" },
      }),
    );
  });

  it("links without the new key keep working", async () => {
    await assertSucceeds(
      updateDoc(doc(dbAs(PRIMARY), `careLinks/${OLD_LINK}_${PRIMARY}`), {
        permissions: { viewRoutines: true, manageRoutines: false },
      }),
    );
  });
});
