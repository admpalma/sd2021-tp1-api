package tp1.server.rest.client;

import tp1.api.service.util.Result;
import tp1.api.service.util.Result.ErrorCode;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Shared client behavior.
 * 
 * Used to retry an operation in a loop.
 * 
 * @author smduarte
 *
 */
public abstract class RetryClient {
	private static Logger Log = Logger.getLogger(RetryClient.class.getName());

	protected static final int READ_TIMEOUT = 5000;
	protected static final int CONNECT_TIMEOUT = 5000;

	protected static final int RETRY_SLEEP = 1000;
	protected static final int MAX_RETRIES = 10;
	
	// higher order function to retry forever a call until it succeeds
	// and return an object of some type T to break the loop
	protected <T> Result<T> reTry(Supplier<Result<T>> func) {
		for (int i = 0; i < MAX_RETRIES; i++)
			try {
				return func.get();
			} catch (Exception x) {
				Log.fine("Exception: " + x.getMessage());
				x.printStackTrace();
				try {
					Thread.sleep(RETRY_SLEEP);
				} catch (InterruptedException ignored) {
				}
			}
		return Result.error( ErrorCode.INTERNAL_ERROR ); //TODO timeout?????
	}
}