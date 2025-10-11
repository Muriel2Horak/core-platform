/**
 * Groups Cube - Keycloak Groups with RLS
 * 
 * Hierarchical group structure with parent-child relationships
 * Pre-aggregations for common group queries
 */

cube(`Groups`, {
  sql: `
    SELECT * FROM groups
    WHERE tenant_id = ${SECURITY_CONTEXT.tenantId.filter('tenant_id')}
  `,
  
  joins: {
    Tenants: {
      relationship: `belongsTo`,
      sql: `${Groups.tenant_id} = ${Tenants.id}`
    },
    
    ParentGroup: {
      relationship: `belongsTo`,
      sql: `${Groups.parent_group_id} = ${Groups.id}`
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
    
    keycloakGroupId: {
      sql: `keycloak_group_id`,
      type: `string`
    },
    
    tenantId: {
      sql: `tenant_id`,
      type: `string`
    },
    
    name: {
      sql: `name`,
      type: `string`
    },
    
    path: {
      sql: `path`,
      type: `string`
    },
    
    parentGroupId: {
      sql: `parent_group_id`,
      type: `string`
    },
    
    createdAt: {
      sql: `created_at`,
      type: `time`
    },
    
    updatedAt: {
      sql: `updated_at`,
      type: `time`
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
      drillMembers: [id, name, path]
    },
    
    topLevelGroupsCount: {
      sql: `id`,
      type: `count`,
      filters: [
        { sql: `${CUBE}.parent_group_id IS NULL` }
      ]
    },
    
    childGroupsCount: {
      sql: `id`,
      type: `count`,
      filters: [
        { sql: `${CUBE}.parent_group_id IS NOT NULL` }
      ]
    }
  },
  
  // ============================================================================
  // PRE-AGGREGATIONS
  // ============================================================================
  
  preAggregations: {
    // Daily group hierarchy rollup
    dailyGroupCounts: {
      measures: [count, topLevelGroupsCount, childGroupsCount],
      dimensions: [tenantId],
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
    
    // Group hierarchy snapshot
    groupHierarchy: {
      measures: [count],
      dimensions: [name, path, parentGroupId],
      refreshKey: {
        every: `2 hours`
      }
    }
  },
  
  // ============================================================================
  // SEGMENTS
  // ============================================================================
  
  segments: {
    topLevelGroups: {
      sql: `${CUBE}.parent_group_id IS NULL`
    },
    
    childGroups: {
      sql: `${CUBE}.parent_group_id IS NOT NULL`
    }
  }
});
