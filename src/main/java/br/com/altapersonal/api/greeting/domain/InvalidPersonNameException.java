package br.com.altapersonal.api.greeting.domain;

import br.com.altapersonal.api.shared.domain.DomainException;

public final class InvalidPersonNameException extends DomainException {

    public InvalidPersonNameException(String message) {
        super(message);
    }
}
