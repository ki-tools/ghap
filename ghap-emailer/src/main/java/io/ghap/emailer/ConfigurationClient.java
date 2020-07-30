package io.ghap.emailer;

import io.ghap.emailer.data.*;

import java.util.List;

public class ConfigurationClient {
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
          System.err.println("Usage --list <property_name>");
          System.exit(0);
        }
      }
      else if(args[argCounter].equals("--remove")) {
        if(args.length == argCounter+2) {
          command = new RemoveCommand(args[argCounter + 1]);
          break;
        } else {
          System.err.println("Usage --remove <property_name>");
          System.exit(0);
        }
      } else if(args[argCounter].equals("--add")) {
        if(args.length == argCounter+3) {
          command = new AddCommand(args[argCounter + 1], args[argCounter +2]);
          break;
        } else {
          System.err.println("Usage --add <property_name> <property_value>");
          System.exit(0);
        }
      }
    }
    if(command == null) {
      System.err.println("You must specify mode via one of the following command line flags: --list, ---list-all, --add, --remove");
      System.exit(0);
    }
    command.execute();
  }

  interface Command {
    void execute();
  }

  private static class ListCommand implements Command {
    private String property_name;
    ListCommand(String property_name) {
      this.property_name = property_name;
    }

    public void execute() {
      ConfigurationFactory factory = new ConfigurationFactoryImpl();
      Configuration configuration = factory.getConfiguration(property_name);
      System.out.println(String.format("The value of %s is %s",configuration.getPropertyName(),configuration.getPropertyValue()));
    }
  }

  private static class RemoveCommand implements Command {
    private String property_name;
    RemoveCommand(String property_name) {
      this.property_name = property_name;
    }

    public void execute() {
      ConfigurationFactory factory = new ConfigurationFactoryImpl();
      factory.removeConfiguration(property_name);
      System.out.println(String.format("%s has been removed.", property_name));
    }
  }

  private static class AddCommand implements Command {
    private String property_name;
    private String property_value;

    AddCommand(String property_name, String property_value) {
      this.property_name = property_name;
      this.property_value = property_value;
    }

    public void execute() {
      ConfigurationFactory factory = new ConfigurationFactoryImpl();
      factory.setConfiguration(property_name,property_value);
      System.out.println(String.format("%s has been added.", property_name));
    }
  }

  private static class ListAllCommand implements Command {
    ListAllCommand() {
    }

    public void execute() {
      ConfigurationFactory factory = new ConfigurationFactoryImpl();
      List<Configuration> configs = factory.getConfigurations();
      for(Configuration config: configs) {
        new ListCommand(config.getPropertyName()).execute();
      }
    }
  }
}
