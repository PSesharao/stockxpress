package com.seshrao.stockxpress.inventoryservice;

import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import com.seshrao.stockxpress.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableEurekaClient
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository){

	// return args -> { }: This is the lambda expression defining the implementation of the
	// run method of CommandLineRunner run(String... args)

	// inventoryRepository is injected through method injection here

		return args -> {
			Inventory inventory1 = Inventory.builder()
					.skuCode("Iphone_13")
					.quantity(100)
					.build();

			Inventory inventory2 = Inventory.builder()
					.skuCode("Iphone_13_red")
					.quantity(0)
					.build();

			inventoryRepository.save(inventory1) ;
			inventoryRepository.save(inventory2) ;
		};
	}

}
