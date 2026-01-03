#!/usr/bin/env python3
"""
Aggregate test results from various frameworks into unified format for Test Runner API.

Supports:
- JUnit XML (Maven Surefire, JUnit 5)
- Playwright JSON Reporter
- Pytest JSON Report

Outputs JSON in format expected by POST /v1/console/tests/ci/results endpoint.
"""

import argparse
import json
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any
import xml.etree.ElementTree as ET


def parse_junit_xml(xml_file: Path) -> List[Dict[str, Any]]:
    """Parse JUnit XML file and extract test results."""
    tree = ET.parse(xml_file)
    root = tree.getroot()

    results = []

    # Handle both <testsuite> and <testsuites> root elements
    testsuites = root.findall('.//testsuite')
    if not testsuites:
        testsuites = [root] if root.tag == 'testsuite' else []

    for testsuite in testsuites:
        suite_name = testsuite.get('name', 'Unknown Suite')

        for testcase in testsuite.findall('testcase'):
            class_name = testcase.get('classname', '')
            method_name = testcase.get('name', '')
            duration_str = testcase.get('time', '0')

            # Convert duration to milliseconds
            try:
                duration_ms = int(float(duration_str) * 1000)
            except (ValueError, TypeError):
                duration_ms = 0

            # Determine status
            status = 'passed'
            error_message = None
            stack_trace = None

            failure = testcase.find('failure')
            error = testcase.find('error')
            skipped = testcase.find('skipped')

            if failure is not None:
                status = 'failed'
                error_message = failure.get('message', 'Test failed')
                stack_trace = failure.text
            elif error is not None:
                status = 'error'
                error_message = error.get('message', 'Test error')
                stack_trace = error.text
            elif skipped is not None:
                status = 'skipped'
                error_message = skipped.get('message', 'Test skipped')

            result = {
                'testName': f"{class_name}.{method_name}",
                'className': class_name,
                'methodName': method_name,
                'status': status,
                'durationMs': duration_ms,
                'errorMessage': error_message,
                'stackTrace': stack_trace,
            }

            results.append(result)

    return results


def parse_playwright_json(json_file: Path) -> List[Dict[str, Any]]:
    """Parse Playwright JSON reporter output."""
    with open(json_file, 'r') as f:
        data = json.load(f)

    results = []

    for suite in data.get('suites', []):
        suite_file = suite.get('file', 'unknown')

        for spec in suite.get('specs', []):
            test_title = spec.get('title', 'Unknown Test')

            for test in spec.get('tests', []):
                for result in test.get('results', []):
                    status_map = {
                        'passed': 'passed',
                        'failed': 'failed',
                        'timedOut': 'failed',
                        'skipped': 'skipped',
                        'interrupted': 'error',
                    }

                    pw_status = result.get('status', 'unknown')
                    status = status_map.get(pw_status, 'failed')

                    duration_ms = result.get('duration', 0)

                    error_message = None
                    stack_trace = None
                    if result.get('error'):
                        error_obj = result['error']
                        error_message = error_obj.get('message', 'Test failed')
                        stack_trace = error_obj.get('stack', '')

                    screenshots = []
                    videos = []
                    for attachment in result.get('attachments', []):
                        if attachment.get('contentType', '').startswith('image/'):
                            screenshots.append(attachment.get('path', ''))
                        elif attachment.get('contentType', '').startswith('video/'):
                            videos.append(attachment.get('path', ''))

                    test_result = {
                        'testName': test_title,
                        'className': suite_file,
                        'methodName': test_title,
                        'status': status,
                        'durationMs': duration_ms,
                        'errorMessage': error_message,
                        'stackTrace': stack_trace,
                        'screenshots': screenshots if screenshots else None,
                        'videos': videos if videos else None,
                    }

                    results.append(test_result)

    return results


def parse_pytest_json(json_file: Path) -> List[Dict[str, Any]]:
    """Parse pytest JSON report output."""
    with open(json_file, 'r') as f:
        data = json.load(f)

    results = []

    for test in data.get('tests', []):
        nodeid = test.get('nodeid', '')

        # Parse nodeid: tests/test_file.py::TestClass::test_method
        parts = nodeid.split('::')
        file_path = parts[0] if len(parts) > 0 else 'unknown'
        class_name = parts[1] if len(parts) > 1 else ''
        method_name = parts[2] if len(parts) > 2 else parts[1] if len(parts) > 1 else ''

        test_name = f"{class_name}.{method_name}" if class_name else method_name

        # Convert pytest outcome to our status
        outcome_map = {
            'passed': 'passed',
            'failed': 'failed',
            'skipped': 'skipped',
            'error': 'error',
            'xfailed': 'skipped',  # Expected failure
            'xpassed': 'passed',   # Unexpected pass
        }

        outcome = test.get('outcome', 'unknown')
        status = outcome_map.get(outcome, 'failed')

        duration_ms = int(test.get('duration', 0) * 1000)

        error_message = None
        stack_trace = None
        call_info = test.get('call', {})
        if call_info and 'longrepr' in call_info:
            error_message = str(call_info['longrepr'])[:500]  # Truncate
            stack_trace = str(call_info.get('traceback', ''))

        result = {
            'testName': test_name,
            'className': class_name or file_path,
            'methodName': method_name,
            'status': status,
            'durationMs': duration_ms,
            'errorMessage': error_message,
            'stackTrace': stack_trace,
        }

        results.append(result)

    return results


