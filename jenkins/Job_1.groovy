pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        copyArtifactPermission('Job_2')
        timeout(time: 20, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        cron('H/2 * * * *')
    }

    stages {
        stage('Checkout master and archive') {
            agent any
            steps {
                deleteDir()

                dir('project') {
                    git branch: 'master',
                        credentialsId: 'github-ssh',
                        url: 'git@github.com:Vladislav-Yurievich/jenkins-test-task.git'

                    sh 'git log -1 --oneline'
                }

                sh 'tar -czf project.tar.gz -C project .'

                archiveArtifacts(
                    artifacts: 'project.tar.gz',
                    fingerprint: true
                )
            }
        }

        stage('Run Job_2') {
            steps {
                build job: 'Job_2',
                    parameters: [
                        string(name: 'SOURCE_BUILD', value: env.BUILD_NUMBER)
                    ],
                    wait: true,
                    propagate: true
            }
        }
    }
}
