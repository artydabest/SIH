import type { NextFunction, Request, Response } from "express";

/**
 * Minimal shared-secret auth for the prototype.
 *
 * What it does: requires `X-API-Key` on requests it guards, closing the
 * "anyone can POST fake emergencies" hole for the demo.
 * What it deliberately does NOT do: replace real device attestation or
 * an authenticated warning-issuing authority — say so if asked.
 *
 * Design constraints:
 *  - Reads (GET) stay open so the demo never dies in front of judges, and
 *    socitizen-app polling keeps working even if a key is misconfigured.
 *  - When API_KEY is unset the guard is a no-op and logs once, so a fresh
 *    clone boots without surprises.
 */

let warnedUnset = false;

/** Reads never require a key — visibility must survive key misconfigurations. */
const READ_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

export function apiKeyGuard(req: Request, res: Response, next: NextFunction): void {
  const expected = process.env.API_KEY;

  if (!expected || READ_METHODS.has(req.method)) {
    if (!expected && !warnedUnset && !READ_METHODS.has(req.method)) {
      warnedUnset = true;
      console.warn(
        "API_KEY not set — write endpoints are unauthenticated (dev mode). " +
          "Set API_KEY in .env to lock them down."
      );
    }
    next();
    return;
  }

  const provided = req.get("X-API-Key");
  if (provided !== expected) {
    res.status(401).json({ success: false, message: "Missing or invalid API key" });
    return;
  }

  next();
}
