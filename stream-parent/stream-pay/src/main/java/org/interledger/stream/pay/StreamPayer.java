package org.interledger.stream.pay;

import org.interledger.core.InterledgerAddress.AllocationScheme;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.ScaledExchangeRate;
import org.interledger.fx.Slippage;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.StreamException;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.AmountFilter;
import org.interledger.stream.pay.filters.AssetDetailsFilter;
import org.interledger.stream.pay.filters.ExchangeRateFilter;
import org.interledger.stream.pay.filters.FailureFilter;
import org.interledger.stream.pay.filters.MaxPacketAmountFilter;
import org.interledger.stream.pay.filters.PacingFilter;
import org.interledger.stream.pay.filters.SequenceFilter;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.probing.ExchangeRateProber;
import org.interledger.stream.pay.probing.model.EstimatedPaymentOutcome;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;
import org.interledger.stream.pay.trackers.AssetDetailsTracker;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;

/**
 * An interface for making payments using the Interledger STREAM protocol.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md"
 */
public interface StreamPayer {

  /**
   * Obtain a payment quote in order to perform a payment by doing the following:
   *
   * <ol>
   *   <li>Query the recipient's payment pointer, if provided.</li>
   *   <li>Ensure a viable payment path to the recipient.</li>
   *   <li>Probe the realized rate to the recipient.</li>
   *   <li>Prepare to enforce the exchange rate by comparing against rates pulled from external sources.</li>
   * </ol>
   *
   * @param paymentOptions A {@link PaymentOptions} with all of the details necessary to make a payment.
   *
   * @return A {@link CompletableFuture} that completes with a {@link Quote}.
   */
  CompletableFuture<Quote> getQuote(PaymentOptions paymentOptions) throws StreamException;

  /**
   * Make an actual payment using the probed (or otherwise statically configured) information in {@code quote}.
   *
   * @param quote A soft {@link Quote} with details about a payment path.
   *
   * @return A {@link CompletableFuture} that completes with a payment {@link PaymentReceipt} (which has an
   *   optionally-present {@link StreamPayerException} if anything went wrong with the payment).
   */
  CompletableFuture<PaymentReceipt> pay(Quote quote);

  /**
   * The default implementation of {@link StreamPayer}.
   */
  class Default implements StreamPayer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Link<? extends LinkSettings> link;
    private final SpspClient spspClient;
    private final StreamEncryptionUtils streamEncryptionUtils;
    private final ExchangeRateProvider oracleExchangeRateProvider;

    /**
     * Required-args Constructor.
     *
     * @param streamEncryptionUtils      A {@link StreamEncryptionUtils}.
     * @param link                       A {@link Link}.
     * @param oracleExchangeRateProvider An {@link ExchangeRateProvider}.
     */
    public Default(
      final StreamEncryptionUtils streamEncryptionUtils,
      final Link<? extends LinkSettings> link,
      final ExchangeRateProvider oracleExchangeRateProvider
    ) {
      this(streamEncryptionUtils, link, oracleExchangeRateProvider, new SimpleSpspClient());
    }

    // TODO: Re-work Constructors.
    @VisibleForTesting
    Default(
      final StreamEncryptionUtils streamEncryptionUtils,
      final Link<? extends LinkSettings> link,
      final ExchangeRateProvider oracleExchangeRateProvider,
      final SpspClient spspClient
    ) {
      this.link = Objects.requireNonNull(link);
      this.spspClient = Objects.requireNonNull(spspClient);
      this.oracleExchangeRateProvider = Objects.requireNonNull(oracleExchangeRateProvider);
      this.streamEncryptionUtils = Objects.requireNonNull(streamEncryptionUtils);
    }

