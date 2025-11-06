#!/usr/bin/env python3
"""Integration tests for path_validator.py

Tests the complete workflow:
1. YAML parsing from story files
2. Path validation
3. Coverage reporting (text + JSON)
4. CLI integration

Run: python3 test_integration.py
"""

import json
import sys
import time
from pathlib import Path

# Add lib to path
sys.path.insert(0, str(Path(__file__).parent / 'lib'))

from yaml_parser import parse_story_path_mapping, get_story_id
from path_checker import validate_path_mapping
from coverage_reporter import generate_text_report, generate_json_report


class TestResult:
    """Simple test result tracker."""
    
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.errors = []
    
    def assert_equal(self, actual, expected, message):
        """Assert equality."""
        if actual == expected:
            self.passed += 1
            print(f"  ✅ {message}")
        else:
            self.failed += 1
            error = f"  ❌ {message}\n     Expected: {expected}\n     Got: {actual}"
            print(error)
            self.errors.append(error)
    
    def assert_true(self, condition, message):
        """Assert condition is true."""
        if condition:
            self.passed += 1
            print(f"  ✅ {message}")
        else:
            self.failed += 1
            error = f"  ❌ {message} - condition was False"
            print(error)
            self.errors.append(error)
    
    def assert_in(self, item, container, message):
        """Assert item in container."""
        if item in container:
            self.passed += 1
            print(f"  ✅ {message}")
        else:
            self.failed += 1
            error = f"  ❌ {message}\n     '{item}' not found in {type(container).__name__}"
            print(error)
            self.errors.append(error)
    
    def summary(self):
        """Print summary."""
        total = self.passed + self.failed
        print(f"\n{'='*60}")
        print(f"Test Results: {self.passed}/{total} passed")
        if self.failed > 0:
            print(f"❌ {self.failed} tests FAILED")
            return 1
        else:
            print(f"✅ All tests PASSED")
            return 0


def test_yaml_parser():
    """Test YAML parsing on real story files."""
    print("\n📋 Test 1: YAML Parser")
    print("─" * 60)
    
    result = TestResult()
    repo_root = Path.cwd()
    
    # Test CORE-005 (has path_mapping)
    story_file = repo_root / 'backlog/EPIC-001-backlog-system/stories/CORE-005-git-commit-tracker/README.md'
    
    if not story_file.exists():
        print(f"⚠️  Skipping: {story_file} not found")
        return result
    
    # Parse story
    story_id = get_story_id(story_file)
    path_mapping = parse_story_path_mapping(story_file)
    
    result.assert_equal(story_id, 'CORE-005', "Story ID parsed correctly")
    result.assert_equal(len(path_mapping['code_paths']), 1, "1 code path")
    result.assert_equal(len(path_mapping['test_paths']), 1, "1 test path")
    result.assert_equal(len(path_mapping['docs_paths']), 3, "3 docs paths")
    result.assert_in('scripts/backlog/git_tracker.sh', path_mapping['code_paths'], "git_tracker.sh in code_paths")
    
    return result


def test_path_checker():
    """Test path existence validation."""
    print("\n🔍 Test 2: Path Checker")
    print("─" * 60)
    
    result = TestResult()
    repo_root = Path.cwd()
    
    # Test CORE-005
    story_file = repo_root / 'backlog/EPIC-001-backlog-system/stories/CORE-005-git-commit-tracker/README.md'
    
    if not story_file.exists():
        print(f"⚠️  Skipping: {story_file} not found")
        return result
    
    path_mapping = parse_story_path_mapping(story_file)
    results = validate_path_mapping(path_mapping, repo_root)
    
    # Check code_paths (should exist)
    result.assert_equal(results['code_paths'].exist_count, 1, "git_tracker.sh exists")
    result.assert_equal(results['code_paths'].percentage, 100.0, "code_paths 100%")
    
    # Check test_paths (should be missing)
    result.assert_equal(results['test_paths'].exist_count, 0, "test_git_tracker.sh missing (expected)")
    result.assert_equal(results['test_paths'].percentage, 0.0, "test_paths 0%")
    result.assert_in('scripts/backlog/test_git_tracker.sh', results['test_paths'].missing_paths, "Missing test file tracked")
    
    # Check docs_paths (should exist)
    result.assert_equal(results['docs_paths'].exist_count, 3, "All 3 docs exist")
    result.assert_equal(results['docs_paths'].percentage, 100.0, "docs_paths 100%")
    
    return result


