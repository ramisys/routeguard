const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  // This is the Firebase UID (uid)
  id: { type: String, required: true, unique: true },
  email: { type: String, required: true },
  displayName: { type: String },
  username: { type: String, unique: true, sparse: true },
  reputationPoints: { type: Number, default: 0 },
  level: { type: Number, default: 1 },
  role: { type: String, enum: ['user', 'admin'], default: 'user' },
  joinedAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', userSchema);