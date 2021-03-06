package rsc.publisher;

import java.util.Objects;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import rsc.subscriber.SubscriptionHelper;
import rsc.util.ExceptionHelper;

/**
 * Maps the downstream Subscriber into an upstream Subscriber
 * which allows implementing custom operators via lambdas.
 *
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 */
public final class PublisherLift<T, R> extends PublisherSource<T, R> {

    final Function<Subscriber<? super R>, Subscriber<? super T>> lifter;

    public PublisherLift(Publisher<? extends T> source, Function<Subscriber<? super R>, Subscriber<? super T>> lifter) {
        super(source);
        this.lifter = Objects.requireNonNull(lifter, "operator");
    }

    public Function<Subscriber<? super R>, Subscriber<? super T>> operator() {
        return lifter;
    }

    @Override
    public void subscribe(Subscriber<? super R> s) {

        Subscriber<? super T> ts;
        try {
            ts = lifter.apply(s);
        } catch (Throwable e) {
            ExceptionHelper.throwIfFatal(e);
            SubscriptionHelper.error(s, ExceptionHelper.unwrap(e));
            return;
        }

        if (ts == null) {
            SubscriptionHelper.error(s, new NullPointerException("The operator returned a null Subscriber"));
            return;
        }

        source.subscribe(ts);
    }
}
