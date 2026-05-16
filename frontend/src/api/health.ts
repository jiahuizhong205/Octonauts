import http from './http';

export interface HealthResponse {
  code: number;
  message: string;
  data: {
    status: string;
  };
}

export function fetchHealth() {
  return http.get<HealthResponse>('/v1/health');
}
