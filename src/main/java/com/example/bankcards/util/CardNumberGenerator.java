package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CardNumberGenerator {

    @Value("${bin}")
    private String bin;
    private static final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        String numberWithoutCheckDigit = bin + IntStream.range(0, 15 - bin.length())
                .mapToObj(i -> String.valueOf(secureRandom.nextInt(10)))
                .collect(Collectors.joining());

        int checkDigit = calculateCheckDigit(numberWithoutCheckDigit);

        return numberWithoutCheckDigit + checkDigit;
    }

    private static int calculateCheckDigit(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int digit = Character.getNumericValue(number.charAt(number.length() - 1 - i));

            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }

}