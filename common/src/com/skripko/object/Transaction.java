package com.skripko.object;

import com.sun.istack.internal.Nullable;

import java.util.concurrent.*;

import static com.skripko.common.SelenideUtils.print;

public class Transaction {
	private long timeout = 20;
	private Option option;
	public enum Option {
		SOFT_ERROR
	}

	public Transaction(long timeout, Option... option) {
		this.timeout = timeout;
		if (option.length > 0 && option[0] == Option.SOFT_ERROR) {
			this.option = option[0];
		}
	}

	/**
	 * One can adjust timeout settings through public static member
	 * And setup SOFT_ERROR configuration for task execution
	 *
	 * @param task
	 * @return As you wish)
	 */
	@Nullable
	public Object executeWithTimeLimit(Callable task) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		try {
			Future<Boolean> oneTaskResult = service.submit(task);
			return oneTaskResult.get(timeout, TimeUnit.SECONDS);
		} catch (Throwable th) {
			if (option == Option.SOFT_ERROR) {
				print("> Soft transaction throwable message: " + th.getMessage());
				return null;
			} else {
				throw new RuntimeException(th);
			}
		} finally {
			service.shutdown();
		}
	}
}