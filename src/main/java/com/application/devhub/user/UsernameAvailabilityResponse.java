package com.application.devhub.user;

public record UsernameAvailabilityResponse(boolean available, Reason reason) {

    static UsernameAvailabilityResponse free() {
        return new UsernameAvailabilityResponse(true, null);
    }

    static UsernameAvailabilityResponse unavailable(Reason reason) {
        return new UsernameAvailabilityResponse(false, reason);
    }

    public enum Reason {
        TAKEN,
        INVALID
    }
}
