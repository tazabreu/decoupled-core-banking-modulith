package com.banking.modulith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootApplication
public class DecoupledCoreBankingModulithApplication {
	public static void main(String[] args) {
		// Verify and visualize modules
		var modules = ApplicationModules.of(DecoupledCoreBankingModulithApplication.class);
		modules.forEach(System.out::println);

		SpringApplication.run(DecoupledCoreBankingModulithApplication.class, args);
	}
}
