import { useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { saveBlob } from "@/lib/download";
import { formatDateTime } from "@/lib/format";

import { formatFileSize, validateAttachmentFiles } from "./attachment-rules";
import {
  useAttachments,
  useDeleteAttachment,
  useDownloadAttachment,
  useUploadAttachments,
} from "./hooks";
import type { AttachmentResponse } from "./types";

export function AttachmentList({
  requestId,
  canModify,
}: {
  requestId: number;
  canModify: boolean;
}) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { data: attachments, isLoading } = useAttachments(requestId);
  const upload = useUploadAttachments();
  const remove = useDeleteAttachment();
  const download = useDownloadAttachment();

  if (import.meta.env.VITE_ATTACHMENTS_ENABLED !== "true") {
    return null;
  }

  const onFiles = (files: FileList | null) => {
    const selected = Array.from(files ?? []);
    if (selected.length === 0) return;
    const validation = validateAttachmentFiles(selected, attachments?.length ?? 0);
    if (validation) {
      setError(validation);
      return;
    }
    setError(null);
    upload.mutate(
      { id: requestId, files: selected },
      {
        onSuccess: () => {
          if (inputRef.current) inputRef.current.value = "";
        },
      },
    );
  };

  const save = (attachment: AttachmentResponse) => {
    download.mutate(
      { requestId, attachmentId: attachment.id },
      { onSuccess: (blob) => saveBlob(blob, attachment.originalFilename) },
    );
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-3">
        <h4 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          File đính kèm
        </h4>
        {canModify && (
          <div>
            <Label htmlFor="attachments-upload" className="sr-only">
              Tải file đính kèm
            </Label>
            <Input
              ref={inputRef}
              id="attachments-upload"
              type="file"
              multiple
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              className="max-w-56"
              disabled={upload.isPending}
              onChange={(e) => onFiles(e.target.files)}
            />
          </div>
        )}
      </div>
      <p className="text-xs text-muted-foreground">
        Chỉ PDF, JPG, PNG. Tối đa 5 file, mỗi file 5MB.
      </p>
      {error && <p className="text-xs text-destructive">{error}</p>}
      {isLoading && <p className="text-sm text-muted-foreground">Đang tải file...</p>}
      {!isLoading && attachments?.length === 0 && (
        <p className="text-sm text-muted-foreground">Chưa có file đính kèm.</p>
      )}
      {attachments?.map((attachment) => (
        <div
          key={attachment.id}
          className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-border p-3"
        >
          <div>
            <p className="font-medium">{attachment.originalFilename}</p>
            <p className="text-xs text-muted-foreground">
              {formatFileSize(attachment.sizeBytes)} · {attachment.uploadedByName ?? "Người dùng"} ·{" "}
              {formatDateTime(attachment.createdAt)}
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={download.isPending}
              onClick={() => save(attachment)}
            >
              Tải xuống
            </Button>
            {canModify && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={remove.isPending}
                onClick={() => remove.mutate({ requestId, attachmentId: attachment.id })}
              >
                Xóa
              </Button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
