package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;

public class ComparatorWithDescription<T> implements Comparator<T> {
  public static <T> ComparatorWithDescription<T> given(Comparator<T> delegate, String description) {
    return new ComparatorWithDescription<>(delegate, description);
  }

  private final Comparator<T> delegate;
  private final String description;

  private ComparatorWithDescription(Comparator<T> delegate, String description) {
    this.delegate = checkNotNull(delegate);
    this.description = checkNotNull(description);
  }

  @Override
  public int compare(T l1, T l2) {
    return delegate.compare(l1, l2);
  }

  @Override
  public String toString() {
    return description;
  }
}
