package org.interledger.codecs.stream.frame.helpers;

import org.interledger.codecs.stream.StreamPacketFixturesTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Properties;

/**
 * This is a helper class containing methods necessary to manage the StreamPacketFixturesTest dependencies.
 */
public class FixtureManager {
  /**
   * This static method reads the {@code fixtures.properties} file in the resources directory making the values
   * available for lookup by the corresponding keys in the properties file. The {@code fixtures.properties} file is
   * used to maintain metadata information regarding the URL to lookup the fixtures in the RFC repository along with
   * the filename and the integrity using SHA-256 for the file.
   * @return {@link Properties}
   */
  public static Properties readProperties() {
    Properties properties = new Properties();
    String propertiesConfiguration = "fixtures.properties";
    InputStream streamPacketFixturesContent =
        StreamPacketFixturesTest.class.getClassLoader().getResourceAsStream(propertiesConfiguration);
    if (streamPacketFixturesContent != null) {
      try {
        properties.load(streamPacketFixturesContent);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return properties;
  }

  /**
   * This method takes in the byte array and computes the SHA-256 checksum on the byte array.
   * @param in Input Bytes to compute the digest.
   * @return byte[]
   */
  public static byte[] checksum(byte[] in) {
    Objects.requireNonNull(in);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(in);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return new byte[0];
    }
  }

  /**
   * This method helps download the fixture file from the location in the RFC repository and saves the filename at
   * {@code /tmp/filename.ext} given the URL and the name of the output file.
   * @param urlString The {@link String} URL where the fixtures file is located for download.
   * @param filename The {@link String} name for the target download file. The default base path is {@code /tmp/}
   * @return byte[] containing the bytes of the files which is persisted at {@code /tmp/} and read again.
   */
  public static byte[] downloadFixtureFile(String urlString, String filename) {
    Objects.requireNonNull(urlString);
    Objects.requireNonNull(filename);
    String basePath = "/tmp/";
    Path filePath = Paths.get(basePath + filename);
    try (InputStream in = URI.create(urlString).toURL().openStream()) {
      Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      return Files.readAllBytes(filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new byte[0];
  }

  /**
   * This method helps test if the machine running the test has Internet connectivity.
   * @return boolean {@code true} if the machine has an active Internet connection, else {@code false}.
   */
  public static boolean checkInternetConnectivity() {
    final int timeout = 5000;
    Socket socket = new Socket();
    InetSocketAddress address = new InetSocketAddress("google.com", 80);
    try {
      socket.connect(address, timeout);
      if (socket.isConnected()) {
        return true;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return false;
  }
}