    @Override
    public CompletableFuture<Quote> getQuote(final PaymentOptions paymentOptions) throws StreamException {

      return CompletableFuture.supplyAsync(() -> {

        final BigInteger totalAmountToSendInSourceUnits = this.obtainValidatedAmountToSend(paymentOptions);

        // TODO: Once we know the account details, ensure that the indicated currency in paymentOptions matches the
        //  account's currency
        final StreamConnectionDetails streamConnectionDetails = this.fetchRecipientAccountDetails(paymentOptions);
        this.validateAllocationSchemes(paymentOptions.senderAccountDetails(), streamConnectionDetails);

        final StreamConnection streamConnection = this.newStreamConnection(paymentOptions, streamConnectionDetails);
        final ExchangeRateProber exchangeRateProber = newExchangeRateProber();

        //////////////////
        // Probe the path.
        //////////////////
        final ExchangeRateProbeOutcome rateProbeOutcome = exchangeRateProber.probePath(streamConnection);

        // If there's an error, then throw immediately.
        rateProbeOutcome.errorPackets().stream().findFirst()
          .map(streamPacketReply -> streamPacketReply.exception())
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter($ -> StreamPayerException.class.isAssignableFrom($.getClass()))
          .map($ -> (StreamPayerException) $)
          .ifPresent(error -> {
            throw error;
          });

        final PaymentSharedStateTracker paymentSharedStateTracker = exchangeRateProber
          .getPaymentSharedStateTracker(streamConnection).orElseThrow(() -> new StreamPayerException(
            String.format("No PaymentSharedStateTracker found for streamConnection=%s", streamConnection),
            SendState.RateProbeFailed
          ));

        // Get the current FX rate.
        final ExchangeRate currentExternalExchangeRate = getExchangeRate(paymentOptions, rateProbeOutcome);

        final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails(
          streamConnection, exchangeRateProber
        );
        //TODO: Check this against JS
        final ScaledExchangeRate scaledExternalRate = this.determineScaledExternalRate(
          paymentOptions.senderAccountDetails(),
          destinationAccountDetails,
          currentExternalExchangeRate,
          paymentOptions.slippage()
        );

        final Ratio minScaledExchangeRate = scaledExternalRate.lowerBound();
        logger.debug("Calculated min exchange rate of {}", minScaledExchangeRate);

        final EstimatedPaymentOutcome estimatedPaymentOutcome = paymentSharedStateTracker
          .getAmountTracker().setPaymentTarget(
            PaymentType.FIXED_SEND,  // Invoices are fixed delivery, but not yet supported.
            minScaledExchangeRate,
            rateProbeOutcome.maxPacketAmount().value(),
            totalAmountToSendInSourceUnits
          );

        logger.debug("Quote complete. Assembling normalized version");

        // Normalize all values to the
        final Denomination sourceDenomination = this.checkForSourceDenomination(paymentOptions.senderAccountDetails());
        final Denomination destinationDenomination = this
          .checkForReceiverDenomination(streamConnection, exchangeRateProber);

        final Function<Ratio, Ratio> shiftRate = (rate) -> this
          .shiftRateForNormalization(rate, sourceDenomination.assetScale(), destinationDenomination.assetScale());

        final Ratio lowerBoundRate = shiftRate
          .apply(paymentSharedStateTracker.getExchangeRateTracker().getLowerBoundRate());
        final Ratio upperBoundRate = shiftRate
          .apply(paymentSharedStateTracker.getExchangeRateTracker().getUpperBoundRate());
        final Ratio minExchangeRate = shiftRate.apply(minScaledExchangeRate);
        final BigInteger maxSourceAmount = new BigDecimal(estimatedPaymentOutcome.maxSendAmountInWholeSourceUnits())
          .movePointLeft(sourceDenomination.assetScale())
          .setScale(0, RoundingMode.HALF_EVEN)
          .toBigIntegerExact();
        final BigInteger minDeliveryAmount = new BigDecimal(
          estimatedPaymentOutcome.minDeliveryAmountInWholeDestinationUnits())
          .movePointLeft(destinationDenomination.assetScale())
          .setScale(0, RoundingMode.HALF_EVEN)
          .toBigIntegerExact();

        // Estimate how long the payment may take based on max packet amount, RTT, and rate of packet sending
        // TODO: Unit test!
        final int packetFrequency = paymentSharedStateTracker.getPacingTracker().getPacketFrequency();
        final Duration estimatedDuration = Duration.of(
          estimatedPaymentOutcome.estimatedNumberOfPackets().multiply(BigInteger.valueOf(packetFrequency)).longValue(),
          ChronoUnit.MILLIS
        );

        final Quote quote = Quote.builder()
          .paymentOptions(paymentOptions)
          .streamConnection(streamConnection)
          .paymentSharedStateTracker(paymentSharedStateTracker)
          .sourceAccount(paymentOptions.senderAccountDetails())
          .destinationAccount(destinationAccountDetails)
          .estimatedDuration(estimatedDuration)
          .estimatedPaymentOutcome(
            EstimatedPaymentOutcome.builder()
              .maxSendAmountInWholeSourceUnits(maxSourceAmount)
              .minDeliveryAmountInWholeDestinationUnits(minDeliveryAmount)
              .estimatedNumberOfPackets(estimatedPaymentOutcome.estimatedNumberOfPackets())
              .build()
          )
          .minAllowedExchangeRate(minExchangeRate)
          // Translate the upper and lower bound rates into a rate scaled for sender and receiver account scales.
          .estimatedExchangeRate(ExchangeRateProbeOutcome.builder().from(rateProbeOutcome)
            .lowerBoundRate(lowerBoundRate)
            .upperBoundRate(upperBoundRate)
            .build())
          .build();

        if (logger.isDebugEnabled()) {
          logger.debug("Quote complete: {}", quote);
        }
        return quote;
      });

    }

