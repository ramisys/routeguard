const express = require('express');
const router = express.Router();
const cloudinary = require('../config/cloudinary');
const authMiddleware = require('../middleware/auth');

/**
 * @route   GET /api/media/sign
 * @desc    Generate a signature for Cloudinary signed upload
 * @access  Protected
 */
router.get('/sign', authMiddleware, (req, res) => {
  try {
    if (!process.env.CLOUDINARY_API_SECRET || !process.env.CLOUDINARY_API_KEY) {
      console.error('Cloudinary environment variables are missing');
      return res.status(500).json({ message: 'Server configuration error: Cloudinary secrets missing' });
    }

    const timestamp = Math.round(new Date().getTime() / 1000);
    const paramsToSign = {
      timestamp: timestamp,
      folder: 'obstacles'
    };

    const signature = cloudinary.utils.api_sign_request(
      paramsToSign,
      process.env.CLOUDINARY_API_SECRET
    );

    res.json({
      signature,
      timestamp,
      cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
      api_key: process.env.CLOUDINARY_API_KEY
    });
  } catch (err) {
    console.error('Signature Generation Error:', err);
    res.status(500).json({ message: 'Failed to generate upload signature', error: err.message });
  }
});

module.exports = router;
