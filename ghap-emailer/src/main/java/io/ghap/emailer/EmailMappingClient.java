package io.ghap.emailer;

import io.ghap.emailer.data.EmailFactory;
import io.ghap.emailer.data.EmailFactoryImpl;

import java.util.List;
import java.util.Set;

public class EmailMappingClient {
  public static void main(String[] args) {
    Command command = null;
    for(int argCounter = 0; argCounter < args.length; argCounter++) {
      if(args[argCounter].equals("--list-all")) {
        command = new ListAllCommand();
        break;
      } else if(args[argCounter].equals("--list")) {
        if(args.length == argCounter+2) { // this +1 for 1 based sizing
          command = new ListCommand(args[argCounter + 1]);
          break;
        } else {
          System.err.println("Usage --list <inbound email address to list outbound addresses for>");
          System.exit(0);
        }
      }
      else if(args[argCounter].equals("--remove-all")) {
        if(args.length == argCounter+2) { // this +1 for 1 based sizing
          command = new RemoveAllCommand(args[argCounter + 1]);
          break;
        } else {
          System.err.println("Usage --remove-all <inbound email address to remove all outbound addresses for>");
          System.exit(0);
        }
      } else if(args[argCounter].equals("--remove")) {
        if(args.length == argCounter+3) {
          command = new RemoveCommand(args[argCounter + 1], args[argCounter +2]);
          break;
        } else {
          System.err.println("Usage --remove <inbound email address> <outbound addresses>");
          System.exit(0);
        }
      } else if(args[argCounter].equals("--add")) {
        if(args.length == argCounter+3) {
          command = new AddCommand(args[argCounter + 1], args[argCounter +2]);
          break;
        } else {
          System.err.println("Usage --add <inbound email address> <outbound addresses>");
          System.exit(0);
        }
      }
    }
    if(command == null) {
      System.err.println("You must specify mode via one of the following command line flags: --list, ---list-all, --add, --remove --remove-all");
      System.exit(0);
    }
    command.execute();
  }

  interface Command {
    void execute();
  }

  private static class ListCommand implements Command {
    private String inbound;
    ListCommand(String inbound) {
      this.inbound = inbound;
    }

    public void execute() {
      EmailFactory factory = new EmailFactoryImpl();
      Set<String> outbound = factory.getMappedEmailAddresses(inbound);
      System.out.println(String.format("%s maps to the following email addresses:",inbound));
      for(String address: outbound) {
        System.out.println(String.format("\t%s", address));
      }
    }
  }

  private static class RemoveAllCommand implements Command {
    private String inbound;
    RemoveAllCommand(String inbound) {
      this.inbound = inbound;
    }

    public void execute() {
      EmailFactory factory = new EmailFactoryImpl();
      factory.removeAllMappedEmailAddresses(inbound);
      System.out.println(String.format("Removed all addresses mapped to %s.", inbound));
    }
  }

  private static class ListAllCommand implements Command {
    ListAllCommand() {
    }

    public void execute() {
      EmailFactory factory = new EmailFactoryImpl();
      List<String> inboundAddrs = factory.getAllMappedFromEmailAddresses();
      for(String inbound: inboundAddrs) {
        new ListCommand(inbound).execute();
      }
    }
  }


  private static class RemoveCommand implements Command {
    private String inbound;
    private String outbound;

    RemoveCommand(String inbound, String outbound) {
      this.inbound = inbound;
      this.outbound = outbound;
    }

    public void execute() {
      EmailFactory factory = new EmailFactoryImpl();
      factory.removeMappedEmailAddress(inbound, outbound);
      System.out.println(String.format("Removed %s from %s address mapping.", outbound, inbound));
    }
  }

  private static class AddCommand implements Command {
    private String inbound;
    private String outbound;

    AddCommand(String inbound, String outbound) {
      this.inbound = inbound;
      this.outbound = outbound;
    }

    public void execute() {
      EmailFactory factory = new EmailFactoryImpl();
      factory.addMapEmailAddress(inbound, outbound);
      System.out.println(String.format("%s added to %s address mapping.", outbound, inbound));
    }
  }
}
