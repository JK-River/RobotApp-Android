package com.kobot.lib.utils;

import java.io.*;

/**
 * Created by machao on 2015/6/11.
 */
public class ObjectCache<T> {
  private String filePath;

  public ObjectCache(String filePath) {
    this.filePath = filePath;
  }

  public void saveObjectListToFile(T object) {
    ObjectOutputStream out = null;

    try {
      File file = new File(filePath);
      if (!file.exists()) {
        file.createNewFile();
      }
      if (file.exists()) {
        out = new ObjectOutputStream(new FileOutputStream(this.filePath));
        out.writeObject(object);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (out != null) {
          out.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @SuppressWarnings("unchecked")
  public T loadObjectListFromFile() {
    ObjectInputStream in = null;
    Object list = null;
    try {
      File file = new File(filePath);
      if (file.exists()) {
        in = new ObjectInputStream(new FileInputStream(filePath));
        list = in.readObject();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return (T) list;
  }
}