    @Override
    public CompletableFuture<PaymentReceipt> pay(final Quote quote) {
      Objects.requireNonNull(quote);

      final List<StreamPacketFilter> streamPacketFilters = Lists.newArrayList(
        // First so all other controllers log the sequence number
        new SequenceFilter(quote.paymentSharedStateTracker()),
        // Fail-fast on terminal rejects or timeouts
        new FailureFilter(),
        // Fail-fast on destination asset detail conflict
        new AssetDetailsFilter(quote.paymentSharedStateTracker()),
        // Fail-fast if max packet amount is 0
        new MaxPacketAmountFilter(quote.paymentSharedStateTracker().getMaxPacketAmountTracker()),
        // Limit how frequently packets are sent and early return
        new PacingFilter(quote.paymentSharedStateTracker().getPacingTracker()),
        new AmountFilter(quote.paymentSharedStateTracker()),
        new ExchangeRateFilter(quote.paymentSharedStateTracker().getExchangeRateTracker())
        // TODO: PendingRequestTracker?
      );

      return new RunLoop(link, streamPacketFilters, streamEncryptionUtils, quote.paymentSharedStateTracker())
        .start(quote);
    }

    //////////////////////
    // Visible for Testing
    //////////////////////

    /**
     * Merely constructs a new instance of {@link ExchangeRateProber} whenever getQuote is called. Exists for mocking
     * during tests in order to simulate various exchange-rate scenarios.
     *
     * @return
     */
    @VisibleForTesting
    protected ExchangeRateProber newExchangeRateProber() {
      return new ExchangeRateProber.Default(streamEncryptionUtils, link);
    }

    /**
     * Constructs a new instance of {@link StreamConnection}. Exists for mocking during tests in order to simulate
     * various exchange-rate scenarios.
     *
     * @param paymentOptions          A {@link PaymentOptions}.
     * @param streamConnectionDetails A {@link StreamConnectionDetails}.
     *
     * @return
     */
    @VisibleForTesting
    protected StreamConnection newStreamConnection(
      final PaymentOptions paymentOptions, final StreamConnectionDetails streamConnectionDetails
    ) {
      Objects.requireNonNull(paymentOptions);
      Objects.requireNonNull(streamConnectionDetails);
      return new StreamConnection(
        paymentOptions.senderAccountDetails(),
        // SPSP Details.
        streamConnectionDetails.destinationAddress(),
        streamConnectionDetails.sharedSecret()
        // No dest denomination known yet.
      );
    }

    //////////////////
    // Private Helpers
    //////////////////

