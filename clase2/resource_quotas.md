Resource quotas:
$ kubectl delete namespace my-namespace 
$ kubectl create namespace my-namespace 
$ kubectl config set-context --current --namespace=my-namespace
Crea archivo resource-quota.yaml con contendo:
kind: ResourceQuota  
apiVersion: v1  
metadata:
 name: my-resource-quota
 namespace: my-namespace
spec:
 hard:
   pods: 5
   "requests.cpu": "2"
   "requests.memory": 1024Mi
   "limits.cpu": "4"
   "limits.memory": 2048Mi
$ kubectl apply -f resource-quota.yaml
$ kubectl get resourcequota -n my-namespace
my-resource-quota   13s   pods: 0/5, requests.cpu: 0/2, requests.memory: 0/1Gi   limits.cpu: 0/4, limits.memory: 0/2Gi

Pod Illegal
Crea archivo pod-illegal.yaml
kind: Pod 
apiVersion: v1 
metadata:
  name: pod-illegal
  namespace: my-namespace
spec:
  containers:
    - name: nginx-illegal 
      image: nginx:alpine
$ kubectl apply pod-illegal.yaml
Error from server (Forbidden): error when creating "pod-illegal.yaml": pods "pod-illegal" is forbidden: failed quota: my-resource-quota: must specify limits.cpu,limits.memory,requests.cpu,requests.memory


Pod con request y limits:
Crear archivo pod-legal.yaml con contenido:
kind: Pod  
apiVersion: v1  
metadata:
 name: pod-one
 namespace: my-namespace
spec:  
 containers:
   - name: nginx-pod-one  
     image: nginx:alpine
     resources:
       requests:
         memory: 768Mi
         cpu: "0.5"
       limits:
         memory: 1024Mi  
         cpu: "2"

$ kubectl apply -f pod-legal.yaml
$ kubectl get pods -n my-namespace
$ kubectl get resourcequota -n my-namespace
my-resource-quota   4m43s   pods: 1/5, requests.cpu: 500m/2, requests.memory: 768Mi/1Gi   limits.cpu: 2/4, limits.memory: 1Gi/2Gi

Pod legal 2:
Crear archiv pod-legal2.yaml con contenido:
kind: Pod  
apiVersion: v1  
metadata:
 name: pod-two
 namespace: my-namespace
spec:  
 containers:
   - name: nginx-pod-two
     image: nginx:alpine
     resources:
       requests:
         memory: 512Mi
         cpu: "0.5"
       limits:
         memory: 1024Mi  
         cpu: "2"
$ kubectl apply -f pod-legal2.yaml
# Debe salir error, porque 768+512 superan los 1024.

Deploy legal:
Crear archivo deploy-legal.yaml
kind: Deployment
apiVersion: apps/v1
metadata:
  name: nginx-dp
  namespace: my-namespace
spec:
  selector:
    matchLabels:
      app: nginx
  replicas: 5
  template:
    metadata:
      labels:
        app: nginx
    spec:  
      containers:
      - name: nginx
        image: nginx:alpine
        resources:
          limits:
            memory: 0Mi  
            cpu: "0"

$ kubectl apply -f deploy-legal.yaml
$ kubectl get pods -n my-namespace
$ kubectl get deploy -n my-namespace

Revisando el status de un Resource Quota:
$ kubectl describe resourcequota -n my-namespace
Name:            my-resource-quota
Namespace:       my-namespace
Resource         Used   Hard
--------         ----   ----
limits.cpu       2      4
limits.memory    1Gi    2Gi
pods             1      5
requests.cpu     500m   2
requests.memory  768Mi  1Gi
