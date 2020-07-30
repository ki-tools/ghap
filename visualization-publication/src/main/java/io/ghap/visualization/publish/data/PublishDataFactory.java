package io.ghap.visualization.publish.data;

import java.io.File;
import java.io.InputStream;

/**
 * Created by snagy on 9/30/15.
 */
public interface PublishDataFactory {

  AppPublishResult publish(File file, String type, String keyName, String meta);
  void prepare(String keyName);
  String registry(String url);
  InputStream image(String url);
}
