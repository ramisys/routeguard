const mongoose = require('mongoose');

const obstacleSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  type: { type: String, required: true },
  severity: { type: String, default: 'MODERATE' },
  location: {
    type: { type: String, enum: ['Point'], default: 'Point' },
    coordinates: { type: [Number], required: true } // [longitude, latitude]
  },
  roadName: { type: String },
  description: { type: String },
  imageUrl: { type: String },
  
  // NEW: Accountability fields
  reporterId: { type: String, required: true },
  reporterName: { type: String },
  
  reportedAt: { type: Date, default: Date.now },
  expiresAt: { type: Date },
  isActive: { type: Boolean, default: true },
  confirmCount: { type: Number, default: 0 }
});

obstacleSchema.index({ location: '2dsphere' });

module.exports = mongoose.model('Obstacle', obstacleSchema);