node {
    def server = Artifactory.server 'ART'
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo = Artifactory.newBuildInfo()
    
    stage ('Clone') {
        checkout scm
    }

    stage ('Artifactory configuration') {
        rtGradle.tool = 'GRADLE_TOOL' 
        rtGradle.deployer repo: 'libs-release-local', server: server
        rtGradle.resolver repo: 'libs-release', server: server
        buildInfo.env.capture = true
    }


    stage ('Test') {
        rtGradle.run buildFile: 'build.gradle', tasks: 'test', buildInfo: buildInfo
    }

    stage ('Build Plugin') {
        rtGradle.run buildFile: 'build.gradle', tasks: 'buildPlugin', buildInfo: buildInfo
    }

    if (env.BRANCH_NAME == 'master') {
        stage ('Publish build info') {
            def uploadSpec = """
                        {
                         "files": [
                            {
                              "pattern": "build/distributions/*.zip",
                              "target": "generic/celesta-intellij-plugin/${currentBuild.number}/"
                            }
                            ]
                        }"""
            buildInfo = server.upload spec: uploadSpec
            server.publishBuildInfo buildInfo
        }
    }
}
