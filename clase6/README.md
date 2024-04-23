# Clase 5

## 0. Pre-requisito
```
```

## 1. Secured Runtime Sandbox
```

```

## 2. Immutability of Containers at Runtime
```
```

## 3. Pod Mutual TLS
```
```

## 4. Open Policy Agent (OPA)
```
wget https://github.com/open-policy-agent/opa/releases/download/v0.63.0/opa_linux_amd64
chmod +x opa_linux_amd64
./opa_linux_amd64 -s
```

## 5. Threat Detection with Falco
```
helm repo add falcosecurity https://falcosecurity.github.io/charts
helm repo update

kubectl create namespace falco
helm install falco -n falco --set driver.kind=ebpf --set tty=true falcosecurity/falco \
--set falcosidekick.enabled=true \
--set falcosidekick.config.slack.webhookurl=$(base64 --decode <<< "aHR0cHM6Ly9ob29rcy5zbGFjay5jb20vc2VydmljZXMvVDA0QUhTRktMTTgvQjA1SzA3NkgyNlMvV2ZHRGQ5MFFDcENwNnFzNmFKNkV0dEg4") \
--set falcosidekick.config.slack.minimumpriority=notice \
--set falcosidekick.config.customfields="user:changeme"


kubectl get all -n falco -o wide
kubectl get pods -n falco -o wide

kubectl logs -f -l app.kubernetes.io/name=falco -n falco -c falco
```

### b) Detectando amenazas:
```
kubectl run alpine --image alpine -- sh -c "sleep infinity"
kubectl exec -it alpine -- sh -c "uptime"
kubectl exec -it alpine -- sh
# apk add vim
# vim /etc/passwd

kubectl logs -l app.kubernetes.io/name=falco -n falco -c falco | grep Notice
```

## 6. CIS K8s Benchmark
```
git clone https://github.com/aquasecurity/kube-bench
cd kube-bench
kubectl apply -f job.yaml

kubectl get pods
kubectl logs -l app=kube-bench
```