    @VisibleForTesting
    protected ExchangeRate getExchangeRate(PaymentOptions paymentOptions, ExchangeRateProbeOutcome rateProbeOutcome) {

      try {
        ExchangeRate exchangeRate = this.oracleExchangeRateProvider.getExchangeRate(
          paymentOptions.senderAccountDetails().denomination()
            .orElseThrow(() -> new StreamPayerException(
              String.format("SourceAccount denomination is required for FX. sourceDenomination=%s",
                rateProbeOutcome.destinationDenomination()), SendState.UnknownSourceAsset)).assetCode(),
          rateProbeOutcome.destinationDenomination()
            .orElseThrow(() -> new StreamPayerException(String.format(
              "Receiver denomination is required for FX (Receiver never shared asset details). "
                + "destinationDenomination=%s",
              rateProbeOutcome.destinationDenomination()), SendState.UnknownDestinationAsset)).assetCode()
        );

        if (exchangeRate.getFactor().numberValue(BigDecimal.class).compareTo(BigDecimal.ZERO) <= 0) {
          throw new StreamPayerException(String.format(
            "External exchange rate was 0. paymentOptions=%s rateProbeOutcoime=%s",
            paymentOptions, rateProbeOutcome), SendState.ExternalRateUnavailable
          );
        }

        return exchangeRate;
      } catch (StreamPayerException spe) {
        throw spe;
      } catch (Exception e) {
        throw new StreamPayerException(String.format(
          "No rate found in oracleExchangeRateProvider. paymentOptions=%s rateProbeOutcome=%s",
          paymentOptions, rateProbeOutcome
        ), e, SendState.ExternalRateUnavailable
        );
      }
    }

    /**
     * <p>Given an exchange-rate (representing the number of source units in a single destination unit), this methods
     * constructs a new exchange-rate that accounts for the scale of both the source and destination accounts (see more
     * about "scale" directly below). For example, given an FX rate of 1, a source scale of 3 and a destination scale of
     * 0, this method will return the number 300, which represents the rate to multiple one source unit by in order to
     * arrive at the proper number of destination units.</p>
     *
     * <p>Scale is defined as the difference in orders of magnitude between a `standard unit` and a corresponding
     * `fractional unit`. More formally, the asset scale is a non-negative integer (0, 1, 2, …) such that one `standard
     * unit` equals `10^(-scale)` of a corresponding `fractional unit`. If the fractional unit equals the standard unit,
     * then the asset scale is 0.</p>
     *
     * <p>For example, if an asset is denominated in U.S. Dollars, then the standard unit will be a "dollar." To
     * represent a fractional unit such as "cents", a scale of 2 must be used, where 100 cents equals 1.00 dollars.</p>
     *
     * @param rate
     * @param sourceAssetScale
     * @param destinationAssetScale
     *
     * @return
     */
    @VisibleForTesting
    protected Ratio shiftRateForNormalization(
      final Ratio rate, final short sourceAssetScale, final short destinationAssetScale
    ) {
      Preconditions.checkArgument(sourceAssetScale >= 0);
      Preconditions.checkArgument(destinationAssetScale >= 0);

      if (destinationAssetScale > sourceAssetScale) // <-- Move decimal left
      {
        // To move the decimal left, multiply the denominator by the 10 raised to the delta power.
        final int delta = destinationAssetScale - sourceAssetScale;
        return Ratio.builder().from(rate)
          .denominator(rate.denominator().multiply(BigInteger.TEN.pow(delta)))
          .build();
      } else { // <-- Move decimal left (even if 0 spaces)
        // To move the decimal right, multiply the numerator by the 10 raised to the delta power.
        final int delta = sourceAssetScale - destinationAssetScale;
        return Ratio.builder().from(rate)
          .numerator(rate.numerator().multiply(BigInteger.TEN.pow(delta)))
          .build();
      }
    }

