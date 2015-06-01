package org.bufio;

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.assertArrayEquals;

public class AbstractCsvScannerTest {
	@Test
	public void test() throws IOException {
		CsvRecordScanner s = new CsvRecordScanner(new StringReader("1,ID1,1.1\n2,ID2,\n3,ID3,0.3"));
		CsvRecord[] records = new CsvRecord[3];
		int i = 0;
		while (s.scan()) {
			if (s.atEndOfRow()) {
				records[i++] = s.record;
			}
		}
		assertArrayEquals(new CsvRecord[]{
				new CsvRecord(1, "ID1", 1.1), new CsvRecord(2, "ID2", 0), new CsvRecord(3, "ID3", 0.3)}, records);
	}

	private static class CsvRecordScanner extends AbstractCsvScanner<CsvRecord> {
		private CsvRecord record;
		private CsvRecordScanner(Reader r) {
			super(r);
		}

		@Override
		protected CsvRecord newToken(char[] data, int start, int end) {
			final String s;
			if (start == end) {
				s = "";
			} else {
				s = new String(data, start, end - start);
			}
			final int col = column();
			if (col == 1) {
				record = new CsvRecord();
				record.code = Integer.parseInt(s);
			} else if (col == 2) {
				record.name = s;
			} else if (col == 3) {
				record.value = s.isEmpty() ? 0 : Double.parseDouble(s);
			}
			return record;
		}
	}

	private static class CsvRecord {
		private int code;
		private String name;
		private double value;

		public CsvRecord(int code, String name, double value) {
			this.code = code;
			this.name = name;
			this.value = value;
		}

		public CsvRecord() {
		}

		@Override
		public String toString() {
			return "CsvRecord{" +
					"code=" + code +
					", name='" + name + '\'' +
					", value=" + value +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CsvRecord csvRecord = (CsvRecord) o;

			if (code != csvRecord.code) return false;
			if (Double.compare(csvRecord.value, value) != 0) return false;
			if (!name.equals(csvRecord.name)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = code;
			result = 31 * result + name.hashCode();
			temp = Double.doubleToLongBits(value);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
	}
}