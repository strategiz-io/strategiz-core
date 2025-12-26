#!/usr/bin/env python3
"""
Aggregate quality metrics from Maven analysis tools.

This script parses XML reports from SpotBugs, PMD, Checkstyle, and JaCoCo,
then aggregates them into a JSON payload suitable for posting to the
Strategiz quality metrics cache endpoint.

Usage:
    python aggregate-quality-metrics.py <output-json>

Environment Variables:
    GIT_COMMIT_HASH: Git commit SHA (required)
    GIT_BRANCH: Git branch name (required)
    BUILD_NUMBER: CI build number (optional)
    ANALYSIS_SOURCE: Source of analysis (default: "github-actions")
"""

import sys
import json
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime, timezone
import os
import glob


def parse_spotbugs_report(report_path):
    """Parse SpotBugs XML report and extract bug counts by severity."""
    if not report_path.exists():
        print(f"Warning: SpotBugs report not found at {report_path}")
        return {"bugs": 0, "vulnerabilities": 0}

    try:
        tree = ET.parse(report_path)
        root = tree.getroot()

        bugs = 0
        vulnerabilities = 0

        for bug in root.findall('.//BugInstance'):
            priority = bug.get('priority', '3')
            category = bug.get('category', '')

            # Priority 1 = High, 2 = Medium, 3 = Low
            # Category SECURITY = vulnerability, others = bug
            if 'SECURITY' in category or 'MALICIOUS' in category:
                vulnerabilities += 1
            else:
                bugs += 1

        return {"bugs": bugs, "vulnerabilities": vulnerabilities}
    except Exception as e:
        print(f"Error parsing SpotBugs report: {e}")
        return {"bugs": 0, "vulnerabilities": 0}


def parse_pmd_report(report_path):
    """Parse PMD XML report and extract code smell counts."""
    if not report_path.exists():
        print(f"Warning: PMD report not found at {report_path}")
        return {"code_smells": 0}

    try:
        tree = ET.parse(report_path)
        root = tree.getroot()

        code_smells = len(root.findall('.//violation'))

        return {"code_smells": code_smells}
    except Exception as e:
        print(f"Error parsing PMD report: {e}")
        return {"code_smells": 0}


def parse_checkstyle_report(report_path):
    """Parse Checkstyle XML report and extract style violation counts."""
    if not report_path.exists():
        print(f"Warning: Checkstyle report not found at {report_path}")
        return {"style_violations": 0}

    try:
        tree = ET.parse(report_path)
        root = tree.getroot()

        violations = len(root.findall('.//error'))

        return {"style_violations": violations}
    except Exception as e:
        print(f"Error parsing Checkstyle report: {e}")
        return {"style_violations": 0}


def parse_jacoco_report(report_path):
    """Parse JaCoCo XML report and extract coverage percentage."""
    if not report_path.exists():
        print(f"Warning: JaCoCo report not found at {report_path}")
        return {"coverage": 0.0}

    try:
        tree = ET.parse(report_path)
        root = tree.getroot()

        # Find line coverage counter
        line_counter = None
        for counter in root.findall('.//counter[@type="LINE"]'):
            line_counter = counter
            break

        if line_counter is not None:
            covered = int(line_counter.get('covered', '0'))
            missed = int(line_counter.get('missed', '0'))
            total = covered + missed

            if total > 0:
                coverage = (covered / total) * 100.0
            else:
                coverage = 0.0
        else:
            coverage = 0.0

        return {"coverage": round(coverage, 2)}
    except Exception as e:
        print(f"Error parsing JaCoCo report: {e}")
        return {"coverage": 0.0}


def find_report_file(pattern):
    """Find report file matching glob pattern."""
    files = glob.glob(pattern, recursive=True)
    if files:
        return Path(files[0])
    return None


def calculate_technical_debt(bugs, vulnerabilities, code_smells, style_violations):
    """
    Estimate technical debt in hours.

    Rough estimates:
    - Bug: 2 hours to fix
    - Vulnerability: 4 hours to fix
    - Code smell: 0.5 hours to fix
    - Style violation: 0.1 hours to fix
    """
    hours = (bugs * 2) + (vulnerabilities * 4) + (code_smells * 0.5) + (style_violations * 0.1)

    if hours < 60:
        return f"{int(hours)}h"
    else:
        days = hours / 8  # 8-hour workday
        return f"{int(days)}d"


def calculate_reliability_rating(bugs, vulnerabilities):
    """
    Calculate reliability rating (A-E scale).

    Based on SonarQube rating system:
    A = 0 bugs
    B = 1-10 bugs
    C = 11-50 bugs
    D = 51-100 bugs
    E = 100+ bugs
    """
    total = bugs + vulnerabilities

    if total == 0:
        return "A"
    elif total <= 10:
        return "B"
    elif total <= 50:
        return "C"
    elif total <= 100:
        return "D"
    else:
        return "E"


