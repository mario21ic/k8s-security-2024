package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;


@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
            SpringApplication.run(DemoApplication.class, args);
            System.out.println("Iniciando..");

            String pathToFile = "/vault/secrets/database.txt";
            System.out.println("Archivo: " + pathToFile);
            try {
                String content = Files.readString(Paths.get(pathToFile));
                System.out.println(content);
            } catch (IOException e) {
                System.out.println("Ocurrio un error al leer el archivo: " + e.getMessage());
            }
            System.out.println("Iniciado");
	}

}
