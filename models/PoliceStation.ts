import mongoose, { Document, Model, Schema } from 'mongoose';

export interface IPoliceStation extends Document {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  phone: string;
  email?: string;
  jurisdiction: string;
  type: 'police_station' | 'circle_office' | 'commissionerate';
  city: 'Hyderabad' | 'Secunderabad';
  zone: string;
  policeVehicles: number;
  emergencyContact: string;
  isEmergencyAvailable: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const PoliceStationSchema: Schema<IPoliceStation> = new Schema(
  {
    name: {
      type: String,
      required: [true, 'Please provide police station name'],
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
    jurisdiction: {
      type: String,
      required: [true, 'Please provide jurisdiction area'],
    },
    type: {
      type: String,
      enum: ['police_station', 'circle_office', 'commissionerate'],
      default: 'police_station',
    },
    city: {
      type: String,
      enum: ['Hyderabad', 'Secunderabad'],
      required: true,
    },
    zone: {
      type: String,
      required: [true, 'Please provide zone'],
    },
    policeVehicles: {
      type: Number,
      default: 1,
    },
    emergencyContact: {
      type: String,
      required: [true, 'Please provide emergency contact'],
    },
    isEmergencyAvailable: {
      type: Boolean,
      default: true,
    },
  },
  {
    timestamps: true,
  }
);

// Index for geospatial queries
PoliceStationSchema.index({ latitude: 1, longitude: 1 });

const PoliceStation: Model<IPoliceStation> = mongoose.models.PoliceStation || mongoose.model<IPoliceStation>('PoliceStation', PoliceStationSchema);

export default PoliceStation;
