# Deployment Guide

This document provides instructions for deploying the Strategiz Core backend in different environments.

## Prerequisites

Before deployment, ensure you have the following:

- Node.js (v16 or higher)
- npm (v6 or higher)
- Firebase account and project
- Firebase CLI (`npm install -g firebase-tools`)
- Java Development Kit (JDK) 11 or higher
- Maven 3.6 or higher

## Environment Configuration

### Environment Variables

Create a `.env` file in the root directory based on the `.env.example` file:

```
cp .env.example .env
```

Required environment variables:

```
# Server Configuration
PORT=8080
NODE_ENV=development

# Firebase Configuration
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_DATABASE_URL=https://your-project-id.firebaseio.com
FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com

# Exchange API Keys (For Testing Only)
TEST_BINANCE_API_KEY=your_test_binance_api_key
TEST_BINANCE_API_SECRET=your_test_binance_api_secret
TEST_KRAKEN_API_KEY=your_test_kraken_api_key
TEST_KRAKEN_API_SECRET=your_test_kraken_api_secret
```

> **Note**: Never commit your `.env` file to version control. It's already included in `.gitignore`.

### Firebase Service Account

Download your Firebase service account key and save it as `serviceAccountKey.json` in the `config` directory.

## Local Development

### Running the Backend Server

To run the server locally:

```bash
# Using Maven
mvn spring-boot:run

# Or using the provided script
./mvnw spring-boot:run
```

The server will start on port 8080 by default (or the port specified in your `application.properties` file).

### Running with Docker

If you prefer Docker, you can use the provided Dockerfile:

```bash
# Build the Docker image
docker build -t strategiz-core .

# Run the container
docker run -p 8080:8080 strategiz-core
```

## Production Deployment

### Preparing for Production

1. Update `application-prod.properties` with production-specific configurations
2. Build the application:

```bash
mvn clean package -P prod
```

This will create a JAR file in the `target` directory.

### Deploying to Firebase Functions

This API is designed to be deployed as a Firebase Cloud Function. Follow these steps to deploy:

1. Login to Firebase (if not already logged in):
   ```bash
   firebase login
   ```

2. Initialize your Firebase project (if not already initialized):
   ```bash
   firebase init
   ```
   - Select "Functions" when prompted
   - Select your Firebase project
   - Choose "Use an existing project" and select your project
   - Select JavaScript when asked about language
   - Choose "No" when asked about ESLint
   - Choose "Yes" to install dependencies

3. Deploy to Firebase:
   ```bash
   npm run deploy
   ```
   
   Or use the provided deployment script:
   ```bash
   ./deploy.bat
   ```

4. After deployment, your API will be available at:
   ```
   https://us-central1-[YOUR-PROJECT-ID].cloudfunctions.net/api
   ```

5. Update the client-side application to use this URL by setting the `REACT_APP_API_URL` environment variable.

### Deploying to a VPS or Cloud Provider

For deploying to a VPS or cloud provider like AWS, Azure, or Google Cloud:

1. Copy the JAR file to your server
2. Create a `application-prod.properties` file on the server
3. Run the application:

```bash
java -jar strategiz-core.jar --spring.profiles.active=prod
```

### Using a Process Manager

For production deployments, use a process manager like systemd or PM2:

#### systemd (Linux)

Create a systemd service file at `/etc/systemd/system/strategiz-core.service`:

```
[Unit]
Description=Strategiz Core
After=network.target

[Service]
User=yourusername
WorkingDirectory=/path/to/app
ExecStart=/usr/bin/java -jar strategiz-core.jar --spring.profiles.active=prod
Restart=always

[Install]
WantedBy=multi-user.target
```

Then enable and start the service:

```bash
sudo systemctl enable strategiz-core
sudo systemctl start strategiz-core
```

## Monitoring and Logging

### Viewing Logs

#### Firebase Logs

To view the logs for your deployed Firebase functions:

```bash
npm run logs
```

Or:

```bash
firebase functions:log
```

#### Server Logs

Server logs are stored in the `logs` directory by default. In production, consider using a logging service like Logstash, Fluentd, or CloudWatch.

### Health Checks

The application exposes a health check endpoint at `/health` that can be used by monitoring services to verify the application is running correctly.

## Troubleshooting

### Common Deployment Issues

#### Port Already in Use
- Check if port 8080 is already in use with `netstat -ano | findstr 8080` (Windows) or `lsof -i :8080` (Linux/Mac)
- Change the port in `application.properties` if needed

#### Firebase Connection Issues
- Verify that your `serviceAccountKey.json` file is valid and has the correct permissions
- Check if your Firebase project has the necessary services enabled (Firestore, Authentication)

#### Java Version Conflicts
- Ensure you're using a compatible JDK version (11 or higher)
- Check for conflicting JDK installations with `java -version` and `which java`

#### Memory Issues
- If the application crashes with out-of-memory errors, adjust the JVM heap size:
  ```bash
  java -Xmx512m -jar strategiz-core.jar
  ```

### Getting Help

If you encounter issues not covered here, please:
1. Check the application logs for specific error messages
2. Search for the error in the project's issue tracker
3. Create a new issue with detailed information about the problem
