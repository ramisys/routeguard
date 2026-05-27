const express = require('express');
const router = express.Router();
const { body, query, validationResult } = require('express-validator');

const Obstacle = require('../models/Obstacle');
const Comment = require('../models/Comment');
const notificationService = require('../services/notificationService');
const authMiddleware = require('../middleware/auth');

// Validation middleware
const validate = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    console.error('Validation Errors for', req.method, req.url, ':', errors.array());
    return res.status(400).json({ errors: errors.array() });
  }
  next();
};

// Log all requests to this router
router.use((req, res, next) => {
  console.log(`[ObstacleRouter] ${req.method} ${req.url}`);
  next();
});

/**
 * @route   GET /api/obstacles/nearby
 * @desc    Find obstacles near a specific location
 * @access  Protected (Requires Bearer Token)
 */
router.get('/nearby',
  authMiddleware,
  [
    query('lat').isFloat({ min: -90, max: 90 }),
    query('lng').isFloat({ min: -180, max: 180 }),
    query('radius').optional().isInt({ min: 1, max: 100000 })
  ],
  validate,
  async (req, res) => {
  try {
    const { lat, lng, radius } = req.query;
    const radiusInMeters = parseInt(radius) || 5000;

    console.log('=== Map Search Request ===');
    console.log(`User: ${req.user.email}`);
    console.log(`Location: [${lng}, ${lat}], Radius: ${radiusInMeters}m`);

    const obstacles = await Obstacle.find({
      location: {
        $near: {
          $geometry: {
            type: 'Point',
            coordinates: [parseFloat(lng), parseFloat(lat)],
          },
          $maxDistance: radiusInMeters,
        },
      },
      isActive: true,
    });

    console.log(`Found ${obstacles.length} obstacles in range.`);

    res.json({
      success: true,
      obstacles: obstacles
    });
  } catch (err) {
    console.error('GET Nearby Error:', err.message);
    res.status(500).json({ message: err.message });
  }
});

/**
 * @route   POST /api/obstacles
 * @desc    Submit a new hazard report
 * @access  Protected (Requires Bearer Token)
 */
router.post('/',
  authMiddleware,
  [
    body('type').notEmpty().trim().escape(),
    body('description').optional().trim().escape(),
    body('roadName').optional().trim().escape(),
    body('location.coordinates').isArray({ min: 2, max: 2 }),
    body('location.coordinates.*').isFloat()
  ],
  validate,
  async (req, res) => {
  console.log('=== Received New Report ===');

  try {
    // 1. Prepare data using the verified user from authMiddleware
    // We override reporter info from the token to prevent spoofing
    const obstacleData = {
      ...req.body,
      reporterId: req.user.id,
      reporterName: req.user.displayName || req.user.email,
    };

    const obstacle = new Obstacle(obstacleData);
    const newObstacle = await obstacle.save();

    console.log(`1. Success: Saved ${newObstacle.type} to MongoDB`);

    // 2. Respond to Android app immediately to stop loading toast
    res.status(201).json(newObstacle);

    console.log('2. Success: Response sent to app');

    // 3. Handle notification in the background
    if (
      notificationService &&
      typeof notificationService.broadcastHazard === 'function'
    ) {
      notificationService
        .broadcastHazard(newObstacle)
        .then(() => console.log('3. Optional: Notification broadcasted'))
        .catch((err) =>
          console.error(
            '3. Optional: Notification failed (ignored):',
            err.message
          )
        );
    }
  } catch (err) {
    console.error('!!! Error saving report:', err.message);

    if (!res.headersSent) {
      res.status(400).json({ message: err.message });
    }
  }
});

/**
 * @route   POST /api/obstacles/:id/confirm
 * @desc    Upvote/Confirm a hazard is still there
 */
router.post('/:id/confirm', authMiddleware, async (req, res) => {
  try {
    const obstacle = await Obstacle.findOneAndUpdate(
      { id: req.params.id },
      { $inc: { confirmCount: 1 } },
      { new: true }
    );

    res.json(obstacle);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/**
 * @route   POST /api/obstacles/:id/clear
 * @desc    Mark a hazard as cleared (isActive = false)
 */
router.post('/:id/clear', authMiddleware, async (req, res) => {
  try {
    const obstacle = await Obstacle.findOneAndUpdate(
      { id: req.params.id },
      { isActive: false },
      { new: true }
    );

    res.json(obstacle);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/**
 * @route   GET /api/obstacles/:id/comments
 * @desc    Fetches all comments for a specific hazard
 */
router.get('/:id/comments', authMiddleware, async (req, res) => {
  try {
    const comments = await Comment.find({ obstacleId: req.params.id })
                                 .sort({ timestamp: -1 });
    res.status(200).json(comments);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch comments' });
  }
});

/**
 * @route   POST /api/obstacles/:id/comments
 * @desc    Adds a new comment to a specific hazard
 */
router.post('/:id/comments',
  authMiddleware,
  [
    body('text').notEmpty().trim()
  ],
  validate,
  async (req, res) => {
    console.log(`=== New Comment for Obstacle ${req.params.id} ===`);
    console.log(`Body:`, req.body);
    try {
      const newComment = new Comment({
        obstacleId: req.params.id,
        userName: req.user.displayName || req.user.email || 'Anonymous',
        text: req.body.text,
        timestamp: Date.now()
      });

      const savedComment = await newComment.save();
      res.status(201).json(savedComment);
    } catch (err) {
      console.error('Comment Post Error:', err);
      res.status(400).json({ error: 'Failed to post comment' });
    }
});

module.exports = router;