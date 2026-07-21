import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";
import type { Severity } from "@/lib/types";

const severityClasses: Record<Severity, string> = {
  INFO: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
  LOW: "bg-slate-500/10 text-slate-600 dark:text-slate-400",
  MEDIUM: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
  HIGH: "bg-red-500/10 text-red-600 dark:text-red-400",
};

export function Badge({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-black/5 dark:bg-white/10",
        className,
      )}
      {...props}
    />
  );
}

export function SeverityBadge({ severity }: { severity: Severity }) {
  return <Badge className={severityClasses[severity]}>{severity}</Badge>;
}
