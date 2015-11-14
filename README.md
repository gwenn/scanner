Yet another CSV reader/pull parser/stream parser with small memory usage.

All credit goes to:
* Rob Pike, creator of [Scanner](http://tip.golang.org/pkg/bufio/#Scanner) interface,
* D. Richard Hipp, for his [CSV parser](http://www.sqlite.org/cgi/src/artifact/6276582ee4e9114e) implementation.

[![Build Status](https://travis-ci.org/gwenn/scanner.svg)](https://travis-ci.org/gwenn/scanner)

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

