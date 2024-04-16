### 1. SCA - Dependency check
```
cd dependency_check
./scan.sh
```

### 2. SBOM - Syft
```
curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s -- -b /usr/local/bin

syft -q mario21ic/nginx:alpine

syft -q alpine
syft -q ubuntu:22.04
```

### 3. Loki
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

### 4. Debugging Pods
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

### 5. Debugging Nodes
Ejecutar un ssh simulado
```
kubectl get nodes
kubectl debug node/vm-krowdyuser -ti --image=ubuntu:22.04

# ps fax
# apt update && apt install htop -y
# htop
```

### 6. Security Context
```
```

### 7. Resource Quotas
```
```

### 8. RBAC
```
```

### 9. Resource Ingress + TLS
Generamos certificados:
```
openssl req -newkey rsa:2048 -nodes -keyout tls1.key -x509 -days 365 -out tls1.crt -subj '/CN=kubapp1'
openssl req -newkey rsa:2048 -nodes -keyout tls2.key -x509 -days 365 -out tls2.crt -subj '/CN=kubapp2'
```

Codificamos en base64:
```
cat tls1.key | base64
cat tls1.crt | base64

cat tls2.key | base64
cat tls2.crt | base64
```
Con lo obtenido actualizamos el archivo secret-tls.yaml

Instalamos:
```
kubectl apply -f web1.yaml
kubectl apply -f web2.yaml

kubectl apply -f secret-tls.yaml
kubectl apply -f ingress.yaml
```