package io.github.oliviercailloux.minimax.strategies;

import java.util.Comparator;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;

public class MmrLottery {

  /**
   * MAX_COMPARATOR is pessimistic. It considers a question Q1 greater than a question Q2 if
   * (maxQ1>maxQ2) or (maxQ1=maxQ2 & minQ1>minQ2)
   *
   * Thus, the “smallest” question is the best one as we seek to minimize regret.
   *
   */
  public static final ComparatorWithDescription<MmrLottery> MAX_COMPARATOR = getMaxComparator();

  public static final ComparatorWithDescription<MmrLottery> MIN_COMPARATOR = getMinComparator();

  public static ComparatorWithDescription<MmrLottery>
      comparatorFromDescription(String description) {
    switch (description) {
      case "MAX":
        return MAX_COMPARATOR;
      case "MIN":
        return MIN_COMPARATOR;
      default:
        throw new VerifyException();
    }
  }

  public static MmrLottery given(double mmrIfYes, double mmrIfNo) {
    return new MmrLottery(mmrIfYes, mmrIfNo);
  }

  private static ComparatorWithDescription<MmrLottery> getMaxComparator() {
    /**
     * Comparing using pure lexicographic reasoning results in (1.0, 2.0) being considered lower
     * than (0.0, 2.0+1e-16), which we do not want (I have seen a similar situation with max MMRs
     * differing only at the 17th decimal).
     */
    return ComparatorWithDescription
        .given(Comparator.comparingDouble(l -> l.getWorstMmr() + (l.getBestMmr() / 1e6d)), "MAX");
  }

  private static ComparatorWithDescription<MmrLottery> getMinComparator() {
    return ComparatorWithDescription
        .given(Comparator.comparingDouble(l -> l.getBestMmr() + (l.getWorstMmr() / 1e6d)), "MIN");
  }

  private final double mmrIfYes;
  private final double mmrIfNo;

  private MmrLottery(double mmrIfYes, double mmrIfNo) {
    this.mmrIfYes = mmrIfYes;
    this.mmrIfNo = mmrIfNo;
  }

  public double getMmrIfYes() {
    return mmrIfYes;
  }

  public double getMmrIfNo() {
    return mmrIfNo;
  }

  /**
   * @return a maximal MMR among the two.
   */
  public double getWorstMmr() {
    return mmrIfYes >= mmrIfNo ? mmrIfYes : mmrIfNo;
  }

  /**
   * @return a minimal MMR among the two.
   */
  public double getBestMmr() {
    return mmrIfYes <= mmrIfNo ? mmrIfYes : mmrIfNo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mmrIfYes, mmrIfNo);
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof MmrLottery)) {
      return false;
    }

    final MmrLottery q2 = (MmrLottery) o2;
    return mmrIfYes == q2.mmrIfYes && mmrIfNo == q2.mmrIfNo;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("mmrIfYes", mmrIfYes).add("mmrIfNo", mmrIfNo)
        .toString();
  }
}
