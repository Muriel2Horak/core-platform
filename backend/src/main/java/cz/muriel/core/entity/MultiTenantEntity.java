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

@MappedSuperclass @FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantKey", type = String.class)) @Filter(name = "tenantFilter", condition = "tenant_key = :tenantKey") @Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public abstract class MultiTenantEntity {

  @Column(name = "tenant_key", nullable = false)
  private String tenantKey;
}
