Yet another CSV reader/pull parser/stream parser with small memory usage.

All credit goes to:
* Rob Pike, creator of [Scanner](http://tip.golang.org/pkg/bufio/#Scanner) interface,
* D. Richard Hipp, for his [CSV parser](http://www.sqlite.org/cgi/src/artifact/6276582ee4e9114e) implementation.

[![Build Status](https://travis-ci.org/gwenn/scanner.svg)](https://travis-ci.org/gwenn/scanner)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.gwenn/scanner.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.gwenn%22%20AND%20a:%22scanner%22)
[![Javadocs](https://www.javadoc.io/badge/com.github.gwenn/scanner.svg)](https://www.javadoc.io/doc/com.github.gwenn/scanner)

## Iterating over fields

```java
CsvScanner s;
while (s.scan())) {
  String value = s.value();
  // ...
  if (s.atEndOfRow()) {
    // ...
  }
}
```

## Iterating over records

```java
CsvReader r;
while (r.next()) {
  String value1 = r.getString(1);
  // ...
}
```

## Round Tripping

```java
CsvWriter w;
CsvScanner s;
while (s.scan())) {
  w.write(s.value());
  if (s.atEndOfRow()) {
    w.endOfRow();
  }
}
w.flush();
```

or

```java
CsvWriter w;
CsvReader r;
while (r.next()) {
  w.writeRow(r.values());
}
w.flush();
```

LICENSE
-------
Public Domain

