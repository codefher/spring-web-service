pipeline {
  agent any

  tools {
    maven 'Maven 3.9.4'      // coincide con la instalación de Maven que definiste en Manage Jenkins → Global Tool Configuration
    jdk 'jdk-17'             // idem para tu JDK
  }

  environment {
    SERVICE_PORT = '8081'    // el puerto en el que quieres exponer tu servicio
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Prepare') {
      steps {
        // marca mvnw como ejecutable
        sh 'chmod +x mvnw'
      }
    }

    stage('Build & Test') {
      steps {
        // empaqueta + ejecuta tests
        sh './mvnw clean package'
      }
    }

    stage('Archive JAR') {
      steps {
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Run Service') {
      steps {
        // mata cualquier instancia previa y levanta tu jar en background
        sh '''
          pkill -f "demo.*jar" || true
          nohup java -jar target/demo-0.0.1-SNAPSHOT.jar \
            --server.port=${SERVICE_PORT} > service.log 2>&1 &
        '''
      }
    }

    stage('Smoke Test') {
      steps {
        // espera unos segundos a que arranque
        sh 'sleep 10'
        // chequea el endpoint
        sh "curl -f http://localhost:${SERVICE_PORT}/api/greeting"
      }
    }
  }

  post {
    always {
      // Guarda el log del service por si falla
      archiveArtifacts artifacts: 'service.log', allowEmptyArchive: true
    }
    success {
      echo "✅ Build OK y servicio corriendo en puerto ${SERVICE_PORT}"
    }
    failure {
      echo "❌ Algo falló, consulta los artefactos y logs"
    }
  }
}
