package com.example.pricewatch.exception;

public class PriceParsingException extends RuntimeException {

    public PriceParsingException(String message) {
        super(message);
    }

    public PriceParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
