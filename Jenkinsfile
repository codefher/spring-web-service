pipeline {
    agent any

    tools {
        maven 'Maven 3.9.4'
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        IMAGE_NAME          = 'codefher/spring-web-service'
        REGISTRY            = 'https://registry.hub.docker.com'
        CREDS_ID            = 'dockerhub-creds'
        SONARQUBE_SERVER    = 'MySonarQube'
        SONAR_PROJECT_KEY   = 'spring-web-service'
        SONAR_PROJECT_NAME  = 'Spring Web Service'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Sonar Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    sh """
                        ./mvnw sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.projectName='${SONAR_PROJECT_NAME}'
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build & Test') {
            agent {
                docker {
                    image 'maven:3.8.8-eclipse-temurin-17-alpine'
                    args  '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw -B clean verify'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dockerImage = docker.build("${IMAGE_NAME}:${env.BUILD_NUMBER}")
                    dockerImage.tag('latest', true)
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry(env.REGISTRY, env.CREDS_ID) {
                        dockerImage.push(env.BUILD_NUMBER)
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                dir('deploy') {
                    withEnv(["IMAGE_TAG=${env.BUILD_NUMBER}"]) {
                        sh 'docker-compose down || true'
                        sh 'docker-compose pull'
                        sh 'docker-compose up -d --remove-orphans'
                    }
                }
            }
        }
    }

    post {
        success {
            slackSend color: 'good',
                      message: "✅ *${env.JOB_NAME}* #${env.BUILD_NUMBER} desplegado con éxito en *Staging*.\n🔗 http://localhost:8080"
        }
        failure {
            slackSend color: 'danger',
                      message: "❌ *${env.JOB_NAME}* #${env.BUILD_NUMBER} ha fallado. Revisa el log de Jenkins."
        }
        always {
            cleanWs notFailBuild: true, deleteDirs: true
        }
    }
}
