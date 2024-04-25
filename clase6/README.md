# Clase 6

## 0. Pre-requisito
```
# K3s
curl -sfL https://get.k3s.io | sh -
sudo k3s kubectl get node
sudo systemctl stop k3s

# Kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
kubectl version --client


# Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64
minikube start
minikube status
kubectl get nodes

# Docker
curl -sSL https://get.docker.com | sh
sudo usermod -aG docker $USER
sudo su - $USER
docker info
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

cd gvisor/
kubectl apply -f  pod.yaml
kubectl exec -ti nginx-untrusted sh
# dmesg
```
Mas info https://github.com/kubernetes/minikube/blob/master/deploy/addons/gvisor/README.md


## 2. Immutability of Containers at Runtime

### a) Regresando a K3s
Poner minikube stop y start K3S
```
minikube stop
sudo systemctl start k3s

sudo cp /etc/rancher/k3s/k3s.yaml $HOME/.kube/config
export KUBECONFIG=$HOME/.kube/config
sudo chown $USER:$USER $KUBECONFIG
kubectl get nodes
```

### b) Probando cada level
```
cd immutability/

kubectl apply -f 1-immutability.yaml
kubectl exec -ti immutability-pod1 -- sh
# mkdir /demo
# mkdir /var/cache/nginx/demo
# mkdir /var/run/demo
# exit


kubectl apply -f 2-immutability.yaml
kubectl exec -ti immutability-pod2 -- sh
$ id
$ mkdir /demo
$ mkdir /var/cache/nginx/demo
$ ls -la /var/cache/nginx/
$ exit


kubectl apply -f 3-immutability.yaml
kubectl exec -ti immutability-pod3 -- sh
$ id
$ ifconfig
$ mkdir /demo
$ mkdir /var/cache/nginx/demo
$ ls -la /var/cache/nginx/
$ exit
```


## 3. KubeArmor

### a) Instalacion y CLI
```
helm repo add kubearmor https://kubearmor.github.io/charts
helm repo update kubearmor

helm upgrade --install kubearmor-operator kubearmor/kubearmor-operator -n kubearmor --create-namespace
kubectl apply -f https://raw.githubusercontent.com/kubearmor/KubeArmor/main/pkg/KubeArmorOperator/config/samples/sample-config.yml
helm list -A
kubectl get all -n kubearmor

# Install CLI
curl -sfL http://get.kubearmor.io/ | sudo sh -s -- -b /usr/local/bin
karmor version
```

### b) Desplegar apps y probar:
```
cd kubearmor/

kubectl apply -f deploy-java.yaml
kubectl apply -f deploy-nginx.yaml

kubectl get deploy,pods

kubectl logs -l app=java
kubectl exec -ti $(kubectl get pod -l app=java -o jsonpath="{.items[0].metadata.name}") bash
$ ps fax
$ curl localhost:8080
$ cat /vault/secrets/database.txt
$ exit

kubectl logs -l app=nginx
kubectl exec -ti $(kubectl get pod -l app=nginx -o jsonpath="{.items[0].metadata.name}") bash
# ps fax
# curl localhost
# cat /vault/secrets/database.txt
# exit
```

### c) Bloquear por default namespace "default" y aplicar Policies
```
kubectl annotate ns default kubearmor-file-posture=block --overwrite

kubectl apply -f allow-java.yaml
kubectl apply -f allow-nginx.yaml
kubectl get kubearmorpolicy
```

### d) Eliminar Java pods y probar
```
stern java-dp # opcional
kubectl delete pod $(kubectl get pod -l app=java -o jsonpath="{.items[0].metadata.name}")
kubectl logs -l app=java

kubectl exec -ti $(kubectl get pod -l app=java -o jsonpath="{.items[0].metadata.name}") bash
$ ps fax
$ curl localhost:8080
$ cat /vault/secrets/database.txt
$ exit
```

### e) Eliminar Nginx pods y probar
```
stern nginx-dp # opcional
kubectl delete pod $(kubectl get pod -l app=nginx -o jsonpath="{.items[0].metadata.name}")
kubectl logs -l app=nginx

kubectl exec -ti $(kubectl get pod -l app=nginx -o jsonpath="{.items[0].metadata.name}") bash
$ ps fax
$ curl localhost
$ cat /vault/secrets/database.txt
$ exit
```

Opcional: Abriendo port
```
kubectl port-forward $(kubectl get pod -l app=java -o jsonpath="{.items[0].metadata.name}") 8080:8080 --address="0.0.0.0"
```
Navegador http://localhost:8080

### f) Copiando mediante kubectl cp
```
kubectl cp $(kubectl get pod -l app=java -o jsonpath="{.items[0].metadata.name}"):/vault/secrets/database.txt copiado.txt
cat copiado.txt
```
Nota: en esta parte se debe pensar en User Accounts con permisos y Audit Logs.

### g) Obteniendo recomendaciones
```
karmor recommend -n default
```


