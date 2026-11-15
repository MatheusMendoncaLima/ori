/**
 * Care link lifecycle that must run on the server.
 *
 * A caregiver creates a short invitation code; the primary user types it to
 * accept. Only this module creates `careLinks` documents, so profiles and the
 * code are always checked here, never trusted from the client.
 */

import {randomInt} from "node:crypto";
import type {
  DocumentSnapshot,
  Firestore,
  Transaction,
} from "firebase-admin/firestore";

/** Invitation lifetime: long enough to share, short enough to limit misuse. */
export const INVITATION_LIFETIME_MILLIS = 24 * 60 * 60 * 1000;

/** No 0/O or 1/I, so codes are easy to read aloud and type. */
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const CODE_LENGTH = 8;
const CODE_PATTERN = /^[A-Z2-9]{8}$/;
const MAX_CODE_ATTEMPTS = 3;
const ALREADY_EXISTS_GRPC_CODE = 6;

/** Least privilege: the primary user may grant management afterwards. */
const DEFAULT_PERMISSIONS = {
  viewRoutines: true,
  manageRoutines: false,
  manageCommunication: false,
};

export type InvitationErrorCode =
  | "invalid-argument"
  | "permission-denied"
  | "not-found"
  | "failed-precondition"
  | "already-exists";

/** Expected failure with a code that maps directly to an HttpsError. */
export class CareInvitationError extends Error {
  /**
   * @param {InvitationErrorCode} code Error category for the client.
   * @param {string} message Technical description (never shown to users).
   */
  constructor(readonly code: InvitationErrorCode, message: string) {
    super(message);
  }
}

export interface CreatedInvitation {
  code: string;
  expiresAtMillis: number;
}

export interface AcceptedInvitation {
  linkId: string;
  caregiverName: string;
}

/**
 * Generates a random invitation code with a cryptographic generator.
 * @return {string} An 8-character code.
 */
export function generateInvitationCode(): string {
  let code = "";
  for (let index = 0; index < CODE_LENGTH; index++) {
    code += CODE_ALPHABET[randomInt(CODE_ALPHABET.length)];
  }
  return code;
}

/**
 * Normalizes what the user typed and rejects anything that is not a code.
 * @param {unknown} rawCode Value received from the client.
 * @return {string} The code in uppercase without spaces.
 */
export function normalizeInvitationCode(rawCode: unknown): string {
  const code = typeof rawCode === "string" ?
    rawCode.replace(/\s+/g, "").toUpperCase() :
    "";
  if (!CODE_PATTERN.test(code)) {
    throw new CareInvitationError("invalid-argument", "Malformed code");
  }
  return code;
}

/**
 * Creates an invitation for a caller with a profile.
 * @param {Firestore} db Firestore Admin instance.
 * @param {string} caregiverUid Authenticated caller.
 * @param {Date} now Current time (injected for tests).
 * @param {function(): string} nextCode Code generator (injected for tests).
 * @return {Promise<CreatedInvitation>} The code and its expiration.
 */
export async function createInvitation(
  db: Firestore,
  caregiverUid: string,
  now: Date,
  nextCode: () => string = generateInvitationCode,
): Promise<CreatedInvitation> {
  const profile = await db.collection("users").doc(caregiverUid).get();
  if (!profile.exists) {
    throw new CareInvitationError(
      "permission-denied",
      "Only accounts with a profile can invite",
    );
  }

  const expiresAt = new Date(now.getTime() + INVITATION_LIFETIME_MILLIS);
  for (let attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
    const code = nextCode();
    try {
      // create() fails if the code is already taken, so codes never collide.
      await db.collection("invitations").doc(code).create({
        caregiverUid,
        caregiverName: profile.get("displayName"),
        status: "pending",
        createdAt: now,
        expiresAt,
      });
      return {code, expiresAtMillis: expiresAt.getTime()};
    } catch (error) {
      if ((error as {code?: number}).code !== ALREADY_EXISTS_GRPC_CODE) {
        throw error;
      }
    }
  }
  throw new Error("Could not allocate a unique invitation code");
}

