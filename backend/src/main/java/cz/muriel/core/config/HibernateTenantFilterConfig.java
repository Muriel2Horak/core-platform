package cz.muriel.core.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import cz.muriel.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * AOP Aspect pro automatické zapínání Hibernate tenant filtru při každém
 * přístupu k databázi přes service vrstvu.
 */
@Aspect @Component @Slf4j @Configuration
public class HibernateTenantFilterConfig {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Interceptuje všechny metody v service package a automaticky zapne tenant
   * filtr s aktuálním tenant kontextem.
   */
  @Around("execution(* cz.muriel.core.service..*(..))")
  public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
    String tenantKey = TenantContext.getTenantKey();

    if (tenantKey == null) {
      log.debug("No tenant context found - proceeding without filter");
      return joinPoint.proceed();
    }

    Session session = entityManager.unwrap(Session.class);

    try {
      // 🎯 SIMPLIFIED: Use tenantKey directly (no UUID conversion needed)
      session.enableFilter("tenantFilter").setParameter("tenantKey", tenantKey);
      log.debug("Enabled tenant filter for tenant: {}", tenantKey);

      return joinPoint.proceed();

    } finally {
      // Vypni filtr po dokončení operace
      session.disableFilter("tenantFilter");
      log.debug("Disabled tenant filter");
    }
  }
}
