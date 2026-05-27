const mongoose = require('mongoose');

const tokenSchema = new mongoose.Schema({
  token: { type: String, required: true, unique: true },
  lastUpdated: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Token', tokenSchema);