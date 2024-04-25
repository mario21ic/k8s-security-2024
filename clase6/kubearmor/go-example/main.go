package main

import (
    "fmt"
    "os/exec"
    "os"
    "time"
)

func check(e error) {
    if e != nil {
        panic(e)
    }
}

func main() {
    secret_path := "/vault/secrets/database.txt"
    fmt.Println("Archivo " + secret_path)
    dat, err := os.ReadFile(secret_path)
    check(err)
    fmt.Print(string(dat))

    fmt.Println("Iniciando nginx")
    cmd := exec.Command("/usr/sbin/nginx", "-g", "daemon off;")
    if err := cmd.Start(); err != nil {
        fmt.Println("Error al iniciar:", err)
        return
    }

    fmt.Println("Persistiendo")
    // Bucle infinito para que el programa nunca termine
    for {
        time.Sleep(time.Hour)
    }
}
