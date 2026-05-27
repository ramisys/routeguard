const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');

// GET /api/users/profile
// The authMiddleware handles finding/creating the user automatically
router.get('/profile', authMiddleware, (req, res) => {
  // req.user was populated by the middleware
  res.json(req.user);
});

// PATCH /api/users/profile
// Update user profile fields (like username)
router.patch('/profile', authMiddleware, async (req, res) => {
  try {
    const { username } = req.body;
    if (username) {
      // Check if username is already taken
      const existing = await require('../models/User').findOne({ username: username });
      if (existing && existing.id !== req.user.id) {
        return res.status(400).json({ message: 'Username is already taken' });
      }
      req.user.username = username;
    }

    if (req.body.displayName) {
      req.user.displayName = req.body.displayName;
    }

    await req.user.save();
    res.json(req.user);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

module.exports = router;