export type Theme = "light" | "dark";

const KEY = "leave.theme";

/** Stored preference, else the OS preference. */
export function getStoredTheme(): Theme {
  try {
    const s = localStorage.getItem(KEY);
    if (s === "light" || s === "dark") return s;
  } catch {
    // ignore — storage may be blocked
  }
  return window.matchMedia?.("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function applyTheme(theme: Theme): void {
  document.documentElement.classList.toggle("dark", theme === "dark");
}

export function setTheme(theme: Theme): void {
  try {
    localStorage.setItem(KEY, theme);
  } catch {
    // ignore
  }
  applyTheme(theme);
}
