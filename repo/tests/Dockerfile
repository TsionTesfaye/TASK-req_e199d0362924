FROM node:18-alpine
WORKDIR /tests
RUN apk add --no-cache curl bash
COPY package.json ./
RUN npm install --omit=dev
COPY api.test.js run.sh ./
RUN chmod +x run.sh
CMD ["./run.sh"]
