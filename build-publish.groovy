pipeline {
    agent {
        label 'dotnet'
    }

    parameters {
        choice(name: 'PROJECT', choices: ['notifier-api'], description: 'Selecciona el proyecto que deseas desplegar')
        choice(name: 'ENVIRONMENT', choices: ['dev', 'test', 'prod'], description: 'Selecciona el ambiente para desplegar')
        choice(name: 'WITH_DEPS', choices: ['No', 'Si'], description: 'Levantar todas las dependencias?')
    }
    environment {
        CLIENT_ID = 'dc-solutions'

        // This can be nexus3 or nexus2
        NEXUS_VERSION = 'nexus3'
        // This can be http or https
        NEXUS_PROTOCOL = 'http'
        // Where your Nexus is running. 'nexus-3' is defined in the docker-compose file
        DOCKER_REGISTRY = '10.8.0.1:6063'
        // Repository where we will upload the artifact
        NEXUS_REPOSITORY = 'docker-registry'
        // Jenkins credential id to authenticate to Nexus OSS
        NEXUS_CREDENTIAL_ID = 'jenkins_nexus'

        GIT_REPO = 'git@github.com:mbrizuela468/notifier-service.git'

        GIT_CREDENTIAL = 'jenkins_github_ed25519'

        DOCKER_COMPOSE_FILE = 'docker-compose.yml'
    }

    stages {
        stage('Prepair Variables') {
            steps {
                script {
                    // Configurar variables según el ambiente seleccionado
                    sh 'echo PARAMS'
                    sh "echo PROJECT: ${params.PROJECT}"
                    sh "echo ENVIRONMENT: ${params.ENVIRONMENT}"

                    currentBuild.description = "#${env.BUILD_NUMBER} Build: ${params.PROJECT} - ${params.ENVIRONMENT}"
                    currentBuild.displayName = "#${env.BUILD_NUMBER} Build: ${params.PROJECT} - ${params.ENVIRONMENT}"

                    switch (params.PROJECT) {
                        case 'notifier-api':
                            env.DOCKERFILE_NAME = 'NotifierService/Dockerfile'
                            break
                        default:
                            error "Ambiente no válido: ${params.ENVIRONMENT}"
                    }

                    def branchName = env.BRANCH_NAME.replace('/', '-') // Reemplazar '/' si existe en el nombre de la rama

                    env.PROJECT_IMAGE_NAME = "${CLIENT_ID}/${params.PROJECT}"
                    env.IMAGE_TAG = "${branchName}-${env.BUILD_NUMBER}"//-${params.ENVIRONMENT}
                    env.PROJECT_NAME = "${CLIENT_ID}-${params.ENVIRONMENT}"
                }
            }
        }
        stage('Checkout') {
            steps {
                    // Clonar el repositorio
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.BRANCH_NAME}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'CleanCheckout']], // Asegura un checkout limpio
                        userRemoteConfigs: [[url: env.GIT_REPO, credentialsId: env.GIT_CREDENTIAL]]
                    ])
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    if(params.ENVIRONMENT == 'dev'){
                        echo "Construyendo servicio ${params.PROJECT} en DEV..."
                        def imageTag = "${PROJECT_IMAGE_NAME}:${IMAGE_TAG}"
                        // Construir la imagen
                        
                        sh "docker build -f ${env.DOCKERFILE_NAME} -t ${imageTag} ."                                             
                        
                        // Imprimir para debug
                        echo "Image Name: ${imageTag}"
                        echo "Registry Path: ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}"
                        
                        // Crear las etiquetas
                        sh "docker tag ${imageTag} ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:${IMAGE_TAG}"
                        sh "docker tag ${imageTag} ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-dev"
                    }                    
                }
            }
        }

        stage('Promoción a Test') {
            steps {                
                script {
                    if(params.ENVIRONMENT == 'test'){
                        echo "Promoviendo imagen ${PROJECT_IMAGE_NAME}:latest-dev a TEST..."

                        sh """
                            docker pull ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-dev

                            docker tag ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-dev \
                                    ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-test
                        """
                    }
                }
                
            }
        }

        stage('Promoción a Prod') {             
            steps {
                script {
                    if(params.ENVIRONMENT == 'prod'){
                        echo "Promoviendo imagen ${PROJECT_IMAGE_NAME} a TEST..."

                        sh """
                            docker pull ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-test

                            docker tag ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-test \
                                    ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest
                        """
                    }                    
                }
            }
        }


        stage('Push to Nexus') {
            steps {
                script {
                    // Etiquetar y subir la imagen a Nexus
                    withCredentials([usernamePassword(credentialsId: env.NEXUS_CREDENTIAL_ID, passwordVariable: 'NEXUS_PASS', usernameVariable: 'NEXUS_USER')]) {
                        sh "echo ${NEXUS_PASS} | docker login ${DOCKER_REGISTRY} --username ${NEXUS_USER} --password-stdin"

                        if(params.ENVIRONMENT == "dev"){
                            sh "docker push ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:${IMAGE_TAG}"
                            sh "docker push ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-dev"
                        } else if(params.ENVIRONMENT == "test"){
                            sh "docker push ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest-test"
                        } else if(params.ENVIRONMENT == "prod"){
                            sh "docker push ${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:latest"
                        }
                    }
                }
            }
        }

        stage('Run Container') {
            steps {
                script {

                    if(params.ENVIRONMENT != "prod"){
                        def noDeps = params.WITH_DEPS == 'Si' ? '' : '--no-deps'
                        def imageTag = params.ENVIRONMENT == 'dev' ? 'latest-dev' : params.ENVIRONMENT == 'test' ? 'latest-test' : 'latest'

                        withCredentials([usernamePassword(credentialsId: env.NEXUS_CREDENTIAL_ID, passwordVariable: 'NEXUS_PASS', usernameVariable: 'NEXUS_USER')]) {
                            sh "echo ${NEXUS_PASS} | docker login ${DOCKER_REGISTRY} --username ${NEXUS_USER} --password-stdin"

                            sh """
                            IMAGE_TAG=${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:${imageTag} \
                            docker compose --env-file .env.${params.ENVIRONMENT} down ${params.PROJECT} || true

                            IMAGE_TAG=${DOCKER_REGISTRY}/${PROJECT_IMAGE_NAME}:${imageTag} \
                            docker compose --env-file .env.${params.ENVIRONMENT} up -d ${noDeps} ${params.PROJECT}

                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finalizado.'
        }
        failure {
            echo 'El pipeline falló.'
        }
    }
}