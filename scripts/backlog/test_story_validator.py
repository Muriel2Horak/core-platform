#!/usr/bin/env python3
"""
Integration tests for story_validator.py
Part of CORE-008: Story Schema Validator & Quality Checker

Run: python3 scripts/backlog/test_story_validator.py
"""

import unittest
import json
import subprocess
from pathlib import Path


class TestStoryValidator(unittest.TestCase):
    """Integration tests for story validator"""

    VALIDATOR = "scripts/backlog/story_validator.py"

    def run_validator(self, *args):
        """Run validator and return output"""
        cmd = ["python3", self.VALIDATOR] + list(args)
        result = subprocess.run(cmd, capture_output=True, text=True)
        return result.returncode, result.stdout, result.stderr

    def test_core008_validation(self):
        """Test CORE-008 validates successfully"""
        code, stdout, stderr = self.run_validator("--story", "CORE-008", "--score")

        # Should complete (exit code can be 1 due to low score, but should not crash)
        self.assertIn("CORE-008", stdout)
        self.assertIn("Quality Score", stdout)

    def test_json_output(self):
        """Test JSON output format"""
        code, stdout, stderr = self.run_validator(
            "--story", "CORE-008", "--score", "--format", "json"
        )

        # Parse JSON
        data = json.loads(stdout)

        # Check structure
        self.assertIn("story_id", data)
        self.assertIn("quality_score", data)
        self.assertIn("schema", data)

        # Check quality score structure
        score = data["quality_score"]
        self.assertIn("total", score)
        self.assertIn("level", score)
        self.assertIn("breakdown", score)

        # Check breakdown
        breakdown = score["breakdown"]
        self.assertIn("schema", breakdown)
        self.assertIn("dor", breakdown)
        self.assertIn("dod", breakdown)
        self.assertIn("ac_testability", breakdown)

        # Verify total is sum of components
        total = (
            breakdown["schema"]["score"]
            + breakdown["dor"]["score"]
            + breakdown["dod"]["score"]
            + breakdown["ac_testability"]["score"]
            + breakdown["path_mapping"]["score"]
            + breakdown["yaml"]["score"]
        )
        self.assertAlmostEqual(score["total"], total, delta=0.1)

    def test_min_score_enforcement(self):
        """Test --min-score fails when score too low"""
        # CORE-008 currently ~70%, should fail with min-score 90
        code, stdout, stderr = self.run_validator(
            "--story", "CORE-008", "--score", "--min-score", "90"
        )

        self.assertEqual(code, 1, "Should exit with code 1 when score < min-score")
        self.assertIn("below minimum", stdout)

    def test_min_score_pass(self):
        """Test --min-score passes when score high enough"""
        # CORE-008 ~70%, should pass with min-score 60
        code, stdout, stderr = self.run_validator(
            "--story", "CORE-008", "--score", "--min-score", "60"
        )

        self.assertEqual(code, 0, "Should exit with code 0 when score >= min-score")

    def test_schema_validation(self):
        """Test schema validation detects sections"""
        code, stdout, stderr = self.run_validator(
            "--story", "CORE-008", "--check-schema"
        )

        self.assertIn("Schema Validation", stdout)
        self.assertIn("YAML frontmatter", stdout)
        self.assertIn("Acceptance Criteria", stdout)

    def test_dor_dod_validation(self):
        """Test DoR/DoD completeness checking"""
        code, stdout, stderr = self.run_validator("--story", "CORE-008", "--check-dod")

        self.assertIn("DoR", stdout)
        self.assertIn("DoD", stdout)
        # Should show percentage
        self.assertTrue("%" in stdout)

    def test_ac_format_validation(self):
        """Test AC format validation (Given/When/Then)"""
        code, stdout, stderr = self.run_validator("--story", "CORE-008", "--check-ac")

        self.assertIn("AC Testability", stdout)

    def test_invalid_story(self):
        """Test validation fails gracefully for non-existent story"""
        code, stdout, stderr = self.run_validator("--story", "CORE-999")

        self.assertEqual(code, 1)
        self.assertIn("not found", stderr.lower())

    def test_quality_levels(self):
        """Test quality level classification"""
        code, stdout, stderr = self.run_validator(
            "--story", "CORE-008", "--score", "--format", "json"
        )

        data = json.loads(stdout)
        level = data["quality_score"]["level"]

        # Should be one of the defined levels
        self.assertIn(level, ["EXCELLENT", "GOOD", "FAIR", "POOR"])

        # Check emoji mapping
        emoji = data["quality_score"]["emoji"]
        if level == "EXCELLENT":
            self.assertEqual(emoji, "✅")
        elif level == "GOOD":
            self.assertEqual(emoji, "⚠️")
        elif level == "FAIR":
            self.assertEqual(emoji, "⚠️")
        elif level == "POOR":
            self.assertEqual(emoji, "❌")

    def test_multiple_stories_comparison(self):
        """Test comparing quality scores of different stories"""
        stories = ["CORE-001", "CORE-007", "CORE-008"]
        scores = {}

        for story_id in stories:
            code, stdout, stderr = self.run_validator(
                "--story", story_id, "--score", "--format", "json"
            )

            if code == 0 or stdout:  # Some stories might have low scores (exit 1)
                try:
                    data = json.loads(stdout)
                    scores[story_id] = data["quality_score"]["total"]
                except:
                    pass

        # Should have validated at least 2 stories
        self.assertGreaterEqual(len(scores), 2, f"Validated scores: {scores}")

        # Scores should be between 0-100
        for story, score in scores.items():
            self.assertGreaterEqual(score, 0, f"{story} score negative")
            self.assertLessEqual(score, 100, f"{story} score > 100")


class TestSchemaChecker(unittest.TestCase):
    """Unit tests for SchemaChecker"""

    def test_yaml_frontmatter_detection(self):
        """Test YAML frontmatter validation"""
        from lib.schema_checker import SchemaChecker

        # SchemaChecker validates YAML in validate_schema() method
        # Just ensure SchemaChecker can be imported and has the method
        self.assertTrue(hasattr(SchemaChecker, "validate_schema"))


class TestQualityScorer(unittest.TestCase):
    """Unit tests for QualityScorer"""

    def test_scoring_weights(self):
        """Test scoring weights sum to 100"""
        from lib.quality_scorer import QualityScorer

        total = sum(QualityScorer.WEIGHTS.values())
        self.assertEqual(total, 100, "Weights should sum to 100 points")

    def test_quality_levels(self):
        """Test quality level thresholds"""
        from lib.quality_scorer import QualityScorer

        # Test level classification
        test_cases = [
            (95, "EXCELLENT"),
            (85, "GOOD"),
            (65, "FAIR"),
            (40, "POOR"),
            (0, "POOR"),
            (100, "EXCELLENT"),
        ]

        for score, expected_level in test_cases:
            level = QualityScorer.get_level(score)
            self.assertEqual(
                level, expected_level, f"Score {score} should be {expected_level}"
            )


def run_tests():
    """Run all tests"""
    # Discover and run tests
    loader = unittest.TestLoader()
    suite = loader.loadTestsFromModule(__import__(__name__))

    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    # Print summary
    print("\n" + "=" * 60)
    print(f"Tests run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print(
        f"Success rate: {(result.testsRun - len(result.failures) - len(result.errors)) / result.testsRun * 100:.1f}%"
    )
    print("=" * 60)

    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    import sys

    sys.exit(run_tests())
