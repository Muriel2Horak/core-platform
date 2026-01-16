import axios, { type AxiosRequestConfig, type AxiosResponse } from "axios";
import CircuitBreaker from "opossum";

export type BackendClient = {
  get: (url: string) => Promise<AxiosResponse>;
  post: (url: string, data?: unknown) => Promise<AxiosResponse>;
};

const baseURL = process.env.BFF_BACKEND_URL || process.env.BACKEND_URL || "http://backend:8080";
const timeout = Number(process.env.BFF_BACKEND_TIMEOUT_MS || 5000);

const axiosInstance = axios.create({
  baseURL,
  timeout,
});

const breaker = new CircuitBreaker(
  async (config: AxiosRequestConfig) => axiosInstance.request(config),
  {
    timeout,
    errorThresholdPercentage: 50,
    resetTimeout: 30000,
  }
);

breaker.on("open", () => {
  console.warn("BFF circuit breaker open: backend requests failing");
});

breaker.on("halfOpen", () => {
  console.warn("BFF circuit breaker half-open: probing backend");
});

export const createBackendClient = (input: {
  token?: string;
  tenantId: string;
}): BackendClient => {
  const headers: Record<string, string> = {
    "X-Tenant-Id": input.tenantId,
  };

  if (input.token) {
    headers.Authorization = `Bearer ${input.token}`;
  }

  const request = (config: AxiosRequestConfig) =>
    breaker.fire({
      ...config,
      headers: {
        ...headers,
        ...(config.headers || {}),
      },
    });

  return {
    get: (url) => request({ method: "get", url }),
    post: (url, data) => request({ method: "post", url, data }),
  };
};
