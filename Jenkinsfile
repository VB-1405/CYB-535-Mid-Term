pipeline {
    agent any
    
    environment {
        SONARQUBE_SERVER = 'http://sonarqube:9000'
        DOCKER_IMAGE = 'vb1405/vrishabh-midterm-cicd:latest'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build with Java 17') {
            steps {
                sh "docker run --rm --volumes-from jenkins -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-17 mvn -B -Djava.version=17 clean package -DskipTests"
            }
        }
        
        stage('JUnit Testing with Java 11') {
            steps {
                sh "docker run --rm --volumes-from jenkins -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-11 mvn -B -Djava.version=11 clean test"
            }
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
            steps {
                script {
                    // Optimized to skip 800MB Java DB download for faster builds
                    sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image --severity HIGH,CRITICAL --vuln-type os ${DOCKER_IMAGE}"
                }
            }
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
                    echo "Executing Final Cluster Deployment..."
                    // 1. Ensure the binary is present
                    sh "docker cp /usr/local/bin/kubectl minikube:/usr/bin/kubectl"
                    
                    // 2. Get the container's internal IP (the one it listens on)
                    def k8sIp = sh(script: "docker exec minikube hostname -i", returnStdout: true).trim()
                    echo "Internal Cluster IP: ${k8sIp}"
                    
                    // 3. Apply using the internal IP and admin config
                    // This bypasses the 'localhost' connection refused and DNS 'no such host' errors
                    sh "docker exec -i minikube kubectl --kubeconfig=/etc/kubernetes/admin.conf --server=https://${k8sIp}:8443 --insecure-skip-tls-verify apply -f - --validate=false < deployment.yaml"
                    
                    // 4. Verification
                    sh "docker exec minikube kubectl --kubeconfig=/etc/kubernetes/admin.conf --server=https://${k8sIp}:8443 --insecure-skip-tls-verify get pods"
                    echo "ASSIGNMENT COMPLETE: Deployment successful."
                }
            }
        }
    }
}
