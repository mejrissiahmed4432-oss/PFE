export interface Ticket {
    id?: string; // unique id
    title: string;
    description: string;
    category: 'Maintenance' | 'Inspection' | 'Incident';
    priority: 'High' | 'Medium' | 'Low';
    status: 'Open' | 'In Progress' | 'Waiting' | 'Testing' | 'Resolved' | 'Closed' | 'Diagnosing' | 'Waiting for Parts' | 'Completed' | 'Under Repair' | 'In Maintenance' | 'Cancelled';
    equipmentId?: string;
    equipmentName?: string;
    userId?: string;        // createdBy
    userName?: string;      // creator's name
    userRole?: string;      // creator's role
    assignedTo?: string;    // technician assigned
    technicianName?: string; // technician name
    deadline?: string;      // optional deadline date
    attachments?: string[]; // base64 or filenames
    workNote?: string;
    repairTasks?: any[];
    partsUsed?: any[];
    diagnosisResult?: string;
    validationSummary?: string;
    partsInstalled?: any[];
    createdAt?: string;
    updatedAt?: string;
}
