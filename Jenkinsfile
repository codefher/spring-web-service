pipeline {
    /* ---- 1. El build corre dentro de un contenedor Maven válido ---- */
    agent {
        docker {
            // Tag correcto: eclipse-temurin-17 (o slim/alpine)
            image 'maven:3.8.8-eclipse-temurin-17-slim'
            args  '-v $HOME/.m2:/root/.m2'         // cache local del repo Maven
        }
    }

    /* ---- 2. Variables de uso global ---- */
    environment {
        IMAGE_NAME = 'codefher/spring-web-service'
        REGISTRY   = 'https://registry.hub.docker.com'
        CREDS_ID   = 'dockerhub-creds'             // id de tus credenciales en Jenkins
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // Con el contenedor Maven no hace falta chmod; mvnw sigue funcionando
                sh './mvnw clean verify -B'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    /*  Tag numérico = Nº de build
                        Tag latest   = para despliegues manuales o difusiones rápidas  */
                    dockerImage = docker.build("${IMAGE_NAME}:${env.BUILD_NUMBER}")
                    dockerImage.tag('latest', true)
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry(env.REGISTRY, env.CREDS_ID) {
                        dockerImage.push("${env.BUILD_NUMBER}")
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Staging') {
            /*  Cambiamos de “agente” para no depender del contenedor Maven
                (necesitamos docker-compose instalado en el nodo host) */
            agent any
            steps {
                dir('deploy') {
                    withEnv(["IMAGE_TAG=${env.BUILD_NUMBER}"]) {
                        // down puede fallar si el stack nunca existió
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
            echo "✅ Deployed ${IMAGE_NAME}:${env.BUILD_NUMBER} to staging"
            cleanWs()
        }
        failure {
            echo "❌ Algo falló, revisa logs"
            cleanWs()
        }
    }
}
