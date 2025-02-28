package com.paypal.raptor.aiml.utils;

import com.ebay.kernel.cal.api.CalStream;
import com.ebay.kernel.cal.api.CalStreamUtils;
import com.ebay.kernel.cal.api.CalTransaction;
import com.ebay.kernel.cal.api.sync.CalEventHelper;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A ThreadPoolExecutor that can be used with CompleteableFutures to add awareness of the CalStream threadlocal that
 * may be set on the calling thread and need to be passed down to the async thread belonging to the executor to
 * support legacy CAL code
 */
@Component
public class CalAwareThreadPoolExecutor extends ThreadPoolExecutor {
    private static final String TYPE = "AsyncCb";
    private static final String NAME = "CalAwareThreadPoolExecutor";
    private static final int CORE_SIZE = 16;
    private static final int MAX_SIZE = 128;
    private static final int QUEUE_SIZE = 2048;

    public CalAwareThreadPoolExecutor() {
        super(CORE_SIZE, MAX_SIZE, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(QUEUE_SIZE), (r, e) -> {
            CalEventHelper.sendImmediate("ALML_GATEWAY_HTTP_REQUEST", "PREDICT_THREAD_POOL", "1", "");
        });
    }

    @Override
    public void execute(Runnable command) {
        // Create a new runnable wrapping the original, and pass in the CalStream from the main thread as the parent
        super.execute(new CalWrappedRunnable(command, CalStreamUtils.getInstance().currentCalStream()));
    }

    /**
     * Creates a new Runnable that wraps the original Runnable and passes in the calling threads CalStream threadlocal value (if available)
     */
    private class CalWrappedRunnable implements Runnable {
        private final Runnable task;
        private final CalStream parentCalStream;

        /**
         * @param task            the original runnable
         * @param parentCalStream the value of the CalStream threadlocal from the calling thread
         */
        public CalWrappedRunnable(Runnable task, CalStream parentCalStream) {
            this.task = task;
            this.parentCalStream = parentCalStream;
        }

        public void run() {
            CalTransaction asyncCbTxn = parentCalStream.asyncTransaction(TYPE, NAME);
            asyncCbTxn.setStatus("0");
            CalStreamUtils.getInstance().installAsyncStreamAsSync(asyncCbTxn.calStream());//install the CAL threadlocal
            try {
                task.run();//run the wrapped Runnable
            } catch (Throwable th) {
                asyncCbTxn.setStatus(th);
                throw th;
            } finally {
                CalStreamUtils.getInstance().extractSyncStreamAsAsync();//and then uninstall the thread local so the thread can safely be returned to a thread pool
                asyncCbTxn.completed();//uninstall the CAL threadlocal
            }
        }
    }
}