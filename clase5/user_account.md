
User Account:
$ kubectl config current-context
# Debe mostrarnos minikube como valor unico.

Creando certificados para user1
$ openssl genrsa -out user1.key 2048
$ openssl req -new -key user1.key -out user1.csr -subj "/CN=user1/O=group1"
$ ls -la user1.*
$ ls -la /home/tuxito/.minikube/ca.*
$ openssl x509 -req -in user1.csr -CA ~/.minikube/ca.crt -CAkey ~/.minikube/ca.key -CAcreateserial -out user1.crt -days 500
$ ls -la user1.*

Crear user1:
$ kubectl config set-credentials user1 --client-certificate=user1.crt --client-key=user1.key
$ kubectl config set-context user1-context --cluster=minikube --user=user1

Revisar la config:
$ kubectl config view
$ ls -la ~/.kube/config

Cambiar de context:
$ kubectl config use-context user1-context
$ kubectl config current-context
$ kubectl get pods
Debe salir error de permisos porque aun no tiene roles asignados.

Creando RBAC:
$ kubectl config use-context minikube
Crear archivo ua-role.yaml 
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: default
  name: pod-reader
rules:
- apiGroups: [""] # "" indicates the core API group
  resources: ["pods"]
  verbs: ["get", "watch", "list"]

$ kubectl apply -f ua-role.yaml

Crear archivo ua-rolebinding.yaml
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: read-pods
  namespace: default
subjects:
- kind: User
  name: user1 # Name is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role #this must be Role or ClusterRole
  name: pod-reader # must match the name of the Role
  apiGroup: rbac.authorization.k8s.io


$ kubectl apply -f ua-rolebinding.yaml
$ kubectl get role,rolebinding

Probando permisos:
$ kubectl config use-context user1-context
$ kubectl get pods
$ kubectl get pods -w
Ahora si debe permitir listar los pods
$ kubectl get deploy
Debe dar error porque no tiene permisos para listar deployments.
Error from server (Forbidden): deployments.apps is forbidden: User "user1" cannot list resource "deployments" in API group "apps" in the namespace "default"

Nota: Por si sale error en minikube, eliminar y volver a crear:
$ minikube delete
$ rm -rf ~/.minikube
$ minikube start
$ minikube status

Dinamica:
Puedan eliminar los pods $ kubectl delete pods --all
Solucion: cambiar al context minikube y agregar “delete” en verbs del ua-role.yaml.
Darle permisos al user1 de listar deployments
Darle permisos al user1 de poder ejecutar $ kubectl get services

Archivo ua-role2.yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: default
  name: deployment-reader
rules:
- apiGroups: ["apps"] # "" indicates the core API group
  resources: ["deployments"]
  verbs: ["get", "watch", "list"]

Archivo ua-rolebinding2.yaml
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: read-deployments
  namespace: default
subjects:
- kind: User
  name: user1 # Name is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role #this must be Role or ClusterRole
  name: deployment-reader # must match the name of the Role
  apiGroup: rbac.authorization.k8s.io
$ kubectl config use-context minikube
$ kubectl apply -f ua-role2.yaml
$ kubectl apply -f ua-rolebinding2.yaml
Probar:
$ kubectl config use-context user1-context
$ kubectl get deploy
