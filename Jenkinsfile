pipeline {
    agent any
    
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
                        sh "mvn sonar:sonar -Dsonar.host.url=\${SONARQUBE_SERVER}"
                    }
                }
            }
        }
    }
}
