#!/usr/bin/env python3
"""
Story Validator - Main CLI for story quality validation
Part of CORE-008: Story Schema Validator & Quality Checker

Usage:
    python3 scripts/backlog/story_validator.py --story CORE-008
    python3 scripts/backlog/story_validator.py --epic EPIC-001 --score
    python3 scripts/backlog/story_validator.py --story CORE-008 --format json
"""

import sys
import argparse
import json
from pathlib import Path
from typing import Optional

# Add lib to path
sys.path.insert(0, str(Path(__file__).parent))

from lib.schema_checker import SchemaChecker
from lib.quality_scorer import QualityScorer


class Colors:
    """ANSI color codes"""

    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    BLUE = "\033[94m"
    BOLD = "\033[1m"
    END = "\033[0m"


def find_story_path(story_id: str) -> Optional[Path]:
    """Find story README.md by ID"""
    backlog_dir = Path(__file__).parent.parent.parent / "backlog"

    # Search for story directory
    for epic_dir in backlog_dir.iterdir():
        if not epic_dir.is_dir() or not epic_dir.name.startswith("EPIC-"):
            continue

        stories_dir = epic_dir / "stories"
        if not stories_dir.exists():
            continue

        for story_dir in stories_dir.iterdir():
            if story_dir.is_dir() and story_dir.name.startswith(story_id):
                readme = story_dir / "README.md"
                if readme.exists():
                    return readme

    return None


def validate_story_text(
    story_path: Path,
    show_score: bool = False,
    check_schema: bool = False,
    check_dod: bool = False,
    check_ac: bool = False,
    check_yaml: bool = False,
) -> int:
    """Validate story and print text output"""

    checker = SchemaChecker(story_path)
    scorer = QualityScorer(checker)

    story_id = (
        story_path.parent.name.split("-")[0]
        + "-"
        + story_path.parent.name.split("-")[1]
    )

    print(f"\n{Colors.BOLD}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}Story Validation: {story_id}{Colors.END}")
    print(f"{Colors.BOLD}{'='*60}{Colors.END}\n")

    exit_code = 0

    # Schema check
    if check_schema or not any([check_dod, check_ac, check_yaml, show_score]):
        print(f"{Colors.BOLD}📋 Schema Validation{Colors.END}")
        schema_results = checker.validate_schema()
        schema_count = checker.count_sections()

        for section, present in schema_results.items():
            status = f"{Colors.GREEN}✅" if present else f"{Colors.RED}❌"
            print(f"  {status} {section}{Colors.END}")

        print(
            f"\n  Present: {schema_count['present']}/{schema_count['required']} ({schema_count['percentage']}%)"
        )

        missing = checker.get_missing_sections()
        if missing:
            print(f"\n  {Colors.YELLOW}⚠️  Missing sections:{Colors.END}")
            for section in missing:
                print(f"     - {section}")
            exit_code = 1
        print()

    # DoR/DoD check
    if check_dod or not any([check_schema, check_ac, check_yaml, show_score]):
        print(f"{Colors.BOLD}✅ DoR/DoD Completeness{Colors.END}")
        dor_dod = checker.validate_dor_dod_completeness()

        dor = dor_dod["dor"]
        print(f"  DoR: {dor['checked']}/{dor['total']} ({dor['percentage']}%)")
        if dor["percentage"] < 75:
            print(f"  {Colors.YELLOW}⚠️  Below 75% threshold{Colors.END}")
            exit_code = 1
        if dor["unchecked_items"]:
            print(f"  {Colors.YELLOW}Unchecked items:{Colors.END}")
            for item in dor["unchecked_items"][:3]:
                print(f"     - {item}")

        dod = dor_dod["dod"]
        print(f"\n  DoD: {dod['checked']}/{dod['total']} ({dod['percentage']}%)")
        if dod["percentage"] < 80:
            print(f"  {Colors.YELLOW}⚠️  Below 80% threshold{Colors.END}")
            exit_code = 1
        if dod["unchecked_items"]:
            print(f"  {Colors.YELLOW}Unchecked items:{Colors.END}")
            for item in dod["unchecked_items"][:3]:
                print(f"     - {item}")
        print()

    # AC format check
    if check_ac or not any([check_schema, check_dod, check_yaml, show_score]):
        print(f"{Colors.BOLD}🧪 AC Testability{Colors.END}")
        ac_format = checker.validate_ac_format()

        print(
            f"  Testable AC: {ac_format['testable_count']}/{ac_format['ac_count']} ({ac_format['percentage']}%)"
        )

        if ac_format["issues"]:
            print(f"  {Colors.YELLOW}⚠️  Issues:{Colors.END}")
            for issue in ac_format["issues"]:
                print(f"     - {issue}")
            exit_code = 1
        print()

    # Quality score
    if show_score:
        print(f"{Colors.BOLD}📊 Quality Score{Colors.END}")
        score_data = scorer.calculate_score()

        breakdown = score_data["breakdown"]
        print(
            f"  Schema:        {breakdown['schema']['score']:.1f}/{breakdown['schema']['max']} ({breakdown['schema']['percentage']}%)"
        )
        print(
            f"  DoR:           {breakdown['dor']['score']:.1f}/{breakdown['dor']['max']} ({breakdown['dor']['percentage']}%)"
        )
        print(
            f"  DoD:           {breakdown['dod']['score']:.1f}/{breakdown['dod']['max']} ({breakdown['dod']['percentage']}%)"
        )
        print(
            f"  AC Testability: {breakdown['ac_testability']['score']:.1f}/{breakdown['ac_testability']['max']} ({breakdown['ac_testability']['percentage']}%)"
        )
        print(
            f"  Path Mapping:  {breakdown['path_mapping']['score']:.1f}/{breakdown['path_mapping']['max']}"
        )
        print(
            f"  YAML:          {breakdown['yaml']['score']:.1f}/{breakdown['yaml']['max']}"
        )
        print(f"  {Colors.BOLD}{'─'*50}{Colors.END}")
        print(
            f"  {Colors.BOLD}TOTAL: {score_data['total']}/{score_data['max']} {score_data['emoji']} {score_data['level']}{Colors.END}"
        )
        print()

        if score_data["total"] < 70:
            exit_code = 1

    return exit_code


