package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;


public class Main extends AbstractBehavior<Main.SayHello> {
  

  public static void main(String[] args) throws Exception {
    
    final ActorSystem<SayHello> system =
        ActorSystem.create(Main.create(), "hello");

    system.tell(new Main.SayHello("World"));
    system.tell(new Main.SayHello("Akka"));
    

    Thread.sleep(3000);
    system.terminate();
  }
  

  public static record SayHello(String name) {}

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

