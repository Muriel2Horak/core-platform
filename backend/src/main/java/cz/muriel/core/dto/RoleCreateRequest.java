package cz.muriel.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreateRequest {
  @NotBlank(message = "Název role je povinný")
  @Size(min = 2, max = 50, message = "Název role musí být 2-50 znaků")
  private String name;

  @Size(max = 200, message = "Popis nesmí být delší než 200 znaků")
  private String description;
}