    /**
     * Given an external exchange rate found in {@code externalExchangeRate}, construct a scaled exchange rate, which is
     * simply a number that is scaled for use by a destination denomination. For example, imagine an ExchangeRate
     * (assumed to be in scale of 0) of 0.25. Representing this rate in the scale of the destination currency can be
     * done via the following formula:
     * <p/>
     * <pre>externalRate * (10 ^ (destinationAccount.assetScale - sourceAccount.assetScale))</pre>
     * <p/>
     * Per the above, given an XRP/USD rate of 0.25, then the scaled rate would be 25 for a source account with a scale
     * of 0 and a destination account with scale 2.
     *
     * @param sourceAccountDetails      The {@link AccountDetails} for the source account.
     * @param destinationAccountDetails The {@link AccountDetails} for the destination account.
     * @param externalExchangeRate      The {@link InterledgerPreparePacket} being routed.
     * @param slippage                  The {@link Slippage} allowed for this rate.
     *
     * @return An {@link ScaledExchangeRate} representing the scaled exchange rate in the correct units for the
     *   destination account.
     */
    @VisibleForTesting
    protected ScaledExchangeRate determineScaledExternalRate(
      final AccountDetails sourceAccountDetails,
      final AccountDetails destinationAccountDetails,
      final ExchangeRate externalExchangeRate,
      final Slippage slippage
    ) {
      Objects.requireNonNull(destinationAccountDetails);
      Objects.requireNonNull(externalExchangeRate);
      Objects.requireNonNull(slippage);

      final short sourceAccountAssetScale = sourceAccountDetails.denomination()
        .orElseThrow(() -> new StreamPayerException(
          String.format("sourceAccountDetails has no denomination. sourceAccountDetails=%s", sourceAccountDetails),
          SendState.UnknownSourceAsset))
        .assetScale();

      final short destinationAccountAssetScale = destinationAccountDetails.denomination()
        .orElseThrow(() -> new StreamPayerException(
          String.format("destinationAccountDetails has no denomination. destinationAccountDetails=%s",
            destinationAccountDetails), SendState.UnknownSourceAsset))
        .assetScale();

      // The external rate is considered to be in scale == 0. Therefore, to arrive at a scaled rate for the destination,
      // we can simply use the destination scale, and scale the rate that way.
      final Ratio scaledExternalRate = Ratio.from(
        externalExchangeRate.getFactor()
          .numberValueExact(BigDecimal.class)
          .movePointLeft(sourceAccountAssetScale)
          .movePointRight(destinationAccountAssetScale)
      );

      return ScaledExchangeRate.builder()
        .value(scaledExternalRate)
        .originalInputScale(destinationAccountAssetScale)
        .slippage(slippage)
        .build();
    }

    /**
     * Ensure that the receiver has return a {@link Denomination} so that we can properly compute fx-rates.
     *
     * @param accountDetails
     */
    @VisibleForTesting
    protected Denomination checkForSourceDenomination(final AccountDetails accountDetails) {
      Objects.requireNonNull(accountDetails);

      // Destination asset may not be known until now if it was shared over STREAM vs application layer. However,
      // we must have a denomination in order to process the payment, so check this.
      return accountDetails.denomination()
        .orElseThrow(() ->
          new StreamPayerException(
            "Payment not started. Source denomination was not present.", SendState.UnknownSourceAsset
          ));
    }

    /**
     * Ensure that the receiver has return a {@link Denomination} so that we can properly compute fx-rates.
     *
     * @param streamConnection
     * @param exchangeRateProber
     */
    @VisibleForTesting
    protected Denomination checkForReceiverDenomination(
      final StreamConnection streamConnection, final ExchangeRateProber exchangeRateProber
    ) {
      Objects.requireNonNull(streamConnection);
      Objects.requireNonNull(exchangeRateProber);

      // Destination asset may not be known until now if it was shared over STREAM vs application layer. However,
      // we must have a denomination in order to process the payment, so check this.
      return getDestinationAccountDetails(streamConnection, exchangeRateProber)
        .denomination()
        .orElseThrow(() ->
          new StreamPayerException(
            "Payment not started. Receiver never shared destination asset details",
            SendState.UnknownDestinationAsset
          ));
    }

