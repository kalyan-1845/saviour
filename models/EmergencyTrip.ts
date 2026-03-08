import mongoose from 'mongoose';

const EmergencyTripSchema = new mongoose.Schema(
  {
    driverId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Driver',
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
    },
    emergencyType: {
      type: String,
      enum: ['medical', 'accident', 'fire', 'crime', 'police', 'heart_attack', 'pediatric', 'trauma', 'stroke', 'burn'],
      required: true,
    },
    pickupLocation: {
      latitude: Number,
      longitude: Number,
      address: String,
    },
    hospitalId: String,
    hospitalName: String,
    policeStationId: String,
    policeStationName: String,
    dropoffLocation: {
      latitude: Number,
      longitude: Number,
      address: String,
    },
    status: {
      type: String,
      enum: ['pending', 'assigned', 'accepted', 'in-progress', 'completed', 'cancelled'],
      default: 'pending',
    },
    hospitalCaseStatus: {
      type: String,
      enum: ['pending', 'registered', 'ready'],
      default: 'pending',
    },
    hospitalCaseRegisteredAt: Date,
    policeAlertMessage: String,
    notifiedPoliceStationIds: {
      type: [String],
      default: [],
    },
    policeAlertMeta: {
      routeDistanceKm: Number,
      congestionScore: Number,
      congestionLevel: String,
      estimatedSignals: Number,
      redSignalRisk: Number,
      estimatedTimeToClearMins: Number,
      generatedAt: Date,
    },
    estimatedTime: Number,
    actualTime: Number,
    distance: Number,
    createdAt: {
      type: Date,
      default: Date.now,
    },
    completedAt: Date,
  },
  { timestamps: true }
);

export default mongoose.models.EmergencyTrip || mongoose.model('EmergencyTrip', EmergencyTripSchema);
