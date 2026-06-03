package com.peih68.leave.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CsvWriterTest {

    private static final String BOM = "\uFEFF";

    @Test
    void prependsBomAndWritesHeaderRow() {
        String csv = new CsvWriter("a", "b", "c").build();
        assertThat(csv).startsWith(BOM);
        assertThat(csv).isEqualTo(BOM + "a,b,c\r\n");
    }

    @Test
    void writesMultipleRowsWithCrlf() {
        String csv = new CsvWriter("h1", "h2")
                .row("1", "2")
                .row("3", "4")
                .build();
        assertThat(csv).isEqualTo(BOM + "h1,h2\r\n1,2\r\n3,4\r\n");
    }

    @Test
    void quotesFieldsContainingComma() {
        String csv = new CsvWriter("x").row("a,b").build();
        assertThat(csv).contains("\"a,b\"");
    }

    @Test
    void doublesEmbeddedQuotes() {
        String csv = new CsvWriter("x").row("say \"hi\"").build();
        assertThat(csv).contains("\"say \"\"hi\"\"\"");
    }

    @Test
    void quotesFieldsContainingNewline() {
        String csv = new CsvWriter("x").row("line1\nline2").build();
        assertThat(csv).contains("\"line1\nline2\"");
    }

    @Test
    void rendersNullAsEmptyAndStringifiesNumbers() {
        String csv = new CsvWriter("a", "b").row(null, 5).build();
        assertThat(csv).isEqualTo(BOM + "a,b\r\n,5\r\n");
    }
}
