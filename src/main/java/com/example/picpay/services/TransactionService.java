package com.example.picpay.services;
import com.example.picpay.domain.transaction.Transaction;
import com.example.picpay.domain.user.User;
import com.example.picpay.dtos.TransactionDTO;
import com.example.picpay.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionService {

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RestTemplate restTemplate;

    public Transaction createTransaction(TransactionDTO transaction) {

        LocalDateTime now = LocalDateTime.now();
        User sender = this.userService.findUSerById(transaction.senderId());
        User receiver = this.userService.findUSerById(transaction.receiverId());

        userService.validateTransaction(sender, transaction.value());

        boolean isAuthorized = authorizeTransaction(sender, transaction.value());
        if (!isAuthorized) {
            throw new IllegalArgumentException("Transaction not authorized");
        }
        Transaction transactionEntity = new Transaction();
        transactionEntity.setAmmount(transaction.value());
        transactionEntity.setReceiver(receiver);
        transactionEntity.setSender(sender);
        transactionEntity.setTimestamp(now);

        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        transactionRepository.save(transactionEntity);
        userService.saveUser(sender);
        userService.saveUser(receiver);

//        this.notificationService.sendNotification(sender, "Transação realizada com sucesso");
//        this.notificationService.sendNotification(receiver, "Você recebeu uma transação de " + sender.getFistName() + "" + sender.getLastName());
        return transactionEntity;
    }

    public boolean authorizeTransaction(User sender, BigDecimal amount) {
        ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://run.mocky.io/v3/5794d450-d2e2-4412-8131-73d0293ac1cc", Map.class);
        if (authorizationResponse.getStatusCode() == HttpStatus.OK && authorizationResponse.getBody().get("message").equals("Autorizado")) {
            return true;
        }else {
            return false;
        }
    }
}
