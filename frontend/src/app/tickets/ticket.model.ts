export interface Ticket {
    id?: string;
    title: string;
    description: string;
    category: 'Software' | 'Hardware' | 'Network' | 'Generic';
    priority: 'High' | 'Medium' | 'Low';
    status: 'Open' | 'In Progress' | 'Resolved' | 'Closed';
    userId?: string;
    createdAt?: string;
    updatedAt?: string;
}
