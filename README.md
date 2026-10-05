# SEC API

Java library for SEC EDGAR data.

SEC API gives you typed Java access to data from the [U.S. Securities and Exchange Commission (SEC)](https://www.sec.gov/) through its EDGAR system: registered companies, filing references, and fully parsed insider ownership documents. It handles EDGAR's rate limits and retries for you.

## Features

- **Companies:** load all SEC-registered companies that have a stock ticker, with their CIK, name, ticker, and exchange.
- **Filing references:** find filings from the latest feed, from daily indexes, for a whole fiscal quarter, or for a single company. Filter by form type: 3, 4, 5 (and their amendments), 10-Q, 10-K, and 8-K.
- **Ownership documents:** parse insider Forms 3, 4, and 5 into Java objects, including the issuer, reporting owners, transactions, holdings, and footnotes.
- **Built-in rate limiting:** stays within EDGAR's limit of 10 requests per second, makes up to 3 attempts per request, and backs off automatically when EDGAR throttles you.

## Requirements

- Java 17 or newer
- Jackson (`jackson-databind`, `jackson-dataformat-xml`, `jackson-datatype-jsr310`)

## Installation

Build the JAR from source:

```bash
git clone https://github.com/alexkouzel/sec-api.git
cd sec-api
./gradlew jar
```

The JAR is written to `build/libs/`. Add it to your project together with the Jackson dependencies listed above.

## Usage

### 1. Create a client

EDGAR requires every request to identify who is making it, so create a client with a user agent in the format `Company Name contact@company.com`:

```java
EdgarClient client = new EdgarClient("Sample Company admin@example.com");
```

### 2. Load companies

```java
SecCompanyLoader companyLoader = new SecCompanyLoader(client);

List<SecCompany> companies = companyLoader.load();
```

### 3. Load filing references

```java
FileRefLoader fileRefLoader = new FileRefLoader(client);

// All filings from Q3 2023
List<FileRef> quarter = fileRefLoader.loadFiscalQuarter(2023, 3);

// Today's filings
List<FileRef> today = fileRefLoader.loadToday();

// Form 4 filings from 3 days ago
List<FileRef> daysAgo = fileRefLoader.loadDaysAgo(3, FileType.F4);

// The 100 latest Form 4 filings
List<FileRef> latest = fileRefLoader.loadLatest(LatestFilesLimit.HUNDRED, FileType.F4);

// All filings for Tesla (CIK 1318605)
List<FileRef> tesla = fileRefLoader.loadCompany(1318605);
```

Each `FileRef` contains the accession number, issuer CIK, form type, and filing date.

### 4. Load ownership documents (Forms 3, 4, 5)

```java
FileLoader fileLoader = new FileLoader(client);

// From a filing reference
FileF345 form = fileLoader.loadF345ByRef(latest.get(0));

// From a URL
String url = FileUrlBuilder.buildTxt(1318605, "0001972928-24-000002");
FileF345 formByUrl = fileLoader.loadF345ByUrl(url);
```

### Error handling

Loaders throw `HttpRequestException` when a request still fails after 3 attempts, and `ParsingException` when a response can't be parsed.

## Running tests

```bash
./gradlew test
```
