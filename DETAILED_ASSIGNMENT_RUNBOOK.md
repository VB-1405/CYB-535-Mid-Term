# Detailed Assignment Runbook

This document is a complete end-to-end guide for the midterm project:

- Jenkins automation
- SonarQube static analysis
- Docker build and push
- Kubernetes deployment
- Industry improvements:
  - Helm
  - Jenkins agents
  - Argo CD GitOps
  - Trivy scanning
  - Prometheus + Grafana

---

## 1) Project Objective

Build a CI/CD pipeline for a Java app that:

1. Checks out code from GitHub
2. Builds with Java 17
3. Runs tests with Java 11
4. Runs SonarQube analysis
5. Builds Docker image
6. Pushes image to Docker Hub
7. Deploys to Kubernetes

---

## 2) Repository Layout

Key files and folders:

- `Jenkinsfile` - main working pipeline
- `Jenkinsfile.agents` - agent-node variant pipeline
- `pom.xml` - Maven build config
- `Dockerfile` - container image build
- `k8s/deployment.yaml` - baseline K8s deployment/service
- `helm/java-cicd-demo/` - Helm chart
- `argocd/java-cicd-demo-app.yaml` - Argo CD application
- `scripts/install-argocd.sh` - installs Argo CD and app
- `scripts/install-monitoring.sh` - installs Prometheus + Grafana
- `scripts/run-trivy-local.sh` - local Trivy scan
- `monitoring/prometheus-values.yaml`
- `monitoring/grafana-values.yaml`
- `setup-cicd-pipeline.sh` - base environment setup
- `cleanup-cicd-pipeline.sh` - reset script

---

## 3) Prerequisites

Required:

- macOS + Homebrew
- Docker runtime (Docker Desktop or Colima)
- Git + GitHub repo
- Internet access to pull images/charts/plugins

CLI tools used:

- `docker`
- `kubectl`
- `minikube`
- `mvn`
- `helm`
- `trivy`
- `argocd` (CLI)

Install missing tools (if needed):

```bash
brew install kubernetes-cli minikube maven helm trivy argocd
```

---

## 4) Start Infrastructure

### 4.1 Docker runtime

If Docker Desktop is unavailable, use Colima:

```bash
colima start --cpu 4 --memory 8
docker info
```

### 4.2 Run setup script

```bash
chmod +x setup-cicd-pipeline.sh cleanup-cicd-pipeline.sh
./setup-cicd-pipeline.sh
```

This script:

- Checks/install prerequisites
- Creates Docker network `cicd-network`
- Starts Jenkins container
- Starts SonarQube container
- Starts Java env containers
- Starts Minikube

---

## 5) Jenkins Access and User

### 5.1 Initial admin password

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 5.2 Login URL

- `http://localhost:8080`

### 5.3 Local Jenkins user created

- Username: `rohanuser`
- Password: `Rohan@job2026!`

---

## 6) SonarQube Setup

### 6.1 SonarQube URL

- `http://localhost:9000`

### 6.2 Local token creation

Token was generated in local SonarQube and stored in Jenkins credential:

- Jenkins credential ID: `sonarqube-token`

---

## 7) Jenkins Credentials Required

Configured credential IDs:

- `sonarqube-token` (Secret Text)
- `docker-hub-credentials` (Username + PAT token)

Docker Hub credential format:

- Username: Docker Hub username
- Password: Docker Hub access token (PAT), not plain account password

---

## 8) Pipeline Configuration

Pipeline source:

- Repo: `https://github.com/rohansonawane/test-ci-cd.git`
- Branch: `main`
- Script path: `Jenkinsfile`

### Stages in `Jenkinsfile`

1. Checkout Code
2. Helm Chart Lint
3. Build with Java 17
4. Run Tests with Java 11
5. Static Analysis (Sonar on Java 11)
6. Build Docker Image
7. Trivy Security Scan
8. Push to Docker Hub
9. Deploy to Kubernetes

---

## 9) Run and Verify Pipeline

Trigger pipeline:

```bash
# If jenkins-cli.jar exists
INIT_PW="$(docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword)"
/opt/homebrew/opt/openjdk/bin/java -jar jenkins-cli.jar \
  -s http://localhost:8080 \
  -auth "admin:${INIT_PW}" \
  build java-cicd-pipeline -s -v
```

