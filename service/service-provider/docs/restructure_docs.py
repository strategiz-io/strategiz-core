#!/usr/bin/env python3
"""
Script to restructure service-provider MDX documentation files
to follow the new comprehensive template structure.
"""

import os
import re
from pathlib import Path

# Template structure
NEW_STRUCTURE = """---
{frontmatter}
---

# {title}

## 1. Description (Business Purpose)

{business_purpose}

## 2. Product and Technical Specifications

### 2.1 Functional Requirements
{functional_requirements}

### 2.2 Non-functional Requirements

#### Performance Requirements
{performance_requirements}

#### Scalability Requirements
{scalability_requirements}

#### Reliability Requirements
{reliability_requirements}

#### Security Requirements
{security_requirements}

#### Maintainability Requirements
{maintainability_requirements}

### 2.3 Endpoint Details
{endpoint_details}

### 2.4 Request Schema
{request_schema}

### 2.5 Response Schema
{response_schema}

### 2.6 Error Codes
{error_codes}

## 3. API Specifications (Redocly)

### 3.1 OpenAPI Specification (with Redocly integration)
{openapi_spec}

### 3.2 Sample Request
{sample_request}

### 3.3 Sample Response - Success
{sample_response_success}

### 3.4 Sample Error Response
{sample_error_response}

## 4. Design Documentation

### 4.1 Component Diagram (draw.io with embedded code)
{component_diagram}

### 4.2 Flow Diagram (draw.io with embedded code)
{flow_diagram}

### 4.3 Sequence Diagram (Mermaid)
{sequence_diagram}

## 5. Quality

### 5.1 Testing

#### Unit Tests (with strategy and examples)
{unit_tests}

#### Integration Tests (with strategy and examples)
{integration_tests}

#### Test Coverage Report
{test_coverage}

#### Load Testing Results
{load_testing}

### 5.2 Code Quality Metrics

#### Code Coverage
{code_coverage}

#### Complexity Metrics
{complexity_metrics}

#### Security Analysis
{security_analysis}

#### Performance Benchmarks
{performance_benchmarks}

### 5.3 Observability & Monitoring

#### Logging Strategy
{logging_strategy}

#### Metrics Collection
{metrics_collection}

#### Distributed Tracing
{distributed_tracing}

#### Alerts Configuration
{alerts_configuration}

#### Dashboard Panels
{dashboard_panels}

#### SLIs and SLOs (with error budget policy)
{slis_slos}

## 6. Security Considerations

### Authentication & Authorization
{auth_authz}

### Data Protection (in transit and at rest)
{data_protection}

### Security Testing (SAST, DAST, Dependency Scanning)
{security_testing}

### Compliance & Standards (GDPR, PCI DSS, SOC 2, OWASP)
{compliance_standards}

### Security Incident Response
{incident_response}

## 7. Maintenance & Support

### Known Issues
{known_issues}

### Future Enhancements
{future_enhancements}

### Runbooks & Troubleshooting
{runbooks}

### Deployment & Rollback
{deployment_rollback}

### Contact & Support
{contact_support}

## 8. Version History

{version_history}

---

{footer}
"""

def extract_section(content, section_pattern, end_pattern=None):
    """Extract content of a section from markdown."""
    match = re.search(section_pattern, content, re.DOTALL)
    if match:
        start = match.end()
        if end_pattern:
            end_match = re.search(end_pattern, content[start:], re.DOTALL)
            if end_match:
                return content[start:start + end_match.start()].strip()
        return content[start:].strip()
    return ""

def parse_mdx_file(filepath):
    """Parse existing MDX file and extract sections."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extract frontmatter
    frontmatter_match = re.match(r'^---\n(.*?)\n---', content, re.DOTALL)
    frontmatter = frontmatter_match.group(1) if frontmatter_match else ""
    
    # Extract title
    title_match = re.search(r'^# (.+)$', content, re.MULTILINE)
    title = title_match.group(1) if title_match else "API Documentation"
    
    return {
        'frontmatter': frontmatter,
        'title': title,
        'content': content
    }

def main():
    docs_dir = Path(__file__).parent
    mdx_files = [
        'provider-callback-api.mdx',
        'provider-connection-api.mdx',
        'provider-delete-api.mdx',
        'provider-query-api.mdx',
        'provider-update-api.mdx'
    ]
    
    print("Documentation Restructuring Report")
    print("=" * 60)
    
    for mdx_file in mdx_files:
        filepath = docs_dir / mdx_file
        if not filepath.exists():
            print(f"❌ {mdx_file} not found")
            continue
        
        print(f"\n📄 Processing {mdx_file}...")
        parsed = parse_mdx_file(filepath)
        
        # Create backup
        backup_path = filepath.with_suffix('.mdx.backup')
        with open(filepath, 'r') as src, open(backup_path, 'w') as dst:
            dst.write(src.read())
        print(f"   ✓ Backup created: {backup_path.name}")
        
        print(f"   ✓ Parsed frontmatter and title")
        print(f"   ℹ Title: {parsed['title']}")
        print(f"   ℹ Content length: {len(parsed['content'])} chars")
    
    print("\n" + "=" * 60)
    print("✅ Analysis complete. Review backups before proceeding.")
    print("\nNext steps:")
    print("1. Review the TEMPLATE.md for the target structure")
    print("2. Manually map existing content to new sections")
    print("3. Fill in missing sections (NFRs, detailed testing, etc.)")

if __name__ == "__main__":
    main()
