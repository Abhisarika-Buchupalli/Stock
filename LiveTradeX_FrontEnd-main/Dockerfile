# Use an official Node.js image
FROM node:20-alpine

WORKDIR /app

# Only copy package files first (enables Docker cache)
COPY package*.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Now copy the rest of the app
COPY . .

# Expose frontend port
EXPOSE 3000

# Run the app
CMD ["npm", "start"]
