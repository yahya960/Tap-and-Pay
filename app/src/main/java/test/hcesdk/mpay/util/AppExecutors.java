/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.hcesdk.mpay.util;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Global executor pools for the whole application.
 * <p>
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
public class AppExecutors {

    private final Executor diskIO;

    private final Executor networkIO;

    private final MainThreadExecutor mainThread;

    private final PaymentExecutor paymentThread;

    private AppExecutors(Executor diskIO, Executor networkIO, MainThreadExecutor mainThread, PaymentExecutor paymentThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
        this.paymentThread = paymentThread;
    }

    public AppExecutors() {
        this(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(3), new MainThreadExecutor(), new PaymentExecutor());
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor networkIO() {
        return networkIO;
    }

    public MainThreadExecutor mainThread() {
        return mainThread;
    }

    public PaymentExecutor paymentThread() {
        return paymentThread;
    }
}
