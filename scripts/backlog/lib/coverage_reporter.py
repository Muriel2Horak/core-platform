#!/usr/bin/env python3
"""Coverage reporting for path mapping validation.

Generates human-readable text reports and machine-readable JSON reports.
Supports story-level and epic-level aggregation.

See CORE-006 AC3-AC5 for specification.
"""

import json
from pathlib import Path
from typing import Dict, List, Optional

from path_checker import PathValidationResult, get_overall_stats


def generate_text_report(
    story_id: str,
    results: Dict[str, PathValidationResult],
    show_details: bool = True
) -> str:
    """Generate human-readable text report with emojis.
    
    Args:
        story_id: Story ID (e.g., "CORE-005")
        results: Dict mapping category → PathValidationResult
        show_details: Show missing files and glob expansions
        
    Returns:
        Formatted text report string
        
    Example:
        >>> text = generate_text_report("CORE-005", results)
        >>> print(text)
        📊 Path Mapping Coverage: CORE-005
        
        ✅ code_paths:  1/1 (100%) - scripts/backlog/git_tracker.sh
        ⚠️  test_paths:  0/1 (0%)   - MISSING: scripts/backlog/test_git_tracker.sh
        ✅ docs_paths:  3/3 (100%) - backlog/README.md, docs/development/backlog-workflow.md, CHANGELOG.md
        
        📈 Overall: 80% (4/5 paths exist)
    """
    lines = []
    
    # Header
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    lines.append(f"📊 Path Mapping Coverage: {story_id}")
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    lines.append("")
    
    # Category reports
    for category, result in results.items():
        emoji = "✅" if result.percentage == 100 else "⚠️"
        
        # Main line
        lines.append(
            f"{emoji} {category:12} {result.exist_count}/{result.total} "
            f"({result.percentage:.0f}%)"
        )
        
        # Details
        if show_details:
            # Show existing paths (first 3 if many)
            if result.existing_paths:
                preview_paths = result.existing_paths[:3]
                if result.glob_expanded:
                    # Show glob expansions
                    for pattern, matches in result.glob_expanded.items():
                        lines.append(f"   {pattern} → {len(matches)} files")
                else:
                    # Show literal paths
                    paths_str = ", ".join(preview_paths)
                    if len(result.existing_paths) > 3:
                        paths_str += f" ... (+{len(result.existing_paths) - 3} more)"
                    lines.append(f"   {paths_str}")
            
            # Show missing paths
            if result.missing_paths:
                lines.append(f"   ❌ MISSING ({len(result.missing_paths)}):")
                for path in result.missing_paths:
                    lines.append(f"      - {path}")
        
        lines.append("")
    
    # Overall stats
    overall = get_overall_stats(results)
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    lines.append(
        f"📈 Overall: {overall['percentage']:.0f}% "
        f"({overall['exist']}/{overall['total']} paths exist)"
    )
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    
    return "\n".join(lines)


def generate_json_report(
    story_id: str,
    results: Dict[str, PathValidationResult]
) -> str:
    """Generate machine-readable JSON report.
    
    Args:
        story_id: Story ID (e.g., "CORE-005")
        results: Dict mapping category → PathValidationResult
        
    Returns:
        JSON string (pretty-printed)
        
    Example:
        >>> json_str = generate_json_report("CORE-005", results)
        >>> data = json.loads(json_str)
        >>> data['overall']['percentage']
        80.0
    """
    # Build coverage dict
    coverage = {}
    for category, result in results.items():
        coverage[category] = result.to_dict()
        
        # Add glob expansions if any
        if result.glob_expanded:
            coverage[category]['glob_expanded'] = {
                pattern: len(matches)
                for pattern, matches in result.glob_expanded.items()
            }
    
    # Overall stats
    overall = get_overall_stats(results)
    
    # Full report
    report = {
        'story_id': story_id,
        'coverage': coverage,
        'overall': overall
    }
    
    return json.dumps(report, indent=2)


