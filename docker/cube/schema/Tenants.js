/**
 * Tenants Cube - Tenant Registry
 * 
 * Minimal tenant information - display names fetched from Keycloak
 * RLS applied via SECURITY_CONTEXT
 */

cube(`Tenants`, {
  sql: `
    SELECT * FROM tenants
    WHERE id = ${SECURITY_CONTEXT.tenantId.filter('id')}
  `,
  
  joins: {
    Users: {
      relationship: `hasMany`,
      sql: `${Tenants.id} = ${Users.tenant_id}`
    },
    
    Groups: {
      relationship: `hasMany`,
      sql: `${Tenants.id} = ${Groups.tenant_id}`
    }
  },
  
  // ============================================================================
  // DIMENSIONS
  // ============================================================================
  
  dimensions: {
    id: {
      sql: `id`,
      type: `string`,
      primaryKey: true,
      shown: true
    },
    
    key: {
      sql: `key`,
      type: `string`,
      title: `Tenant Key`
    },
    
    keycloakRealmId: {
      sql: `keycloak_realm_id`,
      type: `string`,
      title: `Keycloak Realm ID`
    }
  },
  
  // ============================================================================
  // MEASURES
  // ============================================================================
  
  measures: {
    count: {
      type: `count`,
      drillMembers: [id, key]
    }
  },
  
  // ============================================================================
  // PRE-AGGREGATIONS
  // ============================================================================
  
  preAggregations: {
    // Simple rollup for tenant stats
    main: {
      measures: [count],
      dimensions: [id, key],
      refreshKey: {
        every: `1 day`
      }
    }
  }
});
