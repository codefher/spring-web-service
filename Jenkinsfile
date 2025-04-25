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
                // Ahora sí corren los tests
                sh 'mvn clean package'
            }
            post {
                // 1) Archivamos el JAR generado
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
                // 2) Publicamos resultados de tests JUnit
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                // 3) Publicamos informe de cobertura Jacoco
                success {
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/classes',
                        sourcePattern: 'src/main/java',
                        inclusionPattern: '**/*.class'
                    )
                }
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
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def version = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim()
                    env.IMAGE_TAG = "${version}-${env.BUILD_NUMBER}"
                    dockerImage = docker.build("${IMAGE_NAME}:${env.IMAGE_TAG}")
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
            message: "✅ ${env.JOB_NAME} :${env.IMAGE_TAG} desplegado en http://<HOST>:${EXPOSE_PORT}"
        }
        failure {
            slackSend color: 'danger',
                      message: "❌ ${env.JOB_NAME} #${env.BUILD_NUMBER} ha fallado"
        }
        unsuccessful {
            echo "⚠️ Quality Gate falló, pero seguimos con el pipeline"
            // opcional: marcar build como UNSTABLE en lugar de FAILED
            script { currentBuild.result = 'UNSTABLE' }
        }
    }
}
