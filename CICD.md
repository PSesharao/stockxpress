# CI/CD Pipeline Documentation

This document describes the Continuous Integration and Continuous Deployment (CI/CD) pipeline for the StockXpress microservices platform.

## Table of Contents

- [Overview](#overview)
- [Pipeline Architecture](#pipeline-architecture)
- [Workflows](#workflows)
  - [CI - Build and Test](#ci---build-and-test)
  - [CD - Deploy](#cd---deploy)
  - [Security Scan](#security-scan)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Environment Variables and Secrets](#environment-variables-and-secrets)
- [Deployment Strategy](#deployment-strategy)
- [Monitoring and Notifications](#monitoring-and-notifications)
- [Rollback Procedures](#rollback-procedures)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Overview

The StockXpress CI/CD pipeline is built using GitHub Actions and provides:

- **Automated builds** for all microservices
- **Comprehensive testing** (unit, integration, smoke tests)
- **Security scanning** (dependencies, containers, SAST)
- **Multi-stage deployments** (staging → production)
- **Automated rollback** on deployment failures
- **Docker image management** with versioning

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Code Push/PR                             │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CI - Build and Test                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  Build   │→ │   Unit   │→ │Integration│→│  Docker  │        │
│  │  Maven   │  │  Tests   │  │   Tests   │  │  Images  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Security Scanning                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │Dependency│  │Container │  │   SAST   │  │  Secret  │        │
│  │  Scan    │  │  Scan    │  │(SonarQube│  │  Scan    │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CD - Deploy                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │ Deploy   │→ │  Smoke   │→ │ Deploy   │                      │
│  │ Staging  │  │  Tests   │  │Production│                      │
│  └──────────┘  └──────────┘  └──────────┘                      │
│                                     │                            │
│                                     ▼                            │
│                              ┌──────────┐                        │
│                              │ Rollback │                        │
│                              │(on fail) │                        │
│                              └──────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

## Workflows

### CI - Build and Test

**File:** `.github/workflows/build.yml`

**Trigger:**
- Push to `main`, `develop`, or `feature/*` branches
- Pull requests to `main` or `develop`
- Manual workflow dispatch

**Jobs:**

1. **Build**
   - Checks out code
   - Sets up JDK 8 with Temurin distribution
   - Caches Maven dependencies
   - Builds parent project with all modules
   - Uploads build artifacts

2. **Unit Tests** (Matrix Strategy)
   - Runs unit tests for each service:
     - common-lib
     - product-service
     - order-service
     - inventory-service
     - discovery-server
     - api-gateway
     - notification-service
   - Generates test reports
   - Uploads code coverage reports

3. **Integration Tests**
   - Starts required services (MySQL, MongoDB, Kafka, Redis)
   - Runs integration tests with Testcontainers
   - Uploads test results

4. **Build Docker Images** (Matrix Strategy)
   - Builds Docker images for each service
   - Tags with branch name, SHA, and semantic version
   - Pushes to Docker Hub registry
   - Supports multi-platform builds (amd64, arm64)
   - Uses layer caching for faster builds

5. **Push to Registry Summary**
   - Creates deployment summary
   - Lists all published images

6. **Notify Build Status**
   - Checks overall build status
   - Creates status badge
   - Reports success/failure

**Duration:** ~15-25 minutes (depending on test complexity)

### CD - Deploy

**File:** `.github/workflows/deploy.yml`

**Trigger:**
- Successful completion of CI workflow on `main` branch
- Manual workflow dispatch with environment selection

**Jobs:**

1. **Deploy to Staging**
   - Configures kubectl for staging cluster
   - Updates Kubernetes manifests with new image tags
   - Deploys infrastructure components:
     - MySQL (inventory and order databases)
     - MongoDB
     - Kafka + Zookeeper
     - Redis
     - Zipkin
   - Deploys microservices in order:
     - discovery-server (first)
     - Core services (product, inventory, order, notification)
     - api-gateway (last)
   - Verifies deployment status
   - Creates deployment summary

2. **Smoke Tests**
   - Waits for services to stabilize
   - Tests all service health endpoints:
     - Discovery Server
     - API Gateway
     - Product Service
     - Order Service
     - Inventory Service
   - Runs end-to-end order flow test:
     - Authenticates with Keycloak
     - Creates a test product
     - Places an order
     - Verifies order creation
   - Generates smoke test report

3. **Deploy to Production**
   - Requires staging smoke tests to pass
   - Creates backup of current deployment
   - Updates production Kubernetes manifests
   - Deploys with blue-green strategy:
     - discovery-server first
     - Services deployed one-by-one
     - 30-second wait between deployments
   - Configures production-specific settings:
     - Higher replica counts (3 for services, 2 for gateway)
     - Production log level (WARN)
   - Verifies all pods are running
   - Runs production smoke tests
   - Creates production deployment summary

4. **Rollback on Failure**
   - Triggers automatically if production deployment fails
   - Rolls back all services to previous version
   - Verifies rollback status
   - Creates rollback notification

**Duration:** 
- Staging: ~10-15 minutes
- Smoke Tests: ~3-5 minutes
- Production: ~15-20 minutes

### Security Scan

**File:** `.github/workflows/security-scan.yml`

**Trigger:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Scheduled daily at 2 AM UTC
- Manual workflow dispatch

**Jobs:**

1. **Dependency Vulnerability Scan**
   - OWASP Dependency Check
     - Scans Maven dependencies
     - Fails build on CVSS score ≥ 7
     - Generates SARIF reports
   - Maven Dependency Tree Analysis
   - Snyk dependency scanning
     - High severity threshold
     - All projects scanned
   - Uploads results to GitHub Security tab

2. **Container Image Scan** (Matrix Strategy)
   - Scans each service's Docker image with:
     - **Trivy**: Comprehensive vulnerability scanner
     - **Grype**: Anchore vulnerability scanner
     - **Snyk Container**: Commercial-grade scanning
   - Severity levels: CRITICAL, HIGH, MEDIUM
   - Uploads SARIF results to GitHub Security
   - Generates container scan summary

3. **SAST - SonarQube Analysis**
   - Performs static application security testing
   - Integrates with SonarCloud/SonarQube
   - Analyzes:
     - Code quality
     - Security hotspots
     - Code coverage (with JaCoCo)
     - Technical debt
   - Enforces quality gate
   - Generates analysis summary with dashboard link

4. **Secret Scanning**
   - **TruffleHog**: Detects secrets in code and commits
   - **Gitleaks**: Finds hardcoded credentials
   - Scans entire repository history
   - Only reports verified secrets

5. **Code Quality Analysis**
   - **SpotBugs**: Finds bugs in Java code
     - Max effort level
     - Low threshold
   - **Checkstyle**: Code style validation
   - **PMD**: Code analysis and duplicate detection
   - Uploads reports as artifacts

6. **License Compliance Check**
   - Identifies third-party licenses
   - Generates THIRD-PARTY.txt report
   - Checks for license violations

7. **Security Summary**
   - Aggregates all scan results
   - Creates comprehensive summary table
   - Reports overall security status
   - Fails if critical issues detected

**Duration:** ~20-30 minutes

## Prerequisites

### Required Software

- **GitHub Account** with Actions enabled
- **Docker Hub Account** for container registry
- **Kubernetes Cluster** (staging and production)
- **Maven 3.6+** (handled by GitHub Actions)
- **Java 8** (Temurin distribution)

### Optional Services

- **SonarCloud/SonarQube** for code analysis
- **Snyk Account** for advanced security scanning
- **Keycloak** for authentication (in deployments)

## Setup Instructions

### 1. Fork/Clone Repository

```bash
git clone https://github.com/PSesharao/stockxpress.git
cd stockxpress
```

### 2. Configure GitHub Secrets

Navigate to: `Settings → Secrets and variables → Actions → New repository secret`

Add the following secrets:

#### Docker Registry
```
DOCKER_USERNAME=your-dockerhub-username
DOCKER_PASSWORD=your-dockerhub-token
```

#### Kubernetes Clusters
```
KUBE_CONFIG_STAGING=base64-encoded-kubeconfig-staging
KUBE_CONFIG_PRODUCTION=base64-encoded-kubeconfig-production
```

To encode kubeconfig:
```bash
cat ~/.kube/config-staging | base64 -w 0
cat ~/.kube/config-production | base64 -w 0
```

#### Keycloak (for smoke tests)
```
KEYCLOAK_CLIENT_ID=stockxpress-client
KEYCLOAK_CLIENT_SECRET=your-client-secret
```

#### Security Scanning
```
SONAR_TOKEN=your-sonarcloud-token
SONAR_ORGANIZATION=your-org-name
SNYK_TOKEN=your-snyk-token
GITLEAKS_LICENSE=your-gitleaks-license (optional)
```

### 3. Configure GitHub Environments

Create two environments: `staging` and `production`

**For Production:**
1. Go to `Settings → Environments → New environment`
2. Name: `production`
3. Add protection rules:
   - ✅ Required reviewers (2 reviewers)
   - ✅ Wait timer (5 minutes)
   - ✅ Limit to protected branches (`main` only)

**For Staging:**
1. Name: `staging`
2. No protection rules needed

### 4. Prepare Kubernetes Clusters

#### Create Namespaces

```bash
# Staging
kubectl create namespace stockxpress-staging

# Production
kubectl create namespace stockxpress-production
```

#### Deploy Base Infrastructure

```bash
# Apply infrastructure components
kubectl apply -f k8s/base/infrastructure/ -n stockxpress-staging
kubectl apply -f k8s/base/infrastructure/ -n stockxpress-production
```

### 5. Update Configuration Files

#### Update `pom.xml` Docker Registry

Replace `seshrao` with your Docker Hub username:

```xml
<to><image>registry.hub.docker.com/YOUR_USERNAME/${artifactId}</image></to>
```

#### Create Dependency Check Suppressions (Optional)

Create `dependency-check-suppressions.xml` in project root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <!-- Add suppressions for false positives -->
</suppressions>
```

### 6. Trigger First Build

```bash
git add .
git commit -m "Setup CI/CD pipeline"
git push origin main
```

Monitor the workflow in GitHub Actions tab.

## Environment Variables and Secrets

### Repository Secrets

| Secret Name | Description | Required | Example |
|-------------|-------------|----------|----------|
| `DOCKER_USERNAME` | Docker Hub username | Yes | `seshrao` |
| `DOCKER_PASSWORD` | Docker Hub token/password | Yes | `dckr_pat_xxx` |
| `KUBE_CONFIG_STAGING` | Base64 staging kubeconfig | Yes | `YXBpVmVyc2lvbi...` |
| `KUBE_CONFIG_PRODUCTION` | Base64 production kubeconfig | Yes | `YXBpVmVyc2lvbi...` |
| `KEYCLOAK_CLIENT_ID` | Keycloak client ID | Yes | `stockxpress-client` |
| `KEYCLOAK_CLIENT_SECRET` | Keycloak client secret | Yes | `abc123xyz` |
| `SONAR_TOKEN` | SonarCloud authentication token | Optional | `sqp_xxx` |
| `SONAR_ORGANIZATION` | SonarCloud organization | Optional | `stockxpress-org` |
| `SNYK_TOKEN` | Snyk API token | Optional | `xxx` |
| `GITLEAKS_LICENSE` | Gitleaks license key | Optional | `xxx` |

### Environment Variables

| Variable | Description | Default | Customizable |
|----------|-------------|---------|-------------|
| `JAVA_VERSION` | Java version for builds | `8` | Yes |
| `MAVEN_OPTS` | Maven JVM options | `-Xmx2g` | Yes |
| `DOCKER_REGISTRY` | Container registry | `docker.io` | Yes |
| `KUBECTL_VERSION` | kubectl version | `v1.28.0` | Yes |
| `SONAR_HOST_URL` | SonarQube server URL | `https://sonarcloud.io` | Yes |

## Deployment Strategy

### Staging Deployment

- **Trigger:** Automatic on successful CI build (main branch)
- **Strategy:** Rolling update
- **Replicas:** 1 per service
- **Rollback:** Manual if smoke tests fail
- **Downtime:** Minimal (rolling update)

### Production Deployment

- **Trigger:** Manual approval after staging validation
- **Strategy:** Blue-green with canary phases
- **Replicas:** 3 for services, 2 for API Gateway
- **Rollback:** Automatic on failure
- **Downtime:** Zero-downtime deployment

### Deployment Order

1. **Infrastructure Components** (parallel)
   - MySQL (inventory-db, order-db)
   - MongoDB
   - Kafka + Zookeeper
   - Redis
   - Zipkin

2. **Discovery Server** (wait for ready)
   - Eureka server must be up first

3. **Core Services** (parallel, then wait)
   - product-service
   - inventory-service
   - order-service
   - notification-service

4. **API Gateway** (last)
   - Routes traffic to services

### Rollback Strategy

**Automatic Rollback Triggers:**
- Deployment failure
- Pod crash loops
- Health check failures
- Smoke test failures (production)

**Rollback Process:**
```bash
kubectl rollout undo deployment/<service-name> -n <namespace>
kubectl rollout status deployment/<service-name> -n <namespace>
```

**Manual Rollback:**
```bash
# Rollback to specific revision
kubectl rollout history deployment/<service-name> -n <namespace>
kubectl rollout undo deployment/<service-name> --to-revision=<revision> -n <namespace>
```

## Monitoring and Notifications

### GitHub Actions UI

- **Workflow Runs:** View all executions
- **Job Summaries:** Rich markdown reports
- **Artifacts:** Download test reports, coverage
- **Security Alerts:** View vulnerability findings

### Workflow Status Badges

Add to README.md:

```markdown
![CI](https://github.com/PSesharao/stockxpress/workflows/CI%20-%20Build%20and%20Test/badge.svg)
![CD](https://github.com/PSesharao/stockxpress/workflows/CD%20-%20Deploy/badge.svg)
![Security](https://github.com/PSesharao/stockxpress/workflows/Security%20Scan/badge.svg)
```

### Email Notifications

GitHub sends automatic emails for:
- Workflow failures
- Successful deployments
- Required approvals

### Slack Integration (Optional)

Add to workflow:

```yaml
- name: Notify Slack
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK_URL }}
    payload: |
      {
        "text": "Deployment completed: ${{ job.status }}"
      }
```

## Rollback Procedures

### Automatic Rollback

The pipeline includes automatic rollback for production deployments:

1. **Detection:** Deployment failure or pod crash
2. **Execution:** `kubectl rollout undo` for all services
3. **Verification:** Check pod status and health
4. **Notification:** Create rollback summary

### Manual Rollback

#### Via GitHub Actions UI

1. Go to `Actions → CD - Deploy`
2. Click on failed workflow run
3. Re-run the `rollback` job

#### Via kubectl

```bash
# Connect to production cluster
export KUBECONFIG=~/.kube/config-production

# View deployment history
kubectl rollout history deployment/product-service -n stockxpress-production

# Rollback to previous version
kubectl rollout undo deployment/product-service -n stockxpress-production

# Rollback to specific revision
kubectl rollout undo deployment/product-service --to-revision=5 -n stockxpress-production

# Monitor rollback
kubectl rollout status deployment/product-service -n stockxpress-production

# Verify pods are running
kubectl get pods -n stockxpress-production -l app=product-service
```

#### Rollback All Services

```bash
#!/bin/bash
SERVICES=("discovery-server" "product-service" "inventory-service" "order-service" "notification-service" "api-gateway")

for service in "${SERVICES[@]}"; do
  echo "Rolling back $service..."
  kubectl rollout undo deployment/$service -n stockxpress-production
  kubectl rollout status deployment/$service -n stockxpress-production --timeout=300s
done

echo "Rollback completed!"
```

## Best Practices

### Development Workflow

1. **Feature Branches**
   ```bash
   git checkout -b feature/new-feature
   # Make changes
   git commit -m "feat: add new feature"
   git push origin feature/new-feature
   ```

2. **Create Pull Request**
   - CI runs automatically
   - Review test results
   - Fix any failures
   - Request code review

3. **Merge to Main**
   - CI + CD runs automatically
   - Deploys to staging
   - Run smoke tests
   - Approve production deployment

### Commit Messages

Use conventional commits:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `test:` Test changes
- `refactor:` Code refactoring
- `chore:` Maintenance

### Testing Strategy

1. **Unit Tests** (Required)
   - Write tests for all new code
   - Minimum 80% code coverage
   - Fast execution (<1 min per service)

2. **Integration Tests** (Required)
   - Test service interactions
   - Use Testcontainers for dependencies
   - Run in CI environment

3. **Smoke Tests** (Required for deployment)
   - Test critical paths
   - Verify service health
   - End-to-end scenarios

### Security Best Practices

1. **Never commit secrets**
   - Use GitHub Secrets
   - Use environment variables
   - Scan with TruffleHog/Gitleaks

2. **Keep dependencies updated**
   - Review Dependabot PRs
   - Fix critical vulnerabilities immediately
   - Update monthly

3. **Review security scan results**
   - Check GitHub Security tab weekly
   - Prioritize CRITICAL and HIGH issues
   - Document suppressions

4. **Use signed commits**
   ```bash
   git config --global commit.gpgsign true
   ```

### Performance Optimization

1. **Use caching**
   - Maven dependencies cached
   - Docker layer caching enabled
   - SonarCloud cache

2. **Parallel execution**
   - Unit tests run in parallel (matrix)
   - Container scans in parallel
   - Independent jobs parallelized

3. **Conditional execution**
   - Skip builds on docs changes
   - Skip scans on dependabot PRs (optional)

## Troubleshooting

### Common Issues

#### Build Failures

**Problem:** Maven build fails

**Solution:**
```bash
# Check Maven logs
# Ensure all modules are listed in parent pom.xml
# Verify dependencies are available
# Clear local cache if needed
mvn clean install -U
```

#### Docker Build Failures

**Problem:** Docker image build fails

**Solution:**
```bash
# Check Dockerfile syntax
# Ensure JAR is built first
# Verify base image is accessible
# Check Docker Hub credentials
```

#### Test Failures

**Problem:** Integration tests fail in CI

**Solution:**
```bash
# Check service containers are healthy
# Verify port conflicts
# Increase timeout values
# Check database initialization
```

#### Deployment Failures

**Problem:** Kubernetes deployment fails

**Solution:**
```bash
# Check cluster connectivity
kubectl cluster-info

# Verify namespace exists
kubectl get namespaces

# Check deployment status
kubectl describe deployment <service> -n <namespace>

# View pod logs
kubectl logs <pod-name> -n <namespace>

# Check resource limits
kubectl top nodes
kubectl top pods -n <namespace>
```

#### Secret/Configuration Issues

**Problem:** Missing or invalid secrets

**Solution:**
1. Verify secret exists in GitHub Settings
2. Check secret name matches workflow
3. For kubeconfig, ensure base64 encoding:
   ```bash
   cat kubeconfig | base64 -w 0
   ```
4. Test kubeconfig locally:
   ```bash
   echo "$KUBE_CONFIG" | base64 -d > /tmp/kubeconfig
   kubectl --kubeconfig=/tmp/kubeconfig get nodes
   ```

### Debug Mode

Enable debug logging in workflows:

1. Go to `Settings → Secrets → Variables`
2. Add variable: `ACTIONS_STEP_DEBUG` = `true`
3. Add variable: `ACTIONS_RUNNER_DEBUG` = `true`

### Getting Help

1. **Check Workflow Logs**
   - View detailed step logs
   - Download logs for offline analysis

2. **Review Documentation**
   - GitHub Actions docs
   - Kubernetes docs
   - Service-specific docs

3. **Contact Team**
   - Create GitHub issue
   - Tag relevant team members
   - Provide workflow run link

## Maintenance

### Regular Tasks

**Weekly:**
- Review security scan results
- Check for failed workflows
- Monitor deployment success rate

**Monthly:**
- Update dependencies
- Review and update secrets
- Optimize workflow performance
- Clean up old artifacts

**Quarterly:**
- Update GitHub Actions versions
- Review and update documentation
- Audit access controls
- Performance review

### Workflow Updates

When updating workflows:

1. Create feature branch
2. Test changes thoroughly
3. Use workflow_dispatch for testing
4. Create PR with description
5. Get review from DevOps team
6. Merge and monitor

### Artifact Retention

Artifacts are retained for:
- Build artifacts: 7 days
- Test reports: 7 days
- Security reports: 30 days
- Coverage reports: 7 days

## Metrics and KPIs

### Pipeline Performance

- **Build Time:** Target <20 minutes
- **Deployment Time:** Target <15 minutes
- **Success Rate:** Target >95%
- **Time to Production:** Target <1 hour

### Quality Metrics

- **Code Coverage:** Target >80%
- **Security Score:** Target A
- **Technical Debt:** Target <5%
- **Duplicated Code:** Target <3%

### Operational Metrics

- **Deployment Frequency:** Track daily
- **Change Failure Rate:** Target <5%
- **Mean Time to Recovery:** Target <30 minutes
- **Lead Time for Changes:** Target <4 hours

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)

---

**Last Updated:** 2024

**Maintained by:** DevOps Team

**Contact:** For questions or issues, please create a GitHub issue or contact the DevOps team.