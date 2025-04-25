pipeline {
    /*  El host Jenkins (o nodo con label “docker”) debe tener Docker,
        así los pasos que lo usan funcionan                     */
    agent any          // agente “real”, no contenedor

    environment {
        IMAGE_NAME = 'codefher/spring-web-service'
        REGISTRY   = 'https://registry.hub.docker.com'
        CREDS_ID   = 'dockerhub-creds'
    }

    stages {

        /* ---------- BUILD & TEST DENTRO DEL CONTENEDOR MAVEN ---------- */
        stage('Build & Test') {
            agent {
                docker {
                    image 'maven:3.8.8-eclipse-temurin-17-alpine'
                    args  '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'chmod +x mvnw'                 // o marca +x en Git y quítalo
                sh './mvnw -B clean verify'
            }
        }

        /* ---------- A PARTIR DE AQUÍ CORREMOS EN EL HOST CON DOCKER ---------- */
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
        success { echo "✅ Deployed ${IMAGE_NAME}:${env.BUILD_NUMBER} to staging" }
        failure { echo "❌ Algo falló, revisa logs" }
        always  { cleanWs notFailBuild: true, deleteDirs: true }
    }
}
