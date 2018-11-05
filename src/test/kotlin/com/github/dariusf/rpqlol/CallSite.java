package com.github.dariusf.rpqlol;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CallSite {

  @JsonProperty
  private final Method caller;

  @JsonProperty
  private final Method callee;

  @JsonCreator
  public CallSite(@JsonProperty("caller") Method caller,
                  @JsonProperty("callee") Method callee) {
    this.caller = caller;
    this.callee = callee;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CallSite callSite = (CallSite) o;
    return Objects.equals(caller, callSite.caller) &&
        Objects.equals(callee, callSite.callee);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caller, callee);
  }

  @Override
  public String toString() {
    return "CallSite{" +
        "caller=" + caller +
        ", callee=" + callee +
        '}';
  }

  public Method getCaller() {
    return caller;
  }

  public Method getCallee() {
    return callee;
  }
}


