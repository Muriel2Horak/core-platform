import type { Request } from "express";
import jwt from "jsonwebtoken";
import Redis from "ioredis";
import { createBackendClient, type BackendClient } from "./clients/backend";
import { createUserLoader } from "./dataloaders/user";

export type BffContext = {
  tenantId: string;
  token?: string;
  redis: Redis;
  backendClient: BackendClient;
  dataloaders: {
    userLoader: ReturnType<typeof createUserLoader>;
  };
};

let redisClient: Redis | null = null;

const getRedisClient = (): Redis => {
  if (redisClient) {
    return redisClient;
  }

  if (process.env.REDIS_URL) {
    redisClient = new Redis(process.env.REDIS_URL);
    return redisClient;
  }

  const host = process.env.REDIS_HOST || "redis";
  const port = Number(process.env.REDIS_PORT || 6379);
  const password = process.env.REDIS_PASSWORD || undefined;

  redisClient = new Redis({ host, port, password });
  return redisClient;
};

const extractTenantId = (req: Request, token?: string): string => {
  const claimName =
    process.env.BFF_TENANT_CLAIM ||
    process.env.AUTH_JWT_TENANT_CLAIM ||
    "tenant";

  if (token) {
    const decoded = jwt.decode(token) as Record<string, unknown> | null;
    const tenant = decoded?.[claimName];
    if (typeof tenant === "string" && tenant.length > 0) {
      return tenant;
    }
  }

  const headerTenant = req.headers["x-tenant-id"];
  if (typeof headerTenant === "string" && headerTenant.length > 0) {
    return headerTenant;
  }

  return process.env.BFF_DEFAULT_TENANT || "default";
};

export const createContext = (req: Request): BffContext => {
  const authHeader = req.headers.authorization || "";
  const token = authHeader.startsWith("Bearer ")
    ? authHeader.slice("Bearer ".length)
    : undefined;

  const tenantId = extractTenantId(req, token);
  const redis = getRedisClient();
  const backendClient = createBackendClient({ token, tenantId });

  return {
    tenantId,
    token,
    redis,
    backendClient,
    dataloaders: {
      userLoader: createUserLoader(tenantId, backendClient),
    },
  };
};
