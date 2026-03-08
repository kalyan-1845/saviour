# MongoDB Setup Instructions

## 🎯 Complete! Now Add Your MongoDB URL

I've set up everything for MongoDB. Now you just need to add your MongoDB connection string.

## 📋 What Was Created:

### Models (Database Schemas):
- ✅ **Driver** - Stores driver info (license, vehicle, location)
- ✅ **User** - Stores user info (emergency contacts, medical info)
- ✅ **EmergencyTrip** - Stores trip records

### API Routes (Auto-store data):
- ✅ `POST /api/auth/driver-register` - Register driver
- ✅ `POST /api/auth/driver-login` - Login driver
- ✅ `POST /api/auth/user-register` - Register user
- ✅ `POST /api/auth/user-login` - Login user

### Database Connection:
- ✅ `lib/mongodb.ts` - Connection handler with caching

---

## 🔑 Step 1: Get MongoDB URL

### Option A: Free MongoDB Cloud (Recommended)
1. Go to: https://www.mongodb.com/cloud/atlas
2. Sign up for free account
3. Create a new project
4. Create a database cluster (free tier)
5. Create a database user with password
6. Click "Connect" and copy the connection string
7. Replace `<username>` and `<password>` with your credentials
8. Save the full URI

### Option B: Local MongoDB
If MongoDB is installed locally:
```
mongodb://localhost:27017/sarathi
```

---

## 📝 Step 2: Add to .env.local

The `.env.local` file already has the placeholder:

```dotenv
MONGODB_URI=
```

**Add your MongoDB URL after the `=` sign. For example:**

```dotenv
MONGODB_URI=mongodb+srv://youruser:yourpassword@cluster0.abc123.mongodb.net/sarathi?retryWrites=true&w=majority
```

---

## 🧪 Step 3: Test the Registration

### Test Driver Registration:
```bash
curl -X POST http://localhost:3000/api/auth/driver-register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "email": "driver@example.com",
    "phone": "9876543210",
    "licenseNumber": "DL123456",
    "vehicleNumber": "KA01AB1234",
    "password": "password123",
    "confirmPassword": "password123"
  }'
```

### Test User Registration:
```bash
curl -X POST http://localhost:3000/api/auth/user-register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Smith",
    "email": "user@example.com",
    "phone": "9876543210",
    "password": "password123",
    "confirmPassword": "password123"
  }'
```

---

## 🚀 What Happens Now:

When a user registers:
1. Form data sent to API
2. API validates input
3. Password hashed with bcryptjs
4. Data stored in MongoDB
5. Success response returned

When a user logs in:
1. Email and password checked
2. Password compared (hashed)
3. User/Driver data returned
4. Login successful!

---

## 📊 Collections in MongoDB:

After testing, you'll see these collections:
- **drivers** - All registered drivers
- **users** - All registered users
- **emergencytrips** - All emergency records

---

## ✅ Ready When You:

1. **Get MongoDB URL** from atlas.mongodb.com or local MongoDB
2. **Paste it in `.env.local`** after `MONGODB_URI=`
3. **Restart dev server** (npm run dev)
4. **Test the APIs** - Try registration!

---

## 🛠️ Troubleshooting:

**"Cannot connect to MongoDB"**
- Check MongoDB URL is correct
- Make sure database user exists
- Check IP whitelist in MongoDB Atlas

**"Duplicate key error"**
- Email/license already registered
- Clear database or use different values

**"Password mismatch"**
- Make sure confirmPassword matches password

---

## 📞 You're All Set!

Once you add the MongoDB URL:
- ✅ Registration stores data
- ✅ Login validates against stored data
- ✅ All user data persisted in database
- ✅ Ready for production!

**Tell me when you have the MongoDB URL and I'll verify everything works!**
