package com.zhukovskiy.platform.repository;

import com.zhukovskiy.platform.model.Order;
import com.zhukovskiy.platform.model.OrderStatus;
import com.zhukovskiy.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(User customer);

    List<Order> findBySpecialist(User specialist);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = 'ACTIVE' AND o.customer != ?1")
    List<Order> findAvailableOrdersForSpecialist(User specialist);

    long countByCustomerAndStatusIn(User customer, List<OrderStatus> statuses);
}