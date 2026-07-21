import { useEffect, useState, type ReactNode } from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  MessageSquare,
  TrendingUp,
  Wallet,
  Truck,
  Receipt,
  Moon,
  Sun,
} from "lucide-react";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/chat", label: "AI Chat", icon: MessageSquare },
  { to: "/pipeline", label: "Pipeline & Forecast", icon: TrendingUp },
  { to: "/revenue", label: "Revenue", icon: Wallet },
  { to: "/execution", label: "Execution", icon: Truck },
  { to: "/billing-collections", label: "Billing & Collections", icon: Receipt },
];

function useDarkMode() {
  const [dark, setDark] = useState(() => window.matchMedia("(prefers-color-scheme: dark)").matches);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
  }, [dark]);

  return { dark, toggle: () => setDark((d) => !d) };
}

export function AppShell({ children }: { children: ReactNode }) {
  const { dark, toggle } = useDarkMode();

  return (
    <div className="flex min-h-screen">
      <aside className="w-64 shrink-0 border-r border-[var(--color-border)] bg-[var(--color-surface)] p-4 flex flex-col">
        <div className="mb-6 px-2">
          <h1 className="text-lg font-semibold">Skylark BI Agent</h1>
          <p className="text-xs text-[var(--color-muted)]">AI Business Intelligence</p>
        </div>
        <nav className="flex flex-col gap-1">
          {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-[var(--color-primary)] text-white"
                    : "text-[var(--color-muted)] hover:bg-black/5 dark:hover:bg-white/5",
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>
        <button
          onClick={toggle}
          className="mt-auto flex items-center gap-2 rounded-md px-3 py-2 text-sm text-[var(--color-muted)] hover:bg-black/5 dark:hover:bg-white/5"
        >
          {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          {dark ? "Light mode" : "Dark mode"}
        </button>
      </aside>
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  );
}
