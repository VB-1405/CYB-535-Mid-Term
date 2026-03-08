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
            agent {
                docker { image 'maven:3.9.6-eclipse-temurin-17' }
            }
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }
        
        stage('JUnit Testing with Java 11') {
            agent {
                docker { image 'maven:3.9.6-eclipse-temurin-11' }
            }
            steps {
                sh 'mvn -B test'
            }
        }
        
        stage('SonarQube Analysis') {
            // Sonar 9.9 requires Java 17+ to run the scanner
            agent {
                docker { image 'maven:3.9.6-eclipse-temurin-17' }
            }
            steps {
                script {
                    withSonarQubeEnv('sonarqube') {
                        sh "mvn -B sonar:sonar -Dsonar.host.url=${SONARQUBE_SERVER}"
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    // This uses the host's Docker engine
                    sh "docker build -t ${DOCKER_IMAGE} ."
                }
            }
        }
        
        stage('Trivy Security Scan') {
            steps {
                script {
                    sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image --severity HIGH,CRITICAL ${DOCKER_IMAGE}"
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
                    sh "kubectl apply -f deployment.yaml"
                }
            }
        }
    }
}
