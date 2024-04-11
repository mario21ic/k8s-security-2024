Kubernetes Privileged and Capabilities
Crear archivo pod-privileged.yaml con contenido:
apiVersion: v1
kind: Pod
metadata:
 name: pod-privileged
spec:
 containers:
 - name: sec-ctx-3
   image: ubuntu:18.04
   command: [ "sh", "-c", "sleep 1h" ]
   securityContext:
     allowPrivilegeEscalation: true
$ kubectl apply -f pod-privileged.yaml
$ kubectl get pods
$ kubectl exec -ti pod-privileged bash
# id
# apt update && apt install kmod -y
# lsmod |grep lkm_example
# cat /proc/1/status |grep Cap
Deben ser asi:
CapPrm: 00000000a80425fb
CapEff: 00000000a80425fb
# exit

Crear archivo pod-capabilities.yaml con contenido:
apiVersion: v1
kind: Pod
metadata:
  name: pod-capabilities
spec:
  containers:
  - name: sec-ctx-4
    image: busybox:latest
    command: [ "sh", "-c", "sleep 1h" ]
    securityContext:
      capabilities:
        add: ["NET_ADMIN", "SYS_TIME"]

$ kubectl apply -f pod-capabilities.yaml
$ kubectl get pods
$ kubectl exec -ti pod-capabilities sh
# id
# ip link add dummy01 type dummy
# ip link show
# cat /proc/1/status |grep Cap
Deben ser asi:
CapPrm: 00000000aa0435fb
CapEff: 00000000aa0435fb
# exit


Nota: si desean ver los valores y su combinacion https://github.com/torvalds/linux/blob/master/include/uapi/linux/capability.h 