@Library('ratcheting') _
node {
    def server = Artifactory.server 'ART'
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo = Artifactory.newBuildInfo()
    
    stage ('Clone') {
        checkout scm
    }

    stage ('Artifactory configuration') {
        rtMaven.tool = 'GRADLE_TOOL' 
        rtGradle.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: server
        rtGradle.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: server
        buildInfo.env.capture = true
    }

    try{
        stage ('Exec Gradle') {
            rtGradle.run buildFile: 'build.gradle', tasks: 'build', buildInfo: buildInfo
        }
    } finally {
        /*
        junit 'target/surefire-reports/**/*.xml'
        checkstyle pattern: '**/target/checkstyle-result.xml'
        findbugs pattern: '**/target/spotbugsXml.xml'
        */
    }


    if (env.BRANCH_NAME == 'master') {
        stage ('Publish build info') {
            server.publishBuildInfo buildInfo
        }
    }
}
