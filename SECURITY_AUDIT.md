# Security Audit & Improvements Report - RouteGuard

This document outlines the security vulnerabilities identified in the RouteGuard application and the measures implemented to mitigate them.

## 1. Secrets Management
### Vulnerability: Hardcoded Secrets
**Observation:** The backend had `MONGODB_URI` hardcoded in `server.js` and was using a direct file path for `firebase-auth.json`.
**Risk:** Hardcoding secrets leads to accidental exposure in version control (Git).
**Improvement:** 
- Created `.env.example` to define required environment variables.
- Updated `server.js` and configuration files to use `process.env`.
- Added `.env` to `.gitignore`.
- Centralized Firebase initialization in `backend/src/config/firebase.js`.

## 2. API Security
### Vulnerability: Lack of Security Headers & Rate Limiting
**Observation:** The Express server was missing basic security headers and protection against brute-force attacks.
**Risk:** Vulnerability to Cross-Site Scripting (XSS), clickjacking, and Denial of Service (DoS).
**Improvement:** 
- Integrated `helmet` middleware to set secure HTTP headers.
- Implemented `express-rate-limit` to prevent API abuse.
- Configured CORS with allowed origins.

### Vulnerability: NoSQL Injection
**Observation:** User input was used directly in MongoDB queries.
**Risk:** Attackers could bypass authentication or extract sensitive data using NoSQL injection.
**Improvement:** 
- Added `mongo-sanitize` to clean `req.body` from all requests.
- Implemented `express-validator` for schema-based validation on critical routes (e.g., `POST /api/obstacles`).

## 3. Authentication & Authorization
### Vulnerability: Insufficient Token Verification
**Observation:** While an `authMiddleware` existed, it wasn't consistently applied, and user roles weren't managed.
**Risk:** Unauthorized users could perform actions reserved for admins or other users.
**Improvement:** 
- Updated `authMiddleware` to use the centralized Firebase Admin instance.
- Added a `role` field to the `User` model.
- Created `roleMiddleware.js` for Role-Based Access Control (RBAC).

## 4. Android Security
### Vulnerability: Insecure Storage & Verbose Logging
**Observation:** The app used standard `SharedPreferences` and logged full request/response bodies in non-debug builds.
**Risk:** Sensitive data (if any) could be read from the device storage. Auth tokens could be leaked through system logs.
**Improvement:** 
- Added `androidx.security:security-crypto` library.
- Created `SecurePrefsManager` using `EncryptedSharedPreferences` for secure data persistence.
- Configured `RetrofitClient` to reduce logging level in release builds.
- Enabled ProGuard/R8 obfuscation in `build.gradle.kts`.

## 5. Media Security (Cloudinary)
### Vulnerability: Hardcoded Upload Presets & Unsigned Uploads
**Observation:** The app used hardcoded `upload_preset` and allowed `unsigned` uploads.
**Risk:** Anyone with your `cloud_name` and `upload_preset` could upload any image to your Cloudinary storage, potentially leading to storage exhaustion or hosting of malicious content.
**Improvement:**
- Switched to **Signed Uploads**.
- Implemented a backend signing service (`/api/media/sign`) that generates a signature using the `CLOUDINARY_API_SECRET`.
- Removed `api_secret` and sensitive presets from the Android app.
- Moved `cloud_name` to `BuildConfig`.

## 6. Production Recommendations
- **HTTPS:** Always use TLS/SSL for communication between the app and backend.
- **Nginx:** Use Nginx as a reverse proxy to handle SSL termination and further rate limiting.
- **Logging:** Move away from local Room logging for errors and use a production-grade service like Sentry or Firebase Crashlytics.
- **Monitoring:** Implement health check endpoints and use Prometheus/Grafana or Datadog for system health monitoring.
