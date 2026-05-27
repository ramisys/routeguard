require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const mongoSanitize = require('mongo-sanitize');
const connectDB = require('./config/db');
const { initializeFirebase } = require('./config/firebase');

// Connect to Database
connectDB();

// Initialize Firebase Admin
initializeFirebase();

const app = express();
const PORT = process.env.PORT || 3000;

// 1. Security Headers
app.use(helmet());

// 2. Rate Limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: process.env.NODE_ENV === 'production' ? 100 : 1000, // Higher limit for development
  standardHeaders: true,
  legacyHeaders: false,
  message: 'Too many requests from this IP, please try again after 15 minutes'
});
app.use('/api/', limiter);

// 3. CORS Configuration
const allowedOrigins = process.env.ALLOWED_ORIGINS
  ? process.env.ALLOWED_ORIGINS.split(',')
  : ['http://localhost:3000'];

app.use(cors({
  origin: (origin, callback) => {
    // allow requests with no origin (like mobile apps or curl requests)
    if (!origin) return callback(null, true);
    if (allowedOrigins.indexOf(origin) === -1) {
      const msg = 'The CORS policy for this site does not allow access from the specified Origin.';
      return callback(new Error(msg), false);
    }
    return callback(null, true);
  },
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
}));

// 4. Body Parser and NoSQL Injection Prevention
app.use(express.json({ limit: '10kb' })); // Limit body size
app.use((req, res, next) => {
  req.body = mongoSanitize(req.body);
  next();
});

// Basic Route
app.get('/', (req, res) => {
  res.send('RouteGuard Backend is running securely');
});

// Import Routes
const obstacleRoutes = require('./routes/obstacleRoutes');
const userRoutes = require('./routes/userRoutes');
const mediaRoutes = require('./routes/mediaRoutes');

app.use('/api/obstacles', obstacleRoutes);
app.use('/api/users', userRoutes);
app.use('/api/media', mediaRoutes);

// 5. Global Error Handler
app.use((err, req, res, next) => {
  console.error(err.stack);

  const status = err.statusCode || 500;
  const message = err.message || 'Internal Server Error';

  res.status(status).json({
    status: 'error',
    message: process.env.NODE_ENV === 'production' ? 'Something went wrong' : message,
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack })
  });
});

// Listen on all interfaces
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server is running in ${process.env.NODE_ENV} mode on port ${PORT}`);
});
