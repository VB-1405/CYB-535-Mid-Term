# Midterm CI/CD Assignment: Final Successful Runbook
**Student**: Vrishabh Bhavsar
**Project**: Industrial CI/CD Pipeline with Security Scanning and Multi-Java Architecture

---

## 1. Environment Setup (Terminal)
```bash
# 1. Create Network
docker network create cicd-network

# 2. Start Servers
docker run -d --name jenkins -p 8080:8080 -p 50000:50000 --network cicd-network -v jenkins_home:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock jenkins/jenkins:lts
docker run -d --name sonarqube -p 9000:9000 --network cicd-network sonarqube:lts
minikube start --driver=docker

# 3. Force Link Minikube to CI/CD Network
docker network connect cicd-network minikube

# 4. Install Tools in Jenkins
docker exec -u root jenkins apt-get update && docker exec -u root jenkins apt-get install -y docker.io
docker exec -u root jenkins chmod 666 /var/run/docker.sock
docker exec -u root jenkins sh -c 'curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/arm64/kubectl"'
docker exec -u root jenkins install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

---

## 2. Final Jenkinsfile (The "DNS Fix" Version)
```groovy
pipeline {
    agent any
    environment {
        SONARQUBE_SERVER = 'http://sonarqube:9000'
        DOCKER_IMAGE = 'vb1405/vrishabh-midterm-cicd:latest'
    }
    stages {
        stage('Checkout') { steps { checkout scm } }
        
        stage('Build with Java 17') {
            steps { sh "docker run --rm --volumes-from jenkins -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-17 mvn -B -Djava.version=17 clean package -DskipTests" }
        }
        
        stage('JUnit Testing with Java 11') {
            steps { sh "docker run --rm --volumes-from jenkins -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-11 mvn -B -Djava.version=11 clean test" }
        }
        
        stage('SonarQube Analysis with Java 11') {
            steps {
                script {
                    withSonarQubeEnv('sonarqube') {
                        sh "docker run --rm --volumes-from jenkins --network cicd-network -e SONAR_TOKEN=${SONAR_AUTH_TOKEN} -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-11 mvn -B -Djava.version=11 clean sonar:sonar -Dsonar.host.url=${SONARQUBE_SERVER} -Dsonar.login=${SONAR_AUTH_TOKEN}"
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker run --rm --volumes-from jenkins -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-17 mvn -B -Djava.version=17 clean package -DskipTests"
                    sh "docker build -t ${DOCKER_IMAGE} ."
                }
            }
        }
        
        stage('Trivy Security Scan') {
            steps { sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image --severity HIGH,CRITICAL --pkg-types os ${DOCKER_IMAGE}" }
        }
        
        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                        sh "docker push ${DOCKER_IMAGE}"
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh "docker cp /usr/local/bin/kubectl minikube:/usr/bin/kubectl"
                    // Auto-detect internal server URL and map host to localhost
                    def serverUrl = sh(script: "docker exec minikube grep 'server:' /etc/kubernetes/admin.conf | awk '{print \$2}'", returnStdout: true).trim()
                    def serverHost = serverUrl.replaceAll('https://', '').split(':')[0]
                    sh "docker exec -u root minikube sh -c \"echo '127.0.0.1 ${serverHost}' >> /etc/hosts\""
                    // Final guaranteed deployment
                    sh "docker exec -i minikube sh -c 'cat > /tmp/deploy.yaml && kubectl apply --kubeconfig=/etc/kubernetes/admin.conf --server=${serverUrl} --insecure-skip-tls-verify --validate=false -f /tmp/deploy.yaml' < deployment.yaml"
                    sh "docker exec minikube kubectl get pods --kubeconfig=/etc/kubernetes/admin.conf --server=${serverUrl} --insecure-skip-tls-verify"
                }
            }
        }
    }
}
```

---

## 3. Kubernetes deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vrishabh-midterm-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: vrishabh-midterm-app
  template:
    metadata:
      labels:
        app: vrishabh-midterm-app
    spec:
      containers:
      - name: vrishabh-midterm-app
        image: vb1405/vrishabh-midterm-cicd:latest
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: vrishabh-midterm-service
spec:
  type: NodePort
  selector:
    app: vrishabh-midterm-app
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
    nodePort: 30007
```

---

## 4. Final Verification
Run this command on your **Mac Terminal**:
```bash
minikube service vrishabh-midterm-service
```
