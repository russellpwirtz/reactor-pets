export const config = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api',
  pollingInterval: parseInt(process.env.NEXT_PUBLIC_POLLING_INTERVAL || '5000'),
} as const;
