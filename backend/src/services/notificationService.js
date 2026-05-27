const { admin } = require('../config/firebase');
const Token = require('../models/Token');

/**
 * Broadcasts a hazard alert to all registered devices.
 * In a real-world app, you might filter tokens by proximity.
 */
exports.broadcastHazard = async (obstacle) => {
  try {
    const tokensDoc = await Token.find({});
    const tokens = tokensDoc.map(t => t.token);

    if (tokens.length === 0) return;

    const message = {
      data: {
        obstacleType: obstacle.type,
        distance: 'nearby', // In a full impl, calculate this based on recipient location
        latitude: obstacle.location.coordinates[1].toString(),
        longitude: obstacle.location.coordinates[0].toString()
      },
      tokens: tokens
    };

    const messaging = admin.messaging();
    const response = typeof messaging.sendEachForMulticast === 'function'
      ? await messaging.sendEachForMulticast(message)
      : await Promise.all(
          tokens.map(token => messaging.send({
            data: message.data,
            token
          }))
        ).then(results => ({
          successCount: results.length,
          failureCount: 0
        }));

    console.log(`Successfully sent ${response.successCount} notifications`);

    // Cleanup invalid tokens
    if (response.failureCount > 0) {
      // Logic to remove tokens that are no longer valid
    }
  } catch (error) {
    console.error('Error broadcasting hazard:', error);
  }
};
