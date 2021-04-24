package tp1.server.resources.requester;

import tp1.api.service.util.Result;

import java.util.function.Supplier;

public abstract class AbstractRequester {

    protected final static int MAX_RETRIES = 3;
    protected final static long RETRY_PERIOD = 100;
    protected final static int CONNECTION_TIMEOUT = 900;
    protected final static int REPLY_TIMEOUT = 1000;

    protected <R> Result<R> defaultRetry(Supplier<Result<R>> request) {
        for (int retries = 0; retries < MAX_RETRIES; retries++) {
//            System.out.println("Requesting " + request);
            try {
                Result<R> result = request.get();
//                System.out.println("Done request");
                if (!result.isOK() && result.error() == Result.ErrorCode.INTERNAL_ERROR) {
                    try {
                        Thread.sleep(RETRY_PERIOD);
                    } catch (InterruptedException e) {
//                        System.out.println("Interrupted Exception " + e);
                        //nothing to be done here, if this happens we will just retry sooner.
                    }
                } else {
                    return result;
                }
            }catch(Exception e){
                System.out.println("Caught external loop exception:\n"+e);
            }
        }
        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }
}
