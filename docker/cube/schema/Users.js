/**
 * Users Cube - User Directory with RLS
 * 
 * Implements Row-Level Security via SECURITY_CONTEXT.tenantId filter
 * Pre-aggregations for common queries (daily status counts, weekly signups)
 */

cube(`Users`, {
  sql: `
    SELECT * FROM users_directory
    WHERE tenant_id = ${SECURITY_CONTEXT.tenantId.filter('tenant_id')}
      AND deleted_at IS NULL
  `,
  
  joins: {
    Tenants: {
      relationship: `belongsTo`,
      sql: `${Users.tenant_id} = ${Tenants.id}`
    },
    
    Manager: {
      relationship: `belongsTo`,
      sql: `${Users.manager_id} = ${Users.id}`
    }
  },
  
  // ============================================================================
  // DIMENSIONS
  // ============================================================================
  
  dimensions: {
    id: {
      sql: `id`,
      type: `string`,
      primaryKey: true
    },
    
    tenantId: {
      sql: `tenant_id`,
      type: `string`
    },
    
    keycloakUserId: {
      sql: `keycloak_user_id`,
      type: `string`
    },
    
    username: {
      sql: `username`,
      type: `string`
    },
    
    email: {
      sql: `email`,
      type: `string`
    },
    
    firstName: {
      sql: `first_name`,
      type: `string`
    },
    
    lastName: {
      sql: `last_name`,
      type: `string`
    },
    
    displayName: {
      sql: `display_name`,
      type: `string`
    },
    
    status: {
      sql: `status`,
      type: `string`
    },
    
    department: {
      sql: `department`,
      type: `string`
    },
    
    title: {
      sql: `title`,
      type: `string`
    },
    
    position: {
      sql: `position`,
      type: `string`
    },
    
    costCenter: {
      sql: `cost_center`,
      type: `string`
    },
    
    location: {
      sql: `location`,
      type: `string`
    },
    
    phone: {
      sql: `phone`,
      type: `string`
    },
    
    phoneNumber: {
      sql: `phone_number`,
      type: `string`
    },
    
    isFederated: {
      sql: `is_federated`,
      type: `boolean`
    },
    
    active: {
      sql: `active`,
      type: `boolean`
    },
    
    createdAt: {
      sql: `created_at`,
      type: `time`
    },
    
    updatedAt: {
      sql: `updated_at`,
      type: `time`
    },
    
    managerUsername: {
      sql: `manager_username`,
      type: `string`
    },
    
    deputyUsername: {
      sql: `deputy_username`,
      type: `string`
    },
    
    version: {
      sql: `version`,
      type: `number`
    }
  },
  
  // ============================================================================
  // MEASURES
  // ============================================================================
  
  measures: {
    count: {
      type: `count`,
      drillMembers: [id, username, email, status]
    },
    
    activeCount: {
      sql: `id`,
      type: `count`,
      filters: [
        { sql: `${CUBE}.active = true` }
      ]
    },
    
    inactiveCount: {
      sql: `id`,
      type: `count`,
      filters: [
        { sql: `${CUBE}.active = false` }
      ]
    },
    
    byStatus: {
      sql: `id`,
      type: `count`
    },
    
    byDepartment: {
      sql: `id`,
      type: `count`
    }
  },
  
  // ============================================================================
  // PRE-AGGREGATIONS
  // ============================================================================
  
  preAggregations: {
    // Daily user status counts - refreshed hourly
    dailyStatusCounts: {
      measures: [count, activeCount, inactiveCount],
      dimensions: [status],
      timeDimension: createdAt,
      granularity: `day`,
      refreshKey: {
        every: `1 hour`,
        incremental: true,
        updateWindow: `7 day`
      },
      partitionGranularity: `month`,
      buildRangeStart: {
        sql: `SELECT DATE_TRUNC('month', NOW() - INTERVAL '3 month')`
      },
      buildRangeEnd: {
        sql: `SELECT DATE_TRUNC('day', NOW())`
      }
    },
    
    // Weekly user registration trend
    weeklySignups: {
      measures: [count],
      dimensions: [tenantId],
      timeDimension: createdAt,
      granularity: `week`,
      refreshKey: {
        every: `6 hours`
      },
      partitionGranularity: `month`,
      buildRangeStart: {
        sql: `SELECT DATE_TRUNC('month', NOW() - INTERVAL '6 month')`
      },
      buildRangeEnd: {
        sql: `SELECT DATE_TRUNC('day', NOW())`
      }
    },
    
    // Department rollup for org charts
    departmentRollup: {
      measures: [count, activeCount],
      dimensions: [department, status],
      refreshKey: {
        every: `2 hours`
      }
    }
  },
  
  // ============================================================================
  // SEGMENTS
  // ============================================================================
  
  segments: {
    activeUsers: {
      sql: `${CUBE}.active = true AND ${CUBE}.deleted_at IS NULL`
    },
    
    federatedUsers: {
      sql: `${CUBE}.is_federated = true`
    },
    
    localUsers: {
      sql: `${CUBE}.is_federated = false`
    }
  }
});
