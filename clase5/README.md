# Clase 5

## 0. Pre-requisito

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


## 1. RBAC - User Accounts

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

## 2. Secrets with Kubeseal

### a) Instalación:
Controller:
```
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.26.2/controller.yaml
kubectl get pods -n kube-system
```

Client:
```
KUBESEAL_VERSION='0.26.2'
wget "https://github.com/bitnami-labs/sealed-secrets/releases/download/v${KUBESEAL_VERSION:?}/kubeseal-${KUBESEAL_VERSION:?}-linux-amd64.tar.gz"
tar -xvzf kubeseal-${KUBESEAL_VERSION:?}-linux-amd64.tar.gz kubeseal
sudo install -m 755 kubeseal /usr/local/bin/kubeseal

kubeseal --version
```

### b) Crear Secret y SealedSecret
```
echo -n HelloWorld | kubectl create secret generic mysecret --dry-run=client --from-file=foo=/dev/stdin -o json > mysecret.json
cat mysecret.json

kubeseal < mysecret.json > mysealedsecret.json
cat mysealedsecret.json

kubectl apply -f mysealedsecret.json

kubectl get sealedsecret
kubectl describe sealedsecret mysecret

kubectl get secret
kubectl get secret mysecret -o yaml
```

### c) Backup y Restore del Master Key:
```
kubectl get secret -n kube-system -l sealedsecrets.bitnami.com/sealed-secrets-key -o yaml > master_key.yaml

kubectl apply -f master_key.yaml
# eliminar pod, para reiniciar y agarre el nuevo key
kubectl delete pod -n kube-system -l name=sealed-secrets-controller
```

## 3. Vault integration

### a) Instalación de Vault en K8s:
```
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update

helm install vault hashicorp/vault --set "server.dev.enabled=true"
helm list
kubectl get pods

# Opcional
kubectl port-forward vault-0 8200
```

### b) Crear un Vault secret:
```
kubectl exec -it vault-0 -- /bin/sh
$ vault secrets enable -path=internal kv-v2

$ vault kv put internal/database/config username="db-readonly-username" password="db-secret-password"
$ vault kv get internal/database/config
exit
```

### c) Crear Vault Auth para Kubernetes
```
kubectl exec -it vault-0 -- /bin/sh
vault auth enable kubernetes

vault write auth/kubernetes/config kubernetes_host="https://$KUBERNETES_PORT_443_TCP_ADDR:443"

# Crear politica solo de lectura
$ vault policy write internal-app - <<EOF
path "internal/data/database/config" {
   capabilities = ["read"]
}
EOF

# Solo para Namespace default
$ vault write auth/kubernetes/role/internal-app \
      bound_service_account_names=internal-app \
      bound_service_account_namespaces=default \
      policies=internal-app \
      ttl=24h

$ exit
```
TODO launch app, try different namespace, etc

### 4. Ingress + TLS

Paramos Minikube y reiniciar K3S:
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

### 4. Network Policies - Ingress / Egress
```
```
