/**
 * All API calls go to a relative "/api" URL:
 * - `npm start` (ng serve) proxies /api to the gateway on localhost:8080 (see proxy.conf.json)
 * - in Docker, nginx proxies /api to the api-gateway container (see nginx.conf)
 * Same origin in both cases, so the browser never needs CORS.
 */
export const API_BASE = '/api';
