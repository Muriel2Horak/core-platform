#!/usr/bin/env python3
"""
Schema Checker - validates story structure
Part of CORE-008: Story Schema Validator & Quality Checker
"""

import re
from typing import Dict, List, Optional
from pathlib import Path


class SchemaChecker:
    """Validates story schema and structure"""

    REQUIRED_SECTIONS = [
        "YAML frontmatter",
        "Role / Potřeba / Benefit",
        "Definition of Ready",
        "Acceptance Criteria",
        "AC to Test Mapping",
        "Implementation Mapping",
        "Definition of Done",
        "Subtasks",
    ]

    def __init__(self, story_path: Path):
        self.story_path = story_path
        self.content = story_path.read_text()
        self.results = {}

    def validate_schema(self) -> Dict[str, bool]:
        """Check all required sections present"""
        results = {}

        # 1. YAML frontmatter
        results["YAML frontmatter"] = bool(
            re.search(r"^---\n.*?^---", self.content, re.MULTILINE | re.DOTALL)
        )

        # 2. Role/Need/Benefit
        results["Role / Potřeba / Benefit"] = bool(
            re.search(r"##.*Role.*Potřeba.*Benefit", self.content, re.IGNORECASE)
        )

        # 3. Definition of Ready
        results["Definition of Ready"] = bool(
            re.search(r"##.*Definition of Ready", self.content, re.IGNORECASE)
        )

        # 4. Acceptance Criteria
        results["Acceptance Criteria"] = bool(
            re.search(r"##.*Acceptance Criteria", self.content, re.IGNORECASE)
        )

        # 5. AC to Test Mapping (new in CORE-007)
        results["AC to Test Mapping"] = bool(
            re.search(r"##.*AC.*Test.*Mapping", self.content, re.IGNORECASE)
        )

        # 6. Implementation Mapping
        results["Implementation Mapping"] = bool(
            re.search(r"##.*Implementation Mapping", self.content, re.IGNORECASE)
        )

        # 7. Definition of Done
        results["Definition of Done"] = bool(
            re.search(r"##.*Definition of Done", self.content, re.IGNORECASE)
        )

        # 8. Subtasks
        results["Subtasks"] = bool(
            re.search(r"##.*Subtasks", self.content, re.IGNORECASE)
        )

        self.results = results
        return results

    def count_sections(self) -> Dict[str, int]:
        """Count present vs required sections"""
        if not self.results:
            self.validate_schema()

        present = sum(1 for v in self.results.values() if v)
        required = len(self.REQUIRED_SECTIONS)

        return {
            "present": present,
            "required": required,
            "percentage": round(present / required * 100, 1),
        }

    def get_missing_sections(self) -> List[str]:
        """Return list of missing sections"""
        if not self.results:
            self.validate_schema()

        return [section for section, present in self.results.items() if not present]

    def validate_dor_dod_completeness(self) -> Dict[str, any]:
        """Check DoR/DoD checklist completion"""
        dor_match = re.search(
            r"## .*Definition of Ready.*?\n(.*?)(?=\n##)",
            self.content,
            re.DOTALL | re.IGNORECASE,
        )
        dod_match = re.search(
            r"## .*Definition of Done.*?\n(.*?)(?=\n##|\Z)",
            self.content,
            re.DOTALL | re.IGNORECASE,
        )

        results = {}

        # DoR
        if dor_match:
            dor_text = dor_match.group(1)
            dor_checked = len(re.findall(r"- \[x\]", dor_text, re.IGNORECASE))
            dor_unchecked = len(re.findall(r"- \[ \]", dor_text))
            dor_total = dor_checked + dor_unchecked

            results["dor"] = {
                "checked": dor_checked,
                "total": dor_total,
                "percentage": (
                    round(dor_checked / dor_total * 100, 1) if dor_total > 0 else 0
                ),
                "unchecked_items": re.findall(r"- \[ \] (.+)", dor_text),
            }
        else:
            results["dor"] = {
                "checked": 0,
                "total": 0,
                "percentage": 0,
                "unchecked_items": [],
            }

        # DoD
        if dod_match:
            dod_text = dod_match.group(1)
            dod_checked = len(re.findall(r"- \[x\]", dod_text, re.IGNORECASE))
            dod_unchecked = len(re.findall(r"- \[ \]", dod_text))
            dod_total = dod_checked + dod_unchecked

            results["dod"] = {
                "checked": dod_checked,
                "total": dod_total,
                "percentage": (
                    round(dod_checked / dod_total * 100, 1) if dod_total > 0 else 0
                ),
                "unchecked_items": re.findall(r"- \[ \] (.+)", dod_text),
            }
        else:
            results["dod"] = {
                "checked": 0,
                "total": 0,
                "percentage": 0,
                "unchecked_items": [],
            }

        return results

    def validate_ac_format(self) -> Dict[str, any]:
        """Check AC use Given/When/Then format"""
        ac_section = re.search(
            r"## .*Acceptance Criteria.*?\n(.*?)(?=\n## |\Z)",
            self.content,
            re.DOTALL | re.IGNORECASE,
        )

        if not ac_section:
            return {
                "ac_count": 0,
                "testable_count": 0,
                "percentage": 0,
                "issues": ["No AC section found"],
            }

        ac_text = ac_section.group(1)

        # Find all AC subsections (### AC1:, ### AC2:, etc.)
        ac_blocks = re.findall(
            r"### (AC\d+):.*?\n(.*?)(?=\n### |\Z)", ac_text, re.DOTALL | re.IGNORECASE
        )

        ac_count = len(ac_blocks)
        testable_count = 0
        issues = []

        for ac_id, ac_content in ac_blocks:
            # Match **Given** or Given: or Given -
            has_given = bool(
                re.search(r"(\*\*Given\*\*|Given:|Given -)", ac_content, re.IGNORECASE)
            )
            has_when = bool(
                re.search(r"(\*\*When\*\*|When:|When -)", ac_content, re.IGNORECASE)
            )
            has_then = bool(
                re.search(r"(\*\*Then\*\*|Then:|Then -)", ac_content, re.IGNORECASE)
            )

            if has_given and has_when and has_then:
                testable_count += 1
            else:
                missing = []
                if not has_given:
                    missing.append("Given")
                if not has_when:
                    missing.append("When")
                if not has_then:
                    missing.append("Then")
                issues.append(f"{ac_id}: Missing {', '.join(missing)}")

        return {
            "ac_count": ac_count,
            "testable_count": testable_count,
            "percentage": (
                round(testable_count / ac_count * 100, 1) if ac_count > 0 else 0
            ),
            "issues": issues,
        }
