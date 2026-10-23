package com.alexkouzel.contract;

import com.alexkouzel.file.FileUrlBuilder;
import com.alexkouzel.file.type.f345.OwnershipDocumentLoader;
import com.alexkouzel.file.type.f345.OwnershipDocumentParser;
import com.alexkouzel.file.type.f345.OwnershipForm;
import com.alexkouzel.file.type.f345.footnote.FootnoteID;
import com.alexkouzel.file.type.f345.transaction.TransactionCode;
import com.alexkouzel.testing.FixtureClient;
import com.alexkouzel.testing.Fixtures;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks the ownership model against EDGAR's own definitions: the XSDs in specs/ and
 * real filings in the fixtures. Parsing is strict here (unlike the library) so that
 * new or unmapped elements are noticed. See "Spec drift monitoring" in docs/ROADMAP.md.
 */
public class OwnershipContractTest {
    private static final Path FIXTURES = Path.of("src/test/resources/edgar/ownership");
    private static final Path SPECS = Path.of("../specs");
    private static final String XS = "http://www.w3.org/2001/XMLSchema";

    private static final XmlMapper STRICT_MAPPER = new XmlMapper();

    static {
        STRICT_MAPPER.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    static Stream<String> fixtures() throws IOException {
        try (Stream<Path> files = Files.list(FIXTURES)) {
            return files.map(f -> f.getFileName().toString())
                    .filter(name -> name.endsWith(".txt"))
                    .map(name -> name.substring(0, name.length() - 4))
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    public void fixtureParsesStrictly(String accNo) throws Exception {
        OwnershipDocumentParser parser = new OwnershipDocumentParser(STRICT_MAPPER);

        assertNotNull(parser.parse(Fixtures.text("ownership/" + accNo + ".txt")).ownershipForm());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    public void modelKeepsEveryRepeatedElement(String accNo) throws Exception {
        assertCountsMatch(accNo);
    }

    @Test
    public void everyXsdElementIsMapped() throws Exception {
        Set<String> xsdElements = new TreeSet<>();
        for (Path xsd : ownershipXsds()) {
            NodeList elements = parseXml(Files.readAllBytes(xsd)).getElementsByTagNameNS(XS, "element");
            for (int i = 0; i < elements.getLength(); i++) {
                String name = ((Element) elements.item(i)).getAttribute("name");
                if (!name.isEmpty()) {
                    xsdElements.add(name);
                }
            }
        }
        xsdElements.remove("ownershipDocument"); // the root element itself

        Set<String> modelProperties = new HashSet<>();
        collectProperties(STRICT_MAPPER.constructType(OwnershipForm.class), modelProperties, new HashSet<>());

        Set<String> unmapped = new TreeSet<>(xsdElements);
        unmapped.removeAll(modelProperties);
        assertEquals(Set.of(), unmapped, "XSD elements without a model property");
    }

    @Test
    public void everyXsdTransactionCodeIsKnown() throws Exception {
        String common = Files.readString(latestSpec().resolve("schemas/ownershipDocumentCommon.xsd.xml"));
        Matcher matcher = Pattern.compile(
                "name=\"TRANS_CODE_PICKLIST\".*?<xs:pattern value=\"([^\"]+)\"", Pattern.DOTALL).matcher(common);
        assertEquals(true, matcher.find(), "TRANS_CODE_PICKLIST not found");

        List<String> codes = Arrays.asList(matcher.group(1).split("\\|"));
        assertEquals(18, codes.size());
        List<String> unknown = codes.stream().filter(code -> TransactionCode.ofValue(code) == null).toList();
        assertEquals(List.of(), unknown, "XSD transaction codes missing from TransactionCode");
    }

    private void assertCountsMatch(String accNo) throws Exception {
        String text = Fixtures.text("ownership/" + accNo + ".txt");
        Document xml = parseXml(xmlBlock(text).getBytes(StandardCharsets.UTF_8));

        FixtureClient client = new FixtureClient();
        String url = FileUrlBuilder.buildTxt(1, accNo);
        client.route(url, "ownership/" + accNo + ".txt");
        OwnershipForm form = new OwnershipDocumentLoader(client).loadByUrl(url).ownershipForm();

        Map<String, Integer> expected = new LinkedHashMap<>();
        Map<String, Integer> actual = new LinkedHashMap<>();
        count(expected, actual, xml, "reportingOwner", form.getReportingOwner());
        count(expected, actual, xml, "ownerSignature", form.getOwnerSignature());
        count(expected, actual, xml, "footnote", form.getFootnotes(), f -> f.getFootnote());
        count(expected, actual, xml, "nonDerivativeTransaction", form.getNonDerivativeTable(), t -> t.getNonDerivativeTransaction());
        count(expected, actual, xml, "nonDerivativeHolding", form.getNonDerivativeTable(), t -> t.getNonDerivativeHolding());
        count(expected, actual, xml, "derivativeTransaction", form.getDerivativeTable(), t -> t.getDerivativeTransaction());
        count(expected, actual, xml, "derivativeHolding", form.getDerivativeTable(), t -> t.getDerivativeHolding());
        expected.put("footnoteId", xml.getElementsByTagName("footnoteId").getLength());
        actual.put("footnoteId", countFootnoteRefs(form, Collections.newSetFromMap(new IdentityHashMap<>())));

        assertEquals(expected, actual, "element counts in XML vs model");
    }

    private static void count(Map<String, Integer> expected, Map<String, Integer> actual,
                              Document xml, String element, Collection<?> parsed) {
        expected.put(element, xml.getElementsByTagName(element).getLength());
        actual.put(element, parsed == null ? 0 : parsed.size());
    }

    private static <T> void count(Map<String, Integer> expected, Map<String, Integer> actual,
                                  Document xml, String element, T parent, Function<T, Collection<?>> list) {
        count(expected, actual, xml, element, parent == null ? null : list.apply(parent));
    }

    // Walks the model's fields and counts every FootnoteID reachable from it.
    private static int countFootnoteRefs(Object node, Set<Object> visited) throws IllegalAccessException {
        if (node == null || !visited.add(node)) {
            return 0;
        }
        if (node instanceof FootnoteID) {
            return 1;
        }
        if (node instanceof Enum<?>) {
            return 0;
        }
        if (node instanceof Collection<?> items) {
            int count = 0;
            for (Object item : items) {
                count += countFootnoteRefs(item, visited);
            }
            return count;
        }
        if (!node.getClass().getName().startsWith("com.alexkouzel.")) {
            return 0;
        }
        int count = 0;
        for (Class<?> type = node.getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                field.setAccessible(true);
                count += countFootnoteRefs(field.get(node), visited);
            }
        }
        return count;
    }

    private static void collectProperties(JavaType type, Set<String> names, Set<JavaType> seen) {
        if (type.isContainerType()) {
            collectProperties(type.getContentType(), names, seen);
            return;
        }
        Class<?> raw = type.getRawClass();
        if (raw.isPrimitive() || raw.isEnum() || raw.getName().startsWith("java.") || !seen.add(type)) {
            return;
        }
        BeanDescription description = STRICT_MAPPER.getDeserializationConfig().introspect(type);
        for (BeanPropertyDefinition property : description.findProperties()) {
            if (property.couldDeserialize()) {
                names.add(property.getName());
                collectProperties(property.getPrimaryType(), names, seen);
            }
        }
    }

    private static Path latestSpec() throws IOException {
        try (Stream<Path> dirs = Files.list(SPECS)) {
            return dirs.filter(d -> d.getFileName().toString().startsWith("ownership-xml-v"))
                    .max((a, b) -> compareVersions(a.getFileName().toString(), b.getFileName().toString()))
                    .orElseThrow();
        }
    }

    private static int compareVersions(String a, String b) {
        String[] x = a.replace("ownership-xml-v", "").split("\\.");
        String[] y = b.replace("ownership-xml-v", "").split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int cmp = Integer.compare(i < x.length ? Integer.parseInt(x[i]) : 0, i < y.length ? Integer.parseInt(y[i]) : 0);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static List<Path> ownershipXsds() throws IOException {
        try (Stream<Path> files = Files.list(latestSpec().resolve("schemas"))) {
            List<Path> xsds = new ArrayList<>(files
                    .filter(f -> f.getFileName().toString().startsWith("ownership"))
                    .toList());
            assertEquals(false, xsds.isEmpty(), "no ownership XSDs in " + latestSpec());
            return xsds;
        }
    }

    private static String xmlBlock(String text) {
        int start = text.indexOf("<XML>") + "<XML>".length();
        String xml = text.substring(start, text.indexOf("</XML>", start)).trim();
        return xml.startsWith("<?xml") ? xml.substring(xml.indexOf("?>") + 2).trim() : xml;
    }

    private static Document parseXml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
}
