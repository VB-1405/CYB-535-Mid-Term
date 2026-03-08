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
                        sh "docker run --rm --volumes-from jenkins --network cicd-network -e SONAR_TOKEN=${SONAR_AUTH_TOKEN} -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-11 mvn -B -Djava.version=11 compile sonar:sonar -Dsonar.host.url=${SONARQUBE_SERVER} -Dsonar.login=${SONAR_AUTH_TOKEN}"
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
                    echo "Executing Zero-Guesswork Cluster Deployment..."
                    sh "docker cp /usr/local/bin/kubectl minikube:/usr/bin/kubectl"
                    
                    // 1. Get the exact internal URL and Hostname from the cluster's own config
                    def serverUrl = sh(script: "docker exec minikube grep 'server:' /etc/kubernetes/admin.conf | awk '{print \$2}'", returnStdout: true).trim()
                    def serverHost = serverUrl.replaceAll('https://', '').split(':')[0]
                    def containerIp = sh(script: "docker exec minikube hostname -I | awk '{print \$1}'", returnStdout: true).trim()
                    echo "Internal Server detected: ${serverUrl} (Host: ${serverHost}, Container IP: ${containerIp})"
                    
                    // 2. Map that hostname to the container's own IP inside the container
                    sh "docker exec -u root minikube sh -c \"sed -i '/${serverHost}/d' /etc/hosts && echo '${containerIp} ${serverHost}' >> /etc/hosts\""
                    
                    // 3. Apply the deployment using the cluster's own config
                    sh "docker exec -i minikube sh -c 'cat > /tmp/deploy.yaml && kubectl apply --kubeconfig=/etc/kubernetes/admin.conf -f /tmp/deploy.yaml' < deployment.yaml"
                    
                    // 4. Verification
                    sh "docker exec minikube kubectl get pods --kubeconfig=/etc/kubernetes/admin.conf"
                    echo "ASSIGNMENT COMPLETE: Deployment successful."
                }
            }
        }
    }
}
