#!/usr/bin/env python3
"""
Quality Scorer - calculates story quality score (0-100%)
Part of CORE-008: Story Schema Validator & Quality Checker
"""

from typing import Dict
from .schema_checker import SchemaChecker


class QualityScorer:
    """Calculate story quality score based on CORE-008 formula"""

    # Scoring weights (total = 100 points)
    WEIGHTS = {
        "schema": 40,  # 8 sections × 5 points
        "dor": 15,
        "dod": 15,
        "ac_testability": 15,
        "path_mapping": 10,
        "yaml": 5,
    }

    def __init__(self, checker: SchemaChecker):
        self.checker = checker

    def calculate_score(self) -> Dict[str, any]:
        """Calculate overall quality score"""

        # 1. Schema: 40 points (8 sections × 5)
        schema_results = self.checker.validate_schema()
        schema_count = self.checker.count_sections()
        schema_score = round(
            schema_count["percentage"] / 100 * self.WEIGHTS["schema"], 1
        )

        # 2. DoR: 15 points
        dor_dod = self.checker.validate_dor_dod_completeness()
        dor_score = round(dor_dod["dor"]["percentage"] / 100 * self.WEIGHTS["dor"], 1)

        # 3. DoD: 15 points
        dod_score = round(dor_dod["dod"]["percentage"] / 100 * self.WEIGHTS["dod"], 1)

        # 4. AC Testability: 15 points
        ac_format = self.checker.validate_ac_format()
        ac_score = round(
            ac_format["percentage"] / 100 * self.WEIGHTS["ac_testability"], 1
        )

        # 5. Path Mapping: 10 points (check YAML has path_mapping)
        has_path_mapping = bool("path_mapping:" in self.checker.content)
        path_score = self.WEIGHTS["path_mapping"] if has_path_mapping else 0

        # 6. YAML: 5 points (valid frontmatter)
        has_yaml = schema_results.get("YAML frontmatter", False)
        yaml_score = self.WEIGHTS["yaml"] if has_yaml else 0

        # Total
        total_score = (
            schema_score + dor_score + dod_score + ac_score + path_score + yaml_score
        )

        # Determine level
        if total_score >= 90:
            level = "EXCELLENT"
            emoji = "✅"
        elif total_score >= 70:
            level = "GOOD"
            emoji = "⚠️"
        elif total_score >= 50:
            level = "FAIR"
            emoji = "⚠️"
        else:
            level = "POOR"
            emoji = "❌"

        return {
            "breakdown": {
                "schema": {
                    "score": schema_score,
                    "max": self.WEIGHTS["schema"],
                    "percentage": schema_count["percentage"],
                },
                "dor": {
                    "score": dor_score,
                    "max": self.WEIGHTS["dor"],
                    "percentage": dor_dod["dor"]["percentage"],
                },
                "dod": {
                    "score": dod_score,
                    "max": self.WEIGHTS["dod"],
                    "percentage": dor_dod["dod"]["percentage"],
                },
                "ac_testability": {
                    "score": ac_score,
                    "max": self.WEIGHTS["ac_testability"],
                    "percentage": ac_format["percentage"],
                },
                "path_mapping": {
                    "score": path_score,
                    "max": self.WEIGHTS["path_mapping"],
                },
                "yaml": {"score": yaml_score, "max": self.WEIGHTS["yaml"]},
            },
            "total": round(total_score, 1),
            "level": level,
            "emoji": emoji,
            "max": 100,
        }

    def meets_threshold(self, min_score: float) -> bool:
        """Check if story meets minimum quality threshold"""
        score = self.calculate_score()
        return score["total"] >= min_score

    @staticmethod
    def get_level(score: float) -> str:
        """Get quality level for a given score"""
        if score >= 90:
            return "EXCELLENT"
        elif score >= 70:
            return "GOOD"
        elif score >= 50:
            return "FAIR"
        else:
            return "POOR"
