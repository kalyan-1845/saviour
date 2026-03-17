const { MongoClient } = require('mongodb');
require('dotenv').config({ path: '.env.local' });

const uri = process.env.MONGODB_URI;
const dbName = process.env.MONGODB_DB_NAME || 'sarathi-emergency';

const hospitals = [
  {
    name: 'Apollo Hospitals - Jubilee Hills',
    address: 'Road No 72, Opposite Bharatiya Vidya Bhavan School, Film Nagar, Jubilee Hills, Hyderabad',
    latitude: 17.4206,
    longitude: 78.4111,
    phone: '040-23607777',
    specialties: ['Cardiology', 'Neurology', 'Oncology', 'Emergency'],
    bedsAvailable: 45,
    totalBeds: 500,
    type: 'private',
    city: 'Hyderabad',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'Yashoda Hospitals - Secunderabad',
    address: 'Alexander Road, Secunderabad, Hyderabad',
    latitude: 17.4426,
    longitude: 78.5006,
    phone: '040-45674567',
    specialties: ['Cardiology', 'Gastroenterology', 'Neurology', 'Emergency'],
    bedsAvailable: 30,
    totalBeds: 400,
    type: 'private',
    city: 'Secunderabad',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'KIMS Hospitals - Secunderabad',
    address: '1-8-31/1, Minister Road, Secunderabad',
    latitude: 17.4348,
    longitude: 78.4878,
    phone: '040-44885000',
    specialties: ['Cardiac Sciences', 'Neuro Sciences', 'Renal Sciences', 'Emergency'],
    bedsAvailable: 25,
    totalBeds: 300,
    type: 'private',
    city: 'Secunderabad',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'Osmania General Hospital',
    address: 'Afzal Gunj, Hyderabad',
    latitude: 17.3779,
    longitude: 78.4795,
    phone: '040-24600146',
    specialties: ['General Medicine', 'Surgery', 'Orthopedics', 'Emergency'],
    bedsAvailable: 100,
    totalBeds: 1000,
    type: 'government',
    city: 'Hyderabad',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'Gandhi Hospital',
    address: 'Musheerabad, Secunderabad',
    latitude: 17.4239,
    longitude: 78.5032,
    phone: '040-27505566',
    specialties: ['General Medicine', 'Surgery', 'Pediatrics', 'Emergency'],
    bedsAvailable: 80,
    totalBeds: 1200,
    type: 'government',
    city: 'Secunderabad',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'Care Hospitals - Banjara Hills',
    address: 'Road No. 1, Banjara Hills, Hyderabad',
    latitude: 17.4124,
    longitude: 78.4526,
    phone: '040-61656565',
    specialties: ['Cardiology', 'Neurology', 'Nephrology', 'Emergency'],
    bedsAvailable: 20,
    totalBeds: 250,
    type: 'private',
    city: 'Hyderabad',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }
];

const policeStations = [
  {
    name: 'Jubilee Hills Police Station',
    address: 'Road No 36, Jubilee Hills, Hyderabad',
    latitude: 17.4299,
    longitude: 78.4069,
    phone: '040-27854705',
    jurisdiction: 'Jubilee Hills Area',
    type: 'police_station',
    city: 'Hyderabad',
    zone: 'West Zone',
    emergencyContact: '100',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'Banjara Hills Police Station',
    address: 'Road No 12, Banjara Hills, Hyderabad',
    latitude: 17.4162,
    longitude: 78.4418,
    phone: '040-27853603',
    jurisdiction: 'Banjara Hills Area',
    type: 'police_station',
    city: 'Hyderabad',
    zone: 'West Zone',
    emergencyContact: '100',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'Secunderabad Railway Police Station',
    address: 'Secunderabad Railway Station, Secunderabad',
    latitude: 17.4339,
    longitude: 78.5017,
    phone: '040-27853501',
    jurisdiction: 'Railway Station and Surroundings',
    type: 'police_station',
    city: 'Secunderabad',
    zone: 'North Zone',
    emergencyContact: '100',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: 'Punjagutta Police Station',
    address: 'Punjagutta, Hyderabad',
    latitude: 17.4258,
    longitude: 78.4523,
    phone: '040-27853503',
    jurisdiction: 'Punjagutta Area',
    type: 'police_station',
    city: 'Hyderabad',
    zone: 'West Zone',
    emergencyContact: '100',
    isEmergencyAvailable: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }
];

async function seed() {
  if (!uri) {
    console.error('MONGODB_URI is not defined in .env.local');
    process.exit(1);
  }

  const client = new MongoClient(uri);

  try {
    await client.connect();
    console.log('Connected to MongoDB');
    const db = client.db(dbName);

    // Clear existing
    await db.collection('hospitals').deleteMany({});
    await db.collection('policestations').deleteMany({});

    console.log('Seeding hospitals...');
    await db.collection('hospitals').insertMany(hospitals);

    console.log('Seeding police stations...');
    await db.collection('policestations').insertMany(policeStations);

    console.log('Creating geospatial indexes...');
    await db.collection('hospitals').createIndex({ latitude: 1, longitude: 1 });
    await db.collection('policestations').createIndex({ latitude: 1, longitude: 1 });
    // Note: In a production environment, you would use 2dsphere index for genuine geo-queries.
    // However, since we use custom calculateDistanceKm in the current route.ts, 
    // basic indexes speed up the fetch. 
    // To enable genuine $nearSphere, we'd need to convert lat/lng to Point format.

    console.log('Seed completed successfully!');
  } catch (err) {
    console.error('Seed failed:', err);
  } finally {
    await client.close();
  }
}

seed();
