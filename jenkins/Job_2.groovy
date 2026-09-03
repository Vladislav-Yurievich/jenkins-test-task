pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        copyArtifactPermission('Job_3')
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        string(name: 'SOURCE_BUILD', defaultValue: '',
               description: 'Номер сборки Job_1 с исходным проектом')
    }

    stages {
        stage('Receive and clean project') {
            agent any
            steps {
                script {
                    if (!(params.SOURCE_BUILD ==~ /[1-9][0-9]*/)) {
                        error('Нужен номер сборки Job_1 в SOURCE_BUILD')
                    }
                }

                deleteDir()

                copyArtifacts(
                    projectName: 'Job_1',
                    selector: specific(params.SOURCE_BUILD),
                    filter: 'project.tar.gz',
                    fingerprintArtifacts: true
                )

                sh '''
                    set -eu
                    mkdir project
                    tar -xzf project.tar.gz -C project
                    cd project
                    rm -f -- delete_me_1.txt delete_me_2.txt
                    git status --short
                    tar -czf ../project-after-cleanup.tar.gz .
                '''

                archiveArtifacts(
                    artifacts: 'project-after-cleanup.tar.gz',
                    fingerprint: true
                )
            }
        }

        stage('Run Job_3') {
            steps {
                build job: 'Job_3',
                    parameters: [
                        string(name: 'SOURCE_BUILD', value: env.BUILD_NUMBER)
                    ],
                    wait: true,
                    propagate: true
            }
        }
    }
}
