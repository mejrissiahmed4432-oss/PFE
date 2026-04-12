export interface Ticket {
    id?: string;
    title: string;
    description: string;
    category: 'Maintenance' | 'Inspection' | 'Incident';
    priority: 'High' | 'Medium' | 'Low';
    status: 'Open' | 'In Progress' | 'Waiting' | 'Testing' | 'Resolved' | 'Closed' | 'Diagnosing' | 'Waiting for Parts' | 'Completed' | 'Under Repair' | 'In Maintenance';
    equipmentName?: string;
    userId?: string;        // createdBy
    assignedTo?: string;    // technician assigned
    deadline?: string;      // optional deadline date
    attachments?: string[]; // base64 or filenames
    createdAt?: string;
    updatedAt?: string;
}
