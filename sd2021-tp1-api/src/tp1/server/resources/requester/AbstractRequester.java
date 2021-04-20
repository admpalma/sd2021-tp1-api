package tp1.server.resources.requester;

import tp1.api.service.util.Result;

import java.util.function.Supplier;

public abstract class AbstractRequester {

    protected final static int MAX_RETRIES = 3;
    protected final static long RETRY_PERIOD = 1000;
    protected final static int CONNECTION_TIMEOUT = 1000;
    protected final static int REPLY_TIMEOUT = 600;

    protected <R> Result<R> defaultRetry(Supplier<Result<R>> request) {
        for (int retries = 0; retries < MAX_RETRIES; retries++) {
            Result<R> result = request.get();
            if (!result.isOK() && result.error() == Result.ErrorCode.INTERNAL_ERROR) {
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    //nothing to be done here, if this happens we will just retry sooner.
                }
            } else {
                return result;
            }
        }
        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }
}
