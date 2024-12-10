package com.example;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.*;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.stream.ClosedShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.*;

import java.util.Random;
import java.util.concurrent.CompletionStage;


public class Main extends AbstractBehavior<Main.SayHello> {

  private static final Random rand = new Random(System.currentTimeMillis());

  public static void main(String[] args) throws Exception {
    
    final ActorSystem<SayHello> system =
        ActorSystem.create(Main.create(), "hello");

    system.tell(new Main.SayHello("World"));
    system.tell(new Main.SayHello("Akka"));

    Source<Integer, NotUsed> source = Source.range(1, 100000).named("mysource");

    // Three sinks for fan-out
    Sink<Integer, CompletionStage<Done>> sink1 = Sink.foreach(i -> {
      if(i % 10 == 0) System.out.println("0) " + i + ", ");
      else System.out.print("0) " + i + ", ");
    });
    Sink<Integer, CompletionStage<Done>> sink2 = Sink.foreach(i -> {
      if(i % 10 == 0) System.out.println("1) " + i + ", ");
      else System.out.print("1) " + i + ", ");
    });
    Sink<Integer, CompletionStage<Done>> sink3 = Sink.foreach(i -> {
      if(i % 10 == 0) System.out.println("2) " + i + ", ");
      else System.out.print("2) " + i + ", ");
    });

    Flow<Integer, Integer, NotUsed> flow1 = myFlow("flow0");
    Flow<Integer, Integer, NotUsed> flow2 = myFlow("flow1");
    Flow<Integer, Integer, NotUsed> flow3 = myFlow("flow2");

    RunnableGraph<NotUsed> graph = RunnableGraph.fromGraph(
            GraphDSL.create(builder -> {
              final UniformFanOutShape<Integer, Integer> broadcast = builder.add(Broadcast.create(3));

              // Connect the source to the broadcast
              builder.from(builder.add(source)).toFanOut(broadcast);

              // Connect each branch: flow -> sink
              builder.from(broadcast.out(0)).via(builder.add(flow1)).to(builder.add(sink1.named("sink0")));
              builder.from(broadcast.out(1)).via(builder.add(flow2)).to(builder.add(sink2.named("sink1")));
              builder.from(broadcast.out(2)).via(builder.add(flow3)).to(builder.add(sink3.named("sink2")));

              return ClosedShape.getInstance();
            })
    );

    // Run the graph
    graph.named("bigGraph").run(system);

    Thread.sleep(120000);
    system.terminate();
  }

  private static Flow<Integer, Integer, NotUsed> myFlow(String name) {
    return Flow.of(Integer.class).named(name).map(Main::parseLine);
  }

  private static Integer parseLine(Integer line) throws InterruptedException {
    Thread.sleep(rand.nextInt(20));
    return line + 1;
  }

  public record SayHello(String name) {}

  public static Behavior<SayHello> create() {
    return Behaviors.setup(Main::new);
  }

  private final ActorRef<HelloWorld.Greet> greeter;

  private Main(ActorContext<SayHello> context) {
    super(context);
    greeter = context.spawn(HelloWorld.create(), "greeter");
  }

  @Override
  public Receive<SayHello> createReceive() {
    return newReceiveBuilder().onMessage(SayHello.class, this::onSayHello).build();
  }

  private Behavior<SayHello> onSayHello(SayHello command) {
    ActorRef<HelloWorld.Greeted> replyTo =
        getContext().spawn(HelloWorldBot.create(3), command.name);
    greeter.tell(new HelloWorld.Greet(command.name, replyTo));
    return this;
  }
}

