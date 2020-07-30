package io.ghap.emailer.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EmailFactory {
  EmailMapping addMapEmailAddress(String from, String to);
  void removeMappedEmailAddress(String from, String to);
  void removeAllMappedEmailAddresses(String from);
  Set<String> getMappedEmailAddresses(String from);
  List<String> getAllMappedFromEmailAddresses();
  Map<String,Set<String>> getAllMappedEmailAddresses();
  void update(EmailMapping mapping);
  EmailMapping get(String from);
}
