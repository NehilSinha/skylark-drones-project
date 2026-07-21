// Mirrors the backend's Java record DTOs exactly (field-for-field) — see
// skylark-bi-agent-backend/.../analytics and .../dto. Keep these two in sync by
// hand for now; generating this from the OpenAPI spec is a natural follow-up once
// the contract stabilizes further.

export type Severity = "INFO" | "LOW" | "MEDIUM" | "HIGH";

export interface DataQualityWarning {
  severity: Severity;
  code: string;
  message: string;
  affectedRecordCount: number;
}

export interface StageBucket {
  stage: string;
  count: number;
  value: number;
}

export interface AgingBucket {
  label: string;
  count: number;
  value: number;
}

export interface PipelineAnalyticsResponse {
  totalPipelineValue: number;
  weightedPipelineValue: number;
  stageFunnel: Record<string, StageBucket>;
  averageDealSize: number;
  winRate: number;
  agingBuckets: AgingBucket[];
  warnings: DataQualityWarning[];
}

export interface ForecastResponse {
  weightedForecast: number;
  bestCase: number;
  worstCase: number;
  excludedDealCount: number;
  warnings: DataQualityWarning[];
}

export interface RevenueSummaryResponse {
  bookedRevenue: number;
  billedRevenue: number | null;
  collectedRevenue: number | null;
  outstandingReceivable: number | null;
  warnings: DataQualityWarning[];
}

export interface DelayedWorkOrder {
  dealName: string;
  customerCode: string;
  probableEndDate: string;
  daysOverdue: number;
}

export interface ExecutionAnalyticsResponse {
  statusDistribution: Record<string, number>;
  delayed: DelayedWorkOrder[];
  averageDeliveryVarianceDays: number | null;
  warnings: DataQualityWarning[];
}

export interface BillingAnalyticsResponse {
  billingStatusDistribution: Record<string, number>;
  invoiceStatusDistribution: Record<string, number>;
  totalAmountToBeBilled: number;
  warnings: DataQualityWarning[];
}

export interface AgedReceivable {
  dealName: string;
  customerCode: string;
  amount: number;
  daysSinceProbableEnd: number;
}

export interface CollectionsAnalyticsResponse {
  totalCollected: number;
  totalReceivable: number;
  agedReceivables: AgedReceivable[];
  warnings: DataQualityWarning[];
}

export interface ChatRequest {
  sessionId: string;
  message: string;
}

export interface ChatResponse {
  sessionId: string;
  answer: string;
  dataBacked: boolean;
  toolsInvoked: string[];
}

export interface HealthResponse {
  status: string;
  timestamp: string;
  mondayApiConfigured: boolean;
  mondayBoardsConfigured: boolean;
  llmConfigured: boolean;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  errorCode: string;
  message: string;
  traceId: string;
  details: string[];
}

export interface AnalyticsFilterParams {
  sector?: string[];
  owner?: string[];
  client?: string[];
  status?: string[];
  dateFrom?: string;
  dateTo?: string;
}