def test_text_reporter():
    """Test text report generation."""
    print("\n📊 Test 3: Text Reporter")
    print("─" * 60)
    
    result = TestResult()
    repo_root = Path.cwd()
    
    story_file = repo_root / 'backlog/EPIC-001-backlog-system/stories/CORE-005-git-commit-tracker/README.md'
    
    if not story_file.exists():
        print(f"⚠️  Skipping: {story_file} not found")
        return result
    
    story_id = get_story_id(story_file)
    path_mapping = parse_story_path_mapping(story_file)
    validation_results = validate_path_mapping(path_mapping, repo_root)
    
    # Generate text report
    text_report = generate_text_report(story_id, validation_results)
    
    result.assert_in('CORE-005', text_report, "Story ID in report")
    result.assert_in('✅', text_report, "Success emoji in report")
    result.assert_in('⚠️', text_report, "Warning emoji in report")
    result.assert_in('80%', text_report, "Overall percentage in report")
    result.assert_in('4/5', text_report, "Overall count in report")
    result.assert_in('MISSING', text_report, "Missing files section in report")
    
    return result


def test_json_reporter():
    """Test JSON report generation and structure."""
    print("\n🔧 Test 4: JSON Reporter")
    print("─" * 60)
    
    result = TestResult()
    repo_root = Path.cwd()
    
    story_file = repo_root / 'backlog/EPIC-001-backlog-system/stories/CORE-005-git-commit-tracker/README.md'
    
    if not story_file.exists():
        print(f"⚠️  Skipping: {story_file} not found")
        return result
    
    story_id = get_story_id(story_file)
    path_mapping = parse_story_path_mapping(story_file)
    validation_results = validate_path_mapping(path_mapping, repo_root)
    
    # Generate JSON report
    json_report = generate_json_report(story_id, validation_results)
    
    # Parse JSON
    try:
        data = json.loads(json_report)
        result.assert_true(True, "JSON is valid")
    except json.JSONDecodeError as e:
        result.assert_true(False, f"JSON is valid: {e}")
        return result
    
    # Check structure
    result.assert_in('story_id', data, "Has story_id field")
    result.assert_in('coverage', data, "Has coverage field")
    result.assert_in('overall', data, "Has overall field")
    
    result.assert_equal(data['story_id'], 'CORE-005', "Story ID correct")
    result.assert_equal(data['overall']['percentage'], 80.0, "Overall percentage correct")
    result.assert_equal(data['coverage']['code_paths']['percentage'], 100.0, "code_paths 100%")
    result.assert_equal(data['coverage']['test_paths']['percentage'], 0.0, "test_paths 0%")
    
    return result