    /**
     * Ensure that the receiver has return a {@link Denomination} so that we can properly compute fx-rates.
     *
     * @param streamConnection
     * @param exchangeRateProber
     */
    @VisibleForTesting
    protected AccountDetails getDestinationAccountDetails(
      final StreamConnection streamConnection, final ExchangeRateProber exchangeRateProber
    ) {
      Objects.requireNonNull(streamConnection);
      Objects.requireNonNull(exchangeRateProber);

      // Destination asset may not be known until now if it was shared over STREAM vs application layer. However,
      // we must have a denomination in order to process the payment, so check this.
      return exchangeRateProber.getPaymentSharedStateTracker(streamConnection)
        .map(PaymentSharedStateTracker::getAssetDetailsTracker)
        .map(AssetDetailsTracker::getDestinationAccountDetails)
        .orElseThrow(() -> new StreamPayerException(String.format(
          "No PaymentSharedStateTracker found for streamConnection=%s", streamConnection
        ), SendState.Disconnected
        ));
    }

    /**
     * Sanity check to ensure sender and receiver use the same network/prefix
     *
     * @param sourceAccountDetails    A {@link AccountDetails} for the source of this stream payment.
     * @param streamConnectionDetails A {@link StreamConnectionDetails} for this stream payment.
     */
    private void validateAllocationSchemes(
      final AccountDetails sourceAccountDetails, final StreamConnectionDetails streamConnectionDetails
    ) {
      Objects.requireNonNull(sourceAccountDetails);
      Objects.requireNonNull(streamConnectionDetails);

      // A `private` sender can send to any destination. Otherwise, the source & destination must match.
      if (sourceAccountDetails.interledgerAddress().getAllocationScheme() != AllocationScheme.PRIVATE &&
        sourceAccountDetails.interledgerAddress().getAllocationScheme() !=
          streamConnectionDetails.destinationAddress().getAllocationScheme()
      ) {
        throw new StreamPayerException(
          String.format("Quote failed: incompatible sender/receiver address schemes. "
              + "sourceScheme=%s destinationScheme=%s "
              + "sourceAccountDetails=%s streamConnectionDetails=%s",
            sourceAccountDetails.interledgerAddress().getAllocationScheme().getValue(),
            streamConnectionDetails.destinationAddress().getAllocationScheme().getValue(),
            sourceAccountDetails, streamConnectionDetails
          ),
          SendState.IncompatibleInterledgerNetworks
        );
      }
    }

    /**
     * Validate that the target amount is non-zero and compatible with the precision of the accounts
     */
    private BigInteger obtainValidatedAmountToSend(
      final PaymentOptions paymentOptions
    ) {

      Objects.requireNonNull(paymentOptions);

      final AccountDetails sourceAccountDetails = paymentOptions.senderAccountDetails();

      if (FluentCompareTo.is(paymentOptions.amountToSend()).lessThan(BigDecimal.ZERO)) {
        throw new StreamPayerException(
          "Payment source amount must be a non-negative amount.", SendState.InvalidSourceAmount
        );
      }

      final Denomination sourceDenomination = sourceAccountDetails.denomination().orElseThrow(
        () -> new StreamPayerException(
          "Source account denomination is required to make a payment.", SendState.UnknownSourceAsset)
      );
      try {
        // Shift the amountToSend by the source account's asset scale to turn it into a whole number.
        BigInteger scaledSendAmount = paymentOptions.amountToSend()
          .movePointRight(sourceDenomination.assetScale()).toBigIntegerExact();
        return scaledSendAmount;
      } catch (Exception e) {
        // TODO: Unit test this.
        throw new StreamPayerException("Invalid source scale", e, SendState.InvalidSourceAmount);
      }
    }

    private StreamConnectionDetails fetchRecipientAccountDetails(final PaymentOptions paymentOptions) {
      Objects.requireNonNull(paymentOptions);

      // Note: Some SPSP endpoints return the asset details as part of their response. This implementation ignores those
      // values, preferring to obtain this information at the STREAM layer instead.
      try {
        return spspClient.getStreamConnectionDetails(paymentOptions.destinationPaymentPointer());
      } catch (Exception e) {
        throw new StreamPayerException(
          "Unable to obtain STREAM connection details via SPSP.", e, SendState.QueryFailed
        );
      }
    }
  }
}
