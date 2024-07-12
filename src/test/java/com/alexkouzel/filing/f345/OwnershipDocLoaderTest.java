package com.alexkouzel.filing.f345;

import com.alexkouzel.client.EdgarClient;
import com.alexkouzel.client.exceptions.RequestFailedException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.refs.FilingRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OwnershipDocLoaderTest {

    private static final EdgarClient client = new EdgarClient("TestCompany test@gmail.com");
    private static final OwnershipDocLoader loader = new OwnershipDocLoader(client);

    @Test
    public void loadByRef() throws RequestFailedException, ParsingException {
        FilingRef ref = new FilingRef("0001127602-23-028345", "1000753", FilingType.F4, null);
        OwnershipDoc doc = loader.loadByRef(ref);
        verifyOwnershipDoc(doc);
    }

    private void verifyOwnershipDoc(OwnershipDoc doc) {
        assertNotNull(doc);

        // TODO: Implement this.
    }

}
