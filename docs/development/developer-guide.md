# Strategiz Developer Guide

This comprehensive guide covers all aspects of contributing to and developing for the Strategiz platform, including code standards, conduct, and processes.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Local Development Setup](#local-development-setup)
- [Coding Standards](#coding-standards)
- [Contributing Guidelines](#contributing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Security Policies](#security-policies)
- [Testing Standards](#testing-standards)
- [Release Process](#release-process)

## Local Development Setup

### Prerequisites

Before you begin development, ensure you have the following installed:

* **Java Development Kit (JDK) 21** or higher
* **Maven 3.8+** for building the project
* **Node.js 18+** and **npm** for the frontend
* **HashiCorp Vault** for secret management (handled automatically)
* **Git** for version control

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/strategiz-core.git
   cd strategiz-core
   ```

2. **Install HashiCorp Vault:**
   ```bash
   # On macOS (using Homebrew)
   brew tap hashicorp/tap
   brew install hashicorp/tap/vault
   
   # On Linux (using package manager)
   # wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
   # echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
   # sudo apt update && sudo apt install vault
   ```

3. **Start Vault for local development:**
   ```bash
   ./scripts/local/start-vault-local.sh
   ```
   
   This script will:
   - Start Vault in development mode on port 8200
   - Create a fixed token (`strategiz-local-token`) for local development
   - Enable the KV secrets engine
   - Create the necessary token file at `~/.vault-token`
   - Set up the environment for Spring Boot integration

4. **Build the project:**
   ```bash
   ./scripts/local/build.sh
   ```

5. **Run the application:**
   ```bash
   cd application
   mvn spring-boot:run
   ```

   The application will start on port 8080 and automatically connect to Vault for secret management.

### Vault Configuration

The application uses HashiCorp Vault for secret management in all environments, including local development. Here's what you need to know:

#### Vault Integration

* **Address**: `http://localhost:8200`
* **Token**: `strategiz-local-token` (for local development)
* **Secrets Path**: `secret/strategiz/`
* **Authentication**: TOKEN-based authentication

#### Adding Secrets to Vault

You can add secrets programmatically or via the CLI:

```bash
# Set environment variables
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='strategiz-local-token'

# Add OAuth secrets
vault kv put secret/strategiz/oauth/google \
  client_id="your_google_client_id" \
  client_secret="your_google_client_secret"

vault kv put secret/strategiz/oauth/facebook \
  client_id="your_facebook_client_id" \
  client_secret="your_facebook_client_secret"
```

#### Environment Variables

For initial setup, you can also set environment variables, and they'll be automatically stored in Vault:

```bash
export AUTH_GOOGLE_CLIENT_ID="your_google_client_id"
export AUTH_GOOGLE_CLIENT_SECRET="your_google_client_secret"
export AUTH_FACEBOOK_CLIENT_ID="your_facebook_client_id"
export AUTH_FACEBOOK_CLIENT_SECRET="your_facebook_client_secret"
```

### Development Workflow

#### Starting Development

1. **Start Vault** (if not already running):
   ```bash
   ./scripts/local/start-vault-local.sh
   ```

2. **Build and run the backend**:
   ```bash
   ./scripts/local/build.sh
   cd application
   mvn spring-boot:run
   ```

3. **Start the frontend** (in a separate terminal):
   ```bash
   cd strategiz-ui
   npm install
   npm start
   ```

#### Stopping Development

```bash
# Stop the Spring Boot application (Ctrl+C)
# Stop Vault
pkill vault
```

### Module Structure

The project follows a modular architecture:

```
strategiz-core/
├── application/           # Main Spring Boot application
├── business/             # Business logic modules
├── client/               # External API clients
├── data/                 # Data access layer
├── framework/            # Shared framework components
├── service/              # Service layer (REST controllers)
├── scripts/local/        # Local development scripts
└── strategiz-ui/         # React frontend application
```

### Build Order

Dependencies are built in the following order:
1. Framework modules (exception, logging, secrets, api-docs)
2. Data modules (base, strategy, user, auth, exchange, portfolio, device)
3. Client modules (base, coinbase, coingecko, binanceus, alphavantage, etc.)
4. Business modules (base, portfolio, token-auth, provider-coinbase)
5. Service modules (base, strategy, exchange, portfolio, dashboard, etc.)
6. Application (main Spring Boot app)

### Testing

```bash
# Run all tests
mvn clean test

# Run tests for a specific module
mvn -f service/service-provider/pom.xml test

# Run with coverage
mvn clean verify
```

### Troubleshooting

#### Common Issues

1. **Vault Connection Issues**
   - Ensure Vault is running: `curl http://localhost:8200/v1/sys/health`
   - Check if token file exists: `ls -la ~/.vault-token`
   - Verify token: `vault auth -method=token token=strategiz-local-token`

2. **Build Failures**
   - Ensure all dependencies are built in order using `./scripts/local/build.sh`
   - Check for missing dependencies in `pom.xml` files

3. **Port Conflicts**
   - Vault runs on port 8200
   - Spring Boot application runs on port 8080
   - React frontend runs on port 3000

#### Logs

Application logs are available at:
- Spring Boot: Console output or `application/app_logs.txt`
- Vault: Console output from the vault process

### IDE Setup

#### IntelliJ IDEA

1. Import the project as a Maven project
2. Enable annotation processing
3. Install the Lombok plugin
4. Set up code style according to our standards

#### VS Code

1. Install the Java Extension Pack
2. Install the Spring Boot Extension Pack
3. Configure formatting according to our standards

## Code of Conduct

### Our Pledge

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone, regardless of age, body size, visible or invisible disability, ethnicity, sex characteristics, gender identity and expression, level of experience, education, socio-economic status, nationality, personal appearance, race, religion, or sexual identity and orientation.

We pledge to act and interact in ways that contribute to an open, welcoming, diverse, inclusive, and healthy community.

### Our Standards

Examples of behavior that contributes to a positive environment:

* Using welcoming and inclusive language
* Respecting differing viewpoints and experiences
* Gracefully accepting constructive criticism
* Focusing on what is best for the community
* Showing empathy towards other community members

Examples of unacceptable behavior:

* The use of sexualized language or imagery and unwelcome sexual attention or advances
* Trolling, insulting/derogatory comments, and personal or political attacks
* Public or private harassment
* Publishing others' private information without explicit permission
* Other conduct which could reasonably be considered inappropriate in a professional setting

### Enforcement Responsibilities

Project maintainers are responsible for clarifying and enforcing our standards of acceptable behavior and will take appropriate and fair corrective action in response to any behavior that they deem inappropriate, threatening, offensive, or harmful.

### Scope

This Code of Conduct applies within all project spaces, and also applies when an individual is representing the project or its community in public spaces.

### Enforcement

Instances of abusive, harassing, or otherwise unacceptable behavior may be reported to the project team. All complaints will be reviewed and investigated promptly and fairly.

### Attribution

This Code of Conduct is adapted from the [Contributor Covenant](https://www.contributor-covenant.org/version/2/0/code_of_conduct.html).

## Coding Standards

### Java Code Style

#### Formatting

* Use 4 spaces for indentation, not tabs
* Maximum line length is 120 characters
* Class and method braces go on the same line as the declaration
* Use trailing commas in multi-line initializations
* Always use braces for control structures, even for single-line blocks

```java
// Correct
if (condition) {
    doSomething();
}

// Incorrect
if (condition) doSomething();
```

#### Naming Conventions

* Class names should be in UpperCamelCase
* Method names should be in lowerCamelCase
* Constant names should be in UPPER_SNAKE_CASE
* Package names should be in lowercase
* Interface names should not start with 'I'
* Test classes should end with 'Test'

#### Code Organization

* Organize imports alphabetically and remove unused imports
* Group imports in the following order:
  1. Static imports
  2. Java/javax packages
  3. Third-party libraries
  4. Project imports
* Declare fields at the top of the class, followed by constructors, then methods
* Methods should be ordered by functionality, not by access level

#### Documentation

* All public classes and methods must have JavaDoc comments
* Use `@param`, `@return`, and `@throws` tags appropriately
* Include examples in JavaDoc where appropriate
* Document non-obvious implementation details

### REST API Design Style

#### Endpoint Design

* Use nouns, not verbs, in endpoint paths
* Use plural nouns for collection endpoints
* Use consistent naming conventions for endpoints
* Keep URLs versioned (e.g., `/api/v1/resources`)

#### Response Formatting

* Use consistent response structures
* Use appropriate HTTP status codes
* Include helpful error messages
* Provide pagination for collection endpoints

#### API Security

* Implement proper authentication for all endpoints
* Use HTTPS for all API traffic
* Implement rate limiting
* Validate all input data

## Contributing Guidelines

### Getting Started

1. Fork the repository
2. Clone your fork to your local machine
3. Set up the development environment
4. Create a new branch for your feature/bugfix

### Development Workflow

1. Ensure you're working from the latest code
2. Write code that follows our coding standards
3. Add tests for new functionality
4. Ensure all tests pass locally
5. Update documentation as needed
6. Commit changes with clear, descriptive messages
7. Push changes to your fork
8. Submit a pull request

### Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Types include:
* feat: A new feature
* fix: A bug fix
* docs: Documentation changes
* style: Changes that do not affect the meaning of the code
* refactor: Code change that neither fixes a bug nor adds a feature
* perf: Code change that improves performance
* test: Adding missing tests or correcting existing tests
* chore: Changes to the build process or auxiliary tools

## Pull Request Process

1. Ensure any install or build dependencies are removed before the end of the layer when doing a build
2. Update the README.md or relevant documentation with details of changes to the interface
3. Increase the version numbers in any examples files and the README.md to the new version that this PR would represent
4. The PR should be reviewed by at least one maintainer before it can be merged
5. PRs should be squashed and merged unless there's a good reason to preserve the commit history

### PR Template

```markdown
## Description
Brief description of the changes in this PR.

## Related Issue
Fixes #[issue]

## Type of change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] This change requires a documentation update

## How Has This Been Tested?
Please describe the tests that you ran to verify your changes.

## Checklist:
- [ ] My code follows the style guidelines of this project
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes
```

## Security Policies

### Reporting Security Issues

If you discover a security vulnerability within the project, please send an email to security@strategiz.io. All security vulnerabilities will be promptly addressed.

### Security Best Practices

1. **Data Handling**:
   - Never store sensitive data in plain text
   - Use encryption for all sensitive data
   - Minimize the storage of sensitive data

2. **Authentication & Authorization**:
   - Use Firebase Authentication for user authentication
   - Implement proper role-based access control
   - Never hardcode credentials in the codebase

3. **API Security**:
   - Validate all input data
   - Use rate limiting to prevent abuse
   - Implement proper CORS policies
   - Use HTTPS for all API calls

4. **Dependency Management**:
   - Regularly update dependencies to patch security vulnerabilities
   - Use a dependency scanning tool to identify known vulnerabilities
   - Pin dependency versions to prevent unexpected updates

## Testing Standards

### Unit Testing

* All business logic should have unit tests
* Unit tests should be isolated from external dependencies
* Mock external dependencies to ensure unit tests are fast and reliable
* Aim for high test coverage of critical paths

### Integration Testing

* Integration tests should verify the interaction between components
* Use in-memory databases or containerized services for integration tests
* Verify API contracts through integration tests

### End-to-End Testing

* E2E tests should verify the entire application workflow
* Focus on critical user journeys
* E2E tests should run as part of the CI/CD pipeline

### Test Naming Convention

Tests should follow the naming convention: `methodName_stateUnderTest_expectedBehavior`

Example: `authenticate_invalidCredentials_throwsAuthenticationException`

## Release Process

### Versioning

We follow [Semantic Versioning](https://semver.org/):

* MAJOR version when you make incompatible API changes
* MINOR version when you add functionality in a backwards compatible manner
* PATCH version when you make backwards compatible bug fixes

### Release Checklist

1. Ensure all tests pass in the CI/CD pipeline
2. Update the CHANGELOG.md with details of changes
3. Update version numbers in relevant files
4. Create a new Git tag for the release
5. Build and deploy the application
6. Verify the deployment in staging/production environment
7. Announce the release to stakeholders

### Hotfix Process

For critical issues that require immediate attention:

1. Create a hotfix branch from the production tag
2. Implement and test the fix
3. Create a PR to merge into both main and production branches
4. Deploy the hotfix
5. Update version numbers appropriately
