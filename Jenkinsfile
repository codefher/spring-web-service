pipeline {
  agent any

  environment {
    IMAGE_NAME = 'codefher/spring-web-service'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Test') {
      steps {
        sh 'chmod +x mvnw'
        sh './mvnw clean package'
      }
    }

    stage('Build Docker Image') {
      steps {
        script {
          // construye la imagen usando el Dockerfile de la raíz
          dockerImage = docker.build("${IMAGE_NAME}:${env.BUILD_NUMBER}")
        }
      }
    }

    stage('Push to Docker Hub') {
      steps {
        script {
          docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-creds') {
            dockerImage.push()
          }
        }
      }
    }

    stage('Deploy to Staging') {
      steps {
        dir('deploy') {
          withEnv(["BUILD_NUMBER=${env.BUILD_NUMBER}"]) {
            sh 'docker-compose down || true'
            sh 'docker-compose pull'
            sh 'docker-compose up -d'
          }
        }
      }
    }
  }

  post {
    success {
      echo "✅ Deployed ${IMAGE_NAME}:${env.BUILD_NUMBER} to staging"
    }
    failure {
      echo "❌ Algo falló, revisa logs"
    }
  }
}
