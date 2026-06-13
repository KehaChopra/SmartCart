/**
 * SmartKart Cloud Functions
 *
 * Backend for the SmartKart Android shopping app.
 * Firestore is the primary data store.
 */

const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const {onRequest} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const logger = require("firebase-functions/logger");

const cartSecret = defineSecret("CART_SECRET");

// ---------------------------------------------------------------------------
// Firebase Admin SDK
// ---------------------------------------------------------------------------

initializeApp();

const db = getFirestore();

// ---------------------------------------------------------------------------
// Firestore collections
// ---------------------------------------------------------------------------

const COLLECTIONS = {
  CARTS: "carts",
  PRODUCTS: "products",
  SESSIONS: "sessions",
};

const CART_STATUS = {
  AVAILABLE: "available",
  OCCUPIED: "occupied",
};

const SESSION_STATUS = {
  ACTIVE: "active",
  COMPLETED: "completed",
};

// ---------------------------------------------------------------------------
// Cloud Functions global options
// ---------------------------------------------------------------------------

setGlobalOptions({
  maxInstances: 10,
});

logger.info("SmartKart Cloud Functions initialized");

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * @param {import("firebase-functions/v2/https").Request} req
 * @param {import("firebase-functions/v2/https").Response} res
 * @return {boolean}
 */
function rejectNonPost(req, res) {
  if (req.method !== "POST") {
    res.status(405).json({success: false, error: "Method not allowed"});
    return true;
  }
  return false;
}

/**
 * Requires POST with a valid secret in the JSON body.
 * Browser GET requests (opening the URL directly) receive 405, not invalid secret.
 *
 * @param {import("firebase-functions/v2/https").Request} req
 * @param {import("firebase-functions/v2/https").Response} res
 * @return {boolean}
 */
function rejectUnauthorized(req, res) {
  if (rejectNonPost(req, res)) return true;

  const secret = (req.body || {}).secret;
  if (secret !== cartSecret.value()) {
    res.status(403).json({error: "Unauthorized: invalid secret"});
    return true;
  }

  return false;
}

/**
 * @param {import("firebase-functions/v2/https").Response} res
 * @param {number} status
 * @param {string} message
 */
function sendError(res, status, message) {
  res.status(status).json({success: false, error: message});
}

/**
 * @param {import("firebase-functions/v2/https").Response} res
 * @param {object} data
 */
function sendSuccess(res, data) {
  res.status(200).json({success: true, ...data});
}

/**
 * @param {Array<{price?: number, quantity?: number}>} items
 * @return {number}
 */
function calculateTotalAmount(items) {
  return items.reduce((sum, item) => {
    const price = Number(item.price) || 0;
    const quantity = Number(item.quantity) || 0;
    return sum + price * quantity;
  }, 0);
}

// ---------------------------------------------------------------------------
// HTTP Cloud Functions
// ---------------------------------------------------------------------------

/**
 * Binds an available cart to a user by creating an active session.
 *
 * POST body: { cartId: string, userId: string }
 * Success: { success: true, sessionId: string }
 */
exports.bindCartToUser = onRequest({secrets: [cartSecret]}, async (req, res) => {
  if (rejectUnauthorized(req, res)) return;

  const {cartId, userId} = req.body || {};

  if (!cartId || !userId) {
    sendError(res, 400, "cartId and userId are required");
    return;
  }

  try {
    const sessionId = await db.runTransaction(async (transaction) => {
      const cartRef = db.collection(COLLECTIONS.CARTS).doc(cartId);
      const cartSnap = await transaction.get(cartRef);

      if (!cartSnap.exists) {
        throw new Error("Cart not found");
      }

      const cart = cartSnap.data();
      if (cart.status === CART_STATUS.OCCUPIED) {
        throw new Error("Cart already in use");
      }

      const sessionRef = db.collection(COLLECTIONS.SESSIONS).doc();
      const newSessionId = sessionRef.id;

      transaction.set(sessionRef, {
        sessionId: newSessionId,
        userId,
        cartId,
        startTime: FieldValue.serverTimestamp(),
        status: SESSION_STATUS.ACTIVE,
        items: [],
        totalAmount: 0,
      });

      transaction.update(cartRef, {
        status: CART_STATUS.OCCUPIED,
        currentSessionId: newSessionId,
      });

      return newSessionId;
    });

    sendSuccess(res, {sessionId});
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";

    if (message === "Cart not found") {
      sendError(res, 404, message);
      return;
    }

    if (message === "Cart already in use") {
      sendError(res, 409, message);
      return;
    }

    logger.error("bindCartToUser failed", {cartId, userId, error: message});
    sendError(res, 500, "Internal server error");
  }
});

/**
 * Adds a product to the active session for an occupied cart.
 *
 * POST body: { cartId: string, barcode: string }
 * Success: { success: true, item: object, totalAmount: number }
 */
