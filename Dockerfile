# --- STAGE 1: Build ---
# Start with the official JDK 25 image
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Install Maven manually (this ensures you have exactly what you need)
RUN apt-get update && apt-get install -y maven

# Build the app
COPY src ./src
RUN mvn clean test
RUN ls -la target/

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy the build output to Nginx's html folder
# Replace 'your-app-name' with the name found in your angular.json

COPY --from=build /target/index.html /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

