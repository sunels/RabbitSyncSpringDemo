# Use a base image suitable for your application, e.g., OpenJDK for Java applications
FROM openjdk:17

# Set environment variable for port (default to 8080 if not provided)
ENV PORT=8080

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled application JAR file into the container
COPY target/user-service-0.0.1.jar /app/user-service-0.0.1.jar

# Expose the port where the Spring Boot application will run
EXPOSE $PORT

# Command to run the Spring Boot application with the provided port
CMD ["java", "-jar", "user-service-0.0.1.jar", "--server.port=${PORT}"]
