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
        DEV_PORT     = '8081'
        STAGING_PORT = '8082'
        PROD_PORT    = '80'
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
                sh 'mvn clean package'
            }
            post {
                // 1) Publicamos JUnit siempre
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                // 2) En caso de éxito, archivamos y publicamos cobertura
                success {
                    // 2a) Archivamos el JAR
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true

                    // 2b) Publicamos Jacoco
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
                    // detecta qué rama es
                    def branch = env.BRANCH_NAME
                    def port = (branch == 'main')    ? PROD_PORT :
                                (branch == 'staging') ? STAGING_PORT :
                                                        DEV_PORT
                    // lo expongo al resto del pipeline
                    env.EXPOSE_PORT = port

                    sh "docker stop ${CONTAINER_NAME} || true"
                    sh "docker rm   ${CONTAINER_NAME} || true"
                    sh "docker pull ${IMAGE_NAME}:${env.IMAGE_TAG}"
                    sh """
                        docker run -d \
                        --name ${CONTAINER_NAME} \
                        -p ${port}:8080 \
                        ${IMAGE_NAME}:${env.IMAGE_TAG}
                    """
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    // opcional: darle unos segundos para que Spring arranque
                    sleep time: 10, unit: 'SECONDS'
                    // ejecuta curl dentro del contenedor
                    sh "docker exec spring-web-service curl -f http://localhost:8080/actuator/health"
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
