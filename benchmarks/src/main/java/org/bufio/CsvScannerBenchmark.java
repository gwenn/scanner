package org.bufio;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 5, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
public class CsvScannerBenchmark {
	@Benchmark
	public void testCsvScanner(Blackhole blackhole) throws IOException {
		Reader r = new InputStreamReader(getClass().getResourceAsStream("/mhtdata.csv"), "UTF-8");
		try {
			CsvScanner s = new CsvScanner(r);
			while (s.scan()) {
				blackhole.consume(s.value());
			}
		} finally {
			r.close();
		}
	}

}
