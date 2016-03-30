package org.bufio;

import com.google.common.base.Splitter;
import com.google.common.io.LineReader;
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

import java.io.BufferedReader;
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
		try (Reader r = new InputStreamReader(getClass().getResourceAsStream("/mhtdata.csv"), "UTF-8")) {
			CsvScanner s = new CsvScanner(r);
			while (s.scan()) {
				blackhole.consume(s.value());
			}
		}
	}

	@Benchmark
	public void testStringSplit(Blackhole blackhole) throws IOException {
		try (Reader r = new InputStreamReader(getClass().getResourceAsStream("/mhtdata.csv"), "UTF-8")) {
			BufferedReader s = new BufferedReader(r);
			String line;
			while ((line = s.readLine()) != null) {
				String[] values = line.split(",");
				for (String value : values) {
					blackhole.consume(value);
				}
			}
		}
	}

	@Benchmark
	public void testSplitter(Blackhole blackhole) throws IOException {
		try (Reader r = new InputStreamReader(getClass().getResourceAsStream("/mhtdata.csv"), "UTF-8")) {
			LineReader s = new LineReader(r);
			Splitter splitter = Splitter.on(',');
			String line;
			while ((line = s.readLine()) != null) {
				for (String value : splitter.split(line)) {
					blackhole.consume(value);
				}
			}
		}
	}

}
