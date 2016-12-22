/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.flowable.engine.impl.interceptor;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableOptimisticLockingException;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepts {@link FlowableOptimisticLockingException} and tries to run the same command again. The number of retries and the time waited between retries is configurable.
 * 
 * @author Daniel Meyer
 */
public class RetryInterceptor extends AbstractCommandInterceptor {

  private static Logger log = LoggerFactory.getLogger(RetryInterceptor.class);

  protected int numOfRetries = 3;
  protected int waitTimeInMs = 50;
  protected int waitIncreaseFactor = 5;

  public <T> T execute(CommandConfig config, Command<T> command) {
    long waitTime = waitTimeInMs;
    int failedAttempts = 0;

    do {
      if (failedAttempts > 0) {
        log.info("Waiting for {}ms before retrying the command.", waitTime);
        waitBeforeRetry(waitTime);
        waitTime *= waitIncreaseFactor;
      }

      try {

        // try to execute the command
        return next.execute(config, command);

      } catch (FlowableOptimisticLockingException e) {
          log.info("Caught optimistic locking exception: {}", e);
      }

      failedAttempts++;
    } while (failedAttempts <= numOfRetries);

    throw new FlowableException(numOfRetries + " retries failed with FlowableOptimisticLockingException. Giving up.");
  }

  protected void waitBeforeRetry(long waitTime) {
    try {
      Thread.sleep(waitTime);
    } catch (InterruptedException e) {
      log.debug("I am interrupted while waiting for a retry.");
    }
  }

  public void setNumOfRetries(int numOfRetries) {
    this.numOfRetries = numOfRetries;
  }

  public void setWaitIncreaseFactor(int waitIncreaseFactor) {
    this.waitIncreaseFactor = waitIncreaseFactor;
  }

  public void setWaitTimeInMs(int waitTimeInMs) {
    this.waitTimeInMs = waitTimeInMs;
  }

  public int getNumOfRetries() {
    return numOfRetries;
  }

  public int getWaitIncreaseFactor() {
    return waitIncreaseFactor;
  }

  public int getWaitTimeInMs() {
    return waitTimeInMs;
  }
}