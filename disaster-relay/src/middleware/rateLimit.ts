import rateLimit from "express-rate-limit";
import type { Request, Response, NextFunction } from "express";

/**
 * General API limiter — generous ceiling so the demo never trips it, tight
 * enough that a runaway client cannot hammer the database.
 */
export const apiLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: 300,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { success: false, message: "Too many requests — slow down" },
});

/** Reads bypass the write limiter. */
const READ_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

function onlyWrites(handler: typeof writeLimiter) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (READ_METHODS.has(req.method)) {
      next();
      return;
    }
    handler(req, res, next);
  };
}

/**
 * Strict limiter for write endpoints (emergencies, warnings, check-ins,
 * circle changes). Tuned so a legitimate demo session never hits it, but a
 * flood of fake reports gets throttled to ~2/second.
 */
const strictWriteLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: 120,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { success: false, message: "Too many write requests — try again shortly" },
});

export const writeLimiter = onlyWrites(strictWriteLimiter);
