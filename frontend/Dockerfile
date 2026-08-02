# Local dev only — see /SPEC.md "Local Development". Production deploys don't use this image;
# cd-frontend.yml builds the Vite production bundle and ships it via the Vercel CLI instead.

FROM node:25-alpine
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
