# SEC API
This API allows you to access data from the [U.S. Securities and Exchange Commission (SEC)](https://www.sec.gov/), including:

- Information on listed companies in the USA.
- Filing references for various types: 10-Q, 10-K, 4, 8-K, 13-F, etc.
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

1. Load information about listed companies in the USA:

```java
var listedCompanyLoader = new ListedCompanyLoader(client);
List<ListedCompany> companies = listedCompanyLoader.loadAll();
```

2. Load filing references:

```java
var filingReferenceLoader = new FilingReferenceLoader(client);

// Load filing references on Q3 2023
List<FilingReference> refsQ3 = filingReferenceLoader.loadByFiscalQuarter(2023, 3);

// Load today's filing references
List<FilingReference> refsToday = filingReferenceLoader.loadDaysAgo(0);

// Load references for the latest 80 filings, but skipping the first 20
List<FilingReference> refsLatest = filingReferenceLoader.loadLatest(20, LatestFeedCount.EIGHTY);

// Load references for Tesla's (CIK = 1318605) filings
List<FilingReference> refsTesla = filingReferenceLoader.loadByCik(1318605);
```

3. Load ownership documents:

```java
var ownershipDocumentLoader = new OwnershipDocumentLoader(client);

// Load ownership document by its reference
FilingReference ref = refsToday.get(0);
OwnershipDocument doc1 = ownershipDocumentLoader.loadByRef(ref);

// Load ownership document by a URL that returns a .txt file
String url = FilingUrlBuilder.buildTxtUrl("1318605", "0001972928-24-000002");
OwnershipDocument doc2 = ownershipDocumentLoader.loadByTxtUrl(url);
```