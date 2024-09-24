package org.example.cacheusedemo.controller;

/**
 * @author liuzw
 * @date 2024/6/21
 */
public class TestEx {
  private String id;
  private String name;

  public TestEx() {
  }

  public TestEx(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
