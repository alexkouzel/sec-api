# SEC API
This API allows you to access data from the [U.S. Securities and Exchange Commission (SEC)](https://www.sec.gov/), including:

- Information on public companies in the USA.
- Filing metadata for various types: 10-Q, 10-K, 4, 8-K, 13-F, etc.
- Ownership documents: F3, F4, and F5.

## Getting Started
To begin, create an HTTP client to access SEC EDGAR data:

```java
String userAgent = "TestCompany test.company@gmail.com";
EdgarClient client = new EdgarClient(userAgent);
```

The `userAgent` should follow this format:

```
Sample Company Name AdminContact@<sample company domain>.com
```

This client adheres to the guidelines, as specified in the SEC's [Accessing EDGAR Data](https://www.sec.gov/search-filings/edgar-search-assistance/accessing-edgar-data).

Next, you can use this client as follows: 

1. Load information about public companies in the USA:

```java
var loader = new CompanyLoader(client);

// Load information on all companies
List<Company> companies = loader.loadAll();
```

2. Load filing metadata:

```java
var loader = new FilingMetadataLoader(client);

// Load filing metadata on Q3 2023
List<FilingMetadata> metadata1 = loader.loadByQuarter(2023, 3);

// Load today's filing metadata
List<FilingMetadata> metadata2 = loader.loadDaysAgo(0);

// Load metadata on the latest 80 filings, but skipping the first 20
List<FilingMetadata> metadata3 = loader.loadLatest(20, LatestFeedCount.EIGHTY);

// Load metadata on Tesla's filings
List<FilingMetadata> metadata4 = loader.loadByCik("0001318605");
```

3. Load ownership documents:

```java
var loader = new OwnershipDocumentLoader(client);

// Load ownership document by its filing metadata
FilingMetadata metadata = ...;
OwnershipDocument doc1 = loader.loadByMetadata(metadata);

// Load ownership document by a URL
OwnershipDocument doc2 = loader.loadByUrl("https://www.sec.gov/Archives/edgar/data/1318605/000197292824000002/0001972928-24-000002.txt");
```