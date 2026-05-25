package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.BidDto;
import com.zhukovskiy.platform.model.Bid;
import com.zhukovskiy.platform.model.Order;
import com.zhukovskiy.platform.model.OrderStatus;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.BidRepository;
import com.zhukovskiy.platform.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * Подача заявки на заказ
     */
    @Transactional
    public Bid createBid(Order order, User specialist, BidDto bidDto) {
        // Проверяем, что заказ активен
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new RuntimeException("Заказ уже не активен");
        }

        // Проверяем, что специалист еще не подавал заявку на этот заказ
        if (bidRepository.existsByOrderAndSpecialist(order, specialist)) {
            throw new RuntimeException("Вы уже подали заявку на этот заказ");
        }

        Bid bid = Bid.builder()
                .order(order)
                .specialist(specialist)
                .price(bidDto.getPrice())
                .comment(bidDto.getComment())
                .createdAt(LocalDateTime.now())
                .isSelected(false)
                .build();

        Bid savedBid = bidRepository.save(bid);

        // Уведомляем заказчика о новой заявке
        notificationService.notifyCustomerAboutNewBid(order, savedBid);

        return savedBid;
    }

    /**
     * Получение всех заявок на заказ
     */
    public List<Bid> getBidsByOrder(Order order) {
        return bidRepository.findByOrder(order);
    }

    /**
     * Получение всех заявок специалиста
     */
    public List<Bid> getBidsBySpecialist(User specialist) {
        return bidRepository.findBySpecialist(specialist);
    }

    /**
     * Выбор заявки (при выборе специалиста)
     */
    @Transactional
    public void selectBid(Long bidId, User customer) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        Order order = bid.getOrder();

        // Проверяем, что заказчик имеет право выбирать
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Только заказчик может выбрать заявку");
        }

        // Проверяем, что заказ еще активен
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new RuntimeException("Заказ уже не активен");
        }

        // Отмечаем выбранную заявку
        bid.setIsSelected(true);
        bidRepository.save(bid);

        // Обновляем заказ: выбираем специалиста
        order.setSpecialist(bid.getSpecialist());
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Уведомляем выбранного специалиста
        notificationService.notifySpecialistSelected(order);
    }
}