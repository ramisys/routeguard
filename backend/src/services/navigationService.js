const axios = require('axios');
const turf = require('@turf/turf');
const Obstacle = require('../models/Obstacle');

/**
 * Navigation Service
 * Handles route fetching and safety analysis
 */
class NavigationService {
  constructor() {
    this.osrmBaseUrl = 'http://router.project-osrm.org/route/v1/driving';
  }

  /**
   * Fetches routes and analyzes them for hazards
   */
  async getSafeRoutes(startCoords, endCoords) {
    try {
      // 1. Fetch routes from OSRM
      // Format: {longitude},{latitude};{longitude},{latitude}
      const url = `${this.osrmBaseUrl}/${startCoords};${endCoords}?overview=full&geometries=geojson&alternatives=true`;
      const response = await axios.get(url);

      if (!response.data || !response.data.routes) {
        throw new Error('No routes found');
      }

      const routes = response.data.routes;

      // 2. Analyze each route
      const analyzedRoutes = await Promise.all(routes.map(async (route, index) => {
        return await this.analyzeRouteSafety(route, index);
      }));

      // 3. Rank routes by safety and then by duration
      analyzedRoutes.sort((a, b) => {
        if (a.safetyScore !== b.safetyScore) {
          return b.safetyScore - a.safetyScore; // Higher score first
        }
        return a.duration - b.duration; // Shorter duration second
      });

      return analyzedRoutes;
    } catch (error) {
      console.error('Error in NavigationService:', error.message);
      throw error;
    }
  }

  /**
   * Analyzes a single route against existing obstacles
   */
  async analyzeRouteSafety(route, index) {
    const routeLine = turf.lineString(route.geometry.coordinates);

    // Get bbox of route to optimize DB query
    const bbox = turf.bbox(routeLine);

    // Find all active obstacles within the route's bounding box
    // To be safer, we could expand the bbox slightly
    const obstacles = await Obstacle.find({
      isActive: true,
      location: {
        $geoWithin: {
          $box: [
            [bbox[0] - 0.01, bbox[1] - 0.01], // bottom-left
            [bbox[2] + 0.01, bbox[3] + 0.01]  // top-right
          ]
        }
      }
    });

    let safetyScore = 100;
    const hazardsOnRoute = [];

    obstacles.forEach(obstacle => {
      const obstaclePoint = turf.point(obstacle.location.coordinates);
      // Distance from obstacle to the route line in kilometers
      const distance = turf.pointToLineDistance(obstaclePoint, routeLine, { units: 'kilometers' });

      // Assume a hazard has an impact radius (e.g., 0.1km / 100m)
      const impactRadius = 0.1;

      if (distance < impactRadius) {
        const severityWeight = this.getSeverityWeight(obstacle.severity);
        const penalty = (1 - (distance / impactRadius)) * severityWeight;
        safetyScore -= penalty;

        hazardsOnRoute.push({
          id: obstacle.id,
          type: obstacle.type,
          severity: obstacle.severity,
          distance: distance,
          coordinates: obstacle.location.coordinates
        });
      }
    });

    return {
      id: `route_${index}`,
      geometry: route.geometry,
      duration: route.duration,
      distance: route.distance,
      safetyScore: Math.max(0, Math.round(safetyScore)),
      hazards: hazardsOnRoute,
      isRecommended: false // Will be set by caller after ranking
    };
  }

  getSeverityWeight(severity) {
    switch (severity) {
      case 'CRITICAL': return 50;
      case 'HIGH': return 30;
      case 'MODERATE': return 15;
      case 'LOW': return 5;
      default: return 10;
    }
  }
}

module.exports = new NavigationService();
