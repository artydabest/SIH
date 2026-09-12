import mongoose from "mongoose";

/**
 * One directed Safe Circle link: `ownerDeviceId` added `memberDeviceId`.
 * Safety status lives on the Person record; membership lives here so any
 * device can look up who is in its circle and what each member last reported.
 */
const safeCircleMemberSchema = new mongoose.Schema(
  {
    ownerDeviceId: {
      type: String,
      required: true,
      trim: true,
    },
    memberDeviceId: {
      type: String,
      required: true,
      trim: true,
    },
    memberName: {
      type: String,
      required: false,
      trim: true,
    },
    addedAt: {
      type: Date,
      required: true,
      default: Date.now,
    },
  },
  {
    timestamps: true,
  }
);

// A device cannot add the same member twice.
safeCircleMemberSchema.index({ ownerDeviceId: 1, memberDeviceId: 1 }, { unique: true });

export interface SafeCircleMemberDoc extends mongoose.Document {
  ownerDeviceId: string;
  memberDeviceId: string;
  memberName?: string;
  addedAt: Date;
}

export default mongoose.model<SafeCircleMemberDoc>(
  "SafeCircleMember",
  safeCircleMemberSchema
);
