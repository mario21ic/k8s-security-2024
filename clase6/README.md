# Clase 6

## 0. Pre-requisito
```
```

## 1. Secured Runtime Sandbox with gVisor

### a) Install gVisor/runsc
```
(
  set -e
  ARCH=$(uname -m)
  URL=https://storage.googleapis.com/gvisor/releases/release/latest/${ARCH}
  wget ${URL}/runsc ${URL}/runsc.sha512 \
    ${URL}/containerd-shim-runsc-v1 ${URL}/containerd-shim-runsc-v1.sha512
  sha512sum -c runsc.sha512 \
    -c containerd-shim-runsc-v1.sha512
  rm -f *.sha512
  chmod a+rx runsc containerd-shim-runsc-v1
  sudo mv runsc containerd-shim-runsc-v1 /usr/local/bin
)

sudo runsc install
sudo systemctl restart docker
```

### b) Probando gVisor con Docker
```
docker run --runtime=runsc --rm hello-world

docker run --runtime=runsc --rm -it ubuntu:22.04 /bin/bash
# dmesg
```
Mas info https://gvisor.dev/docs/user_guide/quick_start/docker/

### c) Probando gVisor con Minikube
```
sudo systemctl stop k3s
export KUBECONFIG=$HOME/.kube/config

minikube start --container-runtime=containerd  \
    --docker-opt containerd=/var/run/containerd/containerd.sock
minikube addons enable gvisor

kubectl get runtimeclass,pod gvisor -n kube-system

kuebctl apply -f  pod.yaml
kubectl exec -ti nginx-untrusted sh
# dmesg
```
Mas info https://github.com/kubernetes/minikube/blob/master/deploy/addons/gvisor/README.md


## 2. Immutability of Containers at Runtime
```

```

## 3. KubeArmor

### a) Instalacion
```
helm repo add kubearmor https://kubearmor.github.io/charts
helm repo update kubearmor

helm upgrade --install kubearmor-operator kubearmor/kubearmor-operator -n kubearmor --create-namespace
kubectl apply -f https://raw.githubusercontent.com/kubearmor/KubeArmor/main/pkg/KubeArmorOperator/config/samples/sample-config.yml

# Install CLI
curl -sfL http://get.kubearmor.io/ | sudo sh -s -- -b /usr/local/bin

karmor --version
```

### b) Probando
```
kubectl annotate ns default kubearmor-file-posture=block --overwrite

kubectl apply -f demo5.yaml
kubectl get kubearmorpolicy

kubectl apply -f job.yaml
kubectl logs -l job-name=nginx
```

## 4. Mutual TLS
```

curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/install-edge | sh

export PATH=$HOME/.linkerd2/bin:$PATH

https://linkerd.io/2.15/getting-started/

Browser: http://localhost:8080/

https://linkerd.io/2.15/features/automatic-mtls/

https://linkerd.io/2.15/tasks/validating-your-traffic/


https://linkerd.io/2.15/tasks/generate-certificates/


https://buoyant.io/mtls-guide


https://medium.com/@eshiett314/mutual-tls-with-emissary-ingress-and-linkerd-4aa3ffe0413f
```

## 5. Open Policy Agent (OPA)
```
wget https://github.com/open-policy-agent/opa/releases/download/v0.63.0/opa_linux_amd64
chmod +x opa_linux_amd64
./opa_linux_amd64 -s
```

## 6. Threat Detection with Falco
```
helm repo add falcosecurity https://falcosecurity.github.io/charts
helm repo update

kubectl create namespace falco
helm install falco -n falco --set driver.kind=ebpf --set tty=true falcosecurity/falco \
    --set falcosidekick.enabled=true \
    --set falcosidekick.config.slack.webhookurl=$(base64 --decode <<< "aHR0cHM6Ly9ob29rcy5zbGFjay5jb20vc2VydmljZXMvVDA0QUhTRktMTTgvQjA1SzA3NkgyNlMvV2ZHRGQ5MFFDcENwNnFzNmFKNkV0dEg4") \
    --set falcosidekick.config.slack.minimumpriority=notice \
    --set falcosidekick.config.customfields="user:changeme"


kubectl get all -n falco -o wide
kubectl get pods -n falco -o wide

kubectl logs -f -l app.kubernetes.io/name=falco -n falco -c falco
```

### b) Detectando amenazas:
```
kubectl run alpine --image alpine -- sh -c "sleep infinity"
kubectl exec -it alpine -- sh -c "uptime"
kubectl exec -it alpine -- sh
# apk add vim
# vim /etc/passwd

kubectl logs -l app.kubernetes.io/name=falco -n falco -c falco | grep Notice
```

## 7. CIS K8s Benchmark
```
git clone https://github.com/aquasecurity/kube-bench
cd kube-bench
kubectl apply -f job.yaml

kubectl get pods
kubectl logs -l app=kube-bench
```

