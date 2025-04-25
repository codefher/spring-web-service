pipeline {
    /*  El host Jenkins (o nodo con label “docker”) debe tener Docker,
        así los pasos que lo usan funcionan                     */
    agent any          // agente “real”, no contenedor

    triggers {
        pollSCM('H/5 * * * *') // Opcional: verifica cambios cada 5 minutos
    }

    environment {
        IMAGE_NAME = 'codefher/spring-web-service'
        REGISTRY   = 'https://registry.hub.docker.com'
        CREDS_ID   = 'dockerhub-creds'
        SONARQUBE_SERVER   = 'MySonarQube'
        SONAR_PROJECT_KEY  = 'spring-web-service'
        SONAR_PROJECT_NAME = 'Spring Web Service'
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

    }

    post {
        success { echo "✅ Deployed ${IMAGE_NAME}:${env.BUILD_NUMBER} to staging" }
        failure { echo "❌ Algo falló, revisa logs" }
        always  { cleanWs notFailBuild: true, deleteDirs: true }
    }
}