def calculate_summary(results: List[Dict[str, Any]]) -> Dict[str, int]:
    """Calculate test summary statistics."""
    total = len(results)
    passed = sum(1 for r in results if r['status'] == 'passed')
    failed = sum(1 for r in results if r['status'] == 'failed')
    skipped = sum(1 for r in results if r['status'] == 'skipped')
    errors = sum(1 for r in results if r['status'] == 'error')

    return {
        'total': total,
        'passed': passed,
        'failed': failed,
        'skipped': skipped,
        'errors': errors,
    }


def main():
    parser = argparse.ArgumentParser(description='Aggregate test results for Test Runner API')
    parser.add_argument('--type', required=True, choices=['junit', 'playwright', 'pytest'],
                        help='Test framework type')
    parser.add_argument('--input-dir', help='Input directory containing test result files')
    parser.add_argument('--input-file', help='Input file (for pytest JSON)')
    parser.add_argument('--output', required=True, help='Output JSON file')
    parser.add_argument('--app-id', required=True, help='Application ID')
    parser.add_argument('--commit-hash', help='Git commit hash')
    parser.add_argument('--branch', help='Git branch')
    parser.add_argument('--workflow-run-id', help='GitHub Actions workflow run ID')
    parser.add_argument('--workflow-run-url', help='GitHub Actions workflow run URL')

    args = parser.parse_args()

    # Collect all test results
    all_results = []

    if args.type == 'junit':
        # Find all JUnit XML files in input directory
        input_path = Path(args.input_dir)
        xml_files = list(input_path.rglob('**/surefire-reports/TEST-*.xml'))
        xml_files.extend(input_path.rglob('**/failsafe-reports/TEST-*.xml'))

        print(f"Found {len(xml_files)} JUnit XML files")

        for xml_file in xml_files:
            try:
                results = parse_junit_xml(xml_file)
                all_results.extend(results)
                print(f"  Parsed {xml_file.name}: {len(results)} tests")
            except Exception as e:
                print(f"  Error parsing {xml_file}: {e}", file=sys.stderr)

    elif args.type == 'playwright':
        # Find Playwright JSON reporter output
        input_path = Path(args.input_dir)
        json_files = list(input_path.glob('**/results.json'))

        if not json_files:
            print(f"Warning: No Playwright results.json found in {input_path}", file=sys.stderr)

        for json_file in json_files:
            try:
                results = parse_playwright_json(json_file)
                all_results.extend(results)
                print(f"Parsed {json_file.name}: {len(results)} tests")
            except Exception as e:
                print(f"Error parsing {json_file}: {e}", file=sys.stderr)

    elif args.type == 'pytest':
        # Parse pytest JSON report
        if not args.input_file:
            print("Error: --input-file required for pytest", file=sys.stderr)
            sys.exit(1)

        try:
            results = parse_pytest_json(Path(args.input_file))
            all_results.extend(results)
            print(f"Parsed pytest results: {len(results)} tests")
        except Exception as e:
            print(f"Error parsing pytest results: {e}", file=sys.stderr)

    # Calculate summary
    summary = calculate_summary(all_results)

    # Determine overall status
    if summary['errors'] > 0 or summary['failed'] > 0:
        overall_status = 'failed'
    elif summary['passed'] > 0:
        overall_status = 'passed'
    else:
        overall_status = 'pending'

    # Calculate total duration
    total_duration_ms = sum(r.get('durationMs', 0) for r in all_results)

    # Build output JSON matching TestRunEntity + results
    output = {
        'appId': args.app_id,
        'level': 'app',
        'trigger': 'ci-cd',
        'status': overall_status,
        'startTime': datetime.utcnow().isoformat() + 'Z',
        'endTime': datetime.utcnow().isoformat() + 'Z',
        'durationMs': total_duration_ms,
        'totalTests': summary['total'],
        'passedTests': summary['passed'],
        'failedTests': summary['failed'],
        'skippedTests': summary['skipped'],
        'errorTests': summary['errors'],
        'commitHash': args.commit_hash,
        'branch': args.branch,
        'workflowRunId': args.workflow_run_id,
        'workflowRunUrl': args.workflow_run_url,
        'results': all_results,
    }

    # Write output
    with open(args.output, 'w') as f:
        json.dump(output, f, indent=2)

    print(f"\n✅ Aggregated {summary['total']} test results to {args.output}")
    print(f"   Passed: {summary['passed']}, Failed: {summary['failed']}, "
          f"Skipped: {summary['skipped']}, Errors: {summary['errors']}")
    print(f"   Overall status: {overall_status}")


if __name__ == '__main__':
    main()
