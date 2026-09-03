pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timeout(time: 10, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        string(name: 'SOURCE_BUILD', defaultValue: '',
               description: 'Номер сборки Job_2 с архивом проекта')
    }

    stages {
        stage('Receive project from Job_2') {
            steps {
                script {
                    if (!(params.SOURCE_BUILD ==~ /[1-9][0-9]*/)) {
                        error('Нужен номер сборки Job_2 в SOURCE_BUILD')
                    }
                }
                deleteDir()
                copyArtifacts(
                    projectName: 'Job_2',
                    selector: specific(params.SOURCE_BUILD),
                    filter: 'project-after-cleanup.tar.gz',
                    fingerprintArtifacts: true
                )
                sh 'mkdir project && tar -xzf project-after-cleanup.tar.gz -C project'
            }
        }

        stage('Commit and push to master') {
            steps {
                dir('project') {
                    sshagent(credentials: ['github-ssh']) {
                        sh '''
                            set -eu
                            git config user.name "Jenkins"
                            git config user.email "jenkins@localhost"
                            git add -u

                            if git diff --cached --quiet; then
                                echo "No changes: commit and push are not required."
                            else
                                git diff --cached --stat
                                git commit -m "ci: remove test files"
                                GIT_SSH_COMMAND='ssh -o BatchMode=yes -o StrictHostKeyChecking=yes' git push origin HEAD:master
                            fi
                        '''
                    }
                }
            }
        }
    }
}
