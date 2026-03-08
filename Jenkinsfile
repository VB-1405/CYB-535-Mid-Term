pipeline {
    agent any
    
    tools {
        // This tells Jenkins to use the Maven tool we named 'Maven' in Settings
        maven 'Maven'
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
                sh 'mvn -B clean compile'
            }
        }
        
        stage('JUnit Testing') {
            steps {
                sh 'mvn -B test'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('sonarqube') {
                        // Using the MidTerm credential ID configured in Jenkins
                        sh "mvn -B sonar:sonar -Dsonar.host.url=\${SONARQUBE_SERVER}"
                    }
                }
            }
        }
    }
}
