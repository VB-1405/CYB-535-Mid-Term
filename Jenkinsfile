pipeline {
    agent any
    
    tools {
        // This tells Jenkins to use the Maven version we configured in the Tools menu
        maven 'maven-3.9'
    }
    
    environment {
        SONARQUBE_SERVER = 'http://sonarqube:9000'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('JUnit Testing') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('sonarqube') {
                        // Using the MidTerm credential ID configured in Jenkins
                        sh "mvn sonar:sonar -Dsonar.host.url=\${SONARQUBE_SERVER}"
                    }
                }
            }
        }
    }
}
