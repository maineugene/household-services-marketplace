package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.BidDto;
import com.zhukovskiy.platform.model.Bid;
import com.zhukovskiy.platform.model.Order;
import com.zhukovskiy.platform.model.OrderStatus;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.BidRepository;
import com.zhukovskiy.platform.repository.OrderRepository;
import com.zhukovskiy.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zhukovskiy.platform.exception.BusinessRuleException;
import com.zhukovskiy.platform.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Подача заявки на заказ
     */
    @Transactional
    public Bid createBid(Order order, User specialist, BidDto bidDto) {
        // Проверяем, что заказ активен
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new BusinessRuleException("Заказ уже не активен");
        }

        // Проверяем, что специалист еще не подавал заявку на этот заказ
        if (bidRepository.existsByOrderAndSpecialist(order, specialist)) {
            throw new BusinessRuleException("Вы уже подали заявку на этот заказ");
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
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        Order order = bid.getOrder();

        // Проверяем, что заказчик имеет право выбирать
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessRuleException("Только заказчик может выбрать заявку");
        }

        // Проверяем, что заказ еще активен
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new BusinessRuleException("Заказ уже не активен");
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

    /**
     * Проверка, подавал ли специалист заявку на заказ
     */
    public boolean hasBid(Order order, User specialist) {
        return bidRepository.existsByOrderAndSpecialist(order, specialist);
    }

    /**
     * Получение заявки по ID
     */
    public Bid getBidById(Long id) {
        return bidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
    }
}