def test_performance():
    """Test performance on multiple stories."""
    print("\n⚡ Test 5: Performance")
    print("─" * 60)
    
    result = TestResult()
    repo_root = Path.cwd()
    
    # Find all EPIC-001 stories
    epic_dir = repo_root / 'backlog/EPIC-001-backlog-system/stories'
    
    if not epic_dir.exists():
        print(f"⚠️  Skipping: {epic_dir} not found")
        return result
    
    story_files = list(epic_dir.glob('CORE-*/README.md'))
    story_count = len(story_files)
    
    print(f"  Found {story_count} stories in EPIC-001")
    
    # Time validation
    start_time = time.time()
    
    for story_file in story_files:
        try:
            path_mapping = parse_story_path_mapping(story_file)
            validate_path_mapping(path_mapping, repo_root)
        except Exception:
            pass  # Skip errors for performance test
    
    elapsed_time = time.time() - start_time
    
    print(f"  Validated {story_count} stories in {elapsed_time:.3f}s")
    print(f"  Average: {elapsed_time/story_count*1000:.1f}ms per story")
    
    # AC6: Should process 100 stories < 5 seconds
    # For 5 stories, should be < 250ms (5s / 100 * 5)
    target_time = 0.25  # 250ms for 5 stories
    
    result.assert_true(elapsed_time < target_time, f"Performance < {target_time}s (actual: {elapsed_time:.3f}s)")
    
    # Test epic-level aggregation performance
    start_time = time.time()
    
    story_results = {}
    for story_file in story_files:
        try:
            story_id = get_story_id(story_file)
            if story_id:
                path_mapping = parse_story_path_mapping(story_file)
                validation_results = validate_path_mapping(path_mapping, repo_root)
                story_results[story_id] = validation_results
        except Exception:
            pass
    
    elapsed_time = time.time() - start_time
    
    print(f"  Epic aggregation: {elapsed_time:.3f}s for {len(story_results)} stories")
    result.assert_true(elapsed_time < 0.5, f"Epic aggregation < 0.5s (actual: {elapsed_time:.3f}s)")
    
    return result


def test_edge_cases():
    """Test edge cases and error handling."""
    print("\n🛡️  Test 6: Edge Cases")
    print("─" * 60)
    
    result = TestResult()
    repo_root = Path.cwd()
    
    # Test story without path_mapping (CORE-001)
    story_file = repo_root / 'backlog/EPIC-001-backlog-system/stories/CORE-001-markdown-structure/README.md'
    
    if story_file.exists():
        try:
            path_mapping = parse_story_path_mapping(story_file)
            
            # Should return empty lists (not error)
            result.assert_equal(len(path_mapping['code_paths']), 0, "Empty code_paths for story without mapping")
            result.assert_equal(len(path_mapping['test_paths']), 0, "Empty test_paths for story without mapping")
            result.assert_equal(len(path_mapping['docs_paths']), 0, "Empty docs_paths for story without mapping")
            
            # Validate empty mapping (should be 100% - vacuous truth)
            validation_results = validate_path_mapping(path_mapping, repo_root)
            
            result.assert_equal(validation_results['code_paths'].percentage, 100.0, "Empty mapping = 100% (vacuous)")
            
        except Exception as e:
            result.assert_true(False, f"Handle missing path_mapping gracefully: {e}")
    else:
        print(f"  ⚠️  Skipping: CORE-001 not found")
    
    # Test nonexistent story
    fake_story = repo_root / 'backlog/FAKE-STORY/README.md'
    try:
        parse_story_path_mapping(fake_story)
        result.assert_true(False, "Should raise FileNotFoundError for missing story")
    except FileNotFoundError:
        result.assert_true(True, "FileNotFoundError raised for missing story")
    
    return result


def main():
    """Run all integration tests."""
    print("="*60)
    print("🧪 Path Validator Integration Tests")
    print("="*60)
    
    all_results = []
    
    # Run tests
    all_results.append(test_yaml_parser())
    all_results.append(test_path_checker())
    all_results.append(test_text_reporter())
    all_results.append(test_json_reporter())
    all_results.append(test_performance())
    all_results.append(test_edge_cases())
    
    # Overall summary
    print("\n" + "="*60)
    print("📊 OVERALL SUMMARY")
    print("="*60)
    
    total_passed = sum(r.passed for r in all_results)
    total_failed = sum(r.failed for r in all_results)
    total_tests = total_passed + total_failed
    
    print(f"Total: {total_passed}/{total_tests} assertions passed")
    
    if total_failed > 0:
        print(f"\n❌ {total_failed} assertions FAILED")
        print("\nFailed assertions:")
        for r in all_results:
            for error in r.errors:
                print(error)
        return 1
    else:
        print(f"\n✅ ALL {total_tests} ASSERTIONS PASSED")
        return 0


if __name__ == '__main__':
    sys.exit(main())
