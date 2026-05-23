package com.zhukovskiy.platform.repository;

import com.zhukovskiy.platform.model.Bid;
import com.zhukovskiy.platform.model.Order;
import com.zhukovskiy.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByOrder(Order order);

    List<Bid> findBySpecialist(User specialist);

    Optional<Bid> findByOrderAndSpecialist(Order order, User specialist);

    boolean existsByOrderAndSpecialist(Order order, User specialist);
}