import type {
  AnalyticsFilterParams,
  BillingAnalyticsResponse,
  ChatRequest,
  ChatResponse,
  CollectionsAnalyticsResponse,
  ErrorResponse,
  ExecutionAnalyticsResponse,
  ForecastResponse,
  HealthResponse,
  PipelineAnalyticsResponse,
  RevenueSummaryResponse,
} from "./types";

/** Thrown for any non-2xx response, carrying the backend's structured ErrorResponse when available. */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly errorCode?: string,
    public readonly traceId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });

  if (!response.ok) {
    let body: ErrorResponse | undefined;
    try {
      body = (await response.json()) as ErrorResponse;
    } catch {
      // response body wasn't JSON (e.g. a proxy/network-level error page) — fall through with no structured detail
    }
    throw new ApiError(
      body?.message ?? `Request to ${path} failed with status ${response.status}`,
      response.status,
      body?.errorCode,
      body?.traceId,
    );
  }

  return response.json() as Promise<T>;
}

function toQueryString(filter?: AnalyticsFilterParams): string {
  if (!filter) return "";
  const params = new URLSearchParams();
  filter.sector?.forEach((v) => params.append("sector", v));
  filter.owner?.forEach((v) => params.append("owner", v));
  filter.client?.forEach((v) => params.append("client", v));
  filter.status?.forEach((v) => params.append("status", v));
  if (filter.dateFrom) params.set("dateFrom", filter.dateFrom);
  if (filter.dateTo) params.set("dateTo", filter.dateTo);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

export const api = {
  health: () => request<HealthResponse>("/api/health"),

  pipeline: (filter?: AnalyticsFilterParams) =>
    request<PipelineAnalyticsResponse>(`/api/analytics/pipeline${toQueryString(filter)}`),

  forecast: (horizonStart: string, horizonEnd: string, filter?: AnalyticsFilterParams) =>
    request<ForecastResponse>(
      `/api/analytics/forecast${toQueryString(filter)}${toQueryString(filter) ? "&" : "?"}horizonStart=${horizonStart}&horizonEnd=${horizonEnd}`,
    ),

  revenue: (filter?: AnalyticsFilterParams) =>
    request<RevenueSummaryResponse>(`/api/analytics/revenue${toQueryString(filter)}`),

  execution: (filter?: AnalyticsFilterParams) =>
    request<ExecutionAnalyticsResponse>(`/api/analytics/execution${toQueryString(filter)}`),

  billing: (filter?: AnalyticsFilterParams) =>
    request<BillingAnalyticsResponse>(`/api/analytics/billing${toQueryString(filter)}`),

  collections: (filter?: AnalyticsFilterParams) =>
    request<CollectionsAnalyticsResponse>(`/api/analytics/collections${toQueryString(filter)}`),

  chat: (body: ChatRequest) =>
    request<ChatResponse>("/api/chat", { method: "POST", body: JSON.stringify(body) }),
};
