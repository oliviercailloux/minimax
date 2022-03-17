package io.github.oliviercailloux.minimax.experiment.other_formats;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Stats;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import io.github.oliviercailloux.minimax.experiment.Runs;

public class ToCsv {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(ToCsv.class);

  public static String toCsv(Runs runs, int modulo) {
    checkArgument(modulo >= 1);
    final ImmutableList<Integer> ks = IntStream.rangeClosed(0, runs.getK())
        .filter(i -> i % modulo == 0).boxed().collect(ImmutableList.toImmutableList());

    final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
    formatter.setMaximumFractionDigits(2);

    final StringWriter stringWriter = new StringWriter();
    final CsvWriter writer = new CsvWriter(stringWriter, new CsvWriterSettings());
    writer.writeHeaders("k", "MMR min", "MMR avg", "MMR max", "MMR σ (est.)", "Loss min",
        "Loss avg", "Loss max", "Loss σ (est.)");
    for (int k : ks) {
      writer.addValue("k", k);
      {
        final Stats stat = runs.getMinimalMaxRegretStats().get(k);
        writer.addValue("MMR min", formatter.format(stat.min()));
        writer.addValue("MMR avg", formatter.format(stat.mean()));
        writer.addValue("MMR max", formatter.format(stat.max()));
        final String dev =
            stat.count() >= 2 ? formatter.format(stat.sampleStandardDeviation()) : "";
        writer.addValue("MMR σ (est.)", dev);
      }
      {
        final Stats stat = runs.getLossesStats().get(k);
        writer.addValue("Loss min", formatter.format(stat.min()));
        writer.addValue("Loss avg", formatter.format(stat.mean()));
        writer.addValue("Loss max", formatter.format(stat.max()));
        final String dev =
            stat.count() >= 2 ? formatter.format(stat.sampleStandardDeviation()) : "";
        writer.addValue("Loss σ (est.)", dev);
      }
      writer.writeValuesToRow();
    }
    return stringWriter.toString();
  }
}