def validate_story_json(story_path: Path, show_score: bool = False) -> dict:
    """Validate story and return JSON output"""

    checker = SchemaChecker(story_path)
    scorer = QualityScorer(checker)

    story_id = (
        story_path.parent.name.split("-")[0]
        + "-"
        + story_path.parent.name.split("-")[1]
    )

    result = {
        "story_id": story_id,
        "story_path": str(story_path),
        "schema": {
            "sections": checker.validate_schema(),
            "count": checker.count_sections(),
            "missing": checker.get_missing_sections(),
        },
        "dor_dod": checker.validate_dor_dod_completeness(),
        "ac_format": checker.validate_ac_format(),
    }

    if show_score:
        result["quality_score"] = scorer.calculate_score()

    return result


def main():
    parser = argparse.ArgumentParser(
        description="Story Schema Validator & Quality Checker (CORE-008)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Validate single story
  python3 story_validator.py --story CORE-008
  
  # Validate with quality score
  python3 story_validator.py --story CORE-008 --score
  
  # JSON output for CI/CD
  python3 story_validator.py --story CORE-008 --format json
  
  # Check only specific aspects
  python3 story_validator.py --story CORE-008 --check-schema
  python3 story_validator.py --story CORE-008 --check-dod
  
  # Enforce minimum score
  python3 story_validator.py --story CORE-008 --score --min-score 80
        """,
    )

    parser.add_argument("--story", help="Story ID (e.g., CORE-008)")
    parser.add_argument("--epic", help="Epic ID (validate all stories in epic)")
    parser.add_argument("--check-schema", action="store_true", help="Check schema only")
    parser.add_argument("--check-dod", action="store_true", help="Check DoR/DoD only")
    parser.add_argument("--check-ac", action="store_true", help="Check AC format only")
    parser.add_argument("--check-yaml", action="store_true", help="Check YAML only")
    parser.add_argument("--score", action="store_true", help="Calculate quality score")
    parser.add_argument(
        "--format", choices=["text", "json"], default="text", help="Output format"
    )
    parser.add_argument("--min-score", type=float, help="Minimum quality score (0-100)")
    parser.add_argument(
        "--strict", action="store_true", help="Exit with error if score < min-score"
    )

    args = parser.parse_args()

    if not args.story and not args.epic:
        parser.print_help()
        return 1

    if args.story:
        story_path = find_story_path(args.story)
        if not story_path:
            print(
                f"{Colors.RED}❌ Story {args.story} not found{Colors.END}",
                file=sys.stderr,
            )
            return 1

        if args.format == "json":
            result = validate_story_json(story_path, args.score)
            print(json.dumps(result, indent=2))

            # Check min score in JSON mode
            if args.min_score and args.score:
                if result["quality_score"]["total"] < args.min_score:
                    return 1

            return 0
        else:
            exit_code = validate_story_text(
                story_path,
                args.score,
                args.check_schema,
                args.check_dod,
                args.check_ac,
                args.check_yaml,
            )

            # Check min score
            if args.min_score and args.score:
                checker = SchemaChecker(story_path)
                scorer = QualityScorer(checker)
                score = scorer.calculate_score()["total"]

                if score < args.min_score:
                    print(
                        f"{Colors.RED}❌ Score {score} below minimum {args.min_score}{Colors.END}"
                    )
                    return 1

            return exit_code

    return 0


if __name__ == "__main__":
    sys.exit(main())
