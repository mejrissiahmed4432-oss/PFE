package com.example.hrmicroservice.dto;

import java.util.Map;

public class HrDashboardStats {
    private long totalEmployees;
    private long activeEmployees;
    private long onLeaveEmployees;
    private long terminatedEmployees;
    private long newHiresThisMonth;
    private Map<String, Long> employeesByDepartment;

    public HrDashboardStats(long total, long active, long onLeave, long terminated,
                             long newHires, Map<String, Long> byDepartment) {
        this.totalEmployees = total;
        this.activeEmployees = active;
        this.onLeaveEmployees = onLeave;
        this.terminatedEmployees = terminated;
        this.newHiresThisMonth = newHires;
        this.employeesByDepartment = byDepartment;
    }

    public long getTotalEmployees() { return totalEmployees; }
    public long getActiveEmployees() { return activeEmployees; }
    public long getOnLeaveEmployees() { return onLeaveEmployees; }
    public long getTerminatedEmployees() { return terminatedEmployees; }
    public long getNewHiresThisMonth() { return newHiresThisMonth; }
    public Map<String, Long> getEmployeesByDepartment() { return employeesByDepartment; }
}