Expected final line:

- `Finished: SUCCESS`

---

## 10) Kubernetes Verification

Check deployed resources:

```bash
docker exec minikube /var/lib/minikube/binaries/v1.35.1/kubectl \
  --kubeconfig=/etc/kubernetes/admin.conf \
  get deployments,pods,svc
```

Expected:

- `java-cicd-demo` deployment ready (2 replicas)
- pods in `Running`
- service `java-cicd-demo-service` exposed

---

## 11) Docker Hub Verification

```bash
docker pull rsonawane2/java-cicd-demo:latest
docker images | rg "java-cicd-demo"
```

Expected:

- pull succeeds
- image appears in local images list

---

## 12) SonarQube Verification

In Jenkins console, confirm:

- `ANALYSIS SUCCESSFUL`

In SonarQube UI:

- project `java-cicd-demo` exists
- latest analysis timestamp updates

---

## 13) Industry Improvements

## 13.1 Helm chart

Path:

- `helm/java-cicd-demo`

Validate:

```bash
helm lint helm/java-cicd-demo
```

## 13.2 Jenkins agents approach

File:

- `Jenkinsfile.agents`

Requirements before use:

- Configure Jenkins nodes/agents with labels:
  - `java17-agent`
  - `java11-agent`

## 13.3 Argo CD GitOps

Install + apply app:

```bash
./scripts/install-argocd.sh
```

Verify:

```bash
kubectl get pods -n argocd
kubectl get application -n argocd
```

Expected:

- all Argo CD components running
- app `java-cicd-demo` -> `Synced`, `Healthy`

## 13.4 Trivy security scanning

Run locally:

```bash
./scripts/run-trivy-local.sh rsonawane2/java-cicd-demo:latest
```

Expected:

- vulnerability report output
- HIGH/CRITICAL summary shown

## 13.5 Prometheus + Grafana

Install:

```bash
./scripts/install-monitoring.sh
```

Verify:

```bash
kubectl get deploy,pods,svc -n monitoring
```

Expected:

- Prometheus deployment ready
- Grafana deployment ready

---

## 14) Grafana and Prometheus Access

In this environment, direct NodePort from `minikube ip` may not always route from host.
Use port-forward:

```bash
kubectl -n monitoring port-forward svc/monitoring-grafana 32000:80
kubectl -n monitoring port-forward svc/monitoring-prometheus-server 32001:80
```

Then access:

- Grafana: `http://127.0.0.1:32000`
- Prometheus: `http://127.0.0.1:32001`

Grafana login:

- user: `admin`
- password: `admin123`

---

## 15) Troubleshooting Quick Guide

### Jenkins 401

- Clear browser session/cookies for localhost
- Re-login with valid Jenkins user
- Confirm user via:
  ```bash
  curl -s -u "rohanuser:Rohan@job2026!" http://localhost:8080/whoAmI/api/json
  ```

### Docker login fails in pipeline

- Ensure Jenkins credential `docker-hub-credentials` is correct
- Verify manually:
  ```bash
  echo "<TOKEN>" | docker login -u "<USERNAME>" --password-stdin
  ```

### Sonar auth fails

- Recreate `sonarqube-token` Jenkins secret text
- Ensure pipeline passes `-Dsonar.login=${SONAR_TOKEN}`

### Minikube API timeout

```bash
minikube stop
minikube start --driver=docker
kubectl get nodes
```

### Reset environment

```bash
./cleanup-cicd-pipeline.sh
```

---

## 16) Submission Evidence Checklist

Capture screenshots/log snippets of:

1. Jenkins pipeline with all stages green (`SUCCESS`)
2. Jenkins console end: `Finished: SUCCESS`
3. SonarQube project dashboard (`java-cicd-demo`)
4. Docker Hub repo/tag with pushed image
5. Kubernetes resources (`get deployments,pods,svc`)
6. Argo CD app status (`Synced`, `Healthy`)
7. Grafana and Prometheus UIs reachable
8. Trivy scan output summary

---

## 17) Final Status

This project is fully implemented and validated for:

- core assignment flow
- requested advanced "Real Industry Improvements"

