# Use an official Node.js image
FROM node:18-alpine

WORKDIR /app

# Only copy package files first (enables Docker cache)
COPY package*.json ./

# Install dependencies
RUN npm install --legacy-peer-deps

# Now copy the rest of the app
COPY . .

# Expose frontend port
EXPOSE 3000

# Run the app
CMD ["npm", "start"]
