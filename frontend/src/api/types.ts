export interface VendorDto {
  id: string;
  name: string;
  taxId: string | null;
  email: string | null;
  phone: string | null;
  bankAccountLast4: string | null;
  bankRoutingNumber: string | null;
  bankAccountUpdatedAt: string | null;
  riskScore: number;
  isVerified: boolean;
  source: 'SYNTHETIC' | 'QUICKBOOKS';
  openAlertCount: number;
  invoiceCount: number;
}

export interface InvoiceDto {
  id: string;
  vendorId: string;
  vendorName: string;
  invoiceNumber: string;
  amount: number;
  currency: string;
  issuedDate: string;
  dueDate: string | null;
  status: string;
  flagged: boolean;
}

export type AlertType = 'BANK_ACCOUNT_CHANGE' | 'DUPLICATE_INVOICE' | 'VELOCITY_ANOMALY' | 'ML_ANOMALY';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AlertStatus = 'OPEN' | 'REVIEWING' | 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE' | 'RESOLVED';

export interface AlertDto {
  id: string;
  vendorId: string;
  vendorName: string;
  invoiceId: string | null;
  invoiceNumber: string | null;
  alertType: AlertType;
  severity: Severity;
  riskScore: number;
  reason: string;
  status: AlertStatus;
  createdAt: string;
}

export interface DashboardSummaryDto {
  totalVendors: number;
  totalInvoices: number;
  totalApExposure: number;
  openAlerts: number;
  criticalAlerts: number;
  averageRiskScore: number;
  alertsByType: Record<string, number>;
  recentAlerts: AlertDto[];
}

export interface LoginResponse {
  token: string;
  email: string;
  fullName: string;
  organization: string;
  role: string;
}
