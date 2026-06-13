package com.example.employeemicroservice.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.employeemicroservice.repository.PendingAuditLogRepository;
import com.example.employeemicroservice.model.PendingAuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import com.example.employeemicroservice.model.PendingAuditLog;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlockchainAuditService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainAuditService.class);

    @Value("${blockchain.ganache.url:http://127.0.0.1:7545}")
    private String ganacheUrl;

    @Value("${blockchain.ganache.private-key:}")
    private String privateKey;

    @Value("${blockchain.contract.address:}")
    private String contractAddress;

    @Autowired
    private com.example.employeemicroservice.repository.EmployeeRepository employeeRepository;

    @Autowired
    private com.example.employeemicroservice.repository.PendingAuditLogRepository pendingAuditLogRepository;

    private Web3j web3j;
    private Credentials credentials;
    private boolean isBlockchainAvailable = false;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            web3j = Web3j.build(new HttpService(ganacheUrl));
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            logger.info("Connecté à Ganache : {}", clientVersion);

            if (privateKey != null && !privateKey.isEmpty()
                    && contractAddress != null && !contractAddress.isEmpty()) {
                credentials = Credentials.create(privateKey);
                isBlockchainAvailable = true;
                logger.info("Blockchain opérationnelle. Adresse : {}", credentials.getAddress());
            } else {
                logger.warn("Clé privée ou adresse de contrat manquante — mode simulation.");
            }
        } catch (Exception e) {
            logger.error("Impossible de se connecter à Ganache sur {}", ganacheUrl, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Enregistre une action dans le Smart Contract
    // ─────────────────────────────────────────────────────────────────
    public AuditLogEntry logAction(AuditEventRequest request) {
        String txHash = "SIMULATED_TX_" + System.currentTimeMillis();

        // Résolution du CIN réel à partir de l'e-mail
        String userId = request.getUserId();
        try {
            if (userId != null && !userId.isEmpty()) {
                var empOpt = employeeRepository.findByEmail(userId);
                if (empOpt.isPresent() && empOpt.get().getCin() != null && !empOpt.get().getCin().isEmpty()) {
                    userId = empOpt.get().getCin();
                }
            }
        } catch (Exception e) {
            logger.warn("Impossible de résoudre le CIN pour {}", request.getUserId());
        }

        if (isBlockchainAvailable) {
            try {
                Function function = new Function(
                        "addLog",
                        Arrays.asList(
                                new Utf8String(userId != null ? userId : "unknown"),
                                new Utf8String(request.getUserName() != null ? request.getUserName() : "unknown"),
                                new Utf8String(request.getUserRole() != null ? request.getUserRole() : "unknown"),
                                new Utf8String(request.getAction()),
                                new Utf8String(request.getDetails() != null ? request.getDetails() : "")
                        ),
                        Collections.emptyList()
                );

                String encodedFunction = FunctionEncoder.encode(function);

                org.web3j.protocol.core.methods.response.EthGetTransactionCount ethGetTransactionCount =
                        web3j.ethGetTransactionCount(credentials.getAddress(),
                                org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
                java.math.BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                org.web3j.crypto.RawTransaction rawTransaction = org.web3j.crypto.RawTransaction.createTransaction(
                        nonce,
                        java.math.BigInteger.valueOf(20_000_000_000L),
                        java.math.BigInteger.valueOf(3_000_000L),
                        contractAddress.trim(),
                        encodedFunction
                );

                byte[] signedMessage = org.web3j.crypto.TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = org.web3j.utils.Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

                if (ethSendTransaction.hasError()) {
                    logger.error("Erreur Blockchain : {}", ethSendTransaction.getError().getMessage());
                    saveToPendingLogs(request);
                } else {
                    txHash = ethSendTransaction.getTransactionHash();
                    logger.info("Log enregistré dans la Blockchain. TxHash: {}", txHash);
                }
            } catch (Exception e) {
                logger.error("Échec de l'envoi vers la Blockchain", e);
                saveToPendingLogs(request);
            }
        } else {
            logger.warn("Blockchain indisponible (Ganache éteint). Sauvegarde dans la file d'attente pending_audit_logs.");
            saveToPendingLogs(request);
        }

        return new AuditLogEntry(
                userId,
                request.getUserName(),
                request.getUserRole(),
                request.getAction(),
                request.getDetails(),
                LocalDateTime.now()
        );
    }

    public boolean retryLogToBlockchain(PendingAuditLog pendingLog) {
        if (!isBlockchainAvailable) {
            return false;
        }

        try {
            Function function = new Function(
                    "addLog",
                    Arrays.asList(
                            new Utf8String(pendingLog.getUserId() != null ? pendingLog.getUserId() : "unknown"),
                            new Utf8String(pendingLog.getUserName() != null ? pendingLog.getUserName() : "unknown"),
                            new Utf8String(pendingLog.getUserRole() != null ? pendingLog.getUserRole() : "unknown"),
                            new Utf8String(pendingLog.getAction()),
                            new Utf8String(pendingLog.getDetails() != null ? pendingLog.getDetails() : "")
                    ),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);
            org.web3j.protocol.core.methods.response.EthGetTransactionCount ethGetTransactionCount =
                    web3j.ethGetTransactionCount(credentials.getAddress(),
                            org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
            java.math.BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            org.web3j.crypto.RawTransaction rawTransaction = org.web3j.crypto.RawTransaction.createTransaction(
                    nonce,
                    java.math.BigInteger.valueOf(20_000_000_000L),
                    java.math.BigInteger.valueOf(3_000_000L),
                    contractAddress.trim(),
                    encodedFunction
            );

            byte[] signedMessage = org.web3j.crypto.TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = org.web3j.utils.Numeric.toHexString(signedMessage);

            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

            if (ethSendTransaction.hasError()) {
                logger.error("[Retry] Erreur Blockchain : {}", ethSendTransaction.getError().getMessage());
                return false;
            } else {
                logger.info("[Retry] Log récupéré et enregistré. TxHash: {}", ethSendTransaction.getTransactionHash());
                return true;
            }
        } catch (Exception e) {
            logger.error("[Retry] Échec de l'envoi vers la Blockchain", e);
            return false;
        }
    }

    private void saveToPendingLogs(AuditEventRequest request) {
        try {
            PendingAuditLog pendingLog = new PendingAuditLog(
                    request.getUserId(),
                    request.getUserName(),
                    request.getUserRole(),
                    request.getAction(),
                    request.getDetails(),
                    null // ipAddress not sent in AuditEventRequest currently
            );
            pendingAuditLogRepository.save(pendingLog);
            logger.info("[Store & Forward] Log sauvegardé dans pending_audit_logs suite à une panne Ganache.");
        } catch (Exception e) {
            logger.error("Impossible de sauvegarder dans pending_audit_logs: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Lit tous les logs directement depuis la Blockchain (source unique de vérité)
    // ─────────────────────────────────────────────────────────────────
    public List<AuditLogEntry> getAllBlockchainLogs() {
        List<AuditLogEntry> logs = new java.util.ArrayList<>();

        if (!isBlockchainAvailable) {
            logger.error("Blockchain hors-ligne — impossible de récupérer les logs.");
            return logs;
        }

        try {
            String contract = contractAddress.trim();

            // 1. Nombre de logs dans le Smart Contract
            Function getLogCountFunc = new Function("getLogCount", Collections.emptyList(),
                    Arrays.asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {}));
            String encodedCount = FunctionEncoder.encode(getLogCountFunc);
            org.web3j.protocol.core.methods.request.Transaction txCount =
                    org.web3j.protocol.core.methods.request.Transaction
                            .createEthCallTransaction(null, contract, encodedCount);
            org.web3j.protocol.core.methods.response.EthCall countResp =
                    web3j.ethCall(txCount, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();

            if (countResp.hasError()) throw new Exception("RPC error: " + countResp.getError().getMessage());
            String rawCount = countResp.getValue();
            if (rawCount == null || "0x".equals(rawCount)) return logs;

            List<org.web3j.abi.datatypes.Type> countResults =
                    org.web3j.abi.FunctionReturnDecoder.decode(rawCount, getLogCountFunc.getOutputParameters());
            if (countResults.isEmpty()) return logs;

            long total = ((org.web3j.abi.datatypes.generated.Uint256) countResults.get(0)).getValue().longValue();

            // 2. Récupération de chaque log
            for (int i = 0; i < total; i++) {
                Function getLogFunc = new Function("getLog",
                        Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(i)),
                        Arrays.asList(
                                new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {}, // id
                                new org.web3j.abi.TypeReference<Utf8String>() {},   // userId
                                new org.web3j.abi.TypeReference<Utf8String>() {},   // userName
                                new org.web3j.abi.TypeReference<Utf8String>() {},   // userRole
                                new org.web3j.abi.TypeReference<Utf8String>() {},   // action
                                new org.web3j.abi.TypeReference<Utf8String>() {},   // details
                                new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {} // timestamp
                        ));

                String encodedGetLog = FunctionEncoder.encode(getLogFunc);
                org.web3j.protocol.core.methods.request.Transaction txGet =
                        org.web3j.protocol.core.methods.request.Transaction
                                .createEthCallTransaction(null, contract, encodedGetLog);
                org.web3j.protocol.core.methods.response.EthCall getResp =
                        web3j.ethCall(txGet, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();

                List<org.web3j.abi.datatypes.Type> r =
                        org.web3j.abi.FunctionReturnDecoder.decode(getResp.getValue(), getLogFunc.getOutputParameters());

                if (!r.isEmpty()) {
                    String bcUserId   = ((Utf8String) r.get(1)).getValue();
                    String bcUserName = ((Utf8String) r.get(2)).getValue();
                    String bcRole     = ((Utf8String) r.get(3)).getValue();
                    String bcAction   = ((Utf8String) r.get(4)).getValue();
                    String bcDetails  = ((Utf8String) r.get(5)).getValue();
                    long   tsSecs     = ((org.web3j.abi.datatypes.generated.Uint256) r.get(6)).getValue().longValue();

                    logs.add(new AuditLogEntry(
                            bcUserId, bcUserName, bcRole,
                            bcAction, bcDetails,
                            LocalDateTime.ofEpochSecond(tsSecs, 0, java.time.ZoneOffset.UTC)
                    ));
                }
            }

            java.util.Collections.reverse(logs); // Les plus récents en premier

        } catch (Exception e) {
            logger.error("Erreur lors de la lecture des logs depuis la Blockchain", e);
        }

        return logs;
    }
}
