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

Next, you can use this client as follows: 

1. Load information about public companies in the USA:

```java
var companyLoader = new CompanyLoader(client);
List<Company> companies = companyLoader.loadAll();
```

2. Load filing metadata:

```java
var filingMetadataLoader = new FilingMetadataLoader(client);

// Load filing metadata on Q3 2023
List<FilingMetadata> metadataQ3 = 
        filingMetadataLoader.loadByQuarter(2023, 3);

// Load today's filing metadata
List<FilingMetadata> metadataToday = 
        filingMetadataLoader.loadDaysAgo(0);

// Load metadata for the latest 80 filings, but skipping the first 20
List<FilingMetadata> latestMetadata = 
        filingMetadataLoader.loadLatest(20, LatestFeedCount.EIGHTY);

// Load metadata for Tesla's filings
List<FilingMetadata> teslaMetadata = 
        filingMetadataLoader.loadByCik("1318605");
```

3. Load ownership documents:

```java
var ownershipDocumentLoader = new OwnershipDocumentLoader(client);

// Load ownership document by its filing metadata
FilingMetadata metadata = latestMetadata.get(0);
OwnershipDocument doc1 = ownershipDocumentLoader.loadByMetadata(metadata);

// Load ownership document by a URL
String url = FilingUrlBuilder.buildTxtUrl("1318605", "0001972928-24-000002");
OwnershipDocument doc2 = ownershipDocumentLoader.loadByUrl(url);
```