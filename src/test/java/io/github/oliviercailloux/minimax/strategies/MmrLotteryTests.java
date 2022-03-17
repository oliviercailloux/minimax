package io.github.oliviercailloux.minimax.strategies;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.minimax.elicitation.Question;

public class MmrLotteryTests {

  @Test
  public void test() {

    Comparator<MmrLottery> compMax = MmrLottery.MAX_COMPARATOR;

    assertTrue(compMax.compare(MmrLottery.given(10, 8), MmrLottery.given(8, 6)) > 0); // max1>max2
    assertTrue(compMax.compare(MmrLottery.given(6, 8), MmrLottery.given(10, 6)) < 0); // max1<max2
    assertTrue(compMax.compare(MmrLottery.given(10, 8), MmrLottery.given(8, 10)) == 0); // max1=max2
                                                                                        // &
                                                                                        // min1=min2
    assertTrue(compMax.compare(MmrLottery.given(10, 8), MmrLottery.given(6, 10)) > 0); // max1=max2
                                                                                       // &
                                                                                       // min1>min2
    assertTrue(compMax.compare(MmrLottery.given(10, 4), MmrLottery.given(6, 10)) < 0); // max1=max2
                                                                                       // &
                                                                                       // min1<min2

    Comparator<MmrLottery> compMin = MmrLottery.MIN_COMPARATOR;

    assertTrue(compMin.compare(MmrLottery.given(10, 8), MmrLottery.given(8, 6)) > 0); // min1>min2
    assertTrue(compMin.compare(MmrLottery.given(6, 8), MmrLottery.given(10, 7)) < 0); // min1<min2
    assertTrue(compMin.compare(MmrLottery.given(10, 8), MmrLottery.given(8, 10)) == 0); // min1=min2
                                                                                        // &
                                                                                        // max1=max2
    assertTrue(compMin.compare(MmrLottery.given(10, 6), MmrLottery.given(6, 8)) > 0); // min1=min2 &
                                                                                      // max1>max2
    assertTrue(compMin.compare(MmrLottery.given(8, 6), MmrLottery.given(6, 10)) < 0); // min1=min2 &
                                                                                      // max1<max2
  }
}
