package org.interledger;

import org.interledger.InterledgerPacket.Handler.AbstractHandler;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilqp.QuoteLiquidityRequest;
import org.interledger.ilqp.QuoteLiquidityResponse;

import java.util.Objects;

/**
 * <p>A top-level interface for all Interledger objects that can be encoded and decoded as an
 * Interledger packet using some sort of encoding, such as ANS.1, JSON, Protobuf or some other
 * encoding. Not all POJOs in this library are considered Interledger "packets". For example, an
 * {@link InterledgerAddress} is used in many packet implementations, but is not something that is
 * sent by itself "on the wire" to facilitate Interledger operations. Conversely, {@link
 * InterledgerPayment} is an Interledger packet because it is sent by itself "on the wire" to
 * facilitate Interledger operations.</p>
 */
public interface InterledgerPacket {

  /**
   * A handler interface that defines all types of {@link InterledgerPacket} to handle. For actual
   * usage, consider an instance of {@link Handler.AbstractHandler}, which provides useful
   * scaffolding to assist in actually handling concrete packets at runtime.
   */
  interface Handler<R> {

    /**
     * The main handler method to coerce an instance of {@link InterledgerPacket} into its actual
     * type, apply some business logic, and optionally return a value in response.
     *
     * @param packet An instance of {@link InterledgerPacket}.
     *
     * @return An instance of type {@link R}, in response to the supplied input.
     */
    R execute(InterledgerPacket packet);

    /**
     * A handler for allowing callers to specify logic based upon an unknown result type. This class
     * can be used in the following manner:
     *
     * <pre>
     * <code>
     *
     * final InterledgerPacket decodedPacket = context.read(asn1OerPaymentBytes);
     * final String stringValue = new AbstractHandler&lt;String&gt;() {
     *   protected String handle(final InterledgerPayment interledgerPacket) {
     *      // ... do handling here.
     *      return interledgerPacket.toString();
     *   }
     * }.execute(decodedPacket); // be sure to call .execute!
     *
     * // Do something with 'stringValue'
     * </code>
     * </pre>
     *
     * @param <R> the type of the result of the handler.
     */
    abstract class AbstractHandler<R> implements Handler<R> {

      /**
       * Handle an instance of {@link InterledgerPayment}.
       *
       * @param interledgerPayment An instance of {@link InterledgerPayment}.
       *
       * @return An instance of type {@link R}, in response to the supplied input.
       */
      protected abstract R handle(final InterledgerPayment interledgerPayment);

      /**
       * Handle an instance of {@link QuoteLiquidityRequest}.
       *
       * @param quoteLiquidityRequest An instance of {@link QuoteLiquidityRequest}.
       *
       * @return An instance of type {@link R}, in response to the supplied input.
       */
      protected abstract R handle(final QuoteLiquidityRequest quoteLiquidityRequest);

      /**
       * Handle an instance of {@link QuoteLiquidityResponse}.
       *
       * @param quoteLiquidityResponse An instance of {@link QuoteLiquidityResponse}.
       *
       * @return An instance of type {@link R}, in response to the supplied input.
       */
      protected abstract R handle(final QuoteLiquidityResponse quoteLiquidityResponse);

      // TODO: Handle the rest of the ILQP packets!

      /**
       * The main handler method to coerce an instance of {@link InterledgerPacket} into its actual
       * type, apply some business logic, and optionally return a value in response.
       *
       * @param packet An instance of {@link InterledgerPacket}.
       *
       * @return An instance of type {@link R}, in response to the supplied input.
       */
      @Override
      public final R execute(final InterledgerPacket packet) {
        Objects.requireNonNull(packet);
        if (packet instanceof InterledgerPayment) {
          return this.handle((InterledgerPayment) packet);
        } else if (packet instanceof QuoteLiquidityRequest) {
          return this.handle((QuoteLiquidityRequest) packet);
        } else if (packet instanceof QuoteLiquidityResponse) {
          return this.handle((QuoteLiquidityResponse) packet);
        } else {
          throw new InterledgerRuntimeException("Unhandled InterledgerPacket: " + packet);
        }
      }

      /**
       * A utility class that provides default implementations of {@link AbstractHandler} so that a
       * caller is only forced to implement the handlers that are of interest. The idea behind this
       * class is that an implementor will only override the methods that are desired to be handled,
       * and if any unimplemented methods are called, an exception will be thrown. For example, if a
       * caller knows that the result is going to be of type {@link InterledgerPayment}, then it is
       * not useful to add boilerplate implementations of the other handler methods that do nothing,
       * just to satisfy the abstract-class requirements of {@link AbstractHandler}.
       */
      public static class HelperHandler<R> extends AbstractHandler<R> {

