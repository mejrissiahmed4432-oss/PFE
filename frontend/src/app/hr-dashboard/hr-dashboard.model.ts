export interface HrDashboardStats {
  totalEmployees: number;
  activeEmployees: number;
  onLeaveEmployees: number;
  terminatedEmployees: number;
  newHiresThisMonth: number;
  employeesByDepartment: { [key: string]: number };
}
