package com.medsimple.shared.exception;

/**
 * A request that is well-formed and authorized but not allowed given the current state of the
 * domain — e.g. prescribing on an episode not yet assigned to a physician, or triaging an episode
 * with no completed submissions. Maps to 409 Conflict with the message.
 *
 * <p>Deliberately distinct from a raw {@link IllegalStateException}: that is reserved for internal
 * faults that should never happen (encryption failure, serialization failure) and stays a 500.
 * Domain preconditions are client-actionable and must surface as a clear 4xx, never an opaque 500.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
