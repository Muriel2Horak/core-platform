import DataLoader from "dataloader";
import type { BackendClient } from "../clients/backend";

type UserRecord = {
  id: string;
  name: string;
  email: string;
  role?: {
    id: string;
    name: string;
    permissions?: string[];
  };
};

export const createUserLoader = (tenantId: string, backendClient: BackendClient) =>
  new DataLoader(async (userIds: readonly string[]) => {
    const { data } = await backendClient.post(
      `/api/tenants/${tenantId}/users/batch`,
      { ids: userIds }
    );

    const users = Array.isArray(data) ? (data as UserRecord[]) : [];
    const byId = new Map(users.map((user) => [user.id, user]));

    return userIds.map((id) => byId.get(id) || null);
  });
