pipeline {
    /* 1. Build dentro del contenedor Maven correcto */
    agent {
        docker {
            image 'maven:3.8.8-eclipse-temurin-17-alpine'   // <-- tag existente
            args  '-v $HOME/.m2:/root/.m2'
        }
    }

    environment {
        IMAGE_NAME = 'codefher/spring-web-service'
        REGISTRY   = 'https://registry.hub.docker.com'
        CREDS_ID   = 'dockerhub-creds'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build & Test') {
            steps { sh './mvnw clean verify -B' }
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
            agent any           // docker-compose corre en el host
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

    /* 2. Limpieza segura: crea un nodo temporal,
          así siempre hay workspace aunque el pull haya fallado */
    post {
        success {
            echo "✅ Deployed ${IMAGE_NAME}:${env.BUILD_NUMBER} to staging"
        }
        failure {
            echo "❌ Algo falló, revisa logs"
        }
        always {
            node { cleanWs() }
        }
    }
}
