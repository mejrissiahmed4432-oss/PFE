export interface Software {
  id?: string;
  name: string;
  type: string;
  vendor: string;
  version?: string;
  website?: string;
  status: string;
  totalSeats?: number;
  availableSeats?: number;
  createdAt?: string;
}

export enum LicenseModel {
  SUBSCRIPTION = 'SUBSCRIPTION',
  PERPETUAL = 'PERPETUAL',
  OEM = 'OEM',
  VOLUME = 'VOLUME'
}

export enum ActivationMethod {
  USER_LOGIN = 'USER_LOGIN',
  DEVICE_BOUND = 'DEVICE_BOUND',
  KEY_BASED = 'KEY_BASED',
  SERVER_KMS = 'SERVER_KMS'
}

export enum RenewalType {
  AUTO_RENEW = 'AUTO_RENEW',
  MANUAL = 'MANUAL',
  NONE = 'NONE'
}

export interface LicensePool {
  id?: string;
  softwareId?: string;
  licenseModel: LicenseModel;
  activationMethod: ActivationMethod;
  totalSeats: number;
  availableSeats?: number;
  expirationDate?: string;
  renewalType: RenewalType;
  rawKeys?: string[];
  vendorSyncStatus?: string;
}

export enum AssignedToType {
  USER = 'USER',
  DEVICE = 'DEVICE',
  DEPARTMENT = 'DEPARTMENT'
}

export enum AssignmentStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  REVOKED = 'REVOKED'
}

export interface SoftwareAssignment {
  id?: string;
  licensePoolId: string;
  softwareId: string;
  assignedToType: AssignedToType;
  assignedTargetId: string;
  assignedTargetName: string;
  status?: AssignmentStatus;
  assignedAt?: string;
  expiresAt?: string;
  revokedAt?: string;
  assignedBy?: string;
  licenseKeyUsed?: string;
}
