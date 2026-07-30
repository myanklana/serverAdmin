FROM node:18-alpine

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied when available
COPY package*.json ./
RUN npm ci --only=production || npm install

# Bundle app source
COPY . .

# If your app binds to a different port, change this value
EXPOSE 3000

# Default command (override with docker run <image> <cmd>)
CMD ["npm", "start"]