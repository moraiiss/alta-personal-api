package br.com.altapersonal.api.shared.domain;

/**
 * Base das violações de regra de negócio. O handler HTTP traduz qualquer subclasse em 400.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
