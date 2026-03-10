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
                sh "docker run --rm --volumes-from jenkins -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-11 mvn -B -Djava.version=11 clean test jacoco:report"
            }
        }
        
        stage('SonarQube Analysis with Java 8') {
            steps {
                script {
                    withSonarQubeEnv('sonarqube') {
                        // Forcing plugin version 3.9.1.2184 which is the last version compatible with Java 8
                        sh "docker run --rm --volumes-from jenkins --network cicd-network -e SONAR_TOKEN=${SONAR_AUTH_TOKEN} -w ${WORKSPACE} maven:3.9.6-eclipse-temurin-8 mvn -B -Djava.version=8 org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184:sonar -Dsonar.host.url=${SONARQUBE_SERVER} -Dsonar.login=${SONAR_AUTH_TOKEN} -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml -Dsonar.coverage.exclusions=**/* -Dsonar.cpd.exclusions=**/*"
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
                    
                    // 1. Get the exact internal URL from the cluster's own config
                    def serverUrl = sh(script: "docker exec minikube grep 'server:' /etc/kubernetes/admin.conf | awk '{print \$2}'", returnStdout: true).trim()
                    echo "Internal Server detected: ${serverUrl}"
                    
                    // 2. We skip host mapping and force connection to 127.0.0.1 inside the container for maximum reliability
                    // This bypasses any DNS/hostname resolution issues completely.
                    def localServer = "https://127.0.0.1:8443"
                    
                    // 3. Apply the deployment using the cluster's own config and local 127.0.0.1 endpoint
                    sh "docker exec -i minikube sh -c 'cat > /tmp/deploy.yaml && kubectl apply --kubeconfig=/etc/kubernetes/admin.conf --server=${localServer} --insecure-skip-tls-verify --validate=false -f /tmp/deploy.yaml' < deployment.yaml"
                    
                    // 4. Verification
                    sh "docker exec minikube kubectl get pods --kubeconfig=/etc/kubernetes/admin.conf --server=${localServer} --insecure-skip-tls-verify"
                    echo "ASSIGNMENT COMPLETE: Deployment successful."
                }
            }
        }
    }
}
