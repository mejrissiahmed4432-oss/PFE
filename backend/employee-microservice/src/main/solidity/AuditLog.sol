// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract AuditLog {
    struct AuditEntry {
        uint256 id;
        string  userId;
        string  userName;
        string  userRole;
        string  action;
        string  details;
        uint256 timestamp;
    }

    AuditEntry[] private entries;
    address public owner;

    event LogAdded(uint256 indexed id, string userId, string userRole, string action, uint256 timestamp);

    constructor() { owner = msg.sender; }

    function addLog(
        string memory userId,
        string memory userName,
        string memory userRole,
        string memory action,
        string memory details
    ) public {
        uint256 newId = entries.length;
        entries.push(AuditEntry({
            id: newId,
            userId: userId,
            userName: userName,
            userRole: userRole,
            action: action,
            details: details,
            timestamp: block.timestamp
        }));
        emit LogAdded(newId, userId, userRole, action, block.timestamp);
    }

    function getLogCount() public view returns (uint256) {
        return entries.length;
    }

    function getLog(uint256 index) public view returns (
        uint256 id,
        string memory userId,
        string memory userName,
        string memory userRole,
        string memory action,
        string memory details,
        uint256 timestamp
    ) {
        require(index < entries.length, "Index hors limites");
        AuditEntry storage entry = entries[index];
        return (entry.id, entry.userId, entry.userName, entry.userRole,
                entry.action, entry.details, entry.timestamp);
    }
}
