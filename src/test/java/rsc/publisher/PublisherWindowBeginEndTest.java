package rsc.publisher;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import rsc.processor.DirectProcessor;
import rsc.test.TestSubscriber;
import rsc.util.ConstructorTestBuilder;

public class PublisherWindowBeginEndTest {
    
    @Test
    public void constructors() {
        ConstructorTestBuilder ctb = new ConstructorTestBuilder(PublisherWindowBeginEnd.class);
        
        ctb.addRef("source", PublisherNever.instance());
        ctb.addRef("windowBegin", PublisherNever.instance());
        ctb.addRef("windowEnd", (Function<Object, Publisher<Object>>)v -> PublisherNever.instance());
        ctb.addRef("queueSupplier", (Supplier<Queue<Object>>)() -> new ConcurrentLinkedQueue<>());
        ctb.addInt("bufferSize", 1, Integer.MAX_VALUE);
        
        ctb.test();
    }
    
    @Test
    public void normalBackpressured() {
        
        TestSubscriber<Integer> ts2 = new TestSubscriber<>(0);
        TestSubscriber<Integer> ts3 = new TestSubscriber<>(0);

        TestSubscriber<Px<Integer>> ts1 = new TestSubscriber<Px<Integer>>(0) {
            int index;
            @Override
            public void onNext(Px<Integer> t) {
                if (index++ == 0) {
                    t.subscribe(ts2);
                } else {
                    t.subscribe(ts3);
                }
                super.onNext(t);
            }
        };

        DirectProcessor<Integer> sp2 = new DirectProcessor<>();
        DirectProcessor<Integer> sp3 = new DirectProcessor<>();
        
        new PublisherRange(1, 10).window2(new PublisherRange(1, 3), v -> v == 1 ? sp2 : sp3).subscribe(ts1);

        ts1.assertNoValues()
        .assertNoError()
        .assertNotComplete();

        
        ts1.request(1);
        
        ts1.assertValueCount(1)
        .assertNoError()
        .assertNotComplete();
        
        ts2.assertNoValues()
        .assertNoError()
        .assertNotComplete();
        
        ts3.assertNoValues()
        .assertNoError()
        .assertNotComplete();
        
        ts1.request(1);
        
        ts1.assertValueCount(2)
        .assertNoError()
        .assertNotComplete();
        
        ts2.request(1);

        ts2.assertNoValues()
        .assertNoError()
        .assertNotComplete();
        
        ts3.assertNoValues()
        .assertNoError()
        .assertNotComplete();
        
        ts3.request(2);

        ts2.assertValue(1)
        .assertNoError()
        .assertNotComplete();
        
        ts3.assertValue(1)
        .assertNoError()
        .assertNotComplete();
        
        ts1.cancel();
        
        ts2.request(2);
        ts3.request(1);

        ts2.assertValues(1, 2, 3)
        .assertNoError()
        .assertNotComplete();
        
        ts3.assertValues(1, 2, 3)
        .assertNoError()
        .assertNotComplete();
        
        sp2.onNext(1);
        
        ts2.assertValues(1, 2, 3)
        .assertNoError()
        .assertComplete();

        ts3.assertValues(1, 2, 3)
        .assertNoError()
        .assertNotComplete();
        
        ts3.request(10);

        ts2.assertValues(1, 2, 3)
        .assertNoError()
        .assertComplete();

        ts3.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoError()
        .assertComplete();
        
        ts1.assertNoError();
        
        Assert.assertFalse("sp2 has subscribers?", sp2.hasDownstreams());
        Assert.assertFalse("sp3 has subscribers?", sp3.hasDownstreams());
    }
    
    @Test
    public void empty() {
        TestSubscriber<Px<Integer>> ts1 = new TestSubscriber<>(0);
                
        DirectProcessor<Integer> sp1 = new DirectProcessor<>();
        DirectProcessor<Integer> sp2 = new DirectProcessor<>();
        DirectProcessor<Integer> sp3 = new DirectProcessor<>();
        
        PublisherEmpty.<Integer>instance().window2(sp1, v -> v == 1 ? sp2 : sp3).subscribe(ts1);
        
        ts1.assertNoValues()
        .assertNoError()
        .assertComplete();
        
        Assert.assertFalse("sp1 has subscribers?", sp1.hasDownstreams());
        Assert.assertFalse("sp2 has subscribers?", sp2.hasDownstreams());
        Assert.assertFalse("sp3 has subscribers?", sp3.hasDownstreams());
    }
}
