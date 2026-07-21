import { useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Send, Sparkles, User } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

interface Message {
  role: "user" | "assistant";
  content: string;
  dataBacked?: boolean;
  toolsInvoked?: string[];
}

function newSessionId(): string {
  return `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

export function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "assistant",
      content:
        "Ask me about your pipeline, forecast, revenue, execution status, billing, or collections — e.g. \"What's our Mining sector pipeline value?\"",
    },
  ]);
  const [input, setInput] = useState("");
  const sessionId = useRef(newSessionId());

  const mutation = useMutation({
    mutationFn: (message: string) => api.chat({ sessionId: sessionId.current, message }),
    onSuccess: (response) => {
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: response.answer, dataBacked: response.dataBacked, toolsInvoked: response.toolsInvoked },
      ]);
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? error.message : "The AI provider request failed. Please try again.";
      setMessages((prev) => [...prev, { role: "assistant", content: message }]);
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || mutation.isPending) return;
    setMessages((prev) => [...prev, { role: "user", content: trimmed }]);
    setInput("");
    mutation.mutate(trimmed);
  }

  return (
    <div className="flex h-[calc(100vh-3rem)] flex-col">
      <div>
        <h2 className="text-xl font-semibold">AI Chat</h2>
        <p className="text-sm text-[var(--color-muted)]">Conversational access to every analytics tool.</p>
      </div>

      <div className="mt-4 flex-1 space-y-4 overflow-y-auto rounded-lg border border-[var(--color-border)] p-4">
        {messages.map((message, i) => (
          <div key={i} className={cn("flex gap-3", message.role === "user" && "flex-row-reverse")}>
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-black/5 dark:bg-white/10">
              {message.role === "user" ? <User className="h-4 w-4" /> : <Sparkles className="h-4 w-4 text-[var(--color-primary)]" />}
            </div>
            <Card className={cn("max-w-[75%] p-3", message.role === "user" && "bg-[var(--color-primary)] text-white")}>
              <p className="whitespace-pre-wrap text-sm">{message.content}</p>
              {message.toolsInvoked && message.toolsInvoked.length > 0 && (
                <p className="mt-2 text-xs opacity-70">via {message.toolsInvoked.join(", ")}</p>
              )}
            </Card>
          </div>
        ))}
        {mutation.isPending && (
          <div className="flex gap-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-black/5 dark:bg-white/10">
              <Sparkles className="h-4 w-4 text-[var(--color-primary)]" />
            </div>
            <Card className="p-3 text-sm text-[var(--color-muted)]">Thinking…</Card>
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit} className="mt-4 flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask a business question…"
          className="flex-1 rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
        />
        <Button type="submit" disabled={mutation.isPending || !input.trim()}>
          <Send className="h-4 w-4" />
        </Button>
      </form>
    </div>
  );
}
