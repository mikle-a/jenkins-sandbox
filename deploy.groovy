def toKubernetes(tagToDeploy, namespace, deploymentName) {
    sh("sed -i.bak 's#BUILD_TAG#${tagToDeploy}#' ./deploy/${namespace}/*.yml")

    kubectl(namespace,  "apply -f deploy/${namespace}/")
}

def kubectl(namespace, command) {
    container('kubectl') {
        sh("kubectl --namespace=${namespace} ${command}")
    }
}

def rollback(deploymentName) {
    kubectl("rollout undo deployment/${deploymentName}")
}

return this