export interface Task {
  id: string;
  title: string;
  description: string;
  type: 'Equipment' | 'Maintenance' | 'Stock' | 'General';
  priority: 'High' | 'Medium' | 'Low';
  status: 'Pending' | 'In Progress' | 'Completed' | 'History';
  dueDate: string;
  assignedTo: string;
<<<<<<< HEAD
=======
  userId?: string;
>>>>>>> my-local-work
  createdAt: string;
  updatedAt?: string;
  originalDueDate?: string;
}
