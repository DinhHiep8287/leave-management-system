import { cn } from "./utils";

describe("cn", () => {
  it("joins class names", () => {
    expect(cn("a", "b")).toBe("a b");
  });

  it("resolves conflicting tailwind classes (last wins)", () => {
    expect(cn("p-2", "p-4")).toBe("p-4");
    expect(cn("text-sm text-red-500", "text-blue-500")).toBe("text-sm text-blue-500");
  });

  it("ignores falsy values and handles conditional objects", () => {
    const includeB = false as boolean;
    expect(cn("a", includeB && "b", undefined, { c: true, d: false })).toBe("a c");
  });
});
