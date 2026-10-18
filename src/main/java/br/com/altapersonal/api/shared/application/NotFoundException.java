package br.com.altapersonal.api.shared.application;

/**
 * Base dos casos de "recurso não encontrado". O handler HTTP traduz qualquer subclasse em 404.
 */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
