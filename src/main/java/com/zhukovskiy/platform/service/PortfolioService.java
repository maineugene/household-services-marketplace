package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.PortfolioItemDto;
import com.zhukovskiy.platform.model.PortfolioItem;
import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.repository.PortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;

    private static final String UPLOAD_DIR = "uploads/portfolio/";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /**
     * Получение портфолио специалиста
     */
    public List<PortfolioItem> getPortfolioBySpecialist(SpecialistProfile specialist) {
        return portfolioItemRepository.findBySpecialistProfileOrderByCreatedAtDesc(specialist);
    }

    /**
     * Получение количества работ в портфолио
     */
    public int getPortfolioCount(SpecialistProfile specialist) {
        return portfolioItemRepository.countBySpecialistProfile(specialist);
    }

    /**
     * Добавление работы в портфолио
     */
    @Transactional
    public PortfolioItem addPortfolioItem(SpecialistProfile specialist,
                                          PortfolioItemDto dto,
                                          MultipartFile image) throws IOException {
        if (getPortfolioCount(specialist) >= 20) {
            throw new RuntimeException("Достигнут лимит фотографий в портфолио (максимум 20)");
        }

        String imageUrl = saveImage(image);

        PortfolioItem item = PortfolioItem.builder()
                .specialistProfile(specialist)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .build();

        return portfolioItemRepository.save(item);
    }

    /**
     * Удаление работы из портфолио
     */
    @Transactional
    public void deletePortfolioItem(SpecialistProfile specialist, Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Работа не найдена"));

        // Исправлено: используем specialistProfile
        if (!item.getSpecialistProfile().getId().equals(specialist.getId())) {
            throw new RuntimeException("Нет прав для удаления этой работы");
        }

        deleteImage(item.getImageUrl());
        portfolioItemRepository.delete(item);
    }

    /**
     * Сохранение изображения на диск
     */
    private String saveImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Размер файла превышает допустимый лимит (10 МБ)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Недопустимый формат файла. Разрешены: JPEG, PNG, GIF, WebP");
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Недопустимый формат файла");
        };

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Некорректный путь к файлу");
        }

        Files.copy(file.getInputStream(), filePath);

        return "/uploads/portfolio/" + filename;
    }

    /**
     * Удаление изображения с диска
     */
    private void deleteImage(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при удалении файла: " + e.getMessage());
        }
    }
}