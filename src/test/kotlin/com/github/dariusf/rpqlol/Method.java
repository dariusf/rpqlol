package com.github.dariusf.rpqlol;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Method {

  @JsonProperty
  private final String className;

  @JsonProperty
  private final String methodName;

  @JsonProperty
  private final String desc;

  @JsonCreator
  Method(@JsonProperty("className") String className,
         @JsonProperty("methodName") String methodName,
         @JsonProperty("desc") String desc) {
    this.className = className;
    this.methodName = methodName;
    this.desc = desc;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Method method = (Method) o;
    return Objects.equals(className, method.className) &&
        Objects.equals(methodName, method.methodName) &&
        Objects.equals(desc, method.desc);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, methodName, desc);
  }


  @Override
  public String toString() {
    return "Method{" +
        "className='" + className + '\'' +
        ", methodName='" + methodName + '\'' +
        ", desc='" + desc + '\'' +
        '}';
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getDesc() {
    return desc;
  }
}


