import { Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/layout/AppShell";
import { DashboardPage } from "@/pages/DashboardPage";
import { ChatPage } from "@/pages/ChatPage";
import { PipelinePage } from "@/pages/PipelinePage";
import { RevenuePage } from "@/pages/RevenuePage";
import { ExecutionPage } from "@/pages/ExecutionPage";
import { BillingCollectionsPage } from "@/pages/BillingCollectionsPage";

export default function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/pipeline" element={<PipelinePage />} />
        <Route path="/revenue" element={<RevenuePage />} />
        <Route path="/execution" element={<ExecutionPage />} />
        <Route path="/billing-collections" element={<BillingCollectionsPage />} />
      </Routes>
    </AppShell>
  );
}
