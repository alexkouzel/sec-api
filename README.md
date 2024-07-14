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

This client adheres to a rate limit of 10 requests per second, as specified in the SEC's [Accessing EDGAR Data](https://www.sec.gov/search-filings/edgar-search-assistance/accessing-edgar-data) guidelines. 

Next, set up data loaders that use this client:

```java
// To load information about public companies in the USA
CompanyLoader companyLoader = new CompanyLoader(client);

// To load filing metadata
FilingMetadataLoader metadataLoader = new FilingMetadataLoader(client);

// To load ownership documents
OwnershipDocumentLoader docLoader = new OwnershipDocumentLoader(client);
```

You can then use these loaders as follows:

```java
// Load all public companies in the USA
List<Company> companies = companyLoader.loadAll();

// Load today's metadata on F4 filings (ownership documents)
for (FilingMetadata metadata : metadataLoader.loadDaysAgo(FilingType.F4, 0)) {

    // Load filing content using metadata
    OwnershipDocument doc = docLoader.loadByMetadata(metadata);

    // Process the document as needed
    // ...
}
```

You can load filing metadata using several methods:

1. loadDaysAgo: Load metadata from 'n' days ago.
2. loadByQuarter: Load metadata for a specific financial quarter.
3. loadLatest: Load the latest 'n' metadata entries.
4. loadByCik: Load metadata for a given issuer by CIK (Central Index Key).