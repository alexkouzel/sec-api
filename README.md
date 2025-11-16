# SEC API
This API allows you to access data from the [U.S. Securities and Exchange Commission (SEC)](https://www.sec.gov/), including:

- Companies registered by SEC.
- File references for various types: 10-Q, 10-K, 4, 8-K, 13-F, etc.
- Ownership documents: F3, F4, and F5.

## Getting Started
To begin, create an HTTP client to access SEC data:

```java
String userAgent = "TestCompany test.company@gmail.com";
EdgarClient client = new EdgarClient(userAgent);
```

The `userAgent` should follow this format:

```
Sample Company Name AdminContact@<sample company domain>.com
```

Next, you can use this client as follows: 

1. Create data loaders:
    ```java
    var companyLoader = new SecCompanyLoader(client);
    var fileRefLoader = new FileRefLoader(client);
    var fileLoader = new FileLoader(client);
    ```
   
2. Load information about companies:
    ```java
    List<SecCompany> companies = companyLoader.load();
    ```
   
3. Load file references:
    ```java
    // Load file references from Q3 2023:
    List<FileRef> fileRefs1 = fileRefLoader.loadFiscalQuarter(2023, 3);

    // Load today's file references:
    List<FileRef> fileRefs2 = fileRefLoader.loadToday();

    // Load the latest 80 file references:
    List<FileRef> fileRefs3 = fileRefLoader.loadLatest(LatestFilesLimit.EIGHTY);
    
    // Load file references for Tesla (CIK = 1318605):
    List<FileRef> fileRefs4 = fileRefLoader.loadCompany(1318605);
    ```
   
4. Load ownership documents:
    ```java
    // Load ownership document by its file reference
    FileRef fileRef = fileRefs1.get(0);
    FileF345 file1 = fileLoader.loadF345ByRef(fileRef);
    
    // Load ownership document by a URL that returns a .txt file
    String fileUrl = FileUrlBuilder.buildTxt("1318605", "0001972928-24-000002");
    FileF345 file2 = fileLoader.loadF345ByUrl(fileUrl);
    ```
