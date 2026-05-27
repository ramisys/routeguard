const mongoose = require('mongoose');

const commentSchema = new mongoose.Schema({
    obstacleId: { type: String, required: true, index: true },
    userName: { type: String, required: true },
    text: { type: String, required: true },
    timestamp: { type: Number, default: () => Date.now() } // Uses Unix timestamp for Android compatibility
});

module.exports = mongoose.model('Comment', commentSchema);