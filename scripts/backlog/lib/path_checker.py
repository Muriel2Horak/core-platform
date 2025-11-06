#!/usr/bin/env python3
"""Path existence validator for story path mappings.

Checks if files declared in path_mapping actually exist.
Supports glob patterns (e.g., backend/src/**/*.java).

See CORE-006 AC2 for specification.
"""

import glob
from pathlib import Path
from typing import Dict, List, Set


class PathValidationResult:
    """Result of path validation for a single category (code/test/docs)."""
    
    def __init__(self, category: str, declared_paths: List[str], repo_root: Path):
        """
        Args:
            category: Category name (e.g., "code_paths")
            declared_paths: List of paths from path_mapping
            repo_root: Absolute path to repository root
        """
        self.category = category
        self.declared_paths = declared_paths
        self.repo_root = repo_root
        
        # Results
        self.existing_paths: List[str] = []
        self.missing_paths: List[str] = []
        self.glob_expanded: Dict[str, List[str]] = {}  # glob pattern → matched files
        
    def validate(self) -> None:
        """Validate all declared paths."""
        for path_pattern in self.declared_paths:
            self._validate_single_path(path_pattern)
    
    def _validate_single_path(self, path_pattern: str) -> None:
        """Validate a single path or glob pattern.
        
        Args:
            path_pattern: Relative path or glob pattern (e.g., "backend/**/*.java")
        """
        # Normalize path (relative to repo root)
        normalized_path = self._normalize_path(path_pattern)
        
        # Check if it's a glob pattern
        if '*' in path_pattern or '?' in path_pattern:
            self._validate_glob(path_pattern, normalized_path)
        else:
            self._validate_literal(path_pattern, normalized_path)
    
    def _normalize_path(self, path: str) -> Path:
        """Convert relative path to absolute Path object.
        
        Args:
            path: Relative path from path_mapping
            
        Returns:
            Absolute Path object
        """
        # Remove leading ./
        path = path.lstrip('./')
        
        # If already absolute, convert to relative to repo_root
        if Path(path).is_absolute():
            return Path(path)
        
        # Relative path → join with repo_root
        return self.repo_root / path
    
    def _validate_literal(self, original_path: str, normalized_path: Path) -> None:
        """Validate a literal (non-glob) path.
        
        Args:
            original_path: Original path string from path_mapping
            normalized_path: Normalized absolute Path
        """
        if normalized_path.exists():
            self.existing_paths.append(original_path)
        else:
            self.missing_paths.append(original_path)
    
    def _validate_glob(self, original_pattern: str, normalized_path: Path) -> None:
        """Validate a glob pattern.
        
        Args:
            original_pattern: Original glob pattern from path_mapping
            normalized_path: Normalized absolute Path (with glob pattern)
        """
        # Convert Path back to string for glob
        pattern_str = str(normalized_path)
        
        # Expand glob
        matches = glob.glob(pattern_str, recursive=True)
        
        if matches:
            # Convert absolute matches back to relative paths
            relative_matches = [
                str(Path(m).relative_to(self.repo_root))
                for m in matches
            ]
            self.glob_expanded[original_pattern] = relative_matches
            self.existing_paths.append(original_pattern)
        else:
            self.missing_paths.append(original_pattern)
    
    @property
    def total(self) -> int:
        """Total number of declared paths (including glob patterns as 1)."""
        return len(self.declared_paths)
    
    @property
    def exist_count(self) -> int:
        """Number of paths that exist (glob patterns count as 1 if any matches)."""
        return len(self.existing_paths)
    
    @property
    def missing_count(self) -> int:
        """Number of paths that are missing."""
        return len(self.missing_paths)
    
    @property
    def percentage(self) -> float:
        """Percentage of paths that exist (0-100)."""
        if self.total == 0:
            return 100.0  # No paths declared → 100% (vacuous truth)
        return (self.exist_count / self.total) * 100
    
    def to_dict(self) -> Dict:
        """Convert to dict for JSON serialization."""
        return {
            'total': self.total,
            'exist': self.exist_count,
            'missing': self.missing_paths,
            'percentage': round(self.percentage, 1)
        }


def validate_path_mapping(
    path_mapping: Dict[str, List[str]],
    repo_root: Path = None
) -> Dict[str, PathValidationResult]:
    """Validate all paths in a path_mapping dict.
    
    Args:
        path_mapping: Dict with keys code_paths, test_paths, docs_paths
        repo_root: Absolute path to repository root (default: cwd)
        
    Returns:
        Dict mapping category name → PathValidationResult
        
    Example:
        >>> path_mapping = {
        ...     'code_paths': ['backend/Main.java', 'frontend/**/*.tsx'],
        ...     'test_paths': ['backend/MainTest.java'],
        ...     'docs_paths': ['README.md']
        ... }
        >>> results = validate_path_mapping(path_mapping)
        >>> results['code_paths'].percentage
        100.0
    """
    if repo_root is None:
        repo_root = Path.cwd()
    else:
        repo_root = Path(repo_root).resolve()
    
    results = {}
    
    for category, paths in path_mapping.items():
        result = PathValidationResult(category, paths, repo_root)
        result.validate()
        results[category] = result
    
    return results


def get_overall_stats(results: Dict[str, PathValidationResult]) -> Dict:
    """Get overall statistics across all categories.
    
    Args:
        results: Dict mapping category → PathValidationResult
        
    Returns:
        Dict with keys: total, exist, percentage
        
    Example:
        >>> stats = get_overall_stats(results)
        >>> stats['percentage']
        87.5
    """
    total = sum(r.total for r in results.values())
    exist = sum(r.exist_count for r in results.values())
    
    if total == 0:
        percentage = 100.0
    else:
        percentage = (exist / total) * 100
    
    return {
        'total': total,
        'exist': exist,
        'percentage': round(percentage, 1)
    }


if __name__ == '__main__':
    # Manual testing
    import sys
    from yaml_parser import parse_story_path_mapping
    
    if len(sys.argv) < 2:
        print("Usage: python path_checker.py <story_readme.md>")
        sys.exit(1)
    
    story_file = Path(sys.argv[1])
    
    try:
        # Parse path_mapping from story
        path_mapping = parse_story_path_mapping(story_file)
        
        # Validate paths
        results = validate_path_mapping(path_mapping)
        
        # Print results
        print(f"Path validation for: {story_file.name}")
        print()
        
        for category, result in results.items():
            emoji = "✅" if result.percentage == 100 else "⚠️"
            print(f"{emoji} {category}: {result.exist_count}/{result.total} ({result.percentage:.0f}%)")
            
            if result.missing_paths:
                print(f"   Missing:")
                for path in result.missing_paths:
                    print(f"     - {path}")
            
            if result.glob_expanded:
                print(f"   Glob patterns:")
                for pattern, matches in result.glob_expanded.items():
                    print(f"     {pattern} → {len(matches)} files")
        
        print()
        overall = get_overall_stats(results)
        print(f"📈 Overall: {overall['exist']}/{overall['total']} ({overall['percentage']:.0f}%)")
        
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)
