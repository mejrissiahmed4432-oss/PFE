export interface Supplier {
  id?: string;
  companyName: string;
  address: string;
  phoneNumber: string;
  email: string;
  website: string;
  category: string;
  contactPerson: string;
  rating: number;
  note: string;
  createdAt?: Date;
  updatedAt?: Date;
}
