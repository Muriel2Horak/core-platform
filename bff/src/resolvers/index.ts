import type { BffContext } from "../context";

const cacheGet = async <T>(
  context: BffContext,
  key: string
): Promise<T | null> => {
  try {
    const cached = await context.redis.get(key);
    return cached ? (JSON.parse(cached) as T) : null;
  } catch (error) {
    console.warn(`BFF cache read failed for ${key}`, error);
    return null;
  }
};

const cacheSet = async (
  context: BffContext,
  key: string,
  ttlSeconds: number,
  value: unknown
) => {
  try {
    await context.redis.setex(key, ttlSeconds, JSON.stringify(value));
  } catch (error) {
    console.warn(`BFF cache write failed for ${key}`, error);
  }
};

export const resolvers = {
  Query: {
    dashboard: async (_: unknown, __: unknown, context: BffContext) => {
      const cacheKey = `tenant:${context.tenantId}:dashboard`;
      const cached = await cacheGet(context, cacheKey);
      if (cached) {
        return cached;
      }

      const [stats, activities, users, alerts] = await Promise.all([
        context.backendClient.get(`/api/tenants/${context.tenantId}/stats`),
        context.backendClient.get(`/api/tenants/${context.tenantId}/activities`),
        context.backendClient.get(`/api/tenants/${context.tenantId}/users/top`),
        context.backendClient.get(`/api/tenants/${context.tenantId}/alerts`),
      ]);

      const response = {
        stats: stats.data,
        recentActivities: activities.data,
        topUsers: users.data,
        alerts: alerts.data,
      };

      await cacheSet(context, cacheKey, 60, response);

      return response;
    },

    user: async (
      _: unknown,
      args: { id: string },
      context: BffContext
    ) => context.dataloaders.userLoader.load(args.id),

    tenant: async (_: unknown, __: unknown, context: BffContext) => {
      const cacheKey = `tenant:${context.tenantId}:config`;
      const cached = await cacheGet(context, cacheKey);
      if (cached) {
        return cached;
      }

      const { data } = await context.backendClient.get(
        `/api/tenants/${context.tenantId}/config`
      );

      await cacheSet(context, cacheKey, 300, data);

      return data;
    },

    workflows: async (_: unknown, __: unknown, context: BffContext) => {
      const cacheKey = `tenant:${context.tenantId}:workflows`;
      const cached = await cacheGet(context, cacheKey);
      if (cached) {
        return cached;
      }

      const { data } = await context.backendClient.get(
        `/api/tenants/${context.tenantId}/workflows/summary`
      );

      await cacheSet(context, cacheKey, 60, data);

      return data;
    },
  },
};
