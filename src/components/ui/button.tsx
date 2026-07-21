import type { ButtonHTMLAttributes } from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 rounded-md text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-[var(--color-primary)] text-white hover:opacity-90",
        outline: "border border-[var(--color-border)] hover:bg-black/5 dark:hover:bg-white/5",
        ghost: "hover:bg-black/5 dark:hover:bg-white/5",
      },
      size: {
        default: "h-9 px-4",
        sm: "h-8 px-3",
        icon: "h-9 w-9",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  },
);

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {}

export function Button({ className, variant, size, ...props }: ButtonProps) {
  return <button className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}
