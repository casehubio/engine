package io.casehub.api.model;

public class PredicateBasedCompletion implements CaseCompletion {

  private final String doneWhen;

  private PredicateBasedCompletion(String doneWhen) {
    this.doneWhen = doneWhen;
  }

  public String getDoneWhen() {
    return doneWhen;
  }
}
