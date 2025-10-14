package com.example.platform.workflow.testing;

import com.platform.workflow.testing.WorkflowTestingService;
import com.platform.workflow.testing.WorkflowTestingService.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 🧪 W11: Workflow Testing REST API
 * 
 * Endpoints for workflow simulation and testing: - Simulate workflow execution
 * (dry-run) - Generate test scenarios - Validate transitions without side
 * effects
 * 
 * @since 2025-01-14
 */
@RestController @RequestMapping("/api/v1/workflows/testing") @RequiredArgsConstructor @Slf4j
public class WorkflowTestingController {

  private final WorkflowTestingService testingService;

  /**
   * Simulate workflow execution
   */
  @PostMapping("/simulate") @PreAuthorize("hasRole('WORKFLOW_ADMIN')") @Timed(value = "workflow.testing.simulate", description = "Time to simulate workflow")
  public ResponseEntity<SimulationResult> simulate(@RequestBody SimulationRequest request) {
    log.info("Simulating workflow: entity_type={}, scenario={}", request.entityType(),
        request.scenarioName());

    SimulationResult result = testingService.simulate(request);

    return ResponseEntity.ok(result);
  }

  /**
   * Generate test scenario
   */
  @GetMapping("/scenarios/{entityType}/{scenarioType}") @Timed(value = "workflow.testing.generate_scenario", description = "Time to generate test scenario")
  public ResponseEntity<TestScenario> generateScenario(@PathVariable String entityType,
      @PathVariable String scenarioType) {

    log.info("Generating test scenario: entity_type={}, type={}", entityType, scenarioType);

    TestScenario scenario = testingService.generateScenario(entityType, scenarioType);

    return ResponseEntity.ok(scenario);
  }

  /**
   * Run pre-defined test suite
   */
  @PostMapping("/suites/{entityType}/run") @PreAuthorize("hasRole('WORKFLOW_ADMIN')") @Timed(value = "workflow.testing.run_suite", description = "Time to run test suite")
  public ResponseEntity<TestSuiteResult> runTestSuite(@PathVariable String entityType) {
    log.info("Running test suite for: {}", entityType);

    // Generate and run all scenarios
    TestScenario happyPath = testingService.generateScenario(entityType, "happy-path");
    TestScenario edgeCases = testingService.generateScenario(entityType, "edge-cases");
    TestScenario errorCases = testingService.generateScenario(entityType, "error-cases");

    SimulationResult result1 = testingService.simulate(toRequest(happyPath));
    SimulationResult result2 = testingService.simulate(toRequest(edgeCases));
    SimulationResult result3 = testingService.simulate(toRequest(errorCases));

    int passed = (result1.success() ? 1 : 0) + (result2.success() ? 1 : 0)
        + (result3.success() ? 1 : 0);
    int total = 3;

    TestSuiteResult suiteResult = new TestSuiteResult(entityType, total, passed, total - passed,
        java.util.List.of(result1, result2, result3));

    return ResponseEntity.ok(suiteResult);
  }

  private SimulationRequest toRequest(TestScenario scenario) {
    return new SimulationRequest(scenario.entityType(), scenario.name(), scenario.initialState(),
        scenario.events(), scenario.mockData());
  }

  public record TestSuiteResult(String entityType, int totalTests, int passed, int failed,
      java.util.List<SimulationResult> results) {
  }
}
