const express = require('express');
const router = express.Router();
const navigationController = require('../controllers/navigationController');

// GET /api/navigation/routes?start=lng,lat&end=lng,lat
router.get('/routes', navigationController.getRoutes);

module.exports = router;
