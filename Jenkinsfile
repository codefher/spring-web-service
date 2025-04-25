pipeline {
    agent any

    tools {
        maven 'Maven 3.9.4'            // Asegúrate de que existe con ese nombre en Global Tool Configuration
    }

    environment {
        IMAGE_NAME          = 'codefher/spring-web-service'
        REGISTRY_CREDENTIAL = 'dockerhub-creds'  // ID de tu credential en Jenkins
        SONARQUBE_SERVER    = 'MySonarQube'       // ID del servidor Sonar en Jenkins
        SONAR_PROJECT_KEY   = 'spring-web-service'
        SONAR_PROJECT_NAME  = 'Spring Web Service'
        CONTAINER_NAME      = 'spring-web-service'
        EXPOSE_PORT         = '8081'             // puerto público
        INTERNAL_PORT       = '8080'             // definido en tu Dockerfile
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    sh """
                        mvn sonar:sonar \
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

        stage('Build Docker Image') {
            steps {
                script {
                    // Por defecto usa el Dockerfile en la raíz
                    dockerImage = docker.build("${IMAGE_NAME}:${env.BUILD_NUMBER}")
                }
            }
        }

        stage('Push to Registry') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', "${REGISTRY_CREDENTIAL}") {
                        dockerImage.push()
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // Para despliegue “in-place” en el mismo host que ejecuta Docker
                    sh """
                        # Para no fallar si el contenedor no existe:
                        docker stop ${CONTAINER_NAME}  || true
                        docker rm   ${CONTAINER_NAME}  || true

                        # Trae la última imagen
                        docker pull ${IMAGE_NAME}:${env.BUILD_NUMBER}

                        # Lanza el contenedor
                        docker run -d \\
                          --name ${CONTAINER_NAME} \\
                          -p ${EXPOSE_PORT}:${INTERNAL_PORT} \\
                          ${IMAGE_NAME}:${env.BUILD_NUMBER}
                    """
                }
            }
        }
    }

    post {
        success {
            slackSend color: 'good',
                      message: "✅ ${env.JOB_NAME} #${env.BUILD_NUMBER} desplegado en http://<HOST>:${EXPOSE_PORT}"
        }
        failure {
            slackSend color: 'danger',
                      message: "❌ ${env.JOB_NAME} #${env.BUILD_NUMBER} ha fallado"
        }
    }
}
