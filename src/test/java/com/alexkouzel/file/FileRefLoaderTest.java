package com.alexkouzel.file;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.file.reference.FileRef;
import com.alexkouzel.file.reference.FileRefLoader;
import com.alexkouzel.file.reference.latest.LatestFilesLimit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileRefLoaderTest {
    private static final FileRefLoader loader = new FileRefLoader(new TestEdgarClient());

    @Test
    public void loadCompany() throws HttpRequestException {
        List<FileRef> fileRefs = loader.loadCompany(946581);
        assertFalse(fileRefs.isEmpty());
        verifyFileRef(fileRefs.getFirst());
    }

    @Test
    public void loadFiscalQuarter() throws HttpRequestException, ParsingException {
        List<FileRef> fileRef = loader.loadFiscalQuarter(2022, 3);
        assertFalse(fileRef.isEmpty());
        verifyFileRef(fileRef.getFirst());
    }

    @Test
    public void loadDaysAgo() throws HttpRequestException, ParsingException {
        List<FileRef> fileRefs = loader.loadDaysAgo(1);
        assertFalse(fileRefs.isEmpty());
        verifyFileRef(fileRefs.getFirst());
    }

    @Test
    public void loadLatest() throws HttpRequestException, ParsingException {
        List<FileRef> fileRefs = loader.loadLatest(0, LatestFilesLimit.TEN);
        assertFalse(fileRefs.isEmpty());
        verifyFileRef(fileRefs.getFirst());
    }

    private void verifyFileRef(FileRef fileRef) {
        assertNotNull(fileRef);
        assertNotNull(fileRef.accNo());
        assertNotEquals(0, fileRef.issuerCik());
        assertNotNull(fileRef.type());
        assertNotNull(fileRef.filedAt());
        assertEquals(20, fileRef.accNo().length());
    }
}
