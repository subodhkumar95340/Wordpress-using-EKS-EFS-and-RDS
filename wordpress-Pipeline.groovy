pipeline{
    agent any
    // tools{
    //     maven 'maven 3.8.5'
    // }
    environment {
        registry = '461655781674.dkr.ecr.us-east-2.amazonaws.com/jenkin-pipeline-build-demo'
        registryCredential = 'jenkins-ecr-login-credentials'
        dockerimage = ''
    }
    options{
        buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds()
		timestamps()
    }
    stages{
        stage("Checkout the Project"){
            steps{
                git 'https://github.com/subodhkumar95340/Wordpress-using-EKS-EFS-and-RDS.git'
            }
        }
        stage("Build the Package"){
            steps{
                echo "Building code"
            }
        }
        // stage("Sonar Quality Check"){
        //     steps{
        //         script{
        //             withSonarQubeEnv(installationName: 'sonar-9', credentialsId: 'jenkin-sonar-token') {
        //             sh 'mvn sonar:sonar'
        //             }
        //             timeout(time: 1, unit: 'HOURS') {
        //             def qg = waitForQualityGate()
        //                 if (qg.status != 'OK') {
        //                     error "Pipeline aborted due to quality gate failure: ${qg.status}"
        //                 }
        //             }
        //         }
        //     }
        // }
        stage('Building the Image') {
            //Install "Docker Pipeline" plugin for this step
            // Also, run this command on jenkin server: sudo usermod -a -G docker jenkins
            steps {
                script {
                    dockerImage = docker.build registry + ":$BUILD_NUMBER"
                }
            }
        }
        stage('Deploy the Image to Amazon ECR'){
            steps{
                script{
                    docker.withRegistry("http://" + registry, "ecr:us-east-2:" + registryCredential ) {
                        dockerImage.push()
                    }
                }
            }
        }
        stage('Removing Image from Jenkins Server'){
            steps{
                sh "docker rmi $registry:$BUILD_NUMBER"
            }
        }
        stage('EKS Deployment') {
            steps{   
                script {
                    withKubeConfig([credentialsId: 'eks', serverUrl: '']) {
                        sh ('kubectl apply -f  kube-manifests/')
                    }
                }
            }
        }
    }
    post{
        success {
            mail bcc: '', body: 'Pipeline build successfully', cc: '', from: 'subodh@unifiedinfotech.net', replyTo: '', subject: 'The Pipeline success', to: 'subodh@unifiedinfotech.net'
        }
        failure {  
            mail bcc: '', body: 'Pipeline build not success', cc: '', from: 'subodh@unifiedinfotech.net', replyTo: '', subject: 'The Pipeline failed', to: 'subodh@unifiedinfotech.net'
        } 
        always {
            cleanWs()
        }
    }
}   
