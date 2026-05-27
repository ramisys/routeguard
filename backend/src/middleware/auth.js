const { admin } = require('../config/firebase');
const User = require('../models/User');

const authMiddleware = async (req, res, next) => {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ message: 'No token provided' });
  }

  const idToken = header.split('Bearer ')[1];

  try {
    // 1. Verify the token with Firebase
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const { uid, email, name } = decodedToken;

    // 2. Find the user in your MongoDB or CREATE them if they don't exist
    let user = await User.findOne({ id: uid });
    
    if (!user) {
      user = new User({
        id: uid,
        email: email,
        displayName: name || 'New User'
      });
      await user.save();
      console.log(`Created new user in MongoDB: ${email}`);
    }

    // 3. Attach the user object to the request so other routes can use it
    req.user = user;
    next();
  } catch (error) {
    console.error('Auth Error:', error.message);
    res.status(403).json({ message: 'Unauthorized' });
  }
};

module.exports = authMiddleware;