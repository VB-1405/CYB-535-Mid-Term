# Midterm Project: Building an Industrial CI/CD Pipeline
**Course:** CYB-535: Software Security & DevSecOps  
**Student:** Vrishabh Bhavsar  
**Date:** March 8, 2026

---

## 1. Project Objective
The objective of this assignment is to design, implement, and verify a secure **Continuous Integration/Continuous Deployment (CI/CD)** pipeline. We will move a Java application from source code to a live Kubernetes deployment entirely automatically, ensuring code quality (SonarQube) and container security (Trivy) along the way.

## 2. Prerequisites
Before beginning, ensure the following tools are installed on your host machine (MacOS/Windows/Linux):
*   **Docker Desktop:** Running and accessible via terminal.
*   **Git:** For version control.
*   **Browser:** To access Jenkins and application dashboards.

---

## Part 1: Infrastructure Setup
In this phase, we act as **DevOps Engineers** to provision the virtual infrastructure. We will use Docker containers to simulate a real-world server environment.

### Step 1.1: Create a Dedicated Network
We need a private network so our tools (Jenkins, SonarQube, Kubernetes) can talk to each other securely by name.

**Command:**
```bash
docker network create cicd-network
```
*Why?* Without this, containers are isolated and Jenkins cannot send code to SonarQube.

### Step 1.2: Provision the Jenkins Orchestrator
We will launch Jenkins, mounting the host's Docker socket so Jenkins can spawn its own "child" containers (Docker-in-Docker).

**Command:**
```bash
docker run -d --name jenkins -p 8080:8080 -p 50000:50000 --network cicd-network -v jenkins_home:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock jenkins/jenkins:lts
```
*   **Port 8080:** Web UI Access.
*   **`-v /var/run/docker.sock`:** Crucial for allowing Jenkins to run Docker commands.

### Step 1.3: Provision SonarQube Quality Server
We launch SonarQube to act as our Quality Gate.

**Command:**
```bash
docker run -d --name sonarqube -p 9000:9000 --network cicd-network sonarqube:lts
```
*   **Port 9000:** Web UI Access for quality reports.

### Step 1.4: Provision Kubernetes Cluster (Minikube)
We start a single-node Kubernetes cluster using Docker as the driver.

**Command:**
```bash
minikube start --driver=docker --force
docker network connect cicd-network minikube
```
*Note:* We manually connect Minikube to our `cicd-network` so Jenkins can reach it later.

---

## Part 2: Environment Hardening & Configuration
Now that the servers are running, we must configure them to work together safely.

### Step 2.1: Grant Docker Permissions to Jenkins
By default, the Jenkins user inside the container cannot use Docker. We must grant it root-level access to the socket.

**Command:**
```bash
docker exec -u root jenkins chmod 666 /var/run/docker.sock
```

### Step 2.2: Install Deployment Tools (Kubectl)
Jenkins needs the `kubectl` CLI tool to send commands to our Minikube cluster. We download it directly into the running Jenkins container.

**Command:**
```bash
docker exec -u root jenkins sh -c 'curl -LO "https://dl.k8s.io/release/v1.35.2/bin/linux/arm64/kubectl" && install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl'
```

### Step 2.3: Configure Docker Hub Credentials
To publish our images, Jenkins needs secure access to Docker Hub.
1.  Open Jenkins (`http://localhost:8080`).
2.  Go to **Manage Jenkins** -> **Credentials** -> **System** -> **Global credentials**.
3.  Click **"Add Credentials"**.
4.  **Kind:** Username with password.
5.  **ID:** `docker-hub-credentials`.
6.  **Username/Password:** Enter your Docker Hub login details.

---

## Part 3: The Automated Pipeline Architecture
This is the core of the assignment. We replaced manual execution with a declarative `Jenkinsfile`.

### Step 3.1: Defining the Pipeline Logic (`Jenkinsfile`)
We created a script that defines the following automated stages:

*   **Multi-Java Architecture:**
    *   **Build:** Uses a `maven:3-eclipse-temurin-17` container to compile modern Java code.
    *   **Test:** Uses a `maven:3-eclipse-temurin-11` container to run JUnit tests and generate coverage.
    *   **Quality (Java 8 Target):** Uses a `maven:3-eclipse-temurin-11` container to run the scanner (required for SonarQube 9.9+ connectivity), but explicitly targets **Java 1.8** compatibility for the code analysis via the `-Dsonar.java.source=1.8` flag. This satisfies both the legacy rubric requirement and modern tool constraints.
*   **Security Scanning:**
    *   Uses **Trivy** to scan the built Docker image for High/Critical OS vulnerabilities.
*   **Deployment:**
    *   Uses a "Zero-Guesswork" strategy by connecting to the Minikube API server via `https://127.0.0.1:8443` internally, guaranteeing connection stability.

### Step 3.2: Defining the Infrastructure (`deployment.yaml`)
We created a Kubernetes manifest to define the desired state:
*   **Replicas:** 2 (for High Availability).
*   **Service:** NodePort 30007 (for external access).
*   **Image Policy:** `IfNotPresent` (to efficiently use local images).

### Step 3.3: Enabling Quality Metrics (`pom.xml`)
We added the `jacoco-maven-plugin` to the project. This is essential because SonarQube requires coverage data to pass the "Quality Gate."

---

## Part 4: Execution and Verification
With the setup complete, we trigger the automation.

### Step 4.1: Run the Pipeline
1.  In Jenkins, create a new "Pipeline" job named `vrishabh-midterm-pipeline`.
2.  Link it to the GitHub Repository URL.
3.  Click **"Build Now"**.

### Step 4.2: Verify Quality Gates
Once the build completes, we verify the code quality.
1.  **Access Fix:** Run this command on your Mac to ensure the dashboard link works:
    ```bash
    echo "127.0.0.1 sonarqube" | sudo tee -a /etc/hosts
    ```
2.  Click the link in Jenkins. You should see **Passed** with valid analysis metrics.

### Step 4.3: Verify Final Deployment
We verify that the application is actually running in the cluster.

**Command (Check Pods):**
```bash
kubectl get pods
# Expected Output: 2 pods with status "Running"
```

**Command (Access Application):**
To view the web app on your local machine, forward the port:
```bash
kubectl port-forward svc/vrishabh-midterm-service 9090:80
```
Then open your browser to: **http://localhost:9090**

---

## Conclusion
This assignment successfully demonstrated the transition from manual operations to a fully automated DevSecOps pipeline. By integrating **Trivy security scanning**, **Multi-Java version support**, and **automated Kubernetes deployment**, the project meets and exceeds modern industrial standards.
