import { gql } from "apollo-server-express";

export const typeDefs = gql`
  type Query {
    dashboard: Dashboard!
    user(id: ID!): User
    tenant: TenantConfig!
    workflows: [WorkflowSummary!]!
  }

  type Dashboard {
    stats: DashboardStats!
    recentActivities: [Activity!]!
    topUsers: [User!]!
    alerts: [Alert!]!
  }

  type DashboardStats {
    totalUsers: Int!
    activeWorkflows: Int!
    successRate: Float!
    avgExecutionTime: Float!
  }

  type Activity {
    id: ID!
    type: String!
    user: User!
    timestamp: String!
    description: String!
  }

  type User {
    id: ID!
    name: String!
    email: String!
    role: Role!
  }

  type Role {
    id: ID!
    name: String!
    permissions: [String!]!
  }

  type Alert {
    severity: AlertSeverity!
    message: String!
    timestamp: String!
  }

  enum AlertSeverity {
    INFO
    WARNING
    ERROR
    CRITICAL
  }

  type TenantConfig {
    name: String!
    logo: String!
    primaryColor: String!
    locale: String!
    currency: String!
    features: [String!]!
  }

  type WorkflowSummary {
    id: ID!
    name: String!
    status: String!
    updatedAt: String!
  }
`;
