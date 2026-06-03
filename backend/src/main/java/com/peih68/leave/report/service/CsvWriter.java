package com.peih68.leave.report.service;

/**
 * Minimal RFC-4180 CSV builder. Fields are quoted only when they contain a comma,
 * quote, or line break; embedded quotes are doubled. Rows end with CRLF. A UTF-8 BOM
 * is prepended so Excel opens Vietnamese text in the correct encoding.
 */
public class CsvWriter {

    private static final String BOM = "\uFEFF";
    private static final String CRLF = "\r\n";

    private final StringBuilder sb = new StringBuilder(BOM);

    public CsvWriter(Object... headers) {
        row(headers);
    }

    public CsvWriter row(Object... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        sb.append(CRLF);
        return this;
    }

    public String build() {
        return sb.toString();
    }

    private static String escape(Object value) {
        String s = value == null ? "" : value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
