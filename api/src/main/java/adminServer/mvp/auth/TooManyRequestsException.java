package adminServer.mvp.auth;

public class TooManyRequestsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
