package uk.gov.hmcts.reform.iacasedocumentsapi.infrastructure.security.idam;

public class IdentityManagerResponseException extends RuntimeException {

    public IdentityManagerResponseException(
        String message,
        Throwable cause) {

        super(message, cause);

    }

}
