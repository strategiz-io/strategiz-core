FROM node:14

# Install Firebase CLI
RUN npm install -g firebase-tools

# Set the entrypoint to firebase
ENTRYPOINT ["firebase"]
