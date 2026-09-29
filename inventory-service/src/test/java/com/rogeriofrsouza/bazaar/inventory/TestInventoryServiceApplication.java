package com.rogeriofrsouza.bazaar.inventory;

import org.springframework.boot.SpringApplication;

public class TestInventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(InventoryServiceApplication::main)
                .with(ContainersConfig.class)
                .run(args);
    }
}
