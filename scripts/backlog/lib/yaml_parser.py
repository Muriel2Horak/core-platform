#!/usr/bin/env python3
"""YAML frontmatter parser for story README.md files.

Extracts path_mapping section from story YAML frontmatter.
Supports code_paths, test_paths, docs_paths arrays.

See CORE-006 story for specification.
"""

import re
from pathlib import Path
from typing import Dict, List, Optional

try:
    import yaml
except ImportError:
    # Fallback if PyYAML not installed (use stdlib json for basic parsing)
    yaml = None


class YAMLParseError(Exception):
    """Raised when YAML frontmatter parsing fails."""
    pass


def extract_frontmatter(content: str) -> Optional[str]:
    """Extract YAML frontmatter from Markdown content.
    
    Args:
        content: Full Markdown file content
        
    Returns:
        YAML frontmatter string (between --- markers), or None if not found
        
    Example:
        >>> content = "---\\nid: CORE-001\\n---\\n# Story"
        >>> extract_frontmatter(content)
        'id: CORE-001\\n'
    """
    # Match YAML frontmatter: ^---\n...\n---\n
    pattern = r'^---\s*\n(.*?)\n---\s*\n'
    match = re.search(pattern, content, re.DOTALL | re.MULTILINE)
    
    if not match:
        return None
    
    return match.group(1)


def parse_yaml_frontmatter(frontmatter: str) -> Dict:
    """Parse YAML frontmatter string to dict.
    
    Args:
        frontmatter: YAML string
        
    Returns:
        Parsed dict
        
    Raises:
        YAMLParseError: If YAML is invalid
    """
    if yaml is None:
        raise YAMLParseError(
            "PyYAML not installed. Install with: pip install pyyaml"
        )
    
    try:
        data = yaml.safe_load(frontmatter)
        if not isinstance(data, dict):
            raise YAMLParseError(f"Expected dict, got {type(data)}")
        return data
    except yaml.YAMLError as e:
        raise YAMLParseError(f"Invalid YAML: {e}") from e


def extract_path_mapping(yaml_data: Dict) -> Dict[str, List[str]]:
    """Extract path_mapping section from YAML data.
    
    Args:
        yaml_data: Parsed YAML frontmatter dict
        
    Returns:
        Dict with keys: code_paths, test_paths, docs_paths (empty lists if missing)
        
    Example:
        >>> yaml_data = {
        ...     'id': 'CORE-001',
        ...     'path_mapping': {
        ...         'code_paths': ['backend/Main.java'],
        ...         'test_paths': [],
        ...         'docs_paths': ['README.md']
        ...     }
        ... }
        >>> extract_path_mapping(yaml_data)
        {'code_paths': ['backend/Main.java'], 'test_paths': [], 'docs_paths': ['README.md']}
    """
    path_mapping = yaml_data.get('path_mapping', {})
    
    if not isinstance(path_mapping, dict):
        return {
            'code_paths': [],
            'test_paths': [],
            'docs_paths': []
        }
    
    return {
        'code_paths': path_mapping.get('code_paths', []) or [],
        'test_paths': path_mapping.get('test_paths', []) or [],
        'docs_paths': path_mapping.get('docs_paths', []) or []
    }


def parse_story_path_mapping(story_file: Path) -> Dict[str, List[str]]:
    """Parse path_mapping from story README.md file.
    
    Args:
        story_file: Path to story README.md (absolute or relative)
        
    Returns:
        Dict with code_paths, test_paths, docs_paths lists
        
    Raises:
        FileNotFoundError: If story file doesn't exist
        YAMLParseError: If YAML parsing fails
        
    Example:
        >>> path_mapping = parse_story_path_mapping(
        ...     Path("backlog/EPIC-001-backlog-system/stories/CORE-001-markdown-structure-templates/README.md")
        ... )
        >>> path_mapping['code_paths']
        ['backlog/templates/story.md', 'backlog/templates/subtask.md', 'backlog/templates/epic.md']
    """
    story_path = Path(story_file)
    
    if not story_path.exists():
        raise FileNotFoundError(f"Story file not found: {story_path}")
    
    content = story_path.read_text(encoding='utf-8')
    
    # Extract frontmatter
    frontmatter = extract_frontmatter(content)
    if frontmatter is None:
        # No frontmatter → return empty path mapping (not an error)
        return {
            'code_paths': [],
            'test_paths': [],
            'docs_paths': []
        }
    
    # Parse YAML
    yaml_data = parse_yaml_frontmatter(frontmatter)
    
    # Extract path_mapping
    return extract_path_mapping(yaml_data)


def get_story_id(story_file: Path) -> Optional[str]:
    """Extract story ID from YAML frontmatter.
    
    Args:
        story_file: Path to story README.md
        
    Returns:
        Story ID (e.g., "CORE-001"), or None if not found
    """
    try:
        story_path = Path(story_file)
        content = story_path.read_text(encoding='utf-8')
        frontmatter = extract_frontmatter(content)
        
        if frontmatter is None:
            return None
        
        yaml_data = parse_yaml_frontmatter(frontmatter)
        return yaml_data.get('id')
    except (FileNotFoundError, YAMLParseError):
        return None


if __name__ == '__main__':
    # Manual testing
    import sys
    
    if len(sys.argv) < 2:
        print("Usage: python yaml_parser.py <story_readme.md>")
        sys.exit(1)
    
    story_file = Path(sys.argv[1])
    
    try:
        path_mapping = parse_story_path_mapping(story_file)
        story_id = get_story_id(story_file)
        
        print(f"Story ID: {story_id}")
        print(f"Code paths: {len(path_mapping['code_paths'])}")
        print(f"Test paths: {len(path_mapping['test_paths'])}")
        print(f"Docs paths: {len(path_mapping['docs_paths'])}")
        print()
        print("Path mapping:")
        for category, paths in path_mapping.items():
            print(f"  {category}:")
            for path in paths:
                print(f"    - {path}")
    except (FileNotFoundError, YAMLParseError) as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)
