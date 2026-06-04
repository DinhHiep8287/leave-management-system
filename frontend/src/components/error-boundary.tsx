import { Component, type ReactNode } from "react";

import { Button } from "@/components/ui/button";

type Props = { children: ReactNode };
type State = { hasError: boolean };

/** Catches render-time errors so a single broken screen doesn't blank the whole app. */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    console.error("UI render error:", error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3 p-6 text-center">
          <p className="text-sm text-muted-foreground">Đã xảy ra lỗi hiển thị. Vui lòng tải lại trang.</p>
          <Button variant="outline" onClick={() => window.location.reload()}>
            Tải lại
          </Button>
        </div>
      );
    }
    return this.props.children;
  }
}