        @Override
        protected R handle(InterledgerPayment interledgerPayment) {
          throw new InterledgerRuntimeException(
            "Not yet implemented. Override this method to provide a useful implementation!");
        }

        @Override
        protected R handle(QuoteLiquidityRequest quoteLiquidityRequest) {
          throw new InterledgerRuntimeException(
            "Not yet implemented. Override this method to provide a useful implementation!");
        }

        @Override
        protected R handle(QuoteLiquidityResponse quoteLiquidityResponse) {
          throw new InterledgerRuntimeException(
            "Not yet implemented. Override this method to provide a useful implementation!");
        }
      }
    }
  }

  /**
   * A handler interface that defines all types of {@link InterledgerPacket} to handle. For actual
   * usage, consider an instance of {@link VoidHandler.AbstractVoidHandler}, which provides useful
   * scaffolding to assist in actually handling concrete packets at runtime.
   */
  interface VoidHandler {

    /**
     * The main handler method to coerce an instance of {@link InterledgerPacket} into its actual
     * type, apply some business logic, and optionally return a value in response.
     *
     * @param packet An instance of {@link InterledgerPacket}.
     */
    void execute(InterledgerPacket packet);

    /**
     * An abstract implementation of {@link VoidHandler} for allowing callers to specify logic based
     * upon an unknown result type extending {@link InterledgerPacket}. This class can be used in
     * the following manner:
     *
     * <pre>
     * <code>
     * final InterledgerPacket decodedPacket = context.read(asn1OerPaymentBytes);
     * new AbstractVoidHandler() {
     *   protected void handle(final InterledgerPayment interledgerPayment) {
     *      // ... do handling here.
     *   }
     * }.execute(decodedPacket); // be sure to call .execute!
     * </code>
     * </pre>
     */
    abstract class AbstractVoidHandler implements VoidHandler {

      /**
       * Handle an instance of {@link InterledgerPayment}.
       *
       * @param interledgerPayment An instance of {@link InterledgerPayment}.
       */
      protected abstract void handle(final InterledgerPayment interledgerPayment);

      /**
       * Handle an instance of {@link QuoteLiquidityRequest}.
       *
       * @param quoteLiquidityRequest An instance of {@link QuoteLiquidityRequest}.
       */
      protected abstract void handle(final QuoteLiquidityRequest quoteLiquidityRequest);

      /**
       * Handle an instance of {@link QuoteLiquidityResponse}.
       *
       * @param quoteLiquidityResponse An instance of {@link QuoteLiquidityResponse}.
       */
      protected abstract void handle(final QuoteLiquidityResponse quoteLiquidityResponse);

      // TODO: Handle the rest of the ILQP packets!

      /**
       * The main handler method to coerce an instance of {@link InterledgerPacket} into its actual
       * type, apply some business logic, and optionally return a value in response.
       *
       * @param packet An instance of {@link InterledgerPacket}.
       */
      public final void execute(final InterledgerPacket packet) {
        Objects.requireNonNull(packet);
        if (packet instanceof InterledgerPayment) {
          this.handle((InterledgerPayment) packet);
        } else if (packet instanceof QuoteLiquidityRequest) {
          this.handle((QuoteLiquidityRequest) packet);
        } else if (packet instanceof QuoteLiquidityResponse) {
          this.handle((QuoteLiquidityResponse) packet);
        } else {
          throw new InterledgerRuntimeException("Unhandled InterledgerPacket: " + packet);
        }
      }

      /**
       * A utility class that provides default implementations of {@link AbstractVoidHandler}
       * methods so that a caller is only forced to implement the handlers that are of interest. The
       * idea behind this class is that an implementor will only override the methods that are
       * desired to be handled, and if any unimplemented methods are called, an exception will be
       * thrown. For example, if a caller knows that the result is going to be of type {@link
       * InterledgerPayment}, then it is not useful to add boilerplate implementations of the other
       * handler methods that do nothing, just to satisfy the abstract-class requirements of {@link
       * AbstractHandler}.
       */
      public static class HelperHandler extends AbstractVoidHandler {

        @Override
        protected void handle(InterledgerPayment interledgerPayment) {
          throw new InterledgerRuntimeException(
            "Not yet implemented. Override this method to provide a useful implementation!");
        }

        @Override
        protected void handle(QuoteLiquidityRequest quoteLiquidityRequest) {
          throw new InterledgerRuntimeException(
            "Not yet implemented. Override this method to provide a useful implementation!");
        }

        @Override
        protected void handle(QuoteLiquidityResponse quoteLiquidityResponse) {
          throw new InterledgerRuntimeException(
            "Not yet implemented. Override this method to provide a useful implementation!");
        }
      }
    }
  }

}
