import mongoose, { Document, Model, Schema } from 'mongoose';

export interface IHospital extends Document {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  phone: string;
  email?: string;
  specialties: string[];
  bedsAvailable: number;
  totalBeds: number;
  type: 'government' | 'private' | 'semi-government';
  isEmergencyAvailable: boolean;
 ambulanceCount: number;
  rating?: number;
  city: 'Hyderabad' | 'Secunderabad';
  zone?: string;
  createdAt: Date;
  updatedAt: Date;
}

const HospitalSchema: Schema<IHospital> = new Schema(
  {
    name: {
      type: String,
      required: [true, 'Please provide hospital name'],
      trim: true,
    },
    address: {
      type: String,
      required: [true, 'Please provide address'],
      trim: true,
    },
    latitude: {
      type: Number,
      required: [true, 'Please provide latitude'],
    },
    longitude: {
      type: Number,
      required: [true, 'Please provide longitude'],
    },
    phone: {
      type: String,
      required: [true, 'Please provide phone number'],
    },
    email: {
      type: String,
    },
    specialties: {
      type: [String],
      default: [],
    },
    bedsAvailable: {
      type: Number,
      default: 0,
    },
    totalBeds: {
      type: Number,
      default: 0,
    },
    type: {
      type: String,
      enum: ['government', 'private', 'semi-government'],
      default: 'private',
    },
    isEmergencyAvailable: {
      type: Boolean,
      default: true,
    },
    ambulanceCount: {
      type: Number,
      default: 1,
    },
    rating: {
      type: Number,
      min: 0,
      max: 5,
    },
    city: {
      type: String,
      enum: ['Hyderabad', 'Secunderabad'],
      required: true,
    },
    zone: {
      type: String,
    },
  },
  {
    timestamps: true,
  }
);

// Index for geospatial queries
HospitalSchema.index({ latitude: 1, longitude: 1 });

const Hospital: Model<IHospital> = mongoose.models.Hospital || mongoose.model<IHospital>('Hospital', HospitalSchema);

export default Hospital;
