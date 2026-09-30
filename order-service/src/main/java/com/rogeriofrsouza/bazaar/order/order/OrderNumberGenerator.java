package com.rogeriofrsouza.bazaar.order.order;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
class OrderNumberGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 8;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final OrderRepository orderRepository;

    OrderNumberGenerator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String number = randomNumber();
            if (!orderRepository.existsByNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Could not generate a unique order number after " + MAX_ATTEMPTS + " attempts");
    }

    private String randomNumber() {
        StringBuilder number = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(ALPHABET.length());
            number.append(ALPHABET.charAt(index));
        }
        return number.toString();
    }
}
