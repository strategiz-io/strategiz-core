# SonarQube Community Edition for Cloud Run
FROM sonarqube:10-community

# Cloud Run requirements
ENV SONAR_WEB_PORT=8080
ENV SONAR_WEB_CONTEXT=/

# Set user to run SonarQube (Cloud Run requires non-root)
USER sonarqube

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8080/api/system/health || exit 1

# Expose port 8080 for Cloud Run
EXPOSE 8080
