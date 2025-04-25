pipeline {
  agent any

  environment {
    IMAGE_NAME         = 'codefher/spring-web-service'
    CREDS_DOCKERHUB    = 'dockerhub-creds'
    SONARQUBE_SERVER   = 'MySonarQube'
    SONAR_TOKEN_ID     = 'sonar-token'
    SONAR_PROJECT_KEY  = 'spring-web-service'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build, Test & SonarQube') {
      agent {
        docker {
          image 'maven:3.8.8-eclipse-temurin-17-alpine'
          args  '-v $HOME/.m2:/root/.m2 --network host'
        }
      }
      steps {
        sh 'chmod +x mvnw'
        withCredentials([string(credentialsId: SONAR_TOKEN_ID, variable: 'SONAR_TOKEN')]) {
          withSonarQubeEnv(SONARQUBE_SERVER) {
            sh """
              ./mvnw clean verify sonar:sonar \
                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                -Dsonar.login=$SONAR_TOKEN \
                -Dsonar.java.binaries=target/classes
            """
          }
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

    stage('Build & Push Docker Image') {
      steps {
        script {
          docker.withRegistry('', CREDS_DOCKERHUB) {
            def img = docker.build("${IMAGE_NAME}:${env.BUILD_NUMBER}")
            img.push()
            img.push('latest')
          }
        }
      }
    }

    stage('Deploy (docker run)') {
      steps {
        script {
          // Si ya hay un contenedor corriendo, lo paramos y eliminamos
          sh "docker stop spring-web-service || true"
          sh "docker rm spring-web-service || true"

          // Tiramos la última imagen y arrancamos
          sh "docker pull ${IMAGE_NAME}:${env.BUILD_NUMBER}"
          sh """
            docker run -d \
              --name spring-web-service \
              -p 8080:8080 \
              ${IMAGE_NAME}:${env.BUILD_NUMBER}
          """
        }
      }
    }
  }

  post {
    success { echo "✅ Despliegue completado: ${IMAGE_NAME}:${env.BUILD_NUMBER}" }
    failure { echo "❌ Ha fallado el pipeline, revisa los logs." }
    always  { cleanWs() }
  }
}
