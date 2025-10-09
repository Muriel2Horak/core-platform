package cz.muriel.core.entity;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass @FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class)) @Filter(name = "tenantFilter", condition = "tenant_id = :tenantId") @Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public abstract class MultiTenantEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;
}
