import { formatFileSize, validateAttachmentFiles } from "./attachment-rules";

function file(name: string, type: string, size: number): File {
  return new File([new Uint8Array(size)], name, { type });
}

describe("attachment rules", () => {
  it("accepts pdf jpg and png under 5MB", () => {
    expect(validateAttachmentFiles([
      file("a.pdf", "application/pdf", 1024),
      file("b.jpg", "image/jpeg", 1024),
      file("c.png", "image/png", 1024),
    ])).toBeNull();
  });

  it("rejects unsupported types", () => {
    expect(validateAttachmentFiles([file("a.txt", "text/plain", 1024)])).toBe(
      "Chá»‰ há»— trá»£ PDF, JPG hoáº·c PNG.",
    );
  });

  it("rejects too many files including existing attachments", () => {
    expect(validateAttachmentFiles([file("a.pdf", "application/pdf", 1024)], 5)).toBe(
      "Tá»‘i Ä‘a 5 file cho má»—i Ä‘Æ¡n.",
    );
  });

  it("formats file size", () => {
    expect(formatFileSize(1024)).toBe("1 KB");
    expect(formatFileSize(2 * 1024 * 1024)).toBe("2.0 MB");
  });
});
