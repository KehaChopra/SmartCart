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
const crypto = require("crypto");
const Razorpay = require("razorpay");

const cartSecret = defineSecret("CART_SECRET");
const razorpayKeyId = defineSecret("RAZORPAY_KEY_ID");
const razorpayKeySecret = defineSecret("RAZORPAY_KEY_SECRET");

const paymentSecrets = [cartSecret, razorpayKeyId, razorpayKeySecret];

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
  PAYMENTS: "payments",
  USERS: "users",
};

const CART_STATUS = {
  AVAILABLE: "available",
  OCCUPIED: "occupied",
  IDLE: "idle",
};

const SESSION_STATUS = {
  ACTIVE: "active",
  COMPLETED: "completed",
};

const PAYMENT_STATUS = {
  PENDING: "PENDING",
  PAID: "PAID",
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
 * @param {Array<{price?: number, quantity?: number}>|Object|null|undefined} items
 * @return {Array<{price?: number, quantity?: number}>}
 */
function normalizeItemsArray(items) {
  if (!items) return [];
  if (Array.isArray(items)) return [...items];
  if (typeof items === "object") return Object.values(items);
  return [];
}

/**
 * @param {object} item
 * @return {number}
 */
function resolveItemPrice(item) {
  for (const key of ["price", "cost", "unitPrice", "amount"]) {
    if (item[key] == null) continue;
    const value = Number(item[key]);
    if (!Number.isNaN(value)) return value;
  }
  return 0;
}

/**
 * @param {object} item
 * @return {number}
 */
function resolveItemQuantity(item) {
  for (const key of ["qty", "quantity", "count"]) {
    if (item[key] == null) continue;
    const value = Number(item[key]);
    if (!Number.isNaN(value) && value > 0) return value;
  }
  return 1;
}

/**
 * @param {Array<{price?: number, quantity?: number}>|Object|null|undefined} items
 * @return {number}
 */
function calculateTotalAmount(items) {
  return normalizeItemsArray(items).reduce((sum, item) => {
    const price = resolveItemPrice(item);
    const quantity = resolveItemQuantity(item);
    return sum + price * quantity;
  }, 0);
}

/**
 * @param {number} first
 * @param {number} second
 * @return {boolean}
 */
function totalsAreEqual(first, second) {
  return Math.abs(Number(first) - Number(second)) < 0.01;
}

/**
 * @return {import("razorpay")}
 */
function getRazorpayClient() {
  const keyId = razorpayKeyId.value();
  const keySecret = razorpayKeySecret.value();
  if (!keyId || !keySecret) {
    throw new Error("Razorpay credentials are not configured");
  }

  return new Razorpay({
    key_id: keyId,
    key_secret: keySecret,
  });
}

/**
 * @param {number} amountInRupees
 * @return {number}
 */
function rupeesToPaise(amountInRupees) {
  return Math.round(Number(amountInRupees) * 100);
}

/**
 * @param {string} orderId
 * @param {string} paymentId
 * @param {string} signature
 * @return {boolean}
 */
function verifyRazorpaySignature(orderId, paymentId, signature) {
  const keySecret = razorpayKeySecret.value();
  if (!keySecret) {
    throw new Error("Razorpay credentials are not configured");
  }

  const expectedSignature = crypto
      .createHmac("sha256", keySecret)
      .update(`${orderId}|${paymentId}`)
      .digest("hex");

  const receivedBuffer = Buffer.from(signature, "utf8");
  const expectedBuffer = Buffer.from(expectedSignature, "utf8");

  if (receivedBuffer.length !== expectedBuffer.length) {
    return false;
  }

  return crypto.timingSafeEqual(receivedBuffer, expectedBuffer);
}

/**
 * @param {import("firebase-functions/v2/https").Response} res
 * @param {string} reason
 */
function sendPaymentFailed(res, reason) {
  res.status(200).json({
    success: false,
    status: "failed",
    reason,
  });
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

      const items = normalizeItemsArray(session.items);
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

      const items = normalizeItemsArray(session.items);
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

/**
 * Creates a Razorpay order for an active shopping session.
 *
 * POST body: { cartId: string, sessionId: string, secret: string }
 * Success: {
 *   success: true,
 *   orderId: string,
 *   amountPaise: number,
 *   currency: string,
 *   razorpayKeyId: string
 * }
 */
exports.createOrder = onRequest({secrets: paymentSecrets}, async (req, res) => {
  if (rejectUnauthorized(req, res)) return;

  const {cartId, sessionId} = req.body || {};

  if (!cartId || !sessionId) {
    sendError(res, 400, "cartId and sessionId are required");
    return;
  }

  try {
    const sessionRef = db.collection(COLLECTIONS.SESSIONS).doc(sessionId);
    const sessionSnap = await sessionRef.get();

    if (!sessionSnap.exists) {
      sendError(res, 404, "Session not found");
      return;
    }

    const session = sessionSnap.data();
    if (session.status !== SESSION_STATUS.ACTIVE) {
      sendError(res, 400, "Session is not active");
      return;
    }

    if (session.cartId !== cartId) {
      sendError(res, 400, "Session does not belong to this cart");
      return;
    }

    const calculatedTotal = calculateTotalAmount(session.items);
    const storedTotal = Number(session.totalAmount) || 0;
    const totalAmount = calculatedTotal > 0 ? calculatedTotal : storedTotal;

    if (calculatedTotal > 0 && !totalsAreEqual(calculatedTotal, storedTotal)) {
      await sessionRef.update({totalAmount: calculatedTotal});
    }

    if (totalAmount <= 0) {
      sendError(res, 400, "Cart total must be greater than zero");
      return;
    }

    const amountPaise = rupeesToPaise(totalAmount);
    const razorpay = getRazorpayClient();

    const order = await razorpay.orders.create({
      amount: amountPaise,
      currency: "INR",
      receipt: sessionId,
    });

    const paymentRef = db.collection(COLLECTIONS.PAYMENTS).doc(order.id);
    await paymentRef.set({
      orderId: order.id,
      sessionId,
      cartId,
      userId: session.userId || "",
      status: PAYMENT_STATUS.PENDING,
      amount: amountPaise,
      createdAt: FieldValue.serverTimestamp(),
    });

    sendSuccess(res, {
      orderId: order.id,
      amountPaise,
      currency: order.currency || "INR",
      razorpayKeyId: razorpayKeyId.value(),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    logger.error("createOrder failed", {cartId, sessionId, error: message});
    sendError(res, 500, message === "Razorpay credentials are not configured"
      ? message
      : "Could not create payment order");
  }
});

/**
 * Verifies a Razorpay payment and completes the shopping session.
 *
 * POST body: {
 *   orderId: string,
 *   paymentId: string,
 *   signature: string,
 *   secret: string
 * }
 * Success: { success: true, status: "paid", sessionId: string, cartId: string }
 * Failure: { success: false, status: "failed", reason: string }
 */
exports.verifyPayment = onRequest({secrets: paymentSecrets}, async (req, res) => {
  if (rejectUnauthorized(req, res)) return;

  const {orderId, paymentId, signature} = req.body || {};

  if (!orderId || !paymentId || !signature) {
    sendError(res, 400, "orderId, paymentId, and signature are required");
    return;
  }

  try {
    if (!verifyRazorpaySignature(orderId, paymentId, signature)) {
      sendPaymentFailed(res, "Invalid payment signature");
      return;
    }

    const paymentRef = db.collection(COLLECTIONS.PAYMENTS).doc(orderId);
    const paymentSnap = await paymentRef.get();

    if (!paymentSnap.exists) {
      sendPaymentFailed(res, "Payment order not found");
      return;
    }

    const payment = paymentSnap.data();
    if (payment.status === PAYMENT_STATUS.PAID) {
      sendSuccess(res, {
        status: "paid",
        sessionId: payment.sessionId,
        cartId: payment.cartId,
      });
      return;
    }

    const razorpay = getRazorpayClient();
    const razorpayPayment = await razorpay.payments.fetch(paymentId);

    if (razorpayPayment.status !== "captured") {
      sendPaymentFailed(res, `Payment not captured (status: ${razorpayPayment.status})`);
      return;
    }

    const {sessionId, cartId, userId} = payment;
    if (!sessionId || !cartId || !userId) {
      sendPaymentFailed(res, "Payment record is missing session or cart details");
      return;
    }

    const sessionRef = db.collection(COLLECTIONS.SESSIONS).doc(sessionId);
    const cartRef = db.collection(COLLECTIONS.CARTS).doc(cartId);
    const userRef = db.collection(COLLECTIONS.USERS).doc(userId);

    await db.runTransaction(async (transaction) => {
      const sessionSnap = await transaction.get(sessionRef);
      if (!sessionSnap.exists) {
        throw new Error("Session not found");
      }

      transaction.update(paymentRef, {
        status: PAYMENT_STATUS.PAID,
        razorpayPaymentId: paymentId,
        razorpaySignature: signature,
        paidAt: FieldValue.serverTimestamp(),
      });

      transaction.update(sessionRef, {
        status: SESSION_STATUS.COMPLETED,
      });

      transaction.update(cartRef, {
        status: CART_STATUS.IDLE,
        pairedUid: null,
        currentSessionId: null,
      });

      transaction.update(userRef, {
        activeCart: null,
      });
    });

    sendSuccess(res, {
      status: "paid",
      sessionId,
      cartId,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    logger.error("verifyPayment failed", {orderId, paymentId, error: message});

    if (message === "Session not found") {
      sendPaymentFailed(res, message);
      return;
    }

    if (message === "Razorpay credentials are not configured") {
      sendError(res, 500, message);
      return;
    }

    sendPaymentFailed(res, "Could not verify payment");
  }
});
