## 1. Instalación
```
curl -sSL https://get.docker.com | sh
sudo usermod -aG docker $USER
exit
```
Verificar:
```
docker info
docker ps
```


## 2. Container Example
```
docker run hello-world
```


## 3. Docker CLI - syntax
```
docker run --name myalpine alpine:latest echo "Hello from container"
```


### 4. Docker image Basic
```
cd dockerfiles
cat Dockerfile

docker build -t myhtop:latest -f Dockerfile ./
docker build -t myhtop -f Dockerfile ./

docker run -t myhtop htop

docker history myhtop
```


### 5. Docker image multistage
```
cd dockerfiles/
cat Dockerfile.multistage

docker build -t multistage -f Dockerfile.multistage ./
docker run multistage

docker history multistage
```


### 6. Docker image layers with Dive
```
wget https://github.com/wagoodman/dive/releases/download/v0.12.0/dive_0.12.0_linux_amd64.tar.gz
tar -xvf dive_0.12.0_linux_amd64.tar.gz

./dive multistage
```


### 7. Non-root containers - docker image
```
cd non-root

cat Dockerfile
docker build -t mynonroot ./

docker run -v /tmp:/tmp mynonroot touch /tmp/archivo1.txt
stat /tmp/archivo1.txt
```


### 8. Non-root containers - runtime
```
docker run --user 0 alpine id
docker run --user 1000:1000 alpine id

docker run --user $(id -u):$(id -g) -v /tmp:/tmp alpine touch /tmp/archivo2.txt
stat /tmp/archivo2.txt
```

### 9. Non-root containers - docker host
```
sudo nano /etc/docker/daemon.json
{
    "userns-remap": "krowdyuser"
}
sudo systemctl restart docker

docker run -v /tmp:/tmp alpine touch /tmp/archivo3.txt
stat /tmp/archivo3.txt
```


### 10. Port forwarding
```
docker run --name mynginx -p 8080:80 -d nginx:alpine
curl localhost:8080

docker logs -f mynginx
```

### 11. Scanning with Trivy
```
wget https://github.com/aquasecurity/trivy/releases/download/v0.50.1/trivy_0.50.1_Linux-64bit.tar.gz
tar -xvf trivy_0.50.1_Linux-64bit.tar.gz
./trivy image mynonroot

docker pull ubuntu:22.04
./trivy image ubuntu:22.04
```

