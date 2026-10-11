package com.example.accountcoreledgerservices;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    /**
     * Khóa bi quan (Pessimistic Write Lock) trên 1 tài khoản (SELECT ... FOR UPDATE).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") String id);
    /**
     * Khóa đồng thời danh sách các tài khoản theo thứ tự ID ASC (Lock Ordering).
     * Loại trừ hoàn toàn hiện tượng Deadlock tại tầng Database khi 2 giao dịch gọi chéo nhau.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id IN :ids ORDER BY a.id ASC")
    List<Account> findAccountsForUpdateOrdered(@Param("ids") List<String> ids);
}
