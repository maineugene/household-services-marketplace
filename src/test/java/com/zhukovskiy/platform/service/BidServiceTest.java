package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.BidDto;
import com.zhukovskiy.platform.model.*;
import com.zhukovskiy.platform.repository.BidRepository;
import com.zhukovskiy.platform.repository.OrderRepository;
import com.zhukovskiy.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BidService bidService;

    private User customer;
    private User specialist;
    private Order activeOrder;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("customer@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .country("Russia")
                .password("pass")
                .role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1))
                .build();

        specialist = User.builder()
                .id(2L)
                .email("specialist@test.com")
                .firstName("Petr")
                .lastName("Petrov")
                .country("Russia")
                .password("pass")
                .role(Role.SPECIALIST)
                .dob(LocalDate.of(1985, 5, 15))
                .build();

        activeOrder = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.ACTIVE)
                .description("Fix plumbing")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createBid_shouldSaveAndNotify() {
        BidDto bidDto = BidDto.builder()
                .price(new BigDecimal("3000"))
                .comment("I can do it")
                .build();

        when(bidRepository.existsByOrderAndSpecialist(activeOrder, specialist)).thenReturn(false);
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> {
            Bid b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        Bid result = bidService.createBid(activeOrder, specialist, bidDto);

        assertThat(result.getOrder()).isEqualTo(activeOrder);
        assertThat(result.getSpecialist()).isEqualTo(specialist);
        assertThat(result.getPrice()).isEqualByComparingTo("3000");
        assertThat(result.getComment()).isEqualTo("I can do it");
        assertThat(result.getIsSelected()).isFalse();

        verify(notificationService).notifyCustomerAboutNewBid(activeOrder, result);
    }

    @Test
    void createBid_shouldThrowWhenOrderNotActive() {
        Order completedOrder = Order.builder()
                .id(2L)
                .customer(customer)
                .status(OrderStatus.COMPLETED)
                .build();

        BidDto bidDto = BidDto.builder().price(new BigDecimal("1000")).build();

        assertThatThrownBy(() -> bidService.createBid(completedOrder, specialist, bidDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не активен");
    }

    @Test
    void createBid_shouldThrowWhenDuplicateBid() {
        BidDto bidDto = BidDto.builder().price(new BigDecimal("1000")).build();

        when(bidRepository.existsByOrderAndSpecialist(activeOrder, specialist)).thenReturn(true);

        assertThatThrownBy(() -> bidService.createBid(activeOrder, specialist, bidDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("уже подали");
    }

    @Test
    void getBidsByOrder_shouldDelegateToRepository() {
        List<Bid> expected = List.of(Bid.builder().id(1L).build());
        when(bidRepository.findByOrder(activeOrder)).thenReturn(expected);

        assertThat(bidService.getBidsByOrder(activeOrder)).isEqualTo(expected);
    }

    @Test
    void getBidsBySpecialist_shouldDelegateToRepository() {
        List<Bid> expected = List.of(Bid.builder().id(1L).build());
        when(bidRepository.findBySpecialist(specialist)).thenReturn(expected);

        assertThat(bidService.getBidsBySpecialist(specialist)).isEqualTo(expected);
    }

    @Test
    void selectBid_shouldUpdateBidAndOrder() {
        Bid bid = Bid.builder()
                .id(1L)
                .order(activeOrder)
                .specialist(specialist)
                .price(new BigDecimal("3000"))
                .isSelected(false)
                .build();

        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        bidService.selectBid(1L, customer);

        assertThat(bid.getIsSelected()).isTrue();
        assertThat(activeOrder.getSpecialist()).isEqualTo(specialist);
        assertThat(activeOrder.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(activeOrder.getUpdatedAt()).isNotNull();

        verify(notificationService).notifySpecialistSelected(activeOrder);
    }

    @Test
    void selectBid_shouldThrowWhenNotOwner() {
        Bid bid = Bid.builder()
                .id(1L)
                .order(activeOrder)
                .specialist(specialist)
                .build();

        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));

        assertThatThrownBy(() -> bidService.selectBid(1L, specialist))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("заказчик");
    }

    @Test
    void selectBid_shouldThrowWhenOrderNotActive() {
        Order completedOrder = Order.builder()
                .id(2L)
                .customer(customer)
                .status(OrderStatus.COMPLETED)
                .build();

        Bid bid = Bid.builder()
                .id(1L)
                .order(completedOrder)
                .specialist(specialist)
                .build();

        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));

        assertThatThrownBy(() -> bidService.selectBid(1L, customer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не активен");
    }

    @Test
    void selectBid_shouldThrowWhenBidNotFound() {
        when(bidRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.selectBid(99L, customer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    void hasBid_shouldReturnTrue() {
        when(bidRepository.existsByOrderAndSpecialist(activeOrder, specialist)).thenReturn(true);

        assertThat(bidService.hasBid(activeOrder, specialist)).isTrue();
    }

    @Test
    void hasBid_shouldReturnFalse() {
        when(bidRepository.existsByOrderAndSpecialist(activeOrder, specialist)).thenReturn(false);

        assertThat(bidService.hasBid(activeOrder, specialist)).isFalse();
    }

    @Test
    void getBidById_shouldReturnBid() {
        Bid bid = Bid.builder().id(1L).build();
        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));

        assertThat(bidService.getBidById(1L)).isEqualTo(bid);
    }

    @Test
    void getBidById_shouldThrowWhenNotFound() {
        when(bidRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.getBidById(99L))
                .isInstanceOf(RuntimeException.class);
    }
}
