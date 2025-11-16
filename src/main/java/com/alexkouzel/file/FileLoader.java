package com.alexkouzel.file;

import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.file.reference.FileRef;
import com.alexkouzel.file.type.f345.FileF345;
import com.alexkouzel.file.type.f345.OwnershipDocumentLoader;

public class FileLoader {
    private final OwnershipDocumentLoader ownershipDocumentLoader;

    public FileLoader(HttpDataClient client) {
        this.ownershipDocumentLoader = new OwnershipDocumentLoader(client);
    }

    public FileF345 loadF345ByUrl(String url) throws Exception {
        return new FileF345(ownershipDocumentLoader.loadByUrl(url));
    }

    public FileF345 loadF345ByRef(FileRef fileRef) throws Exception {
        return new FileF345(ownershipDocumentLoader.loadByFileRef(fileRef));
    }
}
