#!groovy

@Library('dependency-track')

def dockerRepository = 'https://docker-de.artifacts.dbccloud.dk'
def workerNode = 'devel12'

properties([
    disableConcurrentBuilds()
])
if (env.BRANCH_NAME == 'master') {
    properties([
        pipelineTriggers([
            triggers: [
                [
                    $class: 'jenkins.triggers.ReverseBuildTrigger',
                    upstreamProjects: "Docker-payara6-bump-trigger", threshold: hudson.model.Result.SUCCESS
                ]
            ]
        ])
    ])
}
pipeline {
    agent { label workerNode }
    environment {
        MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
        DOCKER_PUSH_TAG = "${env.BUILD_NUMBER}"
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    tools {
        maven 'maven 3.9'
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
    }
    stages {
        stage("build") {
            steps {
                // Fail Early..
                script {
                    if (! env.BRANCH_NAME) {
                        currentBuild.result = Result.ABORTED
                        throw new hudson.AbortException('Job Started from non MultiBranch Build')
                    } else {
                        println(" Building BRANCH_NAME == ${BRANCH_NAME}")
                    }

                    def status = sh returnStatus: true, script:  """
                        rm -rf \$WORKSPACE/.repo
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo dependency:resolve dependency:resolve-plugins >/dev/null 2>&1 || true
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo clean
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo --fail-at-end install javadoc:aggregate -Dsurefire.useFile=false
                    """

                    junit testResults: '**/target/*-reports/TEST-*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc], unstableTotalAll:1

                    warnings consoleParsers: [
                         [parserName: "Java Compiler (javac)"],
                         [parserName: "JavaDoc Tool"]],
                         unstableTotalAll: "0",
                         failedTotalAll: "0"

                    if ( status != 0 ) {
                        currentBuild.result = Result.FAILURE
                    }
                }
            }
        }
        stage("sonarqube") {
            steps {
                ansiColor('xterm') {
                    withSonarQubeEnv(installationName: 'sonarqube.dbc.dk') {
                        script {
                            def status = 0

                            def sonarOptions = "-Dsonar.branch.name=${BRANCH_NAME}"
                            if (env.BRANCH_NAME != 'main') {
                                sonarOptions += " -Dsonar.newCode.referenceBranch=main"
                            }

                            // Do sonar via maven
                            status += sh returnStatus: true, script: """
                                mvn -B -Dmaven.repo.local=$WORKSPACE/.repo --no-transfer-progress $sonarOptions sonar:sonar
                            """

                            if (status != 0) {
                                error("build failed")
                            }
                        }
                    }
                }
            }
        }
        stage("docker") {
            steps {
                script {
                    def allDockerFiles = findFiles glob: '**/Dockerfile'
                    def dockerFiles = allDockerFiles.findAll { f -> f.path.endsWith("target/docker/Dockerfile") }
                    def version = readMavenPom().version

                    for (def f : dockerFiles) {
                        def dirName = f.path.take(f.path.length() - "target/docker/Dockerfile".length())
                        if ( dirName == '' )
                            dirName = '.'
                        dir(dirName) {
                            modulePom = readMavenPom file: 'pom.xml'
                            def projectArtifactId = modulePom.getArtifactId()
                            def imageName = "${projectArtifactId}-${version}".toLowerCase()
                            if (! env.CHANGE_BRANCH) {
                                imageLabel = env.BRANCH_NAME.toLowerCase()
                            } else {
                                imageLabel = env.CHANGE_BRANCH.toLowerCase()
                            }
                            if ( ! (imageLabel ==~ /master|trunk/) ) {
                                println("Using branch_name ${imageLabel}")
                                imageLabel = imageLabel.split(/\//)[-1]
                            } else {
                                println(" Using Master branch ${BRANCH_NAME}")
                                imageLabel = env.BUILD_NUMBER
                            }

                            println("In ${dirName} build ${projectArtifactId} as ${imageName}:$imageLabel")

                            def app = docker.build("$imageName:${imageLabel}", '--pull --no-cache --file target/docker/Dockerfile .')

                            if (currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
                                docker.withRegistry(dockerRepository, 'docker') {
                                    app.push()
                                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                                        app.push "latest"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        stage("supply-chain gate") {
            steps {
                script {
                    dependencyTrackGate(
                        projectBom:  'target/sbom-java.json',
                        projectTeam: 'de-team',
                        projectType: 'java'
                    )
                }
            }
        }
        stage("Update DIT") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                expression {
                    (currentBuild.result == null || currentBuild.result == 'SUCCESS') && env.BRANCH_NAME == 'master'
                }
            }
            steps {
                script {
                    dir("deploy") {
                        sh "set-new-version services/search/datawell-scan-service.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets ${DOCKER_PUSH_TAG} -b master"
                    }
                }
            }
        }
    }
    post {
        success {
            step([$class: 'JavadocArchiver', javadocDir: 'target/reports/apidocs', keepAll: false])
            archiveArtifacts artifacts: '**/target/*-jar-with-dependencies.jar', fingerprint: true
        }
    }
}
