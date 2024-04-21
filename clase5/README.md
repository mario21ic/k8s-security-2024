Poner stop K3S, instalar Docker y Minikube
```
sudo systemctl stop k3s

# Omitir si ya tienes docker
curl -sSL https://get.docker.com | sh
sudo usermod -aG docker $USER
sudo su - $USER
docker info

curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64
minikube start

export KUBECONFIG=$HOME/.kube/config
kubectl get nodes
```

### 1. RBAC - User Accounts



Creando certificados para user1
```
$ openssl genrsa -out user1.key 2048
$ openssl req -new -key user1.key -out user1.csr -subj "/CN=user1/O=group1"
$ ls -la ~/.minikube/ca.*

$ openssl x509 -req -in user1.csr -CA ~/.minikube/ca.crt -CAkey ~/.minikube/ca.key -CAcreateserial -out user1.crt -days 500

$ ls -la user1.*
```

Crear user1:
```
$ kubectl config set-credentials user1 --client-certificate=user1.crt --client-key=user1.key
$ kubectl config set-context user1-context --cluster=minikube --user=user1
```

Revisar la config:
```
$ kubectl config view
$ cat ~/.kube/config
```
Mirar seccion "users"

Cambiar de context:
```
$ kubectl config use-context user1-context
$ kubectl config current-context

$ kubectl get pods
```
Debe salir error de permisos porque aun NO tiene roles asignados.

Creando RBAC:
```
kubectl config use-context minikube
kubectl apply -f ua-role.yaml
```

Aplicar Rolebinding:
```
$ kubectl apply -f ua-rolebinding.yaml
$ kubectl get role,rolebinding
```

Probando permisos:
```
$ kubectl config use-context user1-context
$ kubectl get pods
$ kubectl get pods -w
Ahora si debe permitir listar los pods
```

Reto:
El user1 debe poder listar deployments y services. Ademas de eliminar pods.
```
$ kubectl get deploy,service
```
Debe dar error porque aun no tiene permisos para listar deployments.
Error from server (Forbidden): deployments.apps is forbidden: User "user1" cannot list resource "deployments" in API group "apps" in the namespace "default"

Solucion: ua-role2.yaml ua-rolebinding2.yaml


### 2. Resource Ingress + TLS

Paramos minikube y reiniciar k3s:
```
minikube stop

sudo systemctl start k3s
sudo chmod 644 /etc/rancher/k3s/k3s.yaml
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

kubectl get pods
```

Opcional: generamos certificados y codificamos en base64:
```
openssl req -newkey rsa:2048 -nodes -keyout tls1.key -x509 -days 365 -out tls1.crt -subj '/CN=kubapp1'
openssl req -newkey rsa:2048 -nodes -keyout tls2.key -x509 -days 365 -out tls2.crt -subj '/CN=kubapp2'

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

Probando:
```
curl --insecure -H "Host: hello-world-v2.info" https://ip-server
curl --insecure -H "Host: hello-world-v2.info" https://ip-server/v1

curl --insecure -H "Host: hello-world-v1.info" https://ip-server
```
Abrir las mismas URLs en un browser.


