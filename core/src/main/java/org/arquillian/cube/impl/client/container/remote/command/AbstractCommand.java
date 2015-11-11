package org.arquillian.cube.impl.client.container.remote.command;

import java.io.Serializable;

import org.jboss.arquillian.container.test.spi.command.Command;

public abstract class AbstractCommand<T> implements Command<T>, Serializable
{
   private static final long serialVersionUID = 1L;

   private T result;
   private Throwable throwable;

   public void setResult(T result) {
      this.result = result;
   }

   @Override
   public T getResult() {
      return result;
   }

   @Override
   public void setThrowable(Throwable throwable) {
      this.throwable = throwable;
   }

   /**
    * @return the throwable
    */
   @Override
   public Throwable getThrowable() {
      return throwable;
   }
}