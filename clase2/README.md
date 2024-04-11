### 1. Instalar K3S
```
curl -sfL https://get.k3s.io | sh -

sudo k3s kubectl get node

cat /etc/rancher/k3s/k3s.yaml
ls -la /etc/rancher/k3s/k3s.yaml
sudo chmod 644 /etc/rancher/k3s/k3s.yaml
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
```

Cliente:
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
kubectl get nodes
```

#### Opcional: K3s worker
En master leer token:
```
cat /var/lib/rancher/k3s/server/token
```

En worker:
```
curl -sfL https://get.k3s.io | K3S_URL=https://<ip-master>:6443 K3S_TOKEN=<token-master> sh -
```

En master verificar:
```
sudo k3s kubectl get node
```


### 2. Voting App
```
git clone https://github.com/mario21ic/example-voting-app

cd example-voting-app/k8s-specifications/
sudo k3s kubectl apply -f ./
```
Votos: http://<ip servidor>:3100
Resultados: http://<ip servidor>:3101


### 3. Linters
a) Kubeval
```
wget https://github.com/instrumenta/kubeval/releases/latest/download/kubeval-linux-amd64.tar.gz
tar xf kubeval-linux-amd64.tar.gz

git clone 
./kubeval *.yaml
```

b) KubeLinter
```
docker run --rm -v $PWD:/dir stackrox/kube-linter lint /dir
```

c) Kube Score
```
docker run -v $(pwd):/project zegl/kube-score:latest score *.yaml
```


### 4. Helm
```
sudo snap install helm --classic
helm version
```


### 5. Prometheus
```
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm search repo prometheus

helm install myprom prometheus-community/prometheus -f prom-values.yaml
helm list
```

Acceso:
```
kubectl get daemonset,svc,deploy,pods

kubectl port-forward myprom-prometheus-server-xxxx 9090 --address="0.0.0.0"
```
Navegar http://localhost:9090/targets


### 6. Grafana
```
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm search repo grafana

helm install mygraf grafana/grafana
helm list
```

Obtener clave y acceso:
```
kubectl get secret mygraf-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
kubectl port-forward mygraf-grafana-b8449cdb6-tkh6j 3000 --address="0.0.0.0"
```
Navegar http://localhost:3000/

Prometheus + Grafana
* En http://myprom-prometheus-server como DataSource http://localhost:3000/connections/datasources
* Importar http://localhost:3000/dashboard/import codigo 15282 y boton Load
* Buscar dashboards https://grafana.com/grafana/dashboards/

### 7. Loki
```
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm search repo loki

helm install myloki grafana/loki-stack
helm list
```

Loki + Grafana:
* En Grafana agregar datasource Loki http://localhost:3000/connections/datasources/new con URL http://myloki:3100
* En Grafana http://localhost:3000/explore poner filtros:
app = loki
app = vote
app = result

* Generar carga y validar logs en Grafana:
```
$ for i in {1..10}; do curl -I 192.168.20.21:31000; done
```

### 8. Debugging Pods
a) Compartir Namespace:
```
kubectl run nginx --image=nginxinc/nginx-unprivileged
kubectl debug -ti nginx --image=ubuntu --target=nginx
# ps aux
# apt update && apt install -y tcpdump
# tcpdump -i any -nn port 80

kubectl port-forward pod/nginx 8080:80
curl localhost:8080
```

b) Copiar Proceso
```
kubectl run nginx2 --image=nginxinc/nginx-unprivileged
kubectl debug -ti nginx2 --image=ubuntu --share-processes --copy-to=nginx2-debug
# ps fax
$ kubectl get pods
kubectl describe pod nginx2
```
Pod nginx2 debe tener 2 containers, el nginx copiado y el debug

c) Copiar Pod failing
```
kubectl run --image=alpine myapp -- false
kubectl debug myapp -ti --copy-to=myapp-debug --container=myapp -- sh
```
Se reemplaza el comando original (false) por el nuevo (sh).
Usa la misma imagen del pod original.

### 9. Debugging Nodes
Ejecutar un ssh simulado
```
kubectl get nodes
kubectl debug node/vm-krowdyuser -ti --image=ubuntu:22.04

# ps fax
# apt update && apt install htop -y
# htop
```