exports.addItemToCart = onRequest({secrets: [cartSecret]}, async (req, res) => {
  if (rejectUnauthorized(req, res)) return;

  const {cartId, barcode} = req.body || {};

  if (!cartId || !barcode) {
    sendError(res, 400, "cartId and barcode are required");
    return;
  }

  try {
    const cartRef = db.collection(COLLECTIONS.CARTS).doc(cartId);
    const cartSnap = await cartRef.get();

    if (!cartSnap.exists) {
      sendError(res, 404, "Cart not found");
      return;
    }

    const cart = cartSnap.data();
    if (cart.status !== CART_STATUS.OCCUPIED || !cart.currentSessionId) {
      sendError(res, 400, "No active session");
      return;
    }

    const productsSnap = await db
        .collection(COLLECTIONS.PRODUCTS)
        .where("barcode", "==", barcode)
        .limit(1)
        .get();

    if (productsSnap.empty) {
      sendError(res, 404, "Product not found");
      return;
    }

    const productDoc = productsSnap.docs[0];
    const product = productDoc.data();
    const productId = product.productId || productDoc.id;

    if (!product.name || product.price == null || !product.barcode) {
      sendError(res, 400, "Product is missing required fields (name, price, barcode)");
      return;
    }

    const sessionRef = db
        .collection(COLLECTIONS.SESSIONS)
        .doc(cart.currentSessionId);

    const result = await db.runTransaction(async (transaction) => {
      const sessionSnap = await transaction.get(sessionRef);

      if (!sessionSnap.exists) {
        throw new Error("No active session");
      }

      const session = sessionSnap.data();
      if (session.status !== SESSION_STATUS.ACTIVE) {
        throw new Error("No active session");
      }

      const items = Array.isArray(session.items) ? [...session.items] : [];
      const existingIndex = items.findIndex(
          (item) => item.productId === productId,
      );

      let item;

      if (existingIndex >= 0) {
        items[existingIndex] = {
          ...items[existingIndex],
          quantity: (items[existingIndex].quantity || 0) + 1,
        };
        item = items[existingIndex];
      } else {
        item = {
          productId,
          name: product.name,
          price: product.price,
          barcode: product.barcode,
          category: product.category || "",
          quantity: 1,
        };
        items.push(item);
      }

      const totalAmount = calculateTotalAmount(items);

      transaction.update(sessionRef, {items, totalAmount});

      return {item, totalAmount};
    });

    sendSuccess(res, result);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";

    if (message === "No active session") {
      sendError(res, 400, message);
      return;
    }

    logger.error("addItemToCart failed", {cartId, barcode, error: message});
    sendError(res, 500, "Internal server error");
  }
});

/**
 * Removes one unit of a product from the active session, or removes the line
 * item when quantity reaches zero. Recalculates session totalAmount.
 *
 * POST body: { cartId: string, barcode: string }
 * Success: { success: true, item: object | null, totalAmount: number }
 */
exports.deleteItemFromCart = onRequest({secrets: [cartSecret]}, async (req, res) => {
  if (rejectUnauthorized(req, res)) return;

  const {cartId, barcode} = req.body || {};

  if (!cartId || !barcode) {
    sendError(res, 400, "cartId and barcode are required");
    return;
  }

  try {
    const cartRef = db.collection(COLLECTIONS.CARTS).doc(cartId);
    const cartSnap = await cartRef.get();

    if (!cartSnap.exists) {
      sendError(res, 404, "Cart not found");
      return;
    }

    const cart = cartSnap.data();
    if (cart.status !== CART_STATUS.OCCUPIED || !cart.currentSessionId) {
      sendError(res, 400, "No active session");
      return;
    }

    const sessionRef = db
        .collection(COLLECTIONS.SESSIONS)
        .doc(cart.currentSessionId);

    const result = await db.runTransaction(async (transaction) => {
      const sessionSnap = await transaction.get(sessionRef);

      if (!sessionSnap.exists) {
        throw new Error("No active session");
      }

      const session = sessionSnap.data();
      if (session.status !== SESSION_STATUS.ACTIVE) {
        throw new Error("No active session");
      }

      const items = Array.isArray(session.items) ? [...session.items] : [];
      const existingIndex = items.findIndex(
          (item) => item.barcode === barcode,
      );

      if (existingIndex < 0) {
        throw new Error("Item not in cart");
      }

      const existingItem = items[existingIndex];
      const currentQuantity = Number(existingItem.quantity) || 0;
      let updatedItem = null;

      if (currentQuantity > 1) {
        updatedItem = {
          ...existingItem,
          quantity: currentQuantity - 1,
        };
        items[existingIndex] = updatedItem;
      } else {
        items.splice(existingIndex, 1);
      }

      const totalAmount = calculateTotalAmount(items);

      transaction.update(sessionRef, {items, totalAmount});

      return {item: updatedItem, totalAmount};
    });

    sendSuccess(res, result);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";

    if (message === "No active session") {
      sendError(res, 400, message);
      return;
    }

    if (message === "Item not in cart") {
      sendError(res, 404, message);
      return;
    }

    logger.error("deleteItemFromCart failed", {cartId, barcode, error: message});
    sendError(res, 500, "Internal server error");
  }
});