def calculate_maintainability_rating(code_smells, style_violations):
    """
    Calculate maintainability rating (A-E scale).

    Based on total technical debt ratio:
    A = 0-5% of codebase
    B = 6-10%
    C = 11-20%
    D = 21-50%
    E = 50%+

    For simplicity, using violation counts as proxy.
    """
    total = code_smells + style_violations

    if total <= 100:
        return "A"
    elif total <= 500:
        return "B"
    elif total <= 1000:
        return "C"
    elif total <= 2000:
        return "D"
    else:
        return "E"


def calculate_quality_gate_status(bugs, vulnerabilities, code_smells, coverage):
    """
    Determine if quality gate passes.

    Quality gate criteria:
    - No critical vulnerabilities
    - Bugs < 50
    - Code smells < 500
    - Coverage > 50%
    """
    if vulnerabilities > 0:
        return "FAILED"
    if bugs >= 50:
        return "FAILED"
    if code_smells >= 500:
        return "FAILED"
    if coverage < 50.0:
        return "FAILED"

    return "PASSED"


def main():
    if len(sys.argv) < 2:
        print("Usage: python aggregate-quality-metrics.py <output-json>")
        sys.exit(1)

    output_file = sys.argv[1]

    # Required environment variables
    git_commit = os.getenv('GIT_COMMIT_HASH')
    git_branch = os.getenv('GIT_BRANCH')

    if not git_commit or not git_branch:
        print("Error: GIT_COMMIT_HASH and GIT_BRANCH environment variables are required")
        sys.exit(1)

    # Optional environment variables
    build_number = os.getenv('BUILD_NUMBER', '')
    analysis_source = os.getenv('ANALYSIS_SOURCE', 'github-actions')

    print("Searching for analysis reports...")

    # Find report files
    spotbugs_report = find_report_file('**/target/spotbugs/*.xml')
    pmd_report = find_report_file('**/target/pmd/pmd.xml')
    checkstyle_report = find_report_file('**/target/checkstyle/checkstyle-result.xml')
    jacoco_report = find_report_file('**/target/site/jacoco/jacoco.xml')

    print(f"SpotBugs report: {spotbugs_report}")
    print(f"PMD report: {pmd_report}")
    print(f"Checkstyle report: {checkstyle_report}")
    print(f"JaCoCo report: {jacoco_report}")

    # Parse reports
    print("\nParsing reports...")
    spotbugs_data = parse_spotbugs_report(spotbugs_report) if spotbugs_report else {"bugs": 0, "vulnerabilities": 0}
    pmd_data = parse_pmd_report(pmd_report) if pmd_report else {"code_smells": 0}
    checkstyle_data = parse_checkstyle_report(checkstyle_report) if checkstyle_report else {"style_violations": 0}
    jacoco_data = parse_jacoco_report(jacoco_report) if jacoco_report else {"coverage": 0.0}

    # Extract values
    bugs = spotbugs_data["bugs"]
    vulnerabilities = spotbugs_data["vulnerabilities"]
    code_smells = pmd_data["code_smells"]
    style_violations = checkstyle_data["style_violations"]
    coverage = jacoco_data["coverage"]

    print(f"\nResults:")
    print(f"  Bugs: {bugs}")
    print(f"  Vulnerabilities: {vulnerabilities}")
    print(f"  Code smells: {code_smells}")
    print(f"  Style violations: {style_violations}")
    print(f"  Coverage: {coverage}%")

    # Calculate derived metrics
    technical_debt = calculate_technical_debt(bugs, vulnerabilities, code_smells, style_violations)
    reliability_rating = calculate_reliability_rating(bugs, vulnerabilities)
    maintainability_rating = calculate_maintainability_rating(code_smells, style_violations)
    security_rating = "A" if vulnerabilities == 0 else "E"
    quality_gate_status = calculate_quality_gate_status(bugs, vulnerabilities, code_smells, coverage)
    total_issues = bugs + vulnerabilities + code_smells + style_violations

    print(f"\nDerived metrics:")
    print(f"  Technical debt: {technical_debt}")
    print(f"  Reliability rating: {reliability_rating}")
    print(f"  Maintainability rating: {maintainability_rating}")
    print(f"  Security rating: {security_rating}")
    print(f"  Quality gate: {quality_gate_status}")

    # Build output JSON
    analysis_id = f"{git_commit[:8]}-{datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S')}"

    output = {
        "analysisId": analysis_id,
        "analyzedAt": datetime.now(timezone.utc).isoformat(),
        "gitCommitHash": git_commit,
        "gitBranch": git_branch,
        "bugs": bugs,
        "vulnerabilities": vulnerabilities,
        "codeSmells": code_smells,
        "coverage": coverage,
        "duplications": 0.0,  # Not calculated from free tools
        "technicalDebt": technical_debt,
        "reliabilityRating": reliability_rating,
        "securityRating": security_rating,
        "maintainabilityRating": maintainability_rating,
        "qualityGateStatus": quality_gate_status,
        "totalIssues": total_issues,
        "newIssues": 0,  # Not calculated without baseline comparison
        "analysisSource": analysis_source,
        "buildNumber": build_number
    }

    # Write output JSON
    with open(output_file, 'w') as f:
        json.dump(output, f, indent=2)

    print(f"\nQuality metrics written to: {output_file}")
    print(f"Analysis ID: {analysis_id}")


if __name__ == "__main__":
    main()
