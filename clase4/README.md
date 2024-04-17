Conexión de K3s y kubectl:
```
sudo systemctl status k3s.service
sudo chmod 644 /etc/rancher/k3s/k3s.yaml
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

kubectl get pods
```


### 1. Security Context
Pod con privileged y escalation:
```
kubectl apply -f pod-privileged.yaml

kubectl get pods
kubectl exec -ti pod-privileged bash
# id
# apt update && apt install kmod gcc make git linux-headers-$(uname -r) -y

# git clone https://github.com/mario21ic/linux-kernel-module-demo
# cd linux-kernel-module-demo/
# make all
# insmod lkm_example.ko
# dmesg | tail # Debe verse el mensaje de Hello

# lsmod |grep lkm_example

# cat /proc/1/status |grep Cap
# exit
```
Deben ser asi:
CapPrm:	000001ffffffffff
CapEff:	000001ffffffffff

Revisar en host:
```
sudo lsmod | grep example
```

Pod con capabilities para network:
```
$ kubectl apply -f pod-capabilities.yaml
$ kubectl get pods
$ kubectl exec -ti pod-capabilities sh
# id
# ip link add dummy01 type dummy
# ip link show
# cat /proc/1/status |grep Cap
# exit
```
Deben ser asi:
CapPrm: 00000000aa0435fb
CapEff: 00000000aa0435fb
# exit


Nota: si desean ver los valores y su combinacion https://github.com/torvalds/linux/blob/master/include/uapi/linux/capability.h 



### 2. Resource Quotas
```
kubectl delete namespace my-namespace 
kubectl create namespace my-namespace 
kubectl config set-context --current --namespace=my-namespace

kubectl apply -f resource-quota.yaml
kubectl get resourcequota -n my-namespace
```
Debe salir:
my-resource-quota   13s   pods: 0/5, requests.cpu: 0/2, requests.memory: 0/1Gi   limits.cpu: 0/4, limits.memory: 0/2Gi

Intentar desplegar un pod:
```
kubectl apply -f pod-illegal.yaml
```
Error from server (Forbidden): error when creating "pod-illegal.yaml": pods "pod-illegal" is forbidden: failed quota: my-resource-quota: must specify limits.cpu,limits.memory,requests.cpu,requests.memory


Crear Pod que pida recursos minimos:
```
kubectl apply -f pod-legal.yaml
kubectl get pods -n my-namespace
kubectl get resourcequota -n my-namespace
```
my-resource-quota   4m43s   pods: 1/5, requests.cpu: 500m/2, requests.memory: 768Mi/1Gi   limits.cpu: 2/4, limits.memory: 1Gi/2Gi

Probar otro pod:
```
$ kubectl apply -f pod-legal2.yaml
```
Debe salir error, porque 768+512 superan los 1024.


Probar Deployment:
```
kubectl apply -f deploy.yaml
kubectl get pods -n my-namespace
kubectl get deploy -n my-namespace
```

Revisando el status de un Resource Quota:
```
$ kubectl describe resourcequota -n my-namespace
```
Name:            my-resource-quota
Namespace:       my-namespace
Resource         Used   Hard
--------         ----   ----
limits.cpu       2      4
limits.memory    1Gi    2Gi
pods             1      5
requests.cpu     500m   2
requests.memory  768Mi  1Gi


Ejercicio:
* Crear namespace "qaenv", desplegar ahi Voting app, limitar el numero de Pods a 7, limitar el numero de Services a 4, intentar escalar a 2 replicas votos y results.
* Crear namespace "staging", desplegar ahi Voting app, limitar el numero de Pods a 14, limitar el numero de Services a 4, intentar escalar replicas votos=4 results=4 worker=4.

### 3. RBAC
Service accounts:
```
kubectl config set-context --current --namespace=my-namespace

kubectl get serviceaccount
kubectl get serviceaccount -A
kubectl get serviceaccount --all-namespaces|grep default
```

Desplegando pod con sa default:
```
kubectl apply -f pod-default.yaml
kubectl get pods
kubectl get svc

NAME         TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   ClusterIP   10.96.0.1    <none>        443/TCP   29m
```
Ese es el service del Kubernetes Api interno.

Verificar su service account
```
$ kubectl describe pod pod-default|grep -i "account"
/var/run/secrets/kubernetes.io/serviceaccount from default-token-tmqxh (ro)

$ kubectl exec -ti pod-default sh
# ls -la /var/run/secrets/kubernetes.io/serviceaccount/
# cat /var/run/secrets/kubernetes.io/serviceaccount/namespace
```

Preguntando al Kubernetes service como usuario anonimo:
```
# apk add --update curl
# curl https://kubernetes/api/v1 --insecure
```
Debe salir error por ser usuario anonimo.

Obteniendo Token del Service Account default:
```
# TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
# curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/ --insecure
```
Debe funcionar sin problemas y listar.

Listar Pods:
```
# curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/namespaces/default/pods/ --insecure
# exit
```
Debe salir error porque el service account default no tiene permisos de listar Pods.

#### Crear Service Account + Role + Role Binding
Crear un SA:
```
$ kubectl apply -f service-account.yaml
$ kubectl get sa
```

Crear Role
```
$ kubectl apply -f role.yaml
$ kubectl get role
```


Crear Role binding para listar pods:
```
$ kubectl apply -f role-binding.yaml
$ kubectl get rolebinding

$ kubectl get role,sa,rolebinding
```

Crear Pod usando el SA anterior:
```
$ kubectl apply -f pod-sa.yaml
$ kubectl get pods
```

Verificar que use el service account demo-sa
```
$ kubectl describe pods pod-demo-sa | grep -i "account"
Service Account:  demo-sa

$ kubectl exec -ti pod-demo-sa sh
# ls -la /var/run/secrets/kubernetes.io/serviceaccount/
# cat /var/run/secrets/kubernetes.io/serviceaccount/namespace
```

Preguntando al kubernetes service:
```
# apk add --update curl
# curl https://kubernetes/api/v1 --insecure
```
Debe salir error por ser usuario anonimo.

Obteniendo token del service account default:
```
# TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
# curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/ --insecure
# curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/namespaces/default/pods/ --insecure
```
En ambos casos debe funcionar sin problemas y listar correctamente.

Probando en listar services:
```
$ curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/namespaces/default/services/ --insecure
```
Debe salir error porque el role es solo para listar pods mas no services.



### 4. Resource Ingress + TLS
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

Probando:
```
curl -H "Host: hello-world.info" http://<ip-server>
```
