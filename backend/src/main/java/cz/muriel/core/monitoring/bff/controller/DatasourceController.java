package cz.muriel.core.monitoring.bff.controller;

import cz.muriel.core.monitoring.bff.service.DatasourceDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController @RequestMapping("/api/monitoring/datasource") @RequiredArgsConstructor @Slf4j
public class DatasourceController {

  private final DatasourceDiscoveryService datasourceDiscoveryService;

  @GetMapping("/prometheus")
  public ResponseEntity<Map<String, String>> getPrometheusDataSource() {
    log.info("Getting Prometheus datasource info for current tenant");

    String uid = datasourceDiscoveryService.getPrometheusDataSourceUid();

    if (uid == null) {
      log.warn("No Prometheus datasource found for tenant");
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(Map.of("uid", uid, "type", "prometheus"));
  }
}
