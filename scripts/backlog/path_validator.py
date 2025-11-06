#!/usr/bin/env python3
"""Path mapping validator CLI tool.

Validates path_mapping in story README.md files and generates coverage reports.
Supports story-level and epic-level validation.

Usage:
    path_validator.py --story CORE-005
    path_validator.py --epic EPIC-001-backlog-system
    path_validator.py --story CORE-005 --format json
    path_validator.py --epic EPIC-001 --show-zero

See CORE-006 story for full specification.
"""

import argparse
import sys
from pathlib import Path
from typing import Dict, List, Optional

# Add lib to path
sys.path.insert(0, str(Path(__file__).parent / 'lib'))

from yaml_parser import parse_story_path_mapping, get_story_id, YAMLParseError
from path_checker import validate_path_mapping, PathValidationResult
from coverage_reporter import (
    generate_text_report,
    generate_json_report,
    generate_epic_text_report,
    generate_epic_json_report
)


def find_story_file(story_id: str, repo_root: Path = None) -> Optional[Path]:
    """Find story README.md by story ID.
    
    Args:
        story_id: Story ID (e.g., "CORE-005")
        repo_root: Repository root (default: cwd)
        
    Returns:
        Path to story README.md, or None if not found
    """
    if repo_root is None:
        repo_root = Path.cwd()
    
    # Search in backlog/*/stories/*/ directories
    backlog_dir = repo_root / 'backlog'
    if not backlog_dir.exists():
        return None
    
    # Find story directory
    for epic_dir in backlog_dir.glob('EPIC-*'):
        stories_dir = epic_dir / 'stories'
        if not stories_dir.exists():
            continue
        
        for story_dir in stories_dir.glob(f'{story_id}-*'):
            readme = story_dir / 'README.md'
            if readme.exists():
                return readme
    
    return None


def find_epic_stories(epic_id: str, repo_root: Path = None) -> List[Path]:
    """Find all story README.md files in an epic.
    
    Args:
        epic_id: Epic ID (e.g., "EPIC-001" or "EPIC-001-backlog-system")
        repo_root: Repository root (default: cwd)
        
    Returns:
        List of paths to story README.md files
    """
    if repo_root is None:
        repo_root = Path.cwd()
    
    # Normalize epic ID (allow short form like "EPIC-001")
    if not epic_id.startswith('EPIC-'):
        epic_id = f'EPIC-{epic_id}'
    
    # Find epic directory
    backlog_dir = repo_root / 'backlog'
    if not backlog_dir.exists():
        return []
    
    epic_dirs = list(backlog_dir.glob(f'{epic_id}*'))
    if not epic_dirs:
        return []
    
    epic_dir = epic_dirs[0]
    stories_dir = epic_dir / 'stories'
    
    if not stories_dir.exists():
        return []
    
    # Find all story READMEs
    story_files = []
    for story_dir in stories_dir.glob('CORE-*'):
        readme = story_dir / 'README.md'
        if readme.exists():
            story_files.append(readme)
    
    return sorted(story_files)


def validate_story(
    story_id: str,
    output_format: str = 'text',
    repo_root: Path = None
) -> int:
    """Validate a single story.
    
    Args:
        story_id: Story ID (e.g., "CORE-005")
        output_format: 'text' or 'json'
        repo_root: Repository root (default: cwd)
        
    Returns:
        Exit code (0=success, 1=error)
    """
    if repo_root is None:
        repo_root = Path.cwd()
    
    # Find story file
    story_file = find_story_file(story_id, repo_root)
    if story_file is None:
        print(f"❌ Error: Story '{story_id}' not found in backlog/", file=sys.stderr)
        return 1
    
    try:
        # Parse path mapping
        path_mapping = parse_story_path_mapping(story_file)
        
        # Validate paths
        results = validate_path_mapping(path_mapping, repo_root)
        
        # Generate report
        if output_format == 'json':
            print(generate_json_report(story_id, results))
        else:
            print(generate_text_report(story_id, results))
        
        return 0
        
    except (FileNotFoundError, YAMLParseError) as e:
        print(f"❌ Error: {e}", file=sys.stderr)
        return 1


def validate_epic(
    epic_id: str,
    output_format: str = 'text',
    show_zero: bool = False,
    repo_root: Path = None
) -> int:
    """Validate all stories in an epic.
    
    Args:
        epic_id: Epic ID (e.g., "EPIC-001-backlog-system")
        output_format: 'text' or 'json'
        show_zero: Include stories with 0% coverage
        repo_root: Repository root (default: cwd)
        
    Returns:
        Exit code (0=success, 1=error)
    """
    if repo_root is None:
        repo_root = Path.cwd()
    
    # Find all stories in epic
    story_files = find_epic_stories(epic_id, repo_root)
    
    if not story_files:
        print(f"❌ Error: Epic '{epic_id}' not found or has no stories", file=sys.stderr)
        return 1
    
    # Validate each story
    story_results: Dict[str, Dict[str, PathValidationResult]] = {}
    
    for story_file in story_files:
        try:
            story_id = get_story_id(story_file)
            if story_id is None:
                continue  # Skip stories without ID
            
            path_mapping = parse_story_path_mapping(story_file)
            results = validate_path_mapping(path_mapping, repo_root)
            
            story_results[story_id] = results
            
        except (FileNotFoundError, YAMLParseError) as e:
            print(f"⚠️  Warning: Skipping {story_file.parent.name}: {e}", file=sys.stderr)
            continue
    
    if not story_results:
        print(f"❌ Error: No valid stories found in epic '{epic_id}'", file=sys.stderr)
        return 1
    
    # Generate report
    if output_format == 'json':
        print(generate_epic_json_report(epic_id, story_results))
    else:
        print(generate_epic_text_report(epic_id, story_results, show_zero))
    
    return 0


def main():
    """Main CLI entry point."""
    parser = argparse.ArgumentParser(
        description='Validate path mappings in story README.md files',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Validate single story (text format)
  %(prog)s --story CORE-005
  
  # Validate story (JSON format)
  %(prog)s --story CORE-005 --format json
  
  # Validate all stories in epic
  %(prog)s --epic EPIC-001-backlog-system
  
  # Show stories with 0%% coverage
  %(prog)s --epic EPIC-001 --show-zero
  
  # Epic report in JSON
  %(prog)s --epic EPIC-001 --format json | jq .

See CORE-006 story for full documentation.
        """
    )
    
    # Mutually exclusive: --story or --epic
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument(
        '--story',
        metavar='STORY_ID',
        help='Validate single story (e.g., CORE-005)'
    )
    group.add_argument(
        '--epic',
        metavar='EPIC_ID',
        help='Validate all stories in epic (e.g., EPIC-001-backlog-system)'
    )
    
    # Optional flags
    parser.add_argument(
        '--format',
        choices=['text', 'json'],
        default='text',
        help='Output format (default: text)'
    )
    parser.add_argument(
        '--show-zero',
        action='store_true',
        help='Include stories with 0%% coverage (epic mode only)'
    )
    
    args = parser.parse_args()
    
    # Validate
    if args.story:
        exit_code = validate_story(args.story, args.format)
    else:
        exit_code = validate_epic(args.epic, args.format, args.show_zero)
    
    sys.exit(exit_code)


if __name__ == '__main__':
    main()