### g) Probando Modo Audit y default
```
kubectl annotate ns default kubearmor-file-posture=audit --overwrite
karmor logs -n default
kubectl exec -ti $(kubectl get pod -l app=nginx -o jsonpath="{.items[0].metadata.name}") cat /vault/secrets/database.txt

# Permitir todo por default
kubectl annotate ns default kubearmor-file-posture=allow --overwrite
```


## 4. Mutual TLS

### a) Instalación
```
curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/install-edge | sh
export PATH=$HOME/.linkerd2/bin:$PATH
linkerd version

linkerd check --pre

linkerd install --crds | kubectl apply -f -
linkerd install | kubectl apply -f -

linkerd check
```

### b) App Demo Emojivoto
```
# Demo app emojivoto
curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/emojivoto.yml | kubectl apply -f -
kubectl -n emojivoto port-forward svc/web-svc 8080:80 --address="0.0.0.0"
```
Navegador http://localhost:8080/

### c) Habilitamos linkerd para el App
```
kubectl get -n emojivoto deploy -o yaml \
  | linkerd inject - \
  | kubectl apply -f -

# verificar
linkerd -n emojivoto check --proxy
```

### d) Explorar Linkerd
```
linkerd viz install | kubectl apply -f - 
linkerd check

linkerd viz dashboard --address 0.0.0.0 &
```
Navegador http://localhost:50750/

### e) Desinstalando anteriores tools
```
helm uninstall kubearmor-operator -n kubearmor
helm uninstall vault
kubectl delete deploy web
kubectl delete deploy web2
kubectl delete deploy orgchart
kubectl delete deploy java-dp
kubectl delete deploy nginx-dp
```

### f) Validando mTLS con edged y tshark
```
linkerd viz -n linkerd edges deployment

curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/emojivoto.yml \
  | linkerd inject --enable-debug-sidecar - \
  | kubectl apply -f -

kubectl -n emojivoto exec -it \
    $(kubectl -n emojivoto get po -o name | grep voting) \
    -c linkerd-debug -- /bin/bash
tshark -i any -d tcp.port==8080,ssl | grep -v 127.0.0.1
```

Mas info:
* https://linkerd.io/2.15/tasks/validating-your-traffic/
* https://linkerd.io/2.15/features/automatic-mtls/
* https://linkerd.io/2.15/tasks/generate-certificates/
* https://buoyant.io/mtls-guide
* https://medium.com/@eshiett314/mutual-tls-with-emissary-ingress-and-linkerd-4aa3ffe0413f


## 5. Policy Management with Kyverno

### a) Instalacion
```
kubectl create -f https://github.com/kyverno/kyverno/releases/download/v1.11.1/install.yaml

kubectl get all -n kyverno

kubectl label namespace kyverno kyverno=disabled
kubectl label namespace kube-system kyverno=disabled
kubectl label namespace default kyverno=enabled
kubectl get namespace --show-labels
```

### b) Asegurando que los pods tengan labels
```
cd kyverno/

kubectl apply -f enforce-labels.yaml
kubectl apply -f test-labels.yaml

kubectl get clusterpolicy
```

### c) Asegurando que no se usen latest
```
kubectl apply -f enforce-no-latest.yaml
kubectl apply -f test-no-latest.yaml

kubectl get clusterpolicy
```

### d) Asegurando que solo venga de un registry valido
```
kubectl apply -f enforce-registry.yaml
kubectl apply -f test-registry.yaml

kubectl get clusterpolicy
kubectl get policyreport -o wide

kubectl describe policyreport
```

### e) Agregando labels por default
```
kubectl apply -f enforce-mutation.yaml
kubectl get clusterpolicy

kubectl run alpine --image alpine
kubectl get pod alpine --show-labels
```


## 6. Threat Detection with Falco
```
helm repo add falcosecurity https://falcosecurity.github.io/charts
helm repo update

kubectl create namespace falco
helm install falco -n falco --create-namespace --set driver.kind=ebpf --set tty=true falcosecurity/falco \
    --set falcosidekick.enabled=true \
    --set falcosidekick.config.slack.webhookurl=$(base64 --decode <<< "aHR0cHM6Ly9ob29rcy5zbGFjay5jb20vc2VydmljZXMvVDA0QUhTRktMTTgvQjA1SzA3NkgyNlMvV2ZHRGQ5MFFDcENwNnFzNmFKNkV0dEg4") \
    --set falcosidekick.config.slack.minimumpriority=notice \
    --set falcosidekick.config.customfields="user:sup3rUs3r"


kubectl get all -n falco -o wide
kubectl get pods -n falco -o wide

kubectl logs -f -l app.kubernetes.io/name=falco -n falco -c falco
```

### b) Detectando amenazas:
```
kubectl run alpine --image alpine -- sh -c "sleep infinity"
kubectl exec -it alpine -- sh -c "uptime"

kubectl exec -it alpine -- sh
# apk add tcpdump nmap vim
# tcpdump
# nmap localhost
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

