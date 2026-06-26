const ALLOWED_TYPES = new Set(["application/pdf", "image/jpeg", "image/png"]);
const MAX_FILES = 5;
const MAX_FILE_SIZE = 5 * 1024 * 1024;

export function validateAttachmentFiles(files: File[], existingCount = 0): string | null {
  if (existingCount + files.length > MAX_FILES) {
    return `Tá»‘i Ä‘a ${MAX_FILES} file cho má»—i Ä‘Æ¡n.`;
  }
  for (const file of files) {
    if (!ALLOWED_TYPES.has(file.type)) {
      return "Chá»‰ há»— trá»£ PDF, JPG hoáº·c PNG.";
    }
    if (file.size > MAX_FILE_SIZE) {
      return "Má»—i file tá»‘i Ä‘a 5MB.";
    }
  }
  return null;
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
