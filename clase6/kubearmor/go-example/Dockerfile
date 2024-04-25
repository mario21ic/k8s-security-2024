FROM golang:1.21 as builder
WORKDIR /src
COPY ./main.go .
RUN go build -o /main ./main.go

#FROM scratch
FROM nginx
RUN apt update && apt install curl procps -y
COPY --from=builder /main /main
COPY ./vault/secrets/database.txt /vault/secrets/
CMD ["/main"]
