<p align="center">
  <img src="https://img.shields.io/badge/Strategiz-Framework-0A66C2?style=for-the-badge" alt="Strategiz Framework"/>
</p>

# Strategiz Framework

> **Cross-cutting concerns and shared infrastructure for the Strategiz Platform**

## Documentation

**[View Full Documentation](./docs/README.md)**

## Modules

| Module | Description | Docs |
|--------|-------------|:----:|
| **framework-authorization** | Two-layer authorization (PASETO + OpenFGA) | [Docs](./framework-authorization/docs/README.md) |
| **framework-exception** | Layered exception handling framework | [Docs](./framework-exception/docs/README.md) |
| **framework-logging** | Structured JSON logging infrastructure | [Docs](./framework-logging/docs/README.md) |
| **framework-secrets** | HashiCorp Vault integration | [Docs](./framework-secrets/docs/README.md) |
| **framework-api-docs** | OpenAPI/Swagger configuration | [Docs](./framework-api-docs/docs/README.md) |

## Quick Links

- [Authorization Quick Start](./framework-authorization/docs/README.md#quick-start)
- [Exception Handling Guide](./framework-exception/docs/README.md)
- [Structured Logging Examples](./framework-logging/docs/README.md#usage)
- [Vault Setup Guide](./framework-secrets/docs/README.md)

## Architecture

```
framework/
├── docs/                        # Framework documentation hub
│   └── README.md               # Central documentation
├── framework-api-docs/          # OpenAPI configuration
├── framework-authorization/     # Auth (PASETO + FGA)
│   └── docs/README.md
├── framework-exception/         # Exception handling
│   └── docs/README.md
├── framework-logging/           # Structured logging
│   └── docs/README.md
├── framework-secrets/           # Vault integration
└── pom.xml                      # Parent POM
```

## Adding Dependencies

```xml
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-authorization</artifactId>
    <version>${project.version}</version>
</dependency>
```

See [Full Documentation](./docs/README.md) for all modules.