def generate_epic_text_report(
    epic_id: str,
    story_results: Dict[str, Dict[str, PathValidationResult]],
    show_zero: bool = False
) -> str:
    """Generate epic-level aggregated text report.
    
    Args:
        epic_id: Epic ID (e.g., "EPIC-001-backlog-system")
        story_results: Dict mapping story_id → (category → PathValidationResult)
        show_zero: Include stories with 0% coverage
        
    Returns:
        Formatted text report string
        
    Example:
        >>> story_results = {
        ...     'CORE-001': results_001,
        ...     'CORE-003': results_003,
        ...     'CORE-005': results_005
        ... }
        >>> text = generate_epic_text_report("EPIC-001", story_results)
    """
    lines = []
    
    # Header
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    lines.append(f"📊 Epic Path Mapping Coverage: {epic_id}")
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    lines.append("")
    
    # Story-level summaries
    epic_total_paths = 0
    epic_exist_paths = 0
    
    for story_id, results in sorted(story_results.items()):
        overall = get_overall_stats(results)
        
        # Skip 0% stories if show_zero=False
        if not show_zero and overall['percentage'] == 0:
            continue
        
        emoji = "✅" if overall['percentage'] == 100 else "⚠️"
        
        # Story summary line
        lines.append(
            f"{emoji} {story_id:12} "
            f"{overall['percentage']:>3.0f}% "
            f"({overall['exist']}/{overall['total']} paths)"
        )
        
        # Category breakdown (compact)
        breakdown = []
        for category, result in results.items():
            short_name = category.replace('_paths', '').replace('_', '')[:4]
            breakdown.append(f"{short_name}:{result.exist_count}/{result.total}")
        
        lines.append(f"   └─ {', '.join(breakdown)}")
        lines.append("")
        
        # Accumulate epic totals
        epic_total_paths += overall['total']
        epic_exist_paths += overall['exist']
    
    # Epic overall
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    if epic_total_paths > 0:
        epic_percentage = (epic_exist_paths / epic_total_paths) * 100
    else:
        epic_percentage = 100.0
    
    lines.append(
        f"📈 Epic Total: {epic_percentage:.0f}% "
        f"({epic_exist_paths}/{epic_total_paths} paths exist)"
    )
    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    
    return "\n".join(lines)


def generate_epic_json_report(
    epic_id: str,
    story_results: Dict[str, Dict[str, PathValidationResult]]
) -> str:
    """Generate epic-level aggregated JSON report.
    
    Args:
        epic_id: Epic ID (e.g., "EPIC-001-backlog-system")
        story_results: Dict mapping story_id → (category → PathValidationResult)
        
    Returns:
        JSON string (pretty-printed)
    """
    stories = []
    epic_total = 0
    epic_exist = 0
    
    for story_id, results in sorted(story_results.items()):
        overall = get_overall_stats(results)
        
        # Story-level data
        story_data = {
            'story_id': story_id,
            'coverage': {
                category: result.to_dict()
                for category, result in results.items()
            },
            'overall': overall
        }
        
        stories.append(story_data)
        epic_total += overall['total']
        epic_exist += overall['exist']
    
    # Epic overall
    if epic_total > 0:
        epic_percentage = round((epic_exist / epic_total) * 100, 1)
    else:
        epic_percentage = 100.0
    
    report = {
        'epic_id': epic_id,
        'stories': stories,
        'epic_overall': {
            'total': epic_total,
            'exist': epic_exist,
            'percentage': epic_percentage
        }
    }
    
    return json.dumps(report, indent=2)


if __name__ == '__main__':
    # Manual testing
    import sys
    from yaml_parser import parse_story_path_mapping, get_story_id
    from path_checker import validate_path_mapping
    
    if len(sys.argv) < 2:
        print("Usage: python coverage_reporter.py <story_readme.md>")
        sys.exit(1)
    
    story_file = Path(sys.argv[1])
    
    try:
        # Parse and validate
        story_id = get_story_id(story_file)
        path_mapping = parse_story_path_mapping(story_file)
        results = validate_path_mapping(path_mapping)
        
        # Text report
        print(generate_text_report(story_id, results))
        print()
        
        # JSON report (if --json flag)
        if len(sys.argv) > 2 and sys.argv[2] == '--json':
            print("JSON Report:")
            print(generate_json_report(story_id, results))
        
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)
