/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.util.poller;

import org.apache.sling.testing.timeouts.TimeoutsProvider;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * Helper for repeating a call until it returns true, with timeout capabilities.
 * Subclasses should override the {@link #call()} method.
 * Can be used with lambda expressions, using the constructor {@link #Polling(Callable c)}.
 *
 * @since 1.1.0
 */
public class Polling implements Callable<Boolean> {

    /**
     * Optional object to be used by the default implementation of call()
     */
    protected final Callable<Boolean> c;

    /**
     * Holder for the last exception thrown by call(), to be used for logging
     */
    protected Exception lastException;

    /**
     * Counter for total waiting time
     */
    protected long waited;

    /**
     * Default constructor to be used in subclasses that override the {@link #call()} method.
     * Should not be used directly on {@code Polling} instances, but only on extended classes.
     * If used directly to get a {@code Polling} instance, executing {@link #poll(long timeout, long delay)}
     * will be equivalent to {@code Thread.sleep(timeout)}
     */
    public Polling() {
        this.c = null;
        this.lastException = null;
        this.waited = 0;
    }

    /**
     * Creates a new instance that uses the {@code Callable} parameter for polling
     *
     * @param c object whose {@code call()} method will be polled
     */
    public Polling(Callable<Boolean> c) {
        this.c = c;
        this.lastException = null;
        this.waited = 0;
    }

    /**
     * <p>Method to be called by {@link #poll(long timeout, long delay)}, potentially multiple times,
     * until it returns true or timeout is reached.<br>
     * Subclasses can override it to change the check accordingly. The method should return true
     * only when the call was successful.<br>
     * It can return false or throw any {@code Exception} to make the poller try again later.</p>
     *
     * <p>The default implementation delegates the call to the {@code Callable c} instance.</p>
     *
     * @return {@code true} to end polling
     * @throws Exception if unable to compute a result
     */
    @Override
    public Boolean call() throws Exception {
        if (c != null) {
            return c.call();
        } else {
            return false;
        }
    }

    /**
     * <p>Tries to execute {@link #call()} until it returns true or until {@code timeout} is reached.
     * Between retries, it waits using {@code Thread.sleep(delay)}. It means the retry is not at a fixed pace,
     * but depends on the execution time of the call itself.</p>
     * <p>The method guarantees that the call() will be executed at least once. If the timeout is 0 or less, then
     * call() will be executed exactly once.</p>
     * <p>The timeout is adjusted using {@link TimeoutsProvider} so the final value can be changed using the
     * system property: {@value org.apache.sling.testing.timeouts.TimeoutsProvider#PROP_TIMEOUT_MULTIPLIER}</p>
     *
     * @param timeout max total execution time, in milliseconds
     * @param delay time to wait between calls, in milliseconds
     *
     * @throws TimeoutException if {@code timeout} was reached
     * @throws InterruptedException if the thread was interrupted while sleeping; caller should throw it further
     */
    public void poll(long timeout, long delay) throws TimeoutException, InterruptedException {
        long start = System.currentTimeMillis();
        long effectiveTimeout = TimeoutsProvider.getInstance().getTimeout(timeout);

        do {
            try {
                boolean success = call();
                if (success) {
                    waited = System.currentTimeMillis() - start;
                    return;
                }
            } catch (Exception e) {
                lastException = e;
            }
            Thread.sleep(delay);
        } while (System.currentTimeMillis() < start + effectiveTimeout);

        waited = System.currentTimeMillis() - start;
        throw new TimeoutException(String.format(message(), effectiveTimeout, delay));
    }

    public long getWaited() {
        return waited;
    }

    /**
     * Returns the string to be used in the {@code TimeoutException}, if needed.
     * The string is passed to {@code String.format(message(), timeout, delay)}, so it can be a format
     * including {@code %1$d} and {@code %2$d}. The field {@code lastException} is also available for logging
     *
     * @return the format string
     */
    protected String message() {
        return "Call failed to return true in %1$d ms. Last exception was: " + lastException;
    }
}
