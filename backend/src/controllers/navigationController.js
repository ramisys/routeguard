const navigationService = require('../services/navigationService');

exports.getRoutes = async (req, res) => {
  try {
    const { start, end } = req.query;

    if (!start || !end) {
      return res.status(400).json({ error: 'Start and end coordinates are required (lng,lat)' });
    }

    const routes = await navigationService.getSafeRoutes(start, end);

    // Mark the first one (after sorting) as recommended
    if (routes.length > 0) {
      routes[0].isRecommended = true;
    }

    res.json({
      success: true,
      count: routes.length,
      routes: routes
    });
  } catch (error) {
    console.error('Navigation Controller Error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to retrieve routes',
      error: error.message
    });
  }
};
