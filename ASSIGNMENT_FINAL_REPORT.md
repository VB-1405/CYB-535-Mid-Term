# Midterm Assignment Final Report

## Title

CI/CD Pipeline with Docker, Kubernetes, Jenkins, and SonarQube

## Student Project Repository

- GitHub: `https://github.com/rohansonawane/test-ci-cd.git`
- Branch: `main`

---

## 1) Objective

Set up and execute a complete CI/CD workflow for a Java application using:

- Jenkins (pipeline automation)
- SonarQube (static code quality analysis)
- Docker (container build/push)
- Kubernetes via Minikube (deployment)

Also implement advanced "real industry improvements":

- Helm charts
- Jenkins agent-based pipeline variant
- GitOps using Argo CD
- Security scanning using Trivy
- Monitoring with Prometheus and Grafana

---

## 2) Environment and Tools

- OS: macOS
- Container runtime: Colima + Docker CLI
- Kubernetes: Minikube
- Build tool: Maven
- CI: Jenkins in Docker
- Code Quality: SonarQube in Docker
- Registry: Docker Hub

Installed/used tools:

- `docker`, `kubectl`, `minikube`, `mvn`
- `helm`, `trivy`, `argocd` CLI

---

## 3) Implementation Summary (Start to End)

### Phase A: Base infrastructure setup

1. Verified installed tools first and installed missing packages.
2. Started Docker runtime.
3. Ran setup script to create:
   - Docker network `cicd-network`
   - Jenkins container
   - SonarQube container
   - Java helper containers
   - Minikube cluster

### Phase B: Core project and pipeline setup

1. Created Java application and test (`App.java`, `AppTest.java`).
2. Added Maven project config (`pom.xml`).
3. Added Dockerfile and Kubernetes manifest.
4. Created Jenkins pipeline (`Jenkinsfile`) with stages:
   - Checkout
   - Build (Java 17)
   - Test (Java 11)
   - Sonar analysis
   - Docker build
   - Docker push
   - Kubernetes deploy
5. Configured Jenkins credentials:
   - `sonarqube-token`
   - `docker-hub-credentials`
6. Connected and pushed source to GitHub repository.

### Phase C: Pipeline stabilization and fixes

During execution, several issues were diagnosed and fixed:

- Docker CLI missing in Jenkins container -> installed Docker CLI in container.
- Lightweight SCM checkout issue -> disabled lightweight checkout for job.
- Sonar runtime compatibility issue -> adjusted scanner runtime and auth usage.
- Sonar credential secret empty -> recreated valid `sonarqube-token`.
- Docker Hub auth mismatch -> updated username/token pair.
- Kubernetes deploy command path/file transfer issues -> switched to robust stdin apply method for `kubectl`.

Result: Jenkins pipeline completed with **SUCCESS**.

### Phase D: Advanced improvements implementation

Added and validated:

1. **Helm chart** under `helm/java-cicd-demo`
2. **Agent-based pipeline variant** in `Jenkinsfile.agents`
3. **Argo CD app manifest** and install script
4. **Trivy scan stage** in pipeline + local scan script
5. **Prometheus + Grafana** install scripts and values files

---

## 4) Final Verification Evidence

### Jenkins

- Latest run status: **SUCCESS**
- All required core stages executed successfully.

### SonarQube

- Analysis completed successfully.
- Project dashboard available at local SonarQube server.

### Docker Hub

- Image pushed successfully:
  - `rsonawane2/java-cicd-demo:latest`

### Kubernetes

- Deployment rolled out successfully.
- Service created and pods running.

### Argo CD

- Argo CD installed.
- Application `java-cicd-demo` status:
  - Sync: `Synced`
  - Health: `Healthy`

### Trivy

- Scan executed for image.
- Report showed no HIGH/CRITICAL findings in latest run.

### Monitoring

- Prometheus and Grafana installed in `monitoring` namespace.
- Services exposed and reachable using port-forward.

---

## 5) Delivered Files

Core:

- `Jenkinsfile`
- `pom.xml`
- `Dockerfile`
- `k8s/deployment.yaml`
- `src/main/java/com/rohan/cicd/App.java`
- `src/test/java/com/rohan/cicd/AppTest.java`

Advanced:

- `Jenkinsfile.agents`
- `helm/java-cicd-demo/*`
- `argocd/java-cicd-demo-app.yaml`
- `scripts/install-argocd.sh`
- `scripts/install-monitoring.sh`
- `scripts/run-trivy-local.sh`
- `monitoring/prometheus-values.yaml`
- `monitoring/grafana-values.yaml`

Documentation:

- `README.md`
- `DETAILED_ASSIGNMENT_RUNBOOK.md`
- `ASSIGNMENT_FINAL_REPORT.md`

---

## 6) Conclusion

The assignment was fully completed end-to-end:

- Core CI/CD requirements were successfully implemented and validated.
- Advanced industry enhancements were added and operational.
- Repository is in a submission-ready state with implementation + runbooks + final report.

