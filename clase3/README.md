Conexión de K3s y kubectl:
```
sudo systemctl status k3s.service
sudo chmod 644 /etc/rancher/k3s/k3s.yaml
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

kubectl get pods
```

Opcional: Instalar Kubecolor
```
https://github.com/hidetatz/kubecolor/releases/download/v0.0.25/kubecolor_0.0.25_Linux_x86_64.tar.gz
tar -xvf kubecolor_0.0.25_Linux_x86_64.tar.gz
sudo mv kubecolor /usr/local/bin/

alias kubectl=kubecolor
alias k=kubecolor

k get pods
```

### 1. SCA - Dependency check
```
cd dependency_check
./scan.sh
```

Opcional: Usando cli:
```
wget https://github.com/jeremylong/DependencyCheck/releases/download/v9.1.0/dependency-check-9.1.0-release.zip
unzip dependency-check-9.1.0-release.zip
./bin/dependency-check.sh --out ./reports --scan /ruta/a/escanear/
```

### 2. SBOM - Syft
```
curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sudo sh -s -- -b /usr/local/bin

syft -q mario21ic/nginx:alpine

syft -q alpine
syft -q ubuntu:22.04
```

Probar con htop y alpine:
```
cd clase1/dockerfiles
docker build -t mario21ic/myhtop ./

syft -q alpine > alpine.txt
syft -q mario21ic/myhtop > myhtop.txt
diff --color alpine.txt myhtop.txt
```


### 3. Loki
```
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm search repo loki

helm install myloki grafana/loki-stack
helm list
```

Obteniendo password y exponiendo Grafana:
```
kubectl get secret mygraf-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo

kubectl get pods | grep mygraf
kubectl port-forward mygraf-grafana-xxxx-xxx 3000 --address="0.0.0.0"
```
Navegar en http://ip-server:3000

Loki + Grafana:
* En Grafana agregar datasource Loki http://localhost:3000/connections/datasources/new con URL http://myloki:3100 y luego click en boton "Save & Test"
Nota seguro sale un falso negativo de que no conecta.

Volviendo a instalar Voting app:
```
cd clase2/example-voting-app/k8s-specifications
kubectl delete -f ./
kubectl apply -f ./
```

* En Grafana http://localhost:3000/explore poner filtros:
app = loki
app = vote
app = result

* Generar carga y validar logs en Grafana:
```
$ for i in {1..10}; do curl -I 192.168.20.21:31000; done
```

### 4. Debugging Pods
a) Compartir Namespace:
```
kubectl run nginx --image=nginxinc/nginx-unprivileged
kubectl debug -ti nginx --image=ubuntu --target=nginx
# ps aux
# apt update && apt install -y tcpdump
# tcpdump -i any -nn port 8080
```

En otra sesion de terminal:
```
kubectl port-forward pod/nginx 8080:8080 --address="0.0.0.0"
```
Navegar al ip-server:8080 o hacer:
```
curl ip-server:8080
```

b) Copiar Proceso
```
kubectl run nginx2 --image=nginxinc/nginx-unprivileged
kubectl debug -ti nginx2 --image=ubuntu --share-processes --copy-to=nginx2-debug
# ps fax
$ kubectl get pods
kubectl describe pod nginx2-debug
```
Pod nginx2 debe tener 2 containers, el nginx copiado y el debug

c) Copiar Pod failing
```
kubectl run --image=alpine myapp -- false
kubectl debug myapp -ti --copy-to=myapp-debug --container=myapp -- sh
```
Se reemplaza el comando original (false) por el nuevo (sh).
Usa la misma imagen del pod original.

### 5. Debugging Nodes
Ejecutar un ssh simulado
```
kubectl get nodes
kubectl debug node/vm-krowdyuser -ti --image=ubuntu:22.04

# ps fax
# apt update && apt install htop -y
# htop
```