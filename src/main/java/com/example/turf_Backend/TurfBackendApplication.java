package com.example.turf_Backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class 	TurfBackendApplication {

	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.configure()
				.filename(".env") // or ".env.dev" if that’s your file
				.ignoreIfMissing()
				.load();
		dotenv.entries().forEach(entry ->
				System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(TurfBackendApplication.class, args);
	}

}
