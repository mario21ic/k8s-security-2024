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

Instalar Stern:
```
wget https://github.com/stern/stern/releases/download/v1.28.0/stern_1.28.0_linux_amd64.tar.gz
tar -xvf stern_1.28.0_linux_amd64.tar.gz
sudo mv stern /usr/local/bin

stern -n kube-system kube-proxy
```
Ctrl+c


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
$ kubectl config get-users

$ kubectl config set-credentials user1 --client-certificate=user1.crt --client-key=user1.key
$ kubectl config set-context user1-context --cluster=minikube --user=user1

$ kubectl config get-users
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
$ kubectl apply -f pod.yaml

$ kubectl get pods
$ kubectl get pods -w
```
Ahora si debe permitir listar los pods

Reto:
El user1 debe poder listar deployments y services. Ademas de eliminar pods.
```
$ kubectl get deploy,service
```
Debe dar error porque aun no tiene permisos para listar deployments.
Error from server (Forbidden): deployments.apps is forbidden: User "user1" cannot list resource "deployments" in API group "apps" in the namespace "default"

Solucion: ua-role2.yaml ua-rolebinding2.yaml


## 2. Secrets with Kubeseal

Poner minikube stop y start K3S
```
minikube stop
sudo systemctl start k3s

sudo cp /etc/rancher/k3s/k3s.yaml $HOME/.kube/config
export KUBECONFIG=$HOME/.kube/config
sudo chown $USER:$USER $KUBECONFIG
kubectl get nodes
```

### a) Instalación:
Controller:
```
kubectl config use-context minikube

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
```

# Opcional: Vault dashboard
Para obtener clave y lanzar web ui:
```
kubectl logs vault-0
kubectl port-forward vault-0 8200
```
Abrir en navegador: http://localhost:8200/ui/


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

Crear K8s Service Account
```
kubectl apply -f service-account.yaml
kubectl get serviceaccounts
```

### c) Verificar Vault Secrets
```
kubectl apply -f deployment-orgchart.yaml
kubectl logs -l app=orgchart

# Opcional
stern orgchart

kubectl exec \
      $(kubectl get pod -l app=orgchart -o jsonpath="{.items[0].metadata.name}") \
      --container orgchart -- ls /vault/secrets
kubectl exec \
      $(kubectl get pod -l app=orgchart -o jsonpath="{.items[0].metadata.name}") \
      --container orgchart -- cat /vault/secrets/database-config.txt
```

Ejercicio:
* Lanzar deployment en otro namespace y validar si funciona.
* Fixear el issue anterior
* Usar template para secrets
Mas info https://developer.hashicorp.com/vault/tutorials/kubernetes/kubernetes-sidecar


## 4. Ingress + TLS

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


## 5. Network Policies - Ingress / Egress

#### a) Instalando curl y probando accesos desde Redis:
```
kubectl get pods --show-labels

kubectl exec -ti $(kubectl get pod -l app=redis -o jsonpath="{.items[0].metadata.name}") -- sh
# apk add curl busybox-extras
# curl vote:5000
# curl result:5001
# telnet db 5432
Ctrl + C
e
```

#### b) Protegiendo Redis y probando desde Redis, DB, Vote, Worker y Result:
```
kubectl -f apply np-redis.yaml
kubectl get networkpolicies

kubectl exec -ti $(kubectl get pod -l app=redis -o jsonpath="{.items[0].metadata.name}") -- sh
# apk add git
# curl vote:5000
# curl result:5001
# telnet db 5432
# exit

kubectl exec -ti $(kubectl get pod -l app=db -o jsonpath="{.items[0].metadata.name}") -- bash
# apk add curl busybox-extras
# telnet redis 6379
# exit

kubectl exec -ti $(kubectl get pod -l app=vote -o jsonpath="{.items[0].metadata.name}") -- bash
# apt update && apt install curl dnsutils telnet -y
# telnet redis 6379
quit
# exit

kubectl exec -ti $(kubectl get pod -l app=worker -o jsonpath="{.items[0].metadata.name}") -- bash
# apt update && apt install curl dnsutils telnet -y
# telnet redis 6379
quit
# exit

kubectl exec -ti $(kubectl get pod -l app=result -o jsonpath="{.items[0].metadata.name}") -- bash
# apt update && apt install curl dnsutils telnet -y
# telnet redis 6379
# exit
```

#### c) Aplicar a todos los componentes y probar:
```
kubectl apply -f np-db.yaml
kubectl apply -f np-vote-deny.yaml
kubectl apply -f np-worker-deny.yaml
kubectl apply -f np-result-deny.yaml
kubectl get networkpolicies

kubectl exec -ti $(kubectl get pod -l app=db -o jsonpath="{.items[0].metadata.name}") -- bash
# apk add git
# curl vote:5000
# curl result:5001
# exit

kubectl exec -ti $(kubectl get pod -l app=vote -o jsonpath="{.items[0].metadata.name}") -- bash
# apt install git -y
# telnet db 5432
# telnet redis 6379
quit
# exit

kubectl exec -ti $(kubectl get pod -l app=worker -o jsonpath="{.items[0].metadata.name}") -- bash
# apt install git -y
# telnet db 5432
quit
# telnet redis 6379
quit
# exit

kubectl exec -ti $(kubectl get pod -l app=result -o jsonpath="{.items[0].metadata.name}") -- bash
# apt install git -y
# telnet redis 6379
# telnet db 5432
quit
# exit
```

## 6. Audit Logs

#### a) Configurar K3s agent
```
cp policy.yaml /var/lib/rancher/k3s/server/manifests/policy.yaml
sudo cp /etc/systemd/system/k3s.service /etc/systemd/system/k3s.service.bkp

sudo nano /etc/systemd/system/k3s.service
ExecStart=/usr/local/bin/k3s \
    server \
    '--kube-apiserver-arg=tls-min-version=VersionTLS12' \
    '--kube-apiserver-arg=audit-policy-file=/var/lib/rancher/k3s/server/manifests/policy.yaml' \
    '--kube-apiserver-arg=audit-log-path=/var/log/kubernetes/audit/audit.log' \
    '--kube-apiserver-arg=audit-log-maxsize=3' \
    '--kube-apiserver-arg=audit-log-maxbackup=10' \
    '--kube-apiserver-arg=audit-log-maxage=7' \
	'--kubelet-arg' \
	'container-log-max-files=3' \
	'--kubelet-arg' \
	'container-log-max-size=10Mi' \

sudo systemctl daemon-reload
sudo systemctl restart k3s
```

#### b) Probando
```
tail -f /var/log/kubernetes/audit/audit.log

kubectl apply -f pod.yaml
kubectl apply -f configmap.yaml
kubectl apply -f deploy.yaml
```
