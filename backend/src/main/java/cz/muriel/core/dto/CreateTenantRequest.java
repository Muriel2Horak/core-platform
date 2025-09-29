package cz.muriel.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 🏢 CREATE TENANT REQUEST DTO A * DTO pro vytvoření nového tenantu v systému
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTenantRequest {

  /**
   * Tenant key - používá se jako realm name a subdoména Musí být DNS-safe
   * (lowercase, číslice, pomlčky)
   */
  @NotBlank(message = "Tenant key je povinný") @Size(min = 3, max = 50, message = "Tenant key musí být 3-50 znaků") @Pattern(regexp = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$", message = "Tenant key může obsahovat pouze malá písmena, číslice a pomlčky. Musí začínat a končit alfanumerickým znakem.")
  private String key;

  /**
   * Display name pro tenant (zobrazovací název)
   */
  @NotBlank(message = "Display name je povinný") @Size(min = 2, max = 100, message = "Display name musí být 2-100 znaků")
  private String displayName;

  /**
   * Volitelné - automatické vytvoření (default: true)
   */
  @Builder.Default
  private boolean autoCreate = true;
}
