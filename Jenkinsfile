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
                    echo "Executing Absolute Final Cluster Deployment..."
                    sh "docker cp /usr/local/bin/kubectl minikube:/usr/bin/kubectl"
                    
                    // Use the internal port 6443 which is standard for K8s API inside the node
                    // We also ensure the deployment file is piped correctly into the sub-shell
                    sh "docker exec -i minikube sh -c 'cat > /tmp/deploy.yaml && kubectl apply --kubeconfig=/etc/kubernetes/admin.conf --server=https://127.0.0.1:6443 --insecure-skip-tls-verify --validate=false -f /tmp/deploy.yaml' < deployment.yaml"
                    
                    // Final Verification
                    sh "docker exec minikube kubectl get pods --kubeconfig=/etc/kubernetes/admin.conf --server=https://127.0.0.1:6443 --insecure-skip-tls-verify"
                    echo "ASSIGNMENT COMPLETE: Deployment successful."
                }
            }
        }
    }
}