/**
 * Accepts an invitation and creates or reactivates the care link, atomically.
 * @param {Firestore} db Firestore Admin instance.
 * @param {string} primaryUserUid Authenticated caller.
 * @param {unknown} rawCode Code typed by the primary user.
 * @param {Date} now Current time (injected for tests).
 * @return {Promise<AcceptedInvitation>} The new link and the caregiver name.
 */
export async function acceptInvitation(
  db: Firestore,
  primaryUserUid: string,
  rawCode: unknown,
  now: Date,
): Promise<AcceptedInvitation> {
  const code = normalizeInvitationCode(rawCode);
  const users = db.collection("users");
  const invitationRef = db.collection("invitations").doc(code);

  /**
   * True when this same user already accepted the code and the link is still
   * active, so repeating the call must not fail.
   * @param {Transaction} transaction Current transaction.
   * @param {DocumentSnapshot} invitation The invitation document.
   * @return {Promise<boolean>} Whether this call is a harmless retry.
   */
  async function isRetryOfAcceptedInvitation(
    transaction: Transaction,
    invitation: DocumentSnapshot,
  ): Promise<boolean> {
    if (
      invitation.get("status") !== "accepted" ||
      invitation.get("acceptedBy") !== primaryUserUid
    ) {
      return false;
    }
    const linkId = `${invitation.get("caregiverUid")}_${primaryUserUid}`;
    const link = await transaction.get(db.collection("careLinks").doc(linkId));
    return link.exists && link.get("status") === "active";
  }

  return db.runTransaction(async (transaction) => {
    const primaryUser = await transaction.get(users.doc(primaryUserUid));
    if (!primaryUser.exists) {
      throw new CareInvitationError(
        "permission-denied",
        "Only accounts with a profile can accept",
      );
    }

    const invitation = await transaction.get(invitationRef);
    if (!invitation.exists) {
      // The app tells this apart from an unpublished function by this text
      // (SERVER_NOT_FOUND_MESSAGE in FirestoreErrors.kt): keep them equal.
      throw new CareInvitationError("not-found", "Unknown code");
    }
    if (await isRetryOfAcceptedInvitation(transaction, invitation)) {
      // The first call succeeded but its answer was lost (timeout, network).
      return {
        linkId: `${invitation.get("caregiverUid")}_${primaryUserUid}`,
        caregiverName: invitation.get("caregiverName"),
      };
    }
    if (invitation.get("status") !== "pending") {
      throw new CareInvitationError("failed-precondition", "Code already used");
    }
    if (invitation.get("expiresAt").toDate().getTime() <= now.getTime()) {
      throw new CareInvitationError("failed-precondition", "Code expired");
    }

    const caregiverUid: string = invitation.get("caregiverUid");
    if (caregiverUid === primaryUserUid) {
      throw new CareInvitationError("failed-precondition", "Self link");
    }
    const caregiver = await transaction.get(users.doc(caregiverUid));
    if (!caregiver.exists) {
      throw new CareInvitationError(
        "failed-precondition",
        "Inviter no longer has a profile",
      );
    }

    const linkId = `${caregiverUid}_${primaryUserUid}`;
    const linkRef = db.collection("careLinks").doc(linkId);
    const existingLink = await transaction.get(linkRef);
    if (existingLink.exists && existingLink.get("status") === "active") {
      throw new CareInvitationError("already-exists", "Link already active");
    }

    // A revoked link is replaced: accepting a new code is a new consent.
    transaction.set(linkRef, {
      caregiverUid,
      primaryUserUid,
      caregiverName: caregiver.get("displayName"),
      primaryUserName: primaryUser.get("displayName"),
      status: "active",
      permissions: DEFAULT_PERMISSIONS,
      createdAt: now,
      revokedAt: null,
    });
    transaction.update(invitationRef, {
      status: "accepted",
      acceptedBy: primaryUserUid,
      acceptedAt: now,
    });
    return {linkId, caregiverName: caregiver.get("displayName")};
  });
}
