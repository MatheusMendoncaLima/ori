/**
 * Entry point for ORI Cloud Functions.
 *
 * Privileged operations live here so they are validated on the server.
 * Today: creating and accepting care link invitations.
 */

import {setGlobalOptions} from "firebase-functions";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/https";
import * as logger from "firebase-functions/logger";
import {initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {
  acceptInvitation,
  CareInvitationError,
  createInvitation,
} from "./careInvitations";

// Caps containers per function to limit cost during unexpected traffic spikes.
setGlobalOptions({maxInstances: 10});

initializeApp();

/**
 * Returns the caller uid or rejects unauthenticated calls.
 * @param {CallableRequest} request Incoming callable request.
 * @return {string} Authenticated uid.
 */
function requireCaller(request: CallableRequest): string {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in first");
  }
  return request.auth.uid;
}

/**
 * Converts expected failures to HttpsError; logs unexpected ones without
 * user content (only the error name).
 * @param {function(): Promise<T>} operation Work to run.
 * @return {Promise<T>} The operation result.
 */
async function runSafely<T>(operation: () => Promise<T>): Promise<T> {
  try {
    return await operation();
  } catch (error) {
    if (error instanceof CareInvitationError) {
      throw new HttpsError(error.code, error.message);
    }
    logger.error("Care invitation failed", {name: (error as Error).name});
    throw new HttpsError("internal", "Unexpected error");
  }
}

export const createCareInvitation = onCall((request) => {
  const caregiverUid = requireCaller(request);
  return runSafely(() =>
    createInvitation(getFirestore(), caregiverUid, new Date()),
  );
});

export const acceptCareInvitation = onCall((request) => {
  const primaryUserUid = requireCaller(request);
  return runSafely(() =>
    acceptInvitation(
      getFirestore(),
      primaryUserUid,
      request.data?.code,
      new Date(),
    ),
  );
});
