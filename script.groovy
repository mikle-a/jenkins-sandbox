def withPod(body) {
    podTemplate(label: 'pod', serviceAccount: 'jenkins', containers: [
            containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
            containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl', command: 'cat', ttyEnabled: true)
    ],
            volumes: [
                    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
            ]
    ) { body() }
}

withPod {
    node('pod') {
        def tag = "${env.BRANCH_NAME}.${env.BUILD_NUMBER}"
        def service = "market-data:${tag}"

        checkout scm

        container('docker') {

            stage('Build') {
                sh("docker build -t ${service} .")
            }

            stage('Test') {
                try {
                    sh("docker run -v `pwd`:/workspace --rm ${service} python setup.py test")
                } finally {
                    step([$class: 'JUnitResultArchiver', testResults: 'results.xml'])
                }
            }

            def tagToDeploy = "mikle-a/${service}"
            stage('Publish') {
                withDockerRegistry(registry: [credentialsId: 'dockerhub']) {
                    sh("docker tag ${service} ${tagToDeploy}")
                    sh("docker push ${tagToDeploy}")
                }
            }
        }
    }